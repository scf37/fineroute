package me.scf37.fine.route.matcher

import org.scalatest.freespec.AnyFreeSpec

class MatcherTest extends AnyFreeSpec {
  "simple paths match" in {
    var pt: PathNode[String] = EmptyPathNode[String]()
    def check(path: Array[String]): Unit = {
      val v = if (path.isEmpty) "empty" else path.mkString
      assert(pt.get(path.toList) == Some(Matched(Nil, Nil, v)))
    }

    def checkFalse(path: Array[String]): Unit = {
      assert(pt.get(path.toList).isEmpty)
    }

    def add(path: Array[String]): Unit = {
      val v = if (path.isEmpty) "empty" else path.mkString
      pt = pt.add(path.toList, v)
    }

    val paths = Seq(
      Array.empty[String],
      Array("a"),
      Array("a", "b", "c"),
      Array("a", "b"),
      Array("a", "e"),
      Array("a", "e", "f", "g")
    )

    paths.foreach(add)
    paths.foreach(check)
    checkFalse(Array("b"))
    checkFalse(Array("a", "x"))
    checkFalse(Array("a", "b" , "d"))
  }

  "path variable test" in {
    var pt: PathNode[String] = EmptyPathNode[String]()
    def add(path: List[String]): Unit = {
      val v = if (path.isEmpty) "empty" else path.map(_.replaceAll("[{}]", "")).mkString
      pt = pt.add(path, v)
    }

    def check(path: List[String], params: String) = {
      val r = pt.get(path)
      val v = if (path.isEmpty) "empty" else path.mkString
      val paramsList = params.map(c => c.toString -> c.toString).toList
      assert(r == Some(Matched(paramsList,  Nil, v)))
    }

    def checkFalse(path: List[String]) = {
      val r = pt.get(path)
      assert(r.isEmpty)
    }

    add(List())
    add(List("{a}"))
    add(List("{a}", "b"))
    add(List("{a}", "b", "{c}"))
    add(List("{a}", "d", "{e}"))
    add(List("{a}", "f", "g"))

    check(List(), "")
    check(List("a"), "a")
    check(List("a", "b"), "a")
    check(List("a", "b", "c"), "ac")
    check(List("a", "d", "e"), "ae")
    check(List("a", "f", "g"), "a")

    checkFalse(List("a", "c"))
    checkFalse(List("a", "b", "c", "d"))

    pt = EmptyPathNode[String]()
    add(List("x"))
    add(List("x", "{y}"))

    check(List("x"), "")
    check(List("x", "y"), "y")
  }

  "star test" in {
    var pt: PathNode[String] = EmptyPathNode[String]()
    def add(path: List[String]): Unit = {
      val v = if (path.isEmpty) "empty" else path.mkString
      pt = pt.add(path, v)
    }

    def check(path: List[String], params: String, unmatched: String, value: String) = {
      val r = pt.get(path)
      val paramsList = params.map(c => c.toString -> c.toString).toList
      val unmatchedList = unmatched.map(c => c.toString).toList
      assert(r == Some(Matched(paramsList, unmatchedList, value)))
    }

    add(List("*"))

    check(List(), "", "", "*")
    check(List("a"), "", "a", "*")
    check(List("a", "b"), "", "ab", "*")

    pt = EmptyPathNode[String]()
    add(List("a"))
    add(List("b", "*"))
    add(List("c", "{d}", "*"))

    check(List("a"), "", "", "a")
    check(List("b"), "", "", "b*")
    check(List("b", "x"), "", "x", "b*")
    check(List("c", "d"), "d", "", "c{d}*")
    check(List("c", "d", "x"), "d", "x", "c{d}*")
    check(List("c", "d", "x", "y", "z"), "d", "xyz", "c{d}*")
  }

  "duplicates should fail" - {
    "same simple paths" in assertThrows[IllegalArgumentException] {
      EmptyPathNode[String]()
        .add(List("a"), "")
        .add(List("a"), "")
    }

    "same var paths" in assertThrows[IllegalArgumentException] {
      EmptyPathNode[String]()
        .add(List("{a}"), "")
        .add(List("{a}"), "")
    }

    "simple and var path" in assertThrows[IllegalArgumentException] {
      EmptyPathNode[String]()
        .add(List("a"), "")
        .add(List("{a}"), "")
    }

    "star and simple path" in assertThrows[IllegalArgumentException] {
      EmptyPathNode[String]()
        .add(List("*"), "")
        .add(List("a"), "")
    }

    "star and var path" in assertThrows[IllegalArgumentException] {
      EmptyPathNode[String]()
        .add(List("*"), "")
        .add(List("{a}"), "")
    }
  }




}
