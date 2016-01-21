/*
 * Copyright 2016 org.NLP4L
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nlp4l.framework.builtin

import java.io.File

import org.apache.lucene.analysis.ja.{JapaneseTokenizer, JapaneseAnalyzer}
import org.apache.lucene.analysis.ja.tokenattributes.ReadingAttribute
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.analysis.util.CharArraySet
import org.nlp4l.framework.models._
import org.nlp4l.framework.processors._
import play.api.Logger

import scala.collection.mutable.ListBuffer
import collection.JavaConversions._
import scala.io.Source
import scala.util.matching.Regex

class TextRecordsProcessorFactory(settings: Map[String, String]) extends ProcessorFactory(settings) {
  override def getInstance: Processor = {
    new TextRecordsProcessor(getStrParamRequired("file"), settings.getOrElse("encoding", "UTF-8"))
  }
}

class TextRecordsProcessor(val file: String, val encoding: String) extends Processor {
  override def execute(data: Option[Dictionary]): Option[Dictionary] = {
    val ff = new File(file).getAbsoluteFile
    val f = Source.fromFile(ff, encoding)
    try {
      Some(Dictionary(f.getLines().map(a => Record(Seq(Cell("text", a.trim)))).toList))
    }
    finally {
      f.close()
    }
  }
}

class StandardSolrQueryLogProcessorFactory(settings: Map[String, String]) extends RecordProcessorFactory(settings) {

  val REGEX = "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}).*INFO.*o.a.s.c.S.Request.*/select params=\\{(.*)\\} hits=(\\d+) status=\\d+ QTime=(\\d+)"

  override def getInstance: RecordProcessor = {
    new StandardSolrQueryLogProcessor(settings.getOrElse("regex", REGEX).r, settings.getOrElse("separator", ","))
  }
}

class StandardSolrQueryLogProcessor(val pattern: Regex, val separator: String) extends RecordProcessor {

  private val logger = Logger(this.getClass)

  override def execute(data: Option[Record]): Option[Record] = {
    data match {
      case Some(record) => {
        val value = record.cellList(0).value.asInstanceOf[String]
        value match {
          case pattern(a,b,c,d) => {
            val cellDate = Cell("date", a)
            val params = b.split("&")
            val q = params.filter(_.startsWith("q=")).map(a => a.substring(2))
            val cellQ = if(q.length > 0) Cell("q", q(0)) else Cell("q", "")
            val cellFq = Cell("fq", params.filter(_.startsWith("fq=")).map(a => a.substring(3)).mkString(separator))
            val cellFacetField = Cell("facet_field", params.filter(_.startsWith("facet.field=")).map(a => a.substring(12)).mkString(separator))
            val cellFacetQuery = Cell("facet_query", params.filter(_.startsWith("facet.query=")).map(a => a.substring(12)).mkString(separator))
            val cellHits = Cell("hits", c.toInt)
            val cellQTime = Cell("QTime", d.toInt)
            Some(Record(Seq(cellDate, cellQ, cellFq, cellFacetField, cellFacetQuery, cellHits, cellQTime)))
          }
          case _ => None
        }
      }
      case _ => None
    }
  }
}

class StandardSolrQueryLogDictionaryAttributeFactory(settings: Map[String, String]) extends DictionaryAttributeFactory(settings) {
  override def getInstance: DictionaryAttribute = {

    val list = Seq[CellAttribute](
      CellAttribute("date", CellType.StringType, true, true),
      CellAttribute("q", CellType.StringType, true, true),
      CellAttribute("fq", CellType.StringType, true, true),
      CellAttribute("facet_field", CellType.StringType, true, true),
      CellAttribute("facet_query", CellType.StringType, true, true),
      CellAttribute("hits", CellType.IntType, true, true),
      CellAttribute("QTime", CellType.IntType, true, true)
    )
    new DictionaryAttribute("solrQueryLog", list)
  }
}

object StopWordsUtil {

  private val logger = Logger(this.getClass)

