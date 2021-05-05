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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.flowable.rest.service.api.RestActionRequest;
import org.flowable.rest.service.api.RestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Frederik Heremans
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Jobs" }, description = "Manage Jobs", authorizations = { @Authorization(value = "basicAuth") })
public class JobResource extends JobBaseResource {

    private static final String EXECUTE_ACTION = "execute";
    private static final String MOVE_ACTION = "move";
    private static final String MOVE_TO_HISTORY_JOB_ACTION = "moveToHistoryJob";

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    
    @ApiOperation(value = "Get a single job", tags = { "Jobs" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the job exists and is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested job does not exist.")
    })
    @GetMapping(value = "/management/jobs/{jobId}", produces = "application/json")
    public JobResponse getJob(@ApiParam(name = "jobId") @PathVariable String jobId, HttpServletRequest request) {
        Job job = getJobById(jobId);
        return restResponseFactory.createJobResponse(job);
    }

    @ApiOperation(value = "Get a single timer job", tags = { "Jobs" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the timer job exists and is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested job does not exist.")
    })
    @GetMapping(value = "/management/timer-jobs/{jobId}", produces = "application/json")
    public JobResponse getTimerJob(@ApiParam(name = "jobId") @PathVariable String jobId, HttpServletRequest request) {
        Job job = getTimerJobById(jobId);
        return restResponseFactory.createJobResponse(job);
    }

    @ApiOperation(value = "Get a single suspended job", tags = { "Jobs" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the suspended job exists and is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested job does not exist.")
    })
    @GetMapping(value = "/management/suspended-jobs/{jobId}", produces = "application/json")
    public JobResponse getSuspendedJob(@ApiParam(name = "jobId") @PathVariable String jobId, HttpServletRequest request) {
        Job job = getSuspendedJobById(jobId);
        return restResponseFactory.createJobResponse(job);
    }

    @ApiOperation(value = "Get a single deadletter job", tags = { "Jobs" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the suspended job exists and is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested job does not exist.")
    })
    @GetMapping(value = "/management/deadletter-jobs/{jobId}", produces = "application/json")
    public JobResponse getDeadletterJob(@ApiParam(name = "jobId") @PathVariable String jobId, HttpServletRequest request) {
        Job job = getDeadLetterJobById(jobId);
        return restResponseFactory.createJobResponse(job);
    }

