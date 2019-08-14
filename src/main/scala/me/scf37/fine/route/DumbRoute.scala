package me.scf37.fine.route

import cats.MonadError
import cats.implicits._
import me.scf37.fine.route.meta.Meta

/**
 * Dumb route, cannot be combined or composed, but supports sequential andThen
 *
 * @tparam F
 * @tparam Req
 * @tparam Resp
 */
trait DumbRoute[F[_], Req, Resp] extends (Req => F[() => F[Resp]]) {
  /**
   * Look up handler by request
   *
   * @param req HTTP request
   * @return request handler for that request
   * @throws RouteUnmatchedException if there is no endpoint for this request
   * @throws RouteParamParseException if path/query params conversion failed
   */
  override def apply(req: Req): F[() => F[Resp]]

  /** meta information on route endpoints, used to generate docs and clients */
  def meta: Seq[Meta]

  /**
   * Compose two routes sequentially, i.e. run first route, then run second if first fails.
   *
   * It allows combining routes with conflicting paths but has worse performance
   *
   * @param r
   * @return
   */
  def andThen(r: DumbRoute[F, Req, Resp])(implicit M: MonadError[F, Throwable]): DumbRoute[F, Req, Resp] =
    DumbRoute.mk(meta ++ r.meta) { req =>
      this(req).orElse(r(req))
    }

}

object DumbRoute {
  /**
   * Create DumbRoute out of working function and meta
   * @param meta route meta
   * @param f route working function
   * @tparam F effect
   * @tparam Req HTTP request type
   * @tparam Resp HTTP response type
   * @return
   */
  def mk[F[_], Req, Resp](meta: Seq[Meta])(f: Req => F[() => F[Resp]]): DumbRoute[F, Req, Resp] = {
    val meta0 = meta
    new DumbRoute[F, Req, Resp] {
      override def apply(req: Req): F[() => F[Resp]] = f(req)

      /** meta information on route endpoints, used to generate docs and clients */
      override def meta: Seq[Meta] = meta0
    }
  }
}
