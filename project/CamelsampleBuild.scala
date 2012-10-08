import sbt._
import sbt.Keys._

object CamelsampleBuild extends Build {

  lazy val camelsample = Project(
    id = "camelsample",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "camelSample",
      organization := "org.xebia",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.10.0-M7",
      resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
      resolvers += "Sonatype" at "https://oss.sonatype.org/content/repositories/releases",
      libraryDependencies ++= Seq("com.typesafe.akka"   %%  "akka-actor"     % "2.1-M2",
                                  "com.typesafe.akka"   %%  "akka-camel"     % "2.1-M2",
                                  "com.typesafe.akka"   %%  "akka-testkit"   % "2.1-M2",
                                  "org.apache.activemq" %   "activemq-core"  % "5.4.1" ,
                                  "org.apache.activemq" %   "activemq-camel" % "5.4.1" ,
                                  "org.scalatest"       %%  "scalatest"      % "1.9-2.10.0-M7-B1")
    )
  )
}
