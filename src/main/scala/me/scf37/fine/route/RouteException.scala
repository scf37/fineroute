package me.scf37.fine.route

import scala.util.control.NoStackTrace

/** Base trait for all route evaluation errors */
sealed trait RouteException extends Exception with NoStackTrace

/** Returned by route effect if request is not matched */
object RouteUnmatchedException extends Exception("No handler found for this request") with RouteException


/** Base trait for route path/query parameters parsing errors */
sealed trait RouteParamParseException extends RouteException

/** path template is missing required path parameter */
case class RouteNoPathParameterException(param: String, template: String)
  extends Exception(s"There is no path parameter '$param' in path template '$template'") with RouteParamParseException

/** request is missing required query parameter */
case class RouteNoQueryParameterException(param: String, url: String)
  extends Exception(s"There is no query parameter '$param' in URL '$url'") with RouteParamParseException

/** path parameter can't be converted to target type */
case class RouteBadPathParameterException(param: String, value: String, reason: String, cause: Throwable)
  extends Exception(s"Can't parse '$param'='$value': $reason", cause) with RouteParamParseException

/** query parameter can't be converted to target type */
case class RouteBadQueryParameterException(param: String, value: String, reason: String, cause: Throwable)
  extends Exception(s"Can't parse '$param'='$value': $reason") with RouteParamParseException
