package me.scf37.fine.route.endpoint.impl

import cats.MonadError
import me.scf37.fine.route.meta.Meta
import cats.implicits._
import me.scf37.fine.route.endpoint.Endpoint
import me.scf37.fine.route.endpoint.EndpointBuilder
import me.scf37.fine.route.endpoint.MatchedRequest
import me.scf37.fine.route.typeclass.RouteHttpResponse

class EndpointBuilder0[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse, Produces](
  protected val meta: Meta,
  protected val onEndpointCreated: Endpoint[F, Req, Resp] => Unit,
  private val handler: Produces => F[Resp]
)
  extends EndpointBuilderImpl[F, Req, Resp, Produces,
    () => F[Produces],
    EndpointBuilder1[F, Req, Resp, Produces, ?],
    EndpointBuilder0[F, Req, Resp, ?]
  ] {
  override protected def monadError: MonadError[F, Throwable] = implicitly

  override protected def respT: RouteHttpResponse[Resp] = implicitly

  override protected def self(m: Meta => Meta): Self =
    new EndpointBuilder0[F, Req, Resp, Produces](m(meta), onEndpointCreated, handler)

  override protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): EndpointBuilder1[F, Req, Resp, Produces, T] =
    new EndpointBuilder1[F, Req, Resp, Produces, T](m(meta), onEndpointCreated, handler, value)

  override protected def resp[T](m: Meta => Meta, value: T => F[Resp]): EndpointBuilder0[F, Req, Resp, T] =
    new EndpointBuilder0[F, Req, Resp, T](m(meta), onEndpointCreated, value)

  override protected def makeHandler(handler: () => F[Produces]): MatchedRequest[Req] => F[Resp] =
    _ => handler().flatMap(this.handler)


}

class EndpointBuilder1[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse, Produces, P1](
  protected val meta: Meta,
  protected val onEndpointCreated: Endpoint[F, Req, Resp] => Unit,
  private val handler: Produces => F[Resp],
  private val param1: MatchedRequest[Req] => F[P1]
)
  extends EndpointBuilderImpl[F, Req, Resp, Produces,
    P1 => F[Produces],
    EndpointBuilder2[F, Req, Resp, Produces, P1, ?],
    EndpointBuilder1[F, Req, Resp, ?, P1]
  ] {
  override protected def monadError: MonadError[F, Throwable] = implicitly

  override protected def respT: RouteHttpResponse[Resp] = implicitly

  override protected def self(m: Meta => Meta): Self =
    new EndpointBuilder1[F, Req, Resp, Produces, P1](m(meta), onEndpointCreated, handler, param1)

  override protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): EndpointBuilder2[F, Req, Resp, Produces, P1, T] =
    new EndpointBuilder2[F, Req, Resp, Produces, P1, T](m(meta), onEndpointCreated, handler, param1, value)

  override protected def resp[T](m: Meta => Meta, value: T => F[Resp]): EndpointBuilder1[F, Req, Resp, T, P1] =
    new EndpointBuilder1[F, Req, Resp, T, P1](m(meta), onEndpointCreated, value, param1)

  override protected def makeHandler(handler: (P1) => F[Produces]): MatchedRequest[Req] => F[Resp] =
    req =>
      param1(req).flatMap { p1 =>
        handler(p1).flatMap(this.handler)
      }

}

