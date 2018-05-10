package uritemplate4s

import fastparse.all._

private[uritemplate4s] object PercentEncoder {
  import UriTemplateParser._

  @inline def percentEncode(s: String): String =
    s.getBytes("UTF-8").flatMap(byte => encodeChar(byte.toChar)).mkString

  private def encodeChar(ch: Char): String = s"%${"%04x".format(ch.toInt).substring(2).toUpperCase}"

  lazy val nonUnreserved: P[List[Literal]] =
    P(unreserved.rep(min = 1).!.map(Encoded) | (!unreserved ~ AnyChar).rep(min = 1).!.map(Unencoded)).rep.map(_.toList)
  lazy val nonUnreservedAndReserved: P[List[Literal]] =
    P((unreserved | reserved).rep(min = 1).!.map(Encoded) | (!(unreserved | reserved) ~ AnyChar).rep(min = 1).!.map(Unencoded)).rep.map(_.toList)
}
