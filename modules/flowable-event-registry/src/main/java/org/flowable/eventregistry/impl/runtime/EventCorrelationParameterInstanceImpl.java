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

import org.flowable.eventregistry.api.runtime.EventCorrelationParameterInstance;
import org.flowable.eventregistry.model.EventCorrelationParameterDefinition;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class EventCorrelationParameterInstanceImpl implements EventCorrelationParameterInstance {

    protected EventCorrelationParameterDefinition eventCorrelationParameterDefinition;
    protected Object value;

    public EventCorrelationParameterInstanceImpl(
        EventCorrelationParameterDefinition eventCorrelationParameterDefinition, Object value) {
        this.eventCorrelationParameterDefinition = eventCorrelationParameterDefinition;
        this.value = value;
    }

    @Override
    public EventCorrelationParameterDefinition getEventCorrelationParameterDefinition() {
        return eventCorrelationParameterDefinition;
    }

    public void setEventCorrelationParameterDefinition(EventCorrelationParameterDefinition eventCorrelationParameterDefinition) {
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

}
