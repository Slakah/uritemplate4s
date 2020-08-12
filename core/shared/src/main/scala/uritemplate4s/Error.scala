package uritemplate4s

/** Base error type. */
sealed abstract class Error extends Exception {
  final override def fillInStackTrace(): Throwable = this
}

/** Error when a [[UriTemplate]] is expanded. */
sealed abstract class ExpandFailure extends Error {
  def message: String
}

final case class ExpandFailures(errors: List[ExpandFailure]) extends ExpandFailure {
  override def message: String = errors.map(_.message).mkString(", ")
  override def getMessage: String = message
}

/** Error when the value supplied for substitution is not supported. */
final case class InvalidCombinationFailure(message: String) extends ExpandFailure {
  override def getMessage: String = message
}

/**
  * Error when a value has not been supplied for the template variable.
  * {{{
  * >>> uritemplate"http://{missingval}.com{/present}".expand("present" -> "foo").toEither
  * Left(uritemplate4s.MissingValueFailure: missingval wasn't supplied as a variable value)
  * }}}
  */
final case class MissingValueFailure(varname: String) extends ExpandFailure {
  override val message: String = s"$varname wasn't supplied as a variable value"
  override def getMessage: String = message
}

/**
  * Error when parsing a [[UriTemplate]].
  * @param index the index where parsing failed.
  * @param message error message.
  */
final case class ParseFailure(index: Int, message: String) extends Error {
  override def getMessage: String = message
}
