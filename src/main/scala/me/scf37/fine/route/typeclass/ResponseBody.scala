package me.scf37.fine.route.typeclass

trait ResponseBody[T] {
  def contentType: String
  def write(body: T): Either[Throwable, Array[Byte]]
}

object ResponseBody {
  implicit def apply[T: ResponseBody]: ResponseBody[T] = implicitly
}
