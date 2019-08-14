package me.scf37.fine.route

import cats.MonadError
import cats.implicits._
import cats.kernel.CommutativeMonoid
import cats.kernel.Monoid
import cats.~>
import me.scf37.fine.route.endpoint.Endpoint
import me.scf37.fine.route.matcher.Matcher
import me.scf37.fine.route.meta.Meta
import me.scf37.fine.route.typeclass.RouteHttpRequest
import me.scf37.fine.route.typeclass.RouteHttpResponse

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
trait Route[F[_], Req, Resp] extends DumbRoute[F, Req, Resp] {
  /** MonadError instance for F, implement it */
  protected def monadError: MonadError[F, Throwable]

  /** RouteHttpRequest instance for Req, implement it */
  protected def routeHttpRequest: RouteHttpRequest[Req]

  /** RouteHttpResponse instance for Resp, implement it */
  protected def routeHttpResponse: RouteHttpResponse[Resp]

  private implicit val monadError1: MonadError[F, Throwable] = monadError
  private implicit val routeHttpRequest1: RouteHttpRequest[Req] = routeHttpRequest
  private implicit val routeHttpResponse1: RouteHttpResponse[Resp] = routeHttpResponse

  @volatile
  private var matcher: Matcher[F, Req, Resp] = Matcher[F, Req, Resp]()

  /** meta information on route endpoints, used to generate docs and clients */
  override def meta: Seq[Meta] = matcher.endpoints.map(_.meta)

  /**
   * Look up handler by request
   *
   * @param req HTTP request
   * @return request handler for that request
   * @throws RouteUnmatchedException if there is no endpoint for this request
   * @throws RouteParamParseException if path/query params conversion failed
   */
  override def apply(req: Req): F[() => F[Resp]] = {
    matcher.matchRequest(req).map {
      case (req, endpoint) => () => endpoint.handle(req)
    }
  }

  /**
   * DLS for building routes, e.g. endpoint.get("/") {() => Future successful Response("hello")}
   */
  protected def endpoint = Endpoint.builder2[F, Req, Resp] {e =>
    addEndpoint(e)
  }

  /**
   * Combine two routes
   *
   * @param r another route
   * @return new route containing endpoints from both routes
   */
  def combine(r: Route[F, Req, Resp]): Route[F, Req, Resp] = Monoid[Route[F, Req, Resp]].combine(this, r)

  /**
   * Map route response
   *
   * @param f mapping function
   * @tparam Resp2 new response type
   * @return new route with the same endpoints but different response type
   */
  def map[Resp2: RouteHttpResponse](f: Resp => Resp2): Route[F, Req, Resp2] = {
    Route.mk(matcher.endpoints.map(e => e.map(f)): _*)
  }

  /**
   * Map route request
   *
   * @param f mapping function
   * @tparam Req2 new request type
   * @return new route with the same endpoints but different request type
   */
  def local[Req2: RouteHttpRequest](f: Req2 => Req): Route[F, Req2, Resp] = {
    Route.mk(matcher.endpoints.map(e => e.local(f)): _*)
  }

  /**
   * Map route effect, e.g. from Route[Either] to Route[Future]
   * @param f effect mapping function
   * @tparam G new route effect
   * @return new route with the same endpoints but different effect type
   */
  def mapK[G[_]: MonadError[?[_], Throwable]](f: F ~> G): Route[G, Req, Resp] =
    Route.mk(matcher.endpoints.map(e => e.mapK(f)): _*)

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
  def compose[Req2: RouteHttpRequest, Resp2: RouteHttpResponse](filter: (Req => F[Resp]) => (Req2 => F[Resp2])) =
    Route.mk(matcher.endpoints.map(e => e.compose(filter)): _*)

  private def addEndpoint(e: Endpoint[F, Req, Resp]): Unit = {
    matcher = matcher.addEndpoint(e)
  }

  private def addMatcher(m: Matcher[F, Req, Resp]): Unit = {
    matcher = matcher.addMatcher(m)
  }

}

object Route {
  private case class RouteImpl[F[_], Req, Resp](
  monadError: MonadError[F, Throwable],
  routeHttpRequest: RouteHttpRequest[Req],
  routeHttpResponse: RouteHttpResponse[Resp]
  ) extends Route[F, Req, Resp]

  /**
   * Empty route
   *
   * @tparam F Route effect
   * @tparam Req Route HTTP request type
   * @tparam Resp Route HTTP response type
   * @return empty route
   */
  def empty[F[_]: MonadError[?[_], Throwable], Req: RouteHttpRequest, Resp: RouteHttpResponse]: Route[F, Req, Resp] =
    RouteImpl(implicitly, implicitly, implicitly)

  /**
   * Make route out of set of Endpoint instances
   *
   * @param endpoints endpoints
   * @tparam F Route effect
   * @tparam Req Route HTTP request type
   * @tparam Resp Route HTTP response type
   * @return route
   */
  def mk[F[_]: MonadError[?[_], Throwable], Req: RouteHttpRequest, Resp: RouteHttpResponse](endpoints: Endpoint[F, Req, Resp]*): Route[F, Req, Resp] = {
    val r = empty[F, Req, Resp]
    endpoints.foreach(r.addEndpoint)
    r
  }

  /** CommutativeMonoid for Route */
  implicit def monoidInstance[F[_]: MonadError[?[_], Throwable], Req: RouteHttpRequest, Resp: RouteHttpResponse]: CommutativeMonoid[Route[F, Req, Resp]] = new CommutativeMonoid[Route[F, Req, Resp]] {
    override def empty: Route[F, Req, Resp] = Route.empty

    override def combine(x: Route[F, Req, Resp], y: Route[F, Req, Resp]): Route[F, Req, Resp] = {
      val r = empty
      r.addMatcher(x.matcher)
      r.addMatcher(y.matcher)
      r
    }
  }
}
