package me.scf37.fine.route.endpoint.impl

import cats.MonadError
import cats.implicits._
import me.scf37.fine.route.endpoint.Endpoint
import me.scf37.fine.route.endpoint.MatchedRequest
import me.scf37.fine.route.meta.Meta
import me.scf37.fine.route.typeclass.RouteHttpResponse

/** Endpoint builder for handler with 0 arguments. See EndpointBuilder for builder API */
class EndpointBuilder0[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse, Produces](
  protected val meta0: Meta,
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
    new EndpointBuilder0[F, Req, Resp, Produces](m(meta0), onEndpointCreated, handler)

  override protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): EndpointBuilder1[F, Req, Resp, Produces, T] =
    new EndpointBuilder1[F, Req, Resp, Produces, T](m(meta0), onEndpointCreated, handler, value)

  override protected def resp[T](m: Meta => Meta, value: T => F[Resp]): EndpointBuilder0[F, Req, Resp, T] =
    new EndpointBuilder0[F, Req, Resp, T](m(meta0), onEndpointCreated, value)

  override protected def makeHandler(handler: () => F[Produces]): MatchedRequest[Req] => F[Resp] =
    _ => handler().flatMap(this.handler)
}

/** Endpoint builder for handler with 1 argument. See EndpointBuilder for builder API */
class EndpointBuilder1[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse, Produces, P1](
  protected val meta0: Meta,
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
    new EndpointBuilder1[F, Req, Resp, Produces, P1](m(meta0), onEndpointCreated, handler, param1)

  override protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): EndpointBuilder2[F, Req, Resp, Produces, P1, T] =
    new EndpointBuilder2[F, Req, Resp, Produces, P1, T](m(meta0), onEndpointCreated, handler, param1, value)

  override protected def resp[T](m: Meta => Meta, value: T => F[Resp]): EndpointBuilder1[F, Req, Resp, T, P1] =
    new EndpointBuilder1[F, Req, Resp, T, P1](m(meta0), onEndpointCreated, value, param1)

  override protected def makeHandler(handler: (P1) => F[Produces]): MatchedRequest[Req] => F[Resp] =
    req =>
      param1(req).flatMap { p1 =>
        handler(p1).flatMap(this.handler)
      }

}

/** Endpoint builder for handler with 2 arguments. See EndpointBuilder for builder API */
class EndpointBuilder2[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse, Produces, P1, P2](
  protected val meta0: Meta,
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
    new EndpointBuilder2[F, Req, Resp, Produces, P1, P2](m(meta0), onEndpointCreated, handler, param1, param2)

  override protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): EndpointBuilder3[F, Req, Resp, Produces, P1, P2, T] =
    new EndpointBuilder3[F, Req, Resp, Produces, P1, P2, T](m(meta0), onEndpointCreated, handler, param1, param2, value)

  override protected def resp[T](m: Meta => Meta, value: T => F[Resp]): EndpointBuilder2[F, Req, Resp, T, P1, P2] =
    new EndpointBuilder2[F, Req, Resp, T, P1, P2](m(meta0), onEndpointCreated, value, param1, param2)

  override protected def makeHandler(handler: (P1, P2) => F[Produces]): MatchedRequest[Req] => F[Resp] =
    req =>
      param1(req).flatMap { p1 =>
        param2(req).flatMap { p2 =>
          handler(p1, p2).flatMap(this.handler)
        }
      }
}

/** Endpoint builder for handler with 3 arguments. See EndpointBuilder for builder API */
class EndpointBuilder3[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse, Produces, P1, P2, P3](
  protected val meta0: Meta,
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
    new EndpointBuilder3[F, Req, Resp, Produces, P1, P2, P3](m(meta0), onEndpointCreated, handler, param1, param2, param3)

  override protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): EndpointBuilder4[F, Req, Resp, Produces, P1, P2, P3, T] =
    new EndpointBuilder4[F, Req, Resp, Produces, P1, P2, P3, T](m(meta0), onEndpointCreated, handler, param1, param2, param3, value)

  override protected def resp[T](m: Meta => Meta, value: T => F[Resp]): EndpointBuilder3[F, Req, Resp, T, P1, P2, P3] =
    new EndpointBuilder3[F, Req, Resp, T, P1, P2, P3](meta0, onEndpointCreated, value, param1, param2, param3)

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

