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

import org.flowable.eventregistry.api.runtime.EventCorrelationParameterInstance;
import org.flowable.eventregistry.model.EventCorrelationParameter;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class EventCorrelationParameterInstanceImpl implements EventCorrelationParameterInstance {

    protected EventCorrelationParameter eventCorrelationParameterDefinition;
    protected Object value;

    public EventCorrelationParameterInstanceImpl(
        EventCorrelationParameter eventCorrelationParameterDefinition, Object value) {
        this.eventCorrelationParameterDefinition = eventCorrelationParameterDefinition;
        this.value = value;
    }

    @Override
    public EventCorrelationParameter getEventCorrelationParameterDefinition() {
        return eventCorrelationParameterDefinition;
    }

    public void setEventCorrelationParameterDefinition(EventCorrelationParameter eventCorrelationParameterDefinition) {
        this.eventCorrelationParameterDefinition = eventCorrelationParameterDefinition;
    }

    @Override
    public String getDefinitionName() {
        return eventCorrelationParameterDefinition.getName();
    }

    @Override
    public String getDefinitionType() {
        return eventCorrelationParameterDefinition.getType();
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
        EventCorrelationParameterInstanceImpl that = (EventCorrelationParameterInstanceImpl) o;
        return Objects.equals(eventCorrelationParameterDefinition, that.eventCorrelationParameterDefinition) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventCorrelationParameterDefinition, value);
    }
}
