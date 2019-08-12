package me.scf37.fine.route.endpoint

import me.scf37.fine.route.meta.Meta

case class MatchedRequest[Req](
  req: Req,
  url: String,
  meta: Meta,
  unmatchedPath: String,
  pathParams: Map[String, String],
  queryParams: Map[String, String],
  body: () => Array[Byte]
)
