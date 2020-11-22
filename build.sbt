lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-deprecation",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xlint"
)

lazy val commonSettings = Seq(
  scalaVersion := "2.13.2",
  organization := "me.scf37",
  scalacOptions ++= compilerOptions,
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
)

lazy val testDeps = libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.1.2" % "test, provided"
)

lazy val core = project.in(file("core"))
  .settings(
    name := "core",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.typelevel" %% "cats-effect" % "2.2.0",
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
