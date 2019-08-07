package me.scf37.fine.route

import scala.util.control.NoStackTrace

sealed trait RouteException extends Exception with NoStackTrace

object RouteUnmatchedException extends Exception("No handler found for this request") with RouteException


sealed trait RouteParamParseException extends RouteException

case class RouteNoPathParameterException(param: String, template: String)
  extends Exception(s"There is no path parameter '$param' in path template '$template'") with RouteParamParseException

case class RouteNoQueryParameterException(param: String, url: String)
  extends Exception(s"There is no query parameter '$param' in URL '$url'") with RouteParamParseException

case class RouteBadPathParameterException(param: String, value: String, reason: String, cause: Throwable)
  extends Exception(s"Can't parse '$param'='$value': $reason", cause) with RouteParamParseException

case class RouteBadQueryParameterException(param: String, value: String, reason: String, cause: Throwable)
  extends Exception(s"Can't parse '$param'='$value': $reason") with RouteParamParseException