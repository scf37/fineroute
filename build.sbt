lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-deprecation",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-unchecked",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xlint"
)

lazy val commonSettings = Seq(
  scalaVersion := "2.12.10",
  organization := "me.scf37",
  scalacOptions ++= compilerOptions,
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7"),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
)

lazy val testDeps = libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.0" % "test, provided"
)

scalaVersion := "2.12.6"

lazy val core = project.in(file("core"))
  .settings(
    name := "core",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.typelevel" %% "cats-effect" % "1.3.1",
    ),
    testDeps,
    commonSettings
  )

lazy val openapi = project.in(file("openapi"))
  .dependsOn(core)
  .settings(
    name := "openapi",
    libraryDependencies ++= Seq(
      "io.swagger.core.v3" % "swagger-core" % "2.1.1",
      "io.swagger.core.v3" % "swagger-models" % "2.1.1"
    ),
    testDeps,
    commonSettings
  )
