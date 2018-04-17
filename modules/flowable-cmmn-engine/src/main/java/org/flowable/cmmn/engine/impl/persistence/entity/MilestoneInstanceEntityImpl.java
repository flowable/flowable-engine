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
public class MilestoneInstanceEntityImpl extends AbstractEntity implements MilestoneInstanceEntity {
    
    protected String name;
    protected Date timeStamp;
    protected String caseInstanceId;
    protected String caseDefinitionId;
    protected String elementId;
    
    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("id", id);
        persistentState.put("name", name);
        persistentState.put("timeStamp", timeStamp);
        persistentState.put("caseInstanceId", caseInstanceId);
        persistentState.put("caseDefinitionId", caseDefinitionId);
        persistentState.put("elementId", elementId);
        return persistentState;
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Date getTimeStamp() {
        return timeStamp;
    }
    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }
    public String getCaseInstanceId() {
        return caseInstanceId;
    }
    public void setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }
    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }
    public void setCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }
    public String getElementId() {
        return elementId;
    }
    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

}
