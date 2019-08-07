package me.scf37.fine.route

import me.scf37.fine.route.typeclass.RequestBody
import me.scf37.fine.route.typeclass.ResponseBody
import me.scf37.fine.route.typeclass.RequestParam
import me.scf37.fine.route.typeclass.RequestParams

import scala.reflect.runtime.universe._

trait RouteBuilder[F[_], Req, Resp, Produces, Handler, NextBuilder[_], RespBuilder[_]] {
  type Self = RouteBuilder[F, Req, Resp, Produces, Handler, NextBuilder, RespBuilder]

  def withRequest: NextBuilder[Req]
  def withUnmatchedPath: NextBuilder[String]

  def pathParam[T: TypeTag: RequestParam](name: String, description: String): NextBuilder[T]
  def pathParams[T: TypeTag: RequestParams]: NextBuilder[T]

  def queryParam[T: TypeTag: RequestParam](name: String, description: String): NextBuilder[T]
  def queryParams[T: TypeTag: RequestParams]: NextBuilder[T]

  def consumes[T: TypeTag: RequestBody]: NextBuilder[T]
  def produces[T: TypeTag: ResponseBody]: RespBuilder[T]

  def tags(tag: String*): Self
  def summary(value: String): Self
  def description(value: String): Self
  def resultCode[T: TypeTag](code: Int, description: String): Self
  def routeData(value: Any): Self

  def tagDescription(tag: String, description: String): this.type

  def get(pathPattern: String)(h: Handler): Route[F, Req, Resp]
  def put(pathPattern: String)(h: Handler): Route[F, Req, Resp]
  def post(pathPattern: String)(h: Handler): Route[F, Req, Resp]
  def delete(pathPattern: String)(h: Handler): Route[F, Req, Resp]
  def patch(pathPattern: String)(h: Handler): Route[F, Req, Resp]
}
