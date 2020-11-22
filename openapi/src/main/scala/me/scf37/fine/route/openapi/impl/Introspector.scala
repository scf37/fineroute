package me.scf37.fine.route.openapi.impl

import java.lang.annotation.Annotation

import scala.collection.immutable.ArraySeq
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._

sealed trait IType

case class IOption(value: IType) extends IType
case class IList(value: IType) extends IType
case class IEnum(name: String, annotations: Seq[Annotation], values: Seq[String]) extends IType
case class ICaseClass(name: String, annotations: Seq[Annotation], fields: Seq[IField]) extends IType
// used to break recursive types
case class ICaseClassRef(name: String) extends IType
case class IPrimitive(name: String) extends IType
object IUnit extends IType
case class IField(name: String, annotations: Seq[Annotation], tpe: IType)

/**
 * introspect type, recognizing structure required for documentation generator
 */
trait Introspector {
  /**
   * Introspect type
   * @param t type to introspect
   * @return parsed type representation
   */
  def introspect(t: Type): IType
}

object Introspector extends Introspector {
  def introspect(t: Type): IType = doIntrospect(t, Set.empty)

  private def doIntrospect(t: Type, knownTypes: Set[String]): IType = {
    val c = cls(t)
    def annotations = c.getAnnotations.filter(_.annotationType() != classOf[scala.reflect.ScalaSignature])

    def fields(t: Type, c: Class[_], knownTypes: Set[String]): Seq[IField] = {
      val constructor: Option[MethodSymbol] = t.members.find {
        case s: MethodSymbol if s.isConstructor && s.paramLists.nonEmpty && s.paramLists.head.nonEmpty => true
        case _ => false
      }.map(_.asMethod)

      constructor.flatMap { ctor =>
        ctor.paramLists.headOption.map { paramList =>
          val javaCtor = c.getConstructors.find(_.getParameterCount == paramList.size).getOrElse {
            throw new RuntimeException(s"Failed to find ${paramList.size}-arg constructor in $c")
          }

          paramList.zip(javaCtor.getParameterAnnotations).map {
            case (paramSymbol, paramAnnotations) =>
              IField(paramSymbol.name.decodedName.toString.trim, ArraySeq.unsafeWrapArray(paramAnnotations), doIntrospect(paramSymbol.typeSignature, knownTypes))
          }
        }
      }.fold(Seq.empty[IField])(identity)
    }

    val typeName = c.getSimpleName

    if (t =:= typeOf[Unit] || t =:= typeOf[Nothing])
      IUnit
    else if (t <:< typeOf[IterableOnce[_]])
      IList(doIntrospect(t.typeArgs.head, knownTypes))
    else if (t <:< typeOf[Option[_]])
      IOption(doIntrospect(t.typeArgs.head, knownTypes))
    else if (t.typeSymbol.isClass && t.typeSymbol.asClass.isTrait && getKnownSubclasses(t).nonEmpty)
      IEnum(typeName, ArraySeq.unsafeWrapArray(annotations), sealedTraitEnumValues(t))
    else if (c.isPrimitive || c.getName.startsWith("java.") || c.getName.startsWith("scala."))
      IPrimitive(typeName.charAt(0).toLower.toString + c.getSimpleName.substring(1))
    else {
      if (knownTypes.contains(typeName))
        ICaseClassRef(typeName)
      else
        ICaseClass(typeName, ArraySeq.unsafeWrapArray(annotations), fields(t, c, knownTypes + typeName))
    }
  }

  private def sealedTraitEnumValues(t: Type): Seq[String] =
    getKnownSubclasses(t).flatMap { e =>
      val v = runtimeMirror(classLoader).reflectModule(e.asClass.module.asModule).instance
      Option(v).map(_.toString)
    }.toSeq.sorted

  private[this] def getKnownSubclasses(traitType: Type): Set[Symbol] = {
    traitType.typeSymbol.asClass.knownDirectSubclasses.filter(_.isModuleClass)
  }

  private[this] def cls(t: Type): Class[_] = universe.runtimeMirror(classLoader).runtimeClass(t)

  private[this] def classLoader = Thread.currentThread().getContextClassLoader
}
