package org.nlp4l.framework.actors

import org.nlp4l.framework.dao.JobDAO
import org.nlp4l.framework.dao.RunDAO
import org.nlp4l.framework.models.JobMessage
import org.nlp4l.framework.processors.ProcessorChain2

import akka.actor.Actor
import akka.actor.Props
import javax.inject.Inject


class ProcessorChain2Actor @Inject()(jobDAO: JobDAO, runDAO: RunDAO) extends Actor {
  override def receive: Receive = {
    case JobMessage(jobId) =>
      val dicAttr = ProcessorChain2.getDictionaryAttribute(jobDAO, jobId)
      val chain = ProcessorChain2.getChain(jobDAO, jobId)
      chain.process(jobDAO, runDAO, jobId, dicAttr)
  }
}

object ProcessorChain2Actor {
  def props = Props[ProcessorChain2Actor]
}

