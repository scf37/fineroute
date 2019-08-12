package me.scf37.fine.route.model

import me.scf37.fine.route.RouteException
import me.scf37.fine.route.typeclass.RequestBody
import me.scf37.fine.route.typeclass.RequestParams
import me.scf37.fine.route.typeclass.ResponseBody

import scala.util.Try

case class Request(data: String)
case class Response(
  data: String,
  contentType: String
)
case class RequestBody1(data: String)
object RequestBody1 {
  implicit val p = new RequestBody[RequestBody1] {
    override def parse(request: Array[Byte]): Either[RouteException, RequestBody1] =
      Right(RequestBody1(new String(request)))

    override def contentType: String = "application/x-www-form-urlencoded"
  }
}

case class ResponseBody1(data: String)
object ResponseBody1 {
  implicit val p = new ResponseBody[ResponseBody1] {
    override def contentType: String = "application/json"

    override def write(body: ResponseBody1): Either[Throwable, Array[Byte]] = Right(body.data.getBytes())
  }
}
case class Params(a: Int, b: String)
object Params {
  implicit val p1 = new RequestParams[Params] {
    override def parse(paramsMap: Map[String, String]): Either[Throwable, Params] =
      Try(Params(a = paramsMap("a").toInt, b = paramsMap("b"))).toEither
  }
}
