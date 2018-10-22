package uritemplate4s

import fastparse._, NoWhitespace._

/** URI Template parser [[https://tools.ietf.org/html/rfc6570#section-2]]. */
private[uritemplate4s] object UriTemplateParser {
  // 1.5. Notational Conventions
  def alpha[_: P]: P0 = P(CharIn("a-z", "A-Z"))
  def digit[_: P]: P0 = P(CharIn("0-9"))
  def hexdig[_: P]: P0 = P(CharIn("0-9", "a-f", "A-F"))
  def pctEncoded[_: P]: P0 = P("%" ~ hexdig ~ hexdig)
  def unreserved[_: P]: P0 = P(alpha | digit | StringIn("-", ".", "_", "~"))
  def reserved[_: P]: P0 =  P(genDelims | subDelims)
  def genDelims[_: P]: P0 = P(StringIn(":", "/", "?", "#", "[", "]", "@"))
  def subDelims[_: P]: P0 =  P(CharIn(
    "!", "$", "&", "'", "(", ")",
    "*", "+", ",", ";", "="))
  def ucschar[_: P]: P0 = P(HexIntIn(
    0xA0.toChar to 0xD7FF.toChar, 0xF900.toChar to 0xFDCF.toChar, 0xFDF0.toChar to 0xFFEF.toChar,
    0x40000.toChar to 0x4FFFD.toChar, 0x50000.toChar to 0x5FFFD.toChar, 0x60000.toChar to 0x6FFFD.toChar,
    0x70000.toChar to 0x7FFFD.toChar, 0x80000.toChar to 0x8FFFD.toChar, 0x90000.toChar to 0x9FFFD.toChar,
    0xA0000.toChar to 0xAFFFD.toChar, 0xB0000.toChar to 0xBFFFD.toChar, 0xC0000.toChar to 0xCFFFD.toChar,
    0xD0000.toChar to 0xDFFFD.toChar, 0xE1000.toChar to 0xEFFFD.toChar))
  def iprivate[_: P]: P0 = P(HexIntIn(
    0xE000.toChar to 0xF8FF.toChar, 0xF0000.toChar to 0xFFFFD.toChar, 0x100000.toChar to 0x10FFFD.toChar))
  // 2. Syntax
  def uriTemplate[_: P]: P[List[Component]] = P((expression | literals).rep ~ End).map(_.toList)
  // 2.1 Literals
  def literals[_: P]: P[Literal] = P(allowedLiterals.rep(1).!.map[Literal](Encoded) | (!"}" ~ unallowedLiterals).rep(1).!.map[Literal](Unencoded))
  def allowedLiterals[_: P]: P0 = P(reserved | unreserved | pctEncoded)
  def unallowedLiterals[_: P]: P0 = P(
    HexIntIn(
      List(0x21.toChar),  0x23.toChar to 0x24.toChar, 0x26.toString, 0x28.toChar to 0x3B.toChar, List(0x3D.toChar), 0x3F.toChar to 0x5B.toChar,
      List(0x5D.toChar), List(0x5F.toChar), 0x61.toChar to 0x7A.toChar, List(0x7E.toChar))
    | ucschar | iprivate | pctEncoded)
  // 2.2. Expressions
  def expression[_: P]: P[Expression] = P("{" ~/ operator.? ~ variableList ~ "}").map {
    case (None, vl) => Expression(Simple, vl)
    case (Some(op), vl) => Expression(op, vl)
  }
  def operator[_: P]: P[Operator] = opLevel2 | opLevel3 // | opReserve
  def opLevel2[_: P]: P[Operator] = P("+".!.map(_ => Reserved) | "#".!.map(_ => Fragment))
  def opLevel3[_: P]: P[Operator] =
    P(".".!.map(_ => NameLabel) | "/".!.map(_ => PathSegment) | ";".!.map(_ => PathParameter) | "?".!.map(_ => Query) | "&".!.map(_ => QueryContinuation))
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


  def HexIntIn(chars: Seq[Char]*)(implicit ctx: P[_]): P0 = {
    CharPred(chars.flatten.contains)
  }
}
