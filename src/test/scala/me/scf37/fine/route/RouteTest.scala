package me.scf37.fine.route

import cats.MonadError
import me.scf37.fine.route.model.RequestBody1
import me.scf37.fine.route.model.ResponseBody1
import me.scf37.fine.route.typeclass.RouteHttpRequest
import me.scf37.fine.route.typeclass.RouteHttpResponse

import scala.concurrent.Future

class RouteTest {
  val r = new Route[Future, RequestBody1, ResponseBody1] {
    override protected implicit val monadError: MonadError[Future, Throwable] = implicitly
    override protected implicit val routeHttpRequest: RouteHttpRequest[RequestBody1] = implicitly
    override protected implicit val routeHttpResponse: RouteHttpResponse[ResponseBody1] = implicitly

    override def apply(v1: RequestBody1): Future[() => Future[ResponseBody1]] = ???

    endpoint
      .summary("hello endpoint")
      .get("/") { () =>
        Future.successful(ResponseBody1("hello!"))
      }

    endpoint
      .summary("world endpoint")
      .get("/world") { () =>
        Future.successful(ResponseBody1("world!"))
      }
  }
}
