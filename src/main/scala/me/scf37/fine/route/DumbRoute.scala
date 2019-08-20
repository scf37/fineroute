package me.scf37.fine.route

import cats.implicits._
import cats.{Monad, MonadError, ~>}

/**
 * Dumb route, cannot be combined or composed, but supports sequential andThen
 *
 * @tparam F
 * @tparam Req
 * @tparam Resp
 */
trait DumbRoute[F[_], Req, Resp] extends (Req => F[Req => F[Resp]]) {
  /**
   * Look up handler by request
   *
   * @param req HTTP request
   * @return request handler for that request
   * @throws RouteUnmatchedException if there is no endpoint for this request
   * @throws RouteParamParseException if path/query params conversion failed
   */
  override def apply(req: Req): F[Req => F[Resp]]

  /** meta information on route endpoints, used to generate docs and clients */
  def meta: RouteMeta

  /**
   * Compose two routes sequentially, i.e. run first route, then run second if first fails.
   *
   * It allows combining routes with conflicting paths but has worse performance
   *
   * @param r
   * @return
   */
  def andThen(r: DumbRoute[F, Req, Resp])(implicit M: MonadError[F, Throwable]): DumbRoute[F, Req, Resp] =
    DumbRoute.mk(meta combine r.meta) { req =>
      this(req).recoverWith {
        case _: RouteException => r(req)
      }
    }

  /**
   * Map route response
   *
   * @param f mapping function
   * @tparam Resp2 new response type
   * @return new route with the same endpoints but different response type
   */
  def map[Resp2](f: Resp => Resp2)(implicit M: Monad[F]): DumbRoute[F, Req, Resp2] = {
    DumbRoute.mk(meta)(req => this(req).map(h => (req2 => h(req2).map(f))))
  }

  /**
   * Map route request
   *
   * @param f mapping function
   * @tparam Req2 new request type
   * @return new route with the same endpoints but different request type
   */
  def local[Req2](f: Req2 => Req)(implicit M: Monad[F]): DumbRoute[F, Req2, Resp] = {
    DumbRoute.mk(meta)(req => this(f(req)).map(h => req2 => h(f(req2))))
  }

  /**
   * Map route effect, e.g. from Route[Either] to Route[Future]
   * @param f effect mapping function
   * @tparam G new route effect
   * @return new route with the same endpoints but different effect type
   */
  def mapK[G[_]: MonadError[?[_], Throwable]](f: F ~> G)(implicit M: Monad[F]): DumbRoute[G, Req, Resp] =
    DumbRoute.mk(meta)(req => f(this(req).map(h => req2 => f(h(req2)))))

  /**
   * Wrap this route with filter. Filter is executed after matching but before endpoint handler
   *
   * Filter is a function that takes handler and produces another handler.
   *
   * @param filter filter
   * @tparam Resp2 new response type
   * @return route that wraps endpoint handlers with this filter
   */
  def compose[Resp2](filter: (Req => F[Resp]) => (Req => F[Resp2]))(implicit M: Monad[F]): DumbRoute[F, Req, Resp2] =
    DumbRoute.mk[F, Req, Resp2](meta) { req =>
      this(req).map(filter)
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
  def mk[F[_], Req, Resp](meta: RouteMeta)(f: Req => F[Req => F[Resp]]): DumbRoute[F, Req, Resp] = {
    val meta0 = meta
    new DumbRoute[F, Req, Resp] {
      override def apply(req: Req): F[Req => F[Resp]] = f(req)

      /** meta information on route endpoints, used to generate docs and clients */
      override def meta: RouteMeta = meta0
    }
  }
}
