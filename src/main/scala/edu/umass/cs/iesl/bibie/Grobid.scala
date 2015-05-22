package edu.umass.cs.iesl.bibie

/**
 * Created by kate on 5/13/15.
 */

import cc.factorie.app.nlp._
import scala.io.Source
import scala.collection.mutable.ArrayBuffer

/**
 * Infrastructure for comparison of GROBID vs IESL.
 * We load documents from a file generated by GROBID's training/eval process, which contains (a) all the features and
 * feature values GROBID uses and (b) gold labels. This way, we can more or less directly compare our learning vs.
 * GROBID's learning. One thing to note however is that GROBID uses a cascade of CRF's (I'm not 100% sure how this works
 * yet). This might be a TODO to consider implementing.
 */

class GoldCitationLabel(val label: String, val token: Token)
class PreFeatures(val features: Array[String], val token: Token)

/*
M m M M M M M M M M LINESTART ALLCAP NODIGIT 1 0 0 0 0 0 0 0 0 0 0 0 NOPUNCT 1 I-<author>
. . . . . . . . . . LINEIN ALLCAP NODIGIT 1 0 0 0 0 0 0 0 0 0 0 0 DOT 1 <author>
Kitsuregawa kitsuregawa K Ki Kit Kits a wa awa gawa LINEIN INITCAP NODIGIT 0 0 0 0 0 0 0 0 0 0 0 0 NOPUNCT 1 <author>
, , , , , , , , , , LINEIN ALLCAP NODIGIT 1 0 0 0 0 0 0 0 0 0 0 0 COMMA 1 <author>
H h H H H H H H H H LINEIN ALLCAP NODIGIT 1 0 0 0 0 0 0 0 0 0 0 0 NOPUNCT 2 <author>
. . . . . . . . . . LINEIN ALLCAP NODIGIT 1 0 0 0 0 0 0 0 0 0 0 0 DOT 2 <author>
Tanaka tanaka T Ta Tan Tana a ka aka naka LINEIN INITCAP NODIGIT 0 1 0 0 0 0 0 0 0 0 0 0 NOPUNCT 2 <author>
, , , , , , , , , , LINEIN ALLCAP NODIGIT 1 0 0 0 0 0 0 0 0 0 0 0 COMMA 3 <author>
and and a an and and d nd and and LINEIN NOCAPS NODIGIT 0 0 1 0 0 0 0 0 0 0 0 0 NOPUNCT 3 <author>
T t T T T T T T T T LINEIN ALLCAP NODIGIT 1 0 0 0 0 0 0 0 0 0 0 0 NOPUNCT 3 <author>
. . . . . . . . . . LINEIN ALLCAP NODIGIT 1 0 0 0 0 0 0 0 0 0 0 0 DOT 4 <author>
Moto moto M Mo Mot Moto o to oto Moto LINEIN INITCAP NODIGIT 0 0 0 0 0 0 0 0 0 0 0 0 NOPUNCT 4 <author>
- - - - - - - - - - LINEIN ALLCAP NODIGIT 1 0 0 0 0 0 0 0 0 0 0 0 HYPHEN 4 <author>
oka oka o ok oka oka a ka oka oka LINEIN NOCAPS NODIGIT 0 1 0 0 0 0 0 0 0 0 0 0 NOPUNCT 5 <author>
 */

object LoadGrobid {
  def fromFilename(filename: String, withFeatures: Boolean = true): Seq[Document] = {
    val whitespace = "\\s+".r
    val buff = new ArrayBuffer[Document]()
    var currDoc = new Document("")
    var currSent = new Sentence(currDoc)
    val lines = Source.fromFile(filename).getLines()
    var tokenCount = 0
    var docCount = 0
    while (lines.hasNext) {
      val line = lines.next()
      val parts = whitespace.split(line)
      if (parts.length > 1) {
        val label = {
          val l = parts.last.dropRight(1)
          if (l.startsWith("I-<")) {
            val ll = l.drop(3)
            "B-" + ll
          } else {
            val ll = l.drop(1)
            "I-" + ll
          }
        }
        val string = parts.head
        val features = parts.dropRight(1)
        val token = new Token(currSent, string)
        if (withFeatures) token.attr += new PreFeatures(features, token) //put in PreFeatures so we can freeze CitationFeaturesDomain after loading training / before loading dev
        //token.attr += new CitationFeatures(token)
        //token.attr[CitationFeatures] ++= features
        token.attr += new CitationLabel(if (!LabelDomain.frozen || LabelDomain.categories.contains(label)) label else "O", token)
        tokenCount += 1
      } else {
        if (currSent.length > 0) currDoc.appendString("")
        if (currDoc.tokenCount > 0) {
          buff += currDoc
          currDoc = new Document("")
          currSent = new Sentence(currDoc)
          docCount += 1
        }
      }
    }
    println(s"Loaded $docCount docs with $tokenCount tokens from file $filename.")
    buff
  }

  def fromFilenameLabeled(filename: String): Seq[Document] = {
    val whitespace = "\\s+".r
    val buff = new ArrayBuffer[Document]()
    var currDoc = new Document("")
    var currSent = new Sentence(currDoc)
    val lines = Source.fromFile(filename).getLines()
    var tokenCount = 0
    var docCount = 0

    assert(lines.nonEmpty, s"no lines loaded from $filename")

    val okayLines = new ArrayBuffer[String]()
    try {
      while (lines.hasNext) okayLines += lines.next()
    } catch {
      case e: Exception => println(e)
    }

    for (line <- okayLines) {
      //      val line = lines.next()
      val parts = whitespace.split(line)
      if (parts.length > 1) {
        val guessLabel = {
          val l = parts.last.dropRight(1)
          if (l.startsWith("I-<")) {
            val ll = l.drop(3)
            "B-" + ll
          } else {
            val ll = l.drop(1)
            "I-" + ll
          }
        }
        val trueLabel = {
          val l = parts.dropRight(1).last.dropRight(1)
          if (l.startsWith("I-<")) {
            val ll = l.drop(3)
            "B-" + ll
          } else {
            val ll = l.drop(1)
            "I-" + ll
          }
        }
        val string = parts.head
        val features = parts.dropRight(1)
        val token = new Token(currSent, string)
        token.attr += new CitationFeatures(token)
        token.attr[CitationFeatures] ++= features
        token.attr += new CitationLabel(if (!LabelDomain.frozen || LabelDomain.categories.contains(guessLabel)) guessLabel else "O", token)
        token.attr += new GoldCitationLabel(if (!LabelDomain.frozen || LabelDomain.categories.contains(trueLabel)) trueLabel else "O", token)
        tokenCount += 1
      } else {
        if (currSent.length > 0) currDoc.appendString("")
        if (currDoc.tokenCount > 0) {
          buff += currDoc
          currDoc = new Document("")
          currSent = new Sentence(currDoc)
          docCount += 1
        }
      }
    }
    println(s"Loaded $docCount docs with $tokenCount tokens from file $filename.")
    buff
  }
}








