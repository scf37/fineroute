package me.scf37.fine.route

import cats.implicits._
import cats.kernel.CommutativeMonoid
import cats.{MonadError, ~>}
import me.scf37.fine.route.endpoint.{Endpoint, MatchedRequest}
import me.scf37.fine.route.matcher.Matcher
import me.scf37.fine.route.typeclass.RouteHttpRequest

/**
 * Route - return request handler by request, where request handler is `() => F[Resp]`.
 * It is generic (any request/response type, any MonadError effect), efficient (O(ln(n)) matching speed)
 * and composable.
 *
 * Using Route with specific effect and web framework.
 *
 * Recommended approach is to create specific trait extending Route and providing required typeclasses:
 * trait AkkaFutureRoute extends Route[Future, Request, Response] {
 *   override protected def monadError ...
 *   override protected def routeHttpRequest ...
 *   override protected def routeHttpResponse ...
 * }
 *
 * Building routes.
 *
 * 1. Using endpoint DSL
 * trait MyRoute extends AkkaFutureRoute {
 *   endpoint.get("/") { () => ... }
 *   endpoint.consumes[LoginRequest].post("/login") { loginRequest => ... }
 * }
 * 2. Composing routes
 * - trait TotalRoute extends MyRoute with AnotherRoute
 * - myRoute.andThen(otherRoute)
 * - cats.Monoid instance
 * 3. Building routes manually
 * Route.mk(Endpoint(...), Endpoint(...))
 *
 * @tparam F Route effect, must be MonadError[?[_], Throwable]
 * @tparam Req Route HTTP request type, must be RouteHttpRequest
 * @tparam Resp Route HTTP response type, must be RouteHttpResponse
 */
sealed trait Route[F[_], Req, Resp] extends (RouteRequest => F[Req => F[Resp]]) {
   /** meta information on route endpoints, used to generate docs and clients */
  def meta: RouteMeta// = RouteMeta(matcher.endpoints.map(_.meta))

  /**
   * Look up handler by request
   *
   * @param req HTTP request
   * @return request handler for that request
   * @throws RouteUnmatchedException if there is no endpoint for this request
   * @throws RouteParamParseException if path/query params conversion failed
   */
  override def apply(req: RouteRequest): F[Req => F[Resp]]

  /**
   * Combine two routes
   *
   * @param r another route
   * @return new route containing endpoints from both routes
   */
  def combine(r: Route[F, Req, Resp]): Route[F, Req, Resp]

  /**
   * Map route response
   *
   * @param f mapping function
   * @tparam Resp2 new response type
   * @return new route with the same endpoints but different response type
   */
  def map[Resp2](f: Resp => Resp2): Route[F, Req, Resp2]

  /**
   * Map route request
   *
   * @param f mapping function
   * @tparam Req2 new request type
   * @return new route with the same endpoints but different request type
   */
  def rmap[Req2: RouteHttpRequest](f: Req2 => Req): Route[F, Req2, Resp]

  /**
   * Map route effect, e.g. from Route[Either] to Route[Future]
   * @param f effect mapping function
   * @tparam G new route effect
   * @return new route with the same endpoints but different effect type
   */
  def mapK[G[_]: MonadError[?[_], Throwable]](f: F ~> G): Route[G, Req, Resp]

  /**
   * Wrap this route with filter. Filter is executed after matching but before endpoint handler
   *
   * Filter is a function that takes handler and produces another handler.
   *
   * @param filter filter
   * @tparam Req2 new request type
   * @tparam Resp2 new response type
   * @return route that wraps endpoint handlers with this filter
   */
  def compose[Req2: RouteHttpRequest, Resp2](filter: (Req => F[Resp]) => (Req2 => F[Resp2]))

  /**
   * Compose two routes sequentially, i.e. run first route, then run second if first fails.
   *
   * It allows combining routes with conflicting paths but has worse performance
   *
   * @param r
   * @return
   */
  def andThen(r: Route[F, Req, Resp]): Route[F, Req, Resp]

  def compose0[Req2, Resp2](filter: (RouteRequest => F[Req => F[Resp]]) => (RouteRequest => F[Req2 => F[Resp2]])): Route[F, Req2, Resp2]
}

object Route {

  /**
   * Empty route
   *
   * @tparam F Route effect
   * @tparam Req Route HTTP request type
   * @tparam Resp Route HTTP response type
   * @return empty route
   */
  def empty[F[_]: MonadError[?[_], Throwable], Req: RouteHttpRequest, Resp]: Route[F, Req, Resp] =
    RouteImpl(Nil)

  /**
   * Make route out of set of Endpoint instances
   *
   * @param endpoints endpoints
   * @tparam F Route effect
   * @tparam Req Route HTTP request type
   * @tparam Resp Route HTTP response type
   * @return route
   */
  def mk[F[_]: MonadError[?[_], Throwable], Req: RouteHttpRequest, Resp](endpoints: Endpoint[F, Req, Resp]*): Route[F, Req, Resp] = {
    RouteImpl(List(Matcher.mk(endpoints: _*)))
  }

  def mk[F[_]: MonadError[?[_], Throwable], Req, Resp](meta: RouteMeta)(f: RouteRequest => F[Req => F[Resp]]): Route[F, Req, Resp] = {
    DumbRoute(meta, f)
  }

