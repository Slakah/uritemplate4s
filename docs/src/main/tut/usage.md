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

When the supplied template is unable to be  parsed, the result is a `Left` containing the error with details of the cause.

```tut:book
UriTemplate.parse("http://example.com/search{q")
```

Extract the URI Template from the `Either` can be achieved in a number of ways, the below complete example uses pattern matching:

```tut:book
val template = UriTemplate.parse(rawTemplate) match {
  case Left(error) => throw new Exception(s"Unable to parse $rawTemplate, due to:\n${error.message}")
  case Right(parsedTemplate) => parsedTemplate
}
// Do stuff with template
```
