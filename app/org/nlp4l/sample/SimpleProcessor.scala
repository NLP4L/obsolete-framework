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

package org.nlp4l.sample

import org.nlp4l.framework.processors._
import org.nlp4l.framework.models._

import scala.collection.mutable.ListBuffer

/**
 * NLP4L framework Processor sample
 * 
 */


/**
 * Dictionary schema definition
 * 
 */
class SimpleDictionaryAttributeFactory(settings: Map[String, String]) extends DictionaryAttributeFactory(settings) {
  override def getInstance: DictionaryAttribute = {
    
    /**
     * User defined cell hashcode function
     */
    def ignoreThisCell(d: Any): Int = {
      0
    }
    
    /**
     * User defined format function
     */
    class SimpleCellAttribute(name: String, cellType: CellType, isEditable: Boolean, isSortable: Boolean, userDefinedHashCode:(Any) => Int)
      extends CellAttribute(name, cellType, isEditable, isSortable, userDefinedHashCode) {
      override def format(cell: Any): String = {
        "<a href='https://github.com/NLP4L#" + cell.toString() + "'>" + cell.toString() + "</a>"
      }
    }
    val list = Seq[CellAttribute](
      CellAttribute("cell01", CellType.StringType, true, true),
      new SimpleCellAttribute("cell02", CellType.IntType, false, true, ignoreThisCell),
      CellAttribute("cell03", CellType.DoubleType, false, true, ignoreThisCell),
      CellAttribute("cell02_check", CellType.StringType, false, false, ignoreThisCell),
      CellAttribute("cell04", CellType.FloatType, false, true, ignoreThisCell)
      )
    new DictionaryAttribute("simple", list)
  }
}


class SimpleProcessorFactory(settings: Map[String, String]) extends ProcessorFactory(settings) {
  override def getInstance: Processor = {
    new SimpleProcessor(settings.get("param1"), settings.get("param2"))
  }
}


class SimpleProcessor(val param1: Option[String], val param2: Option[String]) extends Processor {
  override def execute(data: Option[Dictionary]): Option[Dictionary] = {
    Thread.sleep(5000)
    val rcrd01 = Record(Seq(Cell("cell01", param1.getOrElse("0001")), Cell("cell02", param2.getOrElse("2").toInt), Cell("cell03", 3.1), Cell("cell02_check", null), Cell("cell04", null)))
    val rcrd02 = Record(Seq(Cell("cell01", param1.getOrElse("0002")), Cell("cell02", null), Cell("cell03", null), Cell("cell02_check", null), Cell("cell04", null)))
    data match {
      case Some(dic) => {
        val ss = dic.recordList.toBuffer
        ss += rcrd01
        Some(Dictionary(ss))
      }
      case None => {
        val ss = ListBuffer(rcrd01, rcrd02)
        (1 to 20).toList.foreach { n=>
          val rcrd = Record(Seq(Cell("cell01", n.toString()), Cell("cell02", n), Cell("cell03", n.toDouble), Cell("cell02_check", null), Cell("cell04", n.toFloat)))
          ss += rcrd
        }
        val dic = Dictionary(ss)
        Some(dic)
      }
    }
  }
}


class SimpleRecordProcessorFactory(settings: Map[String, String]) extends RecordProcessorFactory(settings) {
  override def getInstance: RecordProcessor = {
    new SimpleRecordProcessor()
  }
}

/**
 * RecordProcessor sample
 * 
 * Set the cell value according to the other cell value
 * 
 */
class SimpleRecordProcessor() extends RecordProcessor {
  override def execute(data: Option[Record]): Option[Record] = {
    data match {
      case Some(rec) => {
        val celllist = ListBuffer.empty[Cell]
        celllist += rec.cellList(0)
        celllist += rec.cellList(1)
        celllist += rec.cellList(2)
        val orgCell3 = rec.cellList(3)
        val cell1 = rec.cellList(1)
        if(cell1.value != null && cell1.value.asInstanceOf[Int] % 2 == 0) {
          val newCell3 = Cell(orgCell3.name, "EVEN")
          celllist += newCell3
        } else if(cell1.value != null && cell1.value.asInstanceOf[Int] % 2 == 1) {
          val newCell3 = Cell(orgCell3.name, "ODD")
          celllist += newCell3
        } else {
          val newCell3 = Cell(orgCell3.name, null)
          celllist += newCell3
        }
        celllist += rec.cellList(4)
        Some(Record(celllist))
      }
      case None => {
        None
      }
    }
  }
}




class SimpleValidatorFactory(settings: Map[String, String]) extends ValidatorFactory(settings) {
  override def getInstance: Validator = {
    new SimpleValidator
  }
}

class SimpleValidator extends Validator {
  override def validate (data: Option[Dictionary]): Tuple2[Boolean, Seq[String]] = {
    
    // Some validation
    
    // NO error
    (true, Seq())
  }
}

class Simple2ValidatorFactory(settings: Map[String, String]) extends ValidatorFactory(settings) {
  override def getInstance: Validator = {
    new Simple2Validator
  }
}

class Simple2Validator extends Validator {
  override def validate (data: Option[Dictionary]): Tuple2[Boolean, Seq[String]] = {
    
    // Some validation
    Thread.sleep(3000)
    
    // Error occured
    (false, Seq("err 01", "err 02"))
  }
}


class SimpleWriterFactory(settings: Map[String, String]) extends WriterFactory(settings) {
  override def getInstance: Writer = {
    new SimpleWriter(settings.get("filename"))
  }
}


class SimpleWriter(val filename: Option[String]) extends Writer {
  override def write (data: Option[Dictionary]): Tuple3[Boolean, Seq[String], Seq[String]] = {
    Thread.sleep(3000)
    
    (false, Seq("err 01", "err 02", filename.getOrElse("")), Seq())
    //(true, Seq())
  }
}
