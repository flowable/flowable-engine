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
package org.flowable.common.engine.impl.eventregistry.runtime;

import org.flowable.common.engine.api.eventregistry.definition.EventPayloadDefinition;
import org.flowable.common.engine.api.eventregistry.runtime.EventPayloadInstance;

/**
  * @author Joram Barrez
  */
public class EventPayloadInstanceImpl implements EventPayloadInstance {

    protected EventPayloadDefinition eventPayloadDefinition;
    protected Object value;

    public EventPayloadInstanceImpl(EventPayloadDefinition eventPayloadDefinition, Object value) {
        this.eventPayloadDefinition = eventPayloadDefinition;
        this.value = value;
    }

    @Override
    public EventPayloadDefinition getEventPayloadDefinition() {
        return eventPayloadDefinition;
    }

    public void setEventPayloadDefinition(EventPayloadDefinition eventPayloadDefinition) {
        this.eventPayloadDefinition = eventPayloadDefinition;
    }

    @Override
    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

}
