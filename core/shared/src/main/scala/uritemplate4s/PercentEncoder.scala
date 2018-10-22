package uritemplate4s

import fastparse._, NoWhitespace._

private[uritemplate4s] object PercentEncoder {
  import UriTemplateParser._

  @inline def percentEncode(s: String): String =
    s.getBytes("UTF-8").flatMap(byte => encodeChar(byte.toChar)).mkString

  private def encodeChar(ch: Char): String = s"%${"%04x".format(ch.toInt).substring(2).toUpperCase}"

  def nonUnreserved[_: P]: P[List[Literal]] =
    P(unreserved.rep(1).!.map(Encoded) | (!unreserved ~ AnyChar).rep(1).!.map(Unencoded)).rep.map(_.toList)
  def nonUnreservedAndReserved[_: P]: P[List[Literal]] =
    P((unreserved | reserved).rep(1).!.map(Encoded) | (!(unreserved | reserved) ~ AnyChar).rep(1).!.map(Unencoded)).rep.map(_.toList)
}
