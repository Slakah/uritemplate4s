package uritemplate4s

import scala.reflect.macros.whitebox

final class UriTemplateLiteralMacros(val c: whitebox.Context) {
  import c.universe._
  import ast._

  def uriTemplateStringContext(args: c.Expr[Any]*): c.Expr[UriTemplate] = {
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
          case Left(err: ParseFailure) =>
            c.abort(c.enclosingPosition, s"not a valid URI Template, ${err.message}")
          case Right(template) =>
            uriTemplateLiteral(template)
        }
      case _ => c.abort(c.enclosingPosition, "Invalid use of the URI Template interpolator")
    }
  }

  private def uriTemplateLiteral(template: UriTemplate): c.Expr[UriTemplate] = template match {
    case ComponentsUriTemplate(components) =>
      val componentTrees = components.map {
        case LiteralComponent(Encoded(value)) =>
          q"_root_.uritemplate4s.ast.LiteralComponent(_root_.uritemplate4s.ast.Encoded($value))"
        case LiteralComponent(Unencoded(value)) =>
          q"_root_.uritemplate4s.ast.LiteralComponent(_root_.uritemplate4s.ast.Unencoded($value))"
        case Expression(op, variableList) =>
          val opTree = op match {
            case Simple => q"_root_.uritemplate4s.ast.Simple"
            case Reserved => q"_root_.uritemplate4s.ast.Reserved"
            case Fragment => q"_root_.uritemplate4s.ast.Fragment"
            case NameLabel => q"_root_.uritemplate4s.ast.NameLabel"
            case PathSegment => q"_root_.uritemplate4s.ast.PathSegment"
            case PathParameter => q"_root_.uritemplate4s.ast.PathParameter"
            case Query => q"_root_.uritemplate4s.ast.Query"
            case QueryContinuation => q"_root_.uritemplate4s.ast.QueryContinuation"
          }
          val variableListTrees = variableList.map {
            case Varspec(varname, modifier) =>
              val modifierTree = modifier match {
                case EmptyModifier => q"_root_.uritemplate4s.ast.EmptyModifier"
                case Prefix(maxLength) => q"_root_.uritemplate4s.ast.Prefix($maxLength)"
                case Explode => q"_root_.uritemplate4s.ast.Explode"
              }
              q"_root_.uritemplate4s.ast.Varspec($varname, $modifierTree)"
          }
          q"_root_.uritemplate4s.ast.Expression($opTree, _root_.scala.List(..$variableListTrees))"
      }
      val componentListTree = q"_root_.scala.List(..$componentTrees)"
      c.Expr[UriTemplate](q"""_root_.uritemplate4s.ComponentsUriTemplate($componentListTree): _root_.uritemplate4s.UriTemplate""")
  }
}
