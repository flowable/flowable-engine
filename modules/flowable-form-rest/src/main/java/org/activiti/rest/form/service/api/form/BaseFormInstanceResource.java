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
package org.activiti.rest.form.service.api.form;

import org.activiti.engine.query.QueryProperty;
import org.activiti.form.api.FormInstanceQuery;
import org.activiti.form.api.FormService;
import org.activiti.form.engine.impl.SubmittedFormQueryProperty;
import org.activiti.rest.api.DataResponse;
import org.activiti.rest.form.FormRestResponseFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yvo Swillens
 */
public class BaseFormInstanceResource {

  private static Map<String, QueryProperty> allowedSortProperties = new HashMap<>();

  static {
    allowedSortProperties.put("submittedDate", SubmittedFormQueryProperty.SUBMITTED_DATE);
    allowedSortProperties.put("tenantId", SubmittedFormQueryProperty.TENANT_ID);
  }

  @Autowired
  protected FormService formService;

  @Autowired
  protected FormRestResponseFactory restResponseFactory;

  protected DataResponse getQueryResponse(FormInstanceQueryRequest queryRequest, Map<String, String> requestParams) {

    FormInstanceQuery query = formService.createFormInstanceQuery();

    if (queryRequest.getFormDefinitionId() != null) {
      query.formDefinitionId(queryRequest.getFormDefinitionId());
    }

    if (queryRequest.getTaskId() != null) {
      query.taskId(queryRequest.getTaskId());
    }

    if (queryRequest.getProcessInstanceId() != null) {
      query.processInstanceId(queryRequest.getProcessInstanceId());
    }

    if (queryRequest.getProcessDefinitionId() != null) {
      query.processDefinitionId(queryRequest.getProcessDefinitionId());
    }

    if (queryRequest.getSubmittedDate() != null) {
      query.submittedDate(queryRequest.getSubmittedDate());
    }

    if (queryRequest.getSubmittedBy() != null) {
      query.submittedBy(queryRequest.getSubmittedBy());
    }

    if (queryRequest.getTenantId() != null) {
      query.deploymentTenantId(queryRequest.getTenantId());
    }

    return new FormInstancePaginateList(restResponseFactory).paginateList(requestParams, queryRequest, query, "submittedDate", allowedSortProperties);
  }

}
