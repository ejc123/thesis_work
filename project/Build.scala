import sbt._
import sbt.Keys._
import sbtassembly.Plugin._
import AssemblyKeys._

object MyBuild extends Build {

  // "com.azavea.geotrellis" %% "geotrellis-server" % "0.9.0-SNAPSHOT" % "compile",
  // "org.eclipse.jetty" % "jetty-webapp" % "8.1.0.RC4",
  // "com.sun.jersey" % "jersey-bundle" % "1.11",
  lazy val sharedSettings = Defaults.defaultSettings ++ Seq(
      //organization := "org.sample.demo",

      name := "geotrellis-example",

      scalaVersion := "2.10.2",

      scalacOptions ++= Seq("-deprecation",
        "-unchecked",
        "-optimize",
        "-feature"),

      parallelExecution := false,

    mainClass in (Compile,run) := Some("geotrellis.rest.WebRunner"),

      javaOptions in run += "-Xmx8G",

      libraryDependencies ++= Seq(
        "com.azavea.geotrellis" %% "geotrellis-geotools" % "0.9.0-SNAPSHOT" % "compile",
        "com.azavea.geotrellis" %% "geotrellis-tasks" % "0.9.0-SNAPSHOT" % "compile",
        "com.azavea.geotrellis" %% "geotrellis" % "0.9.0-SNAPSHOT" % "compile",
        "junit" % "junit" % "4.5" % "test",
        "org.scalatest"  %% "scalatest"  % "1.9.1" % "test",
        "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.2",
        "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.2"
      ),

      resolvers ++= Seq(
        "opengeo" at "http://repo.opengeo.org/",
        "NL4J Repository" at "http://nativelibs4java.sourceforge.net/maven/",
        "maven2 dev repository" at "http://download.java.net/maven/2",
        "Scala Test" at "http://www.scala-tools.org/repo-reloases/",
        "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
        "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
        "sonatypeSnapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
        "Geotools" at "http://download.osgeo.org/webdav/geotools/"
      ),

      // enable forking in run
      fork in run := true
    )

    lazy val localAssembly = assemblySettings ++ Seq(
        mainClass in assembly := Some("net.christeson.geotrellis.template.RunMe"),
        mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
          {
            case x if x startsWith "org/apache/commons/collections" => MergeStrategy.first
            case x if x startsWith "org/postgresql" => MergeStrategy.filterDistinctLines
            case PathList("META-INF", "registryFile.jai") => MergeStrategy.filterDistinctLines
            case x => old(x)
          }
        }
    )

  lazy val project = Project(
       id = "example",
       base = file("."),
       settings = sharedSettings ++ localAssembly
  )
}
