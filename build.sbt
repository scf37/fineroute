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
  scalaVersion := "2.12.4",
  name := "fineroute",
  organization := "me.scf37",
  scalacOptions ++= compilerOptions
)

lazy val testDeps = libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.0" % "test, provided"
)

scalaVersion := "2.12.6"

lazy val fineroute = project.in(file("."))
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.typelevel" %% "cats-effect" % "1.3.1",
      "com.softwaremill.tapir" %% "tapir-core" % "0.10.1"
    ),
    testDeps,
    commonSettings
  )

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7")
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
