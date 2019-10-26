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
package org.flowable.common.engine.impl.eventbus;

import java.time.LocalDateTime;
import java.util.Map;

import org.flowable.common.engine.api.eventbus.FlowableEventBusEvent;

public class FlowableEventBusEventImpl implements FlowableEventBusEvent {

    protected String type;
    protected String scopeId;
    protected String scopeType;
    protected String scopeDefinitionId;
    protected String scopeDefinitionKey;
    protected String correlationKey;
    protected LocalDateTime created;
    protected Map<String, Object> data;
    
    public FlowableEventBusEventImpl() {}
    
    public FlowableEventBusEventImpl(String type, String scopeId, String scopeType, String correlationKey) {
        this.type = type;
        this.scopeId = scopeId;
        this.scopeType = scopeType;
        this.correlationKey = correlationKey;
        this.created = LocalDateTime.now();
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
    public String getScopeId() {
        return scopeId;
    }

    @Override
    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    @Override
    public String getScopeType() {
        return scopeType;
    }

    @Override
    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }
    
    @Override
    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    @Override
    public void setScopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
    }

    @Override
    public String getScopeDefinitionKey() {
        return scopeDefinitionKey;
    }

    @Override
    public void setScopeDefinitionKey(String scopeDefinitionKey) {
        this.scopeDefinitionKey = scopeDefinitionKey;
    }

    @Override
    public String getCorrelationKey() {
        return correlationKey;
    }
    
    @Override
    public void setCorrelationKey(String correlationKey) {
        this.correlationKey = correlationKey;
    }
    
    @Override
    public LocalDateTime getCreated() {
        return created;
    }

    @Override
    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    @Override
    public Map<String, Object> getData() {
        return data;
    }
    
    @Override
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
