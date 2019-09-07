import microsites._
import sbtcrossproject.{crossProject, CrossType}

lazy val commonSettings = Seq(
  organization := "com.gubbns",
  homepage := Some(url(s"https://slakah.github.io/${name.value}/")),
  licenses += "MIT" -> url("http://opensource.org/licenses/MIT"),
  scmInfo := Some(ScmInfo(
    url(s"https://github.com/Slakah/${name.value}"),
    s"scm:git@github.com:Slakah/${name.value}.git"
  )),
  // https://scalacenter.github.io/scalafix/docs/users/installation.html
  addCompilerPlugin(scalafixSemanticdb),
  scalacOptions ++= scalacOpts :+ "-Yrangepos"
)

lazy val publishSettings = Seq(
  autoAPIMappings := true,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  useGpg := false,
  pgpPassphrase ~= (_.orElse(sys.env.get("PGP_PASSPHRASE").map(_.toCharArray))),
  pgpPublicRing := file(s"./pubring.asc"),
  pgpSecretRing := file(s"./secring.asc"),
  apiURL := Some(url("https://slakah.github.io/uritemplate4s/api/latest/uritemplate4s/")),
  pomExtra := {
    <developers>
      <developer>
        <id>slakah</id>
        <name>James Collier</name>
        <url>https://github.com/Slakah</url>
      </developer>
    </developers>
  }
)

lazy val betterMonadicForVersion = "0.3.1"
lazy val catsVersion = "2.0.0-RC2"
lazy val circeVersion = "0.12.0-RC4"
lazy val contextualVersion = "1.2.1"
lazy val fastparseVersion = "2.1.3"
lazy val handyUriTemplatesVersion = "2.1.8"
lazy val monixVersion = "3.0.0-RC5"
lazy val scalafixNoinferVersion = "0.1.0-M1"
lazy val scalajsDomVersion = "0.9.7"
lazy val utestVersion = "0.7.1"

ThisBuild / scalafixDependencies +=
  "com.eed3si9n.fix" %% "scalafix-noinfer" % scalafixNoinferVersion

ThisBuild / libraryDependencies +=
  compilerPlugin("com.olegpy" %% "better-monadic-for" % betterMonadicForVersion)

ThisBuild / releaseEarlyWith := SonatypePublisher
ThisBuild / releaseEarlyEnableLocalReleases := true
ThisBuild / organization := "com.gubbns"

publishArtifact := false

addCommandAlias("validate", Seq(
  "scalafixEnable",
  "scalafix --check",
  "test:scalafix --check",
  "test:compile",
  "+test",
  "tut").mkString(";", ";", "")
)

lazy val bench = project
  .enablePlugins(JmhPlugin)
  .dependsOn(coreJVM)
  .settings(moduleName := "uritemplate4s-bench")
  .settings(
    noPublishSettings,
    libraryDependencies ++= Seq(
      "com.damnhandy" % "handy-uri-templates" % handyUriTemplatesVersion
    )
  )

lazy val docs = project
  .enablePlugins(MicrositesPlugin, SiteScaladocPlugin, GhpagesPlugin, SiteScaladocPlugin, ScalaJSPlugin)
  .settings(moduleName := "uritemplate4s-docs")
  .settings(
    // TODO: use .dependsOn when scalajs deps support 2.13
    scalaVersion := "2.12.9",
    libraryDependencies += "com.gubbns" %%% "uritemplate4s" % "0.4.0",
    commonSettings,
    noPublishSettings,
    docsSettings,
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % catsVersion,
      "org.typelevel" %%% "cats-kernel" % catsVersion,
      "org.typelevel" %%% "cats-macros" % catsVersion,
      "io.circe" %%% "circe-core" % circeVersion,
      "io.circe" %%% "circe-generic" % circeVersion,
      "io.circe" %%% "circe-parser" % circeVersion,
      "io.monix" %%% "monix-execution" % monixVersion,
      "io.monix" %%% "monix-reactive" % monixVersion,
      "org.scala-js" %%% "scalajs-dom" % scalajsDomVersion
    )
  )

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .in(file("core"))
  .jsSettings(
    // currently sbt-doctest doesn't work in JS builds
    // https://github.com/tkawachi/sbt-doctest/issues/52
    doctestGenTests := Seq.empty
  )
  .settings(
    scalaVersion := "2.13.0",
    crossScalaVersions := List(scalaVersion.value, "2.12.9"),
    commonSettings,
    publishSettings,
    name := "uritemplate4s",
    testFrameworks += new TestFramework("utest.runner.Framework"),
    sourceGenerators in Compile += (sourceManaged in Compile).map(Boilerplate.gen).taskValue,
    doctestTestFramework := DoctestTestFramework.MicroTest,
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
      "com.propensive" %%% "contextual" % contextualVersion,
      "com.lihaoyi" %%% "fastparse" % fastparseVersion
    ) ++ Seq(
      "org.typelevel" %%% "cats-core" % catsVersion,
      "org.typelevel" %%% "cats-kernel" % catsVersion,
      "org.typelevel" %%% "cats-macros" % catsVersion,
      "com.lihaoyi" %%% "utest" % utestVersion,
      "io.circe" %%% "circe-core" % circeVersion,
      "io.circe" %%% "circe-generic" % circeVersion,
      "io.circe" %%% "circe-parser" % circeVersion
    ).map(_ % "test")
  )

