package me.scf37.fine.route.typeclass

/**
 * Typeclass for request body class, for .consumes[T]
 *
 * @tparam T
 */
trait RequestBody[T] {
  /** parse request body to T */
  def parse(request: Array[Byte]): Either[Throwable, T]

  /** Expected content/type */
  def contentType: String
}

object RequestBody {
  implicit def apply[T: RequestBody]: RequestBody[T] = implicitly
}
