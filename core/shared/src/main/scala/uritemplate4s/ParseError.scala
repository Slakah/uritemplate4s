package uritemplate4s

/** Error when parsing a [[UriTemplate]]. */
sealed trait ParseError {
  def message: String
}

/**
  * Unable to parse the supplied malformed URI Template.
  * @param index the index where parsing failed.
  * @param message error message.
  */
final case class MalformedUriTemplateError(index: Int, override val message: String) extends ParseError

