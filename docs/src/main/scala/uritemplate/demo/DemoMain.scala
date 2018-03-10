package uritemplate.demo

import scala.util.Try
import scala.scalajs.js

import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.window.document
import uritemplate.{StringValue, UriTemplate}

object DemoMain extends js.JSApp {

  def main(): Unit = {

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
    val initialInput = "{var}/{foo}/{bar}"
    val initialVars = "bar=woooo,var=value,foo=waveyy"
    document.getElementById("uritemplate-playground").innerHTML =
      s"""
        <input class="uritemplate-input" value="$initialInput"></input>
        <textarea class="uritemplate-vars">$initialVars</textarea>
        <div class="uritemplate-output"></div>
      """.stripMargin
  }

  private def registerUpdate(): Unit = {
    val input = document.getElementsByClassName("uritemplate-input")(0).asInstanceOf[html.Input]
    val varsInput = document.getElementsByClassName("uritemplate-vars")(0).asInstanceOf[html.Input]
    val output = document.getElementsByClassName("uritemplate-output")(0).asInstanceOf[html.Div]

    def parseVars() = varsInput.value
      .split(',')
      .map { s =>
        val arr = s.split('=')
        arr(0) -> StringValue(arr(1))
      }

    def expandTemplate(): Unit = {
      dom.console.info("on change " + input.value)
      val result = Try(UriTemplate.parse(input.value) match {
        case Left(err) => err.toString
        case Right(template) =>
          template.expand(parseVars(): _*)
            .fold(_.toString, identity)
      }).fold(_.toString, identity)

      output.innerHTML = result
    }

    input.oninput = _ => expandTemplate()
    varsInput.oninput = _ => expandTemplate()
    expandTemplate()
  }
}
