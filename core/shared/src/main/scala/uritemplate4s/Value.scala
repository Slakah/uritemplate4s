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

  /**
    * {{{
    * >>> import uritemplate4s._
    * >>> ToValue[String].apply("woo")
    * StringValue(woo)
    * }}}
    */
  implicit val stringToStringValue: ToStringValue[String] = (s: String) => s
  implicit val charToStringValue: ToStringValue[Char] = (c: Char) => String.valueOf(c)
  implicit val intToStringValue: ToStringValue[Int] = (i: Int) => String.valueOf(i)
  implicit val longToStringValue: ToStringValue[Long] = (l: Long) => String.valueOf(l)
  implicit val booleanToStringValue: ToStringValue[Boolean] = (b: Boolean) => String.valueOf(b)

  /**
    * {{{
    * >>> import uritemplate4s._
    * >>> object WeekDays extends Enumeration { val Mon,Tue,Wed,Thu,Fri = Value }
    * >>> ToValue[WeekDays.Value].apply(WeekDays.Tue)
    * StringValue(Tue)
    * }}}
    */
  implicit def enumToStringValue[E <: Enumeration]: ToStringValue[E#Value] = (e: E#Value) => e.toString
  object WeekDays extends Enumeration { val Mon,Tue,Wed,Thu,Fri = Value }

  /**
    * {{{
    * >>> import uritemplate4s._
    * >>> ToValue[Vector[String]].apply(Vector("red", "green", "blue"))
    * ListValue(Vector(red, green, blue))
    *
    * >>> ToValue[Vector[Int]].apply(Vector(1, 2, 3))
    * ListValue(Vector(1, 2, 3))
    * }}}
    */
  implicit def seqToValue[A, S[A] <: Seq[A]](implicit toValueA: ToStringValue[A]): ToValue[S[A]] =
    (seq: S[A]) => ListValue(seq.map(toValueA.asString))

  /**
    * {{{
    * >>> import uritemplate4s._
    * >>> ToValue[Map[String, Int]].apply(Map("one" -> 1, "two" -> 2, "three" -> 3))
    * AssociativeArray(List((one,1), (two,2), (three,3)))
    * }}}
    */
  implicit def mapLikeToValue[V, M[V] <: Map[String, V]](implicit toValueV: ToStringValue[V]): ToValue[M[V]] =
    (m: M[V]) => AssociativeArray(m.map {
      case (k, v) => k -> toValueV.asString(v)
    }.toList)
}

/**
  * Type class that provides a conversion from A to String.
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
