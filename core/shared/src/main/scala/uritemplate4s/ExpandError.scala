package uritemplate4s

/**
  * A successfully parsed uri template can always be expanded, but there might be some
  * warnings associated.
  */
sealed trait ExpandResult {
  def value: String

  def toEither: Either[Seq[ExpandError], String]
}

object ExpandResult {

  /** The [[UriTemplate]] was successfully expanded. */
  final case class Success(override val value: String) extends ExpandResult {
    @inline override def toEither: Either[Seq[ExpandError], String] = Right(value)
  }

  /**
    * The [[UriTemplate]] expansion was partially successful.
    * In most cases, this warning can be ignored.
    */
  final case class PartialSuccess(
    override val value: String,
    errors: Seq[ExpandError]
  ) extends ExpandResult {
    @inline override def toEither: Either[Seq[ExpandError], String] = Left(errors)
  }
}

/** Error when a [[UriTemplate]] is expanded. */
sealed trait ExpandError {
  def message: String
}

/** Error when the value supplied for substitution is not supported. */
final case class InvalidCombinationError(override val message: String) extends ExpandError
/**
  * Error when a value has not been supplied for the template variable.
  * {{{
  * >>> uritemplate"http://{missingval}.com{/present}".expand("present" -> "foo").toEither
  * Left(List(MissingValueError(missingval)))
  * }}}
  */
final case class MissingValueError(varname: String) extends ExpandError {
  override def message: String = s"$varname wasn't supplied as a variable value"
}