class EndpointBuilder2[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse, Produces, P1, P2](
  protected val meta: Meta,
  protected val onEndpointCreated: Endpoint[F, Req, Resp] => Unit,
  private val handler: Produces => F[Resp],
  private val param1: MatchedRequest[Req] => F[P1],
  private val param2: MatchedRequest[Req] => F[P2]
)
  extends EndpointBuilderImpl[F, Req, Resp, Produces,
    (P1, P2) => F[Produces],
    EndpointBuilder3[F, Req, Resp, Produces, P1, P2, ?],
    EndpointBuilder2[F, Req, Resp, ?, P1, P2]
  ] {
  override protected def monadError: MonadError[F, Throwable] = implicitly

  override protected def respT: RouteHttpResponse[Resp] = implicitly

  override protected def self(m: Meta => Meta): Self =
    new EndpointBuilder2[F, Req, Resp, Produces, P1, P2](m(meta), onEndpointCreated, handler, param1, param2)

  override protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): EndpointBuilder3[F, Req, Resp, Produces, P1, P2, T] =
    new EndpointBuilder3[F, Req, Resp, Produces, P1, P2, T](m(meta), onEndpointCreated, handler, param1, param2, value)

  override protected def resp[T](m: Meta => Meta, value: T => F[Resp]): EndpointBuilder2[F, Req, Resp, T, P1, P2] =
    new EndpointBuilder2[F, Req, Resp, T, P1, P2](m(meta), onEndpointCreated, value, param1, param2)

  override protected def makeHandler(handler: (P1, P2) => F[Produces]): MatchedRequest[Req] => F[Resp] =
    req =>
      param1(req).flatMap { p1 =>
        param2(req).flatMap { p2 =>
          handler(p1, p2).flatMap(this.handler)
        }
      }
}

class EndpointBuilder3[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse, Produces, P1, P2, P3](
  protected val meta: Meta,
  protected val onEndpointCreated: Endpoint[F, Req, Resp] => Unit,
  private val handler: Produces => F[Resp],
  private val param1: MatchedRequest[Req] => F[P1],
  private val param2: MatchedRequest[Req] => F[P2],
  private val param3: MatchedRequest[Req] => F[P3]
)
  extends EndpointBuilderImpl[F, Req, Resp, Produces,
    (P1, P2, P3) => F[Produces],
    EndpointBuilder4[F, Req, Resp, Produces, P1, P2, P3, ?],
    EndpointBuilder3[F, Req, Resp, ?, P1, P2, P3]
  ] {
  override protected def monadError: MonadError[F, Throwable] = implicitly

  override protected def respT: RouteHttpResponse[Resp] = implicitly

  override protected def self(m: Meta => Meta): Self =
    new EndpointBuilder3[F, Req, Resp, Produces, P1, P2, P3](m(meta), onEndpointCreated, handler, param1, param2, param3)

  override protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): EndpointBuilder4[F, Req, Resp, Produces, P1, P2, P3, T] =
    new EndpointBuilder4[F, Req, Resp, Produces, P1, P2, P3, T](m(meta), onEndpointCreated, handler, param1, param2, param3, value)

  override protected def resp[T](m: Meta => Meta, value: T => F[Resp]): EndpointBuilder3[F, Req, Resp, T, P1, P2, P3] =
    new EndpointBuilder3[F, Req, Resp, T, P1, P2, P3](meta, onEndpointCreated, value, param1, param2, param3)

  override protected def makeHandler(handler: (P1, P2, P3) => F[Produces]): MatchedRequest[Req] => F[Resp] =
    req =>
      param1(req).flatMap { p1 =>
        param2(req).flatMap { p2 =>
          param3(req).flatMap { p3 =>
            handler(p1, p2, p3).flatMap(this.handler)
          }
        }
      }
}

