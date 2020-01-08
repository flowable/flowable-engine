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
package org.flowable.ui.modeler.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SendEventServiceTask;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.UserTask;
import org.flowable.cmmn.converter.CmmnXmlConverter;
import org.flowable.cmmn.editor.json.converter.CmmnJsonConverter;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.GenericEventListener;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.xml.converter.DmnXMLConverter;
import org.flowable.editor.dmn.converter.DmnJsonConverter;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;
import org.flowable.eventregistry.json.converter.ChannelJsonConverter;
import org.flowable.eventregistry.json.converter.EventJsonConverter;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventregistry.model.EventModel;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.flowable.ui.common.service.exception.InternalServerErrorException;
import org.flowable.ui.modeler.domain.AbstractModel;
import org.flowable.ui.modeler.domain.AppDefinition;
import org.flowable.ui.modeler.domain.AppModelDefinition;
import org.flowable.ui.modeler.domain.Model;
import org.flowable.ui.modeler.repository.ModelRepository;
import org.flowable.ui.modeler.serviceapi.ModelService;
import org.flowable.ui.modeler.util.BpmnEventModelUtil;
import org.flowable.ui.modeler.util.CmmnEventModelUtil;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Yvo Swillens
 */
public class BaseAppDefinitionService {

    @Autowired
    protected ModelService modelService;

    @Autowired
    protected ModelRepository modelRepository;

    @Autowired
    protected ObjectMapper objectMapper;
    
    protected BpmnJsonConverter bpmnJsonConverter = new BpmnJsonConverter();
    protected BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();

    protected DmnJsonConverter dmnJsonConverter = new DmnJsonConverter();
    protected DmnXMLConverter dmnXMLConverter = new DmnXMLConverter();
    
    protected CmmnJsonConverter cmmnJsonConverter = new CmmnJsonConverter();
    protected CmmnXmlConverter cmmnXMLConverter = new CmmnXmlConverter();
    
    protected EventJsonConverter eventJsonConverter = new EventJsonConverter();
    protected ChannelJsonConverter channelJsonConverter = new ChannelJsonConverter();

    protected void postProcessFlowElements(List<FlowElement> eventRegistryElements, Map<String, StartEvent> noneStartEventMap, BpmnModel bpmnModel) {
        for (Process process : bpmnModel.getProcesses()) {
            postProcessFlowElements(process.getFlowElements(), eventRegistryElements, noneStartEventMap, process.getId(), bpmnModel);
        }
    }
    
    protected void postProcessFlowElements(Collection<FlowElement> traverseElementList, List<FlowElement> eventRegistryElements, 
                    Map<String, StartEvent> noneStartEventMap, String processId, BpmnModel bpmnModel) {
        
        for (FlowElement flowElement : traverseElementList) {
            if (flowElement instanceof Event) {
                
                List<ExtensionElement> eventTypeElements = flowElement.getExtensionElements().get("eventType");
                if (eventTypeElements != null && eventTypeElements.size() > 0) {
                    eventRegistryElements.add(flowElement);
                }
                
                if (flowElement instanceof StartEvent) {
                    StartEvent startEvent = (StartEvent) flowElement;
                    if (CollectionUtils.isEmpty(startEvent.getEventDefinitions())) {
                        if (StringUtils.isEmpty(startEvent.getInitiator())) {
                            startEvent.setInitiator("initiator");
                        }
                        noneStartEventMap.put(processId, startEvent);
                    }
                }
                
            } else if (flowElement instanceof SendEventServiceTask) {
                SendEventServiceTask task = (SendEventServiceTask) flowElement;
                if (StringUtils.isNotEmpty(task.getEventType())) {
                    eventRegistryElements.add(flowElement);
                }
                
            } else if (flowElement instanceof SubProcess) {
                SubProcess subProcess = (SubProcess) flowElement;
                postProcessFlowElements(subProcess.getFlowElements(), eventRegistryElements, noneStartEventMap, processId, bpmnModel);
            }
        }
    }
    
    protected void postProcessPlanItemDefinitions(List<BaseElement> eventRegistryElements, CmmnModel cmmnModel) {
        for (Case caseModel : cmmnModel.getCases()) {
            if (StringUtils.isNotEmpty(caseModel.getStartEventType())) {
                eventRegistryElements.add(caseModel);
            }
            postProcessPlanItemDefinitions(caseModel.getPlanModel().getPlanItemDefinitions(), eventRegistryElements);
        }
    }
    
    protected void postProcessPlanItemDefinitions(Collection<PlanItemDefinition> traverseElementList, List<BaseElement> eventRegistryElements) {
        
        for (PlanItemDefinition planItemDefinition : traverseElementList) {
            if (planItemDefinition instanceof GenericEventListener) {
                GenericEventListener genericEventListener = (GenericEventListener) planItemDefinition;
                if (StringUtils.isNotEmpty(genericEventListener.getEventType())) {
                    eventRegistryElements.add(genericEventListener);
                }
                
            } else if (planItemDefinition instanceof org.flowable.cmmn.model.SendEventServiceTask) {
                org.flowable.cmmn.model.SendEventServiceTask task = (org.flowable.cmmn.model.SendEventServiceTask) planItemDefinition;
                if (StringUtils.isNotEmpty(task.getEventType())) {
                    eventRegistryElements.add(task);
                }
                
            } else if (planItemDefinition instanceof Stage) {
                Stage stage = (Stage) planItemDefinition;
                postProcessPlanItemDefinitions(stage.getPlanItemDefinitions(), eventRegistryElements);
            }
        }
    }

