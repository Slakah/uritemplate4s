package uritemplate4s

/** Error when a [[UriTemplate]] is expanded. */
sealed trait ExpandError {
  def message: String
}

final case class InvalidCombinationError(override val message: String) extends ExpandError

