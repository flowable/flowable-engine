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

package org.activiti.engine.impl.persistence.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.history.HistoricActivityInstance;

/**
 * @author Christian Stettler
 * @author Joram Barrez
 */
public class HistoricActivityInstanceEntity extends HistoricScopeInstanceEntity implements HistoricActivityInstance {

    private static final long serialVersionUID = 1L;

    protected String activityId;
    protected String activityName;
    protected String activityType;
    protected String executionId;
    protected String assignee;
    protected String taskId;
    protected String calledProcessInstanceId;
    protected String tenantId = ProcessEngineConfiguration.NO_TENANT_ID;

    public HistoricActivityInstanceEntity() {

    }

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("endTime", endTime);
        persistentState.put("durationInMillis", durationInMillis);
        persistentState.put("deleteReason", deleteReason);
        persistentState.put("executionId", executionId);
        persistentState.put("assignee", assignee);
        return persistentState;
    }

    // getters and setters //////////////////////////////////////////////////////

    @Override
    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    @Override
    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    @Override
    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    @Override
    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    @Override
    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    @Override
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    @Override
    public String getCalledProcessInstanceId() {
        return calledProcessInstanceId;
    }

    public void setCalledProcessInstanceId(String calledProcessInstanceId) {
        this.calledProcessInstanceId = calledProcessInstanceId;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public Date getTime() {
        return getStartTime();
    }

    // common methods //////////////////////////////////////////////////////////

    @Override
    public String toString() {
        return "HistoricActivityInstanceEntity[activityId=" + activityId + ", activityName=" + activityName + "]";
    }

}
