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

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormInstanceInfo;
import org.flowable.form.model.FormField;
import org.flowable.form.model.FormOutcome;
import org.flowable.form.model.SimpleFormModel;

/**
 * @author Yvo Swillens
 */
public class FormInstanceModelResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    protected String id;
    protected String name;
    protected String description;
    protected String key;
    protected int version;
    protected String formInstanceId;
    protected String submittedBy;
    protected Date submittedDate;
    protected String selectedOutcome;
    protected String taskId;
    protected String processInstanceId;
    protected String processDefinitionId;
    protected String tenantId;
    protected String url;
    protected List<FormField> fields;
    protected List<FormOutcome> outcomes;
    protected String outcomeVariableName;

    public FormInstanceModelResponse(FormInfo formInfo, String url) {
        this.id = formInfo.getId();
        this.name = formInfo.getName();
        this.key = formInfo.getKey();
        this.version = formInfo.getVersion();
        this.description = formInfo.getDescription();
        this.url = url;
        
        SimpleFormModel formModel = (SimpleFormModel) formInfo.getFormModel();
        this.fields = formModel.getFields();
        this.outcomes = formModel.getOutcomes();
        this.outcomeVariableName = formModel.getOutcomeVariableName();
    }

    public FormInstanceModelResponse(FormInstanceInfo formInstanceModel) {
        this(formInstanceModel, null);
        
        setFormInstanceId(formInstanceModel.getFormInstanceId());
        setSubmittedBy(formInstanceModel.getSubmittedBy());
        setSubmittedDate(formInstanceModel.getSubmittedDate());
        setSelectedOutcome(formInstanceModel.getSelectedOutcome());
        setTaskId(formInstanceModel.getTaskId());
        setProcessInstanceId(formInstanceModel.getProcessInstanceId());
        setProcessDefinitionId(formInstanceModel.getProcessDefinitionId());
        setTenantId(formInstanceModel.getTenantId());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getFormInstanceId() {
        return formInstanceId;
    }

    public void setFormInstanceId(String formInstanceId) {
        this.formInstanceId = formInstanceId;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }

    public Date getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(Date submittedDate) {
        this.submittedDate = submittedDate;
    }

    public String getSelectedOutcome() {
        return selectedOutcome;
    }

    public void setSelectedOutcome(String selectedOutcome) {
        this.selectedOutcome = selectedOutcome;
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

    public List<FormField> getFields() {
        return fields;
    }

    public void setFields(List<FormField> fields) {
        this.fields = fields;
    }

    public List<FormOutcome> getOutcomes() {
        return outcomes;
    }

    public void setOutcomes(List<FormOutcome> outcomes) {
        this.outcomes = outcomes;
    }

    public String getOutcomeVariableName() {
        return outcomeVariableName;
    }

    public void setOutcomeVariableName(String outcomeVariableName) {
        this.outcomeVariableName = outcomeVariableName;
    }
}