/** Endpoint builder for handler with 4 arguments. See EndpointBuilder for builder API */
class EndpointBuilder4[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse, Produces, P1, P2, P3, P4](
  protected val meta0: Meta,
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
    new EndpointBuilder4[F, Req, Resp, Produces, P1, P2, P3, P4](m(meta0), onEndpointCreated, handler, param1, param2, param3, param4)

  override protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): EndpointBuilder5[F, Req, Resp, Produces, P1, P2, P3, P4, T] =
    new EndpointBuilder5[F, Req, Resp, Produces, P1, P2, P3, P4, T](m(meta0), onEndpointCreated, handler, param1, param2, param3, param4, value)

  override protected def resp[T](m: Meta => Meta, value: T => F[Resp]): EndpointBuilder4[F, Req, Resp, T, P1, P2, P3, P4] =
    new EndpointBuilder4[F, Req, Resp, T, P1, P2, P3, P4](m(meta0), onEndpointCreated, value, param1, param2, param3, param4)

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

/** Endpoint builder for handler with 5 arguments. See EndpointBuilder for builder API */
class EndpointBuilder5[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse, Produces, P1, P2, P3, P4, P5](
  protected val meta0: Meta,
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
    new EndpointBuilder5[F, Req, Resp, Produces, P1, P2, P3, P4, P5](m(meta0), onEndpointCreated, handler, param1, param2, param3, param4, param5)

  override protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): EndpointBuilder6[F, Req, Resp, Produces, P1, P2, P3, P4, P5, T] =
    new EndpointBuilder6[F, Req, Resp, Produces, P1, P2, P3, P4, P5, T](m(meta0), onEndpointCreated, handler, param1, param2, param3, param4, param5, value)

  override protected def resp[T](m: Meta => Meta, value: T => F[Resp]): EndpointBuilder5[F, Req, Resp, T, P1, P2, P3, P4, P5] =
    new EndpointBuilder5[F, Req, Resp, T, P1, P2, P3, P4, P5](m(meta0), onEndpointCreated, value, param1, param2, param3, param4, param5)

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

/** Endpoint builder for handler with 6 arguments. See EndpointBuilder for builder API */
class EndpointBuilder6[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse, Produces, P1, P2, P3, P4, P5, P6](
  protected val meta0: Meta,
  protected val onEndpointCreated: Endpoint[F, Req, Resp] => Unit,
  private val handler: Produces => F[Resp],
  private val param1: MatchedRequest[Req] => F[P1],
  private val param2: MatchedRequest[Req] => F[P2],
  private val param3: MatchedRequest[Req] => F[P3],
  private val param4: MatchedRequest[Req] => F[P4],
  private val param5: MatchedRequest[Req] => F[P5],
  private val param6: MatchedRequest[Req] => F[P6]
)
  extends EndpointBuilderImpl[F, Req, Resp, Produces,
    (P1, P2, P3, P4, P5, P6) => F[Produces],
    EndpointBuilder7[F, Req, Resp, Produces, P1, P2, P3, P4, P5, P6, ?],
    EndpointBuilder6[F, Req, Resp, ?, P1, P2, P3, P4, P5, P6]
  ] {
  override protected def monadError: MonadError[F, Throwable] = implicitly

  override protected def respT: RouteHttpResponse[Resp] = implicitly

  override protected def self(m: Meta => Meta): Self =
    new EndpointBuilder6[F, Req, Resp, Produces, P1, P2, P3, P4, P5, P6](m(meta0), onEndpointCreated, handler, param1, param2, param3, param4, param5, param6)

  override protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): EndpointBuilder7[F, Req, Resp, Produces, P1, P2, P3, P4, P5, P6, T] =
    new EndpointBuilder7[F, Req, Resp, Produces, P1, P2, P3, P4, P5, P6, T](m(meta0), onEndpointCreated, handler, param1, param2, param3, param4, param5, param6, value)

  override protected def resp[T](m: Meta => Meta, value: T => F[Resp]): EndpointBuilder6[F, Req, Resp, T, P1, P2, P3, P4, P5, P6] =
    new EndpointBuilder6[F, Req, Resp, T, P1, P2, P3, P4, P5, P6](m(meta0), onEndpointCreated, value, param1, param2, param3, param4, param5, param6)

  override protected def makeHandler(handler: (P1, P2, P3, P4, P5, P6) => F[Produces]): MatchedRequest[Req] => F[Resp] =
    req =>
      param1(req).flatMap { p1 =>
        param2(req).flatMap { p2 =>
          param3(req).flatMap { p3 =>
            param4(req).flatMap { p4 =>
              param5(req).flatMap { p5 =>
                param6(req).flatMap { p6 =>
                  handler(p1, p2, p3, p4, p5, p6).flatMap(this.handler)
                }
              }
            }
          }
        }
      }
}

