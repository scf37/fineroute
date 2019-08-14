package me.scf37.fine.route.docs.impl

import org.scalatest.FreeSpec

import scala.reflect.runtime.universe._

case class X(
  a: String,
  b: Option[Int],
  c: Seq[X]
)

sealed trait E
object E {
  case object A extends E
  case object B extends E
}

class IntrospectorTest extends FreeSpec {
  "case classes" in {
    assert(Introspector.introspect(typeOf[X]) ==
      ICaseClass("X", Seq(),
        Seq(
          IField("a", Seq(),IPrimitive("string")),
          IField("b", Seq(), IOption(IPrimitive("int"))),
          IField("c", Seq(), IList(ICaseClassRef("X")))
        )
      )
    )
  }

  "sealed trait enums" in {
    assert(Introspector.introspect(typeOf[E]) == IEnum("E", Nil, Seq("A", "B")))
  }
}
