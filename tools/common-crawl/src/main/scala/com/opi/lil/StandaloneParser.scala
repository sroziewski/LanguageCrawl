package com.opi.lil

import akka.actor.Status.{Failure, Success, Status}
import com.opi.lil.actors.BouncerProtocol
import com.opi.lil.actors.BouncerProtocol.{WARCMessage}
import com.opi.lil.gz.GzURLIterator
import com.opi.lil.utils.Iterator._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


class StandaloneParser {

  def parse(url: String) = {
    val it = GzURLIterator(url)
    val fs: List[Future[(WARCMessage, Status)]] = transform(it){
      line => {
        println(s"[iterator] $line [${line.contains("WARC/1.0")}]")
        line.contains("WARC/1.0")
      }
    }{
      lines => Future { (BouncerProtocol.buildWARCMessage(lines), Success()) }
    }

    val f: Future[List[(WARCMessage, Status)]] = Future.sequence(fs)

    f.onSuccess{ case result => println(s"[Success] List size: ${result.size}")}
    f.onFailure{ case e =>  println(s"[Failure] exception: $e)") }

    f.onComplete( res => { println(s"[Completed]!") })
  }
}
