package me.scf37.fine.route.typeclass

trait RouteHttpResponse[Resp] {
  def write(arr: Array[Byte], contentType: String): Either[Throwable, Resp]
}

object RouteHttpResponse {
  def apply[Resp: RouteHttpResponse]: RouteHttpResponse[Resp] = implicitly
}
