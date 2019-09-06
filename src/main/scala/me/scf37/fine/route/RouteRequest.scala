package me.scf37.fine.route

import me.scf37.fine.route.endpoint.meta.MetaMethod

case class RouteRequest(
  /** HTTP method */
  method: MetaMethod,

  /** request uri, with query part but without protocol. Should start with '/' */
  url: String
)
