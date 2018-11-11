package uritemplate4s

package object syntax {

  implicit final class ToValueOps[A](private val a: A) extends AnyVal {
    def toValue(implicit toValue: ToValue[A]): Value = toValue(a)
  }
}
