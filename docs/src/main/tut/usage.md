---
layout: page
title:  "Usage"
section: "usage"
position: 1
---

# Usage

## Parsing

Parsing an URI Template is achieved as follows:

```tut:book
import uritemplate4s._

val rawTemplate = "http://example.com/search{?q,lang}"
val parseResult = UriTemplate.parse(rawTemplate)
```

Because parsing a template can fail, the result is an `Either` of type `Either[ParseError, UriTemplate]`.

When parsing fails the result is a `Left` containing the error with details of the cause.

```tut:book
UriTemplate.parse("http://example.com/search{q")
```

To extract the parsed URI Template from the `Either`, pattern matching can be used:

```tut:book
val template: UriTemplate = UriTemplate.parse(rawTemplate) match {
  case Left(error) => throw new Exception(s"Unable to parse $rawTemplate, due to:\n${error.message}")
  case Right(parsedTemplate) => parsedTemplate
}
```

## Template Expansion

A template can be expanded by supplying tuples representing the name/value pairs to be used in expansion.

```tut:book
template.expand("q" -> "After the Quake", "lang" -> "en")
```

In the previous example it should be noted that the resultant URI is wrapped in a `ExpandResult.Success`.
There are some possible soft failures which can occur during template expansion, meaning an expansion
result could be either a `ExpandResult.Success` or a `ExpandResult.PartialSuccess`.

To extract the result from either case, the `.value` field can be used.

```tut:book
val uri = template.expand("q" -> "After the Quake", "lang" -> "en").value
```

### List Expansion

List expansion is supported as defined in [RFC 6570 Level 4](https://tools.ietf.org/html/rfc6570#page-8).

```tut:book
val listTemplate = UriTemplate.parse("/search{?list}").right.get
val seq = Seq("red", "green", "blue")
listTemplate.expand("list" -> seq).value
// List and Vectors are also supported
listTemplate.expand("list" -> seq.toList).value
listTemplate.expand("list" -> seq.toVector).value
```

### Associative Array Expansion

Associative array expansion is supported as defined in [RFC 6570 Level 4](https://tools.ietf.org/html/rfc6570#page-8).

```tut:book
val assocTemplate = UriTemplate.parse("/search{?address*}").right.get
val addressMap = Map("city" -> "Manchester", "country" -> "England", "postcode" -> "M2 5DB")
assocTemplate.expand("address" -> addressMap).value
```
