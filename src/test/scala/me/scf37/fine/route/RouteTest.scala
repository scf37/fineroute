package me.scf37.fine.route

import cats.MonadError
import cats.implicits._
import me.scf37.fine.route.model.Request
import me.scf37.fine.route.model.Response
import me.scf37.fine.route.typeclass.RouteHttpRequest
import me.scf37.fine.route.typeclass.RouteHttpResponse
import org.scalatest.FreeSpec

class RouteTest extends FreeSpec {
  trait EitherRoute extends Route[Either[Throwable, ?], Request, Response] {
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
    assert(r(Request("", "/hello/Scala")).get.data == "Hello, Scala!")
    assert(r(Request("", "/hello/Scala/")).get.data == "Hello, Scala!")
    assert(r(Request("", "/hello/Scala?adj1=unused")).get.data == "Hello, Scala!")
    assert(r(Request("", "/hello/World?adj=beautiful")).get.data == "Hello, beautiful World!")

    assertThrows[RouteUnmatchedException.type] {
      r(Request("", "/hello/")).get
    }
    assertThrows[RouteUnmatchedException.type] {
      r(Request("", "/hello")).get
    }
    assertThrows[RouteUnmatchedException.type] {
      r(Request("", "/hello/Scala/1")).get
    }
  }

  implicit class RouteResponseHelper[A](f: Either[Throwable, () => Either[Throwable, A]]) {
    def get: A = f.fold(throw _, _().fold(throw _ , identity))
  }

}
