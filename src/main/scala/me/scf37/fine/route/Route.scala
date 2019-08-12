package me.scf37.fine.route

import cats.MonadError
import cats.implicits._
import me.scf37.fine.route.endpoint.Endpoint
import me.scf37.fine.route.matcher.Matcher
import me.scf37.fine.route.typeclass.RouteHttpRequest
import me.scf37.fine.route.typeclass.RouteHttpResponse

trait Route[F[_], Req, Resp] extends (Req => F[() => F[Resp]]) {
  private var matcher: Matcher[F, Req, Resp] = Matcher[F, Req, Resp]()
  protected implicit val monadError: MonadError[F, Throwable]
  protected implicit val routeHttpRequest: RouteHttpRequest[Req]
  protected implicit val routeHttpResponse: RouteHttpResponse[Resp]

  override def apply(req: Req): F[() => F[Resp]] = {
    matcher.matchRequest(req).map {
      case (req, endpoint) => () => endpoint.handle(req)
    }
  }

  protected def endpoint = Endpoint.builder2[F, Req, Resp](e => matcher = matcher.addEndpoint(e))

}

object Route {
  def mk[F[_], Req, Resp](endpoints: Endpoint[F, Req, Resp]*): Route[F, Req, Resp] = ???
}
