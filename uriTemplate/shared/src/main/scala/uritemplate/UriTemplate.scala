package uritemplate

import fastparse.all._
import ListSyntax._

final case class UriTemplate(value: String) extends AnyVal {

  def expand(vars: (String, Value)*): Either[Error, String] = {
    lazy val varsMap = vars.toMap

    UriTemplateParser.uriTemplate.parse(value) match {
      case Parsed.Success(components, _) =>
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
                  Error.InvalidCombination(s"$name has the  unsupported prefix modifier for a list value of ${l.toString}") :: errs
                case (errs, (Varspec(name, Prefix(_)), AssociativeArray(arr))) =>
                  Error.InvalidCombination(s"$name has the  unsupported prefix modifier for a associative array value of ${arr.toString}") :: errs
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
          Right(result)
        } else {
          Left(Error.InvalidCombination(errorList.mkString(", ")))
        }
      case err: Parsed.Failure => Left(Error.MalformedUriTemplate(err.msg))
    }
  }

  private def explodeStringValue(s: String, operator: Operator, spec: Varspec): List[Literals] = {
    val prefixS = spec.modifier match {
      case Prefix(maxLength) => s.take(maxLength)
      case _ => s
    }
    if (operator.named) {
      val namedSep = if (s.isEmpty) operator.ifemp else "="
      Encoded(spec.varname) :: Encoded(namedSep) :: encode(prefixS, operator.allow)
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
          val namedSep = if (l.isEmpty) operator.ifemp else "="
          Encoded(spec.varname) :: Encoded(namedSep) :: literalValues
        } else {
          literalValues
        }
      case Explode =>
        val lValues = l.toList.map(encode(_, operator.allow))
        if (operator.named) {
          lValues.map { lValue =>
            val namedSep = if (lValue.isEmpty) operator.ifemp else "="
            Encoded(spec.varname) :: Encoded(namedSep) :: lValue
          }.intersperse(List(Encoded(operator.sep))).flatten
        } else {
          lValues.intersperse(List(Encoded(operator.sep))).flatten
        }
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
          val namedSep = if (tuples.isEmpty) operator.ifemp else "="
          Encoded(spec.varname) :: Encoded(namedSep) :: literalValues
        } else {
          literalValues
        }
      case Explode =>
        val nameValues = tuples.toList.map { case (n, v) => n -> encode(v, operator.allow) }
        if (operator.named) {
          nameValues.map { case (n, v) =>
            val namedSep = if (v.isEmpty) operator.ifemp else "="
            encode(n, operator.allow) ::: Encoded(namedSep) :: v
          }.intersperse(List(Encoded(operator.sep))).flatten
        } else {
          nameValues.map { case (n, v) =>
            encode(n, operator.allow) ::: Encoded("=") :: v
          }.intersperse(List(Encoded(operator.sep))).flatten
        }
    }
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

  lazy val nonUnreserved: P[List[Literals]] = P(unreserved.rep(min = 1).!.map(Encoded) | AnyChar.!.map(Unencoded)).rep.map(_.toList)
  lazy val nonUnreservedAndReserved: P[List[Literals]] = P((unreserved | reserved).rep(min = 1).!.map(Encoded) | AnyChar.!.map(Unencoded)).rep.map(_.toList)
}

