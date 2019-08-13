package me.scf37.fine.route.model

import me.scf37.fine.route.RouteException
import me.scf37.fine.route.meta.MetaMethod
import me.scf37.fine.route.typeclass.RequestBody
import me.scf37.fine.route.typeclass.RequestParams
import me.scf37.fine.route.typeclass.ResponseBody
import me.scf37.fine.route.typeclass.RouteHttpRequest
import me.scf37.fine.route.typeclass.RouteHttpResponse

import scala.util.Try

case class Request(data: String, url: String = "")
object Request {
  implicit val r: RouteHttpRequest[Request] = new RouteHttpRequest[Request] {
    override def method(req: Request): MetaMethod = MetaMethod.GET

    override def url(req: Request): String = req.url

    override def queryParams(req: Request): Map[String, String] = req.url.indexOf('?') match {
      case -1 => Map.empty
      case i => req.url.substring(i + 1).split("&").map(_.split("=")).map(p => p(0) -> p(1)).toMap
    }

    override def body(req: Request): () => Array[Byte] = () => req.data.getBytes
  }
}

case class Response(
  data: String,
  contentType: String
)
object Response {
  implicit val r: RouteHttpResponse[Response] = new RouteHttpResponse[Response] {
    override def write(arr: Array[Byte], contentType: String): Either[Throwable, Response] =
      Right(Response(new String(arr), contentType))
  }
}

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