/** Endpoint builder for handler with 7 arguments. See EndpointBuilder for builder API */
class EndpointBuilder7[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse, Produces, P1, P2, P3, P4, P5, P6, P7](
  protected val meta0: Meta,
  protected val onEndpointCreated: Endpoint[F, Req, Resp] => Unit,
  private val handler: Produces => F[Resp],
  private val param1: MatchedRequest[Req] => F[P1],
  private val param2: MatchedRequest[Req] => F[P2],
  private val param3: MatchedRequest[Req] => F[P3],
  private val param4: MatchedRequest[Req] => F[P4],
  private val param5: MatchedRequest[Req] => F[P5],
  private val param6: MatchedRequest[Req] => F[P6],
  private val param7: MatchedRequest[Req] => F[P7]
)
  extends EndpointBuilderImpl[F, Req, Resp, Produces,
    (P1, P2, P3, P4, P5, P6, P7) => F[Produces],
    EndpointBuilder8[F, Req, Resp, Produces, P1, P2, P3, P4, P5, P6, P7, ?],
    EndpointBuilder7[F, Req, Resp, ?, P1, P2, P3, P4, P5, P6, P7]
  ] {
  override protected def monadError: MonadError[F, Throwable] = implicitly

  override protected def respT: RouteHttpResponse[Resp] = implicitly

  override protected def self(m: Meta => Meta): Self =
    new EndpointBuilder7[F, Req, Resp, Produces, P1, P2, P3, P4, P5, P6, P7](m(meta0), onEndpointCreated, handler, param1, param2, param3, param4, param5, param6, param7)

  override protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): EndpointBuilder8[F, Req, Resp, Produces, P1, P2, P3, P4, P5, P6, P7, T] =
    new EndpointBuilder8[F, Req, Resp, Produces, P1, P2, P3, P4, P5, P6, P7, T](m(meta0), onEndpointCreated, handler, param1, param2, param3, param4, param5, param6, param7, value)

  override protected def resp[T](m: Meta => Meta, value: T => F[Resp]): EndpointBuilder7[F, Req, Resp, T, P1, P2, P3, P4, P5, P6, P7] =
    new EndpointBuilder7[F, Req, Resp, T, P1, P2, P3, P4, P5, P6, P7](m(meta0), onEndpointCreated, value, param1, param2, param3, param4, param5, param6, param7)

  override protected def makeHandler(handler: (P1, P2, P3, P4, P5, P6, P7) => F[Produces]): MatchedRequest[Req] => F[Resp] =
    req =>
      param1(req).flatMap { p1 =>
        param2(req).flatMap { p2 =>
          param3(req).flatMap { p3 =>
            param4(req).flatMap { p4 =>
              param5(req).flatMap { p5 =>
                param6(req).flatMap { p6 =>
                  param7(req).flatMap { p7 =>
                    handler(p1, p2, p3, p4, p5, p6, p7).flatMap(this.handler)
                  }
                }
              }
            }
          }
        }
      }
}

