package uritemplate4s

/** A value can be substituted in a template. */
sealed trait Value
/** A single string value for template substitution. */
final case class StringValue(value: String) extends Value
/** A list of values for template substitution. */
final case class ListValue(value: Seq[String]) extends Value
/** Name value pairs for template substitution. */
final case class AssociativeArray(value: Seq[(String, String)]) extends Value

/** Type class that provides a conversion from A to [[Value]]. */
trait ToValue[A] {
  def apply(a: A): Value
}

object ToValue {

  def apply[A](implicit instance: ToValue[A]): ToValue[A] = instance

  implicit lazy val stringToValue: ToStringValue[String] = (s: String) => s

  implicit def seqToValue[E: ToStringValue, S <: Seq[E]]: ToValue[S] =
    (seq: S) => ListValue(seq.map(ToStringValue[E].asString))

  implicit def seqTuplesToValue[V: ToStringValue, T <: Seq[(String, V)]]: ToValue[T] =
    (tuples: T) => AssociativeArray(tuples.map {
      case (k, v) => k -> ToStringValue[V].asString(v)
    })
}

/**
  * Type class that provides a conversion from A to [[String]].
  * This is used to define a specific type class for strings,
  * to be used for the elements of [[ListValue]] and the values
  * of [[AssociativeArray]].
  */
trait ToStringValue[A] extends ToValue[A] {
  override def apply(a: A): Value = StringValue(asString(a))
  def asString(a: A): String
}

object ToStringValue {

  def apply[A](implicit instance: ToStringValue[A]): ToStringValue[A] = instance
}
