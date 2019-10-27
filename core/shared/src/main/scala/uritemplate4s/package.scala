package object uritemplate4s {

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
  implicit final class UriTemplateStringContext(val sc: StringContext) {
    def uritemplate(args: Any*): UriTemplate = macro UriTemplateLiteralMacros.uriTemplateStringContext
  }
}
