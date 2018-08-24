# uritemplate4s [![CircleCI](https://circleci.com/gh/Slakah/uritemplate4s/tree/master.svg?style=svg)](https://circleci.com/gh/Slakah/uritemplate4s/tree/master)

Scala implementation of URI Template ([RFC 6570](https://tools.ietf.org/html/rfc6570)).

## Getting Started

Add the following to your `build.sbt`:

```scala
libraryDependencies += "com.gubbns" %% "uritemplate4s" % "0.1.0-SNAPSHOT"
```

## Usage

```tut:silent
import uritemplate4s._

val template = uritemplate"https://{host}/search{?q}{&params*}"
```
```tut:book
template.expand(
    "host" -> "search-engine.com",
    "q" -> "Esio Trot",
    "params" -> Map("lang" -> "en", "type" -> "book")
  ).value
```
