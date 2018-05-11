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

  implicit val stringToValue: ToValue[String] = (s: String) => StringValue(s)
  implicit def seqToValue[S <: Seq[String]]: ToValue[S] = (seq: S) => ListValue(seq)
  implicit def seqTuplesToValue[T <: Seq[(String, String)]]: ToValue[T] =
    (tuples: T) => AssociativeArray(tuples)
}

