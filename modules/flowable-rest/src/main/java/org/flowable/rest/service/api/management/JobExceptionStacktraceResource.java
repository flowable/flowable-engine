/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.rest.service.api.management;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.flowable.engine.ManagementService;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.engine.runtime.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

/**
 * @author Frederik Heremans
 * @author Joram Barrez
 */
@RestController
@Api(tags = { "Jobs" }, description = "Manage Jobs")
public class JobExceptionStacktraceResource {

  @Autowired
  protected ManagementService managementService;

  @ApiOperation(value = "Get the exception stacktrace for a job", tags = {"Jobs"})
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Indicates the requested job was not found and the stacktrace has been returned. The response contains the raw stacktrace and always has a Content-type of text/plain."),
          @ApiResponse(code = 404, message = "Indicates the requested job was not found or the job doesn’t have an exception stacktrace. Status-description contains additional information about the error.")
  })
  @RequestMapping(value = "/management/jobs/{jobId}/exception-stacktrace", method = RequestMethod.GET)
  public String getJobStacktrace(@ApiParam(name = "jobId") @PathVariable String jobId, HttpServletResponse response) {
    Job job = managementService.createJobQuery().jobId(jobId).singleResult();
    if (job == null) {
      throw new FlowableObjectNotFoundException("Could not find a job with id '" + jobId + "'.", Job.class);
    }

    String stackTrace = managementService.getJobExceptionStacktrace(job.getId());

    if (stackTrace == null) {
      throw new FlowableObjectNotFoundException("Job with id '" + job.getId() + "' doesn't have an exception stacktrace.", String.class);
    }

    response.setContentType("text/plain");
    return stackTrace;
  }

  @ApiOperation(value = "Get the exception stacktrace for a timer job", tags = {"Jobs"})
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Indicates the requested job was not found and the stacktrace has been returned. The response contains the raw stacktrace and always has a Content-type of text/plain."),
          @ApiResponse(code = 404, message = "Indicates the requested job was not found or the job doesn’t have an exception stacktrace. Status-description contains additional information about the error.")
  })
  @RequestMapping(value = "/management/timer-jobs/{jobId}/exception-stacktrace", method = RequestMethod.GET)
  public String getTimerJobStacktrace(@ApiParam(name = "jobId") @PathVariable String jobId, HttpServletResponse response) {
    Job job = managementService.createTimerJobQuery().jobId(jobId).singleResult();
    if (job == null) {
      throw new FlowableObjectNotFoundException("Could not find a job with id '" + jobId + "'.", Job.class);
    }

    String stackTrace = managementService.getTimerJobExceptionStacktrace(job.getId());

    if (stackTrace == null) {
      throw new FlowableObjectNotFoundException("Timer job with id '" + job.getId() + "' doesn't have an exception stacktrace.", String.class);
    }

    response.setContentType("text/plain");
    return stackTrace;
  }

  @ApiOperation(value = "Get the exception stacktrace for a suspended job", tags = {"Jobs"})
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Indicates the requested job was not found and the stacktrace has been returned. The response contains the raw stacktrace and always has a Content-type of text/plain."),
          @ApiResponse(code = 404, message = "Indicates the requested job was not found or the job doesn’t have an exception stacktrace. Status-description contains additional information about the error.")
  })
  @RequestMapping(value = "/management/suspended-jobs/{jobId}/exception-stacktrace", method = RequestMethod.GET)
  public String getSuspendedJobStacktrace(@ApiParam(name = "jobId") @PathVariable String jobId, HttpServletResponse response) {
    Job job = managementService.createSuspendedJobQuery().jobId(jobId).singleResult();
    if (job == null) {
      throw new FlowableObjectNotFoundException("Could not find a job with id '" + jobId + "'.", Job.class);
    }

    String stackTrace = managementService.getSuspendedJobExceptionStacktrace(job.getId());

    if (stackTrace == null) {
      throw new FlowableObjectNotFoundException("Suspended job with id '" + job.getId() + "' doesn't have an exception stacktrace.", String.class);
    }

    response.setContentType("text/plain");
    return stackTrace;
  }

  @ApiOperation(value = "Get the exception stacktrace for a deadletter job", tags = {"Jobs"})
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Indicates the requested job was not found and the stacktrace has been returned. The response contains the raw stacktrace and always has a Content-type of text/plain."),
          @ApiResponse(code = 404, message = "Indicates the requested job was not found or the job doesn’t have an exception stacktrace. Status-description contains additional information about the error.")
  })
  @RequestMapping(value = "/management/deadletter-jobs/{jobId}/exception-stacktrace", method = RequestMethod.GET)
  public String getDeadLetterJobStacktrace(@ApiParam(name = "jobId") @PathVariable String jobId, HttpServletResponse response) {
    Job job = managementService.createDeadLetterJobQuery().jobId(jobId).singleResult();
    if (job == null) {
      throw new FlowableObjectNotFoundException("Could not find a job with id '" + jobId + "'.", Job.class);
    }

    String stackTrace = managementService.getDeadLetterJobExceptionStacktrace(job.getId());

    if (stackTrace == null) {
      throw new FlowableObjectNotFoundException("Suspended job with id '" + job.getId() + "' doesn't have an exception stacktrace.", String.class);
    }

    response.setContentType("text/plain");
    return stackTrace;
  }

}
