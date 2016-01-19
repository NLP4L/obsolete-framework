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

import org.nlp4l.framework.models._
import org.nlp4l.framework.processors._

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
      Some(Dictionary(f.getLines().map(a => Record(Seq(Cell("text", a)))).toList))
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
            val cellFacetField = Cell("facet.field", params.filter(_.startsWith("facet.field=")).map(a => a.substring(12)).mkString(","))
            val cellFacetQuery = Cell("facet.query", params.filter(_.startsWith("facet.query=")).map(a => a.substring(12)).mkString(","))
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
      CellAttribute("facet.field", CellType.StringType, true, true),
      CellAttribute("facet.query", CellType.StringType, true, true),
      CellAttribute("hits", CellType.IntType, true, true),
      CellAttribute("QTime", CellType.IntType, true, true)
    )
    new DictionaryAttribute("solrQueryLog", list)
  }
}
