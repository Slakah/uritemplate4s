package uritemplate.demo

import scala.util.Try
import scala.scalajs.js

import io.circe._
import io.circe.syntax._
import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.window.document
import uritemplate._

object Playground {

  private implicit lazy val decodeValue: Decoder[Value] =
    Decoder[String].map[Value](StringValue) or
      Decoder[JsonNumber].map[Value](n => StringValue(n.toString)) or
      Decoder[Vector[String]].map[Value](ListValue) or
      Decoder[Map[String, String]].map[Value](m => AssociativeArray(m.toVector))

  private implicit lazy val encodeValue: Encoder[Value] = Encoder.instance {
    case StringValue(s) => s.asJson
    case ListValue(l) => l.asJson
    case AssociativeArray(m) => m.toMap.asJson
  }

  def main(args: Array[String]): Unit = {

    val enabled = js.Dynamic.global.URITEMPLATE_PLAYGROUND
      .asInstanceOf[js.UndefOr[Boolean]]
      .getOrElse(false)

    if (enabled) {
      dom.console.info("4")
      renderPlayground()
      registerUpdate()
    }
  }

  private def renderPlayground(): Unit = {
    val initialInput = "http://{string}.com{/list*}{?assoc*}"

    val initialVars: Map[String, Value] = Map(
      "string" -> "foobar",
      "list" -> List("apple", "pear", "orange"),
      "assoc" -> List("foo" -> "bar", "wierd" -> "strange")
    )
    document.getElementById("uritemplate-playground").innerHTML =
      s"""
        <input class="uritemplate-input" value="$initialInput"></input>
        <textarea class="uritemplate-vars">${initialVars.asJson.spaces2}</textarea>
        <div class="uritemplate-output"></div>
      """.stripMargin
  }

  private def registerUpdate(): Unit = {
    val input = document.getElementsByClassName("uritemplate-input")(0).asInstanceOf[html.Input]
    val varsInput = document.getElementsByClassName("uritemplate-vars")(0).asInstanceOf[html.Input]
    val output = document.getElementsByClassName("uritemplate-output")(0).asInstanceOf[html.Div]

    def parseValues() = for {
      js <- parser.parse(varsInput.value)
      values <- js.as[Map[String, Value]]
    } yield values.toList

    def expandTemplate(): Unit = {
      dom.console.info("on change " + input.value)
      val result = Try(UriTemplate.parse(input.value) match {
        case Left(err) => err.toString
        case Right(template) =>
          val valuesResult = parseValues()
          valuesResult.left.foreach(throw _)
          template.expand(valuesResult.right.get: _*)
            .fold(_.toString, identity)
      }).fold(_.toString, identity)

      output.innerHTML = result
    }

    input.oninput = _ => expandTemplate()
    varsInput.oninput = _ => expandTemplate()
    expandTemplate()
  }
}

