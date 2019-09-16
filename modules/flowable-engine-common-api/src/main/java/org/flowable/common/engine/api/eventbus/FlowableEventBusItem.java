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
package org.flowable.common.engine.api.eventbus;

import java.time.LocalDateTime;
import java.util.Map;

public class FlowableEventBusItem {

    protected String type;
    protected String scopeId;
    protected String scopeType;
    protected String scopeDefinitionId;
    protected String scopeDefinitionKey;
    protected String correlationKey;
    protected LocalDateTime created;
    protected Map<String, Object> data;
    
    public FlowableEventBusItem() {}
    
    public FlowableEventBusItem(String type, String scopeId, String scopeType, String correlationKey) {
        this.type = type;
        this.scopeId = scopeId;
        this.scopeType = scopeType;
        this.correlationKey = correlationKey;
        this.created = LocalDateTime.now();
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getScopeId() {
        return scopeId;
    }

    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }
    
    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    public void setScopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
    }

    public String getScopeDefinitionKey() {
        return scopeDefinitionKey;
    }

    public void setScopeDefinitionKey(String scopeDefinitionKey) {
        this.scopeDefinitionKey = scopeDefinitionKey;
    }

    public String getCorrelationKey() {
        return correlationKey;
    }
    
    public void setCorrelationKey(String correlationKey) {
        this.correlationKey = correlationKey;
    }
    
    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
