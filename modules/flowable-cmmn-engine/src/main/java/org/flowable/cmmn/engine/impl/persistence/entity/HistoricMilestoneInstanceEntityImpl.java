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
public class HistoricMilestoneInstanceEntityImpl extends AbstractEntity implements HistoricMilestoneInstanceEntity {
    
    protected String name;
    protected Date timeStamp;
    protected String caseInstanceId;
    protected String caseDefinitionId;
    protected String elementId;
    protected String tenantId;
    
    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("id", id);
        persistentState.put("name", name);
        persistentState.put("timeStamp", timeStamp);
        persistentState.put("caseInstanceId", caseInstanceId);
        persistentState.put("caseDefinitionId", caseDefinitionId);
        persistentState.put("elementId", elementId);
        persistentState.put("tenantId", tenantId);
        return persistentState;
    }
    
    @Override
    public String getName() {
        return name;
    }
    @Override
    public void setName(String name) {
        this.name = name;
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
    public String getCaseInstanceId() {
        return caseInstanceId;
    }
    @Override
    public void setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }
    @Override
    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }
    @Override
    public void setCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }
    @Override
    public String getElementId() {
        return elementId;
    }
    @Override
    public void setElementId(String elementId) {
        this.elementId = elementId;
    }
    @Override
    public String getTenantId() {
        return tenantId;
    }
    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
}
