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
package org.flowable.eventregistry.impl.event;

import java.time.LocalDateTime;
import java.util.Map;

import org.flowable.common.engine.api.eventbus.FlowableEventBusEvent;
import org.flowable.eventregistry.api.runtime.EventInstance;

/**
 * @author Joram Barrez
 */
public class EventRegistryEvent implements FlowableEventBusEvent {
    //TODO decide how we want to handle the FlowableEventBusEvent

    protected String type;
    protected EventInstance eventInstance;

    public EventRegistryEvent(EventInstance eventInstance) {
        this.type = eventInstance.getEventDefinition().getKey();
        this.eventInstance = eventInstance;
    }

    public EventInstance getEventInstance() {
        return eventInstance;
    }

    public void setEventInstance(EventInstance eventInstance) {
        this.eventInstance = eventInstance;
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
        return null;
    }

    @Override
    public void setScopeId(String scopeId) {

    }

    @Override
    public String getScopeType() {
        return null;
    }

    @Override
    public void setScopeType(String scopeType) {

    }

    @Override
    public String getScopeDefinitionId() {
        return null;
    }

    @Override
    public void setScopeDefinitionId(String scopeDefinitionId) {

    }

    @Override
    public String getScopeDefinitionKey() {
        return null;
    }

    @Override
    public void setScopeDefinitionKey(String scopeDefinitionKey) {

    }

    @Override
    public String getCorrelationKey() {
        return null;
    }

    @Override
    public void setCorrelationKey(String correlationKey) {

    }

    @Override
    public LocalDateTime getCreated() {
        return null;
    }

    @Override
    public void setCreated(LocalDateTime created) {

    }

    @Override
    public Map<String, Object> getData() {
        return null;
    }

    @Override
    public void setData(Map<String, Object> data) {

    }
}
