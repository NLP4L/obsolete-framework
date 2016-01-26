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

class RegexValidatorFactory(settings: Map[String, String]) extends ValidatorFactory(settings) {
  override def getInstance: Validator = {
    val accept = settings.getOrElse("regexAccept", null)
    val deny = settings.getOrElse("regexDeny", null)
    (accept, deny) match {
      case (null, null) => throw new IllegalArgumentException(s"either regexAccept or regexDeny must be set")
      case _ => {
        new RegexValidator(getStrParamRequired("cellName"), accept, deny)
      }
    }
  }
}

class RegexValidator(val cellname: String, val accept: String, val deny: String) extends Validator {

  val logger = Logger(this.getClass)

  override def validate (data: Option[Dictionary]): Tuple2[Boolean, Seq[String]] = {
    data match {
      case Some(dic) => {
        val list = dic.cellList[String](cellname, a => a.toString)
        val result = checkPatterns(list)
        if(result._1) (true, Seq())
        else (false, Seq(result._2))
      }
      case None => (true, Seq())
    }
  }

  def checkPatterns(cellList: Seq[String]): (Boolean, String) = {
    (accept, deny) match {
      case (null, null) => (true, "")
      case (a, null) => checkAccept(cellList)
      case (null, b) => checkDeny(cellList)
      case (a, b) => {
        val resultAccept = checkAccept(cellList)
        if(resultAccept._1 == false) resultAccept
        else checkDeny(cellList)
      }
    }
  }

  private def checkAccept(cellList: Seq[String]): (Boolean, String) = {
    val regex = accept.r
    val result = cellList.filter(c => regex.findFirstIn(c) == None)
    if(result.length > 0) (false, s"""cell '${result.head}' of $cellname is not acceptable""")
    else (true, "")
  }

  private def checkDeny(cellList: Seq[String]): (Boolean, String) = {
    val regex = deny.r
    val result = cellList.filter(c => regex.findFirstIn(c) != None)
    if(result.length > 0) (false, s"""cell '${result.head}' of $cellname is denied""")
    else (true, "")
  }
}
