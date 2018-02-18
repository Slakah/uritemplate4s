package uritemplate

sealed trait Error {
  def message: String
}
object Error {
  /** The supplied template could not be parsed correctly. **/
  final case class MalformedUriTemplate(override val message: String) extends Error
  /** Prefix modifer used with a non string value. **/
  final case class InvalidCombination(override val message: String) extends Error
}

