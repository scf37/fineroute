package me.scf37.fine.route.typeclass

import me.scf37.fine.route.meta.MetaMethod

trait RouteHttpRequest[Req] {
  def method(req: Req): MetaMethod
  def url(req: Req): String
  def pathParams(req: Req): Map[String, String]
  def queryParams(req: Req): Map[String, String]
  def body(req: Req): () => Array[Byte]
}

object RouteHttpRequest {
  def apply[Req: RouteHttpRequest]: RouteHttpRequest[Req] = implicitly
}