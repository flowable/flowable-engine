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
package org.flowable.cmmn.engine.impl.persistence.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;

/**
 * @author Joram Barrez
 */
public class SentryPartInstanceEntityImpl extends AbstractEntity implements SentryPartInstanceEntity {

    protected String caseDefinitionId;
    protected String caseInstanceId;
    protected String planItemInstanceId;
    protected String onPartId;
    protected String ifPartId;
    protected Date timeStamp;

    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("caseDefinitionId", caseDefinitionId);
        persistentState.put("caseInstanceId", caseInstanceId);
        persistentState.put("planItemInstanceId", planItemInstanceId);
        persistentState.put("onPartId", onPartId);
        persistentState.put("ifPart", ifPartId);
        persistentState.put("timeStamp", timeStamp);
        return persistentState;
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

    public String getPlanItemInstanceId() {
        return planItemInstanceId;
    }

    public void setPlanItemInstanceId(String planItemInstanceId) {
        this.planItemInstanceId = planItemInstanceId;
    }

    public String getOnPartId() {
        return onPartId;
    }

    public void setOnPartId(String onPartId) {
        this.onPartId = onPartId;
    }

    public String getIfPartId() {
        return ifPartId;
    }

    public void setIfPartId(String ifPartId) {
        this.ifPartId = ifPartId;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

}
