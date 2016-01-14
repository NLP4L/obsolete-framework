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

import org.nlp4l.framework.models._
import org.nlp4l.framework.builtin.{ValidatorChainBuilder, DeployerChainBuilder}
import org.specs2.mutable.Specification

class ConfigSpec extends Specification {

  val conf1 =
    """
      |{
      |  settings : {
      |    greeting : "this is global"
      |    name : "Mike"
      |    code : "global-1"
      |  }
      |  dictionary : [
      |    {
      |      class : org.nlp4l.framework.processors.TestDictionaryAttributeFactory
      |      settings : {
      |        code : "dic-1"
      |      }
      |    }
      |  ]
      |
      |  processors : [
      |    {
      |      class : org.nlp4l.framework.processors.TestProcessorFactory
      |      settings : {
      |        greeting : "this is processor"
      |        code : "proc-1"
      |      }
      |    }
      |    {
      |      class : org.nlp4l.framework.processors.Test2ProcessorFactory
      |      settings : {
      |        code : "proc-2"
      |      }
      |    }
      |  ]
      |
      |  validators : [
      |    {
      |      class : org.nlp4l.framework.processors.TestValidatorFactory
      |      settings : {
      |        name : "Tom"
      |        code : "valid-1"
      |      }
      |    }
      |    {
      |      class : org.nlp4l.framework.processors.Test2ValidatorFactory
      |      settings : {
      |        greeting : "this is validator"
      |        code : "valid-2"
      |      }
      |    }
      |  ]
      |
      |  deployers : [
      |    {
      |      class : org.nlp4l.framework.processors.TestDeployerFactory
      |      settings : {
      |        code : "deploy-1"
      |      }
      |    }
      |    {
      |      class : org.nlp4l.framework.processors.Test2DeployerFactory
      |      settings : {
      |        greeting : "this is deployer"
      |        name : "Tom"
      |        code : "deploy-2"
      |      }
      |    }
      |  ]
      |}
    """.stripMargin

  "global settings" should {

    "can be seen from DictionaryAttributeFactory that doesn't override them" in {
      val dicAttr = new ProcessorChainBuilder().dicBuild(conf1)
      dicAttr.cellAttributeList(0).name must_== "this is global"
      dicAttr.cellAttributeList(1).name must_== "Mike"
    }

    "can be seen from ProcessorFactories that don't override them" in {
      val pChain = new ProcessorChainBuilder().procBuild(1, conf1).result()
      val tp0 : TestProcessor = pChain.chain(0).asInstanceOf[TestProcessor]
      tp0.settings.getOrElse("name", "") must_== "Mike"
      val tp1 : Test2Processor = pChain.chain(1).asInstanceOf[Test2Processor]
      tp1.settings.getOrElse("greeting", "") must_== "this is global"
      tp1.settings.getOrElse("name", "") must_== "Mike"
    }

    "can be seen from ValidatorFactories that don't override them" in {
      val vChain = new ValidatorChainBuilder().build(conf1).result()
      val vp0 : TestValidator = vChain.chain(0).asInstanceOf[TestValidator]
      vp0.settings.getOrElse("greeting", "") must_== "this is global"
      val vp1 : Test2Validator = vChain.chain(1).asInstanceOf[Test2Validator]
      vp1.settings.getOrElse("name", "") must_== "Mike"
    }

    "can be seen from DeployerFactories that don't override them" in {
      val dChain = new DeployerChainBuilder().build(conf1).result()
      val dp0 : TestDeployer = dChain.chain(0).asInstanceOf[TestDeployer]
      dp0.settings.getOrElse("greeting", "") must_== "this is global"
      dp0.settings.getOrElse("name", "") must_== "Mike"
    }

    "can be overridden by DictionaryAttributeFactory" in {
      val dicAttr = new ProcessorChainBuilder().dicBuild(conf1)
      dicAttr.cellAttributeList(2).name must_== "dic-1"
    }

    "can be overridden by ProcessorFactories" in {
      val pChain = new ProcessorChainBuilder().procBuild(1, conf1).result()
      val tp0 : TestProcessor = pChain.chain(0).asInstanceOf[TestProcessor]
      tp0.settings.getOrElse("greeting", "") must_== "this is processor"
      tp0.settings.getOrElse("code", "") must_== "proc-1"
      val tp1 : Test2Processor = pChain.chain(1).asInstanceOf[Test2Processor]
      tp1.settings.getOrElse("code", "") must_== "proc-2"
    }

    "can be seen from ValidatorFactories that don't override them" in {
      val vChain = new ValidatorChainBuilder().build(conf1).result()
      val vp0 : TestValidator = vChain.chain(0).asInstanceOf[TestValidator]
      vp0.settings.getOrElse("name", "") must_== "Tom"
      vp0.settings.getOrElse("code", "") must_== "valid-1"
      val vp1 : Test2Validator = vChain.chain(1).asInstanceOf[Test2Validator]
      vp1.settings.getOrElse("greeting", "") must_== "this is validator"
      vp1.settings.getOrElse("code", "") must_== "valid-2"
    }

    "can be overridden by DeployerFactories" in {
      val dChain = new DeployerChainBuilder().build(conf1).result()
      val dp0 : TestDeployer = dChain.chain(0).asInstanceOf[TestDeployer]
      dp0.settings.getOrElse("code", "") must_== "deploy-1"
      val dp1 : Test2Deployer = dChain.chain(1).asInstanceOf[Test2Deployer]
      dp1.settings.getOrElse("greeting", "") must_== "this is deployer"
      dp1.settings.getOrElse("name", "") must_== "Tom"
      dp1.settings.getOrElse("code", "") must_== "deploy-2"
    }
  }
}

class TestDictionaryAttributeFactory(settings: Map[String, String]) extends DictionaryAttributeFactory(settings){
  override def getInstance(): DictionaryAttribute = {
    new DictionaryAttribute("empty", Seq(
      CellAttribute(settings.getOrElse("greeting", "foo"), CellType.StringType, false, false),
      CellAttribute(settings.getOrElse("name", "bar"), CellType.StringType, false, false),
      CellAttribute(settings.getOrElse("code", "baz"), CellType.StringType, false, false))
    )
  }
}

class TestProcessorFactory(settings: Map[String, String]) extends ProcessorFactory(settings){
  override def getInstance(): Processor = {
    new TestProcessor(settings)
  }
}

class TestProcessor(val settings: Map[String, String]) extends Processor {
}

class Test2ProcessorFactory(settings: Map[String, String]) extends ProcessorFactory(settings){
  override def getInstance(): Processor = {
    new Test2Processor(settings)
  }
}

class Test2Processor(val settings: Map[String, String]) extends Processor {
}

class TestValidatorFactory(settings: Map[String, String]) extends ValidatorFactory(settings){
  override def getInstance(): Validator = {
    new TestValidator(settings)
  }
}

class TestValidator(val settings: Map[String, String]) extends Validator {
  def validate (data: Option[Dictionary]): Tuple2[Boolean, Seq[String]] = {
    (true, Seq())
  }
}

class Test2ValidatorFactory(settings: Map[String, String]) extends ValidatorFactory(settings){
  override def getInstance(): Validator = {
    new Test2Validator(settings)
  }
}

class Test2Validator(val settings: Map[String, String]) extends Validator {
  def validate (data: Option[Dictionary]): Tuple2[Boolean, Seq[String]] = {
    (true, Seq())
  }
}

class TestDeployerFactory(settings: Map[String, String]) extends DeployerFactory(settings){
  override def getInstance(): Deployer = {
    new TestDeployer(settings)
  }
}

class TestDeployer(val settings: Map[String, String]) extends Deployer {
  def deploy (data: Option[Dictionary]): Tuple2[Boolean, Seq[String]] = {
    (true, Seq())
  }
}

class Test2DeployerFactory(settings: Map[String, String]) extends DeployerFactory(settings){
  override def getInstance(): Deployer = {
    new Test2Deployer(settings)
  }
}

class Test2Deployer(val settings: Map[String, String]) extends Deployer {
  def deploy (data: Option[Dictionary]): Tuple2[Boolean, Seq[String]] = {
    (true, Seq())
  }
}
