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
package org.flowable.eventregistry.impl.runtime;

import java.util.Objects;

import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.model.EventPayload;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class EventPayloadInstanceImpl implements EventPayloadInstance {

    protected EventPayload eventPayloadDefinition;
    protected Object value;

    public EventPayloadInstanceImpl(EventPayload eventPayloadDefinition, Object value) {
        this.eventPayloadDefinition = eventPayloadDefinition;
        this.value = value;
    }

    @Override
    public EventPayload getEventPayloadDefinition() {
        return eventPayloadDefinition;
    }

    public void setEventPayloadDefinition(EventPayload eventPayloadDefinition) {
        this.eventPayloadDefinition = eventPayloadDefinition;
    }

    @Override
    public String getDefinitionName() {
        return eventPayloadDefinition.getName();
    }

    @Override
    public String getDefinitionType() {
        return eventPayloadDefinition.getType();
    }

    @Override
    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EventPayloadInstanceImpl that = (EventPayloadInstanceImpl) o;
        return Objects.equals(eventPayloadDefinition, that.eventPayloadDefinition) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventPayloadDefinition, value);
    }

}
