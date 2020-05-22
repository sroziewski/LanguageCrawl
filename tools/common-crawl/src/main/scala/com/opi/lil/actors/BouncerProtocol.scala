package com.opi.lil.actors

import com.opi.lil.core.DatabaseHandler

import scala.collection.mutable.ListBuffer

/**
 * Created by roziewski on 2015-04-03.
 */
object BouncerProtocol {

  case class WARCMessage(
                        WARCType: String,
                        WARCTargetURI: String ,
                        WARCDate: String,
                        WARCRecordID: String,
                        WARCRefersTo: String,
                        WARCBlockDigest: String,
                        ContentType: String,
                        ContentLength: Int,
                        Text: String)

  case class PleaseLetMeIn(document: WARCMessage, dbaseHandler: DatabaseHandler, url: String)

  private[this] val WARCType = "WARC-Type"
  private[this] val WARCTargetURI = "WARC-Target-URI"
  private[this] val WARCDate = "WARC-Date"
  private[this] val WARCRecordID = "WARC-Record-ID"
  private[this] val WARCRefersTo = "WARC-Refers-To"
  private[this] val WARCBlockDigest = "WARC-Block-Digest"
  private[this] val ContentType = "Content-Type"
  private[this] val ContentLength = "Content-Length"

  def buildWARCMessage(chunk: List[String]): WARCMessage = {

    var wType: String = new String()
    var wTarget: String = new String()
    var wDate: String = new String()
    var wRecord: String = new String()
    var wRef: String = new String()
    var wDigest: String = new String()
    var wContentType: String = new String()
    var wContentLength: Int = 0

    val wetHeader = getWetHeader(chunk)
    val wText = (chunk diff wetHeader).mkString(" ")
    val splitWetHeader = wetHeader.map(x=>x.split(":"))

    splitWetHeader.foreach(arr =>{
      arr(0) match {
        case WARCType => wType = arr.drop(1).mkString(":").trim
        case WARCTargetURI => wTarget = arr.drop(1).mkString(":").trim
        case WARCDate => wDate = arr.drop(1).mkString(":").trim
        case WARCRecordID => wRecord = arr.drop(1).mkString(":").trim
        case WARCRefersTo => wRef = arr.drop(1).mkString(":").trim
        case WARCBlockDigest => wDigest = arr.drop(1).mkString(":").trim
        case ContentType => wContentType = arr.drop(1).mkString(":").trim
        case ContentLength => wContentLength = arr(1).trim.toInt
        case _ => None
      }
    })
    WARCMessage(wType,wTarget,wDate,wRecord,wRef,wDigest,wContentType,wContentLength,wText)
  }

  private[this] def getWetHeader(chunk: List[String]): List[String] = {
    import scala.util.control.Breaks._
    val buffer = new ListBuffer[String]
    breakable {
      chunk.foreach(currentLine => {
        if (currentLine.contains(ContentLength) && buffer.nonEmpty) {
          buffer += currentLine
          break
        }
        else
          buffer += currentLine
      })
    }
    buffer.result()
  }
}
