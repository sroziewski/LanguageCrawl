package actor

import akka.actor.Actor
import com.datastax.driver.core.{BatchStatement, Cluster, PreparedStatement, Row}
import document.DatabaseHandler
import document.DocumentProtocol.StartIteratingOverDocuments
import monix.execution.{Ack, Scheduler}
import monix.reactive.Observable
import utils.SpellCorrector

import scala.collection.mutable
import scala.collection.mutable.{HashMap, ListBuffer}

class NgramBuilder(cluster: Cluster, ngram:Int) extends Actor  {

  val databaseHandler = new DatabaseHandler(cluster)

  val corpus = "nkjp-corpus.txt"
  val sp = new SpellCorrector(corpus)
  var batchStatement = new BatchStatement()
  val writeDataStatement = databaseHandler.getSession.prepare(s"INSERT INTO gram${ngram}(word, value) VALUES (?, ?);")
  val contentQuery: PreparedStatement = databaseHandler.getSession.prepare("select * from document;")
  val rareWordsQuery: PreparedStatement = databaseHandler.getSession.prepare("SELECT * FROM count where value<100 allow filtering;")
  val updateDataStatement = databaseHandler.getSession.prepare(s"UPDATE gram${ngram} set value=? where word=?;")
  val selectQuery: PreparedStatement = databaseHandler.getSession.prepare(s"select value from gram${ngram} where word=?;")
  private val currentDirectory = new java.io.File(".").getCanonicalPath

  private val outputLogCount = 10000
  private var rareWords = mutable.HashSet.empty[String]
  private var ngrams = mutable.HashMap.empty[String, Long]

  override def receive: Receive = {
    case StartIteratingOverDocuments() =>
      implicit val cs = databaseHandler.getSession
      implicit val scheduler = Scheduler.Implicits.global

      val observable: Observable[Row] = databaseHandler.query(contentQuery)
      val results = databaseHandler.getDocuments(rareWordsQuery)
      results.foreach(rareWord=>{
        if(! rareWords.contains(rareWord.word))
                  rareWords.add(rareWord.word)
      })
      // nothing happens until we subscribe to this observable
      var j = 0
      observable.subscribe { row =>
        countWords(buildNgram(row, ngram), ngrams)

        if(j%outputLogCount==0){

          ngrams.foreach{
            case (word, count) =>
              val found = databaseHandler.getSession.execute(selectQuery.bind(word))
              if(found.all().size()==0){
                databaseHandler.getSession.execute(writeDataStatement.bind(word, count:java.lang.Long))
              }
              else{
                val v = databaseHandler.getSession.execute(selectQuery.bind(word)).one().getInt("value")
                databaseHandler.getSession.execute(updateDataStatement.bind(count+v:java.lang.Long, word))
              }
          }
          ngrams = HashMap.empty[String, Long]

          log(ngram, j)
        }
        j += 1
        Ack.Continue
      }
  }

  private [this] def log(n:Int, i:Int): Unit = {
    val str = s"${i} documents processed ${i} <-- correct...\n"
    utils.Util.writeFile(s"${currentDirectory}/log-${n}-gram-builder.txt", str)
    println(str)
  }

  def countWords(ws: List[String], map: HashMap[String, Long]): Map[String, Long] = {
    for(word <- ws) {
      val n : Long = map.getOrElse(word, 0)
      map += word -> (n + 1)
    }
    map.toMap
  }

  def buildNgram(document: Row, ngram:Int): List[String] = {
    val words: Array[String] = document.getString("content").split("\\s+")
    var adjustedWords: ListBuffer[String] = new ListBuffer[String]()
    var ngrams: ListBuffer[String] = new ListBuffer[String]()

    words.foreach(word=>{
      if(!rareWords.contains(word)){
        adjustedWords += word
      }
    })
    adjustedWords.sliding(ngram).foreach( l => {
      ngrams += l.mkString(",")
    })
    ngrams.toList
  }

}
