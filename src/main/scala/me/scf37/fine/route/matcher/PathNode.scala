package me.scf37.fine.route.matcher

import cats.implicits._
import cats.kernel.CommutativeMonoid

case class Matched[V](
  /** path parameters, key is var name from template, value is matched value */
  params: List[(String, String)],
  /** list of unmatched path parts, in case of star match */
  unmatched: List[String],
  /** value */
  value: V
)

/**
 * Tree of path nodes, annotated by V
 * Supports fast lookup of paths
 * Supported path parts types:
 * - simple string - exact match
 * - {var} - variable, matches any single path part
 * - * - matches any number of path parts
 *
 * @tparam V
 */
sealed trait PathNode[V] {
  /**
   * look up path node in this tree by path
   *
   * @param path path
   * @return Matched if there is annotated node identified by path `path`
   */
  def get(path: List[String]): Option[Matched[V]]

  /**
   *
   * add path template to this node
   *
   * @param path path to add
   * @param value value to add
   * @return
   */
  def add(path: List[String], value: V): PathNode[V]

  /**
   * Combine this node with other node, merging paths
   * @param node node to combine with
   * @return node containing paths from both nodes or exception on conflict
   */
  def combine(node: PathNode[V]): PathNode[V]

  /**
   * @return value of this node, if set
   */
  def value: Option[V]

  /**
   * @return all values from this tree
   */
  def values: List[V]
}

object PathNode {
  /** make empty root node */
  def apply[V]: PathNode[V] = EmptyPathNode[V]()

  implicit def m[V]: CommutativeMonoid[PathNode[V]] = new CommutativeMonoid[PathNode[V]] {
    override def empty: PathNode[V] = PathNode[V]

    override def combine(x: PathNode[V], y: PathNode[V]): PathNode[V] = x.combine(y)
  }

  private[matcher] def asVar(s: String): Option[String] =
    if (s.startsWith("{") && s.endsWith("}")) Some(s.drop(1).dropRight(1)) else None
}

// represents path with plain string children like `/a` or `/b`
case class PlainPathNode[V](
  // value of /
  value: Option[V],
  // nodes of subpaths
  children: Map[String, PathNode[V]]
) extends PathNode[V] {

  override def get(path: List[String]): Option[Matched[V]] = {
    if (path.isEmpty) return value.map(Matched(Nil, Nil, _))

    children.get(path.head).flatMap(_.get(path.tail))
  }

  override def add(path: List[String], value: V): PathNode[V] = path match {
    case Nil =>
      if (this.value.isDefined) throw new IllegalArgumentException("Duplicate path")
      copy(value = Some(value))

    case part :: tail =>
      if (PathNode.asVar(part).nonEmpty) {
        throw new IllegalArgumentException("Conflicting path")
      }

      copy(children = this.children + (part -> children.getOrElse(part, EmptyPathNode[V]()).add(tail, value)))
  }

  override def combine(node: PathNode[V]): PathNode[V] = {
    node match {
      case PlainPathNode(value, children) =>
        if (node.value.isDefined && value.isDefined)
          throw new IllegalArgumentException("Duplicate path")

        PlainPathNode(
          value.orElse(this.value),
          children.combine(this.children)
        )

      case EmptyPathNode(value) => node.combine(this)

      case _ => throw new IllegalArgumentException("Conflicting path")
    }
  }

  override def values: List[V] = value.fold[List[V]](Nil)(List(_)) ::: children.values.flatMap(_.values).toList
}

// represents path with /{var} child
case class VarPathNode[V](
  // name of var
  name: String,
  // value of /
  value: Option[V],
  // children node
  node: PathNode[V]
) extends PathNode[V] {

  override def get(path: List[String]): Option[Matched[V]] = {
    if (path.isEmpty) return value.map(Matched(Nil, Nil, _))

    node.get(path.tail).map(r => r.copy(params = (name -> path.head) :: r.params))
  }

  override def add(path: List[String], value: V): PathNode[V] = path match {
    case Nil =>
      copy(value = Some(value))

    case part :: tail =>
      val name = PathNode.asVar(part).getOrElse {
        throw new IllegalArgumentException("Conflicting path")
      }

      if (name != this.name) {
        throw new IllegalArgumentException(s"Conflicting path variable name in path: '$name' vs '${this.name}'")
      }

      copy(node = node.add(tail, value))
  }

  override def combine(node: PathNode[V]): PathNode[V] = {
    node match {
      case VarPathNode(name, value, node) =>
        if (name != this.name)
          throw new IllegalArgumentException(s"Conflicting path variable name in path: '$name' vs '${this.name}'")
        if (node.value.isDefined && value.isDefined)
          throw new IllegalArgumentException("Duplicate path")
        VarPathNode(name, this.value.orElse(value), node.combine(this.node))

      case EmptyPathNode(value) =>
        node.combine(this)

      case _ =>
        throw new IllegalArgumentException("Conflicting path")

    }
  }

  override def values: List[V] = value.fold[List[V]](Nil)(List(_)) ::: node.values
}

// represents path with star child
case class StarPathNode[V](
  // value of /
  value0: V
) extends PathNode[V] {
  override def get(path: List[String]): Option[Matched[V]] = {
    Some(Matched(Nil, path, value0))
  }

  override def add(path: List[String], value: V): PathNode[V] = {
    if (path.nonEmpty)
      throw new IllegalArgumentException("Conflicting path")
    this
  }

  override def combine(node: PathNode[V]): PathNode[V] = {
    node match {
      case EmptyPathNode(value) => node.combine(this)
      case _ =>
        throw new IllegalArgumentException("conflicting path")
    }
  }

  override val value: Option[V] = Some(value0)

  override def values: List[V] = List(value0)
}

// represents empty path without children
case class EmptyPathNode[V](value: Option[V] = None) extends PathNode[V] {
  override def get(path: List[String]): Option[Matched[V]] = {
    if (path.isEmpty)
      value.map(v => Matched(Nil, Nil, v))
    else
      None
  }

  override def add(path: List[String], value: V): PathNode[V] = path match {
    case Nil =>
      copy(value = Some(value))

    case part :: tail =>
      if (part == "*") {
        if (this.value.isDefined) throw new IllegalArgumentException("conflicting path")
        return StarPathNode(value).add(tail, value)
      }
      PathNode.asVar(part) match {
        case Some(varName) => VarPathNode(varName, this.value, EmptyPathNode[V]()).add(path, value)
        case None => PlainPathNode(this.value, Map.empty).add(path, value)
      }
  }

  override def combine(node: PathNode[V]): PathNode[V] = {
    if (node.value.isDefined && value.isDefined)
      throw new IllegalArgumentException("Duplicate path")

    if (value.isDefined)
      node.add(Nil, value.get)
    else
      node
  }

  override def values: List[V] = value.toList
}
