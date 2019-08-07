package me.scf37.fine.route

import cats.Applicative
import cats.MonadError
import me.scf37.fine.route.impl.RouteBuilder0
import me.scf37.fine.route.meta.Meta
import me.scf37.fine.route.typeclass.RouteHttpResponse

case class Route[F[_], Req, Resp](
  meta: Meta,
  handle: MatchedRequest[Req] => F[Resp]
)

object Route {
  def builder[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse] =
    new RouteBuilder0[F, Req, Resp, Resp](Meta(), Applicative[F].pure)
}


