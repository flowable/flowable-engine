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
package org.flowable.engine.impl.bpmn.deployer;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.UserTask;
import org.flowable.bpmn.model.ValuedDataObject;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.repository.EngineDeployment;
import org.flowable.common.engine.api.repository.EngineResource;
import org.flowable.common.engine.impl.EngineDeployer;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.DynamicBpmnConstants;
import org.flowable.engine.DynamicBpmnService;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cmd.DeploymentSettings;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.persistence.entity.ResourceEntity;
import org.flowable.engine.impl.persistence.entity.ResourceEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class BpmnDeployer implements EngineDeployer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BpmnDeployer.class);

    protected IdGenerator idGenerator;
    protected ParsedDeploymentBuilderFactory parsedDeploymentBuilderFactory;
    protected BpmnDeploymentHelper bpmnDeploymentHelper;
    protected CachingAndArtifactsManager cachingAndArtifactsManager;
    protected ProcessDefinitionDiagramHelper processDefinitionDiagramHelper;

    @Override
    public void deploy(EngineDeployment deployment, Map<String, Object> deploymentSettings) {
        LOGGER.debug("Processing deployment {}", deployment.getName());

        // The ParsedDeployment represents the deployment, the process definitions, and the BPMN
        // resource, parse, and model associated with each process definition.
        ParsedDeployment parsedDeployment = parsedDeploymentBuilderFactory
                .getBuilderForDeploymentAndSettings(deployment, deploymentSettings)
                .build();

        bpmnDeploymentHelper.verifyProcessDefinitionsDoNotShareKeys(parsedDeployment.getAllProcessDefinitions());

        bpmnDeploymentHelper.copyDeploymentValuesToProcessDefinitions(
                parsedDeployment.getDeployment(), parsedDeployment.getAllProcessDefinitions());
        bpmnDeploymentHelper.setResourceNamesOnProcessDefinitions(parsedDeployment);

        createAndPersistNewDiagramsIfNeeded(parsedDeployment);
        setProcessDefinitionDiagramNames(parsedDeployment);

        if (deployment.isNew()) {
            if (!deploymentSettings.containsKey(DeploymentSettings.IS_DERIVED_DEPLOYMENT)) {
                Map<ProcessDefinitionEntity, ProcessDefinitionEntity> mapOfNewProcessDefinitionToPreviousVersion = getPreviousVersionsOfProcessDefinitions(parsedDeployment);
                setProcessDefinitionVersionsAndIds(parsedDeployment, mapOfNewProcessDefinitionToPreviousVersion);
                persistProcessDefinitionsAndAuthorizations(parsedDeployment);
                updateTimersAndEvents(parsedDeployment, mapOfNewProcessDefinitionToPreviousVersion);

            } else {
                Map<ProcessDefinitionEntity, ProcessDefinitionEntity> mapOfNewProcessDefinitionToPreviousDerivedVersion = 
                                getPreviousDerivedFromVersionsOfProcessDefinitions(parsedDeployment);
                setDerivedProcessDefinitionVersionsAndIds(parsedDeployment, mapOfNewProcessDefinitionToPreviousDerivedVersion, deploymentSettings);
                persistProcessDefinitionsAndAuthorizations(parsedDeployment);
            }
          
        } else {
            makeProcessDefinitionsConsistentWithPersistedVersions(parsedDeployment);
        }

        cachingAndArtifactsManager.updateCachingAndArtifacts(parsedDeployment);

        if (deployment.isNew()) {
            dispatchProcessDefinitionEntityInitializedEvent(parsedDeployment);
        }

        for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
            BpmnModel bpmnModel = parsedDeployment.getBpmnModelForProcessDefinition(processDefinition);
            createLocalizationValues(processDefinition.getId(), bpmnModel.getProcessById(processDefinition.getKey()));
        }
    }

    /**
     * Creates new diagrams for process definitions if the deployment is new, the process definition in question supports it, and the engine is configured to make new diagrams.
     *
     * When this method creates a new diagram, it also persists it via the ResourceEntityManager and adds it to the resources of the deployment.
     */
    protected void createAndPersistNewDiagramsIfNeeded(ParsedDeployment parsedDeployment) {

        final ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        final DeploymentEntity deploymentEntity = parsedDeployment.getDeployment();

        final ResourceEntityManager resourceEntityManager = processEngineConfiguration.getResourceEntityManager();

        for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
            if (processDefinitionDiagramHelper.shouldCreateDiagram(processDefinition, deploymentEntity)) {
                ResourceEntity resource = processDefinitionDiagramHelper.createDiagramForProcessDefinition(
                        processDefinition, parsedDeployment.getBpmnParseForProcessDefinition(processDefinition));
                if (resource != null) {
                    resourceEntityManager.insert(resource, false);
                    deploymentEntity.addResource(resource); // now we'll find it if we look for the diagram name later.
                }
            }
        }
    }

    /**
     * Updates all the process definition entities to have the correct diagram resource name. Must be called after createAndPersistNewDiagramsAsNeeded to ensure that any newly-created diagrams already
     * have their resources attached to the deployment.
     */
    protected void setProcessDefinitionDiagramNames(ParsedDeployment parsedDeployment) {
        Map<String, EngineResource> resources = parsedDeployment.getDeployment().getResources();

        for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
            String diagramResourceName = ResourceNameUtil.getProcessDiagramResourceNameFromDeployment(processDefinition, resources);
            processDefinition.setDiagramResourceName(diagramResourceName);
        }
    }

    /**
     * Constructs a map from new ProcessDefinitionEntities to the previous version by key and tenant. If no previous version exists, no map entry is created.
     */
    protected Map<ProcessDefinitionEntity, ProcessDefinitionEntity> getPreviousVersionsOfProcessDefinitions(
            ParsedDeployment parsedDeployment) {

        Map<ProcessDefinitionEntity, ProcessDefinitionEntity> result = new LinkedHashMap<>();

        for (ProcessDefinitionEntity newDefinition : parsedDeployment.getAllProcessDefinitions()) {
            ProcessDefinitionEntity existingDefinition = bpmnDeploymentHelper.getMostRecentVersionOfProcessDefinition(newDefinition);

            if (existingDefinition != null) {
                result.put(newDefinition, existingDefinition);
            }
        }

        return result;
    }
    
    /**
     * Constructs a map from new ProcessDefinitionEntities to the previous derived from version by key and tenant. If no previous version exists, no map entry is created.
     */
    protected Map<ProcessDefinitionEntity, ProcessDefinitionEntity> getPreviousDerivedFromVersionsOfProcessDefinitions(
            ParsedDeployment parsedDeployment) {

        Map<ProcessDefinitionEntity, ProcessDefinitionEntity> result = new LinkedHashMap<ProcessDefinitionEntity, ProcessDefinitionEntity>();

        for (ProcessDefinitionEntity newDefinition : parsedDeployment.getAllProcessDefinitions()) {
            ProcessDefinitionEntity existingDefinition = bpmnDeploymentHelper.getMostRecentDerivedVersionOfProcessDefinition(newDefinition);

            if (existingDefinition != null) {
                result.put(newDefinition, existingDefinition);
            }
        }

        return result;
    }

    /**
     * Sets the version on each process definition entity, and the identifier. If the map contains an older version for a process definition, then the version is set to that older entity's version
     * plus one; otherwise it is set to 1. Also dispatches an ENTITY_CREATED event.
     */
    protected void setProcessDefinitionVersionsAndIds(ParsedDeployment parsedDeployment,
            Map<ProcessDefinitionEntity, ProcessDefinitionEntity> mapNewToOldProcessDefinitions) {
        CommandContext commandContext = Context.getCommandContext();

        for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
            int version = 1;

            ProcessDefinitionEntity latest = mapNewToOldProcessDefinitions.get(processDefinition);
            if (latest != null) {
                version = latest.getVersion() + 1;
            }

            processDefinition.setVersion(version);
            processDefinition.setId(getIdForNewProcessDefinition(processDefinition));
            Process process = parsedDeployment.getProcessModelForProcessDefinition(processDefinition);
            FlowElement initialElement = process.getInitialFlowElement();
            if (initialElement instanceof StartEvent) {
                StartEvent startEvent = (StartEvent) initialElement;
                if (startEvent.getFormKey() != null) {
                    processDefinition.setHasStartFormKey(true);
                }
            }

            cachingAndArtifactsManager.updateProcessDefinitionCache(parsedDeployment);

            if (CommandContextUtil.getProcessEngineConfiguration(commandContext).getEventDispatcher().isEnabled()) {
                CommandContextUtil.getProcessEngineConfiguration(commandContext).getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_CREATED, processDefinition));
            }
        }
    }
    
    protected void setDerivedProcessDefinitionVersionsAndIds(ParsedDeployment parsedDeployment, 
            Map<ProcessDefinitionEntity, ProcessDefinitionEntity> mapNewToOldProcessDefinitions, Map<String, Object> deploymentSettings) {
        
        CommandContext commandContext = Context.getCommandContext();
        
        for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
            int derivedVersion = 1;
            
            ProcessDefinitionEntity latest = mapNewToOldProcessDefinitions.get(processDefinition);
            if (latest != null) {
                derivedVersion = latest.getDerivedVersion() + 1;
            }
            
            processDefinition.setVersion(0);
            processDefinition.setDerivedVersion(derivedVersion);
            processDefinition.setId(idGenerator.getNextId());
            processDefinition.setDerivedFrom((String) deploymentSettings.get(DeploymentSettings.DERIVED_PROCESS_DEFINITION_ID));
            processDefinition.setDerivedFromRoot((String) deploymentSettings.get(DeploymentSettings.DERIVED_PROCESS_DEFINITION_ROOT_ID));

            Process process = parsedDeployment.getProcessModelForProcessDefinition(processDefinition);
            FlowElement initialElement = process.getInitialFlowElement();
            if (initialElement instanceof StartEvent) {
                StartEvent startEvent = (StartEvent) initialElement;
                if (startEvent.getFormKey() != null) {
                    processDefinition.setHasStartFormKey(true);
                }
            }
            
            cachingAndArtifactsManager.updateProcessDefinitionCache(parsedDeployment);

            if (CommandContextUtil.getProcessEngineConfiguration(commandContext).getEventDispatcher().isEnabled()) {
                CommandContextUtil.getProcessEngineConfiguration(commandContext).getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_CREATED, processDefinition));
            }
        }
    }

    /**
     * Saves each process definition. It is assumed that the deployment is new, the definitions have never been saved before, and that they have all their values properly set up.
     */
    protected void persistProcessDefinitionsAndAuthorizations(ParsedDeployment parsedDeployment) {
        CommandContext commandContext = Context.getCommandContext();
        ProcessDefinitionEntityManager processDefinitionManager = CommandContextUtil.getProcessDefinitionEntityManager(commandContext);

        for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
            processDefinitionManager.insert(processDefinition, false);
            bpmnDeploymentHelper.addAuthorizationsForNewProcessDefinition(parsedDeployment.getProcessModelForProcessDefinition(processDefinition), processDefinition);
        }
    }

    protected void updateTimersAndEvents(ParsedDeployment parsedDeployment,
            Map<ProcessDefinitionEntity, ProcessDefinitionEntity> mapNewToOldProcessDefinitions) {

        for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
            bpmnDeploymentHelper.updateTimersAndEvents(processDefinition,
                    mapNewToOldProcessDefinitions.get(processDefinition),
                    parsedDeployment);
        }
    }

    protected void dispatchProcessDefinitionEntityInitializedEvent(ParsedDeployment parsedDeployment) {
        CommandContext commandContext = Context.getCommandContext();
        for (ProcessDefinitionEntity processDefinitionEntity : parsedDeployment.getAllProcessDefinitions()) {
            if (CommandContextUtil.getProcessEngineConfiguration(commandContext).getEventDispatcher().isEnabled()) {
                CommandContextUtil.getProcessEngineConfiguration(commandContext).getEventDispatcher().dispatchEvent(
                        FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_INITIALIZED, processDefinitionEntity));
            }
        }
    }

    /**
     * Returns the ID to use for a new process definition; subclasses may override this to provide their own identification scheme.
     *
     * Process definition ids NEED to be unique across the whole engine!
     */
    protected String getIdForNewProcessDefinition(ProcessDefinitionEntity processDefinition) {
        String nextId = idGenerator.getNextId();

        String result = processDefinition.getKey() + ":" + processDefinition.getVersion() + ":" + nextId; // ACT-505
        // ACT-115: maximum id length is 64 characters
        if (result.length() > 64) {
            result = nextId;
        }

        return result;
    }

    /**
     * Loads the persisted version of each process definition and set values on the in-memory version to be consistent.
     */
    protected void makeProcessDefinitionsConsistentWithPersistedVersions(ParsedDeployment parsedDeployment) {
        for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
            ProcessDefinitionEntity persistedProcessDefinition = bpmnDeploymentHelper.getPersistedInstanceOfProcessDefinition(processDefinition);

            if (persistedProcessDefinition != null) {
                processDefinition.setId(persistedProcessDefinition.getId());
                processDefinition.setVersion(persistedProcessDefinition.getVersion());
                processDefinition.setSuspensionState(persistedProcessDefinition.getSuspensionState());
                processDefinition.setHasStartFormKey(persistedProcessDefinition.hasStartFormKey());
                processDefinition.setGraphicalNotationDefined(persistedProcessDefinition.isGraphicalNotationDefined());
            }
        }
    }

    protected void createLocalizationValues(String processDefinitionId, Process process) {
        if (process == null)
            return;

        CommandContext commandContext = Context.getCommandContext();
        DynamicBpmnService dynamicBpmnService = CommandContextUtil.getProcessEngineConfiguration(commandContext).getDynamicBpmnService();
        ObjectNode infoNode = dynamicBpmnService.getProcessDefinitionInfo(processDefinitionId);

        boolean localizationValuesChanged = false;
        List<ExtensionElement> localizationElements = process.getExtensionElements().get("localization");
        if (localizationElements != null) {
            for (ExtensionElement localizationElement : localizationElements) {
                if (BpmnXMLConstants.FLOWABLE_EXTENSIONS_PREFIX.equals(localizationElement.getNamespacePrefix()) ||
                        BpmnXMLConstants.ACTIVITI_EXTENSIONS_PREFIX.equals(localizationElement.getNamespacePrefix())) {

                    String locale = localizationElement.getAttributeValue(null, "locale");
                    String name = localizationElement.getAttributeValue(null, "name");
                    String documentation = null;
                    List<ExtensionElement> documentationElements = localizationElement.getChildElements().get("documentation");
                    if (documentationElements != null) {
                        for (ExtensionElement documentationElement : documentationElements) {
                            documentation = StringUtils.trimToNull(documentationElement.getElementText());
                            break;
                        }
                    }

                    String processId = process.getId();
                    if (!isEqualToCurrentLocalizationValue(locale, processId, "name", name, infoNode)) {
                        dynamicBpmnService.changeLocalizationName(locale, processId, name, infoNode);
                        localizationValuesChanged = true;
                    }

                    if (documentation != null && !isEqualToCurrentLocalizationValue(locale, processId, "description", documentation, infoNode)) {
                        dynamicBpmnService.changeLocalizationDescription(locale, processId, documentation, infoNode);
                        localizationValuesChanged = true;
                    }
                }
            }
        }

        boolean isFlowElementLocalizationChanged = localizeFlowElements(process.getFlowElements(), infoNode);
        boolean isDataObjectLocalizationChanged = localizeDataObjectElements(process.getDataObjects(), infoNode);
        if (isFlowElementLocalizationChanged || isDataObjectLocalizationChanged) {
            localizationValuesChanged = true;
        }

        if (localizationValuesChanged) {
            dynamicBpmnService.saveProcessDefinitionInfo(processDefinitionId, infoNode);
        }
    }

    protected boolean localizeFlowElements(Collection<FlowElement> flowElements, ObjectNode infoNode) {
        boolean localizationValuesChanged = false;

        if (flowElements == null)
            return localizationValuesChanged;

        CommandContext commandContext = Context.getCommandContext();
        DynamicBpmnService dynamicBpmnService = CommandContextUtil.getProcessEngineConfiguration(commandContext).getDynamicBpmnService();

        for (FlowElement flowElement : flowElements) {
            if (flowElement instanceof UserTask || flowElement instanceof SubProcess) {
                List<ExtensionElement> localizationElements = flowElement.getExtensionElements().get("localization");
                if (localizationElements != null) {
                    for (ExtensionElement localizationElement : localizationElements) {
                        if (BpmnXMLConstants.FLOWABLE_EXTENSIONS_PREFIX.equals(localizationElement.getNamespacePrefix()) ||
                                BpmnXMLConstants.ACTIVITI_EXTENSIONS_PREFIX.equals(localizationElement.getNamespacePrefix())) {

                            String locale = localizationElement.getAttributeValue(null, "locale");
                            String name = localizationElement.getAttributeValue(null, "name");
                            String documentation = null;
                            List<ExtensionElement> documentationElements = localizationElement.getChildElements().get("documentation");
                            if (documentationElements != null) {
                                for (ExtensionElement documentationElement : documentationElements) {
                                    documentation = StringUtils.trimToNull(documentationElement.getElementText());
                                    break;
                                }
                            }

                            String flowElementId = flowElement.getId();
                            if (!isEqualToCurrentLocalizationValue(locale, flowElementId, "name", name, infoNode)) {
                                dynamicBpmnService.changeLocalizationName(locale, flowElementId, name, infoNode);
                                localizationValuesChanged = true;
                            }

                            if (documentation != null && !isEqualToCurrentLocalizationValue(locale, flowElementId, "description", documentation, infoNode)) {
                                dynamicBpmnService.changeLocalizationDescription(locale, flowElementId, documentation, infoNode);
                                localizationValuesChanged = true;
                            }
                        }
                    }
                }

                if (flowElement instanceof SubProcess) {
                    SubProcess subprocess = (SubProcess) flowElement;
                    boolean isFlowElementLocalizationChanged = localizeFlowElements(subprocess.getFlowElements(), infoNode);
                    boolean isDataObjectLocalizationChanged = localizeDataObjectElements(subprocess.getDataObjects(), infoNode);
                    if (isFlowElementLocalizationChanged || isDataObjectLocalizationChanged) {
                        localizationValuesChanged = true;
                    }
                }
            }
        }

        return localizationValuesChanged;
    }

    protected boolean isEqualToCurrentLocalizationValue(String language, String id, String propertyName, String propertyValue, ObjectNode infoNode) {
        boolean isEqual = false;
        JsonNode localizationNode = infoNode.path("localization").path(language).path(id).path(propertyName);
        if (!localizationNode.isMissingNode() && !localizationNode.isNull() && localizationNode.asText().equals(propertyValue)) {
            isEqual = true;
        }
        return isEqual;
    }

    protected boolean localizeDataObjectElements(List<ValuedDataObject> dataObjects, ObjectNode infoNode) {
        boolean localizationValuesChanged = false;
        CommandContext commandContext = Context.getCommandContext();
        DynamicBpmnService dynamicBpmnService = CommandContextUtil.getProcessEngineConfiguration(commandContext).getDynamicBpmnService();

        for (ValuedDataObject dataObject : dataObjects) {
            List<ExtensionElement> localizationElements = dataObject.getExtensionElements().get("localization");
            if (localizationElements != null) {
                for (ExtensionElement localizationElement : localizationElements) {
                    if (BpmnXMLConstants.FLOWABLE_EXTENSIONS_PREFIX.equals(localizationElement.getNamespacePrefix()) ||
                            BpmnXMLConstants.ACTIVITI_EXTENSIONS_PREFIX.equals(localizationElement.getNamespacePrefix())) {

                        String locale = localizationElement.getAttributeValue(null, "locale");
                        String name = localizationElement.getAttributeValue(null, "name");
                        String documentation = null;

                        List<ExtensionElement> documentationElements = localizationElement.getChildElements().get("documentation");
                        if (documentationElements != null) {
                            for (ExtensionElement documentationElement : documentationElements) {
                                documentation = StringUtils.trimToNull(documentationElement.getElementText());
                                break;
                            }
                        }

                        if (name != null && !isEqualToCurrentLocalizationValue(locale, dataObject.getId(), DynamicBpmnConstants.LOCALIZATION_NAME, name, infoNode)) {
                            dynamicBpmnService.changeLocalizationName(locale, dataObject.getId(), name, infoNode);
                            localizationValuesChanged = true;
                        }

                        if (documentation != null && !isEqualToCurrentLocalizationValue(locale, dataObject.getId(),
                                DynamicBpmnConstants.LOCALIZATION_DESCRIPTION, documentation, infoNode)) {

                            dynamicBpmnService.changeLocalizationDescription(locale, dataObject.getId(), documentation, infoNode);
                            localizationValuesChanged = true;
                        }
                    }
                }
            }
        }

        return localizationValuesChanged;
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

    public BpmnDeploymentHelper getBpmnDeploymentHelper() {
        return bpmnDeploymentHelper;
    }

    public void setBpmnDeploymentHelper(BpmnDeploymentHelper bpmnDeploymentHelper) {
        this.bpmnDeploymentHelper = bpmnDeploymentHelper;
    }

    public CachingAndArtifactsManager getCachingAndArtifcatsManager() {
        return cachingAndArtifactsManager;
    }

    public void setCachingAndArtifactsManager(CachingAndArtifactsManager manager) {
        this.cachingAndArtifactsManager = manager;
    }

    public ProcessDefinitionDiagramHelper getProcessDefinitionDiagramHelper() {
        return processDefinitionDiagramHelper;
    }

    public void setProcessDefinitionDiagramHelper(ProcessDefinitionDiagramHelper processDefinitionDiagramHelper) {
        this.processDefinitionDiagramHelper = processDefinitionDiagramHelper;
    }
}
