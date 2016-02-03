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

package org.nlp4l.framework.controllers

import java.net.URLDecoder

import org.apache.solr.client.solrj.impl._
import org.apache.solr.client.solrj.request.QueryRequest
import org.apache.solr.common.params.ModifiableSolrParams
import play.api.mvc.Action
import play.api.mvc.Controller

class SearchResult extends Controller {

  def searchBySolr(url: String, collection: String, encodedQuery: String) = Action { request =>
    {
      val solr = new HttpSolrClient(url)
      val query = URLDecoder.decode(encodedQuery, "UTF-8")
      val params = new ModifiableSolrParams().add("q", query)
      val idField = request.getQueryString("id").getOrElse("id")
      val hlField = request.getQueryString("hl")
      if(hlField != None){
        params.add("hl", "on").add("hl.fl", hlField.get).
          add("hl.simple.pre", """<em class="lead">""").add("hl.simple.post", "</em>")
      }
      val req = new QueryRequest(params)
      val res = solr.query(collection, params)
      val hlRes = if(hlField != None) res.getHighlighting else null
      Ok(org.nlp4l.framework.views.html.solrSearchResult(query, res.getResults, idField, hlField, hlRes))
    }
  }
}
