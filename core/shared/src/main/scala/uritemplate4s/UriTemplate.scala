package uritemplate4s

import fastparse.all._
import uritemplate4s.Error.MalformedUriTemplate
import uritemplate4s.ListSyntax._
import uritemplate4s.UriTemplate._


trait UriTemplate {
  def expand(vars: (String, Value)*): Result
}

private final class ComponentsUriTemplate(components: List[Component]) extends UriTemplate {

  def expand(vars: (String, Value)*): Result = {
    lazy val varsMap = vars.toMap

    val errorOrLiteralsList: List[Either[Error, Literals]] = for {
      component <- components
      literals <- component match {
        case literals: Literals => List(literals).map(Right.apply)
        case Expression(operator, variableList) =>
          val spec2value: List[(Varspec, Value)] = variableList.flatMap { spec =>
            varsMap.get(spec.varname) match {
              case None => None
              case Some(ListValue(Nil)) => None
              case Some(AssociativeArray(Nil)) => None
              case Some(v) => Some(spec -> v)
            }
          }
          val errorList = spec2value.foldLeft(List.empty[Error]) {
            case (errs, (Varspec(name, Prefix(_)), ListValue(l))) =>
              val listShow = s"[${l.mkString(", ")}]"
              Error.InvalidCombination(s"$name has the  unsupported prefix modifier for a list value of $listShow") :: errs
            case (errs, (Varspec(name, Prefix(_)), AssociativeArray(arr))) =>
              val assocShow = "{" + arr
                .map { case (k, v) => s""""$k": "$v"""" }
                .mkString(", ") + "}"
              Error.InvalidCombination(
                s"$name has the  unsupported prefix modifier for a associative array value of $assocShow") :: errs
            case (errs, _) =>
              errs
          }
          errorList match {
            case Nil =>
              val exploded: List[List[Literals]] = spec2value.map { case (spec, v) =>
                v match {
                  case StringValue(s) => explodeStringValue(s, operator, spec)
                  case ListValue(l) => explodeListValue(l, operator, spec)
                  case AssociativeArray(tuples) => explodeAssociativeArray(tuples, operator, spec)
                }
              }
              (exploded match {
                case Nil => List.empty
                case xs => Encoded(operator.first) :: xs.intersperse(List(Encoded(operator.sep))).flatten
              }).map(Right.apply)
            case errors => errors.map(Left.apply)
          }
      }
    } yield literals

    val (errorList, literalsList) = errorOrLiteralsList.reverse.foldLeft[(List[Error], List[Literals])](Nil -> Nil) {
      case ((errs, literals), Left(err)) => (err :: errs) -> literals
      case ((errs, literals), Right(lits)) => errs -> (lits :: literals)
    }

    if (errorList.isEmpty) {
      val result = literalsList.map {
        case Encoded(encoded) => encoded
        case Unencoded(unencoded) => PercentEncoder.percentEncode(unencoded)
      }.mkString
      Success(result)
    } else {
      // TODO: add result
      PartialSuccess("foo", Error.InvalidCombination(errorList.mkString(", ")))
    }
  }

  private def explodeStringValue(s: String, operator: Operator, spec: Varspec): List[Literals] = {
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

private object PercentEncoder {
  import UriTemplateParser._

  @inline def percentEncode(s: String): String =
    s.getBytes("UTF-8").flatMap(byte => encodeChar(byte.toChar)).mkString

  private def encodeChar(ch: Char): String = s"%${"%04x".format(ch.toInt).substring(2).toUpperCase}"

  lazy val nonUnreserved: P[List[Literals]] =
    P(unreserved.rep(min = 1).!.map(Encoded) | (!unreserved ~ AnyChar).rep(min = 1).!.map(Unencoded)).rep.map(_.toList)
  lazy val nonUnreservedAndReserved: P[List[Literals]] =
    P((unreserved | reserved).rep(min = 1).!.map(Encoded) | (!(unreserved | reserved) ~ AnyChar).rep(min = 1).!.map(Unencoded)).rep.map(_.toList)
}
