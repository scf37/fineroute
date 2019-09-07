package me.scf37.fine.route

import me.scf37.fine.route.endpoint.meta.Meta

/**
 * Modify endpoint Meta when applying filter to route.
 *
 * When filter function passed to Route map, rmap, mapK, compose or compose0 implements this trait,
 * meta of all endpoints of that route will be mapped by filterMeta function.
 */
trait MetaFilter {
  def filterMeta(meta: Meta): Meta
}
