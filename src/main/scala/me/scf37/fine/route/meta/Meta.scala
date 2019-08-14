package me.scf37.fine.route.meta

import scala.reflect.runtime.universe._

/**
 * Route endpoint meta information. Used to generate documentation and clients.
 * Most terminology matches Swagger-s.
 */
case class Meta(
  /** HTTP method */
  method: MetaMethod = MetaMethod.GET,

  /** path pattern */
  pathPattern: String = "",

  /** endpoint tags */
  tags: List[String] = Nil,

  /** Short endpoint summary */
  summary: String = "",

  /** Long endpoint description */
  description: String = "",

  /** for PUT/POST/PATCH, information on request body type */
  consumes: Option[MetaBody] = None,

  /** information on response body type */
  produces: Option[MetaBody] = None,

  /** information on HTTP result codes */
  resultCodes: List[MetaResultCode] = Nil,

  /** information on query and path parameters */
  params: List[MetaParameter] = Nil,

  /** any user-specific data, for tooling working with this meta */
  routeData: List[Any] = Nil
)

case class MetaResultCode(code: Int, desc: String, body: TypeTag[_])

case class MetaBody(mime: String, body: TypeTag[_])

sealed trait MetaParameter {
  def inPath: Boolean
  def mf: TypeTag[_]
}
case class SingleMetaParameter(name: String, desc: String, mf: TypeTag[_], inPath: Boolean) extends MetaParameter
case class MultiMetaParameter(mf: TypeTag[_], inPath: Boolean) extends MetaParameter

sealed trait MetaMethod

object MetaMethod {
  object GET extends MetaMethod {
    override def toString: String = "GET"
  }
  object PUT extends MetaMethod {
    override def toString: String = "PUT"
  }
  object POST extends MetaMethod {
    override def toString: String = "POST"
  }
  object DELETE extends MetaMethod {
    override def toString: String = "DELETE"
  }
  object PATCH extends MetaMethod {
    override def toString: String = "PATCH"
  }
}