lazy val coreJS = core.js
lazy val coreJVM = core.jvm

lazy val scalacOpts = Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-explaintypes", // Explain type errors in more detail.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
  "-language:experimental.macros", // Allow macro definition (besides implementation and application)
  "-language:higherKinds", // Allow higher-kinded types
  "-language:implicitConversions", // Allow definition of implicit functions called views
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
  "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
  "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
  "-Xlint:option-implicit", // Option.apply used implicit view.
  "-Xlint:package-object-classes", // Class or object defined in package object.
  "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
  "-Ywarn-numeric-widen", // Warn when numerics are widened.
  "-Ywarn-unused:-patvars,_", // Warn if something is unused.
  "-Ywarn-value-discard", // Warn when non-Unit expression results are unused.
  "-Ybackend-parallelism", "8", // Enable paralellisation — change to desired number!
  "-Ycache-plugin-class-loader:last-modified", // Enables caching of classloaders for compiler plugins
  "-Ycache-macro-class-loader:last-modified", // and macro definitions. This can lead to performance improvements.
  "-Ywarn-macros:both"
)

lazy val micrositeFullOptJS = taskKey[Unit]("Full build js, and adds it to a managed js dir")

lazy val docsSettings = Seq(
  micrositeName := "uritemplate4s",
  micrositeDescription := "URI template implementation for Scala",
  micrositeBaseUrl := "/uritemplate4s",
  micrositeDocumentationUrl := "/uritemplate4s/api/latest/uritemplate4s/",
  micrositeGithubOwner := "Slakah",
  micrositeGithubRepo := "uritemplate4s",
  micrositeExtraMdFiles := Map(
    file("README.md") -> ExtraMdFileConfig(
      "index.md",
      "home",
      Map("title" -> "Home", "section" -> "home", "position" -> "0")
    ),
    file("LICENSE") -> ExtraMdFileConfig(
      "license.md",
      "page",
      Map("title" -> "License",   "section" -> "License",   "position" -> "101")
    )
  ),
  scalacOptions in Tut += "-Ywarn-unused:-imports",
  micrositePushSiteWith := GHPagesPlugin,
  micrositeGithubToken := sys.env.get("GITHUB_TOKEN"),
  micrositeJsDirectory := (managedResourceDirectories in Compile).value.head / "microsite" / "js",
  git.remoteRepo := "git@github.com:slakah/uritemplate4s.git",
  (includeFilter in makeSite) := (includeFilter in makeSite).value || "*.js.map",
  micrositeFullOptJS := {
    val jsFile = (fullOptJS in Compile).value.data
    val jsMapFileOpt = (fullOptJS in Compile).value.get(scalaJSSourceMap)
    val managedJsDir = (resourceDirectory in Compile).value / "microsite" / "js"
    val targetDir = micrositeJsDirectory.value
    IO.copyFile(jsFile, targetDir / jsFile.name)
    jsMapFileOpt.foreach { jsMapFile =>
      IO.copyFile(jsMapFile, targetDir / jsMapFile.name)
    }
    IO.copyDirectory(managedJsDir, targetDir)
  },
  (mainClass in Compile) := Some("uritemplate4s.demo.Playground"),
  scalaJSUseMainModuleInitializer := true,
  makeMicrosite := makeMicrosite.dependsOn(micrositeFullOptJS).value
) ++ SiteScaladocPlugin.scaladocSettings(SiteScaladoc, mappings in (Compile, packageDoc) in coreJVM, "api/latest")

lazy val noPublishSettings = Seq(
  skip in publish := true,
  PgpKeys.publishSigned := {},
  PgpKeys.publishLocalSigned := {},
  publishArtifact := false
)
