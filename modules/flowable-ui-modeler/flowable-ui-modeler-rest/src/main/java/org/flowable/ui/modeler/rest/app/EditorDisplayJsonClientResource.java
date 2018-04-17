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

import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.ui.modeler.domain.AbstractModel;
import org.flowable.ui.modeler.domain.Model;
import org.flowable.ui.modeler.domain.ModelHistory;
import org.flowable.ui.modeler.service.BpmnDisplayJsonConverter;
import org.flowable.ui.modeler.service.CmmnDisplayJsonConverter;
import org.flowable.ui.modeler.serviceapi.ModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("/app")
public class EditorDisplayJsonClientResource {

    @Autowired
    protected ModelService modelService;

    @Autowired
    protected BpmnDisplayJsonConverter bpmnDisplayJsonConverter;

    @Autowired
    protected CmmnDisplayJsonConverter cmmnDisplayJsonConverter;

    protected ObjectMapper objectMapper = new ObjectMapper();

    @RequestMapping(value = "/rest/models/{modelId}/model-json", method = RequestMethod.GET, produces = "application/json")
    public JsonNode getModelJSON(@PathVariable String modelId) {
        ObjectNode displayNode = objectMapper.createObjectNode();
        Model model = modelService.getModel(modelId);
        if (model.getModelType() != null && AbstractModel.MODEL_TYPE_CMMN == model.getModelType()) {
            cmmnDisplayJsonConverter.processCaseElements(model, displayNode, new org.flowable.cmmn.model.GraphicInfo());
        } else {
            bpmnDisplayJsonConverter.processProcessElements(model, displayNode, new GraphicInfo());
        }
        return displayNode;
    }

    @RequestMapping(value = "/rest/models/{processModelId}/history/{processModelHistoryId}/model-json", method = RequestMethod.GET, produces = "application/json")
    public JsonNode getModelHistoryJSON(@PathVariable String processModelId, @PathVariable String processModelHistoryId) {
        ObjectNode displayNode = objectMapper.createObjectNode();
        ModelHistory model = modelService.getModelHistory(processModelId, processModelHistoryId);
        if (model.getModelType() != null && AbstractModel.MODEL_TYPE_CMMN == model.getModelType()) {
            cmmnDisplayJsonConverter.processCaseElements(model, displayNode, new org.flowable.cmmn.model.GraphicInfo());
        } else {
            bpmnDisplayJsonConverter.processProcessElements(model, displayNode, new GraphicInfo());
        }
        return displayNode;
    }
}
