package uritemplate4s.demo

import scala.scalajs.js

import cats.syntax.either._
import io.circe._
import io.circe.syntax._
import monix.execution.Cancelable
import monix.execution.cancelables.SingleAssignCancelable
import monix.reactive.OverflowStrategy.Unbounded
import monix.reactive._
import org.scalajs.dom.window.document
import org.scalajs.dom.{Event, html}
import uritemplate4s._
import uritemplate4s.syntax._

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
      "string" -> "foobar".toValue,
      "list" -> List("apple", "pear", "orange").toValue,
      "assoc" -> Map("foo" -> "bar", "wierd" -> "strange").toValue
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

    def parseValues(s: String) = for {
      js <- parser.parse(s)
      values <- js.as[Map[String, Value]]
    } yield values.toList


    val templateSource = inputChange(input, "input")
      .map(UriTemplate.parse)

    val valuesSource = inputChange(valuesInput, "input")
      .map(parseValues)

    val _ = templateSource.combineLatest(valuesSource)
      .map { case (templateE, valuesE) =>
        val result = for {
          template <- templateE.leftMap(_.message)
          values <- valuesE.leftMap(_.getMessage)
          result <- template.expand(values: _*)
            .toEither.leftMap(_.message)
        } yield result
        result.merge
      }
      .foreach { result =>
        output.innerHTML = result
        ()
      }
  }
}

