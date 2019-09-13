package me.scf37.fine.route.endpoint

import cats.implicits._
import cats.{Applicative, Functor, MonadError, ~>}
import me.scf37.fine.route.MetaFilter
import me.scf37.fine.route.endpoint.impl.EndpointBuilder0
import me.scf37.fine.route.endpoint.meta.Meta
import me.scf37.fine.route.typeclass.RouteHttpResponse

/**
 * Route endpoint - handler plus meta information
 *
 * @param meta endpoint meta
 * @param handle endpoint handler
 * @tparam F handler effect
 * @tparam Req HTTP request type
 * @tparam Resp HTTP response type
 */
case class Endpoint[F[_], Req, Resp](
  meta: Meta,
  handle: MatchedRequest[Req] => F[Resp]
) {

  /** map endpoint response */
  def map[Resp2](f: Resp => Resp2)(implicit M: Functor[F]): Endpoint[F, Req, Resp2] =
    copy(handle = req => handle(req).map(f), meta = filterMeta(f))

  /** map endpoint request */
  def rmap[Req2](f: Req2 => Req): Endpoint[F, Req2, Resp] =
    copy(handle = req => handle(req.map(f)), meta = filterMeta(f))

  /** map endpoint effect */
  def mapK[G[_]](f: F ~> G): Endpoint[G, Req, Resp] = copy(handle = req => f(handle(req)), meta = filterMeta(f))

  /** Wrap this endpoint with filter.  */
  def compose[Req2, Resp2](filter: (Req => F[Resp]) => (Req2 => F[Resp2])): Endpoint[F, Req2, Resp2] =
    copy(handle = req => {
      filter((r: Req) => handle(req.copy(req = r))).apply(req.req)
    }, meta = filterMeta(filter))

  private def filterMeta(filter: Any): Meta = filter match {
    case f: MetaFilter => f.filterMeta(meta)
    case _ => meta
  }
}

object Endpoint {
  /** Create new endpoint builder */
  def builder[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse] =
    builder2[F, Req, Resp]((_: Endpoint[F, Req, Resp]) => ())

  /** Create new endpoint builder, with callback when builder completes */
  def builder2[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse](
    onCreated: Endpoint[F, Req, Resp] => Unit
  ) =
    new EndpointBuilder0[F, Req, Resp, Resp](Meta(), onCreated, Applicative[F].pure)
}


