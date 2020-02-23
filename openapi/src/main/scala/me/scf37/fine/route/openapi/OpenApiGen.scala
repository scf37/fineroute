package me.scf37.fine.route.openapi

import java.lang.annotation.Annotation

import io.swagger.v3.oas.models.media._
import io.swagger.v3.oas.models.parameters.{Parameter, RequestBody}
import io.swagger.v3.oas.models.responses.{ApiResponse, ApiResponses}
import io.swagger.v3.oas.models.tags.Tag
import io.swagger.v3.oas.models._
import io.swagger.v3.oas.models.info.Info
import me.scf37.fine.route.RouteMeta
import me.scf37.fine.route.endpoint.meta.{Meta, MetaMethod, MultiMetaParameter, SingleMetaParameter}
import me.scf37.fine.route.openapi.impl._

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.reflect.runtime.universe

class OpenApiGen {

  def generate(
      meta: RouteMeta,
      title: String,
      version: String
  ): OpenAPI = {
    val openapi = new OpenAPI
    val components = new Components
    val schemas = new Schemas
    val info = new Info
    info.setTitle(title)
    info.setVersion(version)

    openapi.setPaths(makePaths(meta.endpointMetas, schemas))
    openapi.setTags(makeTags(meta.endpointMetas).asJava)
    components.setSchemas(schemas.makeSchemas().asJava)
    openapi.setComponents(components)
    openapi.setInfo(info)

    openapi
  }

  protected def enrichParameter(p: Parameter, f: IField): Unit = {
    // TODO p.setDescription
  }

  protected def encirhSchema(s: Schema[_], annotations: Seq[Annotation]): Unit = {
    // TODO description
  }

  protected def encodePrimitive(name: String, s: Schema[_]): Unit = {
    // TODO Instant, etc.
  }


  private def makeTags(endpointMetas: Seq[Meta]): Seq[Tag] = {
    endpointMetas.map { endpoint =>
      endpoint.tag -> endpoint.tagDescription
    }.filter(_._1.nonEmpty).toMap.map { case (tag, description) =>
      val t = new Tag
      t.setName(tag)
      if (description.nonEmpty)
        t.setDescription(description)
      t
    }.toSeq
  }

  private def makePaths(endpointMetas: Seq[Meta], schemas: Schemas): Paths = {
    val paths = new Paths

    endpointMetas.groupBy(_.pathPattern).foreach { case (path, endpoints) =>
      val pi = new PathItem
      makeOperations(endpoints, schemas).foreach {
        case (MetaMethod.GET, op) => pi.get(op)
        case (MetaMethod.PUT, op) => pi.put(op)
        case (MetaMethod.POST, op) => pi.post(op)
        case (MetaMethod.DELETE, op) => pi.delete(op)
        case (MetaMethod.PATCH, op) => pi.patch(op)
      }

      paths.addPathItem(path, pi)
    }

    paths
  }

  private def makeOperations(endpoints: Seq[Meta], schemas: Schemas): Seq[(MetaMethod, Operation)] = {
    endpoints.map { endpoint =>
      val op = new Operation
      op.setSummary(endpoint.summary)
      op.setDescription(endpoint.description)
      op.setTags((endpoint.tag :: endpoint.secondaryTags.map(_.name)).asJava)
      op.setParameters(makeParameters(endpoint, schemas).asJava)

      endpoint.consumes.foreach { body =>
        val rb = new RequestBody
        val content = new Content
        val mediaType = new MediaType
        mediaType.setSchema(schemas.makeSchema(body.body.tpe))

        content.addMediaType(body.mime, mediaType)
        rb.setContent(content)
        op.setRequestBody(rb)
      }

      endpoint.produces.foreach { body =>
        val responses = new ApiResponses
        val response = new ApiResponse

        val content = new Content
        val mediaType = new MediaType
        mediaType.setSchema(schemas.makeSchema(body.body.tpe))

        content.addMediaType(body.mime, mediaType)
        response.setContent(content)
        responses.addApiResponse("200", response)
        response.setDescription("")
        op.setResponses(responses)
      }

      endpoint.method -> op
    }
  }

