package org.nlp4l.framework.builtin

import java.net.URLEncoder

import org.nlp4l.framework.models.CellType
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

class StandardSolrSearchSpec extends Specification with Mockito {

  val searchOn = "http://localhost:8983/solr"
  def cellAtt(separatedBy: String, hlField: String = null, queryField: String = null) =
    new StandardSolrSearchCellAttribute(searchOn, "collection1", "id", hlField, queryField, Some(separatedBy), "cell1", CellType.StringType, true, false)

  "StandardSolrSearchCellAttribute" should {
    "format cell value with a separator" in {
      val mockCell = mock[Any]
      mockCell.toString() returns "マック,マクド,マクドナルド"

      val expected =
        List(
          s"""<a href="/search/solr/${URLEncoder.encode(searchOn, "UTF-8")}/collection1/${URLEncoder.encode("マック", "UTF-8")}?id=id&qf=">マック</a>""",
          s"""<a href="/search/solr/${URLEncoder.encode(searchOn, "UTF-8")}/collection1/${URLEncoder.encode("マクド", "UTF-8")}?id=id&qf=">マクド</a>""",
          s"""<a href="/search/solr/${URLEncoder.encode(searchOn, "UTF-8")}/collection1/${URLEncoder.encode("マクドナルド", "UTF-8")}?id=id&qf=">マクドナルド</a>"""
        ).mkString(",")
      cellAtt(",").format(mockCell) mustEqual expected
    }

    "format cell value with multiple separators" in {
      val mockCell = mock[Any]
      mockCell.toString() returns "マック,マクド => マクドナルド"

      val expected =
        List(
          s"""<a href="/search/solr/${URLEncoder.encode(searchOn, "UTF-8")}/collection1/${URLEncoder.encode("マック", "UTF-8")}?id=id&qf=">マック</a>""",
          s"""<a href="/search/solr/${URLEncoder.encode(searchOn, "UTF-8")}/collection1/${URLEncoder.encode("マクド", "UTF-8")}?id=id&qf=">マクド</a>"""
        ).mkString(",") +
          " => " + s"""<a href="/search/solr/${URLEncoder.encode(searchOn, "UTF-8")}/collection1/${URLEncoder.encode("マクドナルド", "UTF-8")}?id=id&qf=">マクドナルド</a>"""
      cellAtt("(,)|(=>)").format(mockCell) mustEqual expected
    }

    "return empty string when cell value is empty" in {
      val mockCell = mock[Any]
      mockCell.toString() returns ""
      cellAtt(",").format(mockCell) mustEqual ""
    }

    "format cell value with highlight" in {
      val mockCell = mock[Any]
      mockCell.toString() returns "マック,マクド,マクドナルド"

      val expected =
        List(
          s"""<a href="/search/solr/${URLEncoder.encode(searchOn, "UTF-8")}/collection1/${URLEncoder.encode("マック", "UTF-8")}?id=id&qf=&hl=body">マック</a>""",
          s"""<a href="/search/solr/${URLEncoder.encode(searchOn, "UTF-8")}/collection1/${URLEncoder.encode("マクド", "UTF-8")}?id=id&qf=&hl=body">マクド</a>""",
          s"""<a href="/search/solr/${URLEncoder.encode(searchOn, "UTF-8")}/collection1/${URLEncoder.encode("マクドナルド", "UTF-8")}?id=id&qf=&hl=body">マクドナルド</a>"""
        ).mkString(",")
      cellAtt(",", "body").format(mockCell) mustEqual expected
    }

    "format cell avlue with query field" in {
      val mockCell = mock[Any]
      mockCell.toString() returns "マック,マクド,マクドナルド"

      val expected =
        List(
          s"""<a href="/search/solr/${URLEncoder.encode(searchOn, "UTF-8")}/collection1/${URLEncoder.encode("マック", "UTF-8")}?id=id&qf=title body">マック</a>""",
          s"""<a href="/search/solr/${URLEncoder.encode(searchOn, "UTF-8")}/collection1/${URLEncoder.encode("マクド", "UTF-8")}?id=id&qf=title body">マクド</a>""",
          s"""<a href="/search/solr/${URLEncoder.encode(searchOn, "UTF-8")}/collection1/${URLEncoder.encode("マクドナルド", "UTF-8")}?id=id&qf=title body">マクドナルド</a>"""
        ).mkString(",")
      cellAtt(",", queryField = "title body").format(mockCell) mustEqual expected
    }

  }
}
