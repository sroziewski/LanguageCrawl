import java.io.File

import MainApp.system
import actor.NgramBuilder
import akka.actor.{ActorRef, ActorSystem, Props}
import com.datastax.driver.core.{Cluster, SocketOptions}
import document.DocumentProtocol.StartIteratingOverDocuments
import utils.Config

import scala.collection.JavaConversions._

object MainApp extends App{

    val system = ActorSystem("Document-Extractor-Manager")

    private def config = system.settings.config

    private val cassandraConfig = config.getConfig("akka.main.db.cassandra")
    private val port = cassandraConfig.getInt("port")
    private val hosts = cassandraConfig.getStringList("hosts")
    private val timeout = 10000000

    val socketOptions = new SocketOptions()
    socketOptions.setReadTimeoutMillis(timeout)
    socketOptions.setConnectTimeoutMillis(timeout)
    socketOptions.setKeepAlive(true)

    lazy val dbaseHandler: Cluster =
        Cluster.builder().
          addContactPoints(hosts: _*).
          withPort(port).
          withSocketOptions(socketOptions).
          build()


    import scopt.OParser
    val builder = OParser.builder[Config]
    val parser1 = {
        import builder._
        OParser.sequence(
            programName("scopt"),
            head("scopt", "4.x"),
            opt[Int]('n', "ngram")
              .action((x, c) => c.copy(ngram = x))
              .text("the ngram length")
              .validate( x =>
                  if (x > 0) success
                  else failure("Option --ngram must be >0 [1--5]") )
        )
    }
    private var ngram = 0
    OParser.parse(parser1, args, Config()) match {
        case Some(config) =>
            if(config.ngram == -1){
                println("You must provide --ngram option must be >0 [1--5]")
                System.exit(1)
            }
            ngram = config.ngram
        case _ =>
            System.exit(1)
    }
    var ngramBuilder : ActorRef = system.actorOf(Props(new NgramBuilder(dbaseHandler, ngram)), name = "NgramBuilder")
    ngramBuilder ! StartIteratingOverDocuments()
    system.whenTerminated.wait()
}
