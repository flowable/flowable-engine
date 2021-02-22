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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.cmmn.editor.json.converter.CmmnJsonConverter;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.dmn.editor.converter.DmnJsonConverter;
import org.flowable.dmn.editor.converter.DmnJsonConverterUtil;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.ui.common.security.SecurityUtils;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.flowable.ui.common.service.exception.InternalServerErrorException;
import org.flowable.ui.modeler.domain.AbstractModel;
import org.flowable.ui.modeler.domain.AppDefinition;
import org.flowable.ui.modeler.domain.AppModelDefinition;
import org.flowable.ui.modeler.domain.Model;
import org.flowable.ui.modeler.model.AppDefinitionPublishRepresentation;
import org.flowable.ui.modeler.model.AppDefinitionRepresentation;
import org.flowable.ui.modeler.model.AppDefinitionUpdateResultRepresentation;
import org.flowable.ui.modeler.repository.ModelRepository;
import org.flowable.ui.modeler.serviceapi.ModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
@Transactional
public class AppDefinitionImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppDefinitionImportService.class);

    @Autowired
    protected AppDefinitionPublishService appDefinitionPublishService;

    @Autowired
    protected ModelService modelService;

    @Autowired
    protected ModelRepository modelRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    protected BpmnJsonConverter bpmnJsonConverter = new BpmnJsonConverter();
    protected CmmnJsonConverter cmmnJsonConverter = new CmmnJsonConverter();
    protected DmnJsonConverter dmnJsonConverter = new DmnJsonConverter();

    public AppDefinitionRepresentation importAppDefinition(HttpServletRequest request, MultipartFile file) {
        try {
            InputStream is = file.getInputStream();
            String fileName = file.getOriginalFilename();

            ConverterContext converterContext = new ConverterContext(modelService, objectMapper);
            return importAppDefinition(is, fileName, null, converterContext);

        } catch (IOException e) {
            throw new InternalServerErrorException("Error loading file", e);
        }
    }

    public AppDefinitionRepresentation importAppDefinitionNewVersion(HttpServletRequest request, MultipartFile file, String appDefId) {
        try {
            InputStream is = file.getInputStream();
            String fileName = file.getOriginalFilename();
            Model appModel = modelService.getModel(appDefId);
            if (!appModel.getModelType().equals(Model.MODEL_TYPE_APP)) {
                throw new BadRequestException("No app definition found for id " + appDefId);
            }

            AppDefinitionRepresentation appDefinition = createAppDefinitionRepresentation(appModel);

            ConverterContext converterContext = new ConverterContext(modelService, objectMapper);
            if (appDefinition.getDefinition() != null && CollectionUtils.isNotEmpty(appDefinition.getDefinition().getModels())) {
                for (AppModelDefinition modelDef : appDefinition.getDefinition().getModels()) {
                    Model processModel = modelService.getModel(modelDef.getId());

                    List<Model> referencedModels = modelRepository.findByParentModelId(processModel.getId());
                    for (Model childModel : referencedModels) {
                        if (Model.MODEL_TYPE_FORM == childModel.getModelType()) {
                            converterContext.addFormModel(childModel);
                        } else if (Model.MODEL_TYPE_DECISION_TABLE == childModel.getModelType()) {
                            converterContext.addDecisionTableModel(childModel);
                        } else if (Model.MODEL_TYPE_DECISION_SERVICE == childModel.getModelType()) {
                            converterContext.addDecisionServiceModel(childModel);

                            List<Model> referencedDecisionTableChildModels = modelRepository.findByParentModelId(childModel.getId());
                            for (Model decisionTableChildModel : referencedDecisionTableChildModels) {
                                converterContext.addDecisionTableModel(decisionTableChildModel);
                            }
                        }
                    }

                    converterContext.addProcessModel(processModel);
                }
            }
            
            if (appDefinition.getDefinition() != null && CollectionUtils.isNotEmpty(appDefinition.getDefinition().getCmmnModels())) {
                for (AppModelDefinition modelDef : appDefinition.getDefinition().getCmmnModels()) {
                    Model caseModel = modelService.getModel(modelDef.getId());

                    List<Model> referencedModels = modelRepository.findByParentModelId(caseModel.getId());
                    for (Model childModel : referencedModels) {
                        if (Model.MODEL_TYPE_FORM == childModel.getModelType()) {
                            converterContext.addFormModel(childModel);
                        } else if (Model.MODEL_TYPE_DECISION_TABLE == childModel.getModelType()) {
                            converterContext.addDecisionTableModel(childModel);
                        } else if (Model.MODEL_TYPE_DECISION_SERVICE == childModel.getModelType()) {
                            converterContext.addDecisionServiceModel(childModel);

                            List<Model> referencedDecisionTableChildModels = modelRepository.findByParentModelId(childModel.getId());
                            for (Model decisionTableChildModel : referencedDecisionTableChildModels) {
                                converterContext.addDecisionTableModel(decisionTableChildModel);
                            }
                        }
                    }

                    converterContext.addCaseModel(caseModel);
                }
            }

            return importAppDefinition(is, fileName, appModel, converterContext);

        } catch (IOException e) {
            throw new InternalServerErrorException("Error loading file", e);
        }
    }

    protected AppDefinitionRepresentation importAppDefinition(InputStream is, String fileName,
            Model existingAppModel, ConverterContext converterContext) {

        if (fileName != null && (fileName.endsWith(".zip"))) {
            Model appDefinitionModel = readZipFile(is, converterContext);
            if (StringUtils.isNotEmpty(appDefinitionModel.getKey()) && StringUtils.isNotEmpty(appDefinitionModel.getModelEditorJson())) {

                importForms(converterContext);
                importDecisionTables(converterContext);
                importDecisionServices(converterContext);
                importBpmnModels(converterContext);
                importCmmnModels(converterContext);

                return importAppDefinitionModel(appDefinitionModel, existingAppModel, converterContext);

            } else {
                throw new BadRequestException("Could not find app definition json");
            }

        } else {
            throw new BadRequestException("Invalid file name, only .zip files are supported not " + fileName);
        }
    }

    public AppDefinitionUpdateResultRepresentation publishAppDefinition(String modelId, AppDefinitionPublishRepresentation publishModel) {

        String currentUserId = SecurityUtils.getCurrentUserId();
        Model appModel = modelService.getModel(modelId);

        // Create pojo representation of the model and the json
        AppDefinitionRepresentation appDefinitionRepresentation = createAppDefinitionRepresentation(appModel);
        AppDefinitionUpdateResultRepresentation result = new AppDefinitionUpdateResultRepresentation();

        // Actual publication
        appDefinitionPublishService.publishAppDefinition(publishModel.getComment(), appModel, currentUserId);

        result.setAppDefinition(appDefinitionRepresentation);
        return result;

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

    protected Model readZipFile(InputStream inputStream, ConverterContext converterContext) {

        Model appDefinitionModel = null;
        ZipInputStream zipInputStream = null;
        try {
            zipInputStream = new ZipInputStream(inputStream);
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                String zipEntryName = zipEntry.getName();
                if (zipEntryName.endsWith("json") || zipEntryName.endsWith("png")) {

                    String modelFileName = null;
                    if (zipEntryName.contains("/")) {
                        modelFileName = zipEntryName.substring(zipEntryName.indexOf('/') + 1);
                    } else {
                        modelFileName = zipEntryName;
                    }

                    if (modelFileName.endsWith(".png")) {
                        converterContext.getModelKeyToThumbnailMap().put(modelFileName.replace(".png", ""), IOUtils.toByteArray(zipInputStream));

                    } else {
                        modelFileName = modelFileName.replace(".json", "");
                        String json = IOUtils.toString(zipInputStream, StandardCharsets.UTF_8);

                        if (zipEntryName.startsWith("bpmn-models/")) {
                            converterContext.getProcessKeyToJsonStringMap().put(modelFileName, json);
                            
                        } else if (zipEntryName.startsWith("cmmn-models/")) {
                            converterContext.getCaseKeyToJsonStringMap().put(modelFileName, json);

                        } else if (zipEntryName.startsWith("form-models/")) {
                            converterContext.getFormKeyToJsonStringMap().put(modelFileName, json);

                        } else if (zipEntryName.startsWith("decision-table-models/")) {
                            converterContext.getDecisionTableKeyToJsonStringMap().put(modelFileName, json);

                        } else if (zipEntryName.startsWith("decision-service-models/")) {
                            converterContext.getDecisionServiceKeyToJsonStringMap().put(modelFileName, json);

                        } else if (!zipEntryName.contains("/")) {
                            appDefinitionModel = createModelObject(json, Model.MODEL_TYPE_APP);
                        }
                    }
                }

                zipEntry = zipInputStream.getNextEntry();
            }
        } catch (Exception e) {
            LOGGER.error("Error reading app definition zip file", e);
            throw new InternalServerErrorException("Error reading app definition zip file");

        } finally {
            if (zipInputStream != null) {
                try {
                    zipInputStream.closeEntry();
                } catch (Exception e) {
                }
                
                try {
                    zipInputStream.close();
                } catch (Exception e) {
                }
            }
        }

        return appDefinitionModel;
    }

    protected void importForms(ConverterContext converterContext) {

        Map<String, String> formMap = converterContext.getFormKeyToJsonStringMap();
        Map<String, byte[]> thumbnailMap = converterContext.getModelKeyToThumbnailMap();

        for (String formKey : formMap.keySet()) {

            Model formModel = createModelObject(formMap.get(formKey), Model.MODEL_TYPE_FORM);

            Model existingModel = converterContext.getFormModelByKey(formModel.getKey());
            Model updatedFormModel = null;
            if (existingModel != null) {
                byte[] imageBytes = null;
                if (thumbnailMap.containsKey(formKey)) {
                    imageBytes = thumbnailMap.get(formKey);
                }
                updatedFormModel = modelService.saveModel(existingModel, formModel.getModelEditorJson(), imageBytes,
                        true, "App definition import", SecurityUtils.getCurrentUserId());

            } else {
                formModel.setId(null);
                updatedFormModel = modelService.createModel(formModel, SecurityUtils.getCurrentUserId());

                if (thumbnailMap.containsKey(formKey)) {
                    updatedFormModel.setThumbnail(thumbnailMap.get(formKey));
                    modelRepository.save(updatedFormModel);
                }
            }

            converterContext.addFormModel(updatedFormModel, formModel.getId());
        }
    }

    protected void importDecisionTables(ConverterContext converterContext) {

        Map<String, String> decisionTableMap = converterContext.getDecisionTableKeyToJsonStringMap();
        Map<String, byte[]> thumbnailMap = converterContext.getModelKeyToThumbnailMap();

        String currentUserId = SecurityUtils.getCurrentUserId();

        for (String decisionTableKey : decisionTableMap.keySet()) {

            Model decisionTableModel = createModelObject(decisionTableMap.get(decisionTableKey), Model.MODEL_TYPE_DECISION_TABLE);

            // migrate to new version
            DecisionTableModelConversionUtil.convertModelToV3(decisionTableModel);

            String oldDecisionTableId = decisionTableModel.getId();

            Model existingModel = converterContext.getDecisionTableModelByKey(decisionTableModel.getKey());
            Model updatedDecisionTableModel = null;
            if (existingModel != null) {
                byte[] imageBytes = null;
                if (thumbnailMap.containsKey(decisionTableKey)) {
                    imageBytes = thumbnailMap.get(decisionTableKey);
                }
                updatedDecisionTableModel = modelService.saveModel(existingModel, decisionTableModel.getModelEditorJson(), imageBytes,
                        true, "App definition import", currentUserId);

            } else {
                decisionTableModel.setId(null);
                updatedDecisionTableModel = modelService.createModel(decisionTableModel, currentUserId);

                if (thumbnailMap.containsKey(decisionTableKey)) {
                    updatedDecisionTableModel.setThumbnail(thumbnailMap.get(decisionTableKey));
                    modelRepository.save(updatedDecisionTableModel);
                }
            }

            converterContext.addDecisionTableModel(updatedDecisionTableModel, oldDecisionTableId);
        }
    }

    protected void importDecisionServices(ConverterContext converterContext) {

        Map<String, String> decisionServicesMap = converterContext.getDecisionServiceKeyToJsonStringMap();
        Map<String, byte[]> thumbnailMap = converterContext.getModelKeyToThumbnailMap();

        String currentUserId = SecurityUtils.getCurrentUserId();
        for (String decisionServiceKey : decisionServicesMap.keySet()) {
            Model decisionServiceModel = createModelObject(decisionServicesMap.get(decisionServiceKey), Model.MODEL_TYPE_DECISION_SERVICE);

            String oldDecisionServiceId = decisionServiceModel.getId();
            ObjectNode decisionServiceModelNode;
            try {
                decisionServiceModelNode = (ObjectNode) objectMapper.readTree(decisionServiceModel.getModelEditorJson());
            } catch (Exception e) {
                LOGGER.error("Error reading decision service json for {}", decisionServiceKey, e);
                throw new InternalServerErrorException("Error reading decision service json for " + decisionServiceKey);
            }

            // remove modelId from import json
            // and update decision table references
            decisionServiceModelNode.remove("modelId");
            DmnJsonConverterUtil.updateDecisionTableModelReferences(decisionServiceModelNode, converterContext);
            String updatedDecisionServiceJson = decisionServiceModelNode.toString();

            Model existingModel = converterContext.getDecisionServiceModelByKey(decisionServiceModel.getKey());
            Model updatedDecisionServiceModel;
            if (existingModel != null) {
                byte[] imageBytes = null;
                if (thumbnailMap.containsKey(decisionServiceKey)) {
                    imageBytes = thumbnailMap.get(decisionServiceKey);
                }
                existingModel.setModelEditorJson(updatedDecisionServiceJson);
                updatedDecisionServiceModel = modelService.saveModel(existingModel, existingModel.getModelEditorJson(), imageBytes,
                    true, "App definition import", currentUserId);

            } else {
                decisionServiceModel.setId(null);
                decisionServiceModel.setModelEditorJson(updatedDecisionServiceJson);

                updatedDecisionServiceModel = modelService.createModel(decisionServiceModel, currentUserId);

                if (thumbnailMap.containsKey(decisionServiceKey)) {
                    updatedDecisionServiceModel.setThumbnail(thumbnailMap.get(decisionServiceKey));
                    modelRepository.save(updatedDecisionServiceModel);
                }
            }

            converterContext.addDecisionServiceModel(updatedDecisionServiceModel, oldDecisionServiceId);
        }
    }

    protected void importBpmnModels(ConverterContext converterContext) {

        Map<String, String> bpmnModelMap = converterContext.getProcessKeyToJsonStringMap();
        Map<String, byte[]> thumbnailMap = converterContext.getModelKeyToThumbnailMap();

        String currentUserId = SecurityUtils.getCurrentUserId();

        for (String bpmnModelKey : bpmnModelMap.keySet()) {

            String bpmnModelJson = bpmnModelMap.get(bpmnModelKey);
            Model bpmnModelObject = createModelObject(bpmnModelJson, Model.MODEL_TYPE_BPMN);
            String oldBpmnModelId = bpmnModelObject.getId();

            JsonNode bpmnModelNode = null;
            try {
                bpmnModelNode = objectMapper.readTree(bpmnModelObject.getModelEditorJson());
            } catch (Exception e) {
                LOGGER.error("Error reading BPMN json for {}", bpmnModelKey, e);
                throw new InternalServerErrorException("Error reading BPMN json for " + bpmnModelKey);
            }

            BpmnModel bpmnModel = bpmnJsonConverter.convertToBpmnModel(bpmnModelNode, converterContext);
            String updatedBpmnJson = bpmnJsonConverter.convertToJson(bpmnModel, converterContext).toString();

            Model existingModel = converterContext.getProcessModelByKey(bpmnModelKey);
            Model updatedProcessModel = null;
            if (existingModel != null) {
                byte[] imageBytes = null;
                if (thumbnailMap.containsKey(bpmnModelKey)) {
                    imageBytes = thumbnailMap.get(bpmnModelKey);
                }

                existingModel.setModelEditorJson(updatedBpmnJson);

                updatedProcessModel = modelService.saveModel(existingModel, existingModel.getModelEditorJson(), imageBytes, true, "App definition import",
                        currentUserId);

            } else {
                bpmnModelObject.setId(null);
                bpmnModelObject.setModelEditorJson(updatedBpmnJson);
                updatedProcessModel = modelService.createModel(bpmnModelObject, currentUserId);

                if (thumbnailMap.containsKey(bpmnModelKey)) {
                    updatedProcessModel.setThumbnail(thumbnailMap.get(bpmnModelKey));
                    modelService.saveModel(updatedProcessModel);
                }
            }

            converterContext.addProcessModel(updatedProcessModel, oldBpmnModelId);
        }
    }
    
    protected void importCmmnModels(ConverterContext converterContext) {

        Map<String, String> cmmnModelMap = converterContext.getCaseKeyToJsonStringMap();
        Map<String, byte[]> thumbnailMap = converterContext.getModelKeyToThumbnailMap();

        String currentUserId = SecurityUtils.getCurrentUserId();

        for (String cmmnModelKey : cmmnModelMap.keySet()) {

            String cmmnModelJson = cmmnModelMap.get(cmmnModelKey);
            Model cmmnModelObject = createModelObject(cmmnModelJson, Model.MODEL_TYPE_CMMN);
            String oldCmmnModelId = cmmnModelObject.getId();

            JsonNode cmmnModelNode = null;
            try {
                cmmnModelNode = objectMapper.readTree(cmmnModelObject.getModelEditorJson());
            } catch (Exception e) {
                LOGGER.error("Error reading CMMN json for {}", cmmnModelKey, e);
                throw new InternalServerErrorException("Error reading CMMN json for " + cmmnModelKey);
            }

            CmmnModel cmmnModel = cmmnJsonConverter.convertToCmmnModel(cmmnModelNode, converterContext);
            String updatedCmmnJson = cmmnJsonConverter.convertToJson(cmmnModel, converterContext).toString();

            Model existingModel = converterContext.getCaseModelByKey(cmmnModelKey);
            Model updatedCaseModel = null;
            if (existingModel != null) {
                byte[] imageBytes = null;
                if (thumbnailMap.containsKey(cmmnModelKey)) {
                    imageBytes = thumbnailMap.get(cmmnModelKey);
                }

                existingModel.setModelEditorJson(updatedCmmnJson);

                updatedCaseModel = modelService.saveModel(existingModel, existingModel.getModelEditorJson(), imageBytes, true, "App definition import",
                        currentUserId);

            } else {
                cmmnModelObject.setId(null);
                cmmnModelObject.setModelEditorJson(updatedCmmnJson);
                updatedCaseModel = modelService.createModel(cmmnModelObject, currentUserId);

                if (thumbnailMap.containsKey(cmmnModelKey)) {
                    updatedCaseModel.setThumbnail(thumbnailMap.get(cmmnModelKey));
                    modelService.saveModel(updatedCaseModel);
                }
            }

            converterContext.addCaseModel(updatedCaseModel, oldCmmnModelId);
        }
    }

    protected AppDefinitionRepresentation importAppDefinitionModel(Model appDefinitionModel,
            Model existingAppModel, ConverterContext converterContext) {

        AppDefinition appDefinition = null;
        try {
            appDefinition = objectMapper.readValue(appDefinitionModel.getModelEditorJson(), AppDefinition.class);
        } catch (Exception e) {
            LOGGER.error("Error reading app definition {}", appDefinitionModel.getName(), e);
            throw new BadRequestException("Error reading app definition", e);
        }

        if (appDefinition.getModels() != null) {
            for (AppModelDefinition modelDef : appDefinition.getModels()) {
                Model newModel = converterContext.getProcessModelById(modelDef.getId());
                if (newModel != null) {
                    modelDef.setId(newModel.getId());
                    modelDef.setName(newModel.getName());
                    modelDef.setCreatedBy(newModel.getCreatedBy());
                    modelDef.setLastUpdatedBy(newModel.getLastUpdatedBy());
                    modelDef.setLastUpdated(newModel.getLastUpdated());
                    modelDef.setVersion(newModel.getVersion());
                }
            }
        }
        
        if (appDefinition.getCmmnModels() != null) {
            for (AppModelDefinition modelDef : appDefinition.getCmmnModels()) {
                Model newModel = converterContext.getCaseModelById(modelDef.getId());
                if (newModel != null) {
                    modelDef.setId(newModel.getId());
                    modelDef.setName(newModel.getName());
                    modelDef.setCreatedBy(newModel.getCreatedBy());
                    modelDef.setLastUpdatedBy(newModel.getLastUpdatedBy());
                    modelDef.setLastUpdated(newModel.getLastUpdated());
                    modelDef.setVersion(newModel.getVersion());
                }
            }
        }

        String currentUserId = SecurityUtils.getCurrentUserId();

        try {
            String updatedAppDefinitionJson = objectMapper.writeValueAsString(appDefinition);

            if (existingAppModel != null) {
                appDefinitionModel = modelService.saveModel(existingAppModel, updatedAppDefinitionJson, null, true, "App definition import",
                        currentUserId);
            } else {
                appDefinitionModel.setId(null);
                appDefinitionModel.setModelEditorJson(updatedAppDefinitionJson);
                appDefinitionModel = modelService.createModel(appDefinitionModel, currentUserId);
            }

            AppDefinitionRepresentation result = new AppDefinitionRepresentation(appDefinitionModel);
            result.setDefinition(appDefinition);
            return result;

        } catch (Exception e) {
            LOGGER.error("Error storing app definition", e);
            throw new InternalServerErrorException("Error storing app definition");
        }
    }

    protected Model createModelObject(String modelJson, int modelType) {
        try {
            JsonNode modelNode = objectMapper.readTree(modelJson);
            Model model = new Model();
            model.setId(modelNode.get("id").asText());
            model.setName(modelNode.get("name").asText());
            model.setKey(modelNode.get("key").asText());

            JsonNode descriptionNode = modelNode.get("description");
            if (descriptionNode != null && !descriptionNode.isNull()) {
                model.setDescription(descriptionNode.asText());
            }

            model.setModelEditorJson(modelNode.get("editorJson").toString());
            model.setModelType(modelType);

            return model;

        } catch (Exception e) {
            LOGGER.error("Error reading model json", e);
            throw new InternalServerErrorException("Error reading model json");
        }
    }
}
