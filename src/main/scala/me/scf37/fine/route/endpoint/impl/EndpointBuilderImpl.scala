package me.scf37.fine.route.endpoint.impl

import cats.Applicative
import cats.MonadError
import me.scf37.fine.route.RouteBadPathParameterException
import me.scf37.fine.route.RouteBadQueryParameterException
import me.scf37.fine.route.RouteNoPathParameterException
import me.scf37.fine.route.RouteNoQueryParameterException
import me.scf37.fine.route.endpoint.Endpoint
import me.scf37.fine.route.endpoint.EndpointBuilder
import me.scf37.fine.route.endpoint.MatchedRequest
import me.scf37.fine.route.endpoint.meta.{Meta, MetaBody, MetaMethod, MetaResultCode, MultiMetaParameter, SecondaryTag, SingleMetaParameter}
import me.scf37.fine.route.typeclass.RequestBody
import me.scf37.fine.route.typeclass.RequestParam
import me.scf37.fine.route.typeclass.RequestParams
import me.scf37.fine.route.typeclass.ResponseBody
import me.scf37.fine.route.typeclass.RouteHttpResponse

import scala.reflect.runtime.universe._

/** Skeletal implementation of EndpointBuilder, reducing count of methods to implement */
trait EndpointBuilderImpl[F[_], Req, Resp, Produces, Handler, NextBuilder[_], RespBuilder[_]]
  extends EndpointBuilder[F, Req, Resp, Produces, Handler, NextBuilder, RespBuilder] {

  private implicit val ff: MonadError[F, Throwable] = monadError
  private implicit val respT1: RouteHttpResponse[Resp] = respT

  protected def monadError: MonadError[F, Throwable]
  protected def respT: RouteHttpResponse[Resp]
  protected def self(m: Meta => Meta): Self
  protected def next[T](m: Meta => Meta, value: MatchedRequest[Req] => F[T]): NextBuilder[T]
  protected def resp[T](m: Meta => Meta, value: T => F[Resp]): RespBuilder[T]
  protected def makeHandler(handler: Handler): MatchedRequest[Req] => F[Resp]
  protected def meta0: Meta
  protected def onEndpointCreated: Endpoint[F, Req, Resp] => Unit

  override def meta(f: Meta => Meta): Self = self(f)

  override def pathParam[T: TypeTag: RequestParam](name: String, description: String): NextBuilder[T] = next(
    meta => meta.copy(params = meta.params :+ SingleMetaParameter(name, description, implicitly[TypeTag[T]], true)),
    req => req.pathParams.get(name) match {
      case None => ff.raiseError(RouteNoPathParameterException(name, req.meta.pathPattern))
      case Some(v) => lift(RequestParam[T].parse(v), e => RouteBadPathParameterException(name, v, e.getMessage, e))
    }
  )

  override def pathParams[T: TypeTag: RequestParams]: NextBuilder[T] = next(
    meta => meta.copy(params = meta.params :+ MultiMetaParameter(implicitly[TypeTag[T]], true)),
    req => lift(RequestParams[T].parse(req.pathParams),
      e => RouteBadPathParameterException("<path>", prettyMap(req.pathParams), e.getMessage, e))
  )

  override def queryParam[T: TypeTag: RequestParam](name: String, description: String): NextBuilder[T] = next(
    meta => meta.copy(params = meta.params :+ SingleMetaParameter(name, description, implicitly[TypeTag[T]], false)),
    req => req.queryParams.get(name) match {
      case None if isOption[T] => ff.pure(None.asInstanceOf[T])
      case None => ff.raiseError(RouteNoQueryParameterException(name, req.url))
      case Some(v) => lift(RequestParam[T].parse(v), e => RouteBadQueryParameterException(name, v, e.getMessage, e))
    }
  )

  override def queryParams[T: TypeTag: RequestParams]: NextBuilder[T] = next(
    meta => meta.copy(params = meta.params :+ MultiMetaParameter(implicitly, false)),
    req => lift(RequestParams[T].parse(req.queryParams),
      e => RouteBadQueryParameterException("<path>", prettyMap(req.pathParams), e.getMessage, e))
  )

  override def consumes[T: TypeTag: RequestBody]: NextBuilder[T] = next(
    meta => meta.copy(consumes = Some(MetaBody(RequestBody[T].contentType, implicitly))),
    req => lift(RequestBody[T].parse(req.body()), identity)
  )

  override def produces[T: TypeTag: ResponseBody]: RespBuilder[T] = resp(
    meta => meta.copy(produces = Some(MetaBody(ResponseBody[T].contentType, implicitly))),
    body => lift(ResponseBody[T].write(body).flatMap(bytes =>
      RouteHttpResponse[Resp].write(bytes, ResponseBody[T].contentType)
    ), identity)
  )

  override def tag(tag: String): Self = self(meta => meta.copy(tag = tag))

  override def secondaryTag(name: String, description: String = "", bgColor: String = "#555555") : Self =
    self(meta => meta.copy(secondaryTags = meta.secondaryTags :+ SecondaryTag(name, description, bgColor)))

  override def summary(value: String): Self = self(_.copy(summary = value))

  override def description(value: String): Self = self(_.copy(description = value))

  override def resultCode[T: TypeTag](code: Int, description: String): Self =
    self(meta => meta.copy(resultCodes =  meta.resultCodes :+ MetaResultCode(code, description, implicitly)))

  override def routeData(value: Any): Self = self(m => m.copy(routeData = m.routeData :+ value ))

  override def tagDescription(tag: String, description: String): EndpointBuilderImpl.this.type = ???

  override def withRequest: NextBuilder[Req] = next(
    meta => meta,
    req => Applicative[F].pure(req.req)
  )

  override def withUnmatchedPath: NextBuilder[String] = next(
    meta => meta,
    req => Applicative[F].pure(req.unmatchedPath)
  )

  override def get(pathPattern: String)(h: Handler): Endpoint[F, Req, Resp] = endpoint(MetaMethod.GET, pathPattern, h)

  override def put(pathPattern: String)(h: Handler): Endpoint[F, Req, Resp] = endpoint(MetaMethod.PUT, pathPattern, h)

  override def post(pathPattern: String)(h: Handler): Endpoint[F, Req, Resp] = endpoint(MetaMethod.POST, pathPattern, h)

  override def delete(pathPattern: String)(h: Handler): Endpoint[F, Req, Resp] = endpoint(MetaMethod.DELETE, pathPattern, h)

  override def patch(pathPattern: String)(h: Handler): Endpoint[F, Req, Resp] = endpoint(MetaMethod.PATCH, pathPattern, h)

  private def endpoint(method: MetaMethod, pathPattern: String, h: Handler): Endpoint[F, Req, Resp] = {
    val e = Endpoint(
      meta = meta0.copy(pathPattern = pathPattern, method = method),
      handle = makeHandler(h)
    )
    onEndpointCreated(e)
    e
  }

  private def lift[A, E <: Throwable](e: Either[E, A], f: Throwable => Throwable): F[A] = e match {
    case Left(e) => MonadError[F, Throwable].raiseError(f(e))
    case Right(v) => Applicative[F].pure(v)
  }

  private def prettyMap(m: Map[String, String]): String = {
    "{" + m.map {case (k, v) => s"$k='$v'"}.mkString(", ") + "}"
  }

  private def isOption[T: TypeTag]: Boolean = implicitly[TypeTag[T]].tpe <:< typeOf[Option[_]]

}