  private def makeParameters(endpoint: Meta, schemas: Schemas): List[Parameter] = {
    endpoint.params.flatMap {
      case SingleMetaParameter(name, desc, mf, inPath) =>
        val p = new Parameter
        p.setName(name)
        p.setDescription(desc)
        p.setIn(if (inPath) "path" else "query")
        p.setSchema(schemas.makeSchema(mf.tpe))
        Seq(p)

      case MultiMetaParameter(mf, inPath) =>
        Introspector.introspect(mf.tpe) match {
          case ICaseClass(name, annotations, fields) =>
            fields.map { f =>
              val p = new Parameter
              p.setName(f.name)
              enrichParameter(p, f)
              p.setIn(if (inPath) "path" else "query")
              p.setSchema(schemas.makeSchema(f.tpe))
              p
            }
          case t => throw new IllegalArgumentException(s"MultiMetaParameter must be case class but got $t")
        }
    }
  }

  private class Schemas {
    private val schemas = mutable.Map.empty[String, Schema[_]]

    def makeSchemas(): Map[String, Schema[_]] = schemas.toMap

    def makeSchema(t: universe.Type): Schema[_] = makeSchema0(Introspector.introspect(t))
    def makeSchema(t: IType): Schema[_] = makeSchema0(t)

    private def computeNeedSchema(t: IType, foundRoot: Boolean = false): Set[String] = t match {
      case IOption(value) => computeNeedSchema(value, foundRoot)
      case IList(value) => computeNeedSchema(value, foundRoot)
      case IEnum(name, annotations, values) => Set.empty
      case ICaseClass(name, annotations, fields) =>
        fields.flatMap(f => computeNeedSchema(f.tpe, true)).toSet ++
          (if (foundRoot) Set.empty else Set(name))
      case ICaseClassRef(name) => computeNeedSchema(t, foundRoot) + name
      case IPrimitive(name) =>Set.empty
      case IUnit =>Set.empty
    }

    private def makeSchema0(t: IType): Schema[_] =
      doMakeSchema(t, computeNeedSchema(t))

    private def doMakeSchema(t: IType, needSchema: Set[String]): Schema[_] = t match {
      case IOption(value) => doMakeSchema(value, needSchema)

      case IList(value) =>
        val s = new ArraySchema
        s.setItems(doMakeSchema(value, needSchema))
        s

      case IEnum(name, annotations, values) =>
        schemas.getOrElseUpdate(name, {
          val s = new StringSchema
          s.setEnum(values.asJava)
          encirhSchema(s, annotations)
          s
        })

        val r = new Schema[Any]()
        r.set$ref(s"#/components/schemas/$name")
        r

      case ICaseClass(name, annotations, fields) =>
        val s = new ObjectSchema
        s.setName(name)
        encirhSchema(s, annotations)
        var requiredFields = Vector.empty[String]
        fields.foreach { f =>
          val ss = doMakeSchema(f.tpe, needSchema)
          ss.setName(name)
          if (!f.tpe.isInstanceOf[IOption]) {
            requiredFields = requiredFields :+ name
          }
          encirhSchema(ss, annotations)
          s.addProperties(f.name, ss)
        }
        if (requiredFields.nonEmpty) {
          s.setRequired(requiredFields.asJava)
        }
        if (needSchema.contains(name)) {
          schemas.getOrElseUpdate(name, s)
        }
        s

      case ICaseClassRef(name) =>
        val r = new Schema[Any]()
        r.set$ref(s"#/components/schemas/$name")
        r

      case IPrimitive(name) =>
        val r = new Schema[Any]()
        val (tpe, fmt) = name match {
          case "int" => "integer" -> "int32"
          case "long" => "integer" -> "int64"
          case "float" => "number" -> "float"
          case "double" => "number" -> "double"
          case "string" => "string" -> null
          case "boolean" => "boolean" -> null
          case _ => (null, null)
        }

        if (tpe != null) {
          r.setType(tpe)
          r.setFormat(fmt)
        } else {
          encodePrimitive(name, r)
        }
        r

      case IUnit =>
        val r = new Schema[Any]()
        r
    }
  }
}
