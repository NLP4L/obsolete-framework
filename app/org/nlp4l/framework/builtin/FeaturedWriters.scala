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

import com.typesafe.config.Config
import org.nlp4l.framework.models.Dictionary
import org.nlp4l.framework.processors.{Writer, WriterFactory}

class CSVFileWriterFactory(settings: Config) extends WriterFactory(settings) {
  override def getInstance: Writer = {
    new CSVFileWriter(getStrParam("separator", ","))
  }
}

class CSVFileWriter(separator: String) extends Writer {
  override def write (data: Option[Dictionary]): Tuple3[Boolean, Seq[String], Seq[String]] = {
    data match {
      case Some(dic) => {
        val result = dic.recordList.map(r => r.mkCsvRecord(separator))
        (true, Seq(), result)
      }
      case None => (false, Seq("no data to be deployed found"), Seq())
    }
  }
}
