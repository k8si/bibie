package edu.umass.cs.iesl.bibie.model

import cc.factorie.app.nlp.{Document, Token, TokenSpan}
import cc.factorie.app.nlp.ner.BIO
import cc.factorie.variable.{CategoricalDomain, DiffList, LabeledCategoricalVariable}

object CitationLabelDomain extends CategoricalDomain[String]

abstract class CLabel(labelname: String) extends LabeledCategoricalVariable(labelname)

class CitationLabel(labelname: String, val token: Token) extends CLabel(labelname) {
  def domain = CitationLabelDomain
  def hasNext = token.hasNext && token.next != null
  def hasPrev = token.hasPrev && token.prev != null
  def next = token.next
  def prev = token.prev
}

class SegmentSpan(doc: Document, start: Int, length: Int, var latch: Boolean = false)(implicit d: DiffList) extends TokenSpan(doc.asSection, start, length) {
  var latchLabel = ""
  var cantBegin = false
  var cantEnd = false
  val restrict = new collection.mutable.HashMap[String, Boolean]()
}

class CitationSpan(doc: Document, labelString: String, start: Int, length: Int) extends TokenSpan(doc.asSection, start, length) {
  val label = new SpanCitationLabel(this, labelString)
  override def toString() = "CitationSpan(" + length + "," + label.categoryValue + ":" + this.string + ")"
}

object SpanLabelDomain extends CategoricalDomain[String]

class SpanCitationLabel(val span: CitationSpan, initialValue: String) extends CLabel(initialValue) {
  def domain = SpanLabelDomain
}

object GrobidLabelDomain extends CategoricalDomain[String] {
  this ++= Vector(
    "<author>",
    "<booktitle>",
    "<date>",
    "<editor>",
    "<institution>",
    "<issue>",
    "<journal>",
    "<location>",
    "<note>",
    "<other>",
    "<pages>",
    "<publisher>",
    "<pubnum>",
    "<tech>",
    "<title>",
    "<volume>",
    "<web>"
  )
  freeze()
}

object BioGrobidLabelDomain extends CategoricalDomain[String] with BIO {
  this ++= encodedTags(GrobidLabelDomain.categories)
  freeze()
}