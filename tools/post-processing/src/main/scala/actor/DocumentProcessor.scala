package actor

import akka.actor.{Actor, ActorLogging}
import com.datastax.driver.core.{Cluster, PreparedStatement, Row}
import document.DatabaseHandler
import document.DocumentProtocol.StartIteratingOverDocuments
import monix.execution.{Ack, Scheduler}
import monix.reactive.Observable

import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

class DocumentProcessor(cluster: Cluster) extends Actor {

  val databaseHandler = new DatabaseHandler(cluster)
  val documentQuery: PreparedStatement = databaseHandler.getSession.prepare("select * from document;")
  val selectQuery: PreparedStatement = databaseHandler.getSession.prepare("select value from count where word=?;")
  val writeDataStatement = databaseHandler.getSession.prepare("INSERT INTO count(word, value) VALUES (?, ?);")
  val updateDataStatement = databaseHandler.getSession.prepare("UPDATE count set value=? where word=?;")
  implicit val cs = databaseHandler.getSession
  implicit val scheduler = Scheduler.Implicits.global
  var i = 0
  var words = HashMap.empty[String, Int]
  private val outputLogCount = 10000

  override def receive: Receive = {
    case StartIteratingOverDocuments(numActors, sentenceLength) =>

      val observable: Observable[Row] = databaseHandler.query(documentQuery)

      observable.subscribe { row =>
        val x = row.getString("content")
          .split("\\s+").toList

        countWords(x, words)

        if (i % outputLogCount == 0) {
          words.foreach{
            case (word, count) =>
              val found = databaseHandler.getSession.execute(selectQuery.bind(word))
              if(found.all().size()==0){
                databaseHandler.getSession.execute(writeDataStatement.bind(word, count:java.lang.Integer))
              }
              else{
                val v = databaseHandler.getSession.execute(selectQuery.bind(word)).one().getInt("value")
                databaseHandler.getSession.execute(updateDataStatement.bind(count+v:java.lang.Integer, word))
              }
          }
          println(s"${i} documents processed... ")

          words = HashMap.empty[String, Int]
        }
        i+=1
        Ack.Continue
      }
  }

  def countWords(ws: List[String], map: HashMap[String, Int]): Map[String, Int] = {
    for(word <- ws) {
      val n = map.getOrElse(word, 0)
      map += (word -> (n + 1))
    }
    map.toMap
  }

}
