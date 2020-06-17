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
package org.flowable.eventregistry.impl.consumer;

import java.util.Collection;
import java.util.Objects;

import org.flowable.eventregistry.api.runtime.EventPayloadInstance;

/**
 * A representation of a correlation key, including the
 * {@link org.flowable.eventregistry.api.runtime.EventPayloadInstance} instances
 * that were used to get to the key value.
 *
 * @author Joram Barrez
 */
public class CorrelationKey {

    protected String value;
    protected Collection<EventPayloadInstance> parameterInstances;

    public CorrelationKey(String value, Collection<EventPayloadInstance> parameterInstances) {
        this.value = value;
        this.parameterInstances = parameterInstances;
    }

    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
    public Collection<EventPayloadInstance> getParameterInstances() {
        return parameterInstances;
    }
    public void setParameterInstances(Collection<EventPayloadInstance> parameterInstances) {
        this.parameterInstances = parameterInstances;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CorrelationKey that = (CorrelationKey) o;
        return Objects.equals(value, that.value) && Objects.equals(parameterInstances, that.parameterInstances);
    }

    @Override
    public int hashCode() {
        return value.hashCode(); // The value is determined by the parameterInstance, so no need to use them in the hashcode
    }
}
