package me.scf37.fine.route.docs

import java.lang.annotation.Annotation

import me.scf37.fine.route.docs.impl.ICaseClass
import me.scf37.fine.route.docs.impl.ICaseClassRef
import me.scf37.fine.route.docs.impl.IEnum
import me.scf37.fine.route.docs.impl.IList
import me.scf37.fine.route.docs.impl.IOption
import me.scf37.fine.route.docs.impl.IPrimitive
import me.scf37.fine.route.docs.impl.IType
import me.scf37.fine.route.docs.impl.IUnit
import me.scf37.fine.route.docs.impl.Introspector
import me.scf37.fine.route.meta.Meta
import me.scf37.fine.route.meta.MetaBody
import me.scf37.fine.route.meta.MetaMethod
import me.scf37.fine.route.meta.MultiMetaParameter
import me.scf37.fine.route.meta.SingleMetaParameter

object HtmlDocs {
  /**
   * Generate single-page HTML documentation from route meta
   *
   * @param head HTML heading
   * @param tagDescriptions tag descriptions to use when generating
   * @param endpoints list of endpoints
   * @param introspector introspector to use
   * @return HTML string
   */
  def generateHtml(
    head: String,
    tagDescriptions: Map[String, String],
    endpoints: Seq[Meta],
    introspector: Introspector = Introspector
  ): String = {
    val endpointsByTag = endpoints.groupBy(_.tags.headOption.getOrElse("")).map {
      case (tag, ops) =>
        val methodOrder = Seq(MetaMethod.GET, MetaMethod.POST, MetaMethod.PUT, MetaMethod.PATCH, MetaMethod.DELETE).zipWithIndex.toMap
        (tag, ops.sortBy(op =>
          (op.pathPattern.count(_ == '/'), op.pathPattern, methodOrder(op.method))
        ))
    }

    val ops = endpointsByTag.toSeq.sortBy(_._1).map { case (tag, ops) =>
      s"""
         <h2>
          $tag${tagDescriptions.get(tag).map("<span class='tag-desc'>" + _ + "</span>").getOrElse("")}
         </h2>
         ${printEndpoints(ops, introspector)}
        """
    }

    s"""
      <!DOCTYPE html>
      <html>
      <head>
        $css
      </head>
      <body>
        <div class="container">
          ${head}
          ${ops.mkString("\n")}
          <h2>Known types</h2>
          ${knownTypes(endpoints, introspector)}
        </div>
      $scripts
      </body>
      </html>
      """
  }

  private def knownTypes(endpoints: Seq[Meta], i: Introspector): String = {
    def collectTypes(t: IType): Set[IType] = t match {
      case IOption(value) => collectTypes(value)
      case IList(value) =>collectTypes(value)
      case IEnum(name, annotations, values) => Set(t)
      case ICaseClass(name, annotations, fields) => fields.flatMap(f => collectTypes(f.tpe)).toSet + t
      case ICaseClassRef(name) =>Set.empty
      case IPrimitive(name) =>Set.empty
      case IUnit =>Set.empty
    }

    val allTypes = endpoints.flatMap { e =>
      e.params.flatMap(p => collectTypes(i.introspect(p.mf.tpe))) ++
        e.produces.fold(Set.empty[IType])(b => collectTypes(i.introspect(b.body.tpe))) ++
        e.consumes.fold(Set.empty[IType])(b => collectTypes(i.introspect(b.body.tpe)))
    }

    allTypes.distinct.sortBy(typeName).map { t=>
      s"""
        <div id="${typeName(t)}" class="type-container">
          <h4>${typeName(t)}</h4>
          ${typeDescription(t)}
        </div>
      """
    }.mkString("\n")

  }

