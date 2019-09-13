package me.scf37.fine.route

import cats.implicits._
import cats.kernel.CommutativeMonoid
import cats.{Functor, ~>}
import me.scf37.fine.route.endpoint.meta.Meta
import me.scf37.fine.route.endpoint.{Endpoint, MatchedRequest}
import me.scf37.fine.route.matcher.Matcher
import me.scf37.fine.route.typeclass.RouteHttpRequest

/**
 * Route - return request handler by request, where request handler is `Req => F[Resp]`.
 * It is generic (any request/response type, any MonadError effect), efficient (O(ln(n)) matching speed)
 * and composable.
 *
 * See RouteDsl and Route.mk methods
 *
 * @tparam F Route effect, must be MonadError[?[_], Throwable]
 * @tparam Req Route HTTP request type, must be RouteHttpRequest
 * @tparam Resp Route HTTP response type, must be RouteHttpResponse
 */
trait Route[F[_], Req, Resp] extends (RouteRequest => Option[Req => F[Resp]]) {
   /** meta information on route endpoints, used to generate docs and clients */
  def meta: RouteMeta

  /**
   * Look up handler by request
   *
   * @param req HTTP request
   * @return request handler for that request
   * @throws RouteUnmatchedException if there is no endpoint for this request
   * @throws RouteParamParseException if path/query params conversion failed
   */
  override def apply(req: RouteRequest): Option[Req => F[Resp]]

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
  def mapK[G[_]: Functor](f: F ~> G): Route[G, Req, Resp]

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
  def compose[Req2: RouteHttpRequest, Resp2](filter: (Req => F[Resp]) => (Req2 => F[Resp2])): Route[F, Req2, Resp2]

  /**
   * Compose two routes sequentially, i.e. run first route, then run second if first fails.
   *
   * It allows combining routes with conflicting paths but has worse performance
   *
   * @param r
   * @return
   */
  def andThen(r: Route[F, Req, Resp]): Route[F, Req, Resp]

  def compose0[Req2, Resp2](filter: (RouteRequest => Option[Req => F[Resp]]) => (RouteRequest => Option[Req2 => F[Resp2]])): Route[F, Req2, Resp2]

  def handleUnmatched(f: Req => F[Resp]): Route[F, Req, Resp]
}

trait CombinableRoute[F[_], Req, Resp] {
  protected[route] def matcher: Matcher[Endpoint[F, Req, Resp]]
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
  def empty[F[_]: Functor, Req: RouteHttpRequest, Resp]: Route[F, Req, Resp] =
    RouteImpl(Matcher())

  /**
   * Make route out of set of Endpoint instances
   *
   * @param endpoints endpoints
   * @tparam F Route effect
   * @tparam Req Route HTTP request type
   * @tparam Resp Route HTTP response type
   * @return route
   */
  def mk[F[_]: Functor, Req: RouteHttpRequest, Resp](endpoints: Endpoint[F, Req, Resp]*): Route[F, Req, Resp] = {
    RouteImpl(endpoints.foldLeft(Matcher[Endpoint[F, Req, Resp]]())((m, e) => m.addEndpoint(e.meta.method, e.meta.pathPattern, e)))
  }

  def mk[F[_]: Functor, Req, Resp](meta: RouteMeta)(f: RouteRequest => Option[Req => F[Resp]]): Route[F, Req, Resp] = {
    DumbRoute(meta, f)
  }

  /** CommutativeMonoid for Route */
  implicit def monoidInstance[F[_]: Functor, Req: RouteHttpRequest, Resp]: CommutativeMonoid[Route[F, Req, Resp]] = new CommutativeMonoid[Route[F, Req, Resp]] {
    override def empty: Route[F, Req, Resp] = Route.empty

    override def combine(x: Route[F, Req, Resp], y: Route[F, Req, Resp]): Route[F, Req, Resp] = {
      x.combine(y)
    }
  }

