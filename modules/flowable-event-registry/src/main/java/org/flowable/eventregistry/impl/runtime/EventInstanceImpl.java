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

import java.util.Collection;
import java.util.stream.Collectors;

import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;

/**
 * @author Joram Barrez
 */
public class EventInstanceImpl implements EventInstance {

    protected String eventKey;
    protected Collection<EventPayloadInstance> payloadInstances;
    protected Collection<EventPayloadInstance> correlationPayloadInstances;
    protected String tenantId;

    public EventInstanceImpl(String eventKey,
            Collection<EventPayloadInstance> payloadInstances) {
        this(eventKey, payloadInstances, EventRegistryEngineConfiguration.NO_TENANT_ID);
    }

    public EventInstanceImpl(String eventKey,
            Collection<EventPayloadInstance> payloadInstances,
            String tenantId) {
        this.eventKey = eventKey;
        this.payloadInstances = payloadInstances;
        this.correlationPayloadInstances = this.payloadInstances.stream()
                .filter(eventPayloadInstance -> eventPayloadInstance.getEventPayloadDefinition().isCorrelationParameter())
                .collect(Collectors.toList());
        this.tenantId = tenantId;
    }

    @Override
    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    @Override
    public Collection<EventPayloadInstance> getPayloadInstances() {
        return payloadInstances;
    }

    public void setPayloadInstances(Collection<EventPayloadInstance> payloadInstances) {
        this.payloadInstances = payloadInstances;
    }

    @Override
    public Collection<EventPayloadInstance> getCorrelationParameterInstances() {
        return correlationPayloadInstances;
    }

    public void setCorrelationParameterInstances(Collection<EventPayloadInstance> correlationParameterInstances) {
        this.correlationPayloadInstances = correlationParameterInstances;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
