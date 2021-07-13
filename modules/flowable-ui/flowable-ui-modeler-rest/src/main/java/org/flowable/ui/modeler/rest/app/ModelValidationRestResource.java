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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;
import org.flowable.editor.language.json.converter.util.JsonConverterUtil;
import org.flowable.ui.modeler.domain.Model;
import org.flowable.ui.modeler.service.ConverterContext;
import org.flowable.ui.modeler.service.ModelRelationService;
import org.flowable.ui.modeler.serviceapi.ModelService;
import org.flowable.validation.ProcessValidator;
import org.flowable.validation.ProcessValidatorFactory;
import org.flowable.validation.ValidationError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Endpoint for the Flowable modeler to validate the current model.
 * <p>
 * Created by Pardo David on 16/02/2017.
 */
@RestController
public class ModelValidationRestResource {

    @Autowired
    protected ModelService modelService;

    @Autowired
    protected ModelRelationService modelRelationService;

    @Autowired
    protected ObjectMapper objectMapper;

    @PostMapping(value = "/rest/model/validate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<ValidationError> validate(@RequestBody JsonNode body) {
        if (body != null && body.has("stencilset")) {
            JsonNode stencilset = body.get("stencilset");
            if (stencilset.has("namespace")) {
                String namespace = stencilset.get("namespace").asText();
                if (namespace.endsWith("bpmn2.0#")) {
                    ConverterContext converterContext = initBpmnConverterContext(body);
                    BpmnModel bpmnModel = new BpmnJsonConverter().convertToBpmnModel(body, converterContext);
                    ProcessValidator validator = new ProcessValidatorFactory().createDefaultProcessValidator();
                    List<ValidationError> errors = validator.validate(bpmnModel);
                    return errors;
                }
            }
        }
        return Collections.emptyList();
    }

    protected ConverterContext initBpmnConverterContext(JsonNode editorJsonNode) {
        ConverterContext converterContext = new ConverterContext(modelService, objectMapper);

        gatherFormModelsFromBpmnJson(editorJsonNode)
                .forEach(converterContext::addFormModel);
        gatherDecisionTableModelsFromBpmnJson(editorJsonNode)
                .forEach(converterContext::addDecisionTableModel);
        gatherDecisionServiceModelsFromBpmnJson(editorJsonNode)
                .forEach(converterContext::addDecisionServiceModel);

        return converterContext;
    }

    protected List<Model> gatherFormModelsFromBpmnJson(JsonNode editorJsonNode) {
        List<JsonNode> formReferenceNodes = JsonConverterUtil.filterOutJsonNodes(JsonConverterUtil.getBpmnProcessModelFormReferences(editorJsonNode));
        Set<String> formIds = JsonConverterUtil.gatherStringPropertyFromJsonNodes(formReferenceNodes, "id");
        List<Model> formModels = formIds.stream()
                .map(formId -> modelService.getModel(formId))
                .collect(Collectors.toList());

        return formModels;
    }

    protected List<Model> gatherDecisionTableModelsFromBpmnJson(JsonNode editorJsonNode) {
        List<JsonNode> decisionTableReferenceNodes = JsonConverterUtil
                .filterOutJsonNodes(JsonConverterUtil.getBpmnProcessModelDecisionTableReferences(editorJsonNode));
        Set<String> decisionTableIds = JsonConverterUtil.gatherStringPropertyFromJsonNodes(decisionTableReferenceNodes, "id");
        List<Model> decisionTableModels = decisionTableIds.stream()
                .map(decisionTableId -> modelService.getModel(decisionTableId))
                .collect(Collectors.toList());

        return decisionTableModels;
    }

    protected List<Model> gatherDecisionServiceModelsFromBpmnJson(JsonNode editorJsonNode) {
        List<JsonNode> decisionServiceReferenceNodes = JsonConverterUtil
                .filterOutJsonNodes(JsonConverterUtil.getBpmnProcessModelDecisionServiceReferences(editorJsonNode));
        Set<String> decisionServiceIds = JsonConverterUtil.gatherStringPropertyFromJsonNodes(decisionServiceReferenceNodes, "id");
        List<Model> decisionServiceModels = decisionServiceIds.stream()
                .map(decisionServiceId -> modelService.getModel(decisionServiceId))
                .collect(Collectors.toList());

        return decisionServiceModels;
    }
}
