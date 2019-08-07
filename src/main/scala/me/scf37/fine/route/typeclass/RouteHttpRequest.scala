package me.scf37.fine.route.typeclass

trait RouteHttpRequest[Req] {
  def url: String
  def pathParams: Map[String, String]
  def queryParams: Map[String, String]
  def body: Array[Byte]
}
