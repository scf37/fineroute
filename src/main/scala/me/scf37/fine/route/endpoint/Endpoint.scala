package me.scf37.fine.route.endpoint

import cats.Applicative
import cats.MonadError
import me.scf37.fine.route.endpoint.impl.EndpointBuilder0
import me.scf37.fine.route.meta.Meta
import me.scf37.fine.route.typeclass.RouteHttpResponse

case class Endpoint[F[_], Req, Resp](
  meta: Meta,
  handle: MatchedRequest[Req] => F[Resp]
)

object Endpoint {
  def builder[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse] =
    builder2[F, Req, Resp]()

  def builder2[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse](
    onCreated: Endpoint[F, Req, Resp] => Unit = (_: Endpoint[F, Req, Resp]) => ()
  ) =
    new EndpointBuilder0[F, Req, Resp, Resp](Meta(), onCreated, Applicative[F].pure)
}


