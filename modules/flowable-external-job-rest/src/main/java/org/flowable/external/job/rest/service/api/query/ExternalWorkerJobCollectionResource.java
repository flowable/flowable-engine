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
package org.flowable.external.job.rest.service.api.query;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.common.rest.api.PaginateListUtil;
import org.flowable.external.job.rest.service.api.ExternalJobRestResponseFactory;
import org.flowable.external.job.rest.service.api.ExternalWorkerJobBaseResource;
import org.flowable.job.api.ExternalWorkerJobQuery;
import org.flowable.job.service.impl.JobQueryProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * @author Filip Hrisafov
 */
@RestController
@Api(tags = { "Info and Query" })
public class ExternalWorkerJobCollectionResource extends ExternalWorkerJobBaseResource {

    public static final Map<String, QueryProperty> PROPERTIES;

    static {
        PROPERTIES = new HashMap<>();
        PROPERTIES.put("id", JobQueryProperty.JOB_ID);
        PROPERTIES.put("dueDate", JobQueryProperty.DUEDATE);
        PROPERTIES.put("createTime", JobQueryProperty.CREATE_TIME);
        PROPERTIES.put("executionId", JobQueryProperty.EXECUTION_ID);
        PROPERTIES.put("processInstanceId", JobQueryProperty.PROCESS_INSTANCE_ID);
        PROPERTIES.put("retries", JobQueryProperty.RETRIES);
        PROPERTIES.put("tenantId", JobQueryProperty.TENANT_ID);
    }

    protected final ExternalJobRestResponseFactory restResponseFactory;

    public ExternalWorkerJobCollectionResource(ExternalJobRestResponseFactory restResponseFactory) {
        this.restResponseFactory = restResponseFactory;
    }

    @ApiOperation(value = "List External Worker Jobs", tags = { "Info and Query" })
    @ApiResponses({
            @ApiResponse(code = 200, message = "Indicates the requested jobs were returned."),
            @ApiResponse(code = 400, message = "Indicates an illegal value has been used in a url query parameter. Status description contains additional details about the error."),
            @ApiResponse(code = 403, message = "Indicates the user does not have the rights to query for external worker jobs."),
    })
    @GetMapping(value = "/jobs", produces = "application/json")
    public DataResponse<ExternalWorkerJobResponse> listExternalWorkerJobs(@ModelAttribute ExternalWorkerJobQueryRequest request) {

        ExternalWorkerJobQuery query = createExternalWorkerJobQuery();

        if (request.getId() != null) {
            query.jobId(request.getId());
        }

        if (request.getProcessInstanceId() != null) {
            query.processInstanceId(request.getProcessInstanceId());
        }
        
        if (request.isWithoutProcessInstanceId()) {
            query.withoutProcessInstanceId();
        }

        if (request.getExecutionId() != null) {
            query.executionId(request.getExecutionId());
        }

        if (request.getProcessDefinitionId() != null) {
            query.processDefinitionId(request.getProcessDefinitionId());
        }

        if (request.getScopeId() != null) {
            query.scopeId(request.getScopeId());
        }
        
        if (request.isWithoutScopeId()) {
            query.withoutScopeId();
        }

        if (request.getSubScopeId() != null) {
            query.subScopeId(request.getSubScopeId());
        }

        if (request.getScopeDefinitionId() != null) {
            query.scopeDefinitionId(request.getScopeDefinitionId());
        }

        if (request.getScopeType() != null) {
            query.scopeType(request.getScopeType());
        }

        if (request.getElementId() != null) {
            query.elementId(request.getElementId());
        }

        if (request.getElementName() != null) {
            query.elementName(request.getElementName());
        }

        if (request.isWithException()) {
            query.withException();
        }

        if (request.getExceptionMessage() != null) {
            query.exceptionMessage(request.getExceptionMessage());
        }

        if (request.getTenantId() != null) {
            query.jobTenantId(request.getTenantId());
        }

        if (request.getTenantIdLike() != null) {
            query.jobTenantIdLike(request.getTenantIdLike());
        }

        if (request.isWithoutTenantId()) {
            query.jobWithoutTenantId();
        }

        if (request.isLocked()) {
            query.locked();
        }

        if (request.isUnlocked()) {
            query.unlocked();
        }
        if (request.isWithoutScopeType()) {
            query.withoutScopeType();
        }

        if (restApiInterceptor != null) {
            restApiInterceptor.accessExternalWorkerJobInfoWithQuery(query, request);
        }

        return PaginateListUtil.paginateList(request, query, "id", PROPERTIES, restResponseFactory::createExternalWorkerJobResponseList);
    }

}
