package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import kotlin.reflect.KClass

enum class InterfaceElementType {
  DSL, ENTITY, PARAMETER
}
class InterfaceTree(
  val details: String,
  val level: Int = 0,
  val type: InterfaceElementType = InterfaceElementType.DSL,
  val children: MutableList<InterfaceTree> = mutableListOf(),
) {

  fun add(child: String, type: InterfaceElementType) =
    InterfaceTree(child, level + 1, type)
      .also { children.add(it) }

  override fun toString(): String {
    val childrenString = children
      .joinToString(separator = "\n") { "$it" }
      .takeIf { it.isNotEmpty() }
      ?.let { "\n$it" }
      ?: ""
    return "${" ".repeat(level * 2)}$details$childrenString"
  }

  fun toHtml(): String = InterfaceTreeHtml().generateHtml(this)
}

fun inspectInterface(
  kClass: KClass<*>,
  markdown: StringBuilder,
  level: Int = 0,
  interfaceTree: InterfaceTree = InterfaceTree(kClass.simpleName.toString(), 0),
): InterfaceTree {
  val indent = "  ".repeat(level)
  kClass.members.filter { it.isAbstract }.forEach { function ->
    markdown.append("\n$indent  - *${function.name.lastWord()}*")
    val functionLeaf = interfaceTree.add(function.name.lastWord(), InterfaceElementType.ENTITY)
    function.parameters
      .filter { it.name != null }
      .filterNot { it.type.toString().endsWith("kotlin.Unit") }
      .forEach { parameter ->
        functionLeaf.add("${parameter.name}: ${parameter.type.toString().lastWord()}", InterfaceElementType.PARAMETER)
        markdown.append("\n$indent    - ${parameter.name}: ${parameter.type.toString().lastWord()}")
//        if (parameter.isOptional) {
//          markdown.append(" = ${parameter.default}")
//          markdown.append(" = ${parameter}")
//        }
//      if (index < function.parameters.size - 1) {
//        markdown.append(", \n")
//      }
      }
    val nextDsl = function.parameters
      .filter { it.name != null }
      .filter { it.type.toString().endsWith("kotlin.Unit") }
      .firstOrNull()
      ?.type?.arguments?.get(0)?.type?.toString()
    nextDsl?.also {
      val childLeaf = functionLeaf.add(it.lastWord(), InterfaceElementType.DSL)
      inspectInterface(Class.forName(it).kotlin, markdown, level + 1, childLeaf)
    }
  }
  return interfaceTree
}

class InterfaceTreeHtml {

  private val initialLevel = 5

  fun generateHtml(interfaceTree: InterfaceTree): String {
    val children = interfaceTree.children
      .joinToString(separator = "\n") { it.toHtml() }
      .takeIf { it.isNotEmpty() }
      ?.let { "\n$it" }
      ?: ""
    val (top, bottom) = details(interfaceTree)
    val bodyStart = if (interfaceTree.level == 0) startBody() else ""
    val bodyEnd = if (interfaceTree.level == 0) endBody() else ""
    return if (interfaceTree.level == 0) {
      htmlDoc("$bodyStart$top$children$bottom$bodyEnd")
    } else {
      "$bodyStart$top$children$bottom$bodyEnd"
    }
  }

  private fun details(interfaceTree: InterfaceTree): Pair<String, String> {
    val indentation = outputLevel(interfaceTree.level)
    val top = when (interfaceTree.type) {
      InterfaceElementType.DSL -> "\n" + """
${" ".repeat(indentation * 2)}<li class="list-group-item"><a data-toggle="collapse" href="#collapse-${interfaceTree.details.lowercase()}" class="text-success">${interfaceTree.details}</a></li>
${" ".repeat(indentation * 2)}<li id="collapse-${interfaceTree.details.lowercase()}" class="list-group-item panel-collapse collapse">
${" ".repeat(indentation * 2)}<ul class="list-group">
      """.trimIndent()
      InterfaceElementType.ENTITY -> "\n" + """
${" ".repeat(indentation * 2)}<li class="list-group-item"><a data-toggle="collapse" href="#collapse-${interfaceTree.details}" class="text-primary">${interfaceTree.details}</a></li>
${" ".repeat(indentation * 2)}<li id="collapse-${interfaceTree.details}" class="list-group-item panel-collapse collapse">
${" ".repeat(indentation * 2)}<ul class="list-group">
      """.trimIndent()
      InterfaceElementType.PARAMETER -> {
        val (name, type) = interfaceTree.details.split(": ")
        "\n" + """
${" ".repeat(indentation * 2)}<li class="list-group-item"><span class="text-info">$name</span>  <span class="text-secondary">$type</span></li>
        """.trimIndent()
      }
    }
    val bottom = when (interfaceTree.type) {
      InterfaceElementType.DSL -> "${" ".repeat(indentation * 2)}</ul>\n</li>\n</li>\n"
      InterfaceElementType.ENTITY -> "${" ".repeat(indentation * 2)}</ul>\n</li>\n</li>\n"
      InterfaceElementType.PARAMETER -> ""
    }
    return top to bottom
  }

  private fun outputLevel(treeLevel: Int) = treeLevel + initialLevel
}

private fun String.lastWord() = this.lastIndexOf(".").let { if (it < 0) this else this.substring(it + 1) }

private fun htmlDoc(body: String) = """
<!DOCTYPE html>
<html>
<head>
  <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css"
        integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T" crossorigin="anonymous">
  <script src="https://code.jquery.com/jquery-3.3.1.slim.min.js"
          integrity="sha384-q8i/X+965DzO0rT7abK41JStQIAqVgRVzpbzo5smXKp4YfRvH+8abtTE1Pi6jizo"
          crossorigin="anonymous"></script>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js"
          integrity="sha384-UO2eT0CpHqdSJQ6hJty5KVphtPhzWj9WO1clHTMGa3JDZwrnQq4sF86dIHNDz0W1"
          crossorigin="anonymous"></script>
  <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js"
          integrity="sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM"
          crossorigin="anonymous"></script>
  <style>
    .list-group-item {
      border: 0
    }
  </style>
</head>
<body>

$body

</body>
</html>
""".trimIndent()

fun startBody(): String = """
<div class="container">
  <div class="panel-group">
    <div class="panel panel-default">
      <div class="panel-heading">
        <ul class="list-group">

""".trimIndent()

fun endBody(): String = """
        </ul>
      </div>
    </div>
  </div>
</div>
""".trimIndent()
