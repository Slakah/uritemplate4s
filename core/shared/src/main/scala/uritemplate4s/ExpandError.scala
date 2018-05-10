package uritemplate4s

sealed trait ExpandError {
  def message: String
}

final case class InvalidCombinationError(override val message: String) extends ExpandError

