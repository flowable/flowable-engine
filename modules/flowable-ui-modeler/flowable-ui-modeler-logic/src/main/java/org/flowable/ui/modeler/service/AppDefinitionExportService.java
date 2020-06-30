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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.flowable.ui.common.service.exception.InternalServerErrorException;
import org.flowable.ui.modeler.domain.AbstractModel;
import org.flowable.ui.modeler.domain.AppDefinition;
import org.flowable.ui.modeler.domain.AppModelDefinition;
import org.flowable.ui.modeler.domain.Model;
import org.flowable.ui.modeler.model.AppDefinitionRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
@Transactional
public class AppDefinitionExportService extends BaseAppDefinitionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppDefinitionExportService.class);

    protected BpmnJsonConverter bpmnJsonConverter = new BpmnJsonConverter();

    public void exportAppDefinition(HttpServletResponse response, String modelId) throws IOException {

        if (modelId == null) {
            throw new BadRequestException("No application definition id provided");
        }

        Model appModel = modelService.getModel(modelId);
        AppDefinitionRepresentation appRepresentation = createAppDefinitionRepresentation(appModel);

        createAppDefinitionZip(response, appModel, appRepresentation);
    }

    public void exportDeployableAppDefinition(HttpServletResponse response, String modelId) throws IOException {

        if (modelId == null) {
            throw new BadRequestException("No application definition id provided");
        }

        Model appModel = modelService.getModel(modelId);
        AppDefinitionRepresentation appRepresentation = createAppDefinitionRepresentation(appModel);

        createAppDefinitionBar(response, appModel, appRepresentation);
    }

    protected void createAppDefinitionZip(HttpServletResponse response, Model appModel, AppDefinitionRepresentation appDefinition) {
        try {
            response.setHeader("Content-Disposition", "attachment; filename=\"" + appDefinition.getName() + ".zip\"; filename*=utf-8''" + UriUtils.encode(appDefinition.getName() + ".zip", "utf-8"));

            ServletOutputStream servletOutputStream = response.getOutputStream();
            response.setContentType("application/zip");

            ZipOutputStream zipOutputStream = new ZipOutputStream(servletOutputStream);

            createZipEntry(zipOutputStream, appModel.getName() + ".json", createModelEntryJson(appModel));

            ConverterContext converterContext = new ConverterContext(modelService, objectMapper);

            List<AppModelDefinition> modelDefinitions = appDefinition.getDefinition().getModels();
            if (CollectionUtils.isNotEmpty(modelDefinitions)) {
                createBpmnZipEntries(modelDefinitions, zipOutputStream, converterContext);
            }

            List<AppModelDefinition> cmmnModelDefinitions = appDefinition.getDefinition().getCmmnModels();
            if (CollectionUtils.isNotEmpty(cmmnModelDefinitions)) {
                createCmmnZipEntries(cmmnModelDefinitions, zipOutputStream, converterContext);
            }

            Collection<Model> allProcessModels = converterContext.getAllProcessModels();
            if (allProcessModels != null) {
                createBpmnZipEntries(allProcessModels, zipOutputStream, converterContext);
            }

            Collection<Model> allCaseModels = converterContext.getAllCaseModels();
            if (allCaseModels != null) {
                createCmmnZipEntries(allCaseModels, zipOutputStream, converterContext);
            }

            Collection<Model> allFormModels = converterContext.getAllFormModels();
            if (allFormModels != null) {
                for (Model formModel : allFormModels) {
                    createZipEntries(formModel, "form-models", zipOutputStream);
                }
            }

            Collection<Model> allDecisionTableModels = converterContext.getAllDecisionTableModels();
            Collection<Model> allDecisionServiceModels = converterContext.getAllDecisionServiceModels();

            Map<String, String> decisionTableEditorJSONs = new HashMap<>();
            if (!allDecisionTableModels.isEmpty()) {
                decisionTableEditorJSONs = allDecisionTableModels.stream()
                    .collect(Collectors.toMap(
                        AbstractModel::getKey,
                        AbstractModel::getModelEditorJson
                    ));
            }

            createDecisionTableZipEntries(allDecisionTableModels, decisionTableEditorJSONs, zipOutputStream);
            createDecisionServiceZipEntries(allDecisionServiceModels, decisionTableEditorJSONs, zipOutputStream);

            zipOutputStream.close();

            // Flush and close stream
            servletOutputStream.flush();
            servletOutputStream.close();

        } catch (Exception e) {
            LOGGER.error("Could not generate app definition zip archive", e);
            throw new InternalServerErrorException("Could not generate app definition zip archive");
        }
    }

    protected void createDecisionTableZipEntries(Collection<Model> decisionTableModels, Map<String, String> decisionTableEditorJSONs, ZipOutputStream zipOutputStream) throws Exception {
        for (Model decisionTableModel : decisionTableModels) {
            createZipEntries(decisionTableModel, "decision-table-models", zipOutputStream);
            try {
                JsonNode decisionTableNode = objectMapper.readTree(decisionTableModel.getModelEditorJson());
                DmnDefinition dmnDefinition = dmnJsonConverter.convertToDmn(decisionTableNode, decisionTableModel.getId(), decisionTableEditorJSONs);
                byte[] dmnXMLBytes = dmnXMLConverter.convertToXML(dmnDefinition);
                createZipEntry(zipOutputStream, "decision-table-models/" + decisionTableModel.getKey() + ".dmn", dmnXMLBytes);
            } catch (Exception e) {
                throw new InternalServerErrorException(String.format("Error converting decision table %s to XML", decisionTableModel.getName()));
            }
        }
    }

    protected void createDecisionServiceZipEntries(Collection<Model> decisionServiceModels, Map<String, String> decisionTableEditorJSONs, ZipOutputStream zipOutputStream) throws Exception {
        for (Model decisionServiceModel : decisionServiceModels) {
            createZipEntries(decisionServiceModel, "decision-service-models", zipOutputStream);
            try {
                JsonNode decisionServiceNode = objectMapper.readTree(decisionServiceModel.getModelEditorJson());
                DmnDefinition dmnDefinition = dmnJsonConverter.convertToDmn(decisionServiceNode, decisionServiceModel.getId(), decisionTableEditorJSONs);
                byte[] dmnXMLBytes = dmnXMLConverter.convertToXML(dmnDefinition);
                createZipEntry(zipOutputStream, "decision-service-models/" + decisionServiceModel.getKey() + ".dmn", dmnXMLBytes);
            } catch (Exception e) {
                throw new InternalServerErrorException(String.format("Error converting decision service %s to XML", decisionServiceModel.getName()));
            }
        }
    }

    public void createAppDefinitionBar(HttpServletResponse response, Model appModel, AppDefinitionRepresentation appDefinition) {

        try {
            response.setHeader("Content-Disposition", "attachment; filename=\"" + appDefinition.getName() + ".bar\"; filename*=utf-8''" + UriUtils.encode(appDefinition.getName() + ".bar", "utf-8"));

            byte[] deployZipArtifact = createDeployableZipArtifact(appModel, appDefinition.getDefinition());

            ServletOutputStream servletOutputStream = response.getOutputStream();
            response.setContentType("application/zip");
            servletOutputStream.write(deployZipArtifact);

            // Flush and close stream
            servletOutputStream.flush();
            servletOutputStream.close();

        } catch (Exception e) {
            LOGGER.error("Could not generate app definition bar archive", e);
            throw new InternalServerErrorException("Could not generate app definition bar archive");
        }
    }

    protected void createBpmnZipEntries(List<AppModelDefinition> modelDefinitions, ZipOutputStream zipOutputStream,
            ConverterContext converterContext) throws Exception {

        for (AppModelDefinition modelDef : modelDefinitions) {
            Model model = modelService.getModel(modelDef.getId());

            List<Model> referencedModels = modelRepository.findByParentModelId(model.getId());
            for (Model childModel : referencedModels) {
                if (Model.MODEL_TYPE_FORM == childModel.getModelType()) {
                    converterContext.addFormModel(childModel);

                } else if (Model.MODEL_TYPE_DECISION_TABLE == childModel.getModelType()) {
                    converterContext.addDecisionTableModel(childModel);
                } else if (Model.MODEL_TYPE_DECISION_SERVICE == childModel.getModelType()) {
                    converterContext.addDecisionServiceModel(childModel);
                    List<Model> referencedDecisionTableModels = modelRepository.findByParentModelId(childModel.getId());
                    referencedDecisionTableModels.stream()
                        .filter(refModel -> Model.MODEL_TYPE_DECISION_TABLE == refModel.getModelType())
                        .forEach(converterContext::addDecisionTableModel);
                }
            }

            BpmnModel bpmnModel = modelService.getBpmnModel(model, converterContext);
            Map<String, StartEvent> noneStartEventMap = new HashMap<>();
            postProcessFlowElements(new ArrayList<>(), noneStartEventMap, bpmnModel);

            for (Process process : bpmnModel.getProcesses()) {
                processUserTasks(process.getFlowElements(), process, noneStartEventMap);
            }

            byte[] modelXML = modelService.getBpmnXML(bpmnModel);

            // add BPMN XML model
            createZipEntry(zipOutputStream, "bpmn-models/" + model.getKey().replaceAll(" ", "") + ".bpmn", modelXML);

            // add JSON model
            createZipEntries(model, "bpmn-models", zipOutputStream);
        }
    }

    protected void createBpmnZipEntries(Collection<Model> models, ZipOutputStream zipOutputStream,
            ConverterContext converterContext) throws Exception {

        for (Model model : models) {

            BpmnModel bpmnModel = modelService.getBpmnModel(model, converterContext);
            Map<String, StartEvent> noneStartEventMap = new HashMap<>();
            postProcessFlowElements(new ArrayList<>(), noneStartEventMap, bpmnModel);

            for (Process process : bpmnModel.getProcesses()) {
                processUserTasks(process.getFlowElements(), process, noneStartEventMap);
            }

            byte[] modelXML = modelService.getBpmnXML(bpmnModel);

            // add BPMN XML model
            createZipEntry(zipOutputStream, "bpmn-models/" + model.getKey().replaceAll(" ", "") + ".bpmn", modelXML);

            // add JSON model
            createZipEntries(model, "bpmn-models", zipOutputStream);
        }
    }

    protected void createCmmnZipEntries(List<AppModelDefinition> modelDefinitions, ZipOutputStream zipOutputStream, ConverterContext converterContext) throws Exception {

        for (AppModelDefinition modelDef : modelDefinitions) {
            Model model = modelService.getModel(modelDef.getId());

            List<Model> referencedModels = modelRepository.findByParentModelId(model.getId());
            for (Model childModel : referencedModels) {
                if (Model.MODEL_TYPE_FORM == childModel.getModelType()) {
                    converterContext.addFormModel(childModel);

                } else if (Model.MODEL_TYPE_DECISION_TABLE == childModel.getModelType()) {
                    converterContext.addDecisionTableModel(childModel);

                } else if (Model.MODEL_TYPE_DECISION_SERVICE == childModel.getModelType()) {
                    converterContext.addDecisionServiceModel(childModel);

                } else if (Model.MODEL_TYPE_BPMN == childModel.getModelType()) {
                    converterContext.addProcessModel(childModel);

                } else if (Model.MODEL_TYPE_CMMN == childModel.getModelType()) {
                    converterContext.addCaseModel(childModel);

                }
            }

            CmmnModel cmmnModel = modelService.getCmmnModel(model, converterContext);
            byte[] modelXML = modelService.getCmmnXML(cmmnModel);

            // add CMMN XML model
            createZipEntry(zipOutputStream, "cmmn-models/" + model.getKey().replaceAll(" ", "") + ".cmmn", modelXML);

            // add JSON model
            createZipEntries(model, "cmmn-models", zipOutputStream);
        }
    }

    protected void createCmmnZipEntries(Collection<Model> models, ZipOutputStream zipOutputStream,
            ConverterContext converterContext) throws Exception {

        for (Model model : models) {
            CmmnModel cmmnModel = modelService.getCmmnModel(model, converterContext);

            byte[] modelXML = modelService.getCmmnXML(cmmnModel);

            // add CMMN XML model
            createZipEntry(zipOutputStream, "cmmn-models/" + model.getKey().replaceAll(" ", "") + ".cmmn", modelXML);

            // add JSON model
            createZipEntries(model, "cmmn-models", zipOutputStream);
        }

    }

    protected void createZipEntries(Model model, String directoryName, ZipOutputStream zipOutputStream) throws Exception {
        createZipEntry(zipOutputStream, directoryName + "/" + model.getKey() + ".json", createModelEntryJson(model));

        if (model.getThumbnail() != null) {
            createZipEntry(zipOutputStream, directoryName + "/" + model.getKey() + ".png", model.getThumbnail());
        }
    }

    protected String createModelEntryJson(Model model) {
        ObjectNode modelJson = objectMapper.createObjectNode();

        modelJson.put("id", model.getId());
        modelJson.put("name", model.getName());
        modelJson.put("key", model.getKey());
        modelJson.put("description", model.getDescription());

        try {
            modelJson.set("editorJson", objectMapper.readTree(model.getModelEditorJson()));
        } catch (Exception e) {
            LOGGER.error("Error exporting model json for id {}", model.getId(), e);
            throw new InternalServerErrorException("Error exporting model json for id " + model.getId());
        }

        return modelJson.toString();
    }

    protected AppDefinitionRepresentation createAppDefinitionRepresentation(AbstractModel model) {
        AppDefinition appDefinition = null;
        try {
            appDefinition = objectMapper.readValue(model.getModelEditorJson(), AppDefinition.class);
        } catch (Exception e) {
            LOGGER.error("Error deserializing app {}", model.getId(), e);
            throw new InternalServerErrorException("Could not deserialize app definition");
        }
        AppDefinitionRepresentation result = new AppDefinitionRepresentation(model);
        result.setDefinition(appDefinition);
        return result;
    }

    protected void createZipEntry(ZipOutputStream zipOutputStream, String filename, String content) throws Exception {
        createZipEntry(zipOutputStream, filename, content.getBytes(StandardCharsets.UTF_8));
    }

    protected void createZipEntry(ZipOutputStream zipOutputStream, String filename, byte[] content) throws Exception {
        ZipEntry entry = new ZipEntry(filename);
        zipOutputStream.putNextEntry(entry);
        zipOutputStream.write(content);
        zipOutputStream.closeEntry();
    }

}
