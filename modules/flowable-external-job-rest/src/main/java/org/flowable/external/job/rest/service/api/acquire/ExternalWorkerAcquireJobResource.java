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
package org.flowable.external.job.rest.service.api.acquire;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableForbiddenException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.rest.variable.EngineRestVariable;
import org.flowable.external.job.rest.service.api.ExternalJobRestResponseFactory;
import org.flowable.external.job.rest.service.api.ExternalWorkerJobBaseResource;
import org.flowable.job.api.AcquiredExternalWorkerJob;
import org.flowable.job.api.ExternalWorkerJob;
import org.flowable.job.api.ExternalWorkerJobAcquireBuilder;
import org.flowable.job.api.ExternalWorkerJobFailureBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * @author Filip Hrisafov
 */
@RestController
@Api(tags = { "Acquire and Execute" })
public class ExternalWorkerAcquireJobResource extends ExternalWorkerJobBaseResource {

    protected final ExternalJobRestResponseFactory restResponseFactory;

    public ExternalWorkerAcquireJobResource(ExternalJobRestResponseFactory restResponseFactory) {
        this.restResponseFactory = restResponseFactory;
    }

    @ApiOperation(value = "Acquire External Worker Jobs", tags = { "Acquire and Execute" })
    @ApiResponses({
            @ApiResponse(code = 200, message = "Indicates the jobs were acquired and locked."),
            @ApiResponse(code = 400, message = "Indicates the request was invalid."),
            @ApiResponse(code = 403, message = "Indicates the user does not have the rights acquire the jobs."),
    })
    @PostMapping(value = "/acquire/jobs", produces = "application/json")
    public List<AcquiredExternalWorkerJobResponse> acquireAndLockJobs(@RequestBody AcquireExternalWorkerJobRequest request) {
        ExternalWorkerJobAcquireBuilder acquireBuilder = createExternalWorkerAcquireBuilder();

        if (restApiInterceptor != null) {
            restApiInterceptor.accessAcquireExternalWorkerJobs(acquireBuilder, request);
        }

        if (StringUtils.isNotEmpty(request.getTopic())) {
            if (request.getLockDuration() != null) {
                acquireBuilder.topic(request.getTopic(), request.getLockDuration());
            } else {
                throw new FlowableIllegalArgumentException("lockDuration is required");
            }
        } else {
            throw new FlowableIllegalArgumentException("topic is required");
        }

        if (request.getScopeType() != null) {
            acquireBuilder.scopeType(request.getScopeType());
        }

        if (StringUtils.isNotEmpty(request.getWorkerId())) {
            List<AcquiredExternalWorkerJob> acquiredJobs = acquireBuilder
                    .acquireAndLock(request.getNumberOfTasks(), request.getWorkerId(), request.getNumberOfRetries());
            return restResponseFactory.createAcquiredExternalWorkerJobResponseList(acquiredJobs);
        } else {
            throw new FlowableIllegalArgumentException("workerId is required");
        }
    }

    @ApiOperation(value = "Complete an External Worker Jobs", code = 204, tags = { "Acquire and Execute" })
    @ApiResponses({
            @ApiResponse(code = 204, message = "Indicates the job was successfully completed."),
            @ApiResponse(code = 400, message = "Indicates the request was invalid."),
            @ApiResponse(code = 403, message = "Indicates the user does not have the rights complete the job."),
            @ApiResponse(code = 404, message = "Indicates the job does not exist."),
    })
    @PostMapping(value = "/acquire/jobs/{jobId}/complete", produces = "application/json")
    public ResponseEntity<?> completeJob(@PathVariable String jobId, @RequestBody ExternalWorkerJobCompleteRequest request) {
        String workerId = request.getWorkerId();
        if (StringUtils.isEmpty(workerId)) {
            throw new FlowableIllegalArgumentException("workerId is required");
        }

        ExternalWorkerJob job = getExternalWorkerJobById(jobId);

        if (!workerId.equals(job.getLockOwner())) {
            throw new FlowableForbiddenException(workerId + " does not hold a lock on the requested job");
        }

        if (job.getProcessInstanceId() != null) {
            if (managementService != null) {
                if (restApiInterceptor != null) {
                    restApiInterceptor.completeExternalWorkerJob(job, request);
                }

                managementService.createExternalWorkerCompletionBuilder(job.getId(), workerId)
                        .variables(extractVariables(request.getVariables()))
                        .complete();
            } else {
                throw new FlowableException("Cannot complete BPMN job. There is no BPMN engine available");
            }
        } else if (ScopeTypes.CMMN.equals(job.getScopeType())) {
            if (cmmnManagementService != null) {
                if (restApiInterceptor != null) {
                    restApiInterceptor.completeExternalWorkerJob(job, request);
                }

                cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(job.getId(), workerId)
                        .variables(extractVariables(request.getVariables()))
                        .complete();
            } else {
                throw new FlowableException("Cannot complete CMMN job. There is no CMMN engine available");
            }
        } else {
            throw new FlowableIllegalArgumentException(
                    "Can only complete BPMN or CMMN external job. Job with id '" + jobId + "' is from scope '" + job.getScopeType() + "'");
        }

        return ResponseEntity.noContent().build();
    }

