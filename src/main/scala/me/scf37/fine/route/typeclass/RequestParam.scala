package me.scf37.fine.route.typeclass

trait RequestParam[T] {
  def parse(value: String): Either[Throwable, T]
}

object RequestParam {
  implicit def apply[T: RequestParam]: RequestParam[T] = implicitly

  implicit def optionInstance[T: RequestParam]: RequestParam[Option[T]] =
    (value: String) => Right(RequestParam[T].parse(value).toOption)

  implicit val stringInstance: RequestParam[String] = (value: String) => Right(value)

  implicit val longInstance: RequestParam[Long] = (value: String) =>
    try Right(value.toLong) catch {case _: NumberFormatException => Left(new NumberFormatException("not a long number"))}

  implicit val intInstance: RequestParam[Int] = (value: String) =>
    try Right(value.toInt) catch {case _: NumberFormatException => Left(new NumberFormatException("not an int number"))}

  implicit val booleanInstance: RequestParam[Boolean] = (value: String) =>
    try Right(value.toBoolean) catch {case _: NumberFormatException => Left(new NumberFormatException("not a boolean"))}

}

