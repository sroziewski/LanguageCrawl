/**
 * Created by Szymon Roziewski on 15/04/2020.
 */
package document

import akka.actor.Status._
import com.datastax.driver.core.Row

import scala.collection.mutable.ListBuffer

object DocumentProtocol {

  case class StartIteratingOverDocuments(numActors: Int, sentenceLength:Int)
  case class ProcessDocuments(documents: ListBuffer[Row])
  case class ReceiveProcessed(document: ProcessedDocument)
  case class NumberOfWrittenChunks(count: Int)
  case class ProcessingFinished(url: String, data: ProcessedDocument)
  case class ProcessedDocument(hash: String, content: String, key: String)
  case class Document(key: String, content: String)

}
