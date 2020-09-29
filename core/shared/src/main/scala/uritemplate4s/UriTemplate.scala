package uritemplate4s

import fastparse._
import uritemplate4s.ListSyntax._
import uritemplate4s.ast._

private[uritemplate4s] trait UriTemplateBase {

  /**
    * Expand the parsed URI Template using the supplied vars.
    * @param vars name value pairs to be substituted in the template.
    * @return the expanded template.
    */
  def expandVars(vars: (String, Value)*): ExpandResult
}

trait UriTemplate extends UriTemplateBase with UriTemplateArities

final case class ComponentsUriTemplate(private val components: List[Component]) extends UriTemplate {

  override def expandVars(vars: (String, Value)*): ExpandResult = {
    lazy val varsMap = vars.toMap

    val errorsAndLiteralsList: List[(List[ExpandFailure], List[Literal])] = for {
      component <- components
      errorsAndLiterals = component match {
        case LiteralComponent(literal) => List.empty -> List(literal)
        case Expression(operator, variableList) =>
          val spec2optValue: List[(Varspec, Option[Value])] = variableList.flatMap { spec =>
            varsMap.get(spec.varname) match {
              case None => Some(spec -> None) // TODO: create a warning?
              case Some(ListValue(Nil)) => None
              case Some(AssociativeArray(Nil)) => None
              case Some(v) => Some(spec -> Some(v))
            }
          }
          val errorList = buildSpecExpansionErrorList(spec2optValue)
          val spec2value = spec2optValue.flatMap {
            case (spec, Some(value)) => Some(spec -> value)
            case (spec, None) => None
          }
          val literalList = explodeSpecs(spec2value, operator)
          (errorList, literalList)
      }
    } yield errorsAndLiterals

    val (errorList, literalList) = errorsAndLiteralsList.foldLeft(List.empty[ExpandFailure] -> List.empty[Literal]) {
      case ((errorAcc, literalsAcc), (errors, literals)) =>
        (errorAcc ::: errors) -> (literalsAcc ::: literals)
    }

    val result = literalList.map {
      case Encoded(encoded) => encoded
      case Unencoded(unencoded) => PercentEncoder.percentEncode(unencoded)
    }.mkString

    if (errorList.isEmpty) {
      ExpandResult.Success(result)
    } else {
      ExpandResult.PartialSuccess(
        result,
        errorList match {
          case error :: Nil => error
          case _ => ExpandFailures(errorList)
        }
      )
    }
  }

  /** Collect any spec to value mismatches. */
  private def buildSpecExpansionErrorList(spec2optValue: List[(Varspec, Option[Value])]) = {
    spec2optValue.foldLeft(List.empty[ExpandFailure]) {
      case (errs, (Varspec(name, _), None)) => MissingValueFailure(name) :: errs
      case (errs, (Varspec(name, Prefix(_)), Some(ListValue(l)))) =>
        val listShow = s"[${l.mkString(", ")}]"
        InvalidCombinationFailure(s"$name has the unsupported prefix modifier for a list value of $listShow") :: errs
      case (errs, (Varspec(name, Prefix(_)), Some(AssociativeArray(arr)))) =>
        val assocShow = "{" + arr
          .map { case (k, v) => s""""$k": "$v"""" }
          .mkString(", ") + "}"
        InvalidCombinationFailure(
          s"$name has the unsupported prefix modifier for a associative array value of $assocShow"
        ) :: errs
      case (errs, _) =>
        errs
    }
  }

  private def explodeSpecs(spec2value: List[(Varspec, Value)], operator: Operator) = {
    val exploded: List[List[Literal]] = spec2value.map { case (spec, value) =>
      value match {
        case StringValue(s) => explodeStringValue(s, operator, spec)
        case ListValue(l) => explodeListValue(l, operator, spec)
        case AssociativeArray(tuples) => explodeAssociativeArray(tuples, operator, spec)
      }
    }
    exploded match {
      case Nil => List.empty
      case xs => Encoded(operator.first) :: xs.intersperse(List(Encoded(operator.sep))).flatten
    }
  }

  private def explodeStringValue(s: String, operator: Operator, spec: Varspec) = {
    val prefixS = spec.modifier match {
      case Prefix(maxLength) => s.take(maxLength)
      case _ => s
    }
    if (operator.named) {
      namedValue(operator, spec, s.isEmpty, encode(prefixS, operator.allow))
    } else {
      encode(prefixS, operator.allow)
    }
  }

  private def explodeListValue(
    l: Seq[String],
    operator: Operator,
    spec: Varspec
  ): List[Literal] = {
    spec.modifier match {
      case EmptyModifier | Prefix(_) =>
        val literalValues = l.toList.map(encode(_, operator.allow)).intersperse(List(Encoded(","))).flatten

        if (operator.named) {
          namedValue(operator, spec, l.isEmpty, literalValues)
        } else {
          literalValues
        }
      case Explode =>
        val lValues = l.toList.map(encode(_, operator.allow))

        lValues
          .map { lValue =>
            if (operator.named) {
              namedValue(operator, spec, lValue.isEmpty, lValue)
            } else {
              lValue
            }
          }
          .intersperse(List(Encoded(operator.sep)))
          .flatten
    }
  }

  private def explodeAssociativeArray(
    tuples: Seq[(String, String)],
    operator: Operator,
    spec: Varspec
  ): List[Literal] = {
    spec.modifier match {
      case EmptyModifier | Prefix(_) =>
        val nameValues = tuples.toList.flatMap { case (n, v) => List(n, v) }
        val literalValues = nameValues.map(encode(_, operator.allow)).intersperse(List(Encoded(","))).flatten

        if (operator.named) {
          namedValue(operator, spec, tuples.isEmpty, literalValues)
        } else {
          literalValues
        }
      case Explode =>
        val nameValues = tuples.toList.map { case (n, v) => n -> encode(v, operator.allow) }
        nameValues
          .map { case (n, v) =>
            val varnameLiterals = encode(n, operator.allow)
            if (operator.named) {
              namedValue(operator, varnameLiterals, v.isEmpty, v)
            } else {
              varnameLiterals ::: Encoded("=") :: v
            }
          }
          .intersperse(List(Encoded(operator.sep)))
          .flatten
    }
  }

  @inline private def namedValue(operator: Operator, spec: Varspec, isEmpty: Boolean, values: List[Literal]) = {
    val namedSep = if (isEmpty) operator.ifemp else "="
    Encoded(spec.varname) :: Encoded(namedSep) :: values
  }

  @inline private def namedValue(
    operator: Operator,
    varnameLiterals: List[Literal],
    isEmpty: Boolean,
    values: List[Literal]
  ) = {
    val namedSep = if (isEmpty) operator.ifemp else "="
    varnameLiterals ::: Encoded(namedSep) :: values
  }

  private def encode(
    s: String,
    allow: Allow
  ): List[Literal] = {
    val result = allow match {
      case Allow.U => parse(s, PercentEncoder.nonUnreserved(_))
      case Allow.`U+R` => parse(s, PercentEncoder.nonUnreservedAndReserved(_))
    }
    result.get.value
  }
}

object UriTemplate {

  /** Parse a URI Template according to [[https://tools.ietf.org/html/rfc6570]]. */
  def parse(template: String): Either[ParseFailure, UriTemplate] = {
    fastparse.parse(template, UriTemplateParser.uriTemplate(_)) match {
      case Parsed.Success(components, _) => Right(ComponentsUriTemplate(components))
      case err: Parsed.Failure => Left(ParseFailure(err.index, err.msg))
    }
  }
}
