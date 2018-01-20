package uritemplate

import utest._

object UriTemplateTests extends TestSuite {

  private def test(
                  vars: Map[String, String])(
                  templatetWithExp: (String, String)*
                  ): Unit = {
    val varList = vars.toList
    templatetWithExp.foreach { case (template, exp) =>
      val actual = UriTemplate(template).expand(varList: _*)
      assert(exp == actual)
    }
  }

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
    'level1 - {
      val vars = Map(
        "var" -> "value",
        "hello" -> "Hello World!"
      )
      "Simple string expansion" - test(vars)(
        "{var}" -> "value",
        "{hello}" -> "Hello%20World%21"
      )
    }

    /**
      * .-----------------------------------------------------------------.
      * | Level 2 examples, with variables having values of               |
      * |                                                                 |
      * |             var   := "value"                                    |
      * |             hello := "Hello World!"                             |
      * |             path  := "/foo/bar"                                 |
      * |                                                                 |
      * |-----------------------------------------------------------------|
      * | Op       Expression            Expansion                        |
      * |-----------------------------------------------------------------|
      * |  +  | Reserved string expansion                     (Sec 3.2.3) |
      * |     |                                                           |
      * |     |    {+var}                value                            |
      * |     |    {+hello}              Hello%20World!                   |
      * |     |    {+path}/here          /foo/bar/here                    |
      * |     |    here?ref={+path}      here?ref=/foo/bar                |
      * |-----+-----------------------------------------------------------|
      * |  #  | Fragment expansion, crosshatch-prefixed       (Sec 3.2.4) |
      * |     |                                                           |
      * |     |    X{#var}               X#value                          |
      * |     |    X{#hello}             X#Hello%20World!                 |
      * '-----------------------------------------------------------------'
      */
    'level2 - {
      val vars = Map(
        "var" -> "value",
        "hello" -> "Hello World!",
        "path" -> "/foo/bar"
      )
      "Reserved string expansion" - test(vars)(
        "{+var}" -> "value",
        "{+hello}" -> "Hello%20World!",
        "{+path}/here" ->"/foo/bar/here",
        "here?ref={+path}" -> "here?ref=/foo/bar"
      )
      "Fragment expansion, crosshatch-prefixed" - test(vars)(
        "X{#var}" -> "X#value",
        "X{#hello}" -> "X#Hello%20World!"
      )
    }
  }
}
