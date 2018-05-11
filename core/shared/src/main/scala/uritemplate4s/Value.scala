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

  implicit val stringToStringValue: ToStringValue[String] = (s: String) => s
  implicit val charToStringValue: ToStringValue[Char] = (c: Char) => String.valueOf(c)
  implicit val intToStringValue: ToStringValue[Int] = (i: Int) => String.valueOf(i)
  implicit val longToStringValue: ToStringValue[Long] = (l: Long) => String.valueOf(l)
  implicit val booleanToStringValue: ToStringValue[Boolean] = (b: Boolean) => String.valueOf(b)
  implicit def enumToStringValue[E <: Enumeration]: ToStringValue[E#Value] = (e: E#Value) => e.toString

  implicit def seqToValue[A, S[A] <: Seq[A]](implicit toValueA: ToStringValue[A]): ToValue[S[A]] =
    (seq: S[A]) => ListValue(seq.map(toValueA.asString))

  implicit def mapLikeToValue[V, M[V] <: Map[String, V]](implicit toValueV: ToStringValue[V]): ToValue[M[V]] =
    (m: M[V]) => AssociativeArray(m.map {
      case (k, v) => k -> toValueV.asString(v)
    }.toList)
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
