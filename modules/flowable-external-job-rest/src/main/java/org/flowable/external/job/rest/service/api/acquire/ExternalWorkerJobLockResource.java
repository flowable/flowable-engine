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

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableForbiddenException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.AbstractServiceConfiguration;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;
import org.flowable.common.rest.exception.FlowableConflictException;
import org.flowable.external.job.rest.service.api.ExternalWorkerJobBaseResource;
import org.flowable.job.api.ExternalWorkerJob;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.cmd.ExtendExternalWorkerJobLockCmd;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@Api(tags = { "Acquire and Execute" })
public class ExternalWorkerJobLockResource extends ExternalWorkerJobBaseResource {

    @ApiOperation(value = "Extend an External Worker Job Lock", tags = { "Acquire and Execute" })
    @ApiResponses({
            @ApiResponse(code = 200, message = "Indicates the external-worker lock was extended."),
            @ApiResponse(code = 400, message = "Indicates the request was invalid."),
            @ApiResponse(code = 403, message = "Indicates the worker does not hold the lock."),
            @ApiResponse(code = 404, message = "Indicates the job does not exist."),
            @ApiResponse(code = 409, message = "Indicates the lock expired or changed concurrently."),
    })
    @PostMapping(value = "/acquire/jobs/{jobId}/extend-lock", produces = "application/json")
    public ExternalWorkerJobLockExtensionResponse extendLock(
            @PathVariable String jobId,
            @RequestBody ExternalWorkerJobLockExtensionRequest request) {

        validateRequest(request);
        ExternalWorkerJob job = getExternalWorkerJobById(jobId);
        if (!request.getWorkerId().equals(job.getLockOwner())) {
            throw new FlowableForbiddenException(request.getWorkerId() + " does not hold a lock on the requested job");
        }
        if (restApiInterceptor != null) {
            restApiInterceptor.extendExternalWorkerJobLock(job, request);
        }

        CommonEngineServiceImpl<?> engineService = selectEngineService(job);
        JobServiceConfiguration jobServiceConfiguration = requireJobServiceConfiguration(engineService);
        try {
            Date lockExpirationTime = engineService.getCommandExecutor().execute(new ExtendExternalWorkerJobLockCmd(
                    jobId,
                    request.getWorkerId(),
                    request.getLockDuration(),
                    request.getExpectedLockExpirationTime(),
                    jobServiceConfiguration));
            return new ExternalWorkerJobLockExtensionResponse(jobId, request.getWorkerId(), lockExpirationTime);
        } catch (FlowableOptimisticLockingException exception) {
            throw new FlowableConflictException(exception.getMessage());
        }
    }

    protected void validateRequest(ExternalWorkerJobLockExtensionRequest request) {
        if (request == null) {
            throw new FlowableIllegalArgumentException("Request body is required");
        }
        if (StringUtils.isEmpty(request.getWorkerId())) {
            throw new FlowableIllegalArgumentException("workerId is required");
        }
        if (request.getLockDuration() == null
                || request.getLockDuration().isZero()
                || request.getLockDuration().isNegative()) {
            throw new FlowableIllegalArgumentException("lockDuration must be positive");
        }
        if (request.getExpectedLockExpirationTime() == null) {
            throw new FlowableIllegalArgumentException("expectedLockExpirationTime is required");
        }
    }

    protected CommonEngineServiceImpl<?> selectEngineService(ExternalWorkerJob job) {
        Object selectedService;
        if (job.getProcessInstanceId() != null) {
            selectedService = managementService;
            if (selectedService == null) {
                throw new FlowableException("Cannot extend BPMN external job lock. There is no BPMN engine available");
            }
        } else if (ScopeTypes.CMMN.equals(job.getScopeType())) {
            selectedService = cmmnManagementService;
            if (selectedService == null) {
                throw new FlowableException("Cannot extend CMMN external job lock. There is no CMMN engine available");
            }
        } else {
            throw new FlowableIllegalArgumentException(
                    "Can only extend BPMN or CMMN external job locks. Job with id '" + job.getId()
                            + "' is from scope '" + job.getScopeType() + "'");
        }

        if (selectedService instanceof CommonEngineServiceImpl<?> engineService) {
            return engineService;
        }
        throw new FlowableException("The selected engine management service does not expose command execution");
    }

    protected JobServiceConfiguration requireJobServiceConfiguration(CommonEngineServiceImpl<?> engineService) {
        AbstractServiceConfiguration<?> serviceConfiguration = engineService.getConfiguration()
                .getServiceConfigurations()
                .get(EngineConfigurationConstants.KEY_JOB_SERVICE_CONFIG);
        if (serviceConfiguration instanceof JobServiceConfiguration jobServiceConfiguration) {
            return jobServiceConfiguration;
        }
        throw new FlowableException("The selected engine does not expose a job service configuration");
    }
}
