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

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.editor.json.converter.CmmnJsonConverter.CmmnModelIdHelper;
import org.flowable.cmmn.editor.json.converter.util.ListenerConverterUtil;
import org.flowable.cmmn.editor.json.model.CmmnModelInfo;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CaseElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.DecisionTask;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.PlanItem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 * @author Yvo Swillens
 */
public class DecisionTaskJsonConverter extends BaseCmmnJsonConverter implements DecisionTableKeyAwareConverter {

    protected Map<String, CmmnModelInfo> decisionTableKeyMap;

    public static void fillTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToCmmnMap, Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
        fillJsonTypes(convertersToCmmnMap);
        fillCmmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_TASK_DECISION, DecisionTaskJsonConverter.class);
    }

    public static void fillCmmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
        convertersToJsonMap.put(DecisionTask.class, DecisionTaskJsonConverter.class);
    }

    @Override
    protected String getStencilId(BaseElement baseElement) {
        return STENCIL_TASK_DECISION;
    }

    @Override
    protected CaseElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, ActivityProcessor processor,
                    BaseElement parentElement, Map<String, JsonNode> shapeMap, CmmnModel cmmnModel, CmmnModelIdHelper cmmnModelIdHelper) {

        DecisionTask decisionTask = new DecisionTask();

        JsonNode decisionTableReferenceNode = CmmnJsonConverterUtil.getProperty(PROPERTY_DECISIONTABLE_REFERENCE, elementNode);
        if (decisionTableReferenceNode != null && decisionTableReferenceNode.has("id") && !decisionTableReferenceNode.get("id").isNull()) {

            String decisionTableKey = decisionTableReferenceNode.get("key").asText();
            if (StringUtils.isNotEmpty(decisionTableKey)) {
                decisionTask.setDecisionRef(decisionTableKey);
            }
        }

        addBooleanField(elementNode, decisionTask, PROPERTY_DECISIONTABLE_THROW_ERROR_NO_HITS, PROPERTY_DECISIONTABLE_THROW_ERROR_NO_HITS_KEY);
        addBooleanField(elementNode, decisionTask, PROPERTY_DECISIONTABLE_FALLBACK_TO_DEFAULT_TENANT, PROPERTY_DECISIONTABLE_FALLBACK_TO_DEFAULT_TENANT_KEY);

        ListenerConverterUtil.convertJsonToLifeCycleListeners(elementNode, decisionTask);

        return decisionTask;
    }

    protected void addBooleanField(JsonNode elementNode, DecisionTask decisionTask, String propertyName, String fieldName) {
        boolean decisionTableThrowErrorOnNoHitsNode = CmmnJsonConverterUtil.getPropertyValueAsBoolean(propertyName, elementNode);
        FieldExtension decisionTableThrowErrorOnNoHitsField = new FieldExtension();
        decisionTableThrowErrorOnNoHitsField.setFieldName(fieldName);
        decisionTableThrowErrorOnNoHitsField.setStringValue(decisionTableThrowErrorOnNoHitsNode ? "true" : "false");
        decisionTask.getFieldExtensions().add(decisionTableThrowErrorOnNoHitsField);
    }

    @Override
    protected void convertElementToJson(ObjectNode elementNode, ObjectNode propertiesNode, ActivityProcessor processor, BaseElement baseElement, CmmnModel cmmnModel) {
        DecisionTask decisionTask = (DecisionTask) ((PlanItem) baseElement).getPlanItemDefinition();

        ObjectNode decisionReferenceNode = objectMapper.createObjectNode();
        propertiesNode.set(PROPERTY_DECISIONTABLE_REFERENCE, decisionReferenceNode);

        CmmnModelInfo modelInfo = decisionTableKeyMap != null && decisionTask.getDecisionRef() != null ? decisionTableKeyMap.get(decisionTask.getDecisionRef()) : null;
        if (modelInfo != null) {
            decisionReferenceNode.put("id", modelInfo.getId());
            decisionReferenceNode.put("name", modelInfo.getName());
            decisionReferenceNode.put("key", modelInfo.getKey());
        }

        for (FieldExtension fieldExtension : decisionTask.getFieldExtensions()) {
            if (PROPERTY_DECISIONTABLE_THROW_ERROR_NO_HITS_KEY.equals(fieldExtension.getFieldName())) {
                propertiesNode.put(PROPERTY_DECISIONTABLE_THROW_ERROR_NO_HITS, Boolean.parseBoolean(fieldExtension.getStringValue()));
            }
            if (PROPERTY_DECISIONTABLE_FALLBACK_TO_DEFAULT_TENANT_KEY.equals(fieldExtension.getFieldName())) {
                propertiesNode.put(PROPERTY_DECISIONTABLE_FALLBACK_TO_DEFAULT_TENANT, Boolean.parseBoolean(fieldExtension.getStringValue()));
            }
        }

        ListenerConverterUtil.convertLifecycleListenersToJson(objectMapper, propertiesNode, decisionTask);
    }

    @Override
    public void setDecisionTableKeyMap(Map<String, CmmnModelInfo> decisionTableMap) {
        decisionTableKeyMap = decisionTableMap;
    }
}
