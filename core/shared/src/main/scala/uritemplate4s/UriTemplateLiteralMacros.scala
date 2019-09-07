package uritemplate4s

import scala.reflect.macros.whitebox

object UriTemplateLiteralMacros {

  final def uriTemplateStringContext(c: whitebox.Context)(args: c.Expr[Any]*): c.Expr[UriTemplate] = {
    import c.universe._
    c.prefix.tree match {
      case Apply(_, Apply(_, parts) :: Nil) =>
        val stringParts = parts.map {
          case Literal(Constant(part: String)) => part
          case _ =>
            c.abort(
              c.enclosingPosition,
              "A StringContext part for the URI Template interpolator is not a string"
            )
        }
        val uriTemplateString = stringParts.mkString
        if (args.nonEmpty) {
          c.abort(c.enclosingPosition, "String interpolation not supported by the URI Template interpolator")
        }

        UriTemplate.parse(uriTemplateString) match {
          case Left(MalformedUriTemplateError(_, message)) =>
            c.abort(c.enclosingPosition, s"not a valid URI Template, $message")
          case Right(_) =>
            val errorMessage = "Unexpected failure when parsing URI Template."
            c.Expr[UriTemplate](
              q"_root_.uritemplate4s.UriTemplate.parse($uriTemplateString).getOrElse(throw new _root_.java.lang.Exception($errorMessage))")
        }
      case _ => c.abort(c.enclosingPosition, "Invalid use of the URI Template interpolator")
    }
  }
}
