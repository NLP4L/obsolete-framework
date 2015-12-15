package org.nlp4l.framework.actors.modules

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

import org.nlp4l.framework.actors.ProcessorChain2Actor

class FrameworkModule extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {
    bindActor[ProcessorChain2Actor]("processor-actor2")
  }
}
