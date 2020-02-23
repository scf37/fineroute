package me.scf37.fine.route.typeclass

/**
 * Typeclass for HTTP response, allowing route to serialize custom classes to response.
 *
 * @tparam Resp route HTTP response type
 */
trait RouteHttpResponse[Resp] {
  def write(arr: Array[Byte], contentType: String): Either[Throwable, Resp]
}

object RouteHttpResponse {
  def apply[Resp: RouteHttpResponse]: RouteHttpResponse[Resp] = implicitly
}
