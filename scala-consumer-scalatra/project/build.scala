import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import sbt._
import Keys._
import org.scalatra.sbt._

object KoauthsamplescalatraBuild extends Build {
  val Organization = "com.hunorkovacs"
  val Name = "koauth-sample-scala-consumer-scalatra"
  val Version = "1.1.1-SNAPSHOT"
  val ScalaVersion = "2.11.6"
  val ScalatraVersion = "2.4.0.RC1"

  lazy val project = Project (
    Name,
    file("."),
    settings = ScalatraPlugin.scalatraWithJRebel ++ Seq(
      organization := Organization,
      name := Name,
      version := Version,
      scalaVersion := ScalaVersion,
      resolvers += Classpaths.typesafeReleases,
      resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
      libraryDependencies ++= Seq(
        "org.scalatra" %% "scalatra" % ScalatraVersion,
        "ch.qos.logback" % "logback-classic" % "1.1.2" % "runtime",
        "org.eclipse.jetty" % "jetty-webapp" % "9.1.5.v20140505" % "container;compile",
        "org.eclipse.jetty" % "jetty-plus" % "9.1.5.v20140505" % "container",
        "javax.servlet" % "javax.servlet-api" % "3.1.0",
        "io.spray" %% "spray-client" % "1.3.3",
        "com.typesafe.akka" %% "akka-actor" % "2.3.11",
        "com.hunorkovacs" %% "koauth" % "1.1.0"
      )
    )
  ).enablePlugins(JavaAppPackaging)
}
