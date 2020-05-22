package com.opi.lil.gz

import java.io._
import java.net.URL
import java.util.zip.GZIPInputStream

object GzFileIterator {
  def apply(file: String) = {
    new BufferedReaderIterator(
      new BufferedReader(
        new InputStreamReader(
          new GZIPInputStream(
            new FileInputStream(file)
    ))))
  }
}