/** Endpoint builder for handler with 8 arguments. See EndpointBuilder for builder API */
class EndpointBuilder8[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse, Produces, P1, P2, P3, P4, P5, P6, P7, P8](
  protected val meta0: Meta,
  protected val onEndpointCreated: Endpoint[F, Req, Resp] => Unit,
  private val handler: Produces => F[Resp],
  private val param1: MatchedRequest[Req] => F[P1],
  private val param2: MatchedRequest[Req] => F[P2],
  private val param3: MatchedRequest[Req] => F[P3],
  private val param4: MatchedRequest[Req] => F[P4],
  private val param5: MatchedRequest[Req] => F[P5],
  private val param6: MatchedRequest[Req] => F[P6],
  private val param7: MatchedRequest[Req] => F[P7],
  private val param8: MatchedRequest[Req] => F[P8]
)
  extends EndpointBuilderImpl[F, Req, Resp, Produces,
    (P1, P2, P3, P4, P5, P6, P7, P8) => F[Produces],
    EndpointBuilder9[F, Req, Resp, Produces, P1, P2, P3, P4, P5, P6, P7, P8, ?],
    EndpointBuilder8[F, Req, Resp, ?, P1, P2, P3, P4, P5, P6, P7, P8]
  ] {
  override protected def monadError: MonadError[F, Throwable] = implicitly

  override protected def respT: RouteHttpResponse[Resp] = implicitly

  override protected def self(m: Meta => Meta): Self =
    new EndpointBuilder8[F, Req, Resp, Produces, P1, P2, P3, P4, P5, P6, P7, P8](m(meta0), onEndpointCreated, handler, param1, param2, param3, param4, param5, param6, param7, param8)

  override protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): EndpointBuilder9[F, Req, Resp, Produces, P1, P2, P3, P4, P5, P6, P7, P8, T] =
    new EndpointBuilder9[F, Req, Resp, Produces, P1, P2, P3, P4, P5, P6, P7, P8, T](m(meta0), onEndpointCreated, handler, param1, param2, param3, param4, param5, param6, param7, param8, value)

  override protected def resp[T](m: Meta => Meta, value: T => F[Resp]): EndpointBuilder8[F, Req, Resp, T, P1, P2, P3, P4, P5, P6, P7, P8] =
    new EndpointBuilder8[F, Req, Resp, T, P1, P2, P3, P4, P5, P6, P7, P8](m(meta0), onEndpointCreated, value, param1, param2, param3, param4, param5, param6, param7, param8)

  override protected def makeHandler(handler: (P1, P2, P3, P4, P5, P6, P7, P8) => F[Produces]): MatchedRequest[Req] => F[Resp] =
    req =>
      param1(req).flatMap { p1 =>
        param2(req).flatMap { p2 =>
          param3(req).flatMap { p3 =>
            param4(req).flatMap { p4 =>
              param5(req).flatMap { p5 =>
                param6(req).flatMap { p6 =>
                  param7(req).flatMap { p7 =>
                    param8(req).flatMap { p8 =>
                      handler(p1, p2, p3, p4, p5, p6, p7, p8).flatMap(this.handler)
                    }
                  }
                }
              }
            }
          }
        }
      }
}

