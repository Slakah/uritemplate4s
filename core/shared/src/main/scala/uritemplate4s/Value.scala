package uritemplate4s

import scala.annotation.implicitNotFound

/** A value can be substituted in a template. */
sealed trait Value

/** A single string value for template substitution. */
final case class StringValue(value: String) extends Value

/** A list of values for template substitution. */
final case class ListValue(value: Seq[String]) extends Value

/** Name value pairs for template substitution. */
final case class AssociativeArray(value: Seq[(String, String)]) extends Value

/** Type class that provides a conversion from A to [[Value]]. */
@implicitNotFound(
  "No Argument instance found for ${A}. For more info, see: https://slakah.github.io/uritemplate4s/tovalue.html"
)
trait ToValue[A] {
  def apply(a: A): Value
}

object ToValue {

  def apply[A](implicit instance: ToValue[A]): ToValue[A] = instance

  /**
    * {{{
    * >>> ToValue[String].apply("woo")
    * StringValue(woo)
    *
    * >>> ToValue[Int].apply(42)
    * StringValue(42)
    *
    * >>> ToValue[Long].apply(42L)
    * StringValue(42)
    * }}}
    */
  implicit val stringToStringValue: ToStringValue[String] = (s: String) => s
  implicit val intToStringValue: ToStringValue[Int] = (i: Int) => String.valueOf(i)
  implicit val longToStringValue: ToStringValue[Long] = (l: Long) => String.valueOf(l)

  /**
    * {{{
    * >>> object WeekDays extends Enumeration { val Mon,Tue,Wed,Thu,Fri = Value }
    * >>> ToValue[WeekDays.Value].apply(WeekDays.Tue)
    * StringValue(Tue)
    * }}}
    */
  implicit def enumToStringValue[E <: Enumeration]: ToStringValue[E#Value] = (e: E#Value) => e.toString

  /**
    * {{{
    * >>> ToValue[Seq[String]].apply(Seq("red", "green", "blue"))
    * ListValue(List(red, green, blue))
    *
    * >>> ToValue[List[String]].apply(List("red", "green", "blue"))
    * ListValue(List(red, green, blue))
    *
    * >>> ToValue[Vector[String]].apply(Vector("red", "green", "blue"))
    * ListValue(Vector(red, green, blue))
    *
    * >>> ToValue[Vector[Int]].apply(Vector(1, 2, 3))
    * ListValue(Vector(1, 2, 3))
    * }}}
    */
  implicit def seqToValue[A](implicit toValueA: ToStringValue[A]): ToValue[Seq[A]] =
    (seq: Seq[A]) => ListValue(seq.map(toValueA.asString))

  implicit def listToValue[A](implicit toValueA: ToStringValue[A]): ToValue[List[A]] =
    (list: List[A]) => ListValue(list.map(toValueA.asString))

  implicit def vectorToValue[A](implicit toValueA: ToStringValue[A]): ToValue[Vector[A]] =
    (seq: Vector[A]) => ListValue(seq.map(toValueA.asString))

  /**
    * {{{
    * >>> ToValue[Map[String, Int]].apply(Map("one" -> 1, "two" -> 2, "three" -> 3))
    * AssociativeArray(List((one,1), (two,2), (three,3)))
    * }}}
    */
  implicit def mapToValue[V](implicit toValueV: ToStringValue[V]): ToValue[Map[String, V]] =
    (m: Map[String, V]) =>
      AssociativeArray(m.map {
        case (k, v) => k -> toValueV.asString(v)
      }.toList)

  /**
    * {{{
    * >>> ToValue[Seq[(String, Int)]].apply(Seq("one" -> 1, "two" -> 2, "three" -> 3))
    * AssociativeArray(List((one,1), (two,2), (three,3)))
    *
    * >>> ToValue[List[(String, Int)]].apply(List("one" -> 1, "two" -> 2, "three" -> 3))
    * AssociativeArray(List((one,1), (two,2), (three,3)))
    *
    * >>> ToValue[Vector[(String, Int)]].apply(Vector("one" -> 1, "two" -> 2, "three" -> 3))
    * AssociativeArray(Vector((one,1), (two,2), (three,3)))
    * }}}
    */
  implicit def seqTuplesToValue[A](implicit toValueA: ToStringValue[A]): ToValue[Seq[(String, A)]] =
    (seq: Seq[(String, A)]) => AssociativeArray(seq.map { case (k, v) => k -> toValueA.asString(v) })

  implicit def listTuplesToValue[A](implicit toValueA: ToStringValue[A]): ToValue[List[(String, A)]] =
    (list: List[(String, A)]) => AssociativeArray(list.map { case (k, v) => k -> toValueA.asString(v) })

  implicit def vectorTuplesToValue[A](implicit toValueA: ToStringValue[A]): ToValue[Vector[(String, A)]] =
    (vector: Vector[(String, A)]) => AssociativeArray(vector.map { case (k, v) => k -> toValueA.asString(v) })
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
