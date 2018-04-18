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

import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author Christian Stettler
 */
public abstract class HistoricScopeInstanceEntityImpl extends AbstractEntity implements HistoricScopeInstanceEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected String processInstanceId;
    protected String processDefinitionId;
    protected Date startTime;
    protected Date endTime;
    protected Long durationInMillis;
    protected String deleteReason;

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
    public String getDeleteReason() {
        return deleteReason;
    }

    @Override
    public void setDeleteReason(String deleteReason) {
        this.deleteReason = deleteReason;
    }
}
