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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.api.runtime.EventCorrelationParameterInstance;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.EventModel;

/**
 * @author Joram Barrez
 */
public class EventInstanceImpl implements EventInstance {

    protected EventDefinition eventDefinition;
    protected EventModel eventModel;
    protected Collection<ChannelDefinition> channelDefinitions;
    protected Collection<ChannelModel> channelModels;
    protected Collection<EventPayloadInstance> payloadInstances = new ArrayList<>();
    protected Collection<EventCorrelationParameterInstance> correlationParameterInstances = new ArrayList<>();
    protected String tenantId;

    public EventInstanceImpl() {

    }

    public EventInstanceImpl(EventModel eventModel,
        List<ChannelModel> channelModels,
        Collection<EventCorrelationParameterInstance> correlationParameterInstances,
        Collection<EventPayloadInstance> payloadInstances) {

        this.eventModel = eventModel;
        this.channelModels = channelModels;
        this.correlationParameterInstances = correlationParameterInstances;
        this.payloadInstances = payloadInstances;
    }

    public EventInstanceImpl(EventModel eventModel,
            List<ChannelModel> channelModels,
            Collection<EventCorrelationParameterInstance> correlationParameterInstances,
            Collection<EventPayloadInstance> payloadInstances,
            String tenantId) {
        this(eventModel, channelModels, correlationParameterInstances, payloadInstances);
        this.tenantId = tenantId;
    }

    @Override
    public EventDefinition getEventDefinition() {
        return eventDefinition;
    }
    public void setEventDefinition(EventDefinition eventDefinition) {
        this.eventDefinition = eventDefinition;
    }
    @Override
    public EventModel getEventModel() {
        return eventModel;
    }
    public void setEventModel(EventModel eventModel) {
        this.eventModel = eventModel;
    }
    @Override
    public Collection<ChannelDefinition> getChannelDefinitions() {
        return channelDefinitions;
    }
    public void setChannelDefinitions(Collection<ChannelDefinition> channelDefinitions) {
        this.channelDefinitions = channelDefinitions;
    }
    @Override
    public Collection<ChannelModel> getChannelModels() {
        return channelModels;
    }
    public void setChannelModels(Collection<ChannelModel> channelModels) {
        this.channelModels = channelModels;
    }
    @Override
    public Collection<EventPayloadInstance> getPayloadInstances() {
        return payloadInstances;
    }
    public void setPayloadInstances(Collection<EventPayloadInstance> payloadInstances) {
        this.payloadInstances = payloadInstances;
    }
    @Override
    public Collection<EventCorrelationParameterInstance> getCorrelationParameterInstances() {
        return correlationParameterInstances;
    }
    public void setCorrelationParameterInstances(
        Collection<EventCorrelationParameterInstance> correlationParameterInstances) {
        this.correlationParameterInstances = correlationParameterInstances;
    }
    @Override
    public String getTenantId() {
        return tenantId;
    }
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
