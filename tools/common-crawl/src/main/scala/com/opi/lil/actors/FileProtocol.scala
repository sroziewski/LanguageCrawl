/**
 * Created by Stokowiec on 2015-04-02.
 */
package com.opi.lil.actors

import akka.actor.Status._

object FileProtocol {

  case class StartDownloading(docRoot: String, numActors: Int)
  case class ProcessFile(url: String)
  case class ProcessingFinished(url: String, data: Data)
  case class Data(url: String, status: Status, completed: Int, all: Int)

}
