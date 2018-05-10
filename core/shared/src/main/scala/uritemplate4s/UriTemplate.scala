package uritemplate4s

import fastparse.all.Parsed
import uritemplate4s.Error.MalformedUriTemplate
import uritemplate4s.ListSyntax._
import uritemplate4s.UriTemplate._


trait UriTemplate {
  def expand(vars: (String, Value)*): Result
}

private final class ComponentsUriTemplate(components: List[Component]) extends UriTemplate {

  def expand(vars: (String, Value)*): Result = {
    lazy val varsMap = vars.toMap

    val errorsAndLiteralsList: List[(List[Error], List[Literals])] = for {
      component <- components
      errorsAndLiterals = component match {
        case literals: Literals => List.empty -> List(literals)
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
          val literals = explodeSpecs(spec2value, operator)
          (errorList, literals)
      }
    } yield errorsAndLiterals

    val (errorList, literalsList) = errorsAndLiteralsList.foldLeft(List.empty[Error] -> List.empty[Literals]) {
      case ((errorAcc, literalsAcc), (errors, literals)) =>
        (errorAcc ::: errors) -> (literalsAcc ::: literals)
    }

    val result = literalsList.map {
      case Encoded(encoded) => encoded
      case Unencoded(unencoded) => PercentEncoder.percentEncode(unencoded)
    }.mkString

    if (errorList.isEmpty) {
      Success(result)
    } else {
      PartialSuccess(result, Error.InvalidCombination(errorList.mkString(", ")))
    }
  }

  /** Collect any spec to value mismatches. */
  private def buildSpecExpansionErrorList(spec2value: List[(Varspec, Value)]) = {
    spec2value.foldLeft(List.empty[Error]) {
      case (errs, (Varspec(name, Prefix(_)), ListValue(l))) =>
        val listShow = s"[${l.mkString(", ")}]"
        Error.InvalidCombination(s"$name has the unsupported prefix modifier for a list value of $listShow") :: errs
      case (errs, (Varspec(name, Prefix(_)), AssociativeArray(arr))) =>
        val assocShow = "{" + arr
          .map { case (k, v) => s""""$k": "$v"""" }
          .mkString(", ") + "}"
        Error.InvalidCombination(
          s"$name has the unsupported prefix modifier for a associative array value of $assocShow") :: errs
      case (errs, _) =>
        errs
    }
  }

  private def explodeSpecs(spec2value: List[(Varspec, Value)], operator: Operator) = {
    val exploded: List[List[Literals]] = spec2value.map { case (spec, value) =>
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
  ): List[Literals] = {
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
  ): List[Literals] = {
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

  @inline private def namedValue(operator: Operator, spec: Varspec, isEmpty: Boolean, values: List[Literals]) = {
    val namedSep = if (isEmpty) operator.ifemp else "="
    Encoded(spec.varname) :: Encoded(namedSep) :: values
  }

  @inline private def namedValue(operator: Operator, varnameLiterals: List[Literals], isEmpty: Boolean, values: List[Literals]) = {
    val namedSep = if (isEmpty) operator.ifemp else "="
    varnameLiterals ::: Encoded(namedSep) :: values
  }

  private def encode(
    s: String,
    allow: Allow
  ): List[Literals] = {
    val encoder = allow match {
      case Allow.U => PercentEncoder.nonUnreserved
      case Allow.`U+R` => PercentEncoder.nonUnreservedAndReserved
    }
    encoder.parse(s).get.value
  }
}

object UriTemplate {

  def parse(template: String): Either[MalformedUriTemplate, UriTemplate] = {

    UriTemplateParser.uriTemplate.parse(template) match {
      case Parsed.Success(components, _) => Right(new ComponentsUriTemplate(components))
      case err: Parsed.Failure => Left(Error.MalformedUriTemplate(err.msg))
    }
  }

  /**
    * A successfully parsed uri template can always be expanded, but there maybe some warnings
    * associated.
    */
  sealed trait Result {
    def value: String

    def toEither: Either[Error, String]
  }

  /** The [[UriTemplate]] was successfully expanded. */
  final case class Success(override val value: String) extends Result {
    override def toEither: Either[Error, String] = Right(value)
  }

  /**
    * The [[UriTemplate]] expansion was partially successful.
    * In most cases, this warning can be ignored.
    */
  final case class PartialSuccess(
    override val value: String,
    error: Error
  ) extends Result {
    override def toEither: Either[Error, String] = Left(error)
  }
}

sealed trait Value
private final case class StringValue(value: String) extends Value
private final case class ListValue(value: Seq[String]) extends Value
private final case class AssociativeArray(value: Seq[(String, String)]) extends Value
object Value {
  implicit def string2stringValue(s: String): Value = StringValue(s)
  implicit def seqString2listValue(seq: Seq[String]): Value = ListValue(seq)
  implicit def seqTuple2associativeValue(tuples: Seq[(String, String)]): Value = AssociativeArray(tuples)
}

private sealed trait Component

private sealed trait Literals extends Component {
  def value: String
}
private final case class Encoded(override val value: String) extends Literals
private final case class Unencoded(override val value: String) extends Literals

private final case class Expression(operator: Operator, variableList: List[Varspec]) extends Component

private sealed class Operator(val first: String, val sep: String, val named: Boolean, val ifemp: String, val allow: Allow)
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

private sealed trait ModifierLevel4
private case object EmptyModifier extends ModifierLevel4
private final case class Prefix(maxLength: Int) extends ModifierLevel4
private case object Explode extends ModifierLevel4
