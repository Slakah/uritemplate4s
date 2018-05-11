package uritemplate4s

import fastparse.all.Parsed
import uritemplate4s.ListSyntax._
import uritemplate4s.UriTemplate._

trait UriTemplate {
  /**
    * Expand the parsed URI Template using the supplied vars.
    * @param vars name value pairs to be substituted in the template.
    * @return the expanded template.
    */
  def expand(vars: (String, Value)*): Result
}

private final class ComponentsUriTemplate(components: List[Component]) extends UriTemplate {

  override def expand(vars: (String, Value)*): Result = {
    lazy val varsMap = vars.toMap

    val errorsAndLiteralsList: List[(List[ExpandError], List[Literal])] = for {
      component <- components
      errorsAndLiterals = component match {
        case literal: Literal => List.empty -> List(literal)
        case Expression(operator, variableList) =>
          val spec2value: List[(Varspec, Value)] = variableList.flatMap { spec =>
            varsMap.get(spec.varname) match {
              case None => None // TODO: create a warning?
              case Some(ListValue(Nil)) => None
              case Some(AssociativeArray(Nil)) => None
              case Some(v) => Some(spec -> v)
            }
          }
          val errorList = buildSpecExpansionErrorList(spec2value)
          val literalList = explodeSpecs(spec2value, operator)
          (errorList, literalList)
      }
    } yield errorsAndLiterals

    val (errorList, literalList) = errorsAndLiteralsList.foldLeft(List.empty[ExpandError] -> List.empty[Literal]) {
      case ((errorAcc, literalsAcc), (errors, literals)) =>
        (errorAcc ::: errors) -> (literalsAcc ::: literals)
    }

    val result = literalList.map {
      case Encoded(encoded) => encoded
      case Unencoded(unencoded) => PercentEncoder.percentEncode(unencoded)
    }.mkString

    if (errorList.isEmpty) {
      Success(result)
    } else {
      PartialSuccess(result, InvalidCombinationError(errorList.mkString(", ")))
    }
  }

  /** Collect any spec to value mismatches. */
  private def buildSpecExpansionErrorList(spec2value: List[(Varspec, Value)]) = {
    spec2value.foldLeft(List.empty[ExpandError]) {
      case (errs, (Varspec(name, Prefix(_)), ListValue(l))) =>
        val listShow = s"[${l.mkString(", ")}]"
        InvalidCombinationError(s"$name has the unsupported prefix modifier for a list value of $listShow") :: errs
      case (errs, (Varspec(name, Prefix(_)), AssociativeArray(arr))) =>
        val assocShow = "{" + arr
          .map { case (k, v) => s""""$k": "$v"""" }
          .mkString(", ") + "}"
        InvalidCombinationError(
          s"$name has the unsupported prefix modifier for a associative array value of $assocShow") :: errs
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
      case (EmptyModifier | Prefix(_)) =>
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
          .intersperse(List(Encoded(operator.sep))).flatten
    }
  }

  private def explodeAssociativeArray(
    tuples: Seq[(String, String)],
    operator: Operator,
    spec: Varspec
  ): List[Literal] = {
    spec.modifier match {
      case (EmptyModifier | Prefix(_)) =>
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
          .intersperse(List(Encoded(operator.sep))).flatten
    }
  }

  @inline private def namedValue(operator: Operator, spec: Varspec, isEmpty: Boolean, values: List[Literal]) = {
    val namedSep = if (isEmpty) operator.ifemp else "="
    Encoded(spec.varname) :: Encoded(namedSep) :: values
  }

  @inline private def namedValue(operator: Operator, varnameLiterals: List[Literal], isEmpty: Boolean, values: List[Literal]) = {
    val namedSep = if (isEmpty) operator.ifemp else "="
    varnameLiterals ::: Encoded(namedSep) :: values
  }

  private def encode(
    s: String,
    allow: Allow
  ): List[Literal] = {
    val encoder = allow match {
      case Allow.U => PercentEncoder.nonUnreserved
      case Allow.`U+R` => PercentEncoder.nonUnreservedAndReserved
    }
    encoder.parse(s).get.value
  }
}

object UriTemplate {
  /** Parse a URI Template according to [[https://tools.ietf.org/html/rfc6570]]. */
  def parse(template: String): Either[ParseError, UriTemplate] = {

    UriTemplateParser.uriTemplate.parse(template) match {
      case Parsed.Success(components, _) => Right(new ComponentsUriTemplate(components))
      case err: Parsed.Failure => Left(MalformedUriTemplateError(err.index, err.msg))
    }
  }

  /**
    * A successfully parsed uri template can always be expanded, but there might be some
    * warnings associated.
    */
  sealed trait Result {
    def value: String

    def toEither: Either[ExpandError, String]
  }

  /** The [[UriTemplate]] was successfully expanded. */
  final case class Success(override val value: String) extends Result {
    override def toEither: Either[ExpandError, String] = Right(value)
  }

  /**
    * The [[UriTemplate]] expansion was partially successful.
    * In most cases, this warning can be ignored.
    */
  final case class PartialSuccess(
    override val value: String,
    error: ExpandError
  ) extends Result {
    override def toEither: Either[ExpandError, String] = Left(error)
  }
}

/** Represents a parsed URI Template component. */
private sealed trait Component

/** URI Template literal [[https://tools.ietf.org/html/rfc6570#section-2.1]]. */
private sealed trait Literal extends Component {
  def value: String
}
/** A [[Literal]] which is encoded. */
private final case class Encoded(override val value: String) extends Literal
/** A [[Literal]] which is unencoded, and will need to be encoded. */
private final case class Unencoded(override val value: String) extends Literal

/** Template expression [[https://tools.ietf.org/html/rfc6570#section-2.2]]. */
private final case class Expression(operator: Operator, variableList: List[Varspec]) extends Component

private sealed class Operator(val first: String, val sep: String, val named: Boolean, val ifemp: String, val allow: Allow)
// https://tools.ietf.org/html/rfc6570#appendix-A
private case object Simple extends Operator("", ",", false, "", Allow.U)
private case object Reserved extends Operator("", ",", false, "", Allow.`U+R`)
private case object Fragment extends Operator("#", ",", false, "", Allow.`U+R`)
private case object NameLabel extends Operator(".", ".", false, "", Allow.U)
private case object PathSegment extends Operator("/", "/", false, "", Allow.U)
private case object PathParameter extends Operator(";", ";", true, "", Allow.U)
private case object Query extends Operator("?", "&", true, "=", Allow.U)
private case object QueryContinuation extends Operator("&", "&", true, "=", Allow.U)

private sealed trait Allow
private object Allow {
  case object U extends Allow
  case object `U+R` extends Allow
}

private final case class Varspec(varname: String, modifier: ModifierLevel4)

/** Value modifier [[https://tools.ietf.org/html/rfc6570#section-2.4]]. */
private sealed trait ModifierLevel4
private case object EmptyModifier extends ModifierLevel4
private final case class Prefix(maxLength: Int) extends ModifierLevel4
private case object Explode extends ModifierLevel4
