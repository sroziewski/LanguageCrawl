package com.opi.lil.core

import akka.actor.Status.{Failure, Success}
import com.datastax.driver.core.{ResultSetFuture, Row, ResultSet, Cluster}
import core.Keyspaces

/**
 * Created by asobkowicz@opi.org.pl on 2015-04-02.
 */
class DatabaseHandler(dbaseHandler: Cluster) {

  import com.opi.lil.actors.FileProtocol.Data

  import scala.collection.JavaConversions._
  import Cassandra.ResultSet._

  def parseRow(r: Row): Data = {
    val url = r.getString("key")
    val state = r.getInt("state") match {
      case 1 => Success()
      case 0 => Failure(null)
    }
    val successful = r.getInt("completed")
    val all = r.getInt("all")


    Data(url, state, successful, all)
  }

  val session = dbaseHandler.connect(Keyspaces.ngramKeyspace)
  val loadDataStatement  = session.prepare("select * from url where key=?;")
  val writeDataStatement = session.prepare("INSERT INTO url(key, state, completed, all) VALUES (?, ?, ?, ?);")
  val writeTextStatement = session.prepare("INSERT INTO text(key, content, page) VALUES (?, ?, ?);")

  def getURLData(url: String) : Iterable[Data] = {
    session.execute(loadDataStatement.bind(url)).map(row => parseRow(row));
  }

  def saveURLData(data: Data) {

    val state = data.status match {
      case Success(_) => 1
      case Failure(_) => 0
    }
    
    session.executeAsync(writeDataStatement.bind(
      data.url: java.lang.String,
      state: java.lang.Integer,
      data.completed : java.lang.Integer,
      data.all : java.lang.Integer))
  }

  def saveText(id: String, text: String, state: Integer): ResultSetFuture =  {
    session.executeAsync(writeTextStatement.bind(id, text, state : java.lang.Integer))
  }
}
