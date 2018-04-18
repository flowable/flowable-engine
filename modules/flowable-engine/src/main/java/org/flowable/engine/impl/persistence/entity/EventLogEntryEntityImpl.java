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

import java.util.Date;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntityNoRevision;

/**
 * An event log entry can only be inserted (and maybe deleted).
 * 
 * @author Joram Barrez
 */
public class EventLogEntryEntityImpl extends AbstractEntityNoRevision implements EventLogEntryEntity {

    protected long logNumber; // cant use id here, it would clash with entity
    protected String type;
    protected String processDefinitionId;
    protected String processInstanceId;
    protected String executionId;
    protected String taskId;
    protected Date timeStamp;
    protected String userId;
    protected byte[] data;
    protected String lockOwner;
    protected String lockTime;
    protected int isProcessed;

    public EventLogEntryEntityImpl() {
    }

    @Override
    public Object getPersistentState() {
        return null; // Not updatable
    }

    @Override
    public long getLogNumber() {
        return logNumber;
    }

    @Override
    public void setLogNumber(long logNumber) {
        this.logNumber = logNumber;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
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
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    @Override
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
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
    public String getTaskId() {
        return taskId;
    }

    @Override
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    @Override
    public Date getTimeStamp() {
        return timeStamp;
    }

    @Override
    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public byte[] getData() {
        return data;
    }

    @Override
    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public String getLockOwner() {
        return lockOwner;
    }

    @Override
    public void setLockOwner(String lockOwner) {
        this.lockOwner = lockOwner;
    }

    @Override
    public String getLockTime() {
        return lockTime;
    }

    @Override
    public void setLockTime(String lockTime) {
        this.lockTime = lockTime;
    }

    @Override
    public int getProcessed() {
        return isProcessed;
    }

    @Override
    public void setProcessed(int isProcessed) {
        this.isProcessed = isProcessed;
    }

    @Override
    public String toString() {
        return timeStamp.toString() + " : " + type;
    }

}
