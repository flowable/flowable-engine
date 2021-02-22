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
package org.flowable.eventregistry.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class EventPayload {

    protected String name;
    protected String type;
    protected boolean correlationParameter;
    
    public EventPayload() {}

    public EventPayload(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public boolean isCorrelationParameter() {
        return correlationParameter;
    }

    public void setCorrelationParameter(boolean correlationParameter) {
        this.correlationParameter = correlationParameter;
    }

    public static EventPayload correlation(String name, String type) {
        EventPayload payload = new EventPayload(name, type);
        payload.setCorrelationParameter(true);
        return payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EventPayload that = (EventPayload) o;
        return Objects.equals(name, that.name) && Objects.equals(type, that.type) && correlationParameter == that.correlationParameter;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }
}
