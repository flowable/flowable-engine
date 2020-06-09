/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.engine.impl.persistence.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author martin.grofcik
 */
public class ActivityInstanceEntityImpl extends AbstractBpmnEngineEntity implements ActivityInstanceEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected String processInstanceId;
    protected String processDefinitionId;
    protected Date startTime;
    protected Date endTime;
    protected Long durationInMillis;
    protected Integer transactionOrder;
    protected String deleteReason;

    protected String activityId;
    protected String activityName;
    protected String activityType;
    protected String executionId;
    protected String assignee;
    protected String taskId;
    protected String calledProcessInstanceId;
    protected String tenantId = ProcessEngineConfiguration.NO_TENANT_ID;

    public ActivityInstanceEntityImpl() {

    }

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("endTime", endTime);
        persistentState.put("transactionOrder", transactionOrder);
        persistentState.put("durationInMillis", durationInMillis);
        persistentState.put("deleteReason", deleteReason);
        persistentState.put("executionId", executionId);
        persistentState.put("taskId", taskId);
        persistentState.put("assignee", assignee);
        persistentState.put("calledProcessInstanceId", calledProcessInstanceId);
        persistentState.put("activityId", activityId);
        persistentState.put("activityName", activityName);
        return persistentState;
    }

    @Override
    public void markEnded(String deleteReason) {
        if (this.endTime == null) {
            this.deleteReason = deleteReason;
            this.endTime = CommandContextUtil.getProcessEngineConfiguration().getClock().getCurrentTime();
            if (endTime != null && startTime != null) {
                this.durationInMillis = endTime.getTime() - startTime.getTime();
            }
        }
    }

    // getters and setters ////////////////////////////////////////////////////////

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    @Override
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    @Override
    public Date getStartTime() {
        return startTime;
    }

    @Override
    public Date getEndTime() {
        return endTime;
    }

    @Override
    public Long getDurationInMillis() {
        return durationInMillis;
    }

    @Override
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @Override
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    @Override
    public void setDurationInMillis(Long durationInMillis) {
        this.durationInMillis = durationInMillis;
    }
    
    @Override
    public Integer getTransactionOrder() {
        return transactionOrder;
    }

    @Override
    public void setTransactionOrder(Integer transactionOrder) {
        this.transactionOrder = transactionOrder;
    }

    @Override
    public String getDeleteReason() {
        return deleteReason;
    }

    @Override
    public void setDeleteReason(String deleteReason) {
        this.deleteReason = deleteReason;
    }

    @Override
    public String getActivityId() {
        return activityId;
    }

    @Override
    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    @Override
    public String getActivityName() {
        return activityName;
    }

    @Override
    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    @Override
    public String getActivityType() {
        return activityType;
    }

    @Override
    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    @Override
    public String getExecutionId() {
        return executionId;
    }

    @Override
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    @Override
    public String getAssignee() {
        return assignee;
    }

    @Override
    public void setAssignee(String assignee) {
        this.assignee = assignee;
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
    public String getCalledProcessInstanceId() {
        return calledProcessInstanceId;
    }

    @Override
    public void setCalledProcessInstanceId(String calledProcessInstanceId) {
        this.calledProcessInstanceId = calledProcessInstanceId;
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
    public Date getTime() {
        return getStartTime();
    }

    @Override
    public String toString() {
        return "ActivityInstanceEntity[id=" + id + ", activityId=" + activityId + ", activityName=" + activityName + ", executionId= " + executionId + "]";
    }

}
