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

package org.nlp4l.framework.processors

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.asScalaSet
import scala.collection.mutable
import scala.concurrent.Await
import scala.collection.convert.WrapAsScala._
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigParseOptions
import com.typesafe.config.ConfigSyntax
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.joda.time.DateTime
import org.nlp4l.framework.dao.JobDAO
import org.nlp4l.framework.dao.RunDAO
import org.nlp4l.framework.models.Dictionary
import org.nlp4l.framework.models.DictionaryAttribute
import org.nlp4l.framework.models.Record
import org.nlp4l.framework.builtin.ReplayProcessor
import org.nlp4l.framework.builtin.WrapProcessor
import org.nlp4l.framework.builtin.SortProcessor
import org.nlp4l.framework.builtin.MergeProcessor
import org.nlp4l.framework.builtin.Constants
import org.nlp4l.framework.builtin.JobStatus
import org.nlp4l.framework.builtin.Job


class ProcessorChain2 (val chain: List[Processor]) {
  private val logger = Logger(this.getClass)

  def process(jobDAO: JobDAO, runDAO: RunDAO, jobId: Int, dicAttr: DictionaryAttribute) = {
    val job = Await.result(jobDAO.get(jobId), scala.concurrent.duration.Duration.Inf)
    val runId = job.lastRunId + 1
    jobDAO.update(Job(job.jobId, job.name, job.config, runId, Some(new DateTime()), job.lastDeployAt))
    def loop(li: List[Processor], js: JobStatus, data:Option[Dictionary] = None): Unit = li match {
      case Nil => ()
      case head :: Nil =>
        var out: Option[Dictionary] = head.execute(data)
        val cname = head.asInstanceOf[AnyRef].getClass.getName
        if(cname == Constants.SORTPROCESSOR_CLASS) {
          out = head.asInstanceOf[SortProcessor].sort(jobDAO, runDAO, jobId, runId, dicAttr, out)
        } else if(cname == Constants.REPLAYPROCESSOR_CLASS) {
          out = head.asInstanceOf[ReplayProcessor].replay(jobDAO, runDAO, jobId, dicAttr, out)
        } else if(cname == Constants.MERGEPROCESSOR_CLASS) {
          out = head.asInstanceOf[MergeProcessor].merge(dicAttr, out)
        }
        runDAO.updateJobStatus(JobStatus(js.id, js.jobId, js.runId, js.total, js.total-li.size+1))
        ProcessorChain2.outputResult(jobDAO, runDAO, jobId, runId, dicAttr, out)
      case head :: tail =>
        var out:Option[Dictionary]  = head.execute(data)
        val cname = head.asInstanceOf[AnyRef].getClass.getName
        if(cname == Constants.SORTPROCESSOR_CLASS) {
          out = head.asInstanceOf[SortProcessor].sort(jobDAO, runDAO, jobId, runId, dicAttr, out)
        } else if(cname == Constants.REPLAYPROCESSOR_CLASS) {
          out = head.asInstanceOf[ReplayProcessor].replay(jobDAO, runDAO, jobId, dicAttr, out)
        } else if(cname == Constants.MERGEPROCESSOR_CLASS) {
          out = head.asInstanceOf[MergeProcessor].merge(dicAttr, out)
        }
        val newjs = JobStatus(js.id, js.jobId, js.runId, js.total, js.total-li.size+1)
        runDAO.updateJobStatus(newjs)
        loop(tail, newjs, out)
    }
    val js = JobStatus(None, jobId, runId, chain.size, 0)
    runDAO.insertJobStatus(js) map {newjs =>
          loop(chain, newjs)
    }
  }
}

object ProcessorChain2 {
  // Processor
  private var mapP: Map[Int, ProcessorChain2] = Map()
  def chainMap: Map[Int, ProcessorChain2] = mapP
  
  // DictionaryAttribute
  private var mapD: Map[Int, DictionaryAttribute] = Map()
  def dicMap: Map[Int, DictionaryAttribute] = mapD
  
  def loadChain(jobDAO: JobDAO, jobId: Int): Unit = {
    jobDAO.get(jobId).map(
        job => {
          val pcb = new ProcessorChain2Builder()
           mapP += (jobId -> pcb.procBuild(jobId, job.config).result())
           var dicAttr = pcb.dicBuild(job.config)

           // Replay data
           var addedRecordList: Map[Int, Record] = Map()
           var modifiedRecordList: Map[Int, Record] = Map()
           val aa = jobDAO.fetchReplayOfAdd(jobId) 
           jobDAO.fetchReplayOfAdd(jobId) foreach { hd: (Int, Int) =>
             val runId: Int = hd._1
             val hashcode: Int = hd._2
             jobDAO.fetchRecordByHashcode(jobId, runId, hashcode) map { rec: Record =>
               addedRecordList += (hashcode -> rec)
             }
           }
           val modifiedList: List[(Int, Int, Int)] = jobDAO.fetchReplayOfMod(jobId)
           jobDAO.fetchReplayOfMod(jobId) foreach { hd: (Int, Int, Int) =>
             val runId: Int = hd._1
             val hashcode: Int = hd._2
             val modToHashcode: Int = hd._3
             jobDAO.fetchRecordByHashcode(jobId, runId, modToHashcode) map { rec: Record =>
               modifiedRecordList += (hashcode -> rec)
             }
           }
           dicAttr.addedRecordList = addedRecordList
           dicAttr.modifiedRecordList = modifiedRecordList
           dicAttr.deletedRecordList = jobDAO.fetchReplayOfDel(jobId)

           mapD += (jobId -> dicAttr)
        }
    )
  }
  
  
  def getChain(jobDAO: JobDAO, jobId: Int): ProcessorChain2 = {
    val job = Await.result(jobDAO.get(jobId), scala.concurrent.duration.Duration.Inf)
    new ProcessorChain2Builder().procBuild(jobId, job.config).result()
  }
  
  def getDictionaryAttribute(jobDAO: JobDAO, jobId: Int): DictionaryAttribute = {
    val job = Await.result(jobDAO.get(jobId), scala.concurrent.duration.Duration.Inf)
    val pcb = new ProcessorChain2Builder()
    var dicAttr = pcb.dicBuild(job.config)

     // Replay data
     var addedRecordList: Map[Int, Record] = Map()
     var modifiedRecordList: Map[Int, Record] = Map()
     jobDAO.fetchReplayOfAdd(jobId) foreach { hd: (Int, Int) =>
       val runId: Int = hd._1
       val hashcode: Int = hd._2
       jobDAO.fetchRecordByHashcode(jobId, runId, hashcode) map { rec: Record =>
         
         println("addedRecordList")
         println("result  hashcode="+rec.hashCode)
         println(rec)
         
         addedRecordList += (hashcode -> rec)
       }
     }
     val modifiedList: List[(Int, Int, Int)] = jobDAO.fetchReplayOfMod(jobId)
     jobDAO.fetchReplayOfMod(jobId) foreach { hd: (Int, Int, Int) =>
       val runId: Int = hd._1
       val hashcode: Int = hd._2
       val modToHashcode: Int = hd._3
       jobDAO.fetchRecordByHashcode(jobId, runId, modToHashcode) map { rec: Record =>
         modifiedRecordList += (hashcode -> rec)
       }
     }
     dicAttr.addedRecordList = addedRecordList
     dicAttr.modifiedRecordList = modifiedRecordList

     dicAttr
  }
  
  /**
   * Save the Dictionary to database
   */
  def outputResult(jobDAO: JobDAO, runDAO: RunDAO, jobId: Int, runId: Int, dicAttr: DictionaryAttribute, dic: Option[Dictionary]): Unit = {
    jobDAO.get(jobId) map {job: Job =>
      dic map { d =>
        runDAO.dropTable(jobId, runId)
        runDAO.createTable(jobId, runId, dicAttr) map {n =>
          runDAO.insertData(jobId, runId, dicAttr, d)
        }
      }
    }
  }

  /**
   * Validate the uploaded job config file
   */
  def validateConf(confStr: String): Boolean = {
    try {
      val config = ConfigFactory.parseString(confStr, ConfigParseOptions.defaults().setSyntax(ConfigSyntax.CONF))
      if (!config.hasPath("dictionary") || !config.hasPath("processors") || !config.hasPath("deployers")) false
      else {
        val b1 = config.getConfigList("dictionary").toList.forall {
          pConf => pConf.hasPath("class")
        }
        val b2 = config.getConfigList("processors").toList.forall {
          pConf => pConf.hasPath("class")
        }
        val b3 = config.getConfigList("deployers").toList.forall {
          pConf => pConf.hasPath("class")
        }
        val b4 =
          if (!config.hasPath("validators")) true
          else {
            config.getConfigList("validators").toList.forall {
              pConf => pConf.hasPath("class")
            }
          }
        b1 && b2 && b3 && b4
      }
    } catch {
      case e: Exception => {
        println(e.getMessage)
        false
      }
    }
  }
}

class ProcessorChain2Builder() {
  val logger = Logger(this.getClass)
  val buf = mutable.ArrayBuffer[Processor]()

  def procBuild(jobId: Int, confStr: String): ProcessorChain2Builder = {
    val config = ConfigFactory.parseString(confStr)

    config.getConfigList("processors").foreach {
      pConf =>
        try {
          val className = pConf.getString("class")
          if(className == Constants.WRAPPROCESSOR_CLASS) {
            buf += wrapBuild(pConf)
          } else {
            val constructor = Class.forName(className).getConstructor(classOf[Map[String, String]])
            val settings = pConf.getConfig("settings").entrySet().map(f => f.getKey -> f.getValue.unwrapped()).toMap
            val facP = constructor.newInstance(settings).asInstanceOf[ProcessorFactory]
            val p = facP.getInstance()
            buf += p
          }
        } catch {
          case e: Exception => logger.error(e.getMessage)
        }
    }
    this
  }
  
  def dicBuild(confStr: String): DictionaryAttribute = {
    val config = ConfigFactory.parseString(confStr)
    val pConf = config.getConfigList("dictionary").get(0)
    val className = pConf.getString("class")
    val constructor = Class.forName(className).getConstructor(classOf[Map[String, String]])
    val settings = pConf.getConfig("settings").entrySet().map(f => f.getKey -> f.getValue.unwrapped()).toMap
    val facP = constructor.newInstance(settings).asInstanceOf[DictionaryAttributeFactory]
    facP.getInstance()
  }
  
  def wrapBuild(wrapConf: Config): Processor = {
    var buf: Seq[RecordProcessor] = Seq()
    val pConf = wrapConf.getConfigList("recordProcessors").get(0)
    
    try {
      val className = pConf.getString("class")
      val constructor = Class.forName(className).getConstructor(classOf[Map[String, String]])
      val settings = pConf.getConfig("settings").entrySet().map(f => f.getKey -> f.getValue.unwrapped()).toMap
      val facP = constructor.newInstance(settings).asInstanceOf[RecordProcessorFactory]
      val p = facP.getInstance()
      buf = buf :+ p
    } catch {
      case e: Exception => logger.error(e.getMessage)
    }
 
    val className = Constants.WRAPPROCESSOR_CLASS
    val constructor = Class.forName(className).getConstructor(classOf[Seq[RecordProcessor]])
    constructor.newInstance(buf).asInstanceOf[WrapProcessor]
  }

  def result() = new ProcessorChain2(buf.toList)
}
