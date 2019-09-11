---
layout: docs
title:  "ToValue"
position: 2
---

# ToValue

[`ToValue`](/uritemplate4s/api/latest/uritemplate4s/ToValue.html) is a type class to convert from
Scala types to a variable value for use in URI Template expansion. There are 3 different types of as defined by [RFC 6570](https://tools.ietf.org/html/rfc6570):
* [`StringValue`](/uritemplate4s/api/latest/uritemplate4s/StringValue.html) - Used for simple string expansion.
* [`ListValue`](/uritemplate4s/api/latest/uritemplate4s/ListValue.html) - List of string values, e.g. expressing URI path segments.
* [`AssociativeArray`](/uritemplate4s/api/latest/uritemplate4s/AssociativeArray.html) - Name value pairs, e.g. defining query params.

## Creating a custom ToValue

A custom `ToValue` can be defined for Scala types without the explicit support provided by uritemplate4s.

An example would be creating a `ToValue` instance for `java.time.Instant`.

```tut:silent
import uritemplate4s._
import java.time.Instant
implicit val instantToStringValue: ToStringValue[Instant] = (instant: Instant) => instant.toString
```
Test the type class is wired in correctly
```tut:book
ToValue[Instant].apply(Instant.now())
uritemplate"http://clock-service.com/clock{?time}".expand("time" -> Instant.now()).value
```
