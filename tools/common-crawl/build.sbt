import TaskProvider.{defaultClass, deploy, deployImpl, remote, remoteFolder, submit}
import sbt._
import sbt.complete.DefaultParsers.spaceDelimited


lazy val commonSettings = Seq(
  organization := "com.opi.lil",
  version := "1.3",
  scalaVersion := "2.10.4",               // desired scala version
  remote := "spark@10.20.20.213",         // host and user name
  remoteFolder := "/home/spark/dev/cc/",  // dest directory of jar files
  defaultClass := "MainApp",
  deploy := deployImpl.value,
  submit := {
    val args: Seq[String] = spaceDelimited("<arg>").parsed
    val className = if (args==Nil) defaultClass.value else args.head
    val jar = new JarData(name.value, version.value, scalaVersion.value)
    Process(s"cmd /C script\\run-java ${remote.value} ${remoteFolder.value} ${jar.fileName()} $className").!
  }
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "cc",
    libraryDependencies ++= Seq(
      //"com.typesafe.akka" %% "akka-actor" % "2.3.6",
      "com.typesafe.akka" %% "akka-actor" % "2.4-SNAPSHOT",
      "com.typesafe.akka" %% "akka-slf4j" % "2.3.6",
      "ch.qos.logback" % "logback-classic" % "1.1.2",
      "com.sun.jna" % "jna" % "3.0.9",
      "com.datastax.cassandra"  % "cassandra-driver-core" % "2.0.1"  exclude("org.xerial.snappy", "snappy-java"),
      "org.xerial.snappy"       % "snappy-java"           % "1.0.5" //https://github.com/ptaoussanis/carmine/issues/5
      )
  )

mainClass in assembly := Some("com.opi.lil.MainApp")
