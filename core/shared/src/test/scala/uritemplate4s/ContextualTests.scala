package uritemplate4s

class ContextualTests extends munit.FunSuite {

  test("parse a URI Template at compile time") {
    val template = uritemplate"http://{woo}.com"
    assert(Right(template) == UriTemplate.parse("http://{woo}.com"))
  }
  test("error when unable to parse a URI Template at compile time") {
    assertEquals(
      compileErrors("""uritemplate"http://{woo.com" """),
      """|error: not a valid URI Template, Position 1:16, found ""
         |uritemplate"http://{woo.com" 
         |^""".stripMargin
    )
  }
}
