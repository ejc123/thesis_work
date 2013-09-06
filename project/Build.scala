import sbt._
import sbt.Keys._

object MyBuild extends Build {

  lazy val project = Project("root", file(".")) settings(
    //organization := "org.sample.demo",

    name := "geotrellis-example",

    scalaVersion := "2.10.2",

    scalacOptions ++= Seq("-deprecation",
                          "-unchecked",
                          "-optimize",
                          "-feature"),

    parallelExecution := false,

    mainClass in (Compile, run) := Some("geotrellis.rest.WebRunner"),

    javaOptions in run += "-Xmx8G",

    libraryDependencies ++= Seq(
      "junit" % "junit" % "4.5" % "test",
      "org.eclipse.jetty" % "jetty-webapp" % "8.1.0.RC4",
      "com.sun.jersey" % "jersey-bundle" % "1.11",
      "com.azavea.geotrellis" %% "geotrellis-geotools" % "0.9.0-SNAPSHOT",
      "com.azavea.geotrellis" %% "geotrellis-server" % "0.9.0-SNAPSHOT",
      "com.azavea.geotrellis" %% "geotrellis-tasks" % "0.9.0-SNAPSHOT",
      "com.azavea.geotrellis" %% "geotrellis" % "0.9.0-SNAPSHOT"
    ),

    resolvers ++= Seq(
      "opengeo" at "http://repo.opengeo.org/",
      "NL4J Repository" at "http://nativelibs4java.sourceforge.net/maven/",
      "maven2 dev repository" at "http://download.java.net/maven/2",
      "Scala Test" at "http://www.scala-tools.org/repo-reloases/",
      "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
      "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
      "sonatypeSnapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
    ),

    // enable forking in run
    fork in run := true
  )
}
