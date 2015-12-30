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

package org.nlp4l.framework.builtin

import scala.concurrent.Await
import scala.util.{ Try, Success, Failure }
import org.nlp4l.framework.dao.JobDAO
import org.nlp4l.framework.dao.RunDAO
import org.nlp4l.framework.models.Dictionary
import org.nlp4l.framework.models.DictionaryAttribute
import org.nlp4l.framework.models.Record
import play.api.Logger
import org.nlp4l.framework.processors.RecordProcessor
import org.nlp4l.framework.processors.ProcessorFactory
import org.nlp4l.framework.processors.Processor



class WrapProcessor(val childList: Seq[RecordProcessor]) extends Processor {
  override def execute(data: Option[Dictionary]): Option[Dictionary] = {
    var reclist: Seq[Record] = Seq()
    data map { dic =>
      dic.recordList foreach { rec: Record =>
        var rec2:Option[Record] = Some(rec)
        childList foreach { recProc: RecordProcessor =>
          rec2 = recProc.execute(rec2)
        }
        reclist = reclist :+ rec2.getOrElse(rec)
      }
    }
    Some(Dictionary(reclist))
  }
}


/**
 * Sort processor factory
 */
class SortProcessorFactory(settings: Map[String, String]) extends ProcessorFactory(settings) {
  override def getInstance: Processor = {
    new SortProcessor(settings.get("cellname"), settings.get("order"))
  }
}

/**
 * Sort Processor
 * 
 * @param key Sort key name
 * @param order Sort order, "desc", "asc"
 */
final class SortProcessor(val key: Option[String], val order: Option[String]) extends Processor {
  private val logger = Logger(this.getClass)
  
  def sort(jobDAO: JobDAO, runDAO: RunDAO, jobId: Int, runId: Int, dicAttr: DictionaryAttribute, dic: Option[Dictionary]): Option[Dictionary] = {
    var out:Option[Dictionary] = dic
    val tmpRunId: Int = runId + 1000000
    
      dic map { d => {
        val f1 = runDAO.createTable(jobId, tmpRunId, dicAttr)
        Await.ready(f1, scala.concurrent.duration.Duration.Inf)
        f1.value.get match {
          case Success(n) => runDAO.insertData(jobId, tmpRunId, dicAttr, d)
          case Failure(ex) => logger.error(ex.getMessage)
        }
        var newout:Dictionary = runDAO.fetchAll(jobId, tmpRunId, key.getOrElse("id"), order.getOrElse("asc"))
        out = Some(newout)
        runDAO.dropTable(jobId, tmpRunId)
      }
    }
    out
  }
}


/**
 * Merge processor factory
 */
class MergeProcessorFactory(settings: Map[String, String]) extends ProcessorFactory(settings) {
  override def getInstance: Processor = {
    new MergeProcessor(settings.get("cellname").getOrElse(""), settings.get("glue").getOrElse(""))
  }
}

/**
 * Sort Processor
 * 
 * @param key Merge key name
 * @param glue string to concatenate
 */
final class MergeProcessor(val key: String, val glue: String) extends Processor {
  private val logger = Logger(this.getClass)
  
  def merge(dicAttr: DictionaryAttribute, dic: Option[Dictionary]): Option[Dictionary] = {
    var out:Option[Dictionary] = dic
    
    dic map { d =>
      var reclist: Seq[Record] = Seq()
      var prevRecord: Record = null
      d.recordList foreach {rec: Record =>
        if(prevRecord != null && rec.canMerge(key, prevRecord)) {
          reclist = reclist.init
          val merged = rec.merge(key, glue, prevRecord)
          reclist = reclist :+ merged
          prevRecord = merged
        } else {
          reclist = reclist :+ rec
          prevRecord = rec
        }
      }
      out = Some(Dictionary(reclist))
    }
    out
  }
}


/**
 * Replay processor
 * This class is to mark that the processors must apply the replay data to dictionary
 */
class ReplayProcessorFactory(settings: Map[String, String]) extends ProcessorFactory(settings) {
  override def getInstance: Processor = {
    new ReplayProcessor()
  }
}

final class ReplayProcessor extends Processor {
  def replay(jobDAO: JobDAO, runDAO: RunDAO, jobId: Int, dicAttr: DictionaryAttribute, dic: Option[Dictionary]): Option[Dictionary] = {
    var recordList: Seq[Record] = Seq()
    dic map { d =>
      d.recordList foreach { r: Record =>
        val hashcode: Int = r.hashCode
        if(dicAttr.modifiedRecordList.contains(hashcode)) {
          dicAttr.modifiedRecordList.get(hashcode) map { modr =>
            recordList = recordList :+ modr
          }
        } else if(!dicAttr.deletedRecordList.contains(hashcode)) {
          recordList = recordList :+ r
        }
      }
      dicAttr.addedRecordList foreach { r =>
        recordList = recordList :+ r._2
      }
      Dictionary(recordList)
    }
  }
}

