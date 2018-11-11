package uritemplate4s

import utest._

object ContextualTests extends TestSuite {

  val tests = Tests {
    "parse a URI Template at compile time" - {
      val template = uritemplate"http://{woo}.com"
      assert(Right(template) == UriTemplate.parse("http://{woo}.com"))
    }
    "error when unable to parse a URI Template at compile time" - {
      compileError("""uritemplate"http://{woo.com" """)
        .check("", """not a valid URI Template, Position 1:16, found """"")
    }
  }
}
