package com.opi.lil.actors

import com.opi.lil.actors.BouncerProtocol.{WARCMessage, PleaseLetMeIn}
import com.opi.lil.core.DatabaseHandler
import com.opi.lil.utils.CassandraWrapper.resultSetFutureToScala

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{ActorLogging, Actor}
import akka.actor.Status.{Failure, Success}
import cld2.Cld2

import scala.util.Try

/**
 * Created by roziewski on 2015-04-03.
 */

class Bouncer extends Actor with ActorLogging {

  var count: Int = 0

  def receive = {

    case PleaseLetMeIn(document: WARCMessage, dbaseHandler: DatabaseHandler, url: String) =>
      // log.info(s"Bouncer has received PleaseLetMeIn message with ID: ${document.WARCRecordID} for url: ${url}")
      count += 1
      val originalSender = sender()
      try {

        val text = document.Text.split("\\s+").take(100).mkString(" ")
        if (Cld2.isPolish(text)) {
          // log.info(s"Polish site occurred with WARCTargetURI: ${document.WARCTargetURI} for url: ${url}")

          val resultSetFuture = dbaseHandler.saveText(document.WARCRecordID, document.Text, 0: java.lang.Integer)
          val f = resultSetFutureToScala(resultSetFuture)

          f onFailure {
            case t => originalSender ! Failure(t)
          }
        }
      } catch {
        case e: Exception => {
          log.error(s"[Exception]: $e")
          originalSender ! Failure(e)
        }
      }

      // Success is when there are no exceptions
      originalSender ! Success()
  }

  override def postStop(): Unit = {
    log.info(s"Bouncer actor is stopped: $count messages received")
  }
}


