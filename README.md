# uritemplate4s
[![CircleCI](https://circleci.com/gh/Slakah/uritemplate4s/tree/main.svg?style=svg)](https://circleci.com/gh/Slakah/uritemplate4s/tree/main)
[![Join the chat at https://gitter.im/slakah/uritemplate4s](https://badges.gitter.im/slakah/uritemplate4s.svg)](https://gitter.im/slakah/uritemplate4s?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Latest version](https://index.scala-lang.org/slakah/uritemplate4s/uritemplate4s/latest.svg?color=orange)](https://index.scala-lang.org/slakah/uritemplate4s/uritemplate4s)

Scala implementation of URI Template ([RFC 6570](https://tools.ietf.org/html/rfc6570)).

## Getting Started

uritemplate4s is currently available for Scala 2.13, 2.12 and [Scala.js](http://www.scala-js.org/).

Add the following to your `build.sbt`:

```scala
libraryDependencies += "com.gubbns" %% "uritemplate4s" % "0.5.7"
```

## Usage

```scala
import uritemplate4s._

val template = uritemplate"https://{host}/search{?q}{&params*}"
template.expand(
    "host" -> "search-engine.com",
    "q" -> "Esio Trot",
    "params" -> Map("lang" -> "en", "type" -> "book")
  ).value
// res0: String = https://search-engine.com/search?q=Esio%20Trot&lang=en&type=book
```

Further documentation is available [here](https://slakah.github.io/uritemplate4s/).
