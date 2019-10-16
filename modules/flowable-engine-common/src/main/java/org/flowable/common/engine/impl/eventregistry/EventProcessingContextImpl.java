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
package org.flowable.common.engine.impl.eventregistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.eventregistry.definition.EventDefinition;
import org.flowable.common.engine.api.eventregistry.EventProcessingContext;
import org.flowable.common.engine.api.eventregistry.runtime.EventInstance;

/**
 * @author Joram Barrez
 */
public class EventProcessingContextImpl implements EventProcessingContext {

    protected String channelKey;
    protected EventDefinition eventDefinition;
    protected String event;
    protected Map<String, Object> payload;
    protected Map<String, Object> processingData;
    protected Collection<EventInstance> eventInstances = new ArrayList<>();

    public EventProcessingContextImpl(String channelKey, String event) {
        this.channelKey = channelKey;
        this.event = event;
    }

    @Override
    public void addProcessingData(String key, Object data) {
        if (processingData == null) {
            processingData = new HashMap<>();
        }
        processingData.put(key, data);
    }

    @Override
    public String getChannelKey() {
        return channelKey;
    }

    public void setChannelKey(String channelKey) {
        this.channelKey = channelKey;
    }

    @Override
    public EventDefinition getEventDefinition() {
        return eventDefinition;
    }

    @Override
    public void setEventDefinition(EventDefinition eventDefinition) {
        this.eventDefinition = eventDefinition;
    }

    @Override
    public String getRawEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getEvent() {
        return event;
    }

    @Override
    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    @Override
    public <T> T getProcessingData(String key, Class<T> clazz) {
        return (T) processingData.get(key);
    }

    public Map<String, Object> getProcessingData() {
        return processingData;
    }

    public void setProcessingData(Map<String, Object> processingData) {
        this.processingData = processingData;
    }

    @Override
    public Collection<EventInstance> getEventInstances() {
        return eventInstances;
    }

    public void setEventInstances(Collection<EventInstance> eventInstances) {
        this.eventInstances = eventInstances;
    }

}
