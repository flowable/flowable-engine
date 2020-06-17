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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSetter;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventModel {

    protected String key;
    protected String name;
    protected Map<String, EventPayload> payload = new LinkedHashMap<>();

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    public Collection<EventPayload> getCorrelationParameters() {
        return payload.values()
                .stream()
                .filter(EventPayload::isCorrelationParameter)
                .collect(Collectors.toList());
    }

    public EventPayload getPayload(String name) {
        return payload.get(name);
    }

    @JsonGetter
    public Collection<EventPayload> getPayload() {
        return payload.values();
    }

    @JsonSetter
    public void setPayload(Collection<EventPayload> payload) {
        for (EventPayload eventPayload : payload) {
            this.payload.put(eventPayload.getName(), eventPayload);
        }
    }

    public void addPayload(String name, String type) {
        EventPayload eventPayload = payload.get(name);
        if (eventPayload != null) {
            eventPayload.setType(type);
        } else {
            payload.put(name, new EventPayload(name, type));
        }
    }

    public void addCorrelation(String name, String type) {
        EventPayload eventPayload = payload.get(name);
        if (eventPayload != null) {
            eventPayload.setCorrelationParameter(true);
        } else {
            payload.put(name, EventPayload.correlation(name, type));
        }
    }

}
