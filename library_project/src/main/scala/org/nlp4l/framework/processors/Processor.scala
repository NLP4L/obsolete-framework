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


abstract class DictionaryAttributeFactory(val settings: Map[String, String]) {
  def getInstance(): DictionaryAttribute
}

abstract class ConfiguredFactory(val settings: Map[String, String]){
  def getStrParamRequired(name: String): String = {
    settings.apply(name)
  }
  def getIntParam(name: String, default: Int): Int = {
    val value = settings.getOrElse(name, default.toString)
    value.toInt
  }
  def getIntParamRequired(name: String): Int = {
    settings.apply(name).toInt
  }
  def getFloatParam(name: String, default: Float): Float = {
    val value = settings.getOrElse(name, default.toString)
    value.toFloat
  }
  def getFloatParamRequired(name: String): Float = {
    settings.apply(name).toFloat
  }
}

abstract class ProcessorFactory(settings: Map[String, String]) extends ConfiguredFactory(settings){
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

