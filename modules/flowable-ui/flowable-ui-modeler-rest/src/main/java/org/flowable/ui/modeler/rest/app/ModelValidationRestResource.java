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

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;
import org.flowable.validation.ProcessValidator;
import org.flowable.validation.ProcessValidatorFactory;
import org.flowable.validation.ValidationError;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Endpoint for the Flowable modeler to validate the current model.
 *
 * Created by Pardo David on 16/02/2017.
 */
@RestController
@RequestMapping("/app")
public class ModelValidationRestResource {

    @PostMapping(value = "/rest/model/validate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<ValidationError> validate(@RequestBody JsonNode body){
        if (body != null && body.has("stencilset")) {
            JsonNode stencilset = body.get("stencilset");
            if (stencilset.has("namespace")) {
                String namespace = stencilset.get("namespace").asText();
                if (namespace.endsWith("bpmn2.0#")) {
                    BpmnModel bpmnModel = new BpmnJsonConverter().convertToBpmnModel(body);
                    ProcessValidator validator = new ProcessValidatorFactory().createDefaultProcessValidator();
                    List<ValidationError> errors = validator.validate(bpmnModel);
                    return errors;
                }
            }
        }
        return Collections.emptyList();
    }

}