    @ApiOperation(value = "Get a single history job job", tags = { "Jobs" })
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Indicates the history job exists and is returned."),
        @ApiResponse(code = 404, message = "Indicates the requested job does not exist.")
    })
    @GetMapping(value = "/management/history-jobs/{jobId}", produces = "application/json")
    public HistoryJobResponse getHistoryJob(@ApiParam(name = "jobId") @PathVariable String jobId) {
        HistoryJob historyJob = getHistoryJobById(jobId);

        if (historyJob != null) {
            HistoryJobResponse historyJobResponse = restResponseFactory.createHistoryJobResponse(historyJob);
            historyJobResponse.setAdvancedJobHandlerConfiguration(managementService.getHistoryJobHistoryJson(historyJob.getId()));
            return historyJobResponse;
        }

        return null;
    }

    @ApiOperation(value = "Delete a job", tags = { "Jobs" })
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the job was found and has been deleted. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested job was not found.")
    })
    @DeleteMapping("/management/jobs/{jobId}")
    public void deleteJob(@ApiParam(name = "jobId") @PathVariable String jobId, HttpServletResponse response) {
        Job job = getJobById(jobId);
        if (restApiInterceptor != null) {
            restApiInterceptor.deleteJob(job);
        }
        
        try {
            managementService.deleteJob(jobId);
        } catch (FlowableObjectNotFoundException aonfe) {
            // Re-throw to have consistent error-messaging across REST-api
            throw new FlowableObjectNotFoundException("Could not find a job with id '" + jobId + "'.", Job.class);
        }
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    @ApiOperation(value = "Delete a timer job", tags = { "Jobs" })
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the job was found and has been deleted. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested job was not found.")
    })
    @DeleteMapping("/management/timer-jobs/{jobId}")
    public void deleteTimerJob(@ApiParam(name = "jobId") @PathVariable String jobId, HttpServletResponse response) {
        Job job = getTimerJobById(jobId);
        if (restApiInterceptor != null) {
            restApiInterceptor.deleteJob(job);
        }
        
        try {
            managementService.deleteTimerJob(jobId);
        } catch (FlowableObjectNotFoundException aonfe) {
            // Re-throw to have consistent error-messaging across REST-api
            throw new FlowableObjectNotFoundException("Could not find a job with id '" + jobId + "'.", Job.class);
        }
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }
    
    @ApiOperation(value = "Delete a suspended job", tags = { "Jobs" })
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the job was found and has been deleted. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested job was not found.")
    })
    @DeleteMapping("/management/suspended-jobs/{jobId}")
    public void deleteSuspendedJob(@ApiParam(name = "jobId") @PathVariable String jobId, HttpServletResponse response) {
        Job job = getSuspendedJobById(jobId);
        if (restApiInterceptor != null) {
            restApiInterceptor.deleteJob(job);
        }
        
        try {
            managementService.deleteSuspendedJob(jobId);
        } catch (FlowableObjectNotFoundException aonfe) {
            // Re-throw to have consistent error-messaging across REST-api
            throw new FlowableObjectNotFoundException("Could not find a job with id '" + jobId + "'.", Job.class);
        }
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    @ApiOperation(value = "Delete a deadletter job", tags = { "Jobs" })
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the job was found and has been deleted. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested job was not found.")
    })
    @DeleteMapping("/management/deadletter-jobs/{jobId}")
    public void deleteDeadLetterJob(@ApiParam(name = "jobId") @PathVariable String jobId, HttpServletResponse response) {
        Job job = getDeadLetterJobById(jobId);
        if (restApiInterceptor != null) {
            restApiInterceptor.deleteJob(job);
        }
        
        try {
            managementService.deleteDeadLetterJob(jobId);
        } catch (FlowableObjectNotFoundException aonfe) {
            // Re-throw to have consistent error-messaging across REST-api
            throw new FlowableObjectNotFoundException("Could not find a job with id '" + jobId + "'.", Job.class);
        }
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    @ApiOperation(value = "Delete a history job", tags = { "Jobs" })
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "Indicates the history job was found and has been deleted. Response-body is intentionally empty."),
        @ApiResponse(code = 404, message = "Indicates the requested job was not found.")
    })
    @DeleteMapping("/management/history-jobs/{jobId}")
    public void deleteHistoryJob(@ApiParam(name = "jobId") @PathVariable String jobId, HttpServletResponse response) {
        HistoryJob historyJob = getHistoryJobById(jobId);
        if (restApiInterceptor != null) {
            restApiInterceptor.deleteHistoryJob(historyJob);
        }

        try {
            managementService.deleteHistoryJob(jobId);
        } catch (FlowableObjectNotFoundException aonfe) {
            // Re-throw to have consistent error-messaging across REST-api
            throw new FlowableObjectNotFoundException("Could not find a job with id '" + jobId + "'.", Job.class);
        }
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    @ApiOperation(value = "Execute a single job", tags = { "Jobs" })
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the job was executed. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested job was not found."),
            @ApiResponse(code = 500, message = "Indicates the an exception occurred while executing the job. The status-description contains additional detail about the error. The full error-stacktrace can be fetched later on if needed.")
    })
    @PostMapping("/management/jobs/{jobId}")
    public void executeJobAction(@ApiParam(name = "jobId") @PathVariable String jobId, @RequestBody RestActionRequest actionRequest, HttpServletResponse response) {
        if (actionRequest == null || !EXECUTE_ACTION.equals(actionRequest.getAction())) {
            throw new FlowableIllegalArgumentException("Invalid action, only 'execute' is supported.");
        }
        
        Job job = getJobById(jobId);

        try {
            managementService.executeJob(job.getId());
        } catch (FlowableObjectNotFoundException aonfe) {
            // Re-throw to have consistent error-messaging across REST-api
            throw new FlowableObjectNotFoundException("Could not find a job with id '" + jobId + "'.", Job.class);
        }

        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    @ApiOperation(value = "Execute a history job", tags = { "Jobs" })
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "Indicates the job was executed. Response-body is intentionally empty."),
        @ApiResponse(code = 404, message = "Indicates the requested job was not found."),
        @ApiResponse(code = 500, message = "Indicates the an exception occurred while executing the job. The status-description contains additional detail about the error. The full error-stacktrace can be fetched later on if needed.")
    })
    @PostMapping("/management/history-jobs/{jobId}")
    public void executeHistoryJob(@ApiParam(name = "jobId") @PathVariable String jobId, @RequestBody RestActionRequest actionRequest, HttpServletResponse response) {
        if (actionRequest == null || !EXECUTE_ACTION.equals(actionRequest.getAction())) {
            throw new FlowableIllegalArgumentException("Invalid action, only 'execute' is supported.");
        }

        HistoryJobResponse historyJob = getHistoryJob(jobId);

        try {
            managementService.executeHistoryJob(historyJob.getId());
        } catch (FlowableObjectNotFoundException aonfe) {
            // Re-throw to have consistent error-messaging across REST-api
            throw new FlowableObjectNotFoundException("Could not find a job with id '" + jobId + "'.", Job.class);
        }

        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    @ApiOperation(value = "Move a single timer job", tags = { "Jobs" })
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the timer job was moved. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested job was not found."),
            @ApiResponse(code = 500, message = "Indicates the an exception occurred while executing the job. The status-description contains additional detail about the error. The full error-stacktrace can be fetched later on if needed.")
    })
    @PostMapping("/management/timer-jobs/{jobId}")
    public void executeTimerJobAction(@ApiParam(name = "jobId") @PathVariable String jobId, @RequestBody RestActionRequest actionRequest, HttpServletResponse response) {
        if (actionRequest == null || !MOVE_ACTION.equals(actionRequest.getAction())) {
            throw new FlowableIllegalArgumentException("Invalid action, only 'move' is supported.");
        }
        
        Job job = getTimerJobById(jobId);

        try {
            managementService.moveTimerToExecutableJob(job.getId());
        } catch (FlowableObjectNotFoundException aonfe) {
            // Re-throw to have consistent error-messaging across REST-api
            throw new FlowableObjectNotFoundException("Could not find a timer job with id '" + jobId + "'.", Job.class);
        }

        response.setStatus(HttpStatus.NO_CONTENT.value());
    }
    
    @ApiOperation(value = "Move a single deadletter job. Accepts 'move' and 'moveToHistoryJob' as action.", tags = { "Jobs" })
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the dead letter job was moved. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested job was not found."),
            @ApiResponse(code = 500, message = "Indicates the an exception occurred while executing the job. The status-description contains additional detail about the error. The full error-stacktrace can be fetched later on if needed.")
    })
    @PostMapping("/management/deadletter-jobs/{jobId}")
    public void executeDeadLetterJobAction(@ApiParam(name = "jobId") @PathVariable String jobId, @RequestBody RestActionRequest actionRequest, HttpServletResponse response) {
        if (actionRequest == null || !(MOVE_ACTION.equals(actionRequest.getAction()) || MOVE_TO_HISTORY_JOB_ACTION.equals(actionRequest.getAction()))) {
            throw new FlowableIllegalArgumentException("Invalid action, only 'move' or 'moveToHistoryJob' is supported.");
        }
        
        Job deadLetterJob = getDeadLetterJobById(jobId);

        if (MOVE_ACTION.equals(actionRequest.getAction())) {

            /*
             * Note that the jobType is checked to know which kind of move that needs to be done.
             * The MOVE_TO_HISTORY_JOB_ACTION allows to specifically force the move to a history job and trigger the else part below.
             */

            try {
                if (HistoryJobEntity.HISTORY_JOB_TYPE.equals(deadLetterJob.getJobType())) {
                    managementService.moveDeadLetterJobToHistoryJob(deadLetterJob.getId(), processEngineConfiguration.getAsyncExecutorNumberOfRetries());

                } else {
                    managementService.moveDeadLetterJobToExecutableJob(deadLetterJob.getId(), processEngineConfiguration.getAsyncExecutorNumberOfRetries());

                }

            } catch (FlowableObjectNotFoundException aonfe) {
                // Re-throw to have consistent error-messaging across REST-API
                throw new FlowableObjectNotFoundException("Could not find a dead letter job with id '" + jobId + "'.", Job.class);
            }

            response.setStatus(HttpStatus.NO_CONTENT.value());

        } else if (MOVE_TO_HISTORY_JOB_ACTION.equals(actionRequest.getAction())) {
            try {
                managementService.moveDeadLetterJobToHistoryJob(deadLetterJob.getId(), processEngineConfiguration.getAsyncHistoryExecutorNumberOfRetries());
            } catch (FlowableObjectNotFoundException aonfe) {
                // Re-throw to have consistent error-messaging across REST-api
                throw new FlowableObjectNotFoundException("Could not find a dead letter job with id '" + jobId + "'.", Job.class);
            }

            response.setStatus(HttpStatus.NO_CONTENT.value());

        }


    }
}
