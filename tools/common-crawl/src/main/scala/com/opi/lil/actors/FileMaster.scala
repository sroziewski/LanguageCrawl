package com.opi.lil.actors

import com.opi.lil.actors.BouncerProtocol.PleaseLetMeIn
import com.opi.lil.utils.Timer._
import com.opi.lil.core.GenericRouter._
import com.opi.lil.core.DatabaseHandler

import akka.actor.Status.Status
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.datastax.driver.core.Cluster
import com.sun.rowset.internal.Row


import scala.io.Source

/**
*  Created by roziewski on 2015-04-01.
*/
class FileMaster(dbaseHandler: Cluster) extends Actor with ActorLogging{
  import FileProtocol._
  import scala.collection.JavaConversions._
  import com.opi.lil.core.Cassandra
  import context.dispatcher

  private[this] val prefix = "https://aws-publicdatasets.s3.amazonaws.com/"
  private[this] val threshold = 1
  private[this] val databaseHandler = new DatabaseHandler(dbaseHandler)

  private[this] val bouncerRouter = makeRouter[Bouncer, PleaseLetMeIn]("BouncerRouter", context)

  private[this] var urls: List[String] = Nil
  private[this] var urlStatues: List[(String, Status)] = Nil

  def receive = {

    case StartDownloading(src, numActors) =>

      log.info(s"StartDownloading message received, creating ${numActors} actors")

      val workers = createWorkers(numActors)
      urls = getURLs(src)
      beginProcessing(urls, workers)

    case ProcessingFinished(url, data: Data) =>
      urlStatues = urlStatues :+ (url, data.status)
      log.info(s"FileWorker ${sender} has finished processing url: ${url}")
      databaseHandler.saveURLData(data)
      if (isFinished) shutdown()
  }

  private[this] def createWorkers(numActors: Int) = {
    for (i <- 0 until numActors) yield context.actorOf(Props(new FileWorker(databaseHandler, bouncerRouter)), name = s"worker-${i}")
  }

  private[this] def getURLs(src: String): List[String] = {
    Source.fromFile(src)
      .getLines()
      .map(line => prefix + line)
      .toList
  }

  private[this] def beginProcessing(urls: List[String], workers: Seq[ActorRef]) {
    urls.zipWithIndex.foreach( e => {
//      doWhenYouCan(workers(e._2 % workers.size) ! ProcessFile(e._1))
      workers(e._2 % workers.size) ! ProcessFile(e._1)
    })
  }

  private[this] def isFinished = {
    log.info(s"Size of urlStatuses: ${urlStatues.size} >= threshold: ${threshold} -> Stop")
    (urlStatues.size >= threshold) || (urlStatues.size == urls.size)
//    (urlStatues.size == urls.size)
  }

  private[this] def shutdown() {
    log.info(s"Number of files to process: ${urls.size}")
    log.info(s"Number of files processed: ${urlStatues.size}")
    log.info(s"System context shutdown: ${self}")
    context.system.shutdown()
  }

  override def postStop(): Unit = {
    log.info(s"Master actor is stopped: ${self}")
  }
}



