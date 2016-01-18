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

import java.io.FileNotFoundException

import org.nlp4l.framework.models.Dictionary
import org.specs2.mutable.Specification

class FeaturedProcessorsSpec extends Specification {

  "TextRecordsProcessor" should {
    "read solr.log file" in {
      val settings = Map("file" -> "test/resources/org/nlp4l/framework/builtin/solr.log", "encoding" -> "UTF-8")
      val processor = new TextRecordsProcessorFactory(settings).getInstance
      val result: Dictionary = processor.execute(None).get
      result must_!= None
      result.recordList.size must_==(188)
    }

    "throw a FileNotFoundException" in {
      val settings = Map("file" -> "test/resources/org/nlp4l/framework/builtin/no_such_file", "encoding" -> "UTF-8")
      val processor = new TextRecordsProcessorFactory(settings).getInstance
      processor.execute(None) must throwA[FileNotFoundException]
    }
  }
}
