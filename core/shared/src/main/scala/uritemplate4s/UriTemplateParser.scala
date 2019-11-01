package uritemplate4s

import fastparse._, NoWhitespace._
import uritemplate4s.ast._

/** URI Template parser [[https://tools.ietf.org/html/rfc6570#section-2]]. */
private[uritemplate4s] object UriTemplateParser {
  // 1.5. Notational Conventions
  def alpha[_: P]: P0 = P(CharIn("a-z", "A-Z"))
  def digit[_: P]: P0 = P(CharIn("0-9"))
  def hexdig[_: P]: P0 = P(CharIn("0-9", "a-f", "A-F"))
  def pctEncoded[_: P]: P0 = P("%" ~ hexdig ~ hexdig)
  def unreserved[_: P]: P0 = P(alpha | digit | StringIn("-", ".", "_", "~"))

  // format: off
  def reserved[_: P]: P0 =  P(CharIn(
    ":", "/", "?", "#", "[", "]", "@", // gen-delims
    "!", "$", "&", "'", "(", ")", "*", "+", ",", ";", "=" // sub-delims
  ))
  // format: on
  def ucschar[_: P]: P0 = P(CharIn("\u00a0-\ud7ff", "\uf900-\ufdcf", "\ufdf0-\uffef", "\u0000-\ufffd"))
  def iprivate[_: P]: P0 = P(CharIn("\ue000-\uf8ff", "\u0000-\ufffd"))
  // 2. Syntax
  def uriTemplate[_: P]: P[List[Component]] = P((expression | literals).rep ~ End).map(_.toList)

  // 2.1 Literals
  def literals[_: P]: P[LiteralComponent] =
    P(allowedLiterals.rep(1).!.map[Literal](Encoded) | (!"}" ~ unallowedLiterals).rep(1).!.map[Literal](Unencoded))
      .map(LiteralComponent)
  def allowedLiterals[_: P]: P0 = P(reserved | unreserved | pctEncoded)

  // format: off
  def unallowedLiterals[_: P]: P0 = P(
    CharIn(
      "\u0021", "\u0023-\u0024", "\u0026", "\u0028-\u003B", "\u003D", "\u003F-\u005B",
      "\u005D", "\u005F", "\u0061-\u007A", "\u007E") | ucschar | iprivate | pctEncoded)
  // format: on
  // 2.2. Expressions
  def expression[_: P]: P[Expression] = P("{" ~/ operator.? ~ variableList ~ "}").map {
    case (None, vl) => Expression(Simple, vl)
    case (Some(op), vl) => Expression(op, vl)
  }
  def operator[_: P]: P[Operator] = opLevel2 | opLevel3 // | opReserve
  def opLevel2[_: P]: P[Operator] = P("+".!.map(_ => Reserved) | "#".!.map(_ => Fragment))

  def opLevel3[_: P]: P[Operator] =
    P(
      ".".!.map(_ => NameLabel) |
        "/".!.map(_ => PathSegment) |
        ";".!.map(_ => PathParameter) |
        "?".!.map(_ => Query) |
        "&".!.map(_ => QueryContinuation)
    )
  def opReserve[_: P]: P[Operator] = P(CharIn("=,!@|")).map(_ => Reserved)
  // 2.3. Variables
  def variableList[_: P]: P[List[Varspec]] = P(varspec.rep(1, sep = ",")).map(_.toList)

  def varspec[_: P]: P[Varspec] = P(varname ~ modifierLevel4.?).map {
    case (n, Some(m)) => Varspec(n, m)
    case (n, None) => Varspec(n, EmptyModifier)
  }
  def varname[_: P]: P[String] = P(varchar ~ (".".? ~ varchar).rep).!
  def varchar[_: P]: P0 = P(alpha | digit | "_" | pctEncoded)
  // 2.4. Value Modifiers
  def modifierLevel4[_: P]: P[ModifierLevel4] = prefix | explode
  def prefix[_: P]: P[ModifierLevel4] = P(":" ~ digit.rep(1, max = 4).!).map(raw => Prefix(raw.toInt))
  def explode[_: P]: P[ModifierLevel4] = P("*").map(_ => Explode)
}
