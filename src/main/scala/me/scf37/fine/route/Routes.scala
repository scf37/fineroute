package me.scf37.fine.route

import cats.MonadError
import cats.implicits._
import cats.~>

/**
 * Teh route - get request handler by request.
 *
 *
 *
 * @tparam F
 * @tparam Req
 * @tparam Resp
 */
trait Routes[F[_], Req, Resp] extends (Req => F[Req => F[Resp]]) {
  self =>

  protected implicit val monadErrorInstance: MonadError[F, Throwable]

  def andThen(route: Routes[F, Req, Resp]): Routes[F, Req, Resp] = Routes.mk { req =>
    self(req).orElse(route(req))
  }

  // wrap route within this Filter
  def compose[Resp2](filter: (Req => F[Resp]) => (Req => F[Resp2])): Routes[F, Req, Resp2] = Routes.mk { req =>
    this(req).map { handler =>
      filter(handler)
    }
  }

  def local[Req2](f: Req2 => Req): Routes[F, Req2, Resp] = Routes.mk { req =>
    this(f(req)).map { handler =>
      req => handler(f(req))
    }
  }

  def map[Resp2](f: Resp => Resp2): Routes[F, Req, Resp2] = Routes.mk { req =>
    this(req).map { handler =>
      req => handler(req).map(f)
    }
  }

  def mapK[G[_]: MonadError[?[_], Throwable]](f: F ~> G): Routes[G, Req, Resp] = Routes.mk { req =>
    f(this(req)).map { handler =>
      (req: Req) => f(handler(req))
    }
  }

}

object Routes {

  def mk[F[_]: MonadError[?[_], Throwable], Req, Resp](f: Req => F[Req => F[Resp]]): Routes[F, Req, Resp] = {
    val me = implicitly[MonadError[F, Throwable]]

    new Routes[F, Req, Resp] {
      override def apply(req: Req): F[Req => F[Resp]] = f(req)

      override protected implicit val monadErrorInstance: MonadError[F, Throwable] = me
    }
  }
}
