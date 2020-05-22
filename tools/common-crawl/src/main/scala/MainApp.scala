package com.opi.lil

import com.opi.lil.actors.{Bouncer, FileMaster}
import com.opi.lil.actors.FileProtocol.StartDownloading
import com.datastax.driver.core.{ProtocolOptions, Cluster}
import akka.actor.{Props, ActorSystem}
import scala.collection.JavaConversions._

object MainApp extends App{

    val system = ActorSystem("Common-Crawl-Manager")

    private def config = system.settings.config

    private val cassandraConfig = config.getConfig("akka.main.db.cassandra")
    private val port = cassandraConfig.getInt("port")
    private val hosts = cassandraConfig.getStringList("hosts").toList

    lazy val dbaseHandler: Cluster =
      Cluster.builder().
        addContactPoints(hosts: _*).
        withCompression(ProtocolOptions.Compression.SNAPPY).
        withPort(port).
        build()

    val m = system.actorOf(Props(new FileMaster(dbaseHandler)), name = "Master")
    val numberOfActors = 12 // 3 times more than #cores

    m ! StartDownloading("wet.paths", numberOfActors)

    system.awaitTermination()


}
