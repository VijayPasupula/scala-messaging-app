import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._
import sbt.io.IO
import sbt.io.syntax.File

ThisBuild / scalaVersion := "3.4.3"

val http4sVersion = "0.23.14"
val fs2Version = "3.9.4"
val catsEffectVersion = "3.5.5"
val circeVersion = "0.14.13"
val laminarVersion = "17.0.0"
val scalaJsDomVersion = "2.8.0"

lazy val backend = (project in file("backend"))
  .settings(
    name := "messaging-backend",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-scala-xml" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.slf4j" % "slf4j-simple" % "2.0.12",
      "co.fs2" %% "fs2-core" % fs2Version,
      "co.fs2" %% "fs2-io" % fs2Version,
      "org.typelevel" %% "cats-effect" % catsEffectVersion
    )
  )

lazy val frontend = (project in file("frontend"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "messaging-frontend",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % circeVersion,
      "io.circe" %%% "circe-generic" % circeVersion,
      "io.circe" %%% "circe-parser" % circeVersion,
      "org.scala-js" %%% "scalajs-dom" % scalaJsDomVersion,
      "com.raquo" %%% "laminar" % laminarVersion
    ),
    scalaJSUseMainModuleInitializer := true,
  )

lazy val root = (project in new File("."))
  .aggregate(backend, frontend)
  .settings(
    name := "scala-messaging-app",
    (Compile / run) := (Compile / run).dependsOn(frontend / Compile / fastOptJS).evaluated, // Corrected scoping for fastOptJS
    (Compile / compile) := (Compile / compile).dependsOn(frontend / Compile / fastOptJS).value   // Corrected scoping for fastOptJS
  )
