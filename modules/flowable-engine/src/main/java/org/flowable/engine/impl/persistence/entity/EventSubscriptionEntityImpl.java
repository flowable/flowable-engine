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

package org.flowable.engine.impl.persistence.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public abstract class EventSubscriptionEntityImpl extends AbstractEntity implements EventSubscriptionEntity, Serializable {

    private static final long serialVersionUID = 1L;

    // persistent state ///////////////////////////
    protected String eventType;
    protected String eventName;
    protected String executionId;
    protected String processInstanceId;
    protected String activityId;
    protected String configuration;
    protected Date created;
    protected String processDefinitionId;
    protected String tenantId;

    // runtime state /////////////////////////////
    protected ExecutionEntity execution;

    public EventSubscriptionEntityImpl() {
        this.created = CommandContextUtil.getProcessEngineConfiguration().getClock().getCurrentTime();
    }

    @Override
    public Object getPersistentState() {
        HashMap<String, Object> persistentState = new HashMap<>();
        persistentState.put("eventName", this.eventName);
        persistentState.put("executionId", this.executionId);
        persistentState.put("processInstanceId", this.processInstanceId);
        persistentState.put("activityId", this.activityId);
        persistentState.put("created", this.created);
        persistentState.put("configuration", this.configuration);
        persistentState.put("tenantId", this.tenantId);
        return persistentState;
    }

    // getters & setters ////////////////////////////

    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @Override
    public String getEventName() {
        return eventName;
    }

    @Override
    public void setEventName(String eventName) {
        this.eventName = eventName;
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
    public ExecutionEntity getExecution() {
        if (execution == null && executionId != null) {
            execution = CommandContextUtil.getExecutionEntityManager().findById(executionId);
        }
        return execution;
    }

    @Override
    public void setExecution(ExecutionEntity execution) {
        this.execution = execution;
        if (execution != null) {
            this.executionId = execution.getId();
            this.processInstanceId = execution.getProcessInstanceId();
        }
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
    public String getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(String configuration) {
        this.configuration = configuration;
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
    public Date getCreated() {
        return created;
    }

    @Override
    public void setCreated(Date created) {
        this.created = created;
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
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EventSubscriptionEntityImpl other = (EventSubscriptionEntityImpl) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

}
