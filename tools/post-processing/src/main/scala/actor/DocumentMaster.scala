package actor

import akka.actor.{Actor, ActorRef}
import com.datastax.driver.core.{BatchStatement, Cluster, PreparedStatement, Row}
import document.DatabaseHandler
import document.DocumentProtocol.{ProcessDocuments, ProcessedDocument, StartIteratingOverDocuments}
import monix.execution.{Ack, Scheduler}
import monix.reactive.Observable
import utils.{Md5, SpellCorrector}

import scala.collection.mutable.ListBuffer


class DocumentMaster(cluster: Cluster) extends Actor  {

  val databaseHandler = new DatabaseHandler(cluster)

  val corpus = "nkjp-corpus.txt"
  val sp = new SpellCorrector(corpus)
  var batchStatement = new BatchStatement()
  val writeDataStatement = databaseHandler.getSession.prepare("INSERT INTO document(hash, content, key) VALUES (?, ?, ?);")
  val contentQuery: PreparedStatement = databaseHandler.getSession.prepare("select * from text;")
  private var count: Int = 0
  private val notSentence = "[--]{2,}|[|]|[——]{2,}".r
  private val emailReg = "[^@]+@[^@]+\\.[^@]+"
  private val urlReg = "\\b(https?|ftp|file|www)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"
  private val siteReg = "[\\.a-zA-Z0-9]+\\.\\b(pl|com)"
  private val digitReg = "[0-9=%,\\.]+"
  private val trashReg = "[-—,%&*()<>:;+\"]+"
  private val commasReg = "[\"\\.,!\\?\\{\\}\\(\\)ӝʒ©̨®▛♠…·³¿›▌§±÷¦♣ៀ—ê♥►═„✷★¡ã\u0094\u0093\u009E\uD83D\uDC9C\u0082\u0083\u0087\u009B\u0083\u0080¢\u009Dˇ\u0082!█~^˘■º→™♦_`@●£¥◄◊¶$â¹✔*¬#”€\\\\'“»+-_<>\\|\\]%°′″]"

  private val currentDirectory = new java.io.File(".").getCanonicalPath

  private var totalSentences = 0
  private var sumOfLengthSentences = 0
  private var sumOfCharSentences: Long = 0
  private val outputLogCount = 10000

  override def receive: Receive = {
    case StartIteratingOverDocuments(numActors, sentenceLength) =>
      implicit val cs = databaseHandler.getSession
      implicit val scheduler = Scheduler.Implicits.global

      val observable: Observable[Row] = databaseHandler.query(contentQuery)
      var checked = 0
      // nothing happens until we subscribe to this observable
      observable.subscribe { row =>
        var i = 0
        checked += checkDocument(row, sp, sentenceLength)
        if(i%outputLogCount==0){
          log(checked, i)
        }
        i += 1
        Ack.Continue
      }
  }

  private [this] def log(checked:Int, i:Int): Unit = {
    val str = s"${i} documents processed ${checked} <-- correct...\n" +
      s"totalSentences : ${totalSentences}\n" +
      s"sumOfLengthSentences : ${sumOfLengthSentences}\n"+
      s"sumOfCharSentences : ${sumOfCharSentences}\n"
    utils.Util.writeFile(s"${currentDirectory}/log.txt", str)
    println(str)
  }

  private[this] def beginProcessing(documents: List[ListBuffer[Row]], workers: Seq[ActorRef]) {
      documents.zipWithIndex.foreach(e => {
        workers(e._2 % workers.size) ! ProcessDocuments(e._1)
      })
  }

  def checkDocument(document: Row, spellCorrector: SpellCorrector, sentenceLength:Int) = {
    val sentences: Array[String] = document.getString("content").split("(?<=[.?!])\\s+(?=[a-zA-Z])").filter(_.split("\\s+").size > 2)
    var result: String = ""

    if (sentences.length > sentenceLength) {
      result = sentences
        .map(processSentence(_, spellCorrector))
        .filter(_.length > 1)
        .foldLeft("")((r, c) => r + "|" + c)
        .trim
    }
	result = result.slice(1, result.length)
	
    if (result.length > 1) {
      // some statistics
      totalSentences += sentences.size
      sumOfLengthSentences += result.split("\\s+").size
      sumOfCharSentences += result.split("\\s+").foldLeft(0)((acc, cur)=>acc+cur.length)
      val md5 = Md5.md5HashString(document.getString("key"))
      val pd = ProcessedDocument(md5, result, document.getString("key"))
      databaseHandler.saveDocument(pd)
      1
    }
    else{
      0
    }
  }

  def processSentence(input: String, spellCorrector: SpellCorrector): String = {
    val regex = ":\\)|:\\(|:P|;\\)|\\^\\.\\^|:~\\(|:\\-o|:\\*\\-/|:\\-c|:\\-D|:'|:bow:|:whistle:|:zzz:|:kiss:|:rose:";

    val sentence = input.trim.replaceAll(regex, "") // remove smileys

    if (notSentence.findFirstIn(sentence).isEmpty) {
      sentence
        .split("\\s+")
        .map(_.toLowerCase)
        .filter(!_.matches(emailReg))
        .filter(!_.matches(urlReg))
        .filter(!_.matches(siteReg))
        .filter(!_.contains(digitReg))
        .filter(!_.matches(trashReg))
        .map(_.replaceAll(commasReg, ""))
        .filter(_.length > 1)
        .map(w => {
          val containsMess = w.map(_.toInt).filter(_ == 65533).length > 0
          if (containsMess) {
            ""
          } else w
        })
        .filter(_.length > 1)
        .foldLeft("")((r, c) => r + " " + c)
        .trim
    }
    else ""
  }

}
