package actor

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.datastax.driver.core.Row
import document.CheckerProtocol.CheckMe
import document.DatabaseHandler
import document.DocumentProtocol.{ProcessedDocument, ReceiveProcessed}
import utils.{Md5, SpellCorrector}

/**
 * Created by sroziewski on 15/04/2020.
 */

class DocumentChecker extends Actor with ActorLogging {

  private var count: Int = 0
  private val notSentence = "[--]{2,}|[|]|[——]{2,}".r
  private val emailReg = "[^@]+@[^@]+\\.[^@]+"
  private val urlReg = "\\b(https?|ftp|file|www)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"
  private val siteReg = "[\\.a-zA-Z0-9]+\\.\\b(pl|com)"
  private val digitReg = "[0-9=%,\\.]+"
  private val trashReg = "[-—,%&*()<>:;+\"]+"
  private val commasReg = "[\"\\.,!\\?\\{\\}\\(\\)»+-_<>\\|\\]%°′″]"

  def receive: Receive = {

    case CheckMe(document: Row, dbaseHandler: DatabaseHandler, spellCorrector: SpellCorrector, sentenceWorkerRef: ActorRef) =>
      val originalSender = sender()

      log.info(s"Checker ${self} has received CheckMe for a document...")

//      val sentences: Array[String] = document.getString("content").split("(?<=[.?!])\\s+(?=[a-zA-Z])").filter(_.split("\\s+").size>2)
//      var result: String = ""
//      if(sentences.length>4){
//        result = sentences
//          .map(processSentence(_, spellCorrector))
//          .filter(_.length>1)
//          .foldLeft("")((r,c) => r+" "+c)
//          .trim
//      }
//
//      if(result.length>1){
//        val md5 = Md5.md5HashString(document.getString("key"))
//        val pd = ProcessedDocument(md5, result, document.getString("key"))
//        println(pd)
////        originalSender ! ReceiveProcessed(pd)
//      }

  }

  private def processSentence(input: String, spellCorrector: SpellCorrector): String = {
    val regex = ":\\)|:\\(|:P|;\\)|\\^\\.\\^|:~\\(|:\\-o|:\\*\\-/|:\\-c|:\\-D|:'|:bow:|:whistle:|:zzz:|:kiss:|:rose:";

    val sentence = input.trim.replaceAll(regex, "") // remove smileys

    if(notSentence.findFirstIn(sentence).isEmpty){
      sentence
        .split("\\s+")
        .map(_.toLowerCase)
        .filter(!_.matches(emailReg))
        .filter(!_.matches(urlReg))
        .filter(!_.matches(siteReg))
        .filter(!_.contains(digitReg))
        .filter(!_.matches(trashReg))
        .map(_.replaceAll(commasReg, ""))
        .filter(_.length>1)
        .map(w=>{
          val containsMess = w.map(_.toInt).filter(_==65533).length > 0
          if(containsMess){
            val corrected = spellCorrector.correctPolish(w)
            if(corrected.map(_.toInt).filter(_==65533).length==0) corrected else ""
          } else w
        })
        .filter(_.length>1)
        .foldLeft("")((r,c) => r+" "+c)
        .trim
    }
    else ""
  }

  override def postStop(): Unit = {
    log.info(s"Bouncer actor is stopped: ${self}, $count messages received")
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {

    log.error(reason, "Bouncer restarting due to [{}] when processing [{}]",
      reason.getMessage(), if (message.isDefined) message.get else "")

    context.children foreach { child =>
      context.unwatch(child)
      context.stop(child)
    }
    postStop()
  }

}