class EndpointBuilder4[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse, Produces, P1, P2, P3, P4](
  protected val meta: Meta,
  protected val onEndpointCreated: Endpoint[F, Req, Resp] => Unit,
  private val handler: Produces => F[Resp],
  private val param1: MatchedRequest[Req] => F[P1],
  private val param2: MatchedRequest[Req] => F[P2],
  private val param3: MatchedRequest[Req] => F[P3],
  private val param4: MatchedRequest[Req] => F[P4]
)
  extends EndpointBuilderImpl[F, Req, Resp, Produces,
    (P1, P2, P3, P4) => F[Produces],
    EndpointBuilder5[F, Req, Resp, Produces, P1, P2, P3, P4, ?],
    EndpointBuilder4[F, Req, Resp, ?, P1, P2, P3, P4]
  ] {
  override protected def monadError: MonadError[F, Throwable] = implicitly

  override protected def respT: RouteHttpResponse[Resp] = implicitly

  override protected def self(m: Meta => Meta): Self =
    new EndpointBuilder4[F, Req, Resp, Produces, P1, P2, P3, P4](m(meta), onEndpointCreated, handler, param1, param2, param3, param4)

  override protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): EndpointBuilder5[F, Req, Resp, Produces, P1, P2, P3, P4, T] =
    new EndpointBuilder5[F, Req, Resp, Produces, P1, P2, P3, P4, T](m(meta), onEndpointCreated, handler, param1, param2, param3, param4, value)

  override protected def resp[T](m: Meta => Meta, value: T => F[Resp]): EndpointBuilder4[F, Req, Resp, T, P1, P2, P3, P4] =
    new EndpointBuilder4[F, Req, Resp, T, P1, P2, P3, P4](m(meta), onEndpointCreated, value, param1, param2, param3, param4)

  override protected def makeHandler(handler: (P1, P2, P3, P4) => F[Produces]): MatchedRequest[Req] => F[Resp] =
    req =>
      param1(req).flatMap { p1 =>
        param2(req).flatMap { p2 =>
          param3(req).flatMap { p3 =>
            param4(req).flatMap { p4 =>
              handler(p1, p2, p3, p4).flatMap(this.handler)
            }
          }
        }
      }
}

class EndpointBuilder5[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse, Produces, P1, P2, P3, P4, P5](
  protected val meta: Meta,
  protected val onEndpointCreated: Endpoint[F, Req, Resp] => Unit,
  private val handler: Produces => F[Resp],
  private val param1: MatchedRequest[Req] => F[P1],
  private val param2: MatchedRequest[Req] => F[P2],
  private val param3: MatchedRequest[Req] => F[P3],
  private val param4: MatchedRequest[Req] => F[P4],
  private val param5: MatchedRequest[Req] => F[P5]
)
  extends EndpointBuilderImpl[F, Req, Resp, Produces,
    (P1, P2, P3, P4, P5) => F[Produces],
    EndpointBuilder6[F, Req, Resp, Produces, P1, P2, P3, P4, P5, ?],
    EndpointBuilder5[F, Req, Resp, ?, P1, P2, P3, P4, P5]
  ] {
  override protected def monadError: MonadError[F, Throwable] = implicitly

  override protected def respT: RouteHttpResponse[Resp] = implicitly

  override protected def self(m: Meta => Meta): Self =
    new EndpointBuilder5[F, Req, Resp, Produces, P1, P2, P3, P4, P5](m(meta), onEndpointCreated, handler, param1, param2, param3, param4, param5)

  override protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): EndpointBuilder6[F, Req, Resp, Produces, P1, P2, P3, P4, P5, T] =
    ???

  override protected def resp[T](m: Meta => Meta, value: T => F[Resp]): EndpointBuilder5[F, Req, Resp, T, P1, P2, P3, P4, P5] =
    new EndpointBuilder5[F, Req, Resp, T, P1, P2, P3, P4, P5](m(meta), onEndpointCreated, value, param1, param2, param3, param4, param5)

  override protected def makeHandler(handler: (P1, P2, P3, P4, P5) => F[Produces]): MatchedRequest[Req] => F[Resp] =
    req =>
      param1(req).flatMap { p1 =>
        param2(req).flatMap { p2 =>
          param3(req).flatMap { p3 =>
            param4(req).flatMap { p4 =>
              param5(req).flatMap { p5 =>
                handler(p1, p2, p3, p4, p5).flatMap(this.handler)
              }
            }
          }
        }
      }
}

trait EndpointBuilder6[F[_], Req, Resp, Produces, P1, P2, P3, P4, P5, P6]
  extends EndpointBuilder[F, Req, Resp, Produces,
    (P1, P2, P3, P4, P5, P6) => F[Resp],
    EndOfTheLine[?],
    EndOfTheLine[?]
  ]


sealed trait EndOfTheLine[_]