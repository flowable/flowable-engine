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
package org.flowable.form.rest.service.api.form;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.form.api.FormInstanceQuery;
import org.flowable.form.api.FormService;
import org.flowable.form.engine.impl.FormInstanceQueryProperty;
import org.flowable.form.rest.FormRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Yvo Swillens
 */
public class BaseFormInstanceResource {

    private static Map<String, QueryProperty> allowedSortProperties = new HashMap<>();

    static {
        allowedSortProperties.put("submittedDate", FormInstanceQueryProperty.SUBMITTED_DATE);
        allowedSortProperties.put("tenantId", FormInstanceQueryProperty.TENANT_ID);
    }

    @Autowired
    protected FormService formService;

    @Autowired
    protected FormRestResponseFactory restResponseFactory;

    protected DataResponse<FormInstanceResponse> getQueryResponse(FormInstanceQueryRequest queryRequest, Map<String, String> requestParams) {

        FormInstanceQuery query = formService.createFormInstanceQuery();

        if (queryRequest.getFormDefinitionId() != null) {
            query.formDefinitionId(queryRequest.getFormDefinitionId());
        }
        if (queryRequest.getFormDefinitionIdLike() != null) {
            query.formDefinitionIdLike(queryRequest.getFormDefinitionIdLike());
        }
        if (queryRequest.getTaskId() != null) {
            query.taskId(queryRequest.getTaskId());
        }
        if (queryRequest.getTaskIdLike() != null) {
            query.taskIdLike(queryRequest.getTaskIdLike());
        }
        if (queryRequest.getProcessInstanceId() != null) {
            query.processInstanceId(queryRequest.getProcessInstanceId());
        }
        if (queryRequest.getProcessInstanceIdLike() != null) {
            query.processInstanceIdLike(queryRequest.getProcessInstanceIdLike());
        }
        if (queryRequest.getProcessDefinitionId() != null) {
            query.processDefinitionId(queryRequest.getProcessDefinitionId());
        }
        if (queryRequest.getProcessDefinitionIdLike() != null) {
            query.processDefinitionIdLike(queryRequest.getProcessDefinitionIdLike());
        }
        if (queryRequest.getScopeId() != null) {
            query.scopeId(queryRequest.getScopeId());
        }
        if (queryRequest.getScopeType() != null) {
            query.scopeType(queryRequest.getScopeType());
        }
        if (queryRequest.getScopeDefinitionId() != null) {
            query.scopeDefinitionId(queryRequest.getScopeDefinitionId());
        }
        if (queryRequest.getSubmittedBy() != null) {
            query.submittedBy(queryRequest.getSubmittedBy());
        }
        if (queryRequest.getSubmittedByLike() != null) {
            query.submittedByLike(queryRequest.getSubmittedByLike());
        }
        if (queryRequest.getTenantId() != null) {
            query.deploymentTenantId(queryRequest.getTenantId());
        }
        if (queryRequest.getTenantIdLike() != null) {
            query.deploymentTenantIdLike(queryRequest.getTenantIdLike());
        }
        if (Boolean.TRUE.equals(queryRequest.isWithoutTenantId())) {
            query.deploymentWithoutTenantId();
        }

        return new FormInstancePaginateList(restResponseFactory).paginateList(requestParams, queryRequest, query, "submittedDate", allowedSortProperties);
    }

}