  /** CommutativeMonoid for Route */
  implicit def monoidInstance[F[_]: MonadError[?[_], Throwable], Req: RouteHttpRequest, Resp]: CommutativeMonoid[Route[F, Req, Resp]] = new CommutativeMonoid[Route[F, Req, Resp]] {
    override def empty: Route[F, Req, Resp] = Route.empty

    override def combine(x: Route[F, Req, Resp], y: Route[F, Req, Resp]): Route[F, Req, Resp] = {
      x.combine(y)
    }
  }

  private case class RouteImpl[F[_]: MonadError[?[_], Throwable], Req: RouteHttpRequest, Resp](
    matchers: List[Matcher[F, Req, Resp]]
  ) extends Route[F, Req, Resp] {

    override def meta: RouteMeta = RouteMeta(matchers.flatMap(matcher => matcher.endpoints.map(_.meta)))

    override def apply(req: RouteRequest): F[Req => F[Resp]] = {

      def loop(matchers: List[Matcher[F, Req, Resp]]): F[Req => F[Resp]] = matchers match {

        case Nil => MonadError[F, Throwable].raiseError(RouteUnmatchedException)

        case m :: tail =>
          m.matchRequest(req).map {
            case (mreq) => (req2: Req) => mreq.value.handle(
              makeRequest(req2, mreq.unmatched.mkString("/"), mreq.params.toMap, mreq.value)
            )
          }.handleErrorWith {
            case RouteUnmatchedException => loop(tail)
            case e => MonadError[F, Throwable].raiseError(e)
          }
      }

      loop(matchers)
    }

    override def combine(r: Route[F, Req, Resp]): Route[F, Req, Resp] = {
      r match {

        case r: RouteImpl[F, Req, Resp] =>
          val matchers = this.matchers.map(Option.apply).zipAll(r.matchers.map(Option.apply), None, None).map {

            case (Some(m1), Some(m2)) => m1.combine(m2)

            case (m1Opt, m2Opt) => m1Opt.orElse(m2Opt).get
          }

          RouteImpl(matchers)

        case _ => r.combine(this)
      }

    }

    override def map[Resp2](f: Resp => Resp2): Route[F, Req, Resp2] = {
      RouteImpl(matchers.map(_.map(f)))
    }

    override def rmap[Req2: RouteHttpRequest](f: Req2 => Req): Route[F, Req2, Resp] = {
      RouteImpl(matchers.map(_.rmap(f)))
    }

    override def mapK[G[_]: MonadError[?[_], Throwable]](f: F ~> G): Route[G, Req, Resp] =
      RouteImpl(matchers.map(_.mapK(f)))

    override def compose[Req2: RouteHttpRequest, Resp2](filter: (Req => F[Resp]) => (Req2 => F[Resp2])) =
      RouteImpl(matchers.map(_.compose(filter)))

    override def andThen(r: Route[F, Req, Resp]): Route[F, Req, Resp] = r match {
      case r: RouteImpl[F, Req, Resp] =>
        RouteImpl(matchers ++ r.matchers)

      case _ => r.andThen(this)
    }

    override def compose0[Req2, Resp2](filter: (RouteRequest => F[Req => F[Resp]]) => (RouteRequest => F[Req2 => F[Resp2]])): Route[F, Req2, Resp2] = {
      Route.mk(meta)(filter(this))
    }

    private def makeRequest(
      req: Req,
      unmatchedPath: String,
      pathParams: Map[String, String],
      e: Endpoint[F, Req, Resp]
    ) = {
      MatchedRequest[Req](
        req = req,
        url = RouteHttpRequest[Req].url(req),
        meta = e.meta,
        unmatchedPath = unmatchedPath,
        pathParams = pathParams,
        queryParams = RouteHttpRequest[Req].queryParams(req),
        body = RouteHttpRequest[Req].body(req)
      )
    }
  }

  private case class DumbRoute[F[_]: MonadError[?[_], Throwable], Req, Resp](
    meta: RouteMeta,
    handler: RouteRequest => F[Req => F[Resp]]
  ) extends Route[F, Req, Resp] {

    override def apply(req: RouteRequest): F[Req => F[Resp]] = handler(req)

    override def combine(r: Route[F, Req, Resp]): Route[F, Req, Resp] = andThen(r)

    override def map[Resp2](f: Resp => Resp2): Route[F, Req, Resp2] = Route.mk(meta) { req =>
      this.apply(req).map(h => req2 => h(req2).map(f))
    }

    override def rmap[Req2: RouteHttpRequest](f: Req2 => Req): Route[F, Req2, Resp] = Route.mk(meta) { req =>
      this.apply(req).map(h => req2 => h(f(req2)))
    }

    override def mapK[G[_] : MonadError[?[_], Throwable]](f: F ~> G): Route[G, Req, Resp] = Route.mk(meta) { req =>
      f(this.apply(req).map(h => req2 => f(h(req2))))
    }

    override def compose[Req2: RouteHttpRequest, Resp2](filter: (Req => F[Resp]) => Req2 => F[Resp2]): Unit = Route.mk(meta) { req =>
      this.apply(req).map(filter)
    }

    override def andThen(r: Route[F, Req, Resp]): Route[F, Req, Resp] = Route.mk(meta ++ r.meta) { req =>
      this.apply(req).handleErrorWith {
        case RouteUnmatchedException => r.apply(req)
        case e => MonadError[F, Throwable].raiseError(e)
      }
    }

    override def compose0[Req2, Resp2](filter: (RouteRequest => F[Req => F[Resp]]) => (RouteRequest => F[Req2 => F[Resp2]])): Route[F, Req2, Resp2] = {
      Route.mk(meta)(filter(this))
    }

  }
}
