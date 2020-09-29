---
layout: docs
title:  "Usage"
position: 1
---

# Usage

## Parsing

Parsing an URI Template at compile time can be achieved as follows:

```scala mdoc:silent
import uritemplate4s._

val template = uritemplate"http://example.com/search{?q,lang}"
```

When an invalid template is supplied, the error will be shown at compile time.

```scala mdoc:fail
uritemplate"http://example.com/search{q"
```

URI Templates are usually dynamically provided, and so will need to be parsed at runtime like so:

```scala mdoc:to-string
val rawTemplate = "http://example.com/search{?q,lang}"
val parseResult = UriTemplate.parse(rawTemplate)
```

Because parsing a template can fail, the result is an `Either` of type `Either[ParseFailure, UriTemplate]`.

When parsing fails the result is a `Left` containing the error with details of the cause.

```scala mdoc
UriTemplate.parse("http://example.com/search{q")
```

To extract the parsed URI Template from the `Either`, pattern matching can be used:

```scala mdoc:silent:nest
val template: UriTemplate = UriTemplate.parse(rawTemplate) match {
  case Left(error) => throw error
  case Right(parsedTemplate) => parsedTemplate
}
```

Or more simply:

```scala mdoc:silent:nest
val template: UriTemplate = UriTemplate.parse(rawTemplate).toTry.get
```

## Template Expansion

A template can be expanded by supplying tuples representing the name/value pairs to be used in expansion.

```scala mdoc
template.expand("q" -> "After the Quake", "lang" -> "en")
```

In the previous example it should be noted that the resultant URI is wrapped in a `ExpandResult.Success`.
There are some possible soft failures which can occur during template expansion, meaning an expansion
result could be either a `ExpandResult.Success` or a `ExpandResult.PartialSuccess`.

To extract the result from either case, the `.value` field can be used.

```scala mdoc
val uri = template.expand("q" -> "After the Quake", "lang" -> "en").value
```

For examples and details of the features supported in URI Template expansion, refer to [RFC 6570 Section 3](https://tools.ietf.org/html/rfc6570#section-3)
as well as the tests included in this project.

### List Expansion

List expansion is supported as defined in [RFC 6570 Level 4](https://tools.ietf.org/html/rfc6570#page-8).

```scala mdoc:silent
val listTemplate = UriTemplate.parse("/search{?list}").toTry.get
val seq = Seq("red", "green", "blue")
```

```scala mdoc
listTemplate.expand("list" -> seq).value
```

List and Vectors are also supported.

```scala mdoc:silent
listTemplate.expand("list" -> seq.toList).value
listTemplate.expand("list" -> seq.toVector).value
```

### Associative Array Expansion

Associative array expansion is supported as defined in [RFC 6570 Level 4](https://tools.ietf.org/html/rfc6570#page-8).

```scala mdoc:silent
val assocTemplate = UriTemplate.parse("/search{?address*}").toTry.get
val addressMap = Map("city" -> "Manchester", "country" -> "England", "postcode" -> "M2 5DB")
```

```scala mdoc
assocTemplate.expand("address" -> addressMap).value
```
