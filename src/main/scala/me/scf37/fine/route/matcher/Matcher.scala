package me.scf37.fine.route.matcher

import cats.implicits._
import cats.{Monad, MonadError, ~>}
import me.scf37.fine.route.endpoint.Endpoint
import me.scf37.fine.route.endpoint.meta.MetaMethod
import me.scf37.fine.route.{RouteRequest, RouteUnmatchedException}

/**
 * Matcher - efficiently matches request against set of endpoints
 *
 * @param roots
 * @tparam F effect
 * @tparam Req HTTP request
 * @tparam Resp HTTP response
 */
case class Matcher[F[_]: MonadError[?[_], Throwable], Req, Resp](
  private val roots: Map[MetaMethod, PathNode[Endpoint[F, Req, Resp]]] = Map.empty[MetaMethod, PathNode[Endpoint[F, Req, Resp]]]
) {

  /**
   * Add new endpoint to this matched
   *
   * @param e
   * @return matcher with this endpoint
   */
  def addEndpoint(e: Endpoint[F, Req, Resp]): Matcher[F, Req,Resp] = {
    val method = e.meta.method
    val node = roots.getOrElse(method, PathNode[Endpoint[F, Req, Resp]])
    val newRoots = roots + (method -> node.add(extractPathParts(e.meta.pathPattern), e))

    copy(roots = newRoots)
  }

  /**
   * Combine two matchers
   *
   * @param m
   * @return
   */
  def combine(m: Matcher[F, Req, Resp]): Matcher[F, Req, Resp] = {
    copy(roots = roots.combine(m.roots))
  }

  def map[Resp2](f: Resp => Resp2): Matcher[F, Req, Resp2] = {
    Matcher.mk(endpoints.map(e => e.map(f)):_*)
  }

  def rmap[Req2](f: Req2 => Req): Matcher[F, Req2, Resp] = {
    Matcher.mk(endpoints.map(e => e.rmap(f)):_*)
  }

  def mapK[G[_]: MonadError[?[_], Throwable]](f: F ~> G): Matcher[G, Req, Resp] = {
    Matcher.mk(endpoints.map(e => e.mapK(f)):_*)
  }

  def compose[Req2, Resp2](filter: (Req => F[Resp]) => (Req2 => F[Resp2])) =
    Matcher.mk(endpoints.map(e => e.compose(filter)):_*)

  /**
   * Find endpoint by request
   *
   * @param req HTTP request
   * @return matched request and endpoint or `RouteUnmatchedException`
   */
  def matchRequest(req: RouteRequest): F[Matched[Endpoint[F, Req, Resp]]] = {

    roots.get(req.method) match {
      case None => MonadError[F, Throwable].raiseError(RouteUnmatchedException)

      case Some(pathNode) =>
        val url = req.url
        if (url.isEmpty)
          MonadError[F, Throwable].raiseError(RouteUnmatchedException)
        else
          pathNode.get(extractPathParts(url)) match {
            case None => MonadError[F, Throwable].raiseError(RouteUnmatchedException)

            case Some(matched) =>
              Monad[F].pure(matched
                //makeRequest(req, matched.unmatched.mkString("/"), matched.params.toMap, matched.value) -> matched.value
              )
          }
    }
  }

  /**
   * @return all endpoints known to this matcher
   */
  def endpoints: List[Endpoint[F, Req, Resp]] = roots.values.flatMap(_.values).toList

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

object Matcher {
  def mk[F[_]: MonadError[?[_], Throwable], Req, Resp](endpoints: Endpoint[F, Req, Resp]*): Matcher[F, Req, Resp] =
    endpoints.foldLeft(Matcher[F, Req, Resp]())((m, e) => m.addEndpoint(e))
}