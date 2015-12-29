package org.nlp4l.sample

import scala.collection.mutable

import org.nlp4l.framework.processors._
import org.nlp4l.framework.models._

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
     * format function customization
     */
    class SimpleCellAttribute(name: String, cellType: CellType, isEditable: Boolean, isSortable: Boolean) extends CellAttribute(name, cellType, isEditable, isSortable) {
      override def format(cell: Any): String = {
        "<a href='https://github.com/NLP4L'>" + cell.toString() + "</a>"
      }
    }
    val list = Seq[CellAttribute](
      CellAttribute("cell01", CellType.StringType, true, true),
      new SimpleCellAttribute("cell02", CellType.IntType, false, true),
      CellAttribute("cell03", CellType.DoubleType, false, true),
      CellAttribute("cell02_check", CellType.StringType, false, false),
      CellAttribute("cell04", CellType.FloatType, false, true)
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
        var ss: Seq[Record] = dic.recordList
        (ss.size+1 to ss.size+20).toList.foreach { n=>
          val rcrd = Record(Seq(Cell("cell01", n.toString()), Cell("cell02", n), Cell("cell03", n.toDouble), Cell("cell02_check", null), Cell("cell04", n.toFloat)))
          ss = ss :+ rcrd
        }
        Some(Dictionary(ss))
      }
      case None => {
        var ss: Seq[Record] = Seq(rcrd01, rcrd02)
        (1 to 20).toList.foreach { n=>
          val rcrd = Record(Seq(Cell("cell01", n.toString()), Cell("cell02", n), Cell("cell03", n.toDouble), Cell("cell02_check", null), Cell("cell04", n.toFloat)))
          ss = ss :+ rcrd
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
        var celllist: Seq[Cell] = Seq()
        celllist = celllist :+ rec.cellList(0)
        celllist = celllist :+ rec.cellList(1)
        celllist = celllist :+ rec.cellList(2)
        var newCell3 = null
        val orgCell3 = rec.cellList(3)
        val cell1 = rec.cellList(1)
        if(cell1.value != null && cell1.value.asInstanceOf[Int] % 2 == 0) {
          val newCell3 = Cell(orgCell3.name, "EVEN")
          celllist = celllist :+ newCell3
        } else if(cell1.value != null && cell1.value.asInstanceOf[Int] % 2 == 1) {
          val newCell3 = Cell(orgCell3.name, "ODD")
          celllist = celllist :+ newCell3
        } else {
          val newCell3 = Cell(orgCell3.name, null)
          celllist = celllist :+ newCell3
        }
        celllist = celllist :+ rec.cellList(4)
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


class SimpleDeployerFactory(settings: Map[String, String]) extends DeployerFactory(settings) {
  override def getInstance: Deployer = {
    new SimpleDeployer(settings.get("filename"))
  }
}


class SimpleDeployer(val filename: Option[String]) extends Deployer {
  override def deploy (data: Option[Dictionary]): Tuple2[Boolean, Seq[String]] = {
    Thread.sleep(3000)
    
    (false, Seq("err 01", "err 02", filename.getOrElse("")))
    //(true, Seq())
  }
}

