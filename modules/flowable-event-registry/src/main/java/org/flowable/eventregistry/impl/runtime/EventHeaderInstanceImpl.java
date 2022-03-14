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

import org.flowable.eventregistry.api.runtime.EventHeaderInstance;
import org.flowable.eventregistry.model.EventHeader;

public class EventHeaderInstanceImpl implements EventHeaderInstance {

    protected EventHeader eventHeaderDefinition;
    protected Object value;

    public EventHeaderInstanceImpl(EventHeader eventHeaderDefinition, Object value) {
        this.eventHeaderDefinition = eventHeaderDefinition;
        this.value = value;
    }

    @Override
    public EventHeader getEventHeaderDefinition() {
        return eventHeaderDefinition;
    }

    public void setEventHeaderDefinition(EventHeader eventHeaderDefinition) {
        this.eventHeaderDefinition = eventHeaderDefinition;
    }

    @Override
    public String getDefinitionName() {
        return eventHeaderDefinition.getName();
    }

    @Override
    public String getDefinitionType() {
        return eventHeaderDefinition.getType();
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
        EventHeaderInstanceImpl that = (EventHeaderInstanceImpl) o;
        return Objects.equals(eventHeaderDefinition, that.eventHeaderDefinition) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventHeaderDefinition, value);
    }

}
