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

package org.flowable.cmmn.rest.service.api.management;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.rest.service.api.CmmnRestResponseFactory;
import org.flowable.cmmn.rest.service.api.RestActionRequest;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

/**
 * @author Tijs Rademakers
 */
@RestController
@Api(tags = { "Jobs" }, authorizations = { @Authorization(value = "basicAuth") })
public class JobResource extends JobBaseResource {

    private static final String EXECUTE_ACTION = "execute";
    private static final String MOVE_ACTION = "move";
    private static final String MOVE_TO_HISTORY_JOB_ACTION = "moveToHistoryJob";
    private static final String RESCHEDULE_ACTION = "reschedule";

    @Autowired
    protected CmmnRestResponseFactory restResponseFactory;

    @Autowired
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    
    @ApiOperation(value = "Get a single job", tags = { "Jobs" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the job exists and is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested job does not exist.")
    })
    @GetMapping(value = "/cmmn-management/jobs/{jobId}", produces = "application/json")
    public JobResponse getJob(@ApiParam(name = "jobId") @PathVariable String jobId) {
        Job job = getJobById(jobId);
        return restResponseFactory.createJobResponse(job);
    }

    @ApiOperation(value = "Get a single timer job", tags = { "Jobs" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the timer job exists and is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested job does not exist.")
    })
    @GetMapping(value = "/cmmn-management/timer-jobs/{jobId}", produces = "application/json")
    public JobResponse getTimerJob(@ApiParam(name = "jobId") @PathVariable String jobId) {
        Job job = getTimerJobById(jobId);
        return restResponseFactory.createTimerJobResponse(job);
    }

    @ApiOperation(value = "Get a single suspended job", tags = { "Jobs" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the suspended job exists and is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested job does not exist.")
    })
    @GetMapping(value = "/cmmn-management/suspended-jobs/{jobId}", produces = "application/json")
    public JobResponse getSuspendedJob(@ApiParam(name = "jobId") @PathVariable String jobId) {
        Job job = getSuspendedJobById(jobId);
        return restResponseFactory.createSuspendedJobResponse(job);
    }

