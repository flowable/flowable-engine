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

import static au.com.rds.activiti.RDSActivitiConstants.ARROW_FORM_KEY;
import static au.com.rds.activiti.RDSActivitiConstants.FORMKEY;
import static au.com.rds.activiti.RDSActivitiConstants.FORM;
import static au.com.rds.activiti.RDSActivitiConstants.BPMN_RDS_NAMESPACE;
import static au.com.rds.activiti.RDSActivitiConstants.BPMN_RDS_NAMESPACE_PREFIX;
import static au.com.rds.activiti.RDSActivitiConstants.PROCESS_FORMKEY;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.activiti.editor.language.json.converter.RDSBpmnJsonConverter;
import org.apache.commons.lang3.StringUtils;
import org.flowable.app.domain.editor.AbstractModel;
import org.flowable.app.domain.editor.AppDefinition;
import org.flowable.app.domain.editor.AppModelDefinition;
import org.flowable.app.domain.editor.Model;
import org.flowable.app.domain.editor.ModelHistory;
import org.flowable.app.domain.editor.ModelRelation;
import org.flowable.app.domain.editor.ModelRelationTypes;
import org.flowable.app.model.editor.ModelKeyRepresentation;
import org.flowable.app.model.editor.ModelRepresentation;
import org.flowable.app.model.editor.ReviveModelResultRepresentation;
import org.flowable.app.model.editor.ReviveModelResultRepresentation.UnresolveModelRepresentation;
import org.flowable.app.model.editor.decisiontable.DecisionTableDefinitionRepresentation;
import org.flowable.app.repository.editor.ModelHistoryRepository;
import org.flowable.app.repository.editor.ModelRelationRepository;
import org.flowable.app.repository.editor.ModelRepository;
import org.flowable.app.repository.editor.ModelSort;
import org.flowable.app.security.SecurityUtils;
import org.flowable.app.service.api.ModelService;
import org.flowable.app.service.exception.BadRequestException;
import org.flowable.app.service.exception.InternalServerErrorException;
import org.flowable.app.service.exception.NotFoundException;
import org.flowable.app.util.XmlUtil;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionAttribute;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.xml.converter.DmnXMLConverter;
import org.flowable.editor.dmn.converter.DmnJsonConverter;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.editor.language.json.converter.util.JsonConverterUtil;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.form.model.FormModel;
import org.flowable.idm.api.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import au.com.rds.schemaformbuilder.formdesignjson.FormDesignJsonService;