    @ApiOperation(value = "Complete an External Worker Job with a BPMN Error", code = 204, tags = { "Acquire and Execute" })
    @ApiResponses({
            @ApiResponse(code = 204, message = "Indicates the job was successfully completed."),
            @ApiResponse(code = 400, message = "Indicates the request was invalid."),
            @ApiResponse(code = 403, message = "Indicates the user does not have the rights complete the job."),
            @ApiResponse(code = 404, message = "Indicates the job does not exist."),
    })
    @PostMapping(value = "/acquire/jobs/{jobId}/bpmnError", produces = "application/json")
    public ResponseEntity<?> bpmnErrorJob(@PathVariable String jobId, @RequestBody ExternalWorkerJobErrorRequest request) {
        String workerId = request.getWorkerId();
        if (StringUtils.isEmpty(workerId)) {
            throw new FlowableIllegalArgumentException("workerId is required");
        }

        ExternalWorkerJob job = getExternalWorkerJobById(jobId);

        if (!workerId.equals(job.getLockOwner())) {
            throw new FlowableForbiddenException(workerId + " does not hold a lock on the requested job");
        }

        if (job.getProcessInstanceId() != null) {
            if (managementService != null) {
                if (restApiInterceptor != null) {
                    restApiInterceptor.bpmnErrorExternalWorkerJob(job, request);
                }

                managementService.createExternalWorkerCompletionBuilder(job.getId(), workerId)
                        .variables(extractVariables(request.getVariables()))
                        .bpmnError(request.getErrorCode());
            } else {
                throw new FlowableException("Cannot complete BPMN job. There is no BPMN engine available");
            }
        } else {
            throw new FlowableIllegalArgumentException(
                    "Can only complete BPMN external job with a BPMN error. Job with id '" + jobId + "' is from scope '" + job.getScopeType() + "'");
        }

        return ResponseEntity.noContent().build();
    }

    @ApiOperation(value = "Complete an External Worker Job with a cmmn terminate transition", code = 204, tags = { "Acquire and Execute" })
    @ApiResponses({
            @ApiResponse(code = 204, message = "Indicates the job was successfully transitioned."),
            @ApiResponse(code = 400, message = "Indicates the request was invalid."),
            @ApiResponse(code = 403, message = "Indicates the user does not have the rights complete the job."),
            @ApiResponse(code = 404, message = "Indicates the job does not exist."),
    })
    @PostMapping(value = "/acquire/jobs/{jobId}/cmmnTerminate", produces = "application/json")
    public ResponseEntity<?> terminateCmmnJob(@PathVariable String jobId, @RequestBody ExternalWorkerJobTerminateRequest request) {
        String workerId = request.getWorkerId();
        if (StringUtils.isEmpty(workerId)) {
            throw new FlowableIllegalArgumentException("workerId is required");
        }

        ExternalWorkerJob job = getExternalWorkerJobById(jobId);

        if (!workerId.equals(job.getLockOwner())) {
            throw new FlowableForbiddenException(workerId + " does not hold a lock on the requested job");
        }

        if (ScopeTypes.CMMN.equals(job.getScopeType())) {
            if (cmmnManagementService != null) {
                if (restApiInterceptor != null) {
                    restApiInterceptor.cmmnTerminateExternalWorkerJob(job, request);
                }

                cmmnManagementService.createCmmnExternalWorkerTransitionBuilder(job.getId(), workerId)
                        .variables(extractVariables(request.getVariables()))
                        .terminate();
            } else {
                throw new FlowableException("Cannot complete CMMN job. There is no CMMN engine available");
            }
        } else {
            throw new FlowableIllegalArgumentException(
                    "Can only terminate CMMN external job. Job with id '" + jobId + "' is from scope '" + job.getScopeType() + "'");
        }

        return ResponseEntity.noContent().build();
    }