  private def printEndpoints(endpoints: Seq[Meta], i: Introspector): String = {
    endpoints.map { e =>
      val queryParams = e.params.filter(!_.inPath).flatMap {
        case SingleMetaParameter(name, desc, mf, inPath) => Seq(name)
        case MultiMetaParameter(mf, inPath) => extractParams(i.introspect(mf.tpe), false).map(_.name)
      }.sorted
      s"""
        <div class="operation method-${e.method}">
          ${operationHead(e.method.toString, e.pathPattern, queryParams, e.summary)}
          <div class="operation-content closed">
            ${description(e.description)}
            ${operationParams(e, i)}
            ${requestBody(e.consumes, i)}
            ${responseBody(e.produces, i)}
          </div>
        </div>
     """
    }.mkString("\n")
  }

  private def responseBody(body: Option[MetaBody], i: Introspector): String = body match {
    case None => ""
    case Some(body) =>
      val t = i.introspect(body.body.tpe)
      s"""<h3>Response body</h3>
            <div class="content-type">${body.mime}</div>
            ${typeReference(t)}
            ${typeDescription(t)}
            """
  }

  private def requestBody(body: Option[MetaBody], i: Introspector): String = body match {
    case None => ""
    case Some(body) =>
      val t = i.introspect(body.body.tpe)
      s"""<h3>Request body</h3>
           <div class="content-type">${body.mime}</div>
           ${typeReference(t)}
           ${typeDescription(t)}
           """
  }

  private def typeReference(tpe: IType) = {
    def link(t: IType): String = t match {
      case IOption(value) => link(value)
      case IList(value) => s"List<${link(value)}>"
      case IEnum(name, annotations, values) => s"""<a class="type-link" href="#$name">$name</a>"""
      case ICaseClass(name, annotations, fields) => s"""<a class="type-link" href="#$name">$name</a>"""
      case ICaseClassRef(name) => s"""<a href="#$name">$name</a>"""
      case IPrimitive(name) => name
      case IUnit => "-"
    }

    link(tpe)
  }

  private def typeDescription(t: IType): String = {
    t match {
      case IOption(value) => typeDescription(value)
      case IList(value) => typeDescription(value)
      case t@IEnum(name, annotations, values) => enumDescrption(t)
      case t@ICaseClass(name, annotations, fields) => caseClassDescrption(t)
      case ICaseClassRef(name) => ""
      case IPrimitive(name) => ""
      case IUnit => ""
    }
  }

  private def caseClassDescrption(t: ICaseClass): String = {
    case class Row(name: String, tpe: String, desc: String, ro: Boolean)

    def makeRows(t: IType, path: String) : Seq[Row] = t match {
      case IOption(value) => makeRows(value, path)
      case IList(value) =>makeRows(value, path + "[]")
      case IEnum(name, annotations, values) =>List(Row(path, name, "", false))
      case ICaseClass(name, annotations, fields) =>
        fields.flatMap { f =>
          val fieldPath = if (path.isEmpty) f.name else path + "." + f.name
          makeRows(f.tpe, fieldPath).map { row =>
            if (row.name == fieldPath)
              row.copy(desc = description(f.annotations), ro = hasReadOnly(f.annotations))
            else
              row
          }
        }
      case ICaseClassRef(name) =>List(Row(path, name, "", false))
      case IPrimitive(name) => List(Row(path, name, "", false))
      case IUnit =>List(Row(path, "-", "", false))
    }

    val rows = makeRows(t, "")

    s"""
      ${
      val desc = description(t.annotations)
      if (desc.nonEmpty) {
        s"""<p class="type-desc">$desc</p>"""
      } else ""
    }
      <table class="pretty-table"><tr>
         <th style="width:5em;max-width:5em">Name</th>
         <th style="width:5em;max-width:5em">Type</th>
         <th>Description</th>
         <th style="width:5em;max-width:5em">Read-only</th>
          ${rows.sortWith(_.name < _.name).map { e =>
            s"<tr><td>${e.name}</td><td>${e.tpe}</td><td>${e.desc}</td><td>${if (e.ro) "yes" else "no"}</td></tr>"
          }.mkString}
       </table>
      """
  }

  private def enumDescrption(t: IEnum): String = {
    s"""
      ${
      val desc = description(t.annotations)
      if (desc.nonEmpty) {
        s"""<p class="type-desc">$desc</p>"""
      } else ""
    }
    ${t.values.map(v => s"<i>$v</i>").mkString(", ")}
    """
  }

