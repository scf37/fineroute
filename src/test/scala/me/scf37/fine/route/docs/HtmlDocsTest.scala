package me.scf37.fine.route.docs

import java.nio.file.{Files, Paths}

import cats.MonadError
import cats.implicits._
import me.scf37.fine.route.RouteBuilder
import me.scf37.fine.route.model.{Request, Response}
import me.scf37.fine.route.typeclass._
import org.scalatest.FreeSpec

@Description("this is test body, suited both for request and response bodies")
case class TestBody(
  @Description("this is int (ro)") @ReadOnly
  i: Int,
  @Description("this is string")
  s: String,
  @Description("enum example")
  e: MyEnum,
  @Description("this is nested X")
  x: X,
  @Description("this is nested array of X")
  xarr: Seq[X]
)

object TestBody {
  implicit val req = new RequestBody[TestBody] {
    override def parse(request: Array[Byte]): Either[Throwable, TestBody] = ???

    override def contentType: String = "application/test-request"
  }

  implicit val resp = new ResponseBody[TestBody] {
    override def contentType: String = "application/test-response"

    override def write(body: TestBody): Either[Throwable, Array[Byte]] = ???
  }
}

@Description("this is inner class")
case class X(
  @Description("this is ii (ro)") @ReadOnly
  ii: Int,
  @Description("this is ss")
  ss: String
)

@Description("parameters - suited both for path and query")
case class Params(
  @Description("this is X")
  x: Int,
  @Description("this is Y")
  y: Int,
  @Description("this is enum!")
  e: MyEnum
)

@Description("Sealed trait enumeration description")
sealed trait MyEnum
object MyEnum {
  case object First extends MyEnum
  case object Second extends MyEnum
  case object Third extends MyEnum

}

object Params {
  implicit val p = new RequestParams[Params] {
    override def parse(paramsMap: Map[String, String]): Either[Throwable, Params] = ???
  }
}

class HtmlDocsTest extends FreeSpec {
  trait EitherRoute extends RouteBuilder[Either[Throwable, ?], Request, Response] {
    override protected def monadError: MonadError[Either[Throwable, ?], Throwable] = implicitly[MonadError[Either[Throwable, ?], Throwable]]
    override protected def routeHttpRequest: RouteHttpRequest[Request] = implicitly[RouteHttpRequest[Request]]
    override protected def routeHttpResponse: RouteHttpResponse[Response] = implicitly[RouteHttpResponse[Response]]
  }

  val r = new EitherRoute {
    endpoint
      .summary("first GET endpoint")
      .description("this is long <s>HTML</s> description.<br>Second line here.")
      .tags("tag1")
      .pathParam[Int]("p1", "path param 1 description")
      .pathParam[Long]("p2", "path param 2 description")
      .queryParam[String]("q1", "query param 1 description")
      .queryParam[Option[Boolean]]("q2", "query param 2 description (opt)")
      .produces[TestBody]
      .get("/e1/{p1}/{p2}")((a, b, c, d) => ???)

    endpoint
      .summary("first POST endpoint")
      .description("this is long <s>HTML</s> description.<br>Second line here.")
      .tags("tag1")
      .pathParam[Int]("p1", "path param 1 description")
      .pathParam[Long]("p2", "path param 2 description")
      .queryParam[Option[String]]("q1", "query param 1 description (opt)")
      .queryParam[Boolean]("q2", "query param 2 description")
      .consumes[TestBody]
      .produces[TestBody]
      .post("/e1/{p1}/{p2}")((a, b, c, d, e) => ???)

    endpoint
      .summary("PUT endpoint")
      .description("PUT endpoint description")
      .tags("tag2")
      .pathParams[Params]
      .consumes[TestBody]
      .produces[TestBody]
      .put("/e2/{x}/{y}/{e}")((a, b) => ???)

    endpoint
      .summary("PATCH endpoint")
      .description("PATCH endpoint description")
      .tags("tag2")
      .pathParams[Params]
      .consumes[TestBody]
      .produces[TestBody]
      .patch("/e2/{x}/{y}")((a, b) => ???)

    endpoint
      .summary("DELETE endpoint")
      .description("DELETE endpoint description")
      .tags("tag2")
      .queryParams[Params]
      .consumes[TestBody]
      .produces[TestBody]
      .delete("/e2/delete")((a, b) => ???)
  }.build()

  "write route docs" in {
    val html = HtmlDocs.generateHtml("<h1>Hello</h1>", Map("tag1" -> "tag1 description", "tag2" -> "tag2 description"), r.meta)
    Files.write(Paths.get("1.html"), html.getBytes("utf-8"))
  }


}
