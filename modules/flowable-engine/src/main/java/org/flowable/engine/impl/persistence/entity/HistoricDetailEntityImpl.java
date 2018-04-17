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

import org.flowable.common.engine.impl.persistence.entity.AbstractEntityNoRevision;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public abstract class HistoricDetailEntityImpl extends AbstractEntityNoRevision implements HistoricDetailEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected String processInstanceId;
    protected String activityInstanceId;
    protected String taskId;
    protected String executionId;
    protected Date time;
    protected String detailType;

    @Override
    public Object getPersistentState() {
        // details are not updatable so we always provide the same object as the state
        return HistoricDetailEntityImpl.class;
    }

    // getters and setters //////////////////////////////////////////////////////

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    @Override
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public String getActivityInstanceId() {
        return activityInstanceId;
    }

    @Override
    public void setActivityInstanceId(String activityInstanceId) {
        this.activityInstanceId = activityInstanceId;
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
    public String getExecutionId() {
        return executionId;
    }

    @Override
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    @Override
    public Date getTime() {
        return time;
    }

    @Override
    public void setTime(Date time) {
        this.time = time;
    }

    @Override
    public String getDetailType() {
        return detailType;
    }

    @Override
    public void setDetailType(String detailType) {
        this.detailType = detailType;
    }

}
