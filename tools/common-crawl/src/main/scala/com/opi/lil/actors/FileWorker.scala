package com.opi.lil.actors

import akka.actor.SupervisorStrategy.Restart
import akka.util.Timeout
import com.opi.lil.actors.FileProtocol.{Data, ProcessFile, ProcessingFinished}
import com.opi.lil.core.{DatabaseHandler, Cassandra}
import com.opi.lil.gz.{GzFileIterator, GzURLIterator}
import com.opi.lil.utils.Iterator.transform

import scala.concurrent.{Future}
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor._
import akka.actor.Status._
import akka.pattern._

class FileWorker(dbaseHandler: DatabaseHandler, bouncerRouter: ActorRef) extends Actor with ActorLogging {
  import BouncerProtocol._
  import scala.concurrent.duration._

  implicit val askTimeout = Timeout(5 seconds)

  def receive = {
    case ProcessFile(url:String) => processFile(url)
  }

  private def processFile(url:String) = {

    log.info(s"Download $url ...")

    val originalSender = sender

    if (isNotProcessed(url)) {
      log.info(s"FileWorker ${self} is processing url: ${url}")
      val fileName = "/tmp/cc/"+url.split("/").last
      downloadFile(url, fileName)
      val it = GzFileIterator(fileName)
      val fs: List[Future[(WARCMessage, Status)]] = transform(it){
        line => line.contains("WARC/1.0")}{
        lines =>
          val msg = BouncerProtocol.buildWARCMessage(lines)
          bouncerRouter ? PleaseLetMeIn(msg, dbaseHandler, url) map {
            case _ => (msg, Success())
          } recover {
            case e: Exception => (msg, Failure(e))
          }
      }

      val f: Future[List[(WARCMessage, Status)]] = Future.sequence(fs)

      f.onSuccess{
        case result => {
          val completed = countCompleted(result)
          log.info(s"[Success] List size: ${result.size}, successCount: $completed)")
          originalSender ! ProcessingFinished(url, Data(url, Success(), completed, result.size))
        }
      }

      f.onFailure{
        case e => {
          log.info(s"[Failure] exception: $e)")
          originalSender ! ProcessingFinished(url, Data(url, Failure(e), 0,0))
        }
      }
    }
  }

  //TODO: Make it non-blocking
  private def isNotProcessed(url: String) = {
    val results = dbaseHandler.getURLData(url)
    results.isEmpty || results.head.status.isInstanceOf[Failure]
  }

  private def countCompleted(result:List[(WARCMessage, Status)]): Int = {
    result.count( _._2 match {
      case Success(_) => true
      case _ => false
    })
  }

  override def postStop(): Unit = {
    log.info(s"Worker actor is stopped: ${self}")
  }

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10,
    withinTimeRange = 10 seconds) {
    case _: Exception =>
      log.info(s"FileWorker actor is retrying: ${self}")
      Restart
  }

  private[this] def downloadFile(url: String, destination: String) = {
    import sys.process._
    import java.net.URL
    import java.io.File

    new URL(url) #> new File(destination)!!
  }

}
