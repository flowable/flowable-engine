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

package org.flowable.compatibility.wrapper;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;

/**
 * Wraps an v5 task to an v6 {@link Task}.
 * 
 * @author Joram Barrez
 */
public class Flowable5TaskWrapper implements Task {

    private org.activiti.engine.task.Task activiti5Task;

    public Flowable5TaskWrapper(org.activiti.engine.task.Task activiti5Task) {
        this.activiti5Task = activiti5Task;
    }

    @Override
    public String getId() {
        return activiti5Task.getId();
    }

    @Override
    public String getName() {
        return activiti5Task.getName();
    }

    @Override
    public String getDescription() {
        return activiti5Task.getDescription();
    }

    @Override
    public int getPriority() {
        return activiti5Task.getPriority();
    }

    @Override
    public String getOwner() {
        return activiti5Task.getOwner();
    }

    @Override
    public String getAssignee() {
        return activiti5Task.getAssignee();
    }

    @Override
    public String getProcessInstanceId() {
        return activiti5Task.getProcessInstanceId();
    }

    @Override
    public String getExecutionId() {
        return activiti5Task.getExecutionId();
    }

    @Override
    public String getTaskDefinitionId() {
        return activiti5Task.getTaskDefinitionKey();
    }

    @Override
    public String getProcessDefinitionId() {
        return activiti5Task.getProcessDefinitionId();
    }

    @Override
    public String getScopeId() {
        return null;
    }

    @Override
    public String getSubScopeId() {
        return null;
    }

    @Override
    public String getScopeType() {
        return null;
    }

    @Override
    public String getScopeDefinitionId() {
        return null;
    }

    @Override
    public String getPropagatedStageInstanceId() {
        return null;
    }

    @Override
    public Date getCreateTime() {
        return activiti5Task.getCreateTime();
    }

    @Override
    public String getTaskDefinitionKey() {
        return activiti5Task.getTaskDefinitionKey();
    }

    @Override
    public Date getDueDate() {
        return activiti5Task.getDueDate();
    }

    @Override
    public String getCategory() {
        return activiti5Task.getCategory();
    }

    @Override
    public String getParentTaskId() {
        return activiti5Task.getParentTaskId();
    }

    @Override
    public String getTenantId() {
        return activiti5Task.getTenantId();
    }

    @Override
    public String getFormKey() {
        return activiti5Task.getFormKey();
    }

    @Override
    public Map<String, Object> getTaskLocalVariables() {
        return activiti5Task.getTaskLocalVariables();
    }

    @Override
    public Map<String, Object> getProcessVariables() {
        return activiti5Task.getProcessVariables();
    }

    @Override
    public Map<String, Object> getCaseVariables() {
        return null;
    }

    @Override
    public List<? extends IdentityLinkInfo> getIdentityLinks() {
        return null;
    }

    @Override
    public Date getClaimTime() {
        return null;
    }

    @Override
    public void setName(String name) {
        activiti5Task.setName(name);
    }

    @Override
    public void setLocalizedName(String name) {
        activiti5Task.setLocalizedName(name);
    }

    @Override
    public void setDescription(String description) {
        activiti5Task.setDescription(description);
    }

    @Override
    public void setLocalizedDescription(String description) {
        activiti5Task.setLocalizedDescription(description);
    }

    @Override
    public void setPriority(int priority) {
        activiti5Task.setPriority(priority);
    }

    @Override
    public void setOwner(String owner) {
        activiti5Task.setOwner(owner);
    }

    @Override
    public void setAssignee(String assignee) {
        activiti5Task.setAssignee(assignee);
    }

    @Override
    public DelegationState getDelegationState() {
        return activiti5Task.getDelegationState();
    }

    @Override
    public void setDelegationState(DelegationState delegationState) {
        activiti5Task.setDelegationState(delegationState);
    }

    @Override
    public void setDueDate(Date dueDate) {
        activiti5Task.setDueDate(dueDate);
    }

    @Override
    public void setCategory(String category) {
        activiti5Task.setCategory(category);
    }

    @Override
    public void setParentTaskId(String parentTaskId) {
        activiti5Task.setParentTaskId(parentTaskId);
    }

    @Override
    public void setTenantId(String tenantId) {
        activiti5Task.setTenantId(tenantId);
    }

    @Override
    public void setFormKey(String formKey) {
        activiti5Task.setFormKey(formKey);
    }

    @Override
    public boolean isSuspended() {
        return activiti5Task.isSuspended();
    }
}
