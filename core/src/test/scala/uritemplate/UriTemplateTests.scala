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
      "Simple string expansion (Sec 3.2.2)" - test(vars)(
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
      "Reserved string expansion (Sec 3.2.3)" - test(vars)(
        "{+var}" -> "value",
        "{+hello}" -> "Hello%20World!",
        "{+path}/here" ->"/foo/bar/here",
        "here?ref={+path}" -> "here?ref=/foo/bar"
      )
      "Fragment expansion, crosshatch-prefixed (Sec 3.2.4)" - test(vars)(
        "X{#var}" -> "X#value",
        "X{#hello}" -> "X#Hello%20World!"
      )
    }

    /**
      * .-----------------------------------------------------------------.
      * | Level 3 examples, with variables having values of               |
      * |                                                                 |
      * |             var   := "value"                                    |
      * |             hello := "Hello World!"                             |
      * |             empty := ""                                         |
      * |             path  := "/foo/bar"                                 |
      * |             x     := "1024"                                     |
      * |             y     := "768"                                      |
      * |                                                                 |
      * |-----------------------------------------------------------------|
      * | Op       Expression            Expansion                        |
      * |-----------------------------------------------------------------|
      * |     | String expansion with multiple variables      (Sec 3.2.2) |
      * |     |                                                           |
      * |     |    map?{x,y}             map?1024,768                     |
      * |     |    {x,hello,y}           1024,Hello%20World%21,768        |
      * |     |                                                           |
      * |-----+-----------------------------------------------------------|
      * |  +  | Reserved expansion with multiple variables    (Sec 3.2.3) |
      * |     |                                                           |
      * |     |    {+x,hello,y}          1024,Hello%20World!,768          |
      * |     |    {+path,x}/here        /foo/bar,1024/here               |
      * |     |                                                           |
      * |-----+-----------------------------------------------------------|
      * |  #  | Fragment expansion with multiple variables    (Sec 3.2.4) |
      * |     |                                                           |
      * |     |    {#x,hello,y}          #1024,Hello%20World!,768         |
      * |     |    {#path,x}/here        #/foo/bar,1024/here              |
      * |     |                                                           |
      * |-----+-----------------------------------------------------------|
      * |  .  | Label expansion, dot-prefixed                 (Sec 3.2.5) |
      * |     |                                                           |
      * |     |    X{.var}               X.value                          |
      * |     |    X{.x,y}               X.1024.768                       |
      * |     |                                                           |
      * |-----+-----------------------------------------------------------|
      * |  /  | Path segments, slash-prefixed                 (Sec 3.2.6) |
      * |     |                                                           |
      * |     |    {/var}                /value                           |
      * |     |    {/var,x}/here         /value/1024/here                 |
      * |     |                                                           |
      * |-----+-----------------------------------------------------------|
      * |  ;  | Path-style parameters, semicolon-prefixed     (Sec 3.2.7) |
      * |     |                                                           |
      * |     |    {;x,y}                ;x=1024;y=768                    |
      * |     |    {;x,y,empty}          ;x=1024;y=768;empty              |
      * |     |                                                           |
      * |-----+-----------------------------------------------------------|
      * |  ?  | Form-style query, ampersand-separated         (Sec 3.2.8) |
      * |     |                                                           |
      * |     |    {?x,y}                ?x=1024&y=768                    |
      * |     |    {?x,y,empty}          ?x=1024&y=768&empty=             |
      * |     |                                                           |
      * |-----+-----------------------------------------------------------|
      * |  &  | Form-style query continuation                 (Sec 3.2.9) |
      * |     |                                                           |
      * |     |    ?fixed=yes{&x}        ?fixed=yes&x=1024                |
      * |     |    {&x,y,empty}          &x=1024&y=768&empty=             |
      * |     |                                                           |
      * '-----------------------------------------------------------------'
      */
    'level3 - {
      val vars = Map(
        "var" -> "value",
        "hello" -> "Hello World!",
        "empty" -> "",
        "path" -> "/foo/bar",
        "x" -> "1024",
        "y" -> "768"
      )
      "String expansion with multiple variables (Sec 3.2.2)" - test(vars)(
        "map?{x,y}" -> "map?1024,768",
        "{x,hello,y}" -> "1024,Hello%20World%21,768"
      )
      "Reserved expansion with multiple variables (Sec 3.2.3)" - test(vars)(
        "{+x,hello,y}" -> "1024,Hello%20World!,768",
        "{+path,x}/here" -> "/foo/bar,1024/here"
      )
      "Fragment expansion with multiple variables (Sec 3.2.4)" - test(vars)(
        "{#x,hello,y}" -> "#1024,Hello%20World!,768",
        "{#path,x}/here" -> "#/foo/bar,1024/here"
      )
      "Label expansion, dot-prefixed (Sec 3.2.5)" - test(vars)(
        "X{.var}" -> "X.value",
        "X{.x,y}" -> "X.1024.768"
      )
      "Path segments, slash-prefixed (Sec 3.2.6)" - test(vars)(
        "{/var}" -> "/value",
        "{/var,x}/here" -> "/value/1024/here"
      )
      "Path-style parameters, semicolon-prefixed (Sec 3.2.7)" - test(vars)(
        "{;x,y}" -> ";x=1024;y=768",
        "{;x,y,empty}" -> ";x=1024;y=768;empty"
      )
      "Form-style query, ampersand-separated (Sec 3.2.8)" - test(vars)(
        "{?x,y}" -> "?x=1024&y=768",
        "{?x,y,empty}" -> "?x=1024&y=768&empty="
      )
      "Form-style query continuation (Sec 3.2.9)" - test(vars)(
        "?fixed=yes{&x}" -> "?fixed=yes&x=1024",
        "{&x,y,empty}" -> "&x=1024&y=768&empty="
      )
    }
  }
}
