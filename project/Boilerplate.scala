import sbt._

object Boilerplate {

  private def template(methods: String) =
    s"""
      |// auto-generated boilerplate, see package/Boilerplate.scala
      |package uritemplate4s
      |
      |import syntax._
      |
      |private[uritemplate4s] trait UriTemplateArities extends UriTemplateBase {
      |${methods.split("\n").map(method => s"  $method").mkString("\n")}
      |}
    """.stripMargin

  private val content = template(
    ("def expand(): ExpandResult = expandVars()" :: (1 to 22).toList.map { numArities =>
      val range = 1 to numArities
      val genericTypes = range.map(i => s"A$i : ToValue").mkString(", ")
      val args = range.map(i => s"a$i: (String, A$i)").mkString(", ")
      val params = range.map(i => s"a$i._1 -> a$i._2.toValue").mkString(", ")
      s"def expand[$genericTypes]($args): ExpandResult = expandVars($params)"
    }).mkString("\n")
  )

  def gen(dir: File): List[File] = {
    val path = dir / "uritemplate4s" / "UriTemplateArities.scala"
    IO.write(path, content)
    List(path)
  }
}
