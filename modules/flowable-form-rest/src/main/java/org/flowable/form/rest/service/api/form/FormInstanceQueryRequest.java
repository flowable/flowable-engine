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

import org.flowable.common.rest.api.PaginateRequest;

/**
 * @author Yvo Swillens
 */
public class FormInstanceQueryRequest extends PaginateRequest {

    private String id;
    private String formDefinitionId;
    private String formDefinitionIdLike;
    private String taskId;
    private String taskIdLike;
    private String processInstanceId;
    private String processInstanceIdLike;
    private String processDefinitionId;
    private String processDefinitionIdLike;
    private String scopeId;
    private String scopeType;
    private String scopeDefinitionId;
    private String submittedBy;
    private String submittedByLike;
    private String tenantId;
    private String tenantIdLike;
    private Boolean withoutTenantId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFormDefinitionId() {
        return formDefinitionId;
    }

    public void setFormDefinitionId(String formDefinitionId) {
        this.formDefinitionId = formDefinitionId;
    }

    public String getFormDefinitionIdLike() {
        return formDefinitionIdLike;
    }

    public void setFormDefinitionIdLike(String formDefinitionIdLike) {
        this.formDefinitionIdLike = formDefinitionIdLike;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskIdLike() {
        return taskIdLike;
    }

    public void setTaskIdLike(String taskIdLike) {
        this.taskIdLike = taskIdLike;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getProcessInstanceIdLike() {
        return processInstanceIdLike;
    }

    public void setProcessInstanceIdLike(String processInstanceIdLike) {
        this.processInstanceIdLike = processInstanceIdLike;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getProcessDefinitionIdLike() {
        return processDefinitionIdLike;
    }

    public void setProcessDefinitionIdLike(String processDefinitionIdLike) {
        this.processDefinitionIdLike = processDefinitionIdLike;
    }

    public String getScopeId() {
        return scopeId;
    }

    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    public void setScopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }

    public String getSubmittedByLike() {
        return submittedByLike;
    }

    public void setSubmittedByLike(String submittedByLike) {
        this.submittedByLike = submittedByLike;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantIdLike() {
        return tenantIdLike;
    }

    public void setTenantIdLike(String tenantIdLike) {
        this.tenantIdLike = tenantIdLike;
    }

    public Boolean isWithoutTenantId() {
        return withoutTenantId;
    }

    public void setWithoutTenantId(Boolean withoutTenantId) {
        this.withoutTenantId = withoutTenantId;
    }
}
