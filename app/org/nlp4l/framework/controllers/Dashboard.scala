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

package org.nlp4l.framework.controllers

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Failure
import scala.util.Success
import com.google.inject.name.Named
import akka.actor.ActorRef
import javax.inject.Inject
import javax.inject.Singleton
import play.api.mvc.Action
import play.api.mvc.Controller
import org.nlp4l.framework.dao.JobDAO
import org.nlp4l.framework.dao.RunDAO
import org.nlp4l.framework.models.CellAttribute
import org.nlp4l.framework.models.DictionaryAttribute
import org.nlp4l.framework.processors.ProcessorChain2Builder
import org.nlp4l.framework.builtin.Job

@Singleton
class Dashboard @Inject()(jobDAO: JobDAO, runDAO: RunDAO, @Named("processor-actor2") processActor: ActorRef) extends Controller {

  def index = Action {
    jobDAO.init
    runDAO.initJobStatus
    Ok(org.nlp4l.framework.views.html.dashboard())
  }

  def joblist = Action {
    Ok(org.nlp4l.framework.views.html.joblist())
  }

  def newJob = Action {
    Ok(org.nlp4l.framework.views.html.newjob("","",""))
  }
  
  def jobstatus = Action {
    Ok(org.nlp4l.framework.views.html.jobstatus())
  }
  
  def editJob(jobId: Int, runId: Int) = Action.async { implicit request =>
    val f: Future[Job] = jobDAO.get(jobId)
    Await.ready(f, scala.concurrent.duration.Duration.Inf)
    f.value.get match {
      case Success(job) => {
        val runIdList: Seq[Int] = runDAO.selectRunList(jobId, job.lastRunId)
        Future.successful(Ok(org.nlp4l.framework.views.html.editjob(job, runIdList, "", "")))
      }
      case Failure(ex) => Future.successful(NotFound(org.nlp4l.framework.views.html.notFound("Job not found")))
    }
  }

  def jobresult(jobId: Int, runId: Int) = Action {
    val f: Future[Job] = jobDAO.get(jobId)
    Await.ready(f, Duration.Inf)
    f.value.get match {
      case Success(job) => {
        val (listTable: String, addForm: String, editForm: String) = createJobResultTable(job, runId)
        val runIdList: Seq[Int] = runDAO.selectRunList(jobId, job.lastRunId)
        Ok(org.nlp4l.framework.views.html.jobresult(job, jobId, runId, runIdList, listTable, addForm, editForm))
      }
      case Failure(ex) => NotFound(org.nlp4l.framework.views.html.notFound("Job not found"))
    }
  }
  
  def createJobResultTable(job: Job, runId: Int): (String, String, String) = {
    val jobId = job.jobId.getOrElse(0)
    
    val dic: DictionaryAttribute = new ProcessorChain2Builder().dicBuild(job.config)
    var ths: String = "<th data-field=\"id\" data-formatter=\"RecordIdFormatter\">ID</th>"
    var addtable: String = ""
    dic.cellAttributeList foreach { c: CellAttribute =>
      ths += "<th data-field=\"" + c.name.toLowerCase() + "\" data-filter-control=\"select\" data-filter-data=\"url:/job/result/filterlist/"+jobId+"/"+runId+"/"+c.name.toLowerCase()+"\""
      addtable += "<th align=\"right\">" + c.name.toLowerCase() + "</th><td><input type=\"text\" class=\"form-control\" id=\"form_" + c.name.toLowerCase() + "\" name=\"" + c.name.toLowerCase() + "\"></td>"
      c.isSortable match {
        case true => ths += " data-sortable=\"true\""
        case _ => ths += " data-sortable=\"false\""
      }
      ths += ">" + c.name.toLowerCase()  + "</th>\n"
    }
    ths += "<th data-field=\"replay\" data-filter-control=\"select\" data-filter-data=\"url:/job/result/filterlist/"+jobId+"/"+runId+"/replay\">Replay</th>"

    val listTable = s"""<table id="table"
           data-toolbar="#toolbar"
           data-toggle="table"
           data-pagination="true"
           data-show-pagination-switch="false"
           data-pagination-first-text="First"
           data-pagination-pre-text="Prev"
           data-pagination-next-text="Next"
           data-pagination-last-text="Last"
           data-id-field="id"
           data-filter-control="true"
           data-side-pagination="server"
           data-url="/job/result/${jobId}/${runId}">
          <thead>
            <tr>
              <th data-field="state" data-checkbox="true"></th>
              ${ths}
            </tr>
          </thead>
        </table>
    """
              
    val addform = s"""
    <form id="addrecord-form" method="post">
      <table id="addform" class="table">
        <tbody>
          <tr>
            <td><button type="submit" id="addrecord-button" class="btn btn-primary"><i class="glyphicon glyphicon-plus"></i>Add</button></td>
            ${addtable}
          </tr>
        </tbody>
      </table>
      <input type="hidden" id="form_id" name="id">
    </form>
    """
    
    val editform = s"""
    <form id="editrecord-form" method="post">
      <table id="editform" class="table">
        <tbody>
          <tr>
            ${addtable}
          </tr>
        </tbody>
      </table>
      <input type="hidden" id="form_id" name="id">
      <input type="hidden" id="form_hashcode" name="hashcode">
    </form>
    """

    (listTable, addform, editform)
  }
}
