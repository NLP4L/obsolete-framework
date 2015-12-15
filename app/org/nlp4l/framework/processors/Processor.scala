package org.nlp4l.framework.processors

import scala.concurrent.Await

import org.nlp4l.framework.dao.JobDAO
import org.nlp4l.framework.dao.RunDAO
import org.nlp4l.framework.models.Dictionary
import org.nlp4l.framework.models.DictionaryAttribute
import org.nlp4l.framework.models.Record

import play.api.Logger


abstract class DictionaryAttributeFactory(val settings: Map[String, String]) {
  def getInstance(): DictionaryAttribute
}

abstract class ProcessorFactory(val settings: Map[String, String]) {
  def getInstance(): Processor
}

abstract class RecordProcessorFactory(val settings: Map[String, String]) {
  def getInstance(): RecordProcessor
}

trait Processor {
  def execute(data: Option[Dictionary]) : Option[Dictionary] = {
    data
  }
}

trait RecordProcessor {
  def execute(data: Option[Record]): Option[Record] = {
    data
  }
}

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
        Await.result(runDAO.createTable(jobId, tmpRunId, dicAttr), scala.concurrent.duration.Duration.Inf)
        runDAO.insertData(jobId, tmpRunId, dicAttr, d)
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


/**
 * Validate processor
 */
abstract class ValidatorFactory(val settings: Map[String, String]) {
  def getInstance(): Validator
}
trait Validator {
  def validate (data: Option[Dictionary]): Tuple2[Boolean, Seq[String]]
}


/**
 * Deploy processor
 */
abstract class DeployerFactory(val settings: Map[String, String]) {
  def getInstance(): Deployer
}
trait Deployer {
  def deploy (data: Option[Dictionary]): Tuple2[Boolean, Seq[String]]
}

