package me.scf37.fine.route.endpoint

import cats.implicits._
import me.scf37.fine.route.RouteBadPathParameterException
import me.scf37.fine.route.RouteBadQueryParameterException
import me.scf37.fine.route.RouteNoPathParameterException
import me.scf37.fine.route.RouteNoQueryParameterException
import me.scf37.fine.route.endpoint
import me.scf37.fine.route.endpoint.meta.Meta
import me.scf37.fine.route.endpoint.meta.MetaMethod
import me.scf37.fine.route.endpoint.meta.MetaResultCode
import me.scf37.fine.route.endpoint.meta.MultiMetaParameter
import me.scf37.fine.route.endpoint.meta.SingleMetaParameter
import me.scf37.fine.route.model.Params
import me.scf37.fine.route.model.Request
import me.scf37.fine.route.model.RequestBody1
import me.scf37.fine.route.model.Response
import me.scf37.fine.route.model.ResponseBody1
import me.scf37.fine.route.typeclass.RouteHttpResponse
import org.scalatest.freespec.AnyFreeSpec

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration

class EndpointBuilderTest extends AnyFreeSpec {
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val respT = new RouteHttpResponse[Response] {
    override def write(arr: Array[Byte], contentType: String): Either[Throwable, Response] =
      Right(Response(new String(arr, "utf-8"), contentType))
  }

  "meta should populate correctly" in {
    val route = Endpoint.builder[Future, Request, Response]

      .summary("summary")
      .description("description")
      .tag("tag1")
      .pathParam[Long]("pathParam", "pathParam description")
      .queryParam[Int]("queryParam", "queryParam description")
      .pathParams[Params]
      .queryParams[Params]
      .consumes[RequestBody1]
      .produces[ResponseBody1]
      .resultCode[ResponseBody1](200, "Successful response")
      .resultCode[String](500, "Internal server error")
      .routeData("hello")
      .post("/{param}/{a}/{b}") { (p1, q1, p2, q2, req) =>
        val vp1: Long = p1
        val vq1: Int = q1
        val vp2: Params = p2
        val vq2: Params = q2
        val vreq: RequestBody1 = req

        // remove unused compilation warning
        (vp1, vq1, vp2, vq2, vreq)

        val r2: ResponseBody1 = ResponseBody1("")
        Future successful r2
      }

    println(route.meta.params)

    val m = route.meta
    assert(m.summary == "summary")
    assert(m.description == "description")
    assert(m.tag == "tag1")
    assert(m.method == MetaMethod.POST)
    assert(m.routeData == List("hello"))
    assert(m.consumes.get.mime == "application/x-www-form-urlencoded")
    assert(m.produces.get.mime == "application/json")
    assert(m.resultCodes.size == 2)
    assert(m.resultCodes.exists {
      case MetaResultCode(200, "Successful response", _) => true
      case _ => false
    })
    assert(m.resultCodes.exists {
      case MetaResultCode(500, "Internal server error", _) => true
      case _ => false
    })
    assert(m.params.size == 4)


    assert(m.params.exists {
      case SingleMetaParameter("pathParam", "pathParam description", _, true) => true
      case _ => false
    })
    assert(m.params.exists {
      case SingleMetaParameter("queryParam", "queryParam description", _, false) => true
      case _ => false
    })
    assert(m.params.exists {
      case MultiMetaParameter(_, true) => true
      case _ => false
    })
    assert(m.params.exists {
      case MultiMetaParameter(_, false) => true
      case _ => false
    })
  }

  "simplest route should not fail" in {
    val r = Endpoint.builder[Future, Request, Response]
      .get("/{param}") { () =>
        val r2: Response = Response("hello", "")
        Future successful r2
      }

    assert(r.handle(mr(r)).get == Response("hello", ""))
  }

  "passing request as parameter should work" in {
    val r = Endpoint.builder[Future, Request, Response]
      .withRequest
      .get("/{param}") { (req) =>
        assert(req.data == "hello")
        val r2: Response = Response("hello", "")
        Future successful r2
      }

    assert(r.handle(mr(r)).get == Response("hello", ""))
  }

  "passing unmatched path as parameter should work" in {
    val r = Endpoint.builder[Future, Request, Response]
      .withUnmatchedPath
      .get("/{param}") { (path) =>
        assert(path == "unmatchedPath")
        val r2: Response = Response("hello", "")
        Future successful r2
      }

    assert(r.handle(mr(r)).get == Response("hello", ""))
  }

