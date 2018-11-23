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
package org.flowable.task.service.impl.persistence.entity;

import java.util.Date;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntityNoRevision;
import org.flowable.task.service.impl.persistence.entity.TaskLogEntryEntity;

/**
 * @author martin.grofcik
 */
public class TaskLogEntryEntityImpl extends AbstractEntityNoRevision implements TaskLogEntryEntity {

    protected long logNumber;
    protected String type;
    protected String taskId;
    protected Date timeStamp;
    protected String userId;
    protected byte[] data;

    public TaskLogEntryEntityImpl() {
    }

    @Override
    public Object getPersistentState() {
        return null; // Not updatable
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    @Override
    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    @Override
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public String getIdPrefix() {
        return TaskServiceEntityConstants.TASK_SERVICE_ID_PREFIX;
    }

    @Override
    public void setLogNumber(long logNumber) {
        this.logNumber = logNumber;
    }

    @Override
    public long getLogNumber() {
        return logNumber;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getTaskId() {
        return taskId;
    }

    @Override
    public Date getTimeStamp() {
        return timeStamp;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "(" + logNumber + ", " + getTaskId() + ", " + timeStamp + ")";
    }
}
