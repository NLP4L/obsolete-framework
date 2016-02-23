package org.nlp4l.framework

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import org.apache.solr.client.solrj.impl.HttpSolrClient

class SolrModule extends AbstractModule {
  override def configure() = {
    bind(classOf[HttpSolrClient]).annotatedWith(Names.named("solr")).toInstance(new HttpSolrClient("http://localhost:8983/solr"))
  }
}
