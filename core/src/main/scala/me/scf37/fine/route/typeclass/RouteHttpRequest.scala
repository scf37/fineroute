package me.scf37.fine.route.typeclass

import me.scf37.fine.route.endpoint.meta.MetaMethod

/**
 * Typeclass for HTTP request, allowing Route to operate on abstract request types
 *
 * @tparam Req route HTTP request type
 */
trait RouteHttpRequest[Req] {
  /** HTTP method */
  def method(req: Req): MetaMethod

  /** request uri, with query part but without protocol. Should start with '/' */
  def url(req: Req): String

  /** request query params */
  def queryParams(req: Req): Map[String, String]

  /** request body */
  def body(req: Req): () => Array[Byte]
}

object RouteHttpRequest {
  def apply[Req: RouteHttpRequest]: RouteHttpRequest[Req] = implicitly
}