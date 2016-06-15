package org.nlp4l.framework.builtin.ner

import com.typesafe.config.ConfigFactory
import org.nlp4l.framework.models.{Cell, CellType, DictionaryAttribute, Record}
import org.specs2.mutable.Specification

class OpenNLPNerExtractionProcessorSpec extends Specification {

  // skip test if model files are not ready.
  skipAllIf(!new java.io.File("/tmp/models/en-sent.bin").exists())

  "OpenNLPNerExtractionDictionaryAttributeFactory" should {
    "construct with setting" in {
      val config = ConfigFactory.parseString(
        """
          |{
          |  fields : ["docId", "category", "body_person"]
          |}
        """.stripMargin)
      val dict: DictionaryAttribute = new OpenNLPNerExtractionDictionaryAttributeFactory(config).getInstance()

      dict.cellAttributeList.length must_==(3)

      dict.getCellAttribute("docId") must_!= None
      dict.getCellAttribute("category") must_!= None
      dict.getCellAttribute("body_person") must_!= None

      dict.getCellAttribute("body_person").get.cellType must_== CellType.StringType
      dict.getCellAttribute("body_person").get.isFilterable must_== true
      dict.getCellAttribute("body_person").get.isSortable must_== true
    }
  }


  "OpenNLPNerRecordProcessor" should {
    "execute extraction" in {
      val config = ConfigFactory.parseString(
        """
          |{
          | sentModel: "/tmp/models/en-sent.bin"
          | tokenModel: "/tmp/models/en-token.bin"
          | nerModels: [
          |   "/tmp/models/en-ner-person.bin",
          |   "/tmp/models/en-ner-location.bin"
          |   ]
          | nerTypes: [
          |   "person",
          |   "location"
          |   ]
          | srcFields: [
          |   "body",
          |   "title"
          |   ]
          | idField: docId
          | passThruFields: [
          |   "category"
          |   ]
          |}
        """.stripMargin)

      val proc = new OpenNLPNerRecordProcessorFactory(config).getInstance()

      val record = Record(Seq(
          Cell("docId", "DOC-001"),
          Cell("category", "Sports"),
          Cell("title", "Mark mets Chris at Boston"),
          Cell("body", "Mr. Mark Warburton and Mr. Chris Heston are in Boston and Los Angeles."))
      )
        val result = proc.execute(Some(record))

        result.get.cellList.length must_==(6)

        result.get.cellList(0).name mustEqual "docId"
        result.get.cellList(0).value mustEqual "DOC-001"

        result.get.cellList(1).name mustEqual "body_person"
        result.get.cellList(1).value mustEqual "Mark Warburton,Chris Heston"

        result.get.cellList(2).name mustEqual "body_location"
        result.get.cellList(2).value mustEqual "Boston,Los Angeles"

        result.get.cellList(3).name mustEqual "title_person"
        result.get.cellList(3).value mustEqual "Mark,Chris"

        result.get.cellList(4).name mustEqual "title_location"
        result.get.cellList(4).value mustEqual "Boston"

        result.get.cellList(5).name mustEqual "category"
        result.get.cellList(5).value mustEqual "Sports"
      }
  }
  "OpenNLPNerRecordProcessor" should {
    "execute extraction, with separator, without passThru" in {
      val config = ConfigFactory.parseString(
        """
          |{
          | sentModel: "/tmp/models/en-sent.bin"
          | tokenModel: "/tmp/models/en-token.bin"
          | nerModels: [
          |   "/tmp/models/en-ner-person.bin"
          |   ]
          | nerTypes: [
          |   "person"
          |   ]
          | srcFields: [
          |   "body"
          |   ]
          | idField: "docId"
          | separator: "|"
          |}
        """.stripMargin)

      val proc = new OpenNLPNerRecordProcessorFactory(config).getInstance()

      val record = Record(Seq(
        Cell("docId", "DOC-001"),
        Cell("category", "Sports"),
        Cell("title", "Mark mets Chris at Boston"),
        Cell("body", "Mr. Mark Warburton and Mr. Chris Heston are in Boston and Los Angeles."))
      )
      val result = proc.execute(Some(record))

      result.get.cellList.length must_==(2)

      result.get.cellList(0).name mustEqual "docId"
      result.get.cellList(0).value mustEqual "DOC-001"

      result.get.cellList(1).name mustEqual "body_person"
      result.get.cellList(1).value mustEqual "Mark Warburton|Chris Heston"
    }
  }

}
