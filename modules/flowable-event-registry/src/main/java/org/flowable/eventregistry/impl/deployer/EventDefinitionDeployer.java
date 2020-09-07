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

import java.util.LinkedHashMap;
import java.util.Map;

import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.persistence.deploy.Deployer;
import org.flowable.eventregistry.impl.persistence.entity.ChannelDefinitionEntity;
import org.flowable.eventregistry.impl.persistence.entity.ChannelDefinitionEntityManager;
import org.flowable.eventregistry.impl.persistence.entity.EventDefinitionEntity;
import org.flowable.eventregistry.impl.persistence.entity.EventDefinitionEntityManager;
import org.flowable.eventregistry.impl.persistence.entity.EventDeploymentEntity;
import org.flowable.eventregistry.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class EventDefinitionDeployer implements Deployer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventDefinitionDeployer.class);

    protected IdGenerator idGenerator;
    protected ParsedDeploymentBuilderFactory parsedDeploymentBuilderFactory;
    protected EventDefinitionDeploymentHelper eventDeploymentHelper;
    protected ChannelDefinitionDeploymentHelper channelDeploymentHelper;
    protected CachingAndArtifactsManager cachingAndArtifactsManager;
    protected boolean usePrefixId;

    @Override
    public void deploy(EventDeploymentEntity deployment) {
        LOGGER.debug("Processing deployment {}", deployment.getName());

        // The ParsedDeployment represents the deployment, the forms, and the form
        // resource, parse, and model associated with each form.
        ParsedDeployment parsedDeployment = parsedDeploymentBuilderFactory.getBuilderForDeployment(deployment).build();

        eventDeploymentHelper.verifyEventDefinitionsDoNotShareKeys(parsedDeployment.getAllEventDefinitions());
        channelDeploymentHelper.verifyChannelDefinitionsDoNotShareKeys(parsedDeployment.getAllChannelDefinitions());

        eventDeploymentHelper.copyDeploymentValuesToEventDefinitions(parsedDeployment.getDeployment(), parsedDeployment.getAllEventDefinitions());
        eventDeploymentHelper.setResourceNamesOnEventDefinitions(parsedDeployment);
        
        channelDeploymentHelper.copyDeploymentValuesToEventDefinitions(parsedDeployment.getDeployment(), parsedDeployment.getAllChannelDefinitions());
        channelDeploymentHelper.setResourceNamesOnEventDefinitions(parsedDeployment);

        if (deployment.isNew()) {
            Map<EventDefinitionEntity, EventDefinitionEntity> mapOfNewEventDefinitionToPreviousVersion = getPreviousVersionsOfEventDefinitions(parsedDeployment);
            setEventDefinitionVersionsAndIds(parsedDeployment, mapOfNewEventDefinitionToPreviousVersion);
            persistEventDefinitions(parsedDeployment);
            
            Map<ChannelDefinitionEntity, ChannelDefinitionEntity> mapOfNewChannelDefinitionToPreviousVersion = getPreviousVersionsOfChannelDefinitions(parsedDeployment);
            setChannelDefinitionVersionsAndIds(parsedDeployment, mapOfNewChannelDefinitionToPreviousVersion);
            persistChannelDefinitions(parsedDeployment);

            // There should be only one channel definition in the cache (otherwise it could be detected as a 'new version' by the EventRegistryChangeDetectionManager)
            for (ChannelDefinitionEntity previousChannelDefinition : mapOfNewChannelDefinitionToPreviousVersion.values()) {
                cachingAndArtifactsManager.removeChannelDefinitionFromCache(previousChannelDefinition.getId());
            }

        } else {
            makeEventDefinitionsConsistentWithPersistedVersions(parsedDeployment);
            makeChannelDefinitionsConsistentWithPersistedVersions(parsedDeployment);
        }

        cachingAndArtifactsManager.updateCachingAndArtifacts(parsedDeployment);
    }

    /**
     * Constructs a map from new event definitions to the previous version by key and tenant. If no previous version exists, no map entry is created.
     */
    protected Map<EventDefinitionEntity, EventDefinitionEntity> getPreviousVersionsOfEventDefinitions(ParsedDeployment parsedDeployment) {

        Map<EventDefinitionEntity, EventDefinitionEntity> result = new LinkedHashMap<>();

        for (EventDefinitionEntity newDefinition : parsedDeployment.getAllEventDefinitions()) {
            EventDefinitionEntity existingEventDefinition = eventDeploymentHelper.getMostRecentVersionOfEventDefinition(newDefinition);

            if (existingEventDefinition != null) {
                result.put(newDefinition, existingEventDefinition);
            }
        }

        return result;
    }
    
    /**
     * Constructs a map from new channel definitions to the previous version by key and tenant. If no previous version exists, no map entry is created.
     */
    protected Map<ChannelDefinitionEntity, ChannelDefinitionEntity> getPreviousVersionsOfChannelDefinitions(ParsedDeployment parsedDeployment) {

        Map<ChannelDefinitionEntity, ChannelDefinitionEntity> result = new LinkedHashMap<>();

        for (ChannelDefinitionEntity newDefinition : parsedDeployment.getAllChannelDefinitions()) {
            ChannelDefinitionEntity existingChannelDefinition = channelDeploymentHelper.getMostRecentVersionOfChannelDefinition(newDefinition);

            if (existingChannelDefinition != null) {
                result.put(newDefinition, existingChannelDefinition);
            }
        }

        return result;
    }

    /**
     * If the map contains an existing version for an event definition, then the event definition is updated, otherwise a new event definition is created.
     */
    protected void setEventDefinitionVersionsAndIds(ParsedDeployment parsedDeployment, Map<EventDefinitionEntity, EventDefinitionEntity> mapOfNewEventDefinitionToPreviousVersion) {
        for (EventDefinitionEntity eventDefinition : parsedDeployment.getAllEventDefinitions()) {
            int version = 1;
            
            EventDefinitionEntity latest = mapOfNewEventDefinitionToPreviousVersion.get(eventDefinition);
            if (latest != null) {
                version = latest.getVersion() + 1;
            }

            eventDefinition.setVersion(version);

            if (usePrefixId) {
                eventDefinition.setId(eventDefinition.getIdPrefix() + idGenerator.getNextId());
            } else {
                eventDefinition.setId(idGenerator.getNextId());
            }
        }
    }
    
    /**
     * If the map contains an existing version for a channel definition, then the channel definition is updated, otherwise a new channel definition is created.
     */
    protected void setChannelDefinitionVersionsAndIds(ParsedDeployment parsedDeployment, Map<ChannelDefinitionEntity, ChannelDefinitionEntity> mapOfNewChannelDefinitionToPreviousVersion) {
        for (ChannelDefinitionEntity channelDefinition : parsedDeployment.getAllChannelDefinitions()) {
            int version = 1;
            
            ChannelDefinitionEntity latest = mapOfNewChannelDefinitionToPreviousVersion.get(channelDefinition);
            if (latest != null) {
                version = latest.getVersion() + 1;
            }

            channelDefinition.setVersion(version);

            if (usePrefixId) {
                channelDefinition.setId(channelDefinition.getIdPrefix() + idGenerator.getNextId());
            } else {
                channelDefinition.setId(idGenerator.getNextId());
            }
        }
    }

    /**
     * Saves each event definition. It is assumed that the deployment is new, the definitions have never been saved before, and that they have all their values properly set up.
     */
    protected void persistEventDefinitions(ParsedDeployment parsedDeployment) {
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = CommandContextUtil.getEventRegistryConfiguration();
        EventDefinitionEntityManager eventDefinitionEntityManager = eventRegistryEngineConfiguration.getEventDefinitionEntityManager();

        for (EventDefinitionEntity eventDefinition : parsedDeployment.getAllEventDefinitions()) {
            eventDefinitionEntityManager.insert(eventDefinition);
        }
    }
    
    /**
     * Saves each channel definition. It is assumed that the deployment is new, the definitions have never been saved before, and that they have all their values properly set up.
     */
    protected void persistChannelDefinitions(ParsedDeployment parsedDeployment) {
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = CommandContextUtil.getEventRegistryConfiguration();
        ChannelDefinitionEntityManager channelDefinitionEntityManager = eventRegistryEngineConfiguration.getChannelDefinitionEntityManager();

        for (ChannelDefinitionEntity channelDefinition : parsedDeployment.getAllChannelDefinitions()) {
            channelDefinitionEntityManager.insert(channelDefinition);
        }
    }

    /**
     * Loads the persisted version of each event definition and set values on the in-memory version to be consistent.
     */
    protected void makeEventDefinitionsConsistentWithPersistedVersions(ParsedDeployment parsedDeployment) {
        for (EventDefinitionEntity eventDefinition : parsedDeployment.getAllEventDefinitions()) {
            EventDefinitionEntity persistedEventDefinition = eventDeploymentHelper.getPersistedInstanceOfEventDefinition(eventDefinition);

            if (persistedEventDefinition != null) {
                eventDefinition.setId(persistedEventDefinition.getId());
                eventDefinition.setVersion(persistedEventDefinition.getVersion());
            }
        }
    }
    
    /**
     * Loads the persisted version of each channel definition and set values on the in-memory version to be consistent.
     */
    protected void makeChannelDefinitionsConsistentWithPersistedVersions(ParsedDeployment parsedDeployment) {
        for (ChannelDefinitionEntity channelDefinition : parsedDeployment.getAllChannelDefinitions()) {
            ChannelDefinitionEntity persistedChannelDefinition = channelDeploymentHelper.getPersistedInstanceOfChannelDefinition(channelDefinition);

            if (persistedChannelDefinition != null) {
                channelDefinition.setId(persistedChannelDefinition.getId());
                channelDefinition.setVersion(persistedChannelDefinition.getVersion());
            }
        }
    }

    public IdGenerator getIdGenerator() {
        return idGenerator;
    }

    public void setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    public ParsedDeploymentBuilderFactory getExParsedDeploymentBuilderFactory() {
        return parsedDeploymentBuilderFactory;
    }

    public void setParsedDeploymentBuilderFactory(ParsedDeploymentBuilderFactory parsedDeploymentBuilderFactory) {
        this.parsedDeploymentBuilderFactory = parsedDeploymentBuilderFactory;
    }

    public EventDefinitionDeploymentHelper getEventDeploymentHelper() {
        return eventDeploymentHelper;
    }

    public void setEventDeploymentHelper(EventDefinitionDeploymentHelper eventDeploymentHelper) {
        this.eventDeploymentHelper = eventDeploymentHelper;
    }    

    public ChannelDefinitionDeploymentHelper getChannelDeploymentHelper() {
        return channelDeploymentHelper;
    }

    public void setChannelDeploymentHelper(ChannelDefinitionDeploymentHelper channelDeploymentHelper) {
        this.channelDeploymentHelper = channelDeploymentHelper;
    }

    public CachingAndArtifactsManager getCachingAndArtifcatsManager() {
        return cachingAndArtifactsManager;
    }

    public void setCachingAndArtifactsManager(CachingAndArtifactsManager manager) {
        this.cachingAndArtifactsManager = manager;
    }

    public boolean isUsePrefixId() {
        return usePrefixId;
    }

    public void setUsePrefixId(boolean usePrefixId) {
        this.usePrefixId = usePrefixId;
    }
}
