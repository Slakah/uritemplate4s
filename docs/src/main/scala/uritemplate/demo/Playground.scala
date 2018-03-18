package uritemplate.demo

import scala.scalajs.js
import scala.util.Try

import io.circe._
import io.circe.syntax._
import monix.execution.Cancelable
import monix.execution.cancelables.SingleAssignCancelable
import monix.reactive.OverflowStrategy.Unbounded
import monix.reactive._
import org.scalajs.dom
import org.scalajs.dom.window.document
import org.scalajs.dom.{Event, html}
import uritemplate._

object Playground {

  import monix.execution.Scheduler.Implicits.global

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
        <div class="form-group">
          <label for="uritemplate-input">URI Template</label>
          <input class="form-control" id="uritemplate-input" value="$initialInput">
          </input>
        </div>
        <div class="form-group">
          <label for="uritemplate-values">Values</label>
          <textarea class="form-control" id="uritemplate-values" rows="10">${initialVars.asJson.spaces2}</textarea>
        </div>
        <div class="form-group">
          <label for="uritemplate-output">URI Output</label>
          <pre id="uritemplate-output"></pre>
        </div>
      """
  }

  def inputChange(target: html.Input, event: String): Observable[String] =
    Observable.create(Unbounded) { subscriber =>
      val c = SingleAssignCancelable()
      // Forced conversion, otherwise canceling will not work!
      def f() = {
        subscriber.onNext(target.value).syncOnStopOrFailure(_ => c.cancel())
      }
      val listener = (_: Event) => f()
      target.addEventListener(event, listener)
      f()
      c := Cancelable(() => target.removeEventListener(event, listener))
    }


  private def registerUpdate(): Unit = {
    val input = document.getElementById("uritemplate-input").asInstanceOf[html.Input]
    val valuesInput = document.getElementById("uritemplate-values").asInstanceOf[html.Input]
    val output = document.getElementById("uritemplate-output").asInstanceOf[html.Html]

    def parseValues() = for {
      js <- parser.parse(valuesInput.value)
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

    val inputSource = inputChange(input, "input")
      .map(_ => input.value)
    val valuesSource = inputChange(valuesInput, "input")
      .map(_ => valuesInput.value)

    inputSource.combineLatest(valuesSource)
      .foreach { s =>
        println(s)
        expandTemplate()
      }

    expandTemplate()
  }
}

