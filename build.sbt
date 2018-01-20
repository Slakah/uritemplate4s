import sbtcrossproject.{crossProject, CrossType}

lazy val commonSettings = Seq(
  organization := "com.gubbns",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.12.4",
)

lazy val uriTemplate = crossProject(JSPlatform, JVMPlatform)
  .settings(
    commonSettings,
    name := "uri-template",
    testFrameworks += new TestFramework("utest.runner.Framework"),
      libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "fastparse" % "1.0.0"
    ) ++ Seq(
      "com.lihaoyi" %%% "utest" % "0.6.0"
    ).map(_ % "test")
  )

lazy val uriTemplateJS = uriTemplate.js
lazy val uriTemplateJVM = uriTemplate.jvm
