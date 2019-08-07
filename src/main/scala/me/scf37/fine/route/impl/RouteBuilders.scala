package me.scf37.fine.route.impl

import cats.MonadError
import me.scf37.fine.route.MatchedRequest
import me.scf37.fine.route.RouteBuilder
import me.scf37.fine.route.meta.Meta
import cats.implicits._
import me.scf37.fine.route.typeclass.RouteHttpResponse

class RouteBuilder0[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse, Produces](protected val meta: Meta, private val r: Produces => F[Resp])
  extends RouteBuilderImpl[F, Req, Resp, Produces,
    () => F[Produces],
    RouteBuilder1[F, Req, Resp, Produces, ?],
    RouteBuilder0[F, Req, Resp, ?]
  ] {
  override protected def f: MonadError[F, Throwable] = implicitly

  override protected def respT: RouteHttpResponse[Resp] = implicitly

  override protected def self(m: Meta => Meta): Self = new RouteBuilder0[F, Req, Resp, Produces](m(meta), r)

  override protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): RouteBuilder1[F, Req, Resp, Produces, T] =
    new RouteBuilder1[F, Req, Resp, Produces, T](m(meta), r, value)

  override protected def resp[T](m: Meta => Meta, value: T => F[Resp]): RouteBuilder0[F, Req, Resp, T] =
    new RouteBuilder0[F, Req, Resp, T](m(meta), value)

  override protected def makeHandler(handler: () => F[Produces]): MatchedRequest[Req] => F[Resp] =
    _ => handler().flatMap(r)


}

class RouteBuilder1[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse, Produces, P1](
  protected val meta: Meta, private val r: Produces => F[Resp], private val param1: MatchedRequest[Req] => F[P1]
)
  extends RouteBuilderImpl[F, Req, Resp, Produces,
    P1 => F[Produces],
    RouteBuilder2[F, Req, Resp, Produces, P1, ?],
    RouteBuilder1[F, Req, Resp, ?, P1]
  ] {
  override protected def f: MonadError[F, Throwable] = implicitly

  override protected def respT: RouteHttpResponse[Resp] = implicitly

  override protected def self(m: Meta => Meta): Self = new RouteBuilder1[F, Req, Resp, Produces, P1](m(meta), r, param1)

  override protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): RouteBuilder2[F, Req, Resp, Produces, P1, T] =
    new RouteBuilder2[F, Req, Resp, Produces, P1, T](m(meta), r, param1, value)

  override protected def resp[T](m: Meta => Meta, value: T => F[Resp]): RouteBuilder1[F, Req, Resp, T, P1] =
    new RouteBuilder1[F, Req, Resp, T, P1](m(meta), value, param1)

  override protected def makeHandler(handler: (P1) => F[Produces]): MatchedRequest[Req] => F[Resp] =
    req =>
      param1(req).flatMap { p1 =>
        handler(p1).flatMap(r)
      }

}

class RouteBuilder2[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse, Produces, P1, P2](
  protected val meta: Meta, private val r: Produces => F[Resp], private val param1: MatchedRequest[Req] => F[P1], private val param2: MatchedRequest[Req] => F[P2]
)
  extends RouteBuilderImpl[F, Req, Resp, Produces,
    (P1, P2) => F[Produces],
    RouteBuilder3[F, Req, Resp, Produces, P1, P2, ?],
    RouteBuilder2[F, Req, Resp, ?, P1, P2]
  ] {
  override protected def f: MonadError[F, Throwable] = implicitly

  override protected def respT: RouteHttpResponse[Resp] = implicitly

  override protected def self(m: Meta => Meta): Self = new RouteBuilder2[F, Req, Resp, Produces, P1, P2](m(meta), r, param1, param2)

  override protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): RouteBuilder3[F, Req, Resp, Produces, P1, P2, T] =
    new RouteBuilder3[F, Req, Resp, Produces, P1, P2, T](m(meta), r, param1, param2, value)

  override protected def resp[T](m: Meta => Meta, value: T => F[Resp]): RouteBuilder2[F, Req, Resp, T, P1, P2] =
    new RouteBuilder2[F, Req, Resp, T, P1, P2](m(meta), value, param1, param2)

  override protected def makeHandler(handler: (P1, P2) => F[Produces]): MatchedRequest[Req] => F[Resp] =
    req =>
      param1(req).flatMap { p1 =>
        param2(req).flatMap { p2 =>
          handler(p1, p2).flatMap(r)
        }
      }
}

