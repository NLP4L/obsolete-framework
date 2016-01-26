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

import org.nlp4l.framework.models.Dictionary
import org.nlp4l.framework.models.DictionaryAttribute
import org.nlp4l.framework.models.Record


abstract class DictionaryAttributeFactory(settings: Map[String, String]) extends ConfiguredFactory(settings){
  def getInstance(): DictionaryAttribute
}

abstract class ConfiguredFactory(val settings: Map[String, Any]){
  def getStrParamRequired(name: String): String = {
    settings.apply(name).toString
  }
  def getIntParam(name: String, default: Int): Int = {
    // this is implemented by using cast???
    //val value = settings.getOrElse(name, default.toString)
    settings.get(name) match {
      case None => default
      case Some(v: Int) => v
      case a => a.get.toString.toInt
    }
  }
  def getIntParamRequired(name: String): Int = {
    settings.apply(name).toString.toInt
  }
  def getLongParam(name: String, default: Long): Long = {
    settings.get(name) match {
      case None => default
      case Some(v: Int) => v.toLong
      case Some(v: Long) => v
      case a => a.get.toString.toLong
    }
  }
  def getLongParamRequired(name: String): Long = {
    settings.apply(name).toString.toLong
  }
  def getFloatParam(name: String, default: Float): Float = {
    settings.get(name) match {
      case None => default
      case Some(v: String) => v.toFloat
      case a => a.get.toString.toFloat
    }
  }
  def getFloatParamRequired(name: String): Float = {
    settings.apply(name).toString.toFloat
  }
  def getBoolParam(name: String, default: Boolean): Boolean = {
    settings.get(name) match {
      case None => default
      case a => a.get.toString.toBoolean
    }
  }
  def getBoolParamRequired(name: String): Boolean = {
    settings.apply(name).toString.toBoolean
  }
}

abstract class ProcessorFactory(settings: Map[String, String]) extends ConfiguredFactory(settings){
  def getInstance(): Processor
}

abstract class RecordProcessorFactory(settings: Map[String, String]) extends ConfiguredFactory(settings){
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


/**
 * Validate processor
 */
abstract class ValidatorFactory(settings: Map[String, String]) extends ConfiguredFactory(settings){
  def getInstance(): Validator
}
trait Validator {
  def validate (data: Option[Dictionary]): Tuple2[Boolean, Seq[String]]
}


/**
 * Deploy processor
 */
abstract class DeployerFactory(settings: Map[String, String]) extends ConfiguredFactory(settings){
  def getInstance(): Deployer
}
trait Deployer {
  def deploy (data: Option[Dictionary]): Tuple2[Boolean, Seq[String]]
}
