package me.scf37.fine.route.matcher

import me.scf37.fine.route.endpoint.meta.MetaMethod

/**
 * Matcher - efficiently matches request against set of endpoints
 *
 * @param roots
 * @tparam E endpoint type
 */
case class Matcher[E](
  private val roots: List[Map[MetaMethod, PathNode[E]]] = List(Map.empty[MetaMethod, PathNode[E]])
) {

  /**
   * Add new endpoint to this matcher
   *
   * @param e
   * @return matcher with this endpoint
   */
  def addEndpoint(method: MetaMethod, pathPattern: String, e: E): Matcher[E] = {
    if (roots.length != 1) {
      throw new IllegalArgumentException("addEndpoint is unsupported to composite matchers (composed via andThen)")
    }
    val node = roots.head.getOrElse(method, PathNode[E])
    val newRoots = roots.head + (method -> node.add(extractPathParts(pathPattern), e))

    copy(roots = List(newRoots))
  }

  /**
   * Combine two matchers
   *
   * @param m
   * @return
   */
  def combine(m: Matcher[E]): Matcher[E] = {
    val empty = Map.empty[MetaMethod, PathNode[E]]
    copy(roots = roots.zipAll(m.roots, empty, empty).map { case (root1, root2) =>
      (root1.keySet ++ root2.keySet).map { method =>
        method -> (root1.getOrElse(method, PathNode[E]) combine root2.getOrElse(method, PathNode[E]))
      }.toMap
    })
  }

  def map[E2](f: E => E2): Matcher[E2] = {
    copy(roots = roots.map(root => root.map(kv => kv._1 -> kv._2.map(f))))
  }

  def andThen(matcher: Matcher[E]): Matcher[E] = Matcher(roots ++ matcher.roots)

  /**
   * Find endpoint by request
   *
   * @param method HTTP request
   * @param uri URI to match
   * @return matched request and endpoint or `RouteUnmatchedException`
   */
  def matchRequest(method: MetaMethod, uri: String): Option[Matched[E]] = {
    var l = roots

    while (l != Nil) {
      l.head.get(method) match {
        case None =>

        case Some(pathNode) =>
          pathNode.get(extractPathParts(uri)) match {
            case None =>

            case Some(matched) =>
              return Some(matched)
          }
      }

      l = l.tail
    }

    None
  }

  /**
   * @return all endpoints known to this matcher
   */
  def endpoints: List[E] = roots.flatMap(_.values.flatMap(_.values))

  private def extractPathParts(url: String): List[String] = {
    val path = url.indexOf("?") match {
      case -1 => url
      case i => url.substring(0, i)
    }

    // strip tailing /-s, don't strip heading /
    var i = path.length - 1
    while (i > 0 && path(i) == '/') i -= 1

    path.substring(1, i + 1).split("/").toList
  }
}