class RouteBuilder3[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse, Produces, P1, P2, P3](
  protected val meta: Meta, private val r: Produces => F[Resp], private val param1: MatchedRequest[Req] => F[P1], private val param2: MatchedRequest[Req] => F[P2], private val param3: MatchedRequest[Req] => F[P3]
)
  extends RouteBuilderImpl[F, Req, Resp, Produces,
    (P1, P2, P3) => F[Produces],
    RouteBuilder4[F, Req, Resp, Produces, P1, P2, P3, ?],
    RouteBuilder3[F, Req, Resp, ?, P1, P2, P3]
  ] {
  override protected def f: MonadError[F, Throwable] = implicitly

  override protected def respT: RouteHttpResponse[Resp] = implicitly

  override protected def self(m: Meta => Meta): Self = new RouteBuilder3[F, Req, Resp, Produces, P1, P2, P3](m(meta), r, param1, param2, param3)

  override protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): RouteBuilder4[F, Req, Resp, Produces, P1, P2, P3, T] =
    new RouteBuilder4[F, Req, Resp, Produces, P1, P2, P3, T](m(meta), r, param1, param2, param3, value)

  override protected def resp[T](m: Meta => Meta, value: T => F[Resp]): RouteBuilder3[F, Req, Resp, T, P1, P2, P3] =
    new RouteBuilder3[F, Req, Resp, T, P1, P2, P3](meta, value, param1, param2, param3)

  override protected def makeHandler(handler: (P1, P2, P3) => F[Produces]): MatchedRequest[Req] => F[Resp] =
    req =>
      param1(req).flatMap { p1 =>
        param2(req).flatMap { p2 =>
          param3(req).flatMap { p3 =>
            handler(p1, p2, p3).flatMap(r)
          }
        }
      }
}

class RouteBuilder4[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse, Produces, P1, P2, P3, P4](
  protected val meta: Meta, private val r: Produces => F[Resp], private val param1: MatchedRequest[Req] => F[P1], private val param2: MatchedRequest[Req] => F[P2], private val param3: MatchedRequest[Req] => F[P3], private val param4: MatchedRequest[Req] => F[P4]
)
  extends RouteBuilderImpl[F, Req, Resp, Produces,
    (P1, P2, P3, P4) => F[Produces],
    RouteBuilder5[F, Req, Resp, Produces, P1, P2, P3, P4, ?],
    RouteBuilder4[F, Req, Resp, ?, P1, P2, P3, P4]
  ] {
  override protected def f: MonadError[F, Throwable] = implicitly

  override protected def respT: RouteHttpResponse[Resp] = implicitly

  override protected def self(m: Meta => Meta): Self = new RouteBuilder4[F, Req, Resp, Produces, P1, P2, P3, P4](m(meta), r, param1, param2, param3, param4)

  override protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): RouteBuilder5[F, Req, Resp, Produces, P1, P2, P3, P4, T] =
    new RouteBuilder5[F, Req, Resp, Produces, P1, P2, P3, P4, T](m(meta), r, param1, param2, param3, param4, value)

  override protected def resp[T](m: Meta => Meta, value: T => F[Resp]): RouteBuilder4[F, Req, Resp, T, P1, P2, P3, P4] =
    new RouteBuilder4[F, Req, Resp, T, P1, P2, P3, P4](m(meta), value, param1, param2, param3, param4)

  override protected def makeHandler(handler: (P1, P2, P3, P4) => F[Produces]): MatchedRequest[Req] => F[Resp] =
    req =>
      param1(req).flatMap { p1 =>
        param2(req).flatMap { p2 =>
          param3(req).flatMap { p3 =>
            param4(req).flatMap { p4 =>
              handler(p1, p2, p3, p4).flatMap(r)
            }
          }
        }
      }
}

class RouteBuilder5[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse, Produces, P1, P2, P3, P4, P5](
  protected val meta: Meta, private val r: Produces => F[Resp], private val param1: MatchedRequest[Req] => F[P1], private val param2: MatchedRequest[Req] => F[P2], private val param3: MatchedRequest[Req] => F[P3], private val param4: MatchedRequest[Req] => F[P4], private val param5: MatchedRequest[Req] => F[P5]
)
  extends RouteBuilderImpl[F, Req, Resp, Produces,
    (P1, P2, P3, P4, P5) => F[Produces],
    RouteBuilder6[F, Req, Resp, Produces, P1, P2, P3, P4, P5, ?],
    RouteBuilder5[F, Req, Resp, ?, P1, P2, P3, P4, P5]
  ] {
  override protected def f: MonadError[F, Throwable] = implicitly

  override protected def respT: RouteHttpResponse[Resp] = implicitly

  override protected def self(m: Meta => Meta): Self = new RouteBuilder5[F, Req, Resp, Produces, P1, P2, P3, P4, P5](m(meta), r, param1, param2, param3, param4, param5)

  override protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): RouteBuilder6[F, Req, Resp, Produces, P1, P2, P3, P4, P5, T] =
    ???

  override protected def resp[T](m: Meta => Meta, value: T => F[Resp]): RouteBuilder5[F, Req, Resp, T, P1, P2, P3, P4, P5] =
    new RouteBuilder5[F, Req, Resp, T, P1, P2, P3, P4, P5](m(meta), value, param1, param2, param3, param4, param5)

  override protected def makeHandler(handler: (P1, P2, P3, P4, P5) => F[Produces]): MatchedRequest[Req] => F[Resp] =
    req =>
      param1(req).flatMap { p1 =>
        param2(req).flatMap { p2 =>
          param3(req).flatMap { p3 =>
            param4(req).flatMap { p4 =>
              param5(req).flatMap { p5 =>
                handler(p1, p2, p3, p4, p5).flatMap(r)
              }
            }
          }
        }
      }
}

trait RouteBuilder6[F[_], Req, Resp, Produces, P1, P2, P3, P4, P5, P6]
  extends RouteBuilder[F, Req, Resp, Produces,
    (P1, P2, P3, P4, P5, P6) => F[Resp],
    EndOfTheLine[?],
    EndOfTheLine[?]
  ]


sealed trait EndOfTheLine[_]