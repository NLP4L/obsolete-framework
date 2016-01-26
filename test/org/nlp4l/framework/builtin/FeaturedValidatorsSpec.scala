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

import org.specs2.mutable.Specification

class FeaturedValidatorsSpec extends Specification {

  "UniqueRecordValidator.unique" should {
    "find a value which appeared in multiple items" in {
      val list = "a,b,c,d,e,f,g,b,h,i,j".split(",").toList
      val validator = new UniqueRecordValidator("")
      validator.unique(list) must_== Some("b")
    }

    "not find any values if all values are unique" in {
      val list = "a,b,c,d,e,f,g,h,i,j".split(",").toList
      val validator = new UniqueRecordValidator("")
      validator.unique(list) must_== None
    }
  }
}
