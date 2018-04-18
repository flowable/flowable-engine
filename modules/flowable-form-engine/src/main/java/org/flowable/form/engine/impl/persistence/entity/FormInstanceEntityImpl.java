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
package org.flowable.form.engine.impl.persistence.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntityNoRevision;
import org.flowable.form.engine.FormEngineConfiguration;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class FormInstanceEntityImpl extends AbstractEntityNoRevision implements FormInstanceEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected String formDefinitionId;
    protected String taskId;
    protected String processInstanceId;
    protected String processDefinitionId;
    protected String scopeId;
    protected String scopeType;
    protected String scopeDefinitionId;
    protected Date submittedDate;
    protected String submittedBy;
    protected String formValuesId;
    protected ResourceRef resourceRef;
    protected String tenantId = FormEngineConfiguration.NO_TENANT_ID;

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        if (resourceRef != null && resourceRef.getId() != null) {
            persistentState.put("formValuesId", resourceRef.getId());
        }
        return persistentState;
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    @Override
    public String getFormDefinitionId() {
        return formDefinitionId;
    }

    @Override
    public void setFormDefinitionId(String formDefinitionId) {
        this.formDefinitionId = formDefinitionId;
    }

    @Override
    public String getTaskId() {
        return taskId;
    }

    @Override
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    @Override
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    @Override
    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    public String getScopeId() {
        return scopeId;
    }

    @Override
    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    @Override
    public String getScopeType() {
        return scopeType;
    }

    @Override
    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    @Override
    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    @Override
    public void setScopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
    }

    @Override
    public Date getSubmittedDate() {
        return submittedDate;
    }

    @Override
    public void setSubmittedDate(Date submittedDate) {
        this.submittedDate = submittedDate;
    }

    @Override
    public String getSubmittedBy() {
        return submittedBy;
    }

    @Override
    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }

    @Override
    public String getFormValuesId() {
        return formValuesId;
    }

    @Override
    public void setFormValuesId(String formValuesId) {
        this.formValuesId = formValuesId;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public byte[] getFormValueBytes() {
        ensureResourceRefInitialized();
        return resourceRef.getBytes();
    }

    @Override
    public void setFormValueBytes(byte[] bytes) {
        ensureResourceRefInitialized();
        resourceRef.setValue("form-" + formDefinitionId, bytes);
    }

    public ResourceRef getResourceRef() {
        return resourceRef;
    }

    protected void ensureResourceRefInitialized() {
        if (resourceRef == null) {
            resourceRef = new ResourceRef();
        }
    }

    @Override
    public String toString() {
        return "SubmittedFormEntity[" + id + "]";
    }

}