private object UriTemplateParser {
  // 1.5. Notational Conventions
  lazy val alpha: P0 = P(CharIn('a' to 'z', 'A' to 'Z'))
  lazy val digit: P0 = P(CharIn('0' to '9'))
  lazy val hexdig: P0 = P(CharIn('0' to '9', 'a' to 'f', 'A' to 'F'))
  lazy val pctEncoded: P0 = P("%" ~ hexdig ~ hexdig)
  lazy val unreserved: P0 = P(alpha | digit | "-" | "." | "_" | "~")
  lazy val reserved: P0 =  P(genDelims | subDelims)
  lazy val genDelims: P0 = P(CharIn(":", "/", "?", "#", "[", "]", "@"))
  lazy val subDelims: P0 =  P(CharIn(
    "!", "$", "&", "'", "(", ")",
    "*", "+", ",", ";", "="))
  lazy val ucschar: P0 = P(CharIn(
    0xA0.toChar to 0xD7FF.toChar, 0xF900.toChar to 0xFDCF.toChar, 0xFDF0.toChar to 0xFFEF.toChar,
    0x40000.toChar to 0x4FFFD.toChar, 0x50000.toChar to 0x5FFFD.toChar, 0x60000.toChar to 0x6FFFD.toChar,
    0x70000.toChar to 0x7FFFD.toChar, 0x80000.toChar to 0x8FFFD.toChar, 0x90000.toChar to 0x9FFFD.toChar,
    0xA0000.toChar to 0xAFFFD.toChar, 0xB0000.toChar to 0xBFFFD.toChar, 0xC0000.toChar to 0xCFFFD.toChar,
    0xD0000.toChar to 0xDFFFD.toChar, 0xE1000.toChar to 0xEFFFD.toChar))
  lazy val iprivate: P0 = P(CharIn(
    0xE000.toChar to 0xF8FF.toChar, 0xF0000.toChar to 0xFFFFD.toChar, 0x100000.toChar to 0x10FFFD.toChar))
  // 2. Syntax
  lazy val uriTemplate: P[List[Component]] = P((expression | literals).rep ~ End).map(_.toList)
  // 2.1 Literals
  lazy val literals: P[Literals] = P(allowedLiterals.rep(min = 1).!.map(Encoded) | (!"}" ~ unallowedLiterals).rep(min = 1).!.map(Unencoded))
  lazy val allowedLiterals: P0 = P(reserved | unreserved | pctEncoded)
  lazy val unallowedLiterals: P0 = P(
    CharIn(
      List(0x21.toChar),  0x23.toChar to 0x24, List(0x26.toChar), 0x28.toChar to 0x3B.toChar, List(0x3D.toChar), 0x3F.toChar to 0x5B.toChar,
      List(0x5D.toChar), List(0x5F.toChar), 0x61.toChar to 0x7A.toChar, List(0x7E.toChar))
    | ucschar | iprivate | pctEncoded)
  // 2.2. Expressions
  lazy val expression: P[Expression] = P("{" ~/ operator.? ~ variableList ~ "}").map {
    case (None, vl) => Expression(Simple, vl)
    case (Some(op), vl) => Expression(op, vl)
  }
  lazy val operator: P[Operator] = opLevel2 | opLevel3 // | opReserve
  lazy val opLevel2: P[Operator] = P("+".!.map(_ => Reserved) | "#".!.map(_ => Fragment))
  lazy val opLevel3: P[Operator] =
    P(".".!.map(_ => NameLabel) | "/".!.map(_ => PathSegment) | ";".!.map(_ => PathParameter) | "?".!.map(_ => Query) | "&".!.map(_ => QueryContinuation))
  lazy val opReserve: P[Operator] = P(CharIn("=,!@|")).map(_ => Reserved)
  // 2.3. Variables
  lazy val variableList: P[List[Varspec]] = P(varspec.rep(min = 1, sep = ",")).map(_.toList)
  lazy val varspec: P[Varspec] = P(varname ~ modifierLevel4.?).map {
    case (n, Some(m)) => Varspec(n, m)
    case (n, None) => Varspec(n, EmptyModifier)
  }
  lazy val varname: P[String] = P(varchar ~ (".".? ~ varchar).rep).!
  lazy val varchar: P0 = P(alpha | digit | "_" | pctEncoded)
  // 2.4. Value Modifiers
  lazy val modifierLevel4: P[ModifierLevel4] = prefix | explode
  lazy val prefix: P[ModifierLevel4] = P(":" ~ digit.rep(min = 1, max = 4).!).map(raw => Prefix(raw.toInt))
  lazy val explode: P[ModifierLevel4] = P("*").map(_ => Explode)
}
