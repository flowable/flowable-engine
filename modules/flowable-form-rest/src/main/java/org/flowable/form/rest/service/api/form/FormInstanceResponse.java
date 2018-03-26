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

import java.util.Date;

import org.flowable.form.api.FormInstance;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Yvo Swillens
 */
public class FormInstanceResponse {

    private String id;
    private String formDefinitionId;
    private String taskId;
    private String processInstanceId;
    private String processDefinitionId;
    private String scopeId;
    private String scopeType;
    private String scopeDefinitionId;
    private Date submittedDate;
    private String submittedBy;
    private String formValuesId;
    private String tenantId;
    private String url;

    public FormInstanceResponse(FormInstance formInstance) {
        this.id = formInstance.getId();
        this.formDefinitionId = formInstance.getFormDefinitionId();
        this.taskId = formInstance.getTaskId();
        this.processInstanceId = formInstance.getProcessInstanceId();
        this.processDefinitionId = formInstance.getProcessDefinitionId();
        this.scopeId = formInstance.getScopeId();
        this.scopeType = formInstance.getScopeType();
        this.scopeDefinitionId = formInstance.getScopeDefinitionId();
        this.submittedDate = formInstance.getSubmittedDate();
        this.submittedBy = formInstance.getSubmittedBy();
        this.formValuesId = formInstance.getFormValuesId();
        this.tenantId = formInstance.getTenantId();
    }

    @ApiModelProperty(example = "48b9ac82-f1d3-11e6-8549-acde48001122")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(example = "818e4703-f1d2-11e6-8549-acde48001122")
    public String getFormDefinitionId() {
        return formDefinitionId;
    }

    public void setFormDefinitionId(String formDefinitionId) {
        this.formDefinitionId = formDefinitionId;
    }

    @ApiModelProperty(example = "88")
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    @ApiModelProperty(example = "66")
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @ApiModelProperty(example = "oneTaskProcess:1:158")
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @ApiModelProperty(example = "243")
    public String getScopeId() {
        return scopeId;
    }

    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    @ApiModelProperty(example = "cmmn")
    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    @ApiModelProperty(example = "caseDef:1:244")
    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    public void setScopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
    }

    @ApiModelProperty(example = "2013-04-17T10:17:43.902+0000")
    public Date getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(Date submittedDate) {
        this.submittedDate = submittedDate;
    }

    @ApiModelProperty(example = "testUser")
    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }

    @ApiModelProperty(example = "818e4703-f1d2-11e6-8549-acde48001110")
    public String getFormValuesId() {
        return formValuesId;
    }

    public void setFormValuesId(String formValuesId) {
        this.formValuesId = formValuesId;
    }

    @ApiModelProperty(example = "null")
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @ApiModelProperty(example = "http://localhost:8182/form/form-instances/48b9ac82-f1d3-11e6-8549-acde48001122")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
