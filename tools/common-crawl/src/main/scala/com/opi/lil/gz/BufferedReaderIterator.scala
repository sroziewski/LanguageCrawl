package com.opi.lil.gz

import java.io.BufferedReader

/**
 * Created by Stokowiec on 2015-04-02.
 */

class BufferedReaderIterator(reader: BufferedReader) extends Iterator[String] {
  var currentLine: String = null
  var isEmpty_ = false

  override def hasNext() = {
    currentLine = reader.readLine()
    isEmpty_ = currentLine == null
    !isEmpty_
  }

  override def next() = currentLine;
  override def isEmpty() = isEmpty_
}