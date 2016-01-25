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

package org.nlp4l.framework.builtin

import org.nlp4l.framework.models.Dictionary
import org.nlp4l.framework.processors.{Validator, ValidatorFactory}
import play.api.Logger

class UniqueRecordValidatorFactory(settings: Map[String, String]) extends ValidatorFactory(settings) {
  override def getInstance: Validator = {
    new UniqueRecordValidator(getStrParamRequired("cellName"))
  }
}

class UniqueRecordValidator(val cellname: String) extends Validator {

  val logger = Logger(this.getClass)

  override def validate (data: Option[Dictionary]): Tuple2[Boolean, Seq[String]] = {
    data match {
      case Some(dic) => {
        try{
          val list = dic.cellList[String](cellname, a => a.toString)
          unique(list.toList) match {
            case Some(value) => {
              val msg = s"""cell value '$value' is found among records multiple times"""
              (false, Seq(msg))
            }
            case _ => (true, Seq())
          }
        }
        catch{
          case e => {
            val msg = s"cell name '$cellname' not found in some Records"
            logger.warn(msg)
            (false, Seq(msg, e.getMessage))
          }
        }
      }
      case None => { (true, Seq()) }
    }
  }

  def unique(list: List[String]): Option[String] = {
    list match {
      case a :: b => {
        if(b.contains(a)) Some(a)
        else unique(b)
      }
      case Nil => None
    }
  }
}
