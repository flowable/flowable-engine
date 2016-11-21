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

import org.activiti.form.api.FormInstance;

import java.util.Date;

/**
 * @author Yvo Swillens
 */
public class FormInstanceResponse {

  private String id;
  private String formDefinitionId;
  private String taskId;
  private String processInstanceId;
  private String processDefinitionId;
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
    this.submittedDate = formInstance.getSubmittedDate();
    this.submittedBy = formInstance.getSubmittedBy();
    this.formValuesId = formInstance.getFormValuesId();
    this.tenantId = formInstance.getTenantId();
  }

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

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public String getProcessInstanceId() {
    return processInstanceId;
  }

  public void setProcessInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  public String getProcessDefinitionId() {
    return processDefinitionId;
  }

  public void setProcessDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  public Date getSubmittedDate() {
    return submittedDate;
  }

  public void setSubmittedDate(Date submittedDate) {
    this.submittedDate = submittedDate;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public void setSubmittedBy(String submittedBy) {
    this.submittedBy = submittedBy;
  }

  public String getFormValuesId() {
    return formValuesId;
  }

  public void setFormValuesId(String formValuesId) {
    this.formValuesId = formValuesId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}
