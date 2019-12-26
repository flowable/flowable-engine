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
package org.flowable.eventregistry.impl.deployer;

import java.util.List;
import java.util.Map;

import org.flowable.eventregistry.impl.parser.ChannelDefinitionParse;
import org.flowable.eventregistry.impl.parser.EventDefinitionParse;
import org.flowable.eventregistry.impl.persistence.entity.ChannelDefinitionEntity;
import org.flowable.eventregistry.impl.persistence.entity.EventDefinitionEntity;
import org.flowable.eventregistry.impl.persistence.entity.EventDeploymentEntity;
import org.flowable.eventregistry.impl.persistence.entity.EventResourceEntity;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.EventModel;

/**
 * An intermediate representation of a DeploymentEntity which keeps track of all of the entity's and resources.
 * 
 * The EventDefinitionEntities are expected to be "not fully set-up" - they may be inconsistent with the DeploymentEntity and/or the persisted versions, 
 * and if the deployment is new, they will not yet be persisted.
 */
public class ParsedDeployment {

    protected EventDeploymentEntity deploymentEntity;

    protected List<EventDefinitionEntity> eventDefinitions;
    protected List<ChannelDefinitionEntity> channelDefinitions;
    protected Map<EventDefinitionEntity, EventDefinitionParse> mapEventDefinitionsToParses;
    protected Map<EventDefinitionEntity, EventResourceEntity> mapEventDefinitionsToResources;
    protected Map<ChannelDefinitionEntity, ChannelDefinitionParse> mapChannelDefinitionsToParses;
    protected Map<ChannelDefinitionEntity, EventResourceEntity> mapChannelDefinitionsToResources;

    public ParsedDeployment(EventDeploymentEntity entity, List<EventDefinitionEntity> eventDefinitions,
            List<ChannelDefinitionEntity> channelDefinitions,
            Map<EventDefinitionEntity, EventDefinitionParse> mapEventDefinitionsToParses,
            Map<EventDefinitionEntity, EventResourceEntity> mapEventDefinitionsToResources,
            Map<ChannelDefinitionEntity, ChannelDefinitionParse> mapChannelDefinitionsToParses,
            Map<ChannelDefinitionEntity, EventResourceEntity> mapChannelDefinitionsToResources) {

        this.deploymentEntity = entity;
        this.eventDefinitions = eventDefinitions;
        this.channelDefinitions = channelDefinitions;
        this.mapEventDefinitionsToParses = mapEventDefinitionsToParses;
        this.mapEventDefinitionsToResources = mapEventDefinitionsToResources;
        this.mapChannelDefinitionsToParses = mapChannelDefinitionsToParses;
        this.mapChannelDefinitionsToResources = mapChannelDefinitionsToResources;
    }

    public EventDeploymentEntity getDeployment() {
        return deploymentEntity;
    }

    public List<EventDefinitionEntity> getAllEventDefinitions() {
        return eventDefinitions;
    }
    
    public List<ChannelDefinitionEntity> getAllChannelDefinitions() {
        return channelDefinitions;
    }

    public EventResourceEntity getResourceForEventDefinition(EventDefinitionEntity eventDefinition) {
        return mapEventDefinitionsToResources.get(eventDefinition);
    }

    public EventDefinitionParse getEventDefinitionParseForEventDefinition(EventDefinitionEntity formDefinition) {
        return mapEventDefinitionsToParses.get(formDefinition);
    }

    public EventModel getEventModelForEventDefinition(EventDefinitionEntity eventDefinition) {
        EventDefinitionParse parse = getEventDefinitionParseForEventDefinition(eventDefinition);

        return (parse == null ? null : parse.getEventModel());
    }
    
    public EventResourceEntity getResourceForChannelDefinition(ChannelDefinitionEntity channelDefinition) {
        return mapChannelDefinitionsToResources.get(channelDefinition);
    }

    public ChannelDefinitionParse getChannelDefinitionParseForChannelDefinition(ChannelDefinitionEntity channelDefinition) {
        return mapChannelDefinitionsToParses.get(channelDefinition);
    }

    public ChannelModel getChannelModelForChannelDefinition(ChannelDefinitionEntity channelDefinition) {
        ChannelDefinitionParse parse = getChannelDefinitionParseForChannelDefinition(channelDefinition);

        return (parse == null ? null : parse.getChannelModel());
    }
}
