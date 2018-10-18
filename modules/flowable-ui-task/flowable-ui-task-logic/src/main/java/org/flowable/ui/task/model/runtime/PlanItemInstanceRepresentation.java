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
package org.flowable.ui.task.model.runtime;

import java.util.Date;

import org.flowable.ui.common.model.AbstractRepresentation;

/**
 * @author Joram Barrez
 */
public class PlanItemInstanceRepresentation extends AbstractRepresentation {

    protected String id;
    protected String caseDefinitionId;
    protected String caseInstanceId;
    protected String stageInstanceId;
    protected boolean isStage;
    protected String elementId;
    protected String planItemDefinitionId;
    protected String planItemDefinitionType;
    protected String name;
    protected String state;
    protected Date createTime;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }
    public void setCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }
    public String getCaseInstanceId() {
        return caseInstanceId;
    }
    public void setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }
    public String getStageInstanceId() {
        return stageInstanceId;
    }
    public void setStageInstanceId(String stageInstanceId) {
        this.stageInstanceId = stageInstanceId;
    }
    public boolean isStage() {
        return isStage;
    }
    public void setStage(boolean stage) {
        isStage = stage;
    }
    public String getElementId() {
        return elementId;
    }
    public void setElementId(String elementId) {
        this.elementId = elementId;
    }
    public String getPlanItemDefinitionId() {
        return planItemDefinitionId;
    }
    public void setPlanItemDefinitionId(String planItemDefinitionId) {
        this.planItemDefinitionId = planItemDefinitionId;
    }
    public String getPlanItemDefinitionType() {
        return planItemDefinitionType;
    }
    public void setPlanItemDefinitionType(String planItemDefinitionType) {
        this.planItemDefinitionType = planItemDefinitionType;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getState() {
        return state;
    }
    public void setState(String state) {
        this.state = state;
    }
    public Date getCreateTime() {
        return createTime;
    }
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