  "path parameter parsing should work" - {
    val r = Endpoint.builder[Future, Request, Response]
      .pathParam[String]("p1", "p1 param")
      .pathParams[Params]
      .get("/{param}") { (p1, pp) =>
        assert(p1 == "p1 value")
        assert(pp.a == 42)
        assert(pp.b == "pp b")

        val r2: Response = Response("hello", "")
        Future successful r2
      }

    "valid case" in {
      assert(r.handle(mr(r).copy(pathParams = Map("p1" -> "p1 value", "a" -> "42", "b" -> "pp b"))).get == Response("hello", ""))
    }

    "can't parse parameter" in {
      assertThrows[RouteBadPathParameterException](r.handle(mr(r).copy(pathParams = Map("p1" -> "p1 value", "a" -> "boo", "b" -> "pp b"))).get == Response("hello", ""))
    }

    "parameter is missing (single)" in {
      assertThrows[RouteNoPathParameterException](r.handle(mr(r).copy(pathParams = Map("a" -> "42", "b" -> "pp b"))).get == Response("hello", ""))
    }

    "parameter is missing (multi)" in {
      assertThrows[RouteBadPathParameterException](r.handle(mr(r).copy(pathParams = Map("p1" -> "p1 value", "a" -> "42"))).get == Response("hello", ""))
    }
  }

  "query parameter parsing should work" - {
    "mandatory parameters" - {
      val r = Endpoint.builder[Future, Request, Response]
        .queryParam[String]("p1", "p1 param")
        .queryParams[Params]
        .get("/{param}") { (p1, pp) =>
          assert(p1 == "p1 value")
          assert(pp.a == 42)
          assert(pp.b == "pp b")

          val r2: Response = Response("hello", "")
          Future successful r2
        }

      "valid case" in {
        assert(r.handle(mr(r).copy(queryParams = Map("p1" -> "p1 value", "a" -> "42", "b" -> "pp b"))).get == Response("hello", ""))
      }

      "can't parse parameter" in {
        assertThrows[RouteBadQueryParameterException](r.handle(mr(r).copy(queryParams = Map("p1" -> "p1 value", "a" -> "boo", "b" -> "pp b"))).get == Response("hello", ""))
      }

      "parameter is missing (single)" in {
        assertThrows[RouteNoQueryParameterException](r.handle(mr(r).copy(queryParams = Map("a" -> "42", "b" -> "pp b"))).get == Response("hello", ""))
      }

      "parameter is missing (multi)" in {
        assertThrows[RouteBadQueryParameterException](r.handle(mr(r).copy(queryParams = Map("p1" -> "p1 value", "a" -> "42"))).get == Response("hello", ""))
      }
    }

    "optional parameters" - {
      val r = Endpoint.builder[Future, Request, Response]
        .queryParam[Boolean]("isNone", "test - expect Nones if true")
        .queryParam[Option[String]]("p1", "p1 param")
        .queryParams[Option[Params]]
        .get("/{param}") { (isNone, p1, pp) =>
          if (isNone) {
            assert(p1.isEmpty)
            assert(pp.isEmpty)
          } else {
            assert(p1 == Some("p1 value"))
            assert(pp.get.a == 42)
            assert(pp.get.b == "pp b")
          }
          val r2: Response = Response("hello", "")
          Future successful r2
        }

      "when defined" - {
        "valid case" in {
          assert(r.handle(mr(r).copy(queryParams = Map("isNone" -> "false", "p1" -> "p1 value", "a" -> "42", "b" -> "pp b"))).get == Response("hello", ""))
        }

        "can't parse parameter - interpreted as None" in {
          assert(r.handle(mr(r).copy(queryParams = Map("isNone" -> "true", "a" -> "boo", "b" -> "pp b"))).get == Response("hello", ""))
        }
      }

      "when missing" - {
        "valid case" in {
          assert(r.handle(mr(r).copy(queryParams = Map("isNone" -> "true"))).get == Response("hello", ""))
        }
      }
    }
  }

  "request body parsing should work" in {
    val r = Endpoint.builder[Future, Request, Response]
      .consumes[RequestBody1]
      .post("/") { (req) =>
        val r2: Response = Response(req.data + "-resp", "my-ct")
        Future successful r2
      }

    assert(r.handle(mr(r).copy(body = () => "test body".getBytes())).get == Response("test body-resp", "my-ct"))
  }

  "response body serialization should work" in {
    val r = Endpoint.builder[Future, Request, Response]
      .produces[ResponseBody1]
      .queryParam[String]("p", "content for response")
      .post("/") { (p) =>
        Future successful ResponseBody1(p + "-resp")
      }

    assert(r.handle(mr(r).copy(queryParams = Map("p" -> "resp-data"))).get == Response("resp-data-resp", "application/json"))
  }



  implicit class FutureHelper[A](f: Future[A]) {
    def get: A = Await.result(f, Duration.Inf)
  }

  private def mr(r: Endpoint[Future, Request, Response], req: Request = Request("hello")): MatchedRequest[Request] = endpoint.MatchedRequest(
    req = req,
    url = "/",
    meta = Meta(),
    unmatchedPath = "unmatchedPath",
    pathParams = Map.empty,
    queryParams = Map.empty,
    body = () => Array.empty
  )

}
