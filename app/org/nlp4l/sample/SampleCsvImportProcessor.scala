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

package org.nlp4l.sample

import java.io.FileNotFoundException
import java.nio.file.Files

import com.typesafe.config.Config
import opennlp.tools.namefind.{NameFinderME, TokenNameFinderModel}
import opennlp.tools.sentdetect.{SentenceDetectorME, SentenceModel}
import opennlp.tools.tokenize.{TokenizerME, TokenizerModel}
import opennlp.tools.util.Span
import org.nlp4l.framework.models.{DictionaryAttribute, Record, _}
import org.nlp4l.framework.processors._
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer


class CsvParseRecordProcessorFactory(settings: Config) extends RecordProcessorFactory(settings) {

  override def getInstance: RecordProcessor = {
    new CsvParseRecordProcessor(getStrListParamRequired("fields"))
  }
}

class CsvParseRecordProcessor(val fields: Seq[String]) extends RecordProcessor {

  // TODO: For a while, this is a RecordProcessor. It will be a Processaor which imports csv data from file.

  override def execute(data: Option[Record]): Option[Record] = {
    data match {
      case Some(record) => {
        val values = record.cellList(0).value.toString.split(",")
        val cells = new ArrayBuffer[Cell]
        fields.zipWithIndex.foreach { case (e, i) => {
            cells += Cell(e.trim, values(i))
          }
        }
        Some(Record(cells.toSeq))
      }
      case _ => None
    }
  }
}