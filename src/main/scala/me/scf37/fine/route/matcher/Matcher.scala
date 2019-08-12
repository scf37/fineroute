package me.scf37.fine.route.matcher

import cats.Monad
import cats.MonadError
import cats.implicits._
import me.scf37.fine.route.RouteUnmatchedException
import me.scf37.fine.route.endpoint.Endpoint
import me.scf37.fine.route.endpoint.MatchedRequest
import me.scf37.fine.route.meta.MetaMethod
import me.scf37.fine.route.typeclass.RouteHttpRequest

/**
 * Matcher - efficiently matches request against set of endpoints
 *
 * @param roots
 * @tparam F effect
 * @tparam Req HTTP request
 * @tparam Resp HTTP response
 */
case class Matcher[F[_]: MonadError[?[_], Throwable], Req: RouteHttpRequest, Resp](
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
  def addMatcher(m: Matcher[F, Req, Resp]): Matcher[F, Req, Resp] = {
    copy(roots = roots.combine(m.roots))
  }

  /**
   * Find endpoint by request
   *
   * @param req HTTP request
   * @return matched request and endpoint or `RouteUnmatchedException`
   */
  def matchRequest(req: Req): F[(MatchedRequest[Req], Endpoint[F, Req, Resp])] = {
    val method = RouteHttpRequest[Req].method(req)

    roots.get(method) match {
      case None => MonadError[F, Throwable].raiseError(RouteUnmatchedException)

      case Some(pathNode) =>
        val url = RouteHttpRequest[Req].url(req)

        pathNode.get(extractPathParts(url)) match {
          case None => MonadError[F, Throwable].raiseError(RouteUnmatchedException)

          case Some(matched) =>
            Monad[F].pure(
              makeRequest(req, matched.unmatched.mkString("/"), matched.params.toMap, matched.value) -> matched.value
            )
        }
    }
  }

  private def extractPathParts(url: String): List[String] = {
    val path = url.indexOf("?") match {
      case -1 => url
      case i => url.substring(0, i)
    }

    // strip tailing /-s, don't strip heading /
    var i = path.length - 1
    while (i > 0 && path(i) == '/') i -= 1

    path.substring(1, i).split("/").toList
  }

  private def makeRequest(
    req: Req,
    unmatchedPath: String,
    pathParams: Map[String, String],
    e: Endpoint[F, Req, Resp]
  ) = {
    MatchedRequest[Req](
      req = req,
      url = RouteHttpRequest[Req].url(req),
      meta = e.meta,
      unmatchedPath = unmatchedPath,
      pathParams = pathParams,
      queryParams = RouteHttpRequest[Req].queryParams(req),
      body = RouteHttpRequest[Req].body(req)
    )
  }
}
