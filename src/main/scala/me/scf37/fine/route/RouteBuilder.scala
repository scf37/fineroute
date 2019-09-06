package me.scf37.fine.route

import cats.MonadError
import me.scf37.fine.route.endpoint.Endpoint
import me.scf37.fine.route.typeclass.{RouteHttpRequest, RouteHttpResponse}

import scala.collection.mutable

trait RouteBuilder[F[_], Req, Resp] {
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

  private val endpoints: mutable.Buffer[Endpoint[F, Req, Resp]] = mutable.Buffer.empty

  /**
   * DLS for building routes, e.g. endpoint.get("/") {() => Future successful Response("hello")}
   */
  protected def endpoint = Endpoint.builder2[F, Req, Resp] {e =>
    endpoints += e
  }

  def build(): Route[F, Req, Resp] = Route.mk(endpoints: _*)
}
