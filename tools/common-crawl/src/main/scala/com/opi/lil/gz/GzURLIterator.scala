package com.opi.lil.gz

import java.io._
import java.net.URL
import java.util.zip.GZIPInputStream

object GzURLIterator {
  def apply(source: String) = {
    new BufferedReaderIterator(
      new BufferedReader(
        new InputStreamReader(
          new GZIPInputStream(
            new URL(source).openStream()))))
  }
}
