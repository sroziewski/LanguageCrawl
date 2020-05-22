import sbt.Def.Initialize
import sbt.Keys._
import sbt._

import scala.sys.process.Process

object TaskProvider {

  // defining task
  val build = taskKey[Unit]("Build up package")
  val deploy = taskKey[Unit]("Deploy to remote machine")
  val submit = inputKey[Unit]("Submit given class from jar to Apache Spark")

  // define settings key
  val remote = settingKey[String]("Address to Apache Spark Master")
  val remoteFolder = settingKey[String]("Deploy destination folder")
  val defaultClass = settingKey[String]("Default class to be submitted to Apache Spark")

  lazy val deployImpl: Initialize[Task[Unit]] =
    Def.task {
      val jar = new JarData(name.value, version.value, scalaVersion.value)
      val remoteName = remote.value
      val dstFolder = remoteFolder.value
      val srcFolder = jar.fileFolder()
      val fileName = jar.fileName()
      Process(s"cmd /C script\\deploy $remoteName $dstFolder $srcFolder $fileName").!
    }
}