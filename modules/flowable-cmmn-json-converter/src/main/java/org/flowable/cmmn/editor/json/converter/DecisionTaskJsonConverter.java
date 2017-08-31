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
package org.flowable.cmmn.editor.json.converter;

import java.util.Map;

import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CaseElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.ServiceTask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 * @author Yvo Swillens
 */
public class DecisionTaskJsonConverter extends BaseCmmnJsonConverter implements DecisionTableAwareConverter {

    protected Map<String, String> decisionTableMap;

    public static void fillTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToBpmnMap,
            Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {

        fillJsonTypes(convertersToBpmnMap);
        fillBpmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_TASK_DECISION, DecisionTaskJsonConverter.class);
    }

    public static void fillBpmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
    }

    protected String getStencilId(BaseElement baseElement) {
        return STENCIL_TASK_DECISION;
    }

    protected CaseElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, BaseElement parentElement, Map<String, JsonNode> shapeMap, CmmnModel cmmnModel) {

        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setType(ServiceTask.DMN_TASK);

        JsonNode decisionTableReferenceNode = getProperty(PROPERTY_DECISIONTABLE_REFERENCE, elementNode);
        if (decisionTableReferenceNode != null && decisionTableReferenceNode.has("id") && !decisionTableReferenceNode.get("id").isNull()) {

            String decisionTableId = decisionTableReferenceNode.get("id").asText();
            if (decisionTableMap != null) {
                String decisionTableKey = decisionTableMap.get(decisionTableId);

                FieldExtension decisionTableKeyField = new FieldExtension();
                decisionTableKeyField.setFieldName(PROPERTY_DECISIONTABLE_REFERENCE_KEY);
                decisionTableKeyField.setStringValue(decisionTableKey);
                serviceTask.getFieldExtensions().add(decisionTableKeyField);
            }
        }

        return serviceTask;
    }

    @Override
    protected void convertElementToJson(ObjectNode elementNode, ObjectNode propertiesNode, BaseElement baseElement, CmmnModel cmmnModel) {

    }

    @Override
    public void setDecisionTableMap(Map<String, String> decisionTableMap) {
        this.decisionTableMap = decisionTableMap;
    }
}
