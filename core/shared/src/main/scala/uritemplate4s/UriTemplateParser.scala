package uritemplate4s

import fastparse.all._

/** URI Template parser [[https://tools.ietf.org/html/rfc6570#section-2]]. */
private[uritemplate4s] object UriTemplateParser {
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
  lazy val literals: P[Literal] = P(allowedLiterals.rep(min = 1).!.map(Encoded) | (!"}" ~ unallowedLiterals).rep(min = 1).!.map(Unencoded))
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