    @ApiOperation(value = "Fail an External Worker Job", code = 204, tags = { "Acquire and Execute" })
    @ApiResponses({
            @ApiResponse(code = 204, message = "Indicates the job was successfully completed."),
            @ApiResponse(code = 400, message = "Indicates the request was invalid."),
            @ApiResponse(code = 403, message = "Indicates the user does not have the rights complete the job."),
            @ApiResponse(code = 404, message = "Indicates the job does not exist."),
    })
    @PostMapping(value = "/acquire/jobs/{jobId}/fail", produces = "application/json")
    public ResponseEntity<?> failJob(@PathVariable String jobId, @RequestBody ExternalWorkerJobFailureRequest request) {
        String workerId = request.getWorkerId();
        if (StringUtils.isEmpty(workerId)) {
            throw new FlowableIllegalArgumentException("workerId is required");
        }

        ExternalWorkerJob job = getExternalWorkerJobById(jobId);

        if (!workerId.equals(job.getLockOwner())) {
            throw new FlowableForbiddenException(workerId + " does not hold a lock on the requested job");
        }

        ExternalWorkerJobFailureBuilder failureBuilder = createExternalWorkerJobFailureBuilder(jobId, workerId);

        if (request.getErrorMessage() != null) {
            failureBuilder.errorMessage(request.getErrorMessage());
        }

        if (request.getErrorDetails() != null) {
            failureBuilder.errorDetails(request.getErrorDetails());
        }

        if (request.getRetries() != null) {
            failureBuilder.retries(request.getRetries());
        }

        if (request.getRetryTimeout() != null) {
            failureBuilder.retryTimeout(request.getRetryTimeout());
        }

        if (restApiInterceptor != null) {
            restApiInterceptor.failExternalWorkerJob(job, request);
        }

        failureBuilder.fail();

        return ResponseEntity.noContent().build();
    }

    protected Map<String, Object> extractVariables(List<EngineRestVariable> restVariables) {
        if (restVariables != null && !restVariables.isEmpty()) {
            Map<String, Object> variables = new HashMap<>();

            for (EngineRestVariable restVariable : restVariables) {
                if (restVariable.getName() == null) {
                    throw new FlowableIllegalArgumentException("Variable name is required.");
                }

                variables.put(restVariable.getName(), restResponseFactory.getVariableValue(restVariable));
            }

            return variables;
        }

        return Collections.emptyMap();
    }

    protected ExternalWorkerJobAcquireBuilder createExternalWorkerAcquireBuilder() {
        if (managementService != null) {
            return managementService.createExternalWorkerJobAcquireBuilder();
        } else if (cmmnManagementService != null) {
            return cmmnManagementService.createExternalWorkerJobAcquireBuilder();
        } else {
            throw new FlowableException("Cannot acquire external jobs. There is no BPMN or CMMN engine available");
        }
    }

    protected ExternalWorkerJobFailureBuilder createExternalWorkerJobFailureBuilder(String jobId, String workerId) {
        if (managementService != null) {
            return managementService.createExternalWorkerJobFailureBuilder(jobId, workerId);
        } else if (cmmnManagementService != null) {
            return cmmnManagementService.createExternalWorkerJobFailureBuilder(jobId, workerId);
        } else {
            throw new FlowableException("Cannot fail external jobs. There is no BPMN or CMMN engine available");
        }
    }
}
