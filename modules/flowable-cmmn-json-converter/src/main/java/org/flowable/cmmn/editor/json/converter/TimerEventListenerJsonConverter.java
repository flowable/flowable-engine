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
import org.flowable.cmmn.editor.json.converter.CmmnJsonConverter.CmmnModelIdHelper;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.TimerEventListener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
public class TimerEventListenerJsonConverter extends AbstractEventListenerJsonConverter {
    
    @Override
    protected String getStencilId(BaseElement baseElement) {
        return CmmnStencilConstants.STENCIL_TIMER_EVENT_LISTENER;
    }
    
    public static void fillTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToCmmnMap,
            Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
        fillJsonTypes(convertersToCmmnMap);
        fillCmmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToBpmnMap) {
        convertersToBpmnMap.put(STENCIL_TIMER_EVENT_LISTENER, TimerEventListenerJsonConverter.class);
    }

    public static void fillCmmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
        convertersToJsonMap.put(TimerEventListener.class, TimerEventListenerJsonConverter.class);
    }

    @Override
    protected void convertElementToJson(ObjectNode elementNode, ObjectNode propertiesNode, ActivityProcessor processor,
            BaseElement baseElement, CmmnModel cmmnModel, CmmnJsonConverterContext converterContext) {
        
        PlanItem planItem = (PlanItem) baseElement;
        TimerEventListener timerEventListener = (TimerEventListener) planItem.getPlanItemDefinition();

        propertiesNode.put(PROPERTY_TIMER_EXPRESSION, timerEventListener.getTimerExpression());
        
        if (timerEventListener.getTimerStartTriggerSourceRef() != null) {
            ObjectNode startTriggerPlanItemNode = propertiesNode.putObject(PROPERTY_TIMER_START_TRIGGER_SOURCE_REF);
            startTriggerPlanItemNode.put("id", timerEventListener.getTimerStartTriggerSourceRef());
            propertiesNode.put(PROPERTY_TIMER_START_TRIGGER_STANDARD_EVENT, timerEventListener.getTimerStartTriggerStandardEvent());
        }

        convertCommonElementToJson(elementNode, propertiesNode, baseElement);
    }

    @Override
    protected BaseElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, ActivityProcessor processor,
            BaseElement parentElement, Map<String, JsonNode> shapeMap, CmmnModel cmmnModel,
            CmmnJsonConverterContext converterContext, CmmnModelIdHelper cmmnModelIdHelper) {
        
        TimerEventListener timerEventListener = new TimerEventListener();
        timerEventListener.setTimerExpression(CmmnJsonConverterUtil.getPropertyValueAsString(CmmnStencilConstants.PROPERTY_TIMER_EXPRESSION, elementNode));
        
        String sourceRefId = null;
        JsonNode sourceRefNode = CmmnJsonConverterUtil.getProperty(CmmnStencilConstants.PROPERTY_TIMER_START_TRIGGER_SOURCE_REF, elementNode);
        if (sourceRefNode != null && sourceRefNode.has("id")) {
            sourceRefId = sourceRefNode.get("id").asText();
        }
        if (StringUtils.isNotEmpty(sourceRefId)) {
            JsonNode referencedNode = shapeMap.get(sourceRefId);  // The id is the json id, not the cmmn plan item id yet
            if (referencedNode != null) {
                timerEventListener.setTimerStartTriggerSourceRef(CmmnJsonConverterUtil.getElementId(referencedNode));
                timerEventListener.setTimerStartTriggerStandardEvent(CmmnJsonConverterUtil.getPropertyValueAsString(CmmnStencilConstants.PROPERTY_TIMER_START_TRIGGER_STANDARD_EVENT, elementNode));
            }
        }

        convertCommonJsonToElement(elementNode, timerEventListener);

        return timerEventListener;
        
    }

}
