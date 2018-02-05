package uritemplate

import scala.io.Source
import scala.util.{Failure, Try}

import cats.instances.list._
import cats.instances.either._
import cats.syntax.either._
import cats.syntax.traverse._
import io.circe._
import io.circe.parser._
import utest._
import utest.framework.TestPath

object ExternalTests extends TestSuite {

  sealed trait ExpectedResult
  case class Expected(result: String) extends ExpectedResult
  case class MultiExpected(results: Seq[String]) extends ExpectedResult
  case object FailExpected extends ExpectedResult

  final case class Test(
    name: String,
    level: Option[Int],
    variables: Map[String, Value],
    testcases: Seq[(String, ExpectedResult)])

  implicit lazy val decodeValue: Decoder[Value] = (c: HCursor) =>
    c.as[String].map(StringValue)
    .orElse(c.as[JsonNumber].map(n => StringValue(n.toString)))
    .orElse(c.as[Vector[String]].map(ListValue))
    .orElse(c.as[Map[String, String]].map(m => AssociativeArray(m.toVector)))

  implicit lazy val decodeExpectedResult: Decoder[ExpectedResult] = (c: HCursor) =>
    c.as[String].map(Expected)
    .orElse(c.as[Vector[String]].map(MultiExpected))
    .orElse(c.as[Boolean].map(_ => FailExpected))

  def decodeTest(name: String): Decoder[Test] =
    Decoder.forProduct3("level", "variables", "testcases") {
      Test(name, _: Option[Int], _: Map[String, Value], _: Seq[(String, ExpectedResult)])
    }


  lazy val decodeTests: Decoder[Seq[Test]] = (c: HCursor) => for {
    name2obj <- c.as[JsonObject].map(_.toList)
    tests <- name2obj.traverse { case (name, js) =>
      js.as[Test](decodeTest(name))
    }
  } yield tests

  private def test()(implicit path: TestPath): Unit = {
    val tests = parseTests(path.value.mkString)
    for {
      Test(name, _, variables, testcases) <- tests
      _ = println(name)
      (template, expectedResult) <- testcases
    } yield {
      val resultTry = Try(UriTemplate(template).expand(variables.toVector: _*))
      expectedResult match {
        case Expected(expectedValue) =>
          val result = resultTry.get
          assert(result == expectedValue)
        case MultiExpected(possibleValues) =>
          val result = resultTry.get
          assert(possibleValues.contains(result))
        case FailExpected =>
          assertMatch(resultTry) { case Failure(_) => }
      }
    }
  }

  private def parseTests(path: String) = {
    val rawJs = Source.fromInputStream(getClass.getResourceAsStream(path)).mkString
    val result = for {
      json <- parse(rawJs)
      tests <- json.as(decodeTests)
    } yield tests
    result.fold(throw _, identity)
  }

  override def tests = Tests {
    "/extended-tests.json" - test()
//    "/negative-tests.json" - test()
    "/spec-examples.json" - test()
//    "/spec-examples-by-section.json" - test()
  }
}
