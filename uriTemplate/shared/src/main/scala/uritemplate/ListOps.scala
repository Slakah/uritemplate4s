package uritemplate

import scala.annotation.tailrec

private[uritemplate] trait ListSyntax {

  implicit def uriTemplateListSyntax[A](l: List[A]): ListOps[A] = new ListOps(l)
}

private[uritemplate] object ListSyntax extends ListSyntax

private[uritemplate] class ListOps[A](private val l: List[A]) extends AnyVal {

  def intersperse(a: A): List[A] = {
    @tailrec
    def intersperse0(acc: List[A], rest: List[A]): List[A] = rest match {
      case Nil => acc
      case x :: Nil => x :: acc
      case x :: xs => intersperse0(a :: x :: acc, xs)
    }
    intersperse0(Nil, l).reverse
  }
}