@Service
@Transactional
public class ModelServiceImpl implements ModelService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelServiceImpl.class);

    public static final String NAMESPACE = "http://flowable.org/modeler";

    protected static final String PROCESS_NOT_FOUND_MESSAGE_KEY = "PROCESS.ERROR.NOT-FOUND";
    
    public static String RDS_FORM_EMPTY_DEFINITION = "{\"sfFormProperties\": {\"items\": [],\"type\": \"root\" },\"sfSchemaProperties\": {}}";

    @Autowired
    protected ModelImageService modelImageService;

    @Autowired
    protected ModelRepository modelRepository;

    @Autowired
    protected ModelHistoryRepository modelHistoryRepository;

    @Autowired
    protected ModelRelationRepository modelRelationRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    FormDesignJsonService formDesignJsonService;

    protected RDSBpmnJsonConverter bpmnJsonConverter = new RDSBpmnJsonConverter();

    protected BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();

    protected DmnJsonConverter dmnJsonConverter = new DmnJsonConverter();
    protected DmnXMLConverter dmnXMLConverter = new DmnXMLConverter();

    @Override
    public Model getModel(String modelId) {
        Model model = modelRepository.get(modelId);

        if (model == null) {
            NotFoundException modelNotFound = new NotFoundException("No model found with the given id: " + modelId);
            modelNotFound.setMessageKey(PROCESS_NOT_FOUND_MESSAGE_KEY);
            throw modelNotFound;
        }

        return model;
    }

    public ModelRepresentation getModelRepresentation(String modelId) {
        Model model = getModel(modelId);
        return new ModelRepresentation(model);
    }

    @Override
    public List<AbstractModel> getModelsByModelType(Integer modelType) {
        return new ArrayList<AbstractModel>(modelRepository.findByModelType(modelType, ModelSort.NAME_ASC));
    }

    @Override
    public ModelHistory getModelHistory(String modelId, String modelHistoryId) {
        // Check if the user has read-rights on the process-model in order to fetch history
        Model model = getModel(modelId);
        ModelHistory modelHistory = modelHistoryRepository.get(modelHistoryId);

        // Check if history corresponds to the current model and is not deleted
        if (modelHistory == null || modelHistory.getRemovalDate() != null || !modelHistory.getModelId().equals(model.getId())) {
            throw new NotFoundException("Process model history not found: " + modelHistoryId);
        }
        return modelHistory;
    }

    @Override
    public byte[] getBpmnXML(AbstractModel model) {
        BpmnModel bpmnModel = getBpmnModel(model);
        return getBpmnXML(bpmnModel);
    }

    public byte[] getBpmnXML(AbstractModel model, boolean resolveFormReference) {
        BpmnModel bpmnModel = getBpmnModel(model, resolveFormReference);
        return getBpmnXML(bpmnModel);
    }

    @Override
    public byte[] getBpmnXML(BpmnModel bpmnModel) {
        for (Process process : bpmnModel.getProcesses()) {
            if (StringUtils.isNotEmpty(process.getId())) {
                char firstCharacter = process.getId().charAt(0);
                // no digit is allowed as first character
                if (Character.isDigit(firstCharacter)) {
                    process.setId("a" + process.getId());
                }
            }
        }
        byte[] xmlBytes = bpmnXMLConverter.convertToXML(bpmnModel);
        return xmlBytes;
    }

    public Map<String,byte[]> getDecisionTableDefinitionsForProcess(String modelId) {
      Map<String,byte[]> dtMaps= new HashMap<String, byte[]>();
      
      List<Model> referencedModels = modelRepository.findByParentModelId(modelId);
      for (Model childModel : referencedModels) {
          if (Model.MODEL_TYPE_DECISION_TABLE == childModel.getModelType()) {
            try {
              JsonNode decisionTableNode = objectMapper.readTree(childModel.getModelEditorJson());
              DmnDefinition dmnDefinition = dmnJsonConverter.convertToDmn(decisionTableNode, childModel.getId(),
                      childModel.getVersion(), childModel.getLastUpdated());
              byte[] dmnXMLBytes = dmnXMLConverter.convertToXML(dmnDefinition);
              dtMaps.put("dmn-" + childModel.getKey() + ".dmn", dmnXMLBytes);
            } catch (Exception e) {
                throw new InternalServerErrorException(String.format("Error converting decision table %s to XML", childModel.getName()));
            }
          }
      } 
      
      return dtMaps;    
    }

    public ModelKeyRepresentation validateModelKey(Model model, Integer modelType, String key) {
        ModelKeyRepresentation modelKeyResponse = new ModelKeyRepresentation();
        modelKeyResponse.setKey(key);

        List<Model> models = modelRepository.findByKeyAndType(key, modelType);
        for (Model modelInfo : models) {
            if (model == null || !modelInfo.getId().equals(model.getId())) {
                modelKeyResponse.setKeyAlreadyExists(true);
                modelKeyResponse.setId(modelInfo.getId());
                modelKeyResponse.setName(modelInfo.getName());
                break;
            }
        }

        return modelKeyResponse;
    }
    
    @Override
    public String createModelJson(ModelRepresentation model) {
        String json = null;
        if (Integer.valueOf(AbstractModel.MODEL_TYPE_FORM).equals(model.getModelType())) {
            try {
                json = objectMapper.writeValueAsString(new FormModel());
            } catch (Exception e) {
                LOGGER.error("Error creating form model", e);
                throw new InternalServerErrorException("Error creating form");
            }

        } else if (Integer.valueOf(AbstractModel.MODEL_TYPE_DECISION_TABLE).equals(model.getModelType())) {
            try {
                DecisionTableDefinitionRepresentation decisionTableDefinition = new DecisionTableDefinitionRepresentation();

                String decisionTableDefinitionKey = model.getName().replaceAll(" ", "");
                decisionTableDefinition.setKey(decisionTableDefinitionKey);

                json = objectMapper.writeValueAsString(decisionTableDefinition);
            } catch (Exception e) {
                LOGGER.error("Error creating decision table model", e);
                throw new InternalServerErrorException("Error creating decision table");
            }

        } else if (Integer.valueOf(AbstractModel.MODEL_TYPE_APP).equals(model.getModelType())) {
            try {
                json = objectMapper.writeValueAsString(new AppDefinition());
            } catch (Exception e) {
                LOGGER.error("Error creating app definition", e);
                throw new InternalServerErrorException("Error creating app definition");
            }

        } else if (Integer.valueOf(AbstractModel.MODEL_TYPE_FORM_RDS).equals(model.getModelType())) {
            try
            {         
              json = RDS_FORM_EMPTY_DEFINITION ;
            }
            catch (Exception e)
            {
              LOGGER.error("Error creating form model", e);
              throw new InternalServerErrorException("Error creating form");
            }
    
        }
        else {
            ObjectNode editorNode = objectMapper.createObjectNode();
            editorNode.put("id", "canvas");
            editorNode.put("resourceId", "canvas");
            ObjectNode stencilSetNode = objectMapper.createObjectNode();
            stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
            editorNode.set("stencilset", stencilSetNode);
            ObjectNode propertiesNode = objectMapper.createObjectNode();
            propertiesNode.put("process_id", model.getKey());
            propertiesNode.put("name", model.getName());
            if (StringUtils.isNotEmpty(model.getDescription())) {
                propertiesNode.put("documentation", model.getDescription());
            }
            editorNode.set("properties", propertiesNode);

            ArrayNode childShapeArray = objectMapper.createArrayNode();
            editorNode.set("childShapes", childShapeArray);
            ObjectNode childNode = objectMapper.createObjectNode();
            childShapeArray.add(childNode);
            ObjectNode boundsNode = objectMapper.createObjectNode();
            childNode.set("bounds", boundsNode);
            ObjectNode lowerRightNode = objectMapper.createObjectNode();
            boundsNode.set("lowerRight", lowerRightNode);
            lowerRightNode.put("x", 130);
            lowerRightNode.put("y", 193);
            ObjectNode upperLeftNode = objectMapper.createObjectNode();
            boundsNode.set("upperLeft", upperLeftNode);
            upperLeftNode.put("x", 100);
            upperLeftNode.put("y", 163);
            childNode.set("childShapes", objectMapper.createArrayNode());
            childNode.set("dockers", objectMapper.createArrayNode());
            childNode.set("outgoing", objectMapper.createArrayNode());
            childNode.put("resourceId", "startEvent1");
            ObjectNode stencilNode = objectMapper.createObjectNode();
            childNode.set("stencil", stencilNode);
            stencilNode.put("id", "StartNoneEvent");
            json = editorNode.toString();
        }
        
        return json;
    }

    @Override
    public Model createModel(Model newModel, User createdBy) {
        newModel.setVersion(1);
        newModel.setCreated(Calendar.getInstance().getTime());
        newModel.setCreatedBy(createdBy.getId());
        newModel.setLastUpdated(Calendar.getInstance().getTime());
        newModel.setLastUpdatedBy(createdBy.getId());

        persistModel(newModel);
        return newModel;
    }

    @Override
    public Model createModel(ModelRepresentation model, String editorJson, User createdBy) {
        Model newModel = new Model();
        newModel.setVersion(1);
        newModel.setName(model.getName());
        newModel.setKey(model.getKey());
        newModel.setModelType(model.getModelType());
        newModel.setCreated(Calendar.getInstance().getTime());
        newModel.setCreatedBy(createdBy.getId());
        newModel.setDescription(model.getDescription());
        newModel.setModelEditorJson(editorJson);
        newModel.setLastUpdated(Calendar.getInstance().getTime());
        newModel.setLastUpdatedBy(createdBy.getId());

        persistModel(newModel);
        return newModel;
    }

    public ModelRepresentation importNewVersion(String modelId, String fileName, InputStream modelStream) {
        Model processModel = getModel(modelId);
        User currentUser = SecurityUtils.getCurrentUserObject();

        if (fileName != null && (fileName.endsWith(".bpmn") || fileName.endsWith(".bpmn20.xml"))) {
            try {
                XMLInputFactory xif = XmlUtil.createSafeXmlInputFactory();
                InputStreamReader xmlIn = new InputStreamReader(modelStream, "UTF-8");
                XMLStreamReader xtr = xif.createXMLStreamReader(xmlIn);
                BpmnModel bpmnModel = bpmnXMLConverter.convertToBpmnModel(xtr);
                if (CollectionUtils.isEmpty(bpmnModel.getProcesses())) {
                    throw new BadRequestException("No process found in definition " + fileName);
                }

                if (bpmnModel.getLocationMap().size() == 0) {
                    throw new BadRequestException("No required BPMN DI information found in definition " + fileName);
                }

                ObjectNode modelNode = bpmnJsonConverter.convertToJson(bpmnModel);

                AbstractModel savedModel = saveModel(modelId, processModel.getName(), processModel.getKey(),
                        processModel.getDescription(), modelNode.toString(), true, "Version import via REST service", currentUser);
                return new ModelRepresentation(savedModel);

            } catch (BadRequestException e) {
                throw e;

            } catch (Exception e) {
                throw new BadRequestException("Import failed for " + fileName + ", error message " + e.getMessage());
            }
        } else {
            throw new BadRequestException("Invalid file name, only .bpmn and .bpmn20.xml files are supported not " + fileName);
        }
    }

    @Override
    public Model createNewModelVersion(Model modelObject, String comment, User updatedBy) {
        return (Model) internalCreateNewModelVersion(modelObject, comment, updatedBy, false);
    }

    @Override
    public ModelHistory createNewModelVersionAndReturnModelHistory(Model modelObject, String comment, User updatedBy) {
        return (ModelHistory) internalCreateNewModelVersion(modelObject, comment, updatedBy, true);
    }

    protected AbstractModel internalCreateNewModelVersion(Model modelObject, String comment, User updatedBy, boolean returnModelHistory) {
        modelObject.setLastUpdated(new Date());
        modelObject.setLastUpdatedBy(updatedBy.getId());
        modelObject.setComment(comment);

        ModelHistory historyModel = createNewModelhistory(modelObject);
        persistModelHistory(historyModel);

        modelObject.setVersion(modelObject.getVersion() + 1);
        persistModel(modelObject);

        return returnModelHistory ? historyModel : modelObject;
    }

    @Override
    public Model saveModel(Model modelObject) {
        return persistModel(modelObject);
    }

    @Override
    public Model saveModel(Model modelObject, String editorJson, byte[] imageBytes, boolean newVersion, String newVersionComment, User updatedBy) {

        return internalSave(modelObject.getName(), modelObject.getKey(), modelObject.getDescription(), editorJson, newVersion,
                newVersionComment, imageBytes, updatedBy, modelObject);
    }

    @Override
    public Model saveModel(String modelId, String name, String key, String description, String editorJson,
            boolean newVersion, String newVersionComment, User updatedBy) {

        Model modelObject = modelRepository.get(modelId);
        return internalSave(name, key, description, editorJson, newVersion, newVersionComment, null, updatedBy, modelObject);
    }

    protected Model internalSave(String name, String key, String description, String editorJson, boolean newVersion,
            String newVersionComment, byte[] imageBytes, User updatedBy, Model modelObject) {

        ObjectNode jsonNode;
        try
        {
            jsonNode = objectMapper.readValue(editorJson, ObjectNode.class);
        }
        catch (Exception e)
        {
            throw new InternalServerErrorException("Could not deserialize model definition");
        }

        if (AbstractModel.MODEL_TYPE_BPMN == modelObject.getModelType())
        {
            ObjectNode metaNode = jsonNode.with("properties");
            ArrayNode historyNode = metaNode.withArray("rds_provenances");
            ObjectNode newHistory = historyNode.addObject();
            newHistory.put("user", updatedBy.getId());
            newHistory.put("time", new Date().getTime());
            newHistory.put("designerVersion", this.getVersionFromJar());
        }
        else if (AbstractModel.MODEL_TYPE_FORM_RDS == modelObject.getModelType())
        {

            ObjectNode metaNode = jsonNode.with("metadata");
            ArrayNode historyNode = metaNode.withArray("provenance");
            ObjectNode newHistory = historyNode.addObject();
            newHistory.put("user", updatedBy.getId());
            newHistory.put("time", new Date().getTime());
            newHistory.put("designerVersion", this.getVersionFromJar());
        }
        editorJson = jsonNode.toString();

        if (!newVersion) {

            modelObject.setLastUpdated(new Date());
            modelObject.setLastUpdatedBy(updatedBy.getId());
            modelObject.setName(name);
            modelObject.setKey(key);
            modelObject.setDescription(description);
            modelObject.setModelEditorJson(editorJson);

            if (imageBytes != null) {
                modelObject.setThumbnail(imageBytes);
            }

        } else {

            ModelHistory historyModel = createNewModelhistory(modelObject);
            persistModelHistory(historyModel);

            modelObject.setVersion(modelObject.getVersion() + 1);
            modelObject.setLastUpdated(new Date());
            modelObject.setLastUpdatedBy(updatedBy.getId());
            modelObject.setName(name);
            modelObject.setKey(key);
            modelObject.setDescription(description);
            modelObject.setModelEditorJson(editorJson);
            modelObject.setComment(newVersionComment);

            if (imageBytes != null) {
                modelObject.setThumbnail(imageBytes);
            }
        }

        return persistModel(modelObject);
    }

    @Override
    public void deleteModel(String modelId) {

        Model model = modelRepository.get(modelId);
        if (model == null) {
            throw new IllegalArgumentException("No model found with id: " + modelId);
        }

        // Fetch current model history list
        List<ModelHistory> history = modelHistoryRepository.findByModelId(model.getId());

        // Move model to history and mark removed
        ModelHistory historyModel = createNewModelhistory(model);
        historyModel.setRemovalDate(Calendar.getInstance().getTime());
        persistModelHistory(historyModel);

        deleteModelAndChildren(model);
    }

    protected void deleteModelAndChildren(Model model) {

        // Models have relations with each other, in all kind of wicked and funny ways.
        // Hence, we remove first all relations, comments, etc. while collecting all models.
        // Then, once all foreign key problem makers are removed, we remove the models

        List<Model> allModels = new ArrayList<Model>();
        internalDeleteModelAndChildren(model, allModels);

        for (Model modelToDelete : allModels) {
            modelRepository.delete(modelToDelete);
        }
    }

    protected void internalDeleteModelAndChildren(Model model, List<Model> allModels) {
        // Delete all related data
        modelRelationRepository.deleteModelRelationsForParentModel(model.getId());

        allModels.add(model);
    }

    @Override
    public ReviveModelResultRepresentation reviveProcessModelHistory(ModelHistory modelHistory, User user, String newVersionComment) {
        Model latestModel = modelRepository.get(modelHistory.getModelId());
        if (latestModel == null) {
            throw new IllegalArgumentException("No process model found with id: " + modelHistory.getModelId());
        }

        // Store the current model in history
        ModelHistory latestModelHistory = createNewModelhistory(latestModel);
        persistModelHistory(latestModelHistory);

        // Populate the actual latest version with the properties in the historic model
        latestModel.setVersion(latestModel.getVersion() + 1);
        latestModel.setLastUpdated(new Date());
        latestModel.setLastUpdatedBy(user.getId());
        latestModel.setName(modelHistory.getName());
        latestModel.setKey(modelHistory.getKey());
        latestModel.setDescription(modelHistory.getDescription());
        latestModel.setModelEditorJson(modelHistory.getModelEditorJson());
        latestModel.setModelType(modelHistory.getModelType());
        latestModel.setComment(newVersionComment);
        persistModel(latestModel);

        ReviveModelResultRepresentation result = new ReviveModelResultRepresentation();

        // For apps, we need to make sure the referenced processes exist as models.
        // It could be the user has deleted the process model in the meantime. We give back that info to the user.
        if (latestModel.getModelType() == AbstractModel.MODEL_TYPE_APP) {
            if (StringUtils.isNotEmpty(latestModel.getModelEditorJson())) {
                try {
                    AppDefinition appDefinition = objectMapper.readValue(latestModel.getModelEditorJson(), AppDefinition.class);
                    for (AppModelDefinition appModelDefinition : appDefinition.getModels()) {
                        if (modelRepository.get(appModelDefinition.getId()) == null) {
                            result.getUnresolvedModels().add(new UnresolveModelRepresentation(appModelDefinition.getId(),
                                    appModelDefinition.getName(), appModelDefinition.getLastUpdatedBy()));
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Could not deserialize app model json (id = {})", latestModel.getId(), e);
                }
            }
        }

        return result;
    }

    public BpmnModel convertToBpmnModelForValidation(JsonNode editorJsonNode) {
      
      String modelId = editorJsonNode.get("modelId").asText();
      
      Map<String, String> formMap = new HashMap<String, String>();
      Map<String, String> decisionTableMap = new HashMap<String, String>();
  
      List<Model> referencedModels = modelRepository.findByParentModelId(modelId);
            for (Model childModel : referencedModels) {
                if (Model.MODEL_TYPE_FORM_RDS == childModel.getModelType()) {
          formMap.put(childModel.getId(), childModel.getKey());
  
                } else if (Model.MODEL_TYPE_DECISION_TABLE == childModel.getModelType()) {
          decisionTableMap.put(childModel.getId(), childModel.getKey());
        }
      }
            
      BpmnModel bpmnModel = bpmnJsonConverter.convertToBpmnModel(editorJsonNode, formMap, decisionTableMap);
      
      return bpmnModel;
      
    }

    @Override
    public BpmnModel getBpmnModel(AbstractModel model) {
        return getBpmnModel(model, true);
    }

    @Override
    public BpmnModel getBpmnModel(AbstractModel model, boolean resolveFormReferene) {
        BpmnModel bpmnModel = null;
        try {
            Map<String, Model> formMap = new HashMap<String, Model>();
            Map<String, Model> decisionTableMap = new HashMap<String, Model>();

            List<Model> referencedModels = modelRepository.findByParentModelId(model.getId());
            for (Model childModel : referencedModels) {
                if (Model.MODEL_TYPE_FORM_RDS == childModel.getModelType()) {
                    formMap.put(childModel.getId(), childModel);

                } else if (Model.MODEL_TYPE_DECISION_TABLE == childModel.getModelType()) {
                    decisionTableMap.put(childModel.getId(), childModel);
                }
            }

            bpmnModel = getBpmnModel(model, formMap, decisionTableMap, resolveFormReferene);

        } catch (Exception e) {
            LOGGER.error("Could not generate BPMN 2.0 model for {}", model.getId(), e);
            throw new InternalServerErrorException("Could not generate BPMN 2.0 model");
        }

        return bpmnModel;
    }

    @Override
    public BpmnModel getBpmnModel(AbstractModel model, Map<String, Model> formMap, Map<String, Model> decisionTableMap) {
        return getBpmnModel(model, formMap, decisionTableMap, true);
    }

    @Override
    public BpmnModel getBpmnModel(AbstractModel model, Map<String, Model> formMap, Map<String, Model> decisionTableMap, boolean resolveFormReference) {
        try {
            ObjectNode editorJsonNode = (ObjectNode) objectMapper.readTree(model.getModelEditorJson());
            Map<String, String> formKeyMap = new HashMap<String, String>();
            for (Model formModel : formMap.values()) {
                formKeyMap.put(formModel.getId(), formModel.getKey());
            }

            Map<String, String> decisionTableKeyMap = new HashMap<String, String>();
            for (Model decisionTableModel : decisionTableMap.values()) {
                decisionTableKeyMap.put(decisionTableModel.getId(), decisionTableModel.getKey());
            }

            BpmnModel bpmnModel = bpmnJsonConverter.convertToBpmnModel(editorJsonNode, formKeyMap, decisionTableKeyMap);
            formsAsExtension(bpmnModel, resolveFormReference);
            return bpmnModel;

        } catch (Exception e) {
            LOGGER.error("Could not generate BPMN 2.0 model for {}", model.getId(), e);
            throw new InternalServerErrorException("Could not generate BPMN 2.0 model", e);
        }
    }

    protected void addOrUpdateExtensionElement(String name, String value, UserTask userTask) {
        List<ExtensionElement> extensionElements = userTask.getExtensionElements().get(name);

        ExtensionElement extensionElement;

        if (CollectionUtils.isNotEmpty(extensionElements)) {
            extensionElement = extensionElements.get(0);
        } else {
            extensionElement = new ExtensionElement();
        }
        extensionElement.setNamespace(NAMESPACE);
        extensionElement.setNamespacePrefix("modeler");
        extensionElement.setName(name);
        extensionElement.setElementText(value);

        if (CollectionUtils.isEmpty(extensionElements)) {
            userTask.addExtensionElement(extensionElement);
        }
    }

    public Long getModelCountForUser(User user, int modelType) {
        return modelRepository.countByModelTypeAndCreatedBy(modelType, user.getId());
    }

    protected Model persistModel(Model model) {

        if (StringUtils.isNotEmpty(model.getModelEditorJson())) {

            // Parse json to java
            ObjectNode jsonNode = null;
            try {
                jsonNode = (ObjectNode) objectMapper.readTree(model.getModelEditorJson());
            } catch (Exception e) {
                LOGGER.error("Could not deserialize json model", e);
                throw new InternalServerErrorException("Could not deserialize json model");
            }

            if ((model.getModelType() == null || model.getModelType().intValue() == Model.MODEL_TYPE_BPMN)) {

                // Thumbnail
                byte[] thumbnail = modelImageService.generateThumbnailImage(model, jsonNode);
                if (thumbnail != null) {
                    model.setThumbnail(thumbnail);
                }

                modelRepository.save(model);

                // Relations
                handleBpmnProcessFormModelRelations(model, jsonNode);
                handleBpmnProcessDecisionTaskModelRelations(model, jsonNode);

            } else if (model.getModelType().intValue() == Model.MODEL_TYPE_FORM ||
                    model.getModelType().intValue() == Model.MODEL_TYPE_DECISION_TABLE) {

                jsonNode.put("name", model.getName());
                jsonNode.put("key", model.getKey());
                modelRepository.save(model);

            } else if (model.getModelType().intValue() == Model.MODEL_TYPE_APP) {

                modelRepository.save(model);
                handleAppModelProcessRelations(model, jsonNode);
            }
            else if (model.getModelType().intValue() == AbstractModel.MODEL_TYPE_FORM_RDS) {
              modelRepository.save(model);
            }
        }

        return model;
    }

    protected void persistModelHistory(ModelHistory modelHistory) {
        modelHistoryRepository.save(modelHistory);
    }

    protected void handleBpmnProcessFormModelRelations(AbstractModel bpmnProcessModel, ObjectNode editorJsonNode) {
        List<JsonNode> formReferenceNodes = JsonConverterUtil.filterOutJsonNodes(JsonConverterUtil.getBpmnProcessModelFormReferences(editorJsonNode));
        Set<String> formIds = JsonConverterUtil.gatherStringPropertyFromJsonNodes(formReferenceNodes, "id");

        handleModelRelations(bpmnProcessModel, formIds, ModelRelationTypes.TYPE_FORM_MODEL_CHILD);
    }

    protected void handleBpmnProcessDecisionTaskModelRelations(AbstractModel bpmnProcessModel, ObjectNode editorJsonNode) {
        List<JsonNode> decisionTableNodes = JsonConverterUtil.filterOutJsonNodes(JsonConverterUtil.getBpmnProcessModelDecisionTableReferences(editorJsonNode));
        Set<String> decisionTableIds = JsonConverterUtil.gatherStringPropertyFromJsonNodes(decisionTableNodes, "id");

        handleModelRelations(bpmnProcessModel, decisionTableIds, ModelRelationTypes.TYPE_DECISION_TABLE_MODEL_CHILD);
    }

    protected void handleAppModelProcessRelations(AbstractModel appModel, ObjectNode appModelJsonNode) {
        Set<String> processModelIds = JsonConverterUtil.getAppModelReferencedModelIds(appModelJsonNode);
        handleModelRelations(appModel, processModelIds, ModelRelationTypes.TYPE_PROCESS_MODEL);
    }

    /**
     * Generic handling of model relations: deleting/adding where needed.
     */
    protected void handleModelRelations(AbstractModel bpmnProcessModel, Set<String> idsReferencedInJson, String relationshipType) {

        // Find existing persisted relations
        List<ModelRelation> persistedModelRelations = modelRelationRepository.findByParentModelIdAndType(bpmnProcessModel.getId(), relationshipType);

        // if no ids referenced now, just delete them all
        if (idsReferencedInJson == null || idsReferencedInJson.size() == 0) {
            for (ModelRelation modelRelation : persistedModelRelations) {
                modelRelationRepository.delete(modelRelation);
            }
            return;
        }

        Set<String> alreadyPersistedModelIds = new HashSet<String>(persistedModelRelations.size());
        for (ModelRelation persistedModelRelation : persistedModelRelations) {
            if (!idsReferencedInJson.contains(persistedModelRelation.getModelId())) {
                // model used to be referenced, but not anymore. Delete it.
                modelRelationRepository.delete(persistedModelRelation);
            } else {
                alreadyPersistedModelIds.add(persistedModelRelation.getModelId());
            }
        }

        // Loop over all referenced ids and see which one are new
        for (String idReferencedInJson : idsReferencedInJson) {

            // if model is referenced, but it is not yet persisted = create it
            if (!alreadyPersistedModelIds.contains(idReferencedInJson)) {

                // Check if model actually still exists. Don't create the relationship if it doesn't exist. The client UI will have cope with this too.
                if (modelRepository.get(idReferencedInJson) != null) {
                    modelRelationRepository.save(new ModelRelation(bpmnProcessModel.getId(), idReferencedInJson, relationshipType));
                }
            }
        }
    }

    protected ModelHistory createNewModelhistory(Model model) {
        ModelHistory historyModel = new ModelHistory();
        historyModel.setName(model.getName());
        historyModel.setKey(model.getKey());
        historyModel.setDescription(model.getDescription());
        historyModel.setCreated(model.getCreated());
        historyModel.setLastUpdated(model.getLastUpdated());
        historyModel.setCreatedBy(model.getCreatedBy());
        historyModel.setLastUpdatedBy(model.getLastUpdatedBy());
        historyModel.setModelEditorJson(model.getModelEditorJson());
        historyModel.setModelType(model.getModelType());
        historyModel.setVersion(model.getVersion());
        historyModel.setModelId(model.getId());
        historyModel.setComment(model.getComment());

        return historyModel;
    }

    protected void populateModelBasedOnHistory(Model model, ModelHistory basedOn) {
        model.setName(basedOn.getName());
        model.setKey(basedOn.getKey());
        model.setDescription(basedOn.getDescription());
        model.setCreated(basedOn.getCreated());
        model.setLastUpdated(basedOn.getLastUpdated());
        model.setCreatedBy(basedOn.getCreatedBy());
        model.setLastUpdatedBy(basedOn.getLastUpdatedBy());
        model.setModelEditorJson(basedOn.getModelEditorJson());
        model.setModelType(basedOn.getModelType());
        model.setVersion(basedOn.getVersion());
        model.setComment(basedOn.getComment());
    }
    
    private void formsAsExtension(BpmnModel bpmnModel, boolean resolveFormReference)
    {
      Map<String, String> processedForms = new HashMap<String, String>();
      List<Process> processes = bpmnModel.getProcesses();
      for (Process process : processes)
      {
        String processFormAttribute = process.getAttributeValue(BPMN_RDS_NAMESPACE, PROCESS_FORMKEY);
        if (StringUtils.isNotBlank(processFormAttribute) && !processedForms.containsKey(processFormAttribute))
        {
          addFormToProcess(process, processFormAttribute, processedForms, resolveFormReference);
        }

        handleProcessForm(process,"viewprocessforms", processedForms, resolveFormReference);
        handleProcessForm(process,"editprocessforms", processedForms, resolveFormReference);
        handleProcessForm(process,"searchprocessforms", processedForms, resolveFormReference);
  
        for (FlowElement flowElement : process.getFlowElements())
        {
          if (flowElement instanceof UserTask || flowElement instanceof StartEvent)
          {
            String formKey = (flowElement instanceof UserTask) ? ((UserTask) flowElement).getFormKey()
                    : ((StartEvent) flowElement).getFormKey();
            if (StringUtils.isNotBlank(formKey) && !processedForms.containsKey(formKey))
            {
              addFormToProcess(process, formKey, processedForms, resolveFormReference);
            }
          }
          else if (flowElement instanceof SequenceFlow)
          {
            String arrowFormKey = flowElement.getAttributeValue(BPMN_RDS_NAMESPACE, ARROW_FORM_KEY);
            if (StringUtils.isNotBlank(arrowFormKey) && !processedForms.containsKey(arrowFormKey))
            {
              addFormToProcess(process, arrowFormKey, processedForms, resolveFormReference);
            }
          }
        }
      }
    }

    private void handleProcessForm(Process process, String elementName, Map processedForms, boolean resolveFormReference) {
        if(process.getExtensionElements().get(elementName) != null && process.getExtensionElements().get(elementName).size() > 0) {
            List<ExtensionElement> viewProcessFormList = process.getExtensionElements().get(elementName).get(0)
                .getChildElements().get("processform");
            if(viewProcessFormList == null) {
                return;
            }

            for(ExtensionElement viewProcessForm : viewProcessFormList) {
                String formkey = viewProcessForm.getAttributeValue(null, "formkey");

                if (StringUtils.isNotBlank(formkey) && !processedForms.containsKey(formkey))
                {
                    addFormToProcess(process, formkey, processedForms, resolveFormReference);
                }
            }
        }
    }


  
    private void addFormToProcess(Process process, String formKey, Map<String, String> processedForms, boolean resolveFormReference)
    {
      String formContent;
      List<String> refKeys = new ArrayList();
      if(resolveFormReference)
      {
          formContent = this.formDesignJsonService.getFormDesignJsonByKeyAndResolveReferenceIfAny(formKey);
      } else {
          formContent = this.formDesignJsonService.getFormDesignJsonByKey(formKey);
          refKeys = this.formDesignJsonService.extractFormReferenceKeys(formContent);
      }
      //Need to get formModel for name and description
      List<Model> formModels = this.modelRepository.findByKeyAndType(formKey, AbstractModel.MODEL_TYPE_FORM_RDS);
      Assert.isTrue(formModels.size()==1, "Only 1 form with same key should exist");
      Model formModel = formModels.get(0);
  
      ExtensionElement extensionElement = new ExtensionElement();
      extensionElement.setNamespace(BPMN_RDS_NAMESPACE);
      extensionElement.setNamespacePrefix(BPMN_RDS_NAMESPACE_PREFIX);
      extensionElement.setName(FORM);

      ExtensionAttribute attribute = new ExtensionAttribute();
      attribute.setName(FORMKEY);
      attribute.setValue(formKey);
      extensionElement.addAttribute(attribute);

      ExtensionAttribute nameAttr = new ExtensionAttribute();
      nameAttr.setName("name");
      nameAttr.setValue(formModel.getName());
      extensionElement.addAttribute(nameAttr);

      ExtensionAttribute descAttr = new ExtensionAttribute();
      nameAttr.setName("description");
      nameAttr.setValue(formModel.getDescription());
      extensionElement.addAttribute(descAttr);

      process.addExtensionElement(extensionElement);
  
      processedForms.put(formKey, formKey);
  
      try
      {
        JsonNode objectNode = objectMapper.readTree(formContent);
        String indented = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectNode);
        extensionElement.setElementText(indented);

        List<JsonNode> parentNodes = objectNode.findParents("formId");
        if (parentNodes != null)
        {
          for (JsonNode parent : parentNodes)
          {
            if ("rds-asf-modal-form".equalsIgnoreCase(parent.get("type").asText()) || "form-button".equalsIgnoreCase(parent.get("type").asText()))
            {
              String formId = parent.get("formId").asText();
              if (StringUtils.isNotBlank(formId) && !processedForms.containsKey(formId))
              {
                addFormToProcess(process, formId, processedForms, resolveFormReference);
              }
            }
          }
        }

        if(!resolveFormReference)
        {
            for (String refKey : refKeys)
            {
                addFormToProcess(process, refKey, processedForms, resolveFormReference);
            }
        }
      }
      catch (Exception e)
      {
        LOGGER.error("Error addFormToProcess ", e);
        throw new FlowableException("Error addFormToProcess", e);
      }
    }

    private String getVersionFromJar()
    {
        return FormDesignJsonService.class.getPackage().getImplementationVersion();
    }
}
