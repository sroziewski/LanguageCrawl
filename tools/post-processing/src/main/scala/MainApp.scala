import actor.{DocumentMaster, DocumentProcessor}
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


    val numberOfActors = 1
    var documentMaster : ActorRef = null

    import scopt.OParser
    val builder = OParser.builder[Config]
    val parser1 = {
        import builder._
        OParser.sequence(
            programName("scopt"),
            head("scopt", "4.x"),
            opt[Int]('s', "sentence-length")
              .action((x, c) => c.copy(sentenceLength = x))
              .text("the length of a sentence for Sentence Extractor")
              .validate( x =>
                  if (x > 0) success
                  else failure("Option --sentence-length must be >0") ),
            opt[String]('m', "mode")
              .action((x, c) => c.copy(mode = x))
              .text("select mode [de=Document Extractor, s=Compute Word Count]")
              .validate( x =>
                  if (x=="de"||x=="s") success
                  else failure("Option --mode must be de or s") )
        )
    }
    private var sentenceLength = 0
    OParser.parse(parser1, args, Config()) match {
        case Some(config) =>
            if(config.mode==""){
                println("You must provide --mode or -m option [de=Document Extractor, s=Compute Word Count]")
                System.exit(1)
            }
            sentenceLength = config.sentenceLength
            if(config.mode=="de") {
                if(config.sentenceLength<1){
                    println(("Option --sentence-length or -s must be > 0"))
                    System.exit(1)
                }
                println("de")
                documentMaster = system.actorOf(Props(new DocumentMaster(dbaseHandler)), name = "DocumentMaster")
            }
            if(config.mode=="s") {
                println("s")
                documentMaster = system.actorOf(Props(new DocumentProcessor(dbaseHandler)), name = "DocumentMaster")
            }
        case _ =>
            System.exit(1)
    }
    if(documentMaster!=null)
        documentMaster ! StartIteratingOverDocuments(numberOfActors, sentenceLength)
    system.whenTerminated.wait()
}
