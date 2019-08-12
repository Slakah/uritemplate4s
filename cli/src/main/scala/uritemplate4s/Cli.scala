package uritemplate4s

import caseapp._
import caseapp.core.{Error => CaseAppError}
import caseapp.core.argparser.{ArgParser, SimpleArgParser}
import cats.implicits._

import ArgParsers._

final case class Options(
  template: UriTemplate,
  vars: Vars
)

object Cli extends CaseApp[Options] {

  override def run(
    options: Options,
    remainingArgs: RemainingArgs
  ): Unit = println(options.template.expandVars(options.vars: _*)
    .value)
}

object ArgParsers {

  implicit val uriTemplateArgParser: ArgParser[UriTemplate] =
    SimpleArgParser.from("uritemplate") { s =>
      UriTemplate.parse(s)
        .left.map(err =>
          CaseAppError.MalformedValue("uritemplate", err.message)
        )
    }

  final type Vars = List[(String, Value)]

  implicit val valueArgParser: ArgParser[Vars] =
    SimpleArgParser.from("value") { s =>
      Right(s.split("\\s").toList.map { entry =>
        val index = entry.indexOf("=")
        if (index == -1) {
          Left(CaseAppError.MalformedValue(
            "key=value",
            "expected key=value"))
          ???
        } else {
          val (key, value) = entry.splitAt(index)
          (key, StringValue(value.stripPrefix("=")))
        }
      })
    }
}
