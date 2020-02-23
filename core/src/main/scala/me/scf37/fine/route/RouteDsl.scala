package me.scf37.fine.route

import cats.{Functor, MonadError, ~>}
import me.scf37.fine.route.endpoint.Endpoint
import me.scf37.fine.route.matcher.Matcher
import me.scf37.fine.route.typeclass.{RouteHttpRequest, RouteHttpResponse}

trait RouteDsl[F[_], Req, Resp] extends Route[F, Req, Resp] with Route.CombinableRoute[F, Req, Resp] {
  /** MonadError instance for F, implement it
   *
   * It is important to implement it as def, not as val to avoid NPEs due to val initialization order
   */
  protected def monadError: MonadError[F, Throwable]

  /** RouteHttpRequest instance for Req, implement it
   *
   * It is important to implement it as def, not as val to avoid NPEs due to val initialization order
   */
  protected def routeHttpRequest: RouteHttpRequest[Req]

  /** RouteHttpResponse instance for Resp, implement it]
   *
   * It is important to implement it as def, not as val to avoid NPEs due to val initialization order
   */
  protected def routeHttpResponse: RouteHttpResponse[Resp]

  private implicit val monadError1: MonadError[F, Throwable] = monadError
  private implicit val routeHttpRequest1: RouteHttpRequest[Req] = routeHttpRequest
  private implicit val routeHttpResponse1: RouteHttpResponse[Resp] = routeHttpResponse

  private var route: Route[F, Req, Resp] = Route.empty

  /**
   * DLS for building routes, e.g. endpoint.get("/") {() => Future successful Response("hello")}
   */
  protected def endpoint = Endpoint.builder2[F, Req, Resp] {e =>
    route = route combine Route.mk(e)
  }

  override def meta: RouteMeta = route.meta

  override def apply(req: RouteRequest): Option[Req => F[Resp]] = route(req)

  override def combine(r: Route[F, Req, Resp]): Route[F, Req, Resp] = route.combine(r)

  override def map[Resp2](f: Resp => Resp2): Route[F, Req, Resp2] = route.map(f)

  override def rmap[Req2: RouteHttpRequest](f: Req2 => Req): Route[F, Req2, Resp] = route.rmap(f)

  override def mapK[G[_]: Functor](f: F ~> G): Route[G, Req, Resp] = route.mapK(f)

  override def compose[Req2: RouteHttpRequest, Resp2](filter: (Req => F[Resp]) => Req2 => F[Resp2]): Route[F, Req2, Resp2] = route.compose(filter)

  override def andThen(r: Route[F, Req, Resp]): Route[F, Req, Resp] = route.andThen(r)

  override def compose0[Req2, Resp2](filter: (RouteRequest => Option[Req => F[Resp]]) => RouteRequest => Option[Req2 => F[Resp2]]): Route[F, Req2, Resp2] = route.compose0(filter)

  override protected[route] def matcher: Matcher[Endpoint[F, Req, Resp]] = route.asInstanceOf[Route.CombinableRoute[F, Req, Resp]].matcher
}
