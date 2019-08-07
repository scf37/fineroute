package me.scf37.fine.route.typeclass

import me.scf37.fine.route.RouteException

trait RequestBody[T] {
  def parse(request: Array[Byte]): Either[RouteException, T]
  def contentType: String
}

object RequestBody {
  implicit def apply[T: RequestBody]: RequestBody[T] = implicitly
}
