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

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableForbiddenException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.external.job.rest.service.api.ExternalJobRestResponseFactory;
import org.flowable.external.job.rest.service.api.ExternalWorkerJobBaseResource;
import org.flowable.job.api.ExternalWorkerJob;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@Api(tags = { "Unacquire" })
public class ExternalWorkerUnacquireJobResource extends ExternalWorkerJobBaseResource {

    protected final ExternalJobRestResponseFactory restResponseFactory;

    public ExternalWorkerUnacquireJobResource(ExternalJobRestResponseFactory restResponseFactory) {
        this.restResponseFactory = restResponseFactory;
    }

    @ApiOperation(value = "Unacquire External Worker Jobs", tags = { "Unacquire" })
    @ApiResponses({
            @ApiResponse(code = 204, message = "Indicates the jobs were unacquired."),
            @ApiResponse(code = 400, message = "Indicates the request was invalid."),
            @ApiResponse(code = 403, message = "Indicates the user does not have the rights to unacquire the jobs."),
    })
    @PostMapping(value = "/unacquire/jobs", produces = "application/json")
    public ResponseEntity<?> unacquireJobs(@RequestBody UnacquireExternalWorkerJobsRequest request) {
        if (restApiInterceptor != null) {
            restApiInterceptor.accessUnacquireExternalWorkerJobs(request);
        }

        if (StringUtils.isEmpty(request.getWorkerId())) {
            throw new FlowableIllegalArgumentException("worker id is required");
        }

        unaquireExternalWorkerJobs(request.getWorkerId(), request.getTenantId());
        
        return ResponseEntity.noContent().build();
    }

    @ApiOperation(value = "Unaquire an External Worker Job", code = 204, tags = { "Unacquire" })
    @ApiResponses({
            @ApiResponse(code = 204, message = "Indicates the job was successfully unaquired."),
            @ApiResponse(code = 400, message = "Indicates the request was invalid."),
            @ApiResponse(code = 403, message = "Indicates the user does not have the rights to unacquire the job."),
            @ApiResponse(code = 404, message = "Indicates the job does not exist."),
    })
    @PostMapping(value = "/unacquire/jobs/{jobId}", produces = "application/json")
    public ResponseEntity<?> unaquireJob(@PathVariable String jobId, @RequestBody UnacquireExternalWorkerJobsRequest request) {
        String workerId = request.getWorkerId();
        if (StringUtils.isEmpty(workerId)) {
            throw new FlowableIllegalArgumentException("worker id is required");
        }

        ExternalWorkerJob job = getExternalWorkerJobById(jobId);

        if (!workerId.equals(job.getLockOwner())) {
            throw new FlowableForbiddenException(workerId + " does not hold a lock on the requested job");
        }

        if (job.getProcessInstanceId() != null) {
            if (managementService != null) {
                if (restApiInterceptor != null) {
                    restApiInterceptor.unacquireExternalWorkerJob(job, request);
                }

                managementService.unacquireExternalWorkerJob(jobId, workerId);
                
            } else {
                throw new FlowableException("Cannot unacquire BPMN job. There is no BPMN engine available");
            }
        } else if (ScopeTypes.CMMN.equals(job.getScopeType())) {
            if (cmmnManagementService != null) {
                if (restApiInterceptor != null) {
                    restApiInterceptor.unacquireExternalWorkerJob(job, request);
                }

                cmmnManagementService.unacquireExternalWorkerJob(jobId, workerId);
                
            } else {
                throw new FlowableException("Cannot unacquire CMMN job. There is no CMMN engine available");
            }
            
        } else {
            throw new FlowableIllegalArgumentException(
                    "Can only unacquire BPMN or CMMN external job. Job with id '" + jobId + "' is from scope '" + job.getScopeType() + "'");
        }

        return ResponseEntity.noContent().build();
    }
    
    protected void unaquireExternalWorkerJobs(String workerId, String tenantId) {
        if (managementService != null) {
            if (StringUtils.isNotEmpty(tenantId)) {
                managementService.unacquireAllExternalWorkerJobsForWorker(workerId, tenantId);
            } else {
                managementService.unacquireAllExternalWorkerJobsForWorker(workerId);
            }
            
        } else if (cmmnManagementService != null) {
            if (StringUtils.isNotEmpty(tenantId)) {
                cmmnManagementService.unacquireAllExternalWorkerJobsForWorker(workerId, tenantId);
            } else {
                cmmnManagementService.unacquireAllExternalWorkerJobsForWorker(workerId);
            }
            
        } else {
            throw new FlowableException("Cannot unacquire external jobs. There is no BPMN or CMMN engine available");
        }
    }
}
