package me.scf37.fine.route.meta

import scala.reflect.runtime.universe._

case class Meta(
  method: MetaMethod = MetaMethod.GET,
  pathPattern: String = "",
  tags: List[String] = Nil,
  summary: String = "",
  description: String = "",
  consumes: Option[MetaBody] = None,
  produces: Option[MetaBody] = None,
  resultCodes: List[MetaResultCode] = Nil,
  params: List[MetaParameter] = Nil,
  routeData: Option[Any] = None
)

case class MetaResultCode(code: Int, desc: String, body: TypeTag[_])

case class MetaBody(mime: String, body: TypeTag[_])

sealed trait MetaParameter
case class SingleMetaParameter(name: String, desc: String, mf: TypeTag[_], inPath: Boolean) extends MetaParameter
case class MultiMetaParameter(mf: TypeTag[_], inPath: Boolean) extends MetaParameter

sealed trait MetaMethod

object MetaMethod {
  object GET extends MetaMethod
  object PUT extends MetaMethod
  object POST extends MetaMethod
  object DELETE extends MetaMethod
  object PATCH extends MetaMethod
}
