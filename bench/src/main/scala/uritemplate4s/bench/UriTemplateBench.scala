package uritemplate4s.bench

import org.openjdk.jmh.annotations.{Benchmark, Scope, State}
import uritemplate4s._

@State(Scope.Thread)
class UriTemplateBench {

  @Benchmark
  def parseSuccess(): Either[ParseError, UriTemplate] =
    UriTemplate.parse("http://{host}.com/search{?q}{&lang}")

  @Benchmark
  def parseFail(): Either[ParseError, UriTemplate] =
    UriTemplate.parse("http://{host.com/search{?q}{&lang}")

  private val template = uritemplate"http://{host}.com/search{?q}{&lang}"

  @Benchmark
  def expandTemplate(): ExpandResult =
    template.expand(
      "host" -> "search-engine",
      "q" -> "After the Quake",
      "lang" -> "en")
}