  // implementation supporting O(log(n)) matching
  private case class RouteImpl[F[_]: Functor, Req: RouteHttpRequest, Resp](
    matcher: Matcher[Endpoint[F, Req, Resp]]
  ) extends Route[F, Req, Resp] with CombinableRoute[F, Req, Resp]{

    override def meta: RouteMeta = RouteMeta(matcher.endpoints.map(_.meta))

    override def apply(req: RouteRequest): Option[Req => F[Resp]] =
      matcher.matchRequest(req.method, req.uri) match {
        case Some(mreq) =>
          Some { (req2: Req) =>
            mreq.value.handle(
              makeRequest(req2, mreq.unmatched.mkString("/"), mreq.params.toMap, mreq.value)
            )
          }

        case None => None
    }

    override def combine(r: Route[F, Req, Resp]): Route[F, Req, Resp] = r match {
      case r: CombinableRoute[F, Req, Resp] =>
        copy(matcher = matcher combine r.matcher)

      case _ => r.andThen(this)
    }

    override def map[Resp2](f: Resp => Resp2): Route[F, Req, Resp2] =
      copy(matcher = matcher.map(e => e.map(f).copy(meta = filterMeta(f, e.meta))))

    override def rmap[Req2: RouteHttpRequest](f: Req2 => Req): Route[F, Req2, Resp] =
      copy(matcher = matcher.map(e => e.rmap(f).copy(meta = filterMeta(f, e.meta))))

    override def mapK[G[_]: Functor](f: F ~> G): Route[G, Req, Resp] =
      RouteImpl(matcher.map(e => e.mapK(f).copy(meta = filterMeta(f, e.meta))))

    override def compose[Req2: RouteHttpRequest, Resp2](filter: (Req => F[Resp]) => (Req2 => F[Resp2])): Route[F, Req2, Resp2] =
      copy(matcher = matcher.map(e => e.compose(filter).copy(meta = filterMeta(filter, e.meta))))

    override def andThen(r: Route[F, Req, Resp]): Route[F, Req, Resp] = r match {
      case r: CombinableRoute[F, Req, Resp] =>
        copy(matcher = matcher andThen r.matcher)

      case _ => r.andThen(this)
    }

    override def compose0[Req2, Resp2](filter: (RouteRequest => Option[Req => F[Resp]]) => (RouteRequest => Option[Req2 => F[Resp2]])): Route[F, Req2, Resp2] = {
      Route.mk(filterRouteMeta(filter, meta))(filter(this))
    }

    override def handleUnmatched(f: Req => F[Resp]): Route[F, Req, Resp] =
      Route.mk(meta)(this).handleUnmatched(f)

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

  // implementation supporting linear search only
  private case class DumbRoute[F[_]: Functor, Req, Resp](
    meta: RouteMeta,
    handler: RouteRequest => Option[Req => F[Resp]]
  ) extends Route[F, Req, Resp] {

    override def apply(req: RouteRequest): Option[Req => F[Resp]] = handler(req)

    override def combine(r: Route[F, Req, Resp]): Route[F, Req, Resp] = andThen(r)

    override def map[Resp2](f: Resp => Resp2): Route[F, Req, Resp2] = Route.mk(filterRouteMeta(f, meta)) { req =>
      this.apply(req).map(h => req2 => h(req2).map(f))
    }

    override def rmap[Req2: RouteHttpRequest](f: Req2 => Req): Route[F, Req2, Resp] = Route.mk(filterRouteMeta(f, meta)) { req =>
      this.apply(req).map(h => req2 => h(f(req2)))
    }

    override def mapK[G[_]: Functor](f: F ~> G): Route[G, Req, Resp] = Route.mk(filterRouteMeta(f, meta)) { req =>
      this.apply(req).map(h => req2 => f(h(req2)))
    }

    override def compose[Req2: RouteHttpRequest, Resp2](filter: (Req => F[Resp]) => Req2 => F[Resp2]): Route[F, Req2, Resp2] = Route.mk(filterRouteMeta(filter, meta)) { req =>
      this.apply(req).map(filter)
    }

    override def andThen(r: Route[F, Req, Resp]): Route[F, Req, Resp] = Route.mk(meta ++ r.meta) { req =>
      this.apply(req).orElse(r.apply(req))
    }

    override def compose0[Req2, Resp2](filter: (RouteRequest => Option[Req => F[Resp]]) => (RouteRequest => Option[Req2 => F[Resp2]])): Route[F, Req2, Resp2] = {
      Route.mk(filterRouteMeta(filter, meta))(filter(this))
    }

    override def handleUnmatched(f: Req => F[Resp]): Route[F, Req, Resp] = Route.mk(meta) { req =>
      this.apply(req).orElse(Some(f))
    }
  }

  private def filterMeta(filter: Any, meta: Meta): Meta = filter match {
    case f: MetaFilter => f.filterMeta(meta)
    case _ => meta
  }

  private def filterRouteMeta(filter: Any, meta: RouteMeta): RouteMeta = filter match {
    case f: MetaFilter => meta.copy(endpointMetas = meta.endpointMetas.map(f.filterMeta))
    case _ => meta
  }

}