/** Endpoint builder for handler with 9 arguments. See EndpointBuilder for builder API */
class EndpointBuilder9[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse, Produces, P1, P2, P3, P4, P5, P6, P7, P8, P9](
  protected val meta0: Meta,
  protected val onEndpointCreated: Endpoint[F, Req, Resp] => Unit,
  private val handler: Produces => F[Resp],
  private val param1: MatchedRequest[Req] => F[P1],
  private val param2: MatchedRequest[Req] => F[P2],
  private val param3: MatchedRequest[Req] => F[P3],
  private val param4: MatchedRequest[Req] => F[P4],
  private val param5: MatchedRequest[Req] => F[P5],
  private val param6: MatchedRequest[Req] => F[P6],
  private val param7: MatchedRequest[Req] => F[P7],
  private val param8: MatchedRequest[Req] => F[P8],
  private val param9: MatchedRequest[Req] => F[P9]
)
  extends EndpointBuilderImpl[F, Req, Resp, Produces,
    (P1, P2, P3, P4, P5, P6, P7, P8, P9) => F[Produces],
    EndpointBuilder10[F, Req, Resp, Produces, P1, P2, P3, P4, P5, P6, P7, P8, P9, ?],
    EndpointBuilder9[F, Req, Resp, ?, P1, P2, P3, P4, P5, P6, P7, P8, P9]
  ] {
  override protected def monadError: MonadError[F, Throwable] = implicitly

  override protected def respT: RouteHttpResponse[Resp] = implicitly

  override protected def self(m: Meta => Meta): Self =
    new EndpointBuilder9[F, Req, Resp, Produces, P1, P2, P3, P4, P5, P6, P7, P8, P9](m(meta0), onEndpointCreated, handler, param1, param2, param3, param4, param5, param6, param7, param8, param9)

  override protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): EndpointBuilder10[F, Req, Resp, Produces, P1, P2, P3, P4, P5, P6, P7, P8, P9, T] =
    new EndpointBuilder10[F, Req, Resp, Produces, P1, P2, P3, P4, P5, P6, P7, P8, P9, T](m(meta0), onEndpointCreated, handler, param1, param2, param3, param4, param5, param6, param7, param8, param9, value)

  override protected def resp[T](m: Meta => Meta, value: T => F[Resp]): EndpointBuilder9[F, Req, Resp, T, P1, P2, P3, P4, P5, P6, P7, P8, P9] =
    new EndpointBuilder9[F, Req, Resp, T, P1, P2, P3, P4, P5, P6, P7, P8, P9](m(meta0), onEndpointCreated, value, param1, param2, param3, param4, param5, param6, param7, param8, param9)

  override protected def makeHandler(handler: (P1, P2, P3, P4, P5, P6, P7, P8, P9) => F[Produces]): MatchedRequest[Req] => F[Resp] =
    req =>
      param1(req).flatMap { p1 =>
        param2(req).flatMap { p2 =>
          param3(req).flatMap { p3 =>
            param4(req).flatMap { p4 =>
              param5(req).flatMap { p5 =>
                param6(req).flatMap { p6 =>
                  param7(req).flatMap { p7 =>
                    param8(req).flatMap { p8 =>
                      param9(req).flatMap { p9 =>
                        handler(p1, p2, p3, p4, p5, p6, p7, p8, p9).flatMap(this.handler)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
}

/** Endpoint builder for handler with 10 arguments. See EndpointBuilder for builder API */
class EndpointBuilder10[F[_]: MonadError[?[_], Throwable], Req, Resp: RouteHttpResponse, Produces, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10](
  protected val meta0: Meta,
  protected val onEndpointCreated: Endpoint[F, Req, Resp] => Unit,
  private val handler: Produces => F[Resp],
  private val param1: MatchedRequest[Req] => F[P1],
  private val param2: MatchedRequest[Req] => F[P2],
  private val param3: MatchedRequest[Req] => F[P3],
  private val param4: MatchedRequest[Req] => F[P4],
  private val param5: MatchedRequest[Req] => F[P5],
  private val param6: MatchedRequest[Req] => F[P6],
  private val param7: MatchedRequest[Req] => F[P7],
  private val param8: MatchedRequest[Req] => F[P8],
  private val param9: MatchedRequest[Req] => F[P9],
  private val param10: MatchedRequest[Req] => F[P10],
)
  extends EndpointBuilderImpl[F, Req, Resp, Produces,
    (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) => F[Produces],
    DeadEnd[?],
    EndpointBuilder10[F, Req, Resp, ?, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10]
  ] {
  override protected def monadError: MonadError[F, Throwable] = implicitly

  override protected def respT: RouteHttpResponse[Resp] = implicitly

  override protected def self(m: Meta => Meta): Self =
    new EndpointBuilder10[F, Req, Resp, Produces, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10](m(meta0), onEndpointCreated, handler, param1, param2, param3, param4, param5, param6, param7, param8, param9, param10)

  override protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): DeadEnd[T] =
    throw new IllegalArgumentException("Only 10 parameters are supported, try packing parameters to case classes")

  override protected def resp[T](m: Meta => Meta, value: T => F[Resp]): EndpointBuilder10[F, Req, Resp, T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10] =
    new EndpointBuilder10[F, Req, Resp, T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10](m(meta0), onEndpointCreated, value, param1, param2, param3, param4, param5, param6, param7, param8, param9, param10)

  override protected def makeHandler(handler: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) => F[Produces]): MatchedRequest[Req] => F[Resp] =
    req =>
      param1(req).flatMap { p1 =>
        param2(req).flatMap { p2 =>
          param3(req).flatMap { p3 =>
            param4(req).flatMap { p4 =>
              param5(req).flatMap { p5 =>
                param6(req).flatMap { p6 =>
                  param7(req).flatMap { p7 =>
                    param8(req).flatMap { p8 =>
                      param9(req).flatMap { p9 =>
                        param10(req).flatMap { p10 =>
                          handler(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10).flatMap(this.handler)
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
}


sealed trait DeadEnd[_]