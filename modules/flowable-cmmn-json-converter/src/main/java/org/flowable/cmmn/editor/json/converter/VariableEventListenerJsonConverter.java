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
import org.flowable.cmmn.editor.constants.CmmnStencilConstants;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.VariableEventListener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class VariableEventListenerJsonConverter extends AbstractEventListenerJsonConverter {


    public static void fillTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToCmmnMap,
                                 Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
        fillJsonTypes(convertersToCmmnMap);
        fillCmmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToCmmnMap) {
        convertersToCmmnMap.put(STENCIL_VARIABLE_EVENT_LISTENER, VariableEventListenerJsonConverter.class);
    }

    public static void fillCmmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
        convertersToJsonMap.put(VariableEventListener.class, VariableEventListenerJsonConverter.class);
    }

    @Override
    protected void convertElementToJson(ObjectNode elementNode, ObjectNode propertiesNode, ActivityProcessor processor,
            BaseElement baseElement, CmmnModel cmmnModel, CmmnJsonConverterContext converterContext) {
        
        PlanItem planItem = (PlanItem) baseElement;
        VariableEventListener variableEventListener = (VariableEventListener) planItem.getPlanItemDefinition();
        
        convertCommonElementToJson(elementNode, propertiesNode, baseElement);
        
        if (StringUtils.isNotEmpty(variableEventListener.getVariableName())) {
            propertiesNode.put(PROPERTY_VARIABLE_LISTENER_VARIABLE_NAME, variableEventListener.getVariableName());
        }
        
        if (StringUtils.isNotEmpty(variableEventListener.getVariableChangeType())) {
            propertiesNode.put(PROPERTY_VARIABLE_LISTENER_VARIABLE_CHANGE_TYPE, variableEventListener.getVariableChangeType());
        }
    }

    @Override
    protected BaseElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, ActivityProcessor processor, BaseElement parentElement,
            Map<String, JsonNode> shapeMap, CmmnModel cmmnModel, CmmnJsonConverterContext converterContext, CmmnJsonConverter.CmmnModelIdHelper cmmnModelIdHelper) {
        
        VariableEventListener variableEventListener = new VariableEventListener();
        convertCommonJsonToElement(elementNode, variableEventListener);
        
        String variableName = getPropertyValueAsString(PROPERTY_VARIABLE_LISTENER_VARIABLE_NAME, elementNode);
        if (StringUtils.isNotEmpty(variableName)) {
            variableEventListener.setVariableName(variableName);
            
            String variableChangeType = getPropertyValueAsString(PROPERTY_VARIABLE_LISTENER_VARIABLE_CHANGE_TYPE, elementNode);
            if (StringUtils.isNotEmpty(variableChangeType)) {
                variableEventListener.setVariableChangeType(variableChangeType);
            }
        }
        
        return variableEventListener;
    }

    @Override
    protected String getStencilId(BaseElement baseElement) {
        return CmmnStencilConstants.STENCIL_VARIABLE_EVENT_LISTENER;
    }
}