    @ApiOperation(value = "Get a single deadletter job", tags = { "Jobs" })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Indicates the suspended job exists and is returned."),
            @ApiResponse(code = 404, message = "Indicates the requested job does not exist.")
    })
    @GetMapping(value = "/cmmn-management/deadletter-jobs/{jobId}", produces = "application/json")
    public JobResponse getDeadletterJob(@ApiParam(name = "jobId") @PathVariable String jobId) {
        Job job = getDeadLetterJobById(jobId);
        return restResponseFactory.createDeadLetterJobResponse(job);
    }

    @ApiOperation(value = "Get a single history job job", tags = { "Jobs" })
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Indicates the history job exists and is returned."),
        @ApiResponse(code = 404, message = "Indicates the requested job does not exist.")
    })
    @GetMapping(value = "/cmmn-management/history-jobs/{jobId}", produces = "application/json")
    public HistoryJobResponse getHistoryJob(@ApiParam(name = "jobId") @PathVariable String jobId) {
        HistoryJob historyJob = getHistoryJobById(jobId);

        if (historyJob != null) {
            HistoryJobResponse historyJobResponse = restResponseFactory.createHistoryJobResponse(historyJob);
            historyJobResponse.setAdvancedJobHandlerConfiguration(managementService.getHistoryJobHistoryJson(historyJob.getId()));
            return historyJobResponse;
        }

        return null;
    }

    @ApiOperation(value = "Delete a job", tags = { "Jobs" }, code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the job was found and has been deleted. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested job was not found..")
    })
    @DeleteMapping("/cmmn-management/jobs/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteJob(@ApiParam(name = "jobId") @PathVariable String jobId) {
        Job job = getJobById(jobId);
        try {
            managementService.deleteJob(job.getId());
        } catch (FlowableObjectNotFoundException e) {
            // Re-throw to have consistent error-messaging across REST-api
            throw new FlowableObjectNotFoundException("Could not find a job with id '" + jobId + "'.", Job.class);
        }
    }

    @ApiOperation(value = "Delete a timer job", tags = { "Jobs" }, code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the job was found and has been deleted. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested job was not found.")
    })
    @DeleteMapping("/cmmn-management/timer-jobs/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTimerJob(@ApiParam(name = "jobId") @PathVariable String jobId) {
        Job job = getTimerJobById(jobId);
        try {
            managementService.deleteTimerJob(job.getId());
        } catch (FlowableObjectNotFoundException e) {
            // Re-throw to have consistent error-messaging across REST-api
            throw new FlowableObjectNotFoundException("Could not find a job with id '" + jobId + "'.", Job.class);
        }
    }

    @ApiOperation(value = "Delete a history job", tags = { "Jobs" }, code = 204)
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "Indicates the history job was found and has been deleted. Response-body is intentionally empty."),
        @ApiResponse(code = 404, message = "Indicates the requested job was not found.")
    })
    @DeleteMapping("/cmmn-management/history-jobs/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHistoryJob(@ApiParam(name = "jobId") @PathVariable String jobId) {
        HistoryJob historyJob = getHistoryJobById(jobId);
        if (restApiInterceptor != null) {
            restApiInterceptor.deleteHistoryJob(historyJob);
        }

        try {
            managementService.deleteHistoryJob(jobId);
        } catch (FlowableObjectNotFoundException e) {
            // Re-throw to have consistent error-messaging across REST-api
            throw new FlowableObjectNotFoundException("Could not find a job with id '" + jobId + "'.", Job.class);
        }
    }
    
    @ApiOperation(value = "Delete a suspended job", tags = { "Jobs" }, code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the job was found and has been deleted. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested job was not found.")
    })
    @DeleteMapping("/cmmn-management/suspended-jobs/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSuspendedJob(@ApiParam(name = "jobId") @PathVariable String jobId) {
        Job job = getSuspendedJobById(jobId);
        try {
            managementService.deleteSuspendedJob(job.getId());
        } catch (FlowableObjectNotFoundException e) {
            // Re-throw to have consistent error-messaging across REST-api
            throw new FlowableObjectNotFoundException("Could not find a job with id '" + jobId + "'.", Job.class);
        }
    }

    @ApiOperation(value = "Delete a deadletter job", tags = { "Jobs" }, code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the job was found and has been deleted. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested job was not found.")
    })
    @DeleteMapping("/cmmn-management/deadletter-jobs/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeadLetterJob(@ApiParam(name = "jobId") @PathVariable String jobId) {
        Job job = getDeadLetterJobById(jobId);
        try {
            managementService.deleteDeadLetterJob(job.getId());
        } catch (FlowableObjectNotFoundException e) {
            // Re-throw to have consistent error-messaging across REST-api
            throw new FlowableObjectNotFoundException("Could not find a job with id '" + jobId + "'.", Job.class);
        }
    }

    @ApiOperation(value = "Execute a single job", tags = { "Jobs" }, code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the job was executed. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested job was not found."),
            @ApiResponse(code = 500, message = "Indicates the an exception occurred while executing the job. The status-description contains additional detail about the error. The full error-stacktrace can be fetched later on if needed.")
    })
    @PostMapping("/cmmn-management/jobs/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void executeJobAction(@ApiParam(name = "jobId") @PathVariable String jobId, @RequestBody RestActionRequest actionRequest) {
        if (actionRequest == null || !EXECUTE_ACTION.equals(actionRequest.getAction())) {
            throw new FlowableIllegalArgumentException("Invalid action, only 'execute' is supported.");
        }
        
        Job job = getJobById(jobId);

        try {
            managementService.executeJob(job.getId());
        } catch (FlowableObjectNotFoundException e) {
            // Re-throw to have consistent error-messaging across REST-api
            throw new FlowableObjectNotFoundException("Could not find a job with id '" + jobId + "'.", Job.class);
        }
    }

    @ApiOperation(value = "Execute a single job action (move or reschedule)", tags = { "Jobs" }, code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the timer job action was executed. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested job was not found."),
            @ApiResponse(code = 500, message = "Indicates the an exception occurred while executing the job. The status-description contains additional detail about the error. The full error-stacktrace can be fetched later on if needed.")
    })
    @PostMapping("/cmmn-management/timer-jobs/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void executeTimerJobAction(@ApiParam(name = "jobId") @PathVariable String jobId, @RequestBody TimerJobActionRequest actionRequest) {
        if (actionRequest == null || !(MOVE_ACTION.equals(actionRequest.getAction()) || RESCHEDULE_ACTION.equals(actionRequest.getAction()))) {
            throw new FlowableIllegalArgumentException("Invalid action, only 'move' or 'reschedule' are supported.");
        }
        
        Job job = getTimerJobById(jobId);

        if (MOVE_ACTION.equals(actionRequest.getAction())) {
            try {
                managementService.moveTimerToExecutableJob(job.getId());
            } catch (FlowableObjectNotFoundException e) {
                // Re-throw to have consistent error-messaging across REST-api
                throw new FlowableObjectNotFoundException("Could not find a timer job with id '" + jobId + "'.", Job.class);
            }
        } else if (RESCHEDULE_ACTION.equals(actionRequest.getAction())) {
            if (actionRequest.getDueDate() == null) {
                throw new FlowableIllegalArgumentException("Invalid reschedule timer action. Reschedule timer actions must have a valid due date");
            }
            try {
                managementService.rescheduleTimeDateValueJob(job.getId(), actionRequest.getDueDate());
            } catch (FlowableObjectNotFoundException e) {
                // Re-throw to have consistent error-messaging across REST-api
                throw new FlowableObjectNotFoundException("Could not find a timer job with id '" + jobId + "'.", Job.class);
            }
        }
    }

    @ApiOperation(value = "Execute a history job", tags = { "Jobs" }, code = 204)
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "Indicates the job was executed. Response-body is intentionally empty."),
        @ApiResponse(code = 404, message = "Indicates the requested job was not found."),
        @ApiResponse(code = 500, message = "Indicates the an exception occurred while executing the job. The status-description contains additional detail about the error. The full error-stacktrace can be fetched later on if needed.")
    })
    @PostMapping("/cmmn-management/history-jobs/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void executeHistoryJob(@ApiParam(name = "jobId") @PathVariable String jobId, @RequestBody RestActionRequest actionRequest) {
        if (actionRequest == null || !EXECUTE_ACTION.equals(actionRequest.getAction())) {
            throw new FlowableIllegalArgumentException("Invalid action, only 'execute' is supported.");
        }

        HistoryJobResponse historyJob = getHistoryJob(jobId);

        try {
            managementService.executeHistoryJob(historyJob.getId());
        } catch (FlowableObjectNotFoundException e) {
            // Re-throw to have consistent error-messaging across REST-api
            throw new FlowableObjectNotFoundException("Could not find a job with id '" + jobId + "'.", Job.class);
        }
    }
    
    @ApiOperation(value = "Move a single deadletter job. Accepts 'move' and 'moveToHistoryJob' as action.", tags = { "Jobs" }, code = 204)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Indicates the dead letter job was moved. Response-body is intentionally empty."),
            @ApiResponse(code = 404, message = "Indicates the requested job was not found."),
            @ApiResponse(code = 500, message = "Indicates the an exception occurred while executing the job. The status-description contains additional detail about the error. The full error-stacktrace can be fetched later on if needed.")
    })
    @PostMapping("/cmmn-management/deadletter-jobs/{jobId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void executeDeadLetterJobAction(@ApiParam(name = "jobId") @PathVariable String jobId, @RequestBody RestActionRequest actionRequest) {
        if (actionRequest == null || !(MOVE_ACTION.equals(actionRequest.getAction()) || MOVE_TO_HISTORY_JOB_ACTION.equals(actionRequest.getAction()))) {
            throw new FlowableIllegalArgumentException("Invalid action, only 'move' or 'moveToHistoryJob' is supported.");
        }
        
        Job deadLetterJob = getDeadLetterJobById(jobId);

        if (MOVE_ACTION.equals(actionRequest.getAction())) {

            if (restApiInterceptor != null) {
                restApiInterceptor.moveDeadLetterJob(deadLetterJob, MOVE_ACTION);
            }
            /*
             * Note that the jobType is checked to know which kind of move that needs to be done.
             * The MOVE_TO_HISTORY_JOB_ACTION allows to specifically force the move to a history job and trigger the else part below.
             */

            try {
                if (HistoryJobEntity.HISTORY_JOB_TYPE.equals(deadLetterJob.getJobType())) {
                    managementService.moveDeadLetterJobToHistoryJob(deadLetterJob.getId(), cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());

                } else {
                    managementService.moveDeadLetterJobToExecutableJob(deadLetterJob.getId(), cmmnEngineConfiguration.getAsyncExecutorNumberOfRetries());

                }

            } catch (FlowableObjectNotFoundException e) {
                // Re-throw to have consistent error-messaging across REST-API
                throw new FlowableObjectNotFoundException("Could not find a dead letter job with id '" + jobId + "'.", Job.class);
            }

        } else if (MOVE_TO_HISTORY_JOB_ACTION.equals(actionRequest.getAction())) {
            if (restApiInterceptor != null) {
                restApiInterceptor.moveDeadLetterJob(deadLetterJob, MOVE_TO_HISTORY_JOB_ACTION);
            }
            try {
                managementService.moveDeadLetterJobToHistoryJob(deadLetterJob.getId(), cmmnEngineConfiguration.getAsyncHistoryExecutorNumberOfRetries());
            } catch (FlowableObjectNotFoundException e) {
                // Re-throw to have consistent error-messaging across REST-api
                throw new FlowableObjectNotFoundException("Could not find a dead letter job with id '" + jobId + "'.", Job.class);
            }

        }

    }
}
