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

package org.nlp4l.framework.builtin.lucene

import com.typesafe.config.Config
import org.nlp4l.framework.models.{Record, _}
import org.nlp4l.framework.processors._
import org.nlp4l.lucene._

import scala.collection.mutable.ArrayBuffer

class LuceneIndexWriterProcessorFactory(settings: Config) extends ProcessorFactory(settings) {

  override def getInstance: Processor = {
    new LuceneIndexWriterProcessor(
      getConfigParam("schemaDef", null),
      getStrParam("schemaFile", null),
      getStrParamRequired("index"),
      getConfigListParam("fieldsMap", null),
      getBoolParam("deleteAll", true),
      getBoolParam("optimize", true)
    )
  }
}

class LuceneIndexWriterProcessor(val schemaDef: Config,
                                 val schemaFile: String,
                                 val index: String,
                                 val fieldsMapConf: Seq[Config],
                                 val deleteAll: Boolean,
                                 val optimize: Boolean)
  extends Processor {

  val fieldsMap: Map[String, String] =
    if (fieldsMapConf != null)
      fieldsMapConf.map(fieldMap => fieldMap.getString("fieldName") -> fieldMap.getString("cellName")).toMap
    else Map()

  override def execute(data: Option[Dictionary]): Option[Dictionary] = {
    val schema =    {
      if (schemaDef != null)
        SchemaLoader.read(schemaDef)
      else if (schemaFile != null) {
        SchemaLoader.loadFile(schemaFile)
      } else {
          throw new IllegalArgumentException("no schema setting found.")
      }
    }
    var counter = 0
    var writer: IWriter = null
    try{
      writer = IWriter(index, schema)

      if (deleteAll) writer.deleteAll()

      val fieldNames = schema.fieldTypes.keySet

      data.get.recordList.foreach {
        record => {
          var fields = new ArrayBuffer[Field]
          fieldNames.foreach {
            fieldName => {
              val cellName = fieldsMap.getOrElse(fieldName, fieldName)
              val value = record.cellValue(cellName)
              if (value != None) {
                fields += Field(fieldName, value.toString)
              }
            }
          }
          val doc = new Document(fields.toSet)
          writer.write(doc)
          counter += 1
        }
      }
      if (optimize) writer.forceMerge(1)
    }
    finally{
      if (writer != null)
        writer.close()
    }

    val result = "success: " + counter  + " documents added."
    val dict = Dictionary(Seq(
      Record(Seq(
        Cell("result", result)
    ))))
    Some(dict)
  }
}
