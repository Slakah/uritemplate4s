package uritemplate4s.bench

import scala.jdk.CollectionConverters._
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
class Uritemplate4sBench {
  import uritemplate4s._

  @Benchmark
  def parseSuccess(): Either[ParseFailure, UriTemplate] =
    UriTemplate.parse("http://{host}.com/search{?q}{&lang}")

  @Benchmark
  def parseFail(): Either[ParseFailure, UriTemplate] =
    UriTemplate.parse("http://{host.com/search{?q}{&lang}")

  private val template: UriTemplate = uritemplate"http://{host}.com/search{?q}{&lang}"

  @Benchmark
  def expandTemplate(): ExpandResult =
    template.expand("host" -> "search-engine", "q" -> "After the Quake", "lang" -> "en")
}

@State(Scope.Thread)
class HandyUriTemplatesBench {
  import com.damnhandy.uri.template._

  @Benchmark
  def parseSuccess(): UriTemplate =
    UriTemplate.fromTemplate("http://{host}.com/search{?q}{&lang}")

  @Benchmark
  def parseFail(): Unit =
    try {
      val _ = UriTemplate.fromTemplate("http://{host.com/search{?q}{&lang}")
    } catch {
      case _: Throwable => ()
    }

  private val template = UriTemplate.fromTemplate("http://{host}.com/search{?q}{&lang}")

  private val vars = Map[String, Object](
    "host" -> "search-engine",
    "q" -> "After the Quake",
    "lang" -> "en"
  ).asJava

  @Benchmark
  def expandTemplate(): String = template.expand(vars)
}
