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

package org.nlp4l.framework.builtin.acronym

import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification

import scala.collection.mutable.ArrayBuffer

class AcronymExtractionProcessorSpec extends Specification {

  "AcronymExtractionProcessor" should {

    val settings = ConfigFactory.parseString(
      """
        |{
        |  textField: "text"
        |  algorithm: simpleCanonical
        |}
      """.stripMargin)
    val processor = new AcronymExtractionProcessorFactory(settings).getInstance.asInstanceOf[AcronymExtractionProcessor]

    "stackWords short" in {
      processor.stackWords("1111  2222   333", false) must_== Array("1111", "2222", "333")
      processor.stackWords("1111  2222   333", true) must_== Array("333", "2222", "1111")
    }

    "stackWords long" in {
      processor.stackWords("1  2   3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23", false) must_==
        Array("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20")
      processor.stackWords("1  2   3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23", true) must_==
        Array("23", "22", "21", "20", "19", "18", "17", "16", "15", "14", "13", "12", "11", "10", "9", "8", "7", "6", "5", "4")
    }

    "testUpperCaseStrict" in {
      val ACRONYM = "JAIST"
      processor.testUpperCaseStrict(ACRONYM, ACRONYM.toCharArray, Array("Japan", "Advanced", "Institute", "of", "Science", "and", "Technology"),
        false, false, ArrayBuffer.empty[String]) must_== Some("Japan Advanced Institute of Science and Technology")
      processor.testUpperCaseStrict(ACRONYM, ACRONYM.toCharArray, Array("Japan", "Advanced", "Institute", "Of", "Science", "And", "Technology"),
        false, false, ArrayBuffer.empty[String]) must_== Some("Japan Advanced Institute Of Science And Technology")
      processor.testUpperCaseStrict(ACRONYM, ACRONYM.toCharArray, Array("Japan", "advanced", "Institute", "of", "Science", "and", "Technology"),
        false, false, ArrayBuffer.empty[String]) must_== None
    }

    "testUpperCaseStrict reverse" in {
      val ACRONYM = "JAIST"
      processor.testUpperCaseStrict(ACRONYM, ACRONYM.toCharArray.reverse, Array("Japan", "Advanced", "Institute", "of", "Science", "and", "Technology").reverse,
        true, false, ArrayBuffer.empty[String]) must_== Some("Japan Advanced Institute of Science and Technology")
      processor.testUpperCaseStrict(ACRONYM, ACRONYM.toCharArray.reverse, Array("Japan", "Advanced", "Institute", "Of", "Science", "And", "Technology").reverse,
        true, false, ArrayBuffer.empty[String]) must_== Some("Japan Advanced Institute Of Science And Technology")
      processor.testUpperCaseStrict(ACRONYM, ACRONYM.toCharArray.reverse, Array("Japan", "advanced", "Institute", "of", "Science", "and", "Technology").reverse,
        true, false, ArrayBuffer.empty[String]) must_== None
    }

    "testUpperCaseStrict looseEnd" in {
      val ACRONYM = "JAIST"
      processor.testUpperCaseStrict(ACRONYM, ACRONYM.toCharArray, Array("Japan", "Advanced", "Institute", "of", "Science", "and", "Technology", "in", "Ishikawa"),
        false, true, ArrayBuffer.empty[String]) must_== Some("Japan Advanced Institute of Science and Technology")
      processor.testUpperCaseStrict(ACRONYM, ACRONYM.toCharArray, Array("Japan", "Advanced", "Institute", "Of", "Science", "And", "Technology", "In", "Ishikawa"),
        false, true, ArrayBuffer.empty[String]) must_== Some("Japan Advanced Institute Of Science And Technology")
      processor.testUpperCaseStrict(ACRONYM, ACRONYM.toCharArray, Array("Japan", "advanced", "Institute", "of", "Science", "and", "Technology", "in", "Ishikawa"),
        false, true, ArrayBuffer.empty[String]) must_== None
    }

    "testUpperCaseStrict reverse looseEnd" in {
      val ACRONYM = "JAIST"
      processor.testUpperCaseStrict(ACRONYM, ACRONYM.toCharArray.reverse, Array("Japan", "Advanced", "Institute", "of", "Science", "and", "Technology", "in", "Ishikawa").reverse,
        true, true, ArrayBuffer.empty[String]) must_== Some("Japan Advanced Institute of Science and Technology in Ishikawa")
      processor.testUpperCaseStrict(ACRONYM, ACRONYM.toCharArray.reverse, Array("Japan", "Advanced", "Institute", "Of", "Science", "And", "Technology", "In", "Ishikawa").reverse,
        true, true, ArrayBuffer.empty[String]) must_== Some("Japan Advanced Institute Of Science And Technology In Ishikawa")
      processor.testUpperCaseStrict(ACRONYM, ACRONYM.toCharArray.reverse, Array("Japan", "advanced", "Institute", "of", "Science", "and", "Technology", "in", "Ishikawa").reverse,
        true, true, ArrayBuffer.empty[String]) must_== None
    }

    "testLowerCaseStrict" in {
      val ACRONYM = "JAIST"
      processor.testLowerCaseStrict(ACRONYM, ACRONYM.toCharArray, Array("japan", "advanced", "institute", "of", "science", "and", "technology"),
        false, false, ArrayBuffer.empty[String]) must_== Some("japan advanced institute of science and technology")
      processor.testLowerCaseStrict(ACRONYM, ACRONYM.toCharArray, Array("Japan", "advanced", "Institute", "of", "Science", "and", "Technology"),
        false, false, ArrayBuffer.empty[String]) must_== None
    }

    "testLowerCaseStrict reverse" in {
      val ACRONYM = "JAIST"
      processor.testLowerCaseStrict(ACRONYM, ACRONYM.toCharArray.reverse, Array("japan", "advanced", "institute", "of", "science", "and", "technology").reverse,
        true, false, ArrayBuffer.empty[String]) must_== Some("japan advanced institute of science and technology")
      processor.testLowerCaseStrict(ACRONYM, ACRONYM.toCharArray.reverse, Array("Japan", "advanced", "Institute", "of", "Science", "and", "Technology").reverse,
        true, false, ArrayBuffer.empty[String]) must_== None
    }

    "testLowerCaseStrict looseEnd" in {
      val ACRONYM = "JAIST"
      processor.testLowerCaseStrict(ACRONYM, ACRONYM.toCharArray, Array("japan", "advanced", "institute", "of", "science", "and", "technology", "in", "Ishikawa"),
        false, true, ArrayBuffer.empty[String]) must_== Some("japan advanced institute of science and technology")
      processor.testLowerCaseStrict(ACRONYM, ACRONYM.toCharArray, Array("Japan", "advanced", "Institute", "of", "Science", "and", "Technology", "in", "Ishikawa"),
        false, true, ArrayBuffer.empty[String]) must_== None
    }

    "testLowerCaseStrict reverse looseEnd" in {
      val ACRONYM = "JAIST"
      processor.testLowerCaseStrict(ACRONYM, ACRONYM.toCharArray.reverse, Array("japan", "advanced", "institute", "of", "science", "and", "technology", "in", "Ishikawa").reverse,
        true, true, ArrayBuffer.empty[String]) must_== Some("japan advanced institute of science and technology in Ishikawa")
      processor.testLowerCaseStrict(ACRONYM, ACRONYM.toCharArray.reverse, Array("Japan", "advanced", "Institute", "of", "Science", "and", "Technology", "in", "Ishikawa").reverse,
        true, true, ArrayBuffer.empty[String]) must_== None
    }
  }
}
