package uritemplate4s

import scala.io.Source

import cats.syntax.all._
import io.circe.{Error => _, _}
import io.circe.parser._
import utest._

object ExternalTests extends TestSuite {
  sealed trait ExpectedResult
  case class Expected(result: String) extends ExpectedResult
  case class MultiExpected(results: Seq[String]) extends ExpectedResult
  case object FailExpected extends ExpectedResult

  final case class Test(
    name: String,
    level: Option[Int],
    variables: Variables,
    testcases: Seq[(String, ExpectedResult)]
  )

  final case class Variables(value: Map[String, Value]) extends AnyVal

  implicit lazy val decodeValue: Decoder[Value] =
    Decoder[String].map[Value](StringValue) or
      Decoder[JsonNumber].map[Value](n => StringValue(n.toString)) or
      Decoder[Vector[String]].map[Value](ListValue) or
      Decoder[Map[String, String]].map[Value](m => AssociativeArray(m.toVector))

  implicit lazy val decodeVariables: Decoder[Variables] = Decoder.instance { c =>
    for {
      m <- c.as[Map[String, Json]]
      vars <-
        m.toList
          .filter(!_._2.isNull)
          .traverse { case (key, value) => value.as[Value].map(key -> _) }
    } yield Variables(vars.toMap)
  }

  implicit lazy val decodeExpectedResult: Decoder[ExpectedResult] = (c: HCursor) =>
    c.as[String]
      .map(Expected)
      .orElse(c.as[Vector[String]].map(MultiExpected))
      .orElse(c.as[Boolean].map(_ => FailExpected))

  def decodeTest(name: String): Decoder[Test] =
    Decoder.forProduct3("level", "variables", "testcases") {
      Test(name, _: Option[Int], _: Variables, _: Seq[(String, ExpectedResult)])
    }

  lazy val decodeTests: Decoder[Seq[Test]] = (c: HCursor) =>
    for {
      name2obj <- c.as[JsonObject].map(_.toList)
      tests <- name2obj.traverse { case (name, js) =>
        js.as[Test](decodeTest(name))
      }
    } yield tests

  private def runTest(path: String): Unit = {
    val tests = parseTests(path)
    val _ = for {
      Test(_, _, variables, testcases) <- tests
      (template, expectedResult) <- testcases
    } yield {
      @SuppressWarnings(Array("scalafix:DisableSyntax.isInstanceOf"))
      val result: Either[Error, String] = for {
        template <- UriTemplate.parse(template)
        expanded <- template.expandVars(variables.value.toVector: _*) match {
          // Tests assume that MissingValueError will not result in error
          case ExpandResult.PartialSuccess(value, _: MissingValueFailure) => Right(value)
          case ExpandResult.PartialSuccess(value, ExpandFailures(errors))
              if errors.exists(_.isInstanceOf[MissingValueFailure]) =>
            Right(value)
          case ExpandResult.PartialSuccess(_, error) => Left(error)
          case ExpandResult.Success(value) => Right(value)
        }
      } yield expanded
      expectedResult match {
        case Expected(expectedValue) =>
          assert(result == Right(expectedValue))
        case MultiExpected(possibleValues) =>
          if (!possibleValues.exists(result.contains)) {
            println(possibleValues)
            println(result)
          }
          assert(possibleValues.exists(result.contains))
        case FailExpected =>
          assertMatch(result) { case Left(_) => }
      }
    }
    ()
  }

  private def parseTests(path: String) = {
    val rawJs = Source.fromInputStream(getClass.getResourceAsStream(path)).mkString
    val result = for {
      json <- parse(rawJs)
      tests <- json.as(decodeTests)
    } yield tests
    result.fold(throw _, identity) /* scalafix:ok */
  }

  override def tests =
    Tests {
      test("/extended-tests.json") - runTest("/extended-tests.json")
      test("/negative-tests.json") - runTest("/negative-tests.json")
      test("/spec-examples.json") - runTest("/spec-examples.json")
      test("/spec-examples-by-section.json") - runTest("/spec-examples-by-section.json")
    }
}
