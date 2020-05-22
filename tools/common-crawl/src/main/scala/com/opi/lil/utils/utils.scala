package com.opi.lil.utils

import java.text.SimpleDateFormat
import java.util.{TimeZone, Calendar}

import com.datastax.driver.core.{ResultSet, ResultSetFuture}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Promise, Future}

object Iterator {

  def transform[A,B](it: Iterator[A])
                    (takeWhile: A => Boolean)
                    (accumulator: List[A] => Future[B]):  List[Future[B]] = {
    @annotation.tailrec
    def loop(acc: List[Future[B]]): List[Future[B]] = {

      @annotation.tailrec
      def takeOne(buffer: ListBuffer[A]): Future[B] = {
        if (it.hasNext) {
          val line = it.next()
          if (takeWhile(line) && buffer.nonEmpty) {
            accumulator(buffer.result())
          } else {
            takeOne(buffer += line)
          }
        } else { accumulator(buffer.result())}
      }

      if (it.isEmpty)
        acc
      else
        loop(takeOne(new ListBuffer[A]) :: acc)
    }
    val res = loop(Nil)
    println(s"Finished, res size: ${res.size}")
    res
  }
}


object Timer {

  def time[R](block: => R): R = {
    val t0 = System.currentTimeMillis()
    val result = block    // call-by-name
    val t1 = System.currentTimeMillis()
    println("Elapsed time: " + prettyTime(t1 - t0))
    result
  }

  private[this] def isWeekend(localCalendar: Calendar): Boolean = {
    val weekend = Seq(1,7) // 1 is Sun, 7 is Sat
    val dow = localCalendar.get(Calendar.DAY_OF_WEEK)
    weekend.foldLeft(false)((r,c) => r||c==dow)
  }

  private[this] def isBehindWork(localCalendar: Calendar): Boolean = {
    val time = localCalendar.getTime
    val hourFormat = new SimpleDateFormat("HH")
    val hour = Integer.parseInt(hourFormat.format(time))
    hour > 16 || hour < 7 || isWeekend(localCalendar)
  }

  def doWhenYouCan[R](block: => R): R = {
    val localCalendar = Calendar.getInstance(TimeZone.getDefault())
   ! isBehindWork(localCalendar) match {
      case true =>
        block // call-by-name
      case false =>
        Thread sleep 100000; doWhenYouCan(block)  // check once a hundred second
    }
  }

  def prettyTime(milis: Long): String = {

    val ss = (milis / 1000) % 60
    val mm = ((milis / 1000)/60) % 60
    val hh = (((milis / 1000)/60)/60) % 24
    val dd = (((milis / 1000)/60)/60)/24

    s"$dd:$hh:$mm:$ss - ($milis) milliseconds"
  }
}

object CassandraWrapper {
  import com.google.common.util.concurrent._

  implicit def resultSetFutureToScala(f: ResultSetFuture): Future[ResultSet] = {
    val p = Promise[ResultSet]()
    Futures.addCallback(f,
      new FutureCallback[ResultSet] {
        def onSuccess(r: ResultSet) = p success r
        def onFailure(t: Throwable) = p failure t
      })
    p.future
  }
}