  def stopwords(file: String, encoding: String, separator: String, columnNum: Int): Set[String] = {
    if(columnNum < 1) throw new IllegalArgumentException(s"column must be more than zero, but it is set to $columnNum")
    val ff = new File(file).getAbsoluteFile
    val f = Source.fromFile(ff, encoding)
    val commentPat = "#.*"
    try {
      f.getLines()
        .map(_.replaceFirst(commentPat, "").trim)
        .filter(_.length > 0)
        .map(a => {
              val params = a.split(separator)
              if(params.length < columnNum){
                logger.info(s"the line '$a' was skipped as it has less than $columnNum columns")
                ""
              }
              else {
                params(columnNum - 1).trim
              }
            })
        .filter(_.length > 0).toSet
    }
    finally {
      f.close()
    }
  }
}

class StopWordsProcessorFactory(settings: Map[String, String]) extends ProcessorFactory(settings) {
  override def getInstance: Processor = {
    val file = getStrParamRequired("file")
    val encoding = settings.getOrElse("encoding", "UTF-8")
    val separator = settings.getOrElse("separator", ",")
    val columnNum = getIntParam("column", 1)
    val stopwords = StopWordsUtil.stopwords(file, encoding, separator, columnNum)
    val cellName = getStrParamRequired("cellName")
    new StopWordsProcessor(stopwords, cellName)
  }
}

class StopWordsProcessor(val stopwords: Set[String], val cellName: String) extends Processor {

  private val logger = Logger(this.getClass)

  override def execute(data: Option[Dictionary]): Option[Dictionary] = {
    if(stopwords.size == 0){
      logger.warn("there is no available stop words in the file.")
      data
    }
    else{
      data match {
        case Some(dic) => {
          Some(Dictionary(
            dic.recordList.filterNot(stopRecord(_)
            )
          ))
        }
        case None => None
      }
    }
  }

  def stopRecord(r: Record): Boolean = {
    val cell = r.cellMap.get(cellName)
    if(cell == None){
      logger.warn(s"there is no cell named '$cellName' in the record $r")
      false
    }
    else{
      stopwords.contains(cell.get.value.toString)
    }
  }
}

class JaUserDictionaryDictionaryAttributeFactory(settings: Map[String, String]) extends DictionaryAttributeFactory(settings) {
  override def getInstance: DictionaryAttribute = {
    val list = Seq[CellAttribute](
      CellAttribute("surface", CellType.StringType, true, true),
      CellAttribute("terms", CellType.StringType, true, true),
      CellAttribute("readings", CellType.StringType, true, true),
      CellAttribute("pos", CellType.StringType, true, true)
    )
    new DictionaryAttribute("jaUserDict", list)
  }
}

class JaUserDictionaryProcessorFactory(settings: Map[String, String]) extends ProcessorFactory(settings) {
  override def getInstance: Processor = {
    new JaUserDictionaryProcessor(getStrParamRequired("cellName"), settings.getOrElse("pos", "カスタム名詞"))
  }
}

class JaUserDictionaryProcessor(val cellname: String, val pos: String) extends Processor {

  val analyzer = new JapaneseAnalyzer(null, JapaneseTokenizer.Mode.NORMAL, CharArraySet.EMPTY_SET, Set.empty[String])

  override def execute(data: Option[Dictionary]): Option[Dictionary] = {
    data match {
      case Some(dic) => {
        val records = ListBuffer.empty[Record]
        dic.recordList.foreach( record => {
          val value = record.cellValue(cellname)
          if(value != None){
            val surface = value.get.toString
            val tr: (String, String) = generateRecord(surface)
            records += Record(Seq(Cell("surface", surface), Cell("terms", tr._1), Cell("readings", tr._2), Cell("pos", pos)))
          }
        })
        Some(Dictionary(records))
      }
      case _ => None
    }
  }

  def generateRecord(surface: String): (String, String) = {
    val terms = ListBuffer.empty[String]
    val readings = ListBuffer.empty[String]
    val tokenStream = analyzer.tokenStream("", surface)
    val termAttr = tokenStream.getAttribute(classOf[CharTermAttribute])
    val readAttr = tokenStream.getAttribute(classOf[ReadingAttribute])
    tokenStream.reset()
    while(tokenStream.incrementToken()){
      terms += termAttr.toString
      if(readAttr.getReading != null)
        readings += readAttr.getReading
      else
        readings += "NOREADING"
    }
    tokenStream.end()
    tokenStream.close()
    (terms.mkString(" "), readings.mkString(" "))
  }
}
