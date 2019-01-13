
package object uritemplate4s {

  object UriTemplateVerifier extends contextual.Verifier[UriTemplate] {

    def check(string: String): Either[(Int, String), UriTemplate] = {
      UriTemplate.parse(string).left.map {
        case MalformedUriTemplateError(index, message) => index -> s"not a valid URI Template, $message"
      }
    }
  }

  /**
    * Parse and validate a Uri Template at compile time.
    * {{{
    * >>> Right(uritemplate"http://{name}.com") == UriTemplate.parse("http://{name}.com")
    * true
    *
    * >>> compileError("""uritemplate"http://{invalid" """).msg
    * not a valid URI Template, Position 1:16, found ""
    * }}}
    */
  implicit final class UriTemplateStringContext(private val sc: StringContext) extends AnyVal {
    def uritemplate = contextual.Prefix(UriTemplateVerifier, sc)
  }
}