    protected void processUserTasks(Collection<FlowElement> flowElements, Process process, Map<String, StartEvent> startEventMap) {

        for (FlowElement flowElement : flowElements) {
            if (flowElement instanceof UserTask) {
                UserTask userTask = (UserTask) flowElement;
                if ("$INITIATOR".equals(userTask.getAssignee())) {
                    if (startEventMap.get(process.getId()) != null) {
                        userTask.setAssignee("${" + startEventMap.get(process.getId()).getInitiator() + "}");
                    }
                }

            } else if (flowElement instanceof SubProcess) {
                processUserTasks(((SubProcess) flowElement).getFlowElements(), process, startEventMap);
            }
        }
    }

    protected String getAppDefinitionJson(Model appDefinitionModel, AppDefinition appDefinition) {
        ObjectNode appDefinitionNode = objectMapper.createObjectNode();
        appDefinitionNode.put("key", appDefinitionModel.getKey());
        appDefinitionNode.put("name", appDefinitionModel.getName());
        appDefinitionNode.put("description", appDefinitionModel.getDescription());
        appDefinitionNode.put("theme", appDefinition.getTheme());
        appDefinitionNode.put("icon", appDefinition.getIcon());
        appDefinitionNode.put("usersAccess", appDefinition.getUsersAccess());
        appDefinitionNode.put("groupsAccess", appDefinition.getGroupsAccess());
        return appDefinitionNode.toString();
    }

    protected AppDefinition resolveAppDefinition(Model appDefinitionModel) throws Exception {
        AppDefinition appDefinition = objectMapper.readValue(appDefinitionModel.getModelEditorJson(), AppDefinition.class);
        return appDefinition;
    }

    protected byte[] createDeployableZipArtifact(Model appDefinitionModel, AppDefinition appDefinition) {

        byte[] deployZipArtifact = null;
        Map<String, byte[]> deployableAssets = new HashMap<>();

        if (CollectionUtils.isNotEmpty(appDefinition.getModels()) || CollectionUtils.isNotEmpty(appDefinition.getCmmnModels())) {
            String appDefinitionJson = getAppDefinitionJson(appDefinitionModel, appDefinition);
            byte[] appDefinitionJsonBytes = appDefinitionJson.getBytes(StandardCharsets.UTF_8);

            deployableAssets.put(appDefinitionModel.getKey() + ".app", appDefinitionJsonBytes);

            Map<String, Model> formMap = new HashMap<>();
            Map<String, Model> decisionTableMap = new HashMap<>();
            Map<String, Model> caseModelMap = new HashMap<>();
            Map<String, Model> processModelMap = new HashMap<>();

            createDeployableAppModels(appDefinitionModel, appDefinition, deployableAssets, formMap, decisionTableMap, caseModelMap, processModelMap);

            if (formMap.size() > 0) {
                for (String formId : formMap.keySet()) {
                    Model formInfo = formMap.get(formId);
                    String formModelEditorJson = formInfo.getModelEditorJson();
                    byte[] formModelEditorJsonBytes = formModelEditorJson.getBytes(StandardCharsets.UTF_8);
                    deployableAssets.put("form-" + formInfo.getKey() + ".form", formModelEditorJsonBytes);
                }
            }

            if (decisionTableMap.size() > 0) {
                for (String decisionTableId : decisionTableMap.keySet()) {
                    Model decisionTableInfo = decisionTableMap.get(decisionTableId);
                    try {
                        JsonNode decisionTableNode = objectMapper.readTree(decisionTableInfo.getModelEditorJson());
                        DmnDefinition dmnDefinition = dmnJsonConverter.convertToDmn(decisionTableNode, decisionTableInfo.getId(),
                                decisionTableInfo.getVersion(), decisionTableInfo.getLastUpdated());
                        byte[] dmnXMLBytes = dmnXMLConverter.convertToXML(dmnDefinition);
                        deployableAssets.put("dmn-" + decisionTableInfo.getKey() + ".dmn", dmnXMLBytes);
                    } catch (Exception e) {
                        throw new InternalServerErrorException(String.format("Error converting decision table %s to XML", decisionTableInfo.getName()));
                    }
                }
            }

            deployZipArtifact = createDeployZipArtifact(deployableAssets);
        }

        return deployZipArtifact;
    }
    
