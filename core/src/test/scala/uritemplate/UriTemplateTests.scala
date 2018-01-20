package uritemplate

import utest._

object UriTemplateTests extends TestSuite {

  val tests = Tests {
    /**
      * .-----------------------------------------------------------------.
      * | Level 1 examples, with variables having values of               |
      * |                                                                 |
      * |             var   := "value"                                    |
      * |             hello := "Hello World!"                             |
      * |                                                                 |
      * |-----------------------------------------------------------------|
      * | Op       Expression            Expansion                        |
      * |-----------------------------------------------------------------|
      * |     | Simple string expansion                       (Sec 3.2.2) |
      * |     |                                                           |
      * |     |    {var}                 value                            |
      * |     |    {hello}               Hello%20World%21                 |
      * '-----------------------------------------------------------------'
      */
    "level 1" - {
      val vars = Map("var" -> "value", "hello" -> "Hello World!").toList
      assert(UriTemplate("{var}").expand(vars: _*) == "value")
      assert(UriTemplate("{hello}").expand(vars: _*) == "Hello%20World%21")
    }
  }
}
