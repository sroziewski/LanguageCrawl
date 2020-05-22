package utils

import java.nio.charset.CodingErrorAction

import scala.io.Codec
import scala.util.matching.Regex.MatchIterator

/**
 * Created by Szymon Roziewski on 18/04/2020.
 */
class SpellCorrector(corpusDir: String) {

  val alphabet = Array("q","w","e","r","t","y","u","i","o","p","a","s","d","f","g","h","j","k","l","z","x","c","v","b","n","m","ą","ż","ź","ć","ń","ó","ł","ę","ś")
  def train(features : MatchIterator): Map[String, Int] = (Map[String, Int]() /: features)((m, f) => m + ((f, m.getOrElse(f, 0) + 1)))
  def words(text : String): MatchIterator = ("[%s]+" format alphabet.mkString).r.findAllIn(text.toLowerCase)
  val decoder = Codec.UTF8.decoder.onMalformedInput(CodingErrorAction.IGNORE)
  val dict = train(words(scala.io.Source.fromResource(corpusDir)(decoder).mkString))
  val invMap = scala.io.Source.fromResource(corpusDir)(decoder).getLines.map{l => l->1}.toMap
  val i =1
  val polishDiacritics = "[a-zA-Z]*"

  def edits(s : Seq[(String, String)]): Seq[String] = (for((a,b) <- s; if b.length > 0) yield a + b.substring(1)) ++
    (for((a,b) <- s; if b.length > 1) yield a + b(1) + b(0) + b.substring(2)) ++
    (for((a,b) <- s; c <- alphabet if b.length > 0) yield a + c + b.substring(1)) ++
    (for((a,b) <- s; c <- alphabet) yield a + c + b)

  def edits1(word : String): Seq[String] = edits(for(i <- 0 to word.length) yield (word take i, word drop i))
  def edits2(word : String): Seq[String] = for(e1 <- edits1(word); e2 <-edits1(e1)) yield e2
  def known(words : Seq[String]): Seq[String] = for(w <- words; found <- dict.get(w)) yield w
  def or[T](candidates : Seq[T], other : => Seq[T]): Seq[T] = if(candidates.isEmpty) other else candidates

  def candidates(word: String): Seq[String] = or(known(List(word)), or(known(edits1(word)), known(edits2(word))))

  def correctPolish(word : String): String = ((-1, word) /: filterCandidates(candidates(word)))(
  (max, word) => if(dict(word) > max._1) (dict(word), word) else max)._2

  def filterCandidates(words: Seq[String]): Seq[String] = {
    words.filter(!_.matches(polishDiacritics)).foldLeft(List[String]())((z, f) => z :+ f)
  }
}


