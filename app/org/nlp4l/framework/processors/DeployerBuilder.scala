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

package org.nlp4l.framework.processors

import com.typesafe.config.ConfigFactory
import org.nlp4l.framework.dao.JobDAO
import play.api.Logger

import scala.collection.convert.WrapAsScala._
import scala.concurrent.Await


object DeployerBuilder {

  val logger = Logger(this.getClass)

  def build(jobDAO: JobDAO, jobId: Int): Deployer = {
    val job = Await.result(jobDAO.get(jobId), scala.concurrent.duration.Duration.Inf)
    build(job.config)
  }

  def build(cfg: String): Deployer = {
    val config = ConfigFactory.parseString(cfg)

    val gSettings: Map[String, Object] =
      if(config.hasPath("settings")) {
        config.getConfig("settings").entrySet().map(f => f.getKey -> f.getValue.unwrapped()).toMap
      }
      else Map()

    val pConf = config.getConfig("deployer")
    try {
      val className = pConf.getString("class")
      val constructor = Class.forName(className).getConstructor(classOf[Map[String, String]])
      var lSettings: Map[String, Object] = Map()
      if(pConf.hasPath("settings")) {
        lSettings = pConf.getConfig("settings").entrySet().map(f => f.getKey -> f.getValue.unwrapped()).toMap
      }
      val settings = gSettings ++ lSettings
      val facP = constructor.newInstance(settings).asInstanceOf[DeployerFactory]
      facP.getInstance()
    } catch {
      case e: Exception => {
        logger.error(e.getMessage)
        throw e
      }
    }
  }
}
