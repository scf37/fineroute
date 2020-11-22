package me.scf37.fine.route

import cats.MonadError
import cats.implicits._
import me.scf37.fine.route.endpoint.meta.MetaMethod
import me.scf37.fine.route.model.Request
import me.scf37.fine.route.model.Response
import me.scf37.fine.route.typeclass.RouteHttpRequest
import me.scf37.fine.route.typeclass.RouteHttpResponse
import org.scalatest.freespec.AnyFreeSpec

class RouteTest extends AnyFreeSpec {
  trait EitherRoute extends RouteDsl[Either[Throwable, ?], Request, Response] {
    override protected def monadError: MonadError[Either[Throwable, ?], Throwable] = implicitly[MonadError[Either[Throwable, ?], Throwable]]
    override protected def routeHttpRequest: RouteHttpRequest[Request] = implicitly[RouteHttpRequest[Request]]
    override protected def routeHttpResponse: RouteHttpResponse[Response] = implicitly[RouteHttpResponse[Response]]
  }

  val r = new EitherRoute {
    endpoint
      .summary("hello endpoint")
      .get("/") { () =>
        Right(Response("hello!", ""))
      }

    endpoint
      .summary("world endpoint")
      .get("/world") { () =>
        Right(Response("world!", ""))
      }

     endpoint
      .summary("endpoint with parameters")
      .pathParam[String]("name", "hello name")
      .queryParam[Option[String]]("adj", "adjective to use")
      .get("/hello/{name}") { (name, adj) =>
        Right(Response(s"Hello, ${adj.fold("")(_ + " ")}${name}!", ""))
      }
  }

  "route meta is present" in {
    assert(r.meta.endpointMetas.length == 3)
  }

  "params are working" in {
    assert(r.get(Request("", "/hello/Scala")).data == "Hello, Scala!")
    assert(r.get(Request("", "/hello/Scala/")).data == "Hello, Scala!")
    assert(r.get(Request("", "/hello/Scala?adj1=unused")).data == "Hello, Scala!")
    assert(r.get(Request("", "/hello/World?adj=beautiful")).data == "Hello, beautiful World!")

    assertThrows[RouteUnmatchedException.type] {
      r.get(Request("", "/hello/"))
    }
    assertThrows[RouteUnmatchedException.type] {
      r.get(Request("", "/hello"))
    }
    assertThrows[RouteUnmatchedException.type] {
      r.get(Request("", "/hello/Scala/1"))
    }
  }

  implicit class RouteResponseHelper(r: Route[Either[Throwable, ?], Request, Response]) {
    def get(req: Request): Response = r(RouteRequest(MetaMethod.GET, req.url)).fold(throw RouteUnmatchedException)(_(req).fold(throw _, identity))
  }

}
