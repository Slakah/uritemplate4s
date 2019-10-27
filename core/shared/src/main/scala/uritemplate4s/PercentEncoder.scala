package uritemplate4s

import fastparse._, NoWhitespace._
import java.nio.charset.StandardCharsets
import scala.annotation.switch

private[uritemplate4s] object PercentEncoder {
  import ast._
  import UriTemplateParser._

  @inline def percentEncode(s: String): String = {
    s.getBytes(StandardCharsets.UTF_8)
      .foldLeft(new StringBuilder) {
        case (sb, byte) =>
          sb.append(memoEncodeChar(byte.toChar))
      }
      .mkString
  }

  private def memoEncodeChar(ch: Char) = (ch: @switch) match {
    case ' ' => "%20"
    case '%' => "%25"
    case '{' => "%7B"
    case '}' => "%7D"
    case '!' => "%21"
    case '*' => "%2A"
    case '\'' => "%27"
    case '(' => "%28"
    case ')' => "%29"
    case ';' => "%3B"
    case ':' => "%3A"
    case '@' => "%40"
    case '&' => "%26"
    case '=' => "%3D"
    case '+' => "%2B"
    case '$' => "%24"
    case ',' => "%2C"
    case '/' => "%2F"
    case '?' => "%3F"
    case '#' => "%23"
    case '[' => "%5B"
    case ']' => "%5D"
    case _ => encodeChar(ch)
  }

  private def encodeChar(ch: Char): String = s"%${"%04X".format(ch.toInt).substring(2)}"

  def nonUnreserved[_: P]: P[List[Literal]] =
    P(unreserved.rep(1).!.map(Encoded) | (!unreserved ~ AnyChar).rep(1).!.map(Unencoded)).rep.map(_.toList)

  def nonUnreservedAndReserved[_: P]: P[List[Literal]] =
    P((unreserved | reserved).rep(1).!.map(Encoded) | (!(unreserved | reserved) ~ AnyChar).rep(1).!.map(Unencoded)).rep
      .map(_.toList)
}
