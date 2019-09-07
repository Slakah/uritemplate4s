package uritemplate4s

/**
  * A successfully parsed uri template can always be expanded, but there might be some
  * warnings associated.
  */
sealed trait ExpandResult {
  def value: String
  def toEither: Either[ExpandFailure, String]
}

object ExpandResult {

  /** The [[UriTemplate]] was successfully expanded. */
  final case class Success(override val value: String) extends ExpandResult {
    @inline override def toEither: Either[ExpandFailure, String] = Right(value)
  }

  /**
    * The [[UriTemplate]] expansion was partially successful.
    * In most cases, this warning can be ignored.
    */
  final case class PartialSuccess(
    override val value: String,
    error: ExpandFailure
  ) extends ExpandResult {
    @inline override def toEither: Either[ExpandFailure, String] = Left(error)
  }
}
