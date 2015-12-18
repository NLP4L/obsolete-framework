/*
 * Copyright 2015 org.NLP4L
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

package org.nlp4l.framework.actors

import org.nlp4l.framework.dao.JobDAO
import org.nlp4l.framework.dao.RunDAO
import org.nlp4l.framework.processors.ProcessorChain2
import akka.actor.Actor
import akka.actor.Props
import javax.inject.Inject
import org.nlp4l.framework.builtin.JobMessage


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