    protected void createDeployableAppModels(Model appDefinitionModel, AppDefinition appDefinition, Map<String, byte[]> deployableAssets, 
                    Map<String, Model> formMap, Map<String, Model> decisionTableMap, Map<String, Model> caseModelMap, Map<String, Model> processModelMap) {
        
        List<AppModelDefinition> appModels = new ArrayList<>();
        if (appDefinition.getModels() != null) {
            appModels.addAll(appDefinition.getModels());
        }
        
        if (appDefinition.getCmmnModels() != null) {
            appModels.addAll(appDefinition.getCmmnModels());
        }
        
        for (AppModelDefinition appModelDef : appModels) {
            
            if (caseModelMap.containsKey(appModelDef.getId()) || processModelMap.containsKey(appModelDef.getId())) {
                return;
            }

            AbstractModel model = modelService.getModel(appModelDef.getId());
            if (model == null) {
                throw new BadRequestException(String.format("Model %s for app definition %s could not be found", appModelDef.getId(), appDefinitionModel.getId()));
            }

            createDeployableModels(model, deployableAssets, formMap, decisionTableMap, caseModelMap, processModelMap);
        }
    }
    
    protected void createDeployableModels(AbstractModel parentModel, Map<String, byte[]> deployableAssets, 
                    Map<String, Model> formMap, Map<String, Model> decisionTableMap, Map<String, Model> caseModelMap, Map<String, Model> processModelMap) {
        
        List<Model> referencedModels = modelRepository.findByParentModelId(parentModel.getId());
        for (Model childModel : referencedModels) {
            if (Model.MODEL_TYPE_FORM == childModel.getModelType()) {
                formMap.put(childModel.getId(), childModel);

            } else if (Model.MODEL_TYPE_DECISION_TABLE == childModel.getModelType()) {
                decisionTableMap.put(childModel.getId(), childModel);
            
            } else if (Model.MODEL_TYPE_CMMN == childModel.getModelType()) {
                caseModelMap.put(childModel.getId(), childModel);
                createDeployableModels(childModel, deployableAssets, formMap, decisionTableMap, caseModelMap, processModelMap);
            
            } else if (Model.MODEL_TYPE_BPMN == childModel.getModelType()) {
                processModelMap.put(childModel.getId(), childModel);
                createDeployableModels(childModel, deployableAssets, formMap, decisionTableMap, caseModelMap, processModelMap);
            }
        }

        Map<String, EventModel> eventModelMap = new HashMap<>();
        Map<String, ChannelModel> channelModelMap = new HashMap<>();
        if (parentModel.getModelType() == null || parentModel.getModelType() == AbstractModel.MODEL_TYPE_BPMN) {
            BpmnModel bpmnModel = modelService.getBpmnModel(parentModel, formMap, decisionTableMap);
            List<FlowElement> eventRegistryElements = new ArrayList<>();
            Map<String, StartEvent> noneStartEventMap = new HashMap<>();
            postProcessFlowElements(eventRegistryElements, noneStartEventMap, bpmnModel);
            
            BpmnEventModelUtil.fillEventModelMap(eventRegistryElements, eventModelMap);
            BpmnEventModelUtil.fillChannelModelMap(eventRegistryElements, channelModelMap);

            for (Process process : bpmnModel.getProcesses()) {
                processUserTasks(process.getFlowElements(), process, noneStartEventMap);
            }

            byte[] modelXML = modelService.getBpmnXML(bpmnModel);
            deployableAssets.put(parentModel.getKey().replaceAll(" ", "") + ".bpmn", modelXML);
            
        } else {
            CmmnModel cmmnModel = modelService.getCmmnModel(parentModel, formMap, decisionTableMap, caseModelMap, processModelMap);
            List<BaseElement> eventRegistryElements = new ArrayList<>();
            postProcessPlanItemDefinitions(eventRegistryElements, cmmnModel);
            
            CmmnEventModelUtil.fillEventModelMap(eventRegistryElements, eventModelMap);
            CmmnEventModelUtil.fillChannelModelMap(eventRegistryElements, channelModelMap);
            
            byte[] modelXML = modelService.getCmmnXML(cmmnModel);
            deployableAssets.put(parentModel.getKey().replaceAll(" ", "") + ".cmmn", modelXML);
        }
        
        if (eventModelMap.size() > 0) {
            for (EventModel eventModel : eventModelMap.values()) {
                String eventJson = eventJsonConverter.convertToJson(eventModel);
                deployableAssets.put("event-" + eventModel.getKey() + ".event", eventJson.getBytes(StandardCharsets.UTF_8));
            }
            
            if (channelModelMap.size() > 0) {
                for (ChannelModel channelModel : channelModelMap.values()) {
                    String channelJson = channelJsonConverter.convertToJson(channelModel);
                    deployableAssets.put("channel-" + channelModel.getKey() + ".channel", channelJson.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }

    protected byte[] createDeployZipArtifact(Map<String, byte[]> deployableAssets) {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ZipOutputStream zos = new ZipOutputStream(baos)) {
            
            for (Map.Entry<String, byte[]> entry : deployableAssets.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zipEntry.setSize(entry.getValue().length);
                zos.putNextEntry(zipEntry);
                zos.write(entry.getValue());
                zos.closeEntry();
            }

            // this is the zip file as byte[]
            return baos.toByteArray();

        } catch (IOException ioe) {
            throw new InternalServerErrorException("Could not create deploy zip artifact");
        }
    }
}
