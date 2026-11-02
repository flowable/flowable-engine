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

package org.flowable.rest.service.api.runtime.process;

import static org.flowable.common.rest.api.PaginateListUtil.paginateList;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.ActivityInstanceQueryProperty;
import org.flowable.engine.runtime.ActivityInstanceQuery;
import org.flowable.rest.service.api.BpmnRestApiInterceptor;
import org.flowable.rest.service.api.RestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Tijs Rademakers
 */
public class ActivityInstanceBaseResource {

    private static Map<String, QueryProperty> allowedSortProperties = new HashMap<>();

    static {
        allowedSortProperties.put("activityId", ActivityInstanceQueryProperty.ACTIVITY_ID);
        allowedSortProperties.put("activityName", ActivityInstanceQueryProperty.ACTIVITY_NAME);
        allowedSortProperties.put("activityType", ActivityInstanceQueryProperty.ACTIVITY_TYPE);
        allowedSortProperties.put("duration", ActivityInstanceQueryProperty.DURATION);
        allowedSortProperties.put("endTime", ActivityInstanceQueryProperty.END);
        allowedSortProperties.put("executionId", ActivityInstanceQueryProperty.EXECUTION_ID);
        allowedSortProperties.put("activityInstanceId", ActivityInstanceQueryProperty.ACTIVITY_INSTANCE_ID);
        allowedSortProperties.put("processDefinitionId", ActivityInstanceQueryProperty.PROCESS_DEFINITION_ID);
        allowedSortProperties.put("processInstanceId", ActivityInstanceQueryProperty.PROCESS_INSTANCE_ID);
        allowedSortProperties.put("startTime", ActivityInstanceQueryProperty.START);
        allowedSortProperties.put("tenantId", ActivityInstanceQueryProperty.TENANT_ID);
    }

    @Autowired
    protected RestResponseFactory restResponseFactory;

    @Autowired
    protected RuntimeService runtimeService;
    
    @Autowired(required=false)
    protected BpmnRestApiInterceptor restApiInterceptor;

    protected DataResponse<ActivityInstanceResponse> getQueryResponse(ActivityInstanceQueryRequest queryRequest, Map<String, String> allRequestParams) {
        ActivityInstanceQuery query = runtimeService.createActivityInstanceQuery();

        // Populate query based on request
        if (queryRequest.getActivityId() != null) {
            query.activityId(queryRequest.getActivityId());
        }

        if (queryRequest.getActivityInstanceId() != null) {
            query.activityInstanceId(queryRequest.getActivityInstanceId());
        }

        if (queryRequest.getActivityName() != null) {
            query.activityName(queryRequest.getActivityName());
        }

        if (queryRequest.getActivityType() != null) {
            query.activityType(queryRequest.getActivityType());
        }

        if (queryRequest.getExecutionId() != null) {
            query.executionId(queryRequest.getExecutionId());
        }

        if (queryRequest.getFinished() != null) {
            Boolean finished = queryRequest.getFinished();
            if (finished) {
                query.finished();
            } else {
                query.unfinished();
            }
        }

        if (queryRequest.getTaskAssignee() != null) {
            query.taskAssignee(queryRequest.getTaskAssignee());
        }
        
        if (queryRequest.getTaskCompletedBy() != null) {
            query.taskCompletedBy(queryRequest.getTaskCompletedBy());
        }

        if (queryRequest.getProcessInstanceId() != null) {
            query.processInstanceId(queryRequest.getProcessInstanceId());
        }

        if (queryRequest.getProcessInstanceIds() != null && !queryRequest.getProcessInstanceIds().isEmpty()) {
            query.processInstanceIds(queryRequest.getProcessInstanceIds());
        }

        if (queryRequest.getProcessDefinitionId() != null) {
            query.processDefinitionId(queryRequest.getProcessDefinitionId());
        }

        if (queryRequest.getTenantId() != null) {
            query.activityTenantId(queryRequest.getTenantId());
        }

        if (queryRequest.getTenantIdLike() != null) {
            query.activityTenantIdLike(queryRequest.getTenantIdLike());
        }

        if (Boolean.TRUE.equals(queryRequest.getWithoutTenantId())) {
            query.activityWithoutTenantId();
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessActivityInfoWithQuery(query, queryRequest);
        }

        return paginateList(allRequestParams, queryRequest, query, "startTime", allowedSortProperties,
            restResponseFactory::createActivityInstanceResponseList);
    }
}
