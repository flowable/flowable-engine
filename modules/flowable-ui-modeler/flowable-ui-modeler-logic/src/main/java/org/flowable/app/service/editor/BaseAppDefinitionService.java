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
package org.flowable.app.service.editor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.app.domain.editor.AbstractModel;
import org.flowable.app.domain.editor.AppDefinition;
import org.flowable.app.domain.editor.AppModelDefinition;
import org.flowable.app.domain.editor.Model;
import org.flowable.app.repository.editor.ModelRepository;
import org.flowable.app.service.api.ModelService;
import org.flowable.app.service.exception.BadRequestException;
import org.flowable.app.service.exception.InternalServerErrorException;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.UserTask;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.xml.converter.DmnXMLConverter;
import org.flowable.editor.dmn.converter.DmnJsonConverter;
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

    protected DmnJsonConverter dmnJsonConverter = new DmnJsonConverter();
    protected DmnXMLConverter dmnXMLConverter = new DmnXMLConverter();

    protected Map<String, StartEvent> processNoneStartEvents(BpmnModel bpmnModel) {
        Map<String, StartEvent> startEventMap = new HashMap<String, StartEvent>();
        for (Process process : bpmnModel.getProcesses()) {
            for (FlowElement flowElement : process.getFlowElements()) {
                if (flowElement instanceof StartEvent) {
                    StartEvent startEvent = (StartEvent) flowElement;
                    if (org.apache.commons.collections.CollectionUtils.isEmpty(startEvent.getEventDefinitions())) {
                        if (StringUtils.isEmpty(startEvent.getInitiator())) {
                            startEvent.setInitiator("initiator");
                        }
                        startEventMap.put(process.getId(), startEvent);
                        break;
                    }
                }
            }
        }
        return startEventMap;
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

        if (CollectionUtils.isNotEmpty(appDefinition.getModels())) {
            String appDefinitionJson = getAppDefinitionJson(appDefinitionModel, appDefinition);
            byte[] appDefinitionJsonBytes = appDefinitionJson.getBytes(StandardCharsets.UTF_8);

            deployableAssets.put(appDefinitionModel.getKey() + ".app", appDefinitionJsonBytes);

            Map<String, Model> formMap = new HashMap<>();
            Map<String, Model> decisionTableMap = new HashMap<>();

            for (AppModelDefinition appModelDef : appDefinition.getModels()) {

                AbstractModel processModel = modelService.getModel(appModelDef.getId());
                if (processModel == null) {
                    throw new BadRequestException(String.format("Model %s for app definition %s could not be found", appModelDef.getId(), appDefinitionModel.getId()));
                }

                List<Model> referencedModels = modelRepository.findByParentModelId(processModel.getId());
                for (Model childModel : referencedModels) {
                    if (Model.MODEL_TYPE_FORM == childModel.getModelType()) {
                        formMap.put(childModel.getId(), childModel);

                    } else if (Model.MODEL_TYPE_DECISION_TABLE == childModel.getModelType()) {
                        decisionTableMap.put(childModel.getId(), childModel);
                    }
                }

                BpmnModel bpmnModel = modelService.getBpmnModel(processModel, formMap, decisionTableMap, true);
                Map<String, StartEvent> startEventMap = processNoneStartEvents(bpmnModel);

                for (Process process : bpmnModel.getProcesses()) {
                    processUserTasks(process.getFlowElements(), process, startEventMap);
                }

                byte[] modelXML = modelService.getBpmnXML(bpmnModel);
                deployableAssets.put(processModel.getKey().replaceAll(" ", "") + ".bpmn", modelXML);
            }

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

    protected byte[] createDeployZipArtifact(Map<String, byte[]> deployableAssets) {
        ByteArrayOutputStream baos = null;
        ZipOutputStream zos = null;
        byte[] deployZipArtifact = null;
        try {
            baos = new ByteArrayOutputStream();
            zos = new ZipOutputStream(baos);

            for (Map.Entry<String, byte[]> entry : deployableAssets.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zipEntry.setSize(entry.getValue().length);
                zos.putNextEntry(zipEntry);
                zos.write(entry.getValue());
                zos.closeEntry();
            }
            
            IOUtils.closeQuietly(zos);

            // this is the zip file as byte[]
            deployZipArtifact = baos.toByteArray();
            
            IOUtils.closeQuietly(baos);
            
        } catch (IOException ioe) {
            throw new InternalServerErrorException("Could not create deploy zip artifact");
        }

        return deployZipArtifact;
    }
}
