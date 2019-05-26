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
  scalaVersion := "2.12.8",
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

lazy val betterMonadicForVersion = "0.3.0"
lazy val catsVersion = "1.6.0"
lazy val circeVersion = "0.11.1"
lazy val contextualVersion = "1.1.0"
lazy val fastparseVersion = "2.1.2"
lazy val handyUriTemplatesVersion = "2.1.8"
lazy val monixVersion = "3.0.0-RC1"
lazy val scalafixNoinferVersion = "0.1.0-M1"
lazy val scalajsDomVersion = "0.9.7"
lazy val utestVersion = "0.6.7"

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
  "test",
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
  .dependsOn(coreJS)
  .settings(moduleName := "uritemplate4s-docs")
  .settings(
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
    commonSettings,
    publishSettings,
    name := "uritemplate4s",
    testFrameworks += new TestFramework("utest.runner.Framework"),
    sourceGenerators in Compile += (sourceManaged in Compile).map(Boilerplate.gen).taskValue,
    doctestTestFramework := DoctestTestFramework.MicroTest,
    libraryDependencies ++= Seq(
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
  "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
  "-encoding", "utf-8",                // Specify character encoding used by source files.
  "-explaintypes",                     // Explain type errors in more detail.
  "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
  "-language:experimental.macros",     // Allow macro definition (besides implementation and application)
  "-language:higherKinds",             // Allow higher-kinded types
  "-language:implicitConversions",     // Allow definition of implicit functions called views
  "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
  "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
  "-Xfuture",                          // Turn on future language features.
  "-Xlint:_",                          // enable all linting options
  "-Yno-adapted-args",                 // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
  "-Ypartial-unification",             // Enable partial unification in type constructor inference
  "-Ywarn-dead-code",                  // Warn when dead code is identified.
  "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
  "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
  "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
  "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
  "-Ywarn-numeric-widen",              // Warn when numerics are widened.
  "-Ywarn-unused:-explicits,-patvars,_",        // Warn if declarations are unused.
  "-Ywarn-value-discard",              // Warn when non-Unit expression results are unused.
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
  publish := {},
  publishLocal := {},
  PgpKeys.publishSigned := {},
  PgpKeys.publishLocalSigned := {},
  publishArtifact := false
)
