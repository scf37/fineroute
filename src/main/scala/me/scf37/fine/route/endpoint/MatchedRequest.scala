package me.scf37.fine.route.endpoint

import me.scf37.fine.route.meta.Meta

/**
 * HTTP request after route matcher
 *
 * @tparam Req route HTTP request type
 */
case class MatchedRequest[Req](
  /** Original HTTP request */
  req: Req,

  /** HTTP request url, without protocol but with query parameters */
  url: String,

  /** Matched endpoint meta information */
  meta: Meta,

  /** Unmatched path suffix, if any */
  unmatchedPath: String,

  /** Path parameters */
  pathParams: Map[String, String],

  /** Query parameters */
  queryParams: Map[String, String],

  /** Request body */
  body: () => Array[Byte]
) {
  def map[Req2](f: Req => Req2): MatchedRequest[Req2] = MatchedRequest[Req2](
    req = f(req),
    url = url,
    meta = meta,
    unmatchedPath = unmatchedPath,
    pathParams = pathParams,
    queryParams = queryParams,
    body = body
  )

}
