package me.scf37.fine.route.typeclass

trait RequestParams[T] {
  def parse(paramsMap: Map[String, String]): Either[Throwable, T]
}

object RequestParams {
  implicit def apply[T: RequestParams]: RequestParams[T] = implicitly

  implicit def optionInstance[T: RequestParams]: RequestParams[Option[T]] =
    (value: Map[String, String]) => Right(RequestParams[T].parse(value).toOption)

}
