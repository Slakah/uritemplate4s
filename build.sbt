
lazy val commonSettings = Seq(
  organization := "com.gubbns",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.12.4"
)

lazy val root = (project in file("."))
  .aggregate(uriTemplate)

lazy val uriTemplate = (project in file("core"))
  .settings(
    commonSettings,
    name := "uri-template",
    testFrameworks += new TestFramework("utest.runner.Framework"),
      libraryDependencies ++= Seq(
      "com.lihaoyi" %% "fastparse" % "1.0.0"
    ) ++ Seq(
      "com.lihaoyi" %% "utest" % "0.6.0"
    ).map(_ % "test")
  )