  private def operationParams(e: Meta, i: Introspector): String = {
    if (e.params.isEmpty) return ""

    val params: Seq[Param] = e.params.flatMap { p =>
      val t = i.introspect(p.mf.tpe)
      p match {
        case SingleMetaParameter(name, desc, mf, inPath) =>
          Seq(Param(inPath, name, typeName(t), desc, if(t.isInstanceOf[IOption]) "no" else "yes"))
        case MultiMetaParameter(mf, inPath) =>
          extractParams(t, inPath)
      }
    }

    s"""<div class="operation-params">
         <h3>Parameters</h3>
         <table class="pretty-table"><tr>
           <th style="width:5em;max-width:5em">Name</th>
           <th style="width:5em;max-width:5em">Type</th>
           <th>Description</th>
           <th style="width:5em;max-width:5em">Required</th> </tr>
         ${
            params.map { p =>
              s"""<tr>
                 <td class="param-${if (p.inPath) "path" else "query"}">${p.name}</td>
                 <td>${p.tpe}</td>
                 <td>${p.desc}</td>
                 <td>${p.mandatory}</td>
                 </tr>
                 """
            }.mkString
          }
         </table>
       </div>
    """
  }

  private def description(desc: String) =
    if (desc.trim.nonEmpty)
      s"""<div class="operation-desc">
              $desc
          </div>"""
    else ""

  private def operationHead(method: String, path: String, queryParams: Seq[String], summary: String): String =
    s"""
       <div class="operation-head" onclick="toggle(this)">
          <div style="display:inline-block">
            <div class="method">$method</div>
            <div class="url">${path.replaceAll("\\{(.+?)\\}", "<span class=\"path-param\">$1</span>")}${
      if (queryParams.nonEmpty) {
        "?" + queryParams.map(s => s"""<span class="query-param">$s</span>""").mkString("&amp;")
      } else ""
    }
            </div>
           </div>
           <div class="summary">${summary}</div>
        </div>
    """

  private def description(annotations: Seq[Annotation]): String = annotations.collectFirst {
    case a: Description => a.value()
  }.getOrElse("")

  private def typeName(t: IType): String = t match {
    case IOption(value) => typeName(value)
    case IList(value) =>s"List<${typeName(value)}>"
    case IEnum(name, annotations, values) => name
    case ICaseClass(name, annotations, fields) =>name
    case ICaseClassRef(name) =>name
    case IPrimitive(name) => name
    case IUnit => "-"
  }

  case class Param(inPath: Boolean, name: String, tpe: String, desc: String, mandatory: String)

  // extract query/path parameters from case class
  def extractParams(t: IType, inPath: Boolean): Seq[Param] = t match {
    case IOption(value) => extractParams(value, inPath).map(p => p.copy(mandatory = "no"))
    case IList(value) => extractParams(value, inPath).map(p => p.copy(tpe = s"List<${p.tpe}>"))
    case IEnum(name, annotations, values) => Seq(Param(inPath, "", name, "", "yes"))
    case ICaseClass(name, annotations, fields) =>fields.flatMap { field =>
      extractParams(field.tpe, inPath).map { p =>
        p.copy(name = field.name, desc = description(field.annotations))
      }
    }
    case ICaseClassRef(name) => Seq(Param(inPath, "", name, "", "yes"))
    case IPrimitive(name) => Seq(Param(inPath, "", name, "", "yes"))
    case IUnit => Seq(Param(inPath, "", "-", "", "yes"))
  }

  private def hasReadOnly(annotations: Seq[Annotation]): Boolean = {
    annotations.exists(_.isInstanceOf[ReadOnly])
  }

  private val scripts =
    """
        <script>
            function toggle(hdr) {
              var body = hdr.parentNode.querySelector(".operation-content")
              body.classList.toggle("closed");
              hdr.parentNode.classList.toggle("operation-expanded")
            }
        </script>
    """

