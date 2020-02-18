package me.scf37.fine.route.typeclass

/**
 * Typeclass for serialization of Response, used by .produces[T]
 *
 * @tparam T
 */
trait ResponseBody[T] {
  /** output content type */
  def contentType: String

  /** Serialize T to bytes */
  def write(body: T): Either[Throwable, Array[Byte]]
}

object ResponseBody {
  implicit def apply[T: ResponseBody]: ResponseBody[T] = implicitly
}
