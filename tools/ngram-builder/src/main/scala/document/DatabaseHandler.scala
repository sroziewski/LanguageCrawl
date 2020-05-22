package document

import java.util.concurrent.Executor

import com.datastax.driver.core._
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}
import document.DocumentProtocol.{Document, ProcessedDocument, RareWord}
import monix.eval.Task
import monix.reactive.Observable

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by Szymon Roziewski 15/04/2020
 */
class DatabaseHandler(dbaseHandler: Cluster) {

  import scala.collection.JavaConversions._

  def parseRow(r: Row): Document = {
    val key = r.getString("key")
    val content = r.getString("content")
    Document(key, content)
  }

  val session: Session = dbaseHandler.connect(Keyspaces.ngramKeyspace)
  val contentQuery: PreparedStatement = session.prepare("select * from text;")
  val documentQuery: PreparedStatement = session.prepare("select * from document;")
  val writeDataStatement = session.prepare("INSERT INTO document(hash, content, key) VALUES (?, ?, ?);")

  def getSession: Session = session

  def getDocuments(query:PreparedStatement): Iterable[RareWord] = {
    session.execute(query.bind()).map(row => RareWord(row.getString("word"), row.getInt("value")))
  }

  def getDocumentsAsFuture(contentQuery : PreparedStatement): Future[ResultSet] = {
    Future {
      session.execute(contentQuery.bind())
    }
  }

  def query(ps : PreparedStatement)(
    implicit executionContext: ExecutionContext, cassandraSession: Session
  ): Observable[Row] = {

    val observable = Observable.fromAsyncStateAction[Future[ResultSet], ResultSet](
      nextResultSet =>
        Task.fromFuture(nextResultSet).flatMap { resultSet =>
          Task((resultSet, resultSet.fetchMoreResults))
        }
    )(getDocumentsAsFuture(ps))

    observable
      .takeWhile(rs => !rs.isExhausted)
      .flatMap { resultSet =>
        val rows = (1 to resultSet.getAvailableWithoutFetching) map (_ => resultSet.one)
        Observable.fromIterable(rows)
      }
  }

  implicit def toScalaFuture[T](lFuture: ListenableFuture[T]): Future[T] = {
    val p = Promise[T]
    Futures.addCallback(lFuture,
      new FutureCallback[T] {
        def onSuccess(result: T) = p.success(result)
        def onFailure(t: Throwable) = p.failure(t)
      }, ExecutionContext.global)
    p.future
  }

  def saveDocument(processedDocument: ProcessedDocument): ResultSetFuture =  {
    session.executeAsync(writeDataStatement.bind(processedDocument.hash, processedDocument.content, processedDocument.key))
  }

  def saveBatch(batchStatement: BatchStatement): ResultSet =  {
    session.execute(batchStatement)
  }

}