  private val css =
    """
      <style type="text/css">

        html {
          font: 400 16px/24px Roboto,sans-serif;
        }

        .tag-desc {
            color: #999999;
            display: inline-block;
            margin-left: 1em;
        }

        .operation {
            margin-bottom: 10px;
        }

        .operation-expanded {
            -webkit-box-shadow: 2px 2px 5px 0px rgba(0,0,0,0.75);
            -moz-box-shadow: 2px 2px 5px 0px rgba(0,0,0,0.75);
            box-shadow: 2px 2px 5px 0px rgba(0,0,0,0.75);
        }

        .operation-expanded .operation-head {
            position: relative;
            border-bottom: 1px solid #999999;
        }

        .operation-head {
            width: 100%;
            cursor: pointer;
            overflow: hidden;
        }
        .operation-head .method {
            width: 4em;
            padding: 0.2em;
            display: inline-block;
        }
        .operation-head .url {
            display: inline-block;
        }
        .operation-head .summary {
            float: right;
            margin-right: 0.3em;
            font-size: 12px;
        }

        .operation-content {
            padding-left: 1em;
            padding-bottom: 1em;
            padding-right: 1em;

            overflow-y: hidden;
        }

        .closed {
            max-height: 0;
            padding-bottom: 0;
        }

        .method-GET .operation-head {
            background-color: #e7f0f7;
        }

        .method-POST .operation-head {
            background-color: #e7f6ec;
        }

        .method-PUT .operation-head {
            background-color: #f9f2e9
        }

        .method-DELETE .operation-head {
            background-color: #f5e8e8
        }

        .method-PATCH .operation-head {
            background-color: #fdecee
        }

        .method-GET .method {
            background-color: #0f6ab4;
        }

        .method-POST .method {
            background-color: #10a54a;
        }

        .method-PUT .method {
            background-color: #c5862b;
        }

        .method-DELETE .method {
            background-color: #a41e22;
        }

        .method-PATCH .method {
            background-color: #ff69b4
        }

        .method {
            color: white;
            text-align: center;
            vertical-align: middle;
            padding: 2px;
        }

        .type-link {
            background-clip: padding-box;
            border-bottom: 1px dashed rgba(0,102,204,.2);
            color: #06c;
            text-decoration: none;
            -webkit-background-clip: padding-box;
        }

        .url {
            font-style: italic;
        }
        .path-param {
            font-weight: bold;
            font-style: italic;
            color: #ec407a;
        }
        .query-param {
          font-style: italic;
          color: #0BCD57;
        }
        .param-query {
          color: #0BCD57;
        }
        table.pretty-table {
            border-collapse: collapse;
            width: 100%;
        }
        .pretty-table td, .pretty-table th {
            padding-left: 0.5em;
            padding-right: 0.5em;
            padding-bottom: 0.3em;
        }

        .pretty-table th {
            color: #999999;
            font-weight: normal;
            font-style: italic;
            text-align: left;
            padding-bottom: 0.3em;
        }

        .pretty-table tr:FIRST-CHILD {
            border-bottom-width: 1px;
            border-bottom-color: #999999;
            border-bottom-style: solid;
        }

        .content-type {
            float: right;
            background-color: #8AC007;
            color: white;
            padding: 1px 4px 2px 4px;
            -webkit-box-shadow: 2px 2px 5px 0px rgba(0,0,0,0.75);
            -moz-box-shadow: 2px 2px 5px 0px rgba(0,0,0,0.75);
            box-shadow: 2px 2px 5px 0px rgba(0,0,0,0.75);
        }

        h3 {
            margin-top: 0.5em;
            margin-bottom: 0.5em;
        }

        body {
            color: #333333;
            margin: 0;
        }

        .container {
            max-width: 960px;
            margin-left: auto;
            margin-right: auto;
        }

        .head {
          background-color: #8aa2be;
        }
        .head-wrap {
            max-width: 960px;
            margin-left: auto;
            margin-right: auto;
        }
        </style>
    """
}
