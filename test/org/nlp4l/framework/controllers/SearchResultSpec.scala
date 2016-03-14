package org.nlp4l.framework.controllers

import java.net.URLEncoder
import java.util

import org.apache.solr.common.params.{SolrParams, ModifiableSolrParams}
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.{SolrDocument, SolrDocumentList}
import org.mockito.ArgumentCaptor
import org.scalatestplus.play.{PlaySpec, OneAppPerSuite}
import org.specs2.mock.Mockito
import org.mockito.Mockito._
import play.api.mvc.Results
import play.api.test.FakeRequest

class SearchResultSpec extends PlaySpec with Mockito with Results with OneAppPerSuite {
  "Search result page" should {
    "search solr with specified query" in {
      val mockSolr = mock[HttpSolrClient]
      val res = mock[QueryResponse]
      val docs = mock[SolrDocumentList]
      docs.getNumFound() returns 10
      docs.size() returns 0
      docs.iterator() returns new util.ArrayList[SolrDocument]().iterator()
      res.getResults() returns docs
      mockSolr.query(anyString, any) returns res

      val controller = new SearchResult(mockSolr)
      val request = FakeRequest("GET", s"/search/solr/${URLEncoder.encode("http://localhost:8080/solr", "UTF-8")}/collection1/aaa?id=id&qf=&hl=body")
      val result = controller.searchBySolr("http://localhost:8080/solr", "collection1", "aaa").apply(request)
      result must not be null

      // check method call for solr client
      val arg0 = ArgumentCaptor.forClass(classOf[String])
      verify(mockSolr).setBaseURL(arg0.capture())
      arg0.getValue mustEqual "http://localhost:8080/solr"

      val arg1 = ArgumentCaptor.forClass(classOf[String])
      val arg2 = ArgumentCaptor.forClass(classOf[SolrParams])
      verify(mockSolr).query(arg1.capture(), arg2.capture())
      arg1.getValue mustEqual  "collection1"
      arg2.getValue.get("q") mustEqual "aaa"
      arg2.getValue.get("hl") mustEqual "on"
      arg2.getValue.get("hl.fl") mustEqual "body"
    }

    "search solr with dismax query" in {
      val mockSolr = mock[HttpSolrClient]
      val res = mock[QueryResponse]
      val docs = mock[SolrDocumentList]
      docs.getNumFound() returns 10
      docs.size() returns 0
      docs.iterator() returns new util.ArrayList[SolrDocument]().iterator()
      res.getResults() returns docs
      mockSolr.query(anyString, any) returns res

      val controller = new SearchResult(mockSolr)
      val request = FakeRequest("GET", s"/search/solr/${URLEncoder.encode("http://localhost:8080/solr", "UTF-8")}/collection1/aaa?id=id&qf=title body desc")
      val result = controller.searchBySolr("http://localhost:8080/solr", "collection1", "aaa").apply(request)
      result must not be null

      // check method call for solr client
      val arg0 = ArgumentCaptor.forClass(classOf[String])
      verify(mockSolr).setBaseURL(arg0.capture())
      arg0.getValue mustEqual "http://localhost:8080/solr"

      val arg1 = ArgumentCaptor.forClass(classOf[String])
      val arg2 = ArgumentCaptor.forClass(classOf[SolrParams])
      verify(mockSolr).query(arg1.capture(), arg2.capture())
      arg1.getValue mustEqual  "collection1"
      arg2.getValue.get("q") mustEqual "aaa"
      arg2.getValue.get("defType") mustEqual "dismax"
      arg2.getValue.get("qf") mustEqual "title body desc"
    }
  }
}
