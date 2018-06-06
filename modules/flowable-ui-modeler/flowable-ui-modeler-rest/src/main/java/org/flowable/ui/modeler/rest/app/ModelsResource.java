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
package org.flowable.ui.modeler.rest.app;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.flowable.ui.common.model.ResultListDataRepresentation;
import org.flowable.ui.common.security.SecurityUtils;
import org.flowable.ui.common.service.exception.ConflictingRequestException;
import org.flowable.ui.common.service.exception.InternalServerErrorException;
import org.flowable.ui.modeler.domain.AbstractModel;
import org.flowable.ui.modeler.domain.Model;
import org.flowable.ui.modeler.model.ModelKeyRepresentation;
import org.flowable.ui.modeler.model.ModelRepresentation;
import org.flowable.ui.modeler.service.FlowableModelQueryService;
import org.flowable.ui.modeler.serviceapi.ModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("/app")
public class ModelsResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelsResource.class);

    @Autowired
    protected FlowableModelQueryService modelQueryService;

    @Autowired
    protected ModelService modelService;

    @Autowired
    protected ObjectMapper objectMapper;

    @RequestMapping(value = "/rest/models", method = RequestMethod.GET, produces = "application/json")
    public ResultListDataRepresentation getModels(@RequestParam(required = false) String filter, @RequestParam(required = false) String sort, @RequestParam(required = false) Integer modelType,
            HttpServletRequest request) {

        return modelQueryService.getModels(filter, sort, modelType, request);
    }

    @RequestMapping(value = "/rest/models-for-app-definition", method = RequestMethod.GET, produces = "application/json")
    public ResultListDataRepresentation getModelsToIncludeInAppDefinition() {
        return modelQueryService.getModelsToIncludeInAppDefinition();
    }
    
    @RequestMapping(value = "/rest/cmmn-models-for-app-definition", method = RequestMethod.GET, produces = "application/json")
    public ResultListDataRepresentation getCmmnModelsToIncludeInAppDefinition() {
        return modelQueryService.getCmmnModelsToIncludeInAppDefinition();
    }

    @RequestMapping(value = "/rest/import-process-model", method = RequestMethod.POST, produces = "application/json")
    public ModelRepresentation importProcessModel(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        return modelQueryService.importProcessModel(request, file);
    }

    /*
     * specific endpoint for IE9 flash upload component
     */
    @RequestMapping(value = "/rest/import-process-model/text", method = RequestMethod.POST)
    public String importProcessModelText(HttpServletRequest request, @RequestParam("file") MultipartFile file) {

        ModelRepresentation modelRepresentation = modelQueryService.importProcessModel(request, file);
        String modelRepresentationJson = null;
        try {
            modelRepresentationJson = objectMapper.writeValueAsString(modelRepresentation);
        } catch (Exception e) {
            LOGGER.error("Error while processing Model representation json", e);
            throw new InternalServerErrorException("Model Representation could not be saved");
        }

        return modelRepresentationJson;
    }
    
    @RequestMapping(value = "/rest/import-case-model", method = RequestMethod.POST, produces = "application/json")
    public ModelRepresentation importCaseModel(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        return modelQueryService.importCaseModel(request, file);
    }

    /*
     * specific endpoint for IE9 flash upload component
     */
    @RequestMapping(value = "/rest/import-case-model/text", method = RequestMethod.POST)
    public String importCaseModelText(HttpServletRequest request, @RequestParam("file") MultipartFile file) {

        ModelRepresentation modelRepresentation = modelQueryService.importCaseModel(request, file);
        String modelRepresentationJson = null;
        try {
            modelRepresentationJson = objectMapper.writeValueAsString(modelRepresentation);
        } catch (Exception e) {
            LOGGER.error("Error while processing Model representation json", e);
            throw new InternalServerErrorException("Model Representation could not be saved");
        }

        return modelRepresentationJson;
    }

    @RequestMapping(value = "/rest/models", method = RequestMethod.POST, produces = "application/json")
    public ModelRepresentation createModel(@RequestBody ModelRepresentation modelRepresentation) {
        modelRepresentation.setKey(modelRepresentation.getKey().replaceAll(" ", ""));
        checkForDuplicateKey(modelRepresentation);

        String json = modelService.createModelJson(modelRepresentation);

        Model newModel = modelService.createModel(modelRepresentation, json, SecurityUtils.getCurrentUserObject());
        return new ModelRepresentation(newModel);
    }

    protected void checkForDuplicateKey(ModelRepresentation modelRepresentation) {
        ModelKeyRepresentation modelKeyInfo = modelService.validateModelKey(null, modelRepresentation.getModelType(), modelRepresentation.getKey());
        if (modelKeyInfo.isKeyAlreadyExists()) {
            throw new ConflictingRequestException("Provided model key already exists: " + modelRepresentation.getKey());
        }
    }

    @RequestMapping(value = "/rest/models/{modelId}/clone", method = RequestMethod.POST, produces = "application/json")
    public ModelRepresentation duplicateModel(@PathVariable String modelId, @RequestBody ModelRepresentation modelRepresentation) {

        String json = null;
        Model model = null;
        if (modelId != null) {
            model = modelService.getModel(modelId);
            json = model.getModelEditorJson();
        }

        if (model == null) {
            throw new InternalServerErrorException("Error duplicating model : Unknown original model");
        }
        
        modelRepresentation.setKey(modelRepresentation.getKey().replaceAll(" ", ""));
        checkForDuplicateKey(modelRepresentation);
        
        if (modelRepresentation.getModelType() == null || modelRepresentation.getModelType().equals(AbstractModel.MODEL_TYPE_BPMN)) {            
            // BPMN model
            ObjectNode editorNode = null;
            try {
                editorNode = (ObjectNode) objectMapper.readTree(json);

                ObjectNode propertiesNode = (ObjectNode) editorNode.get("properties");
                String processId = modelRepresentation.getName().replaceAll(" ", "");
                propertiesNode.put("process_id", processId);
                propertiesNode.put("name", modelRepresentation.getName());
                if (StringUtils.isNotEmpty(modelRepresentation.getDescription())) {
                    propertiesNode.put("documentation", modelRepresentation.getDescription());
                }
                editorNode.set("properties", propertiesNode);

            } catch (IOException e) {
                e.printStackTrace();
            }

            if (editorNode != null) {
                json = editorNode.toString();
            }
        }

        // create the new model
        Model newModel = modelService.createModel(modelRepresentation, json, SecurityUtils.getCurrentUserObject());

        // copy also the thumbnail
        byte[] imageBytes = model.getThumbnail();
        newModel = modelService.saveModel(newModel, newModel.getModelEditorJson(), imageBytes, false, newModel.getComment(), SecurityUtils.getCurrentUserObject());

        return new ModelRepresentation(newModel);
    }

    protected ObjectNode deleteEmbededReferencesFromBPMNModel(ObjectNode editorJsonNode) {
        try {
            internalDeleteNodeByNameFromBPMNModel(editorJsonNode, "formreference");
            internalDeleteNodeByNameFromBPMNModel(editorJsonNode, "subprocessreference");
            return editorJsonNode;
        } catch (Exception e) {
            throw new InternalServerErrorException("Cannot delete the external references");
        }
    }

    protected ObjectNode deleteEmbededReferencesFromStepModel(ObjectNode editorJsonNode) {
        try {
            JsonNode startFormNode = editorJsonNode.get("startForm");
            if (startFormNode != null) {
                editorJsonNode.remove("startForm");
            }
            internalDeleteNodeByNameFromStepModel(editorJsonNode.get("steps"), "formDefinition");
            internalDeleteNodeByNameFromStepModel(editorJsonNode.get("steps"), "subProcessDefinition");
            return editorJsonNode;
        } catch (Exception e) {
            throw new InternalServerErrorException("Cannot delete the external references");
        }
    }

    protected void internalDeleteNodeByNameFromBPMNModel(JsonNode editorJsonNode, String propertyName) {
        JsonNode childShapesNode = editorJsonNode.get("childShapes");
        if (childShapesNode != null && childShapesNode.isArray()) {
            ArrayNode childShapesArrayNode = (ArrayNode) childShapesNode;
            for (JsonNode childShapeNode : childShapesArrayNode) {
                // Properties
                ObjectNode properties = (ObjectNode) childShapeNode.get("properties");
                if (properties != null && properties.has(propertyName)) {
                    JsonNode propertyNode = properties.get(propertyName);
                    if (propertyNode != null) {
                        properties.remove(propertyName);
                    }
                }

                // Potential nested child shapes
                if (childShapeNode.has("childShapes")) {
                    internalDeleteNodeByNameFromBPMNModel(childShapeNode, propertyName);
                }

            }
        }
    }

    private void internalDeleteNodeByNameFromStepModel(JsonNode stepsNode, String propertyName) {

        if (stepsNode == null || !stepsNode.isArray()) {
            return;
        }

        for (JsonNode jsonNode : stepsNode) {

            ObjectNode stepNode = (ObjectNode) jsonNode;
            if (stepNode.has(propertyName)) {
                JsonNode propertyNode = stepNode.get(propertyName);
                if (propertyNode != null) {
                    stepNode.remove(propertyName);
                }
            }

            // Nested steps
            if (stepNode.has("steps")) {
                internalDeleteNodeByNameFromStepModel(stepNode.get("steps"), propertyName);
            }

            // Overdue steps
            if (stepNode.has("overdueSteps")) {
                internalDeleteNodeByNameFromStepModel(stepNode.get("overdueSteps"), propertyName);
            }

            // Choices is special, can have nested steps inside
            if (stepNode.has("choices")) {
                ArrayNode choicesArrayNode = (ArrayNode) stepNode.get("choices");
                for (JsonNode choiceNode : choicesArrayNode) {
                    if (choiceNode.has("steps")) {
                        internalDeleteNodeByNameFromStepModel(choiceNode.get("steps"), propertyName);
                    }
                }
            }
        }
    }

}
