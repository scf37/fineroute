package me.scf37.fine.route

import cats.kernel.Monoid
import me.scf37.fine.route.endpoint.meta.Meta

/**
  * Metadata on route
  *
  * @param endpointMetas metadata every route endpoint
  */
case class RouteMeta(
  endpointMetas: Seq[Meta]
) {
  def ++(other: RouteMeta): RouteMeta = RouteMeta(endpointMetas ++ other.endpointMetas)

  override def toString: String = "Route\n" + endpointMetas.groupBy(_.tag).map { case (tag, endp) =>
    "  " + tag + "\n" + endp.sortBy(e => (e.pathPattern, e.method.toString)).map("    " + _).mkString("\n")
  }.mkString("\n")
}

object RouteMeta {
  implicit val monoidInstance = new Monoid[RouteMeta] {
    override def empty: RouteMeta = RouteMeta(Nil)

    override def combine(x: RouteMeta, y: RouteMeta): RouteMeta = RouteMeta(x.endpointMetas ++ y.endpointMetas)
  }
}