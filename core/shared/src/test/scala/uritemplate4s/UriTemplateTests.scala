package uritemplate4s

import utest._

import uritemplate4s.syntax._

object UriTemplateTests extends TestSuite {

  private def test(
                  vars: Map[String, Value])(
                  testCases: (String, String)*
                  ): Unit = {
    val varList = vars.toList
    testCases.foreach { case (template, exp) =>
      val actual = UriTemplate.parse(template).flatMap(_.expand(varList: _*).toEither)
      assert(Right(exp) == actual)
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
      val vars = Map[String, Value](
        "var" -> "value".toValue,
        "hello" -> "Hello World!".toValue
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
      val vars = Map[String, Value](
        "var" -> "value".toValue,
        "hello" -> "Hello World!".toValue,
        "path" -> "/foo/bar".toValue
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
      val vars = Map[String, Value](
        "var" -> "value".toValue,
        "hello" -> "Hello World!".toValue,
        "empty" -> "".toValue,
        "path" -> "/foo/bar".toValue,
        "x" -> 1024.toValue,
        "y" -> 768.toValue
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

    /**
      * .-----------------------------------------------------------------.
      * | Level 4 examples, with variables having values of               |
      * |                                                                 |
      * |             var   := "value"                                    |
      * |             hello := "Hello World!"                             |
      * |             path  := "/foo/bar"                                 |
      * |             list  := ("red", "green", "blue")                   |
      * |             keys  := [("semi",";"),("dot","."),("comma",",")]   |
      * |                                                                 |
      * | Op       Expression            Expansion                        |
      * |-----------------------------------------------------------------|
      * |     | String expansion with value modifiers         (Sec 3.2.2) |
      * |     |                                                           |
      * |     |    {var:3}               val                              |
      * |     |    {var:30}              value                            |
      * |     |    {list}                red,green,blue                   |
      * |     |    {list*}               red,green,blue                   |
      * |     |    {keys}                semi,%3B,dot,.,comma,%2C         |
      * |     |    {keys*}               semi=%3B,dot=.,comma=%2C         |
      * |     |                                                           |
      * |-----+-----------------------------------------------------------|
      * |  +  | Reserved expansion with value modifiers       (Sec 3.2.3) |
      * |     |                                                           |
      * |     |    {+path:6}/here        /foo/b/here                      |
      * |     |    {+list}               red,green,blue                   |
      * |     |    {+list*}              red,green,blue                   |
      * |     |    {+keys}               semi,;,dot,.,comma,,             |
      * |     |    {+keys*}              semi=;,dot=.,comma=,             |
      * |     |                                                           |
      * |-----+-----------------------------------------------------------|
      * |  #  | Fragment expansion with value modifiers       (Sec 3.2.4) |
      * |     |                                                           |
      * |     |    {#path:6}/here        #/foo/b/here                     |
      * |     |    {#list}               #red,green,blue                  |
      * |     |    {#list*}              #red,green,blue                  |
      * |     |    {#keys}               #semi,;,dot,.,comma,,            |
      * |     |    {#keys*}              #semi=;,dot=.,comma=,            |
      * |     |                                                           |
      * |-----+-----------------------------------------------------------|
      * |  .  | Label expansion, dot-prefixed                 (Sec 3.2.5) |
      * |     |                                                           |
      * |     |    X{.var:3}             X.val                            |
      * |     |    X{.list}              X.red,green,blue                 |
      * |     |    X{.list*}             X.red.green.blue                 |
      * |     |    X{.keys}              X.semi,%3B,dot,.,comma,%2C       |
      * |     |    X{.keys*}             X.semi=%3B.dot=..comma=%2C       |
      * |     |                                                           |
      * |-----+-----------------------------------------------------------|
      * |  /  | Path segments, slash-prefixed                 (Sec 3.2.6) |
      * |     |                                                           |
      * |     |    {/var:1,var}          /v/value                         |
      * |     |    {/list}               /red,green,blue                  |
      * |     |    {/list*}              /red/green/blue                  |
      * |     |    {/list*,path:4}       /red/green/blue/%2Ffoo           |
      * |     |    {/keys}               /semi,%3B,dot,.,comma,%2C        |
      * |     |    {/keys*}              /semi=%3B/dot=./comma=%2C        |
      * |     |                                                           |
      * |-----+-----------------------------------------------------------|
      * |  ;  | Path-style parameters, semicolon-prefixed     (Sec 3.2.7) |
      * |     |                                                           |
      * |     |    {;hello:5}            ;hello=Hello                     |
      * |     |    {;list}               ;list=red,green,blue             |
      * |     |    {;list*}              ;list=red;list=green;list=blue   |
      * |     |    {;keys}               ;keys=semi,%3B,dot,.,comma,%2C   |
      * |     |    {;keys*}              ;semi=%3B;dot=.;comma=%2C        |
      * |     |                                                           |
      * |-----+-----------------------------------------------------------|
      * |  ?  | Form-style query, ampersand-separated         (Sec 3.2.8) |
      * |     |                                                           |
      * |     |    {?var:3}              ?var=val                         |
      * |     |    {?list}               ?list=red,green,blue             |
      * |     |    {?list*}              ?list=red&list=green&list=blue   |
      * |     |    {?keys}               ?keys=semi,%3B,dot,.,comma,%2C   |
      * |     |    {?keys*}              ?semi=%3B&dot=.&comma=%2C        |
      * |     |                                                           |
      * |-----+-----------------------------------------------------------|
      * |  &  | Form-style query continuation                 (Sec 3.2.9) |
      * |     |                                                           |
      * |     |    {&var:3}              &var=val                         |
      * |     |    {&list}               &list=red,green,blue             |
      * |     |    {&list*}              &list=red&list=green&list=blue   |
      * |     |    {&keys}               &keys=semi,%3B,dot,.,comma,%2C   |
      * |     |    {&keys*}              &semi=%3B&dot=.&comma=%2C        |
      * |     |                                                           |
      * '-----------------------------------------------------------------'
      */
    'level4 - {
      val vars = Map[String, Value](
        "var" -> "value".toValue,
        "hello" -> "Hello World!".toValue,
        "path" -> "/foo/bar".toValue,
        "list" -> List("red", "green", "blue").toValue,
        "keys" -> Map("semi" -> ";", "dot" -> ".", "comma" ->",").toValue
      )
      "String expansion with value modifiers (Sec 3.2.2)" - test(vars)(
        "{var:3}" -> "val",
        "{var:30}" -> "value",
        "{list}" -> "red,green,blue",
        "{list*}" -> "red,green,blue",
        "{keys}" -> "semi,%3B,dot,.,comma,%2C",
        "{keys*}" -> "semi=%3B,dot=.,comma=%2C"
      )

      "Reserved expansion with value modifiers (Sec 3.2.3)" - test(vars)(
        "{+path:6}/here" -> "/foo/b/here",
        "{+list}" -> "red,green,blue",
        "{+list*}" -> "red,green,blue",
        "{+keys}"  -> "semi,;,dot,.,comma,,",
        "{+keys*}" -> "semi=;,dot=.,comma=,"
      )
      "Fragment expansion with value modifiers (Sec 3.2.4)" - test(vars)(
        "{#path:6}/here" -> "#/foo/b/here",
        "{#list}" -> "#red,green,blue",
        "{#list*}" -> "#red,green,blue",
        "{#keys}" -> "#semi,;,dot,.,comma,,",
        "{#keys*}" -> "#semi=;,dot=.,comma=,"
      )
      "Label expansion, dot-prefixed (Sec 3.2.5)" - test(vars)(
        "X{.var:3}" -> "X.val",
        "X{.list}" -> "X.red,green,blue",
        "X{.list*}" -> "X.red.green.blue",
        "X{.keys}" -> "X.semi,%3B,dot,.,comma,%2C",
        "X{.keys*}" -> "X.semi=%3B.dot=..comma=%2C"
      )
      "Path segments, slash-prefixed (Sec 3.2.6)" - test(vars)(
        "{/var:1,var}" -> "/v/value",
        "{/list}" -> "/red,green,blue",
        "{/list*}" -> "/red/green/blue",
        "{/list*,path:4}" -> "/red/green/blue/%2Ffoo",
        "{/keys}" -> "/semi,%3B,dot,.,comma,%2C",
        "{/keys*}" -> "/semi=%3B/dot=./comma=%2C"
      )
      "Path-style parameters, semicolon-prefixed (Sec 3.2.7)" - test(vars)(
        "{;hello:5}" -> ";hello=Hello",
        "{;list}" -> ";list=red,green,blue",
        "{;list*}" -> ";list=red;list=green;list=blue",
        "{;keys}" -> ";keys=semi,%3B,dot,.,comma,%2C",
        "{;keys*}" -> ";semi=%3B;dot=.;comma=%2C"
      )
      "Form-style query, ampersand-separated (Sec 3.2.8) " - test(vars)(
        "{?var:3}" -> "?var=val",
        "{?list}" -> "?list=red,green,blue",
        "{?list*}" -> "?list=red&list=green&list=blue",
        "{?keys}" -> "?keys=semi,%3B,dot,.,comma,%2C",
        "{?keys*}" -> "?semi=%3B&dot=.&comma=%2C"
      )
      "Form-style query continuation (Sec 3.2.9)" - test(vars)(
        "{&var:3}" -> "&var=val",
        "{&list}" -> "&list=red,green,blue",
        "{&list*}" -> "&list=red&list=green&list=blue",
        "{&keys}" -> "&keys=semi,%3B,dot,.,comma,%2C",
        "{&keys*}" -> "&semi=%3B&dot=.&comma=%2C"
      )
    }
  }
}
