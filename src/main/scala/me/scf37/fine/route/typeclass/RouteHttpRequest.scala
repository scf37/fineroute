package me.scf37.fine.route.typeclass

import me.scf37.fine.route.meta.MetaMethod

/**
 * Typeclass for HTTP request, allowing Route to operate on abstract request types
 *
 * @tparam Req route HTTP request type
 */
trait RouteHttpRequest[Req] {
  def method(req: Req): MetaMethod
  def url(req: Req): String
  def queryParams(req: Req): Map[String, String]
  def body(req: Req): () => Array[Byte]
}

object RouteHttpRequest {
  def apply[Req: RouteHttpRequest]: RouteHttpRequest[Req] = implicitly
}