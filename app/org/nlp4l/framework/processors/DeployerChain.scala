package org.nlp4l.framework.processors

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.asScalaSet
import scala.collection.mutable
import scala.concurrent.Await

import org.joda.time.DateTime
import org.nlp4l.framework.dao.JobDAO
import org.nlp4l.framework.models.Dictionary
import org.nlp4l.framework.models.Job

import com.typesafe.config.ConfigFactory

import play.api.Logger

class DeployerChain (val chain: List[Deployer]) {
  private val logger = Logger(this.getClass)
  
  def process(jobDAO: JobDAO, jobId: Int, runId: Int, dic: Dictionary): Seq[String] = {
    var errMsg: Seq[String] = Seq()
    def loop(li: List[Deployer], data:Option[Dictionary] = None): Unit = li match {
      case Nil => ()
      case head :: Nil =>
        val out: Tuple2[Boolean, Seq[String]] = head.deploy(data)
        if(!out._1) errMsg = errMsg union out._2
      case head :: tail =>
        val out: Tuple2[Boolean, Seq[String]] = head.deploy(data)
        if(!out._1) errMsg = errMsg union out._2
        loop(tail, data)
    }
    loop(chain, Some(dic))
    val job = Await.result(jobDAO.get(jobId), scala.concurrent.duration.Duration.Inf)
    jobDAO.update(Job(job.jobId, job.name, job.config, runId, job.lastRunAt, Some(new DateTime())))
    errMsg
  }
}

object DeployerChain {

  def getChain(jobDAO: JobDAO, jobId: Int): DeployerChain = {
    val job = Await.result(jobDAO.get(jobId), scala.concurrent.duration.Duration.Inf)
    new DeployerChainBuilder().build(job.config).result()
  }
}


class DeployerChainBuilder() {
  val logger = Logger(this.getClass)
  val buf = mutable.ArrayBuffer[Deployer]()

  def build(confStr: String): DeployerChainBuilder = {
    val config = ConfigFactory.parseString(confStr)

    val v = config.getConfigList("deployers")
    v.foreach {
      pConf =>
        try {
          val className = pConf.getString("class")
          val constructor = Class.forName(className).getConstructor(classOf[Map[String, String]])
          val settings = pConf.getConfig("settings").entrySet().map(f => f.getKey -> f.getValue.unwrapped()).toMap
          val facP = constructor.newInstance(settings).asInstanceOf[DeployerFactory]
          val p:Deployer = facP.getInstance()
          buf += p
        } catch {
          case e: Exception => logger.error(e.getMessage)
        }
    }
    this
  }

  def result() = new DeployerChain(buf.toList)
}
