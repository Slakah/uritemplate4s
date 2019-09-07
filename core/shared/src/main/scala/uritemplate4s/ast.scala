package uritemplate4s

object ast {
  /** Represents a parsed URI Template component. */
  sealed trait Component

  /** URI Template literal [[https://tools.ietf.org/html/rfc6570#section-2.1]]. */
  final case class LiteralComponent(literal: Literal) extends Component
  sealed trait Literal { def value: String }
  /** A [[Literal]] which is encoded. */
  final case class Encoded(override val value: String) extends Literal
  /** A [[Literal]] which is unencoded, and will need to be encoded. */
  final case class Unencoded(override val value: String) extends Literal

  /** Template expression [[https://tools.ietf.org/html/rfc6570#section-2.2]]. */
  final case class Expression(operator: Operator, variableList: List[Varspec]) extends Component

  sealed abstract class Operator(val first: String, val sep: String, val named: Boolean, val ifemp: String, val allow: Allow)
  // https://tools.ietf.org/html/rfc6570#appendix-A
  case object Simple extends Operator("", ",", false, "", Allow.U)
  case object Reserved extends Operator("", ",", false, "", Allow.`U+R`)
  case object Fragment extends Operator("#", ",", false, "", Allow.`U+R`)
  case object NameLabel extends Operator(".", ".", false, "", Allow.U)
  case object PathSegment extends Operator("/", "/", false, "", Allow.U)
  case object PathParameter extends Operator(";", ";", true, "", Allow.U)
  case object Query extends Operator("?", "&", true, "=", Allow.U)
  case object QueryContinuation extends Operator("&", "&", true, "=", Allow.U)

  sealed trait Allow
  object Allow {
    case object U extends Allow
    case object `U+R` extends Allow
  }

  final case class Varspec(varname: String, modifier: ModifierLevel4)

  /** Value modifier [[https://tools.ietf.org/html/rfc6570#section-2.4]]. */
  sealed trait ModifierLevel4
  case object EmptyModifier extends ModifierLevel4
  final case class Prefix(maxLength: Int) extends ModifierLevel4
  case object Explode extends ModifierLevel4
}
