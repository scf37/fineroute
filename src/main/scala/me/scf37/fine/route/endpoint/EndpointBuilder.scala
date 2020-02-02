package me.scf37.fine.route.endpoint

import me.scf37.fine.route.endpoint.meta.Meta
import me.scf37.fine.route.typeclass.RequestBody
import me.scf37.fine.route.typeclass.RequestParam
import me.scf37.fine.route.typeclass.RequestParams
import me.scf37.fine.route.typeclass.ResponseBody

import scala.reflect.runtime.universe._

/**
 * Endpoint builder.
 * Main feature is typed handler - parameter types and order correspond with builder method invocations.
 *
 * @tparam F Route effect
 * @tparam Req Route HTTP request type
 * @tparam Resp Route HTTP response type
 * @tparam Produces response type
 * @tparam Handler handler function type
 * @tparam NextBuilder type of builder with N+1 arguments
 * @tparam RespBuilder type of builder with N arguments but different response type
 */
trait EndpointBuilder[F[_], Req, Resp, Produces, Handler, NextBuilder[_], RespBuilder[_]] {
  private[endpoint] type Self = EndpointBuilder[F, Req, Resp, Produces, Handler, NextBuilder, RespBuilder]

  /** Add request object to handler */
  def withRequest: NextBuilder[Req]

  /** Add unmatched path to handler. It is non-empty if star path pattern is used */
  def withUnmatchedPath: NextBuilder[String]

  /** Manually change endpoint Meta */
  def meta(f: Meta => Meta): Self

  /** Register path parameter, parsed from single path parameter */
  def pathParam[T: TypeTag: RequestParam](name: String, description: String): NextBuilder[T]

  /** Register path parameter, parsed from multiple path parameters */
  def pathParams[T: TypeTag: RequestParams]: NextBuilder[T]

  /** Register query parameter, parsed from single query parameter */
  def queryParam[T: TypeTag: RequestParam](name: String, description: String): NextBuilder[T]

  /** Register query parameter, parsed from multiple query parameters */
  def queryParams[T: TypeTag: RequestParams]: NextBuilder[T]

  /** Register request body type */
  def consumes[T: TypeTag: RequestBody]: NextBuilder[T]

  /** Register response body type */
  def produces[T: TypeTag: ResponseBody]: RespBuilder[T]

  /** Add endpoint tags */
  def tag(tag: String): Self

  def secondaryTag(name: String, description: String = "", bgColor: String = "#555555"): Self

  /** Set short endpoint summary */
  def summary(value: String): Self

  /** Set long endpoint description */
  def description(value: String): Self

  /** Register endpoint HTTP result code */
  def resultCode[T: TypeTag](code: Int, description: String): Self

  /** Add custom user data */
  def routeData(value: Any): Self

  def tagDescription(tag: String, description: String): this.type

  /** Build GET endpoint */
  def get(pathPattern: String)(h: Handler): Endpoint[F, Req, Resp]

  /** Build PUT endpoint */
  def put(pathPattern: String)(h: Handler): Endpoint[F, Req, Resp]

  /** Build POST endpoint */
  def post(pathPattern: String)(h: Handler): Endpoint[F, Req, Resp]

  /** Build DELETE endpoint */
  def delete(pathPattern: String)(h: Handler): Endpoint[F, Req, Resp]

  /** Build PATCH endpoint */
  def patch(pathPattern: String)(h: Handler): Endpoint[F, Req, Resp]
}
