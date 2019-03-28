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

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.editor.constants.CmmnStencilConstants;
import org.flowable.cmmn.editor.json.converter.util.ListenerConverterUtil;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.EventListener;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
public abstract class AbstractEventListenerJsonConverter extends BaseCmmnJsonConverter {

    protected void convertCommonElementToJson(ObjectNode elementNode, ObjectNode propertiesNode, BaseElement baseElement) {
        PlanItemDefinition planItemDefinition = ((PlanItem) baseElement).getPlanItemDefinition();
        ListenerConverterUtil.convertLifecycleListenersToJson(objectMapper, propertiesNode, planItemDefinition);

        if (planItemDefinition instanceof EventListener) {
            EventListener eventListener = (EventListener) planItemDefinition;
            if (StringUtils.isNotEmpty(eventListener.getAvailableConditionExpression())) {
                propertiesNode.put(CmmnStencilConstants.PROPERTY_EVENT_LISTENER_AVAILABLE_CONDITION, eventListener.getAvailableConditionExpression());
            }
        }
    }

    protected void convertCommonJsonToElement(JsonNode elementNode, EventListener eventListener) {
        ListenerConverterUtil.convertJsonToLifeCycleListeners(elementNode, eventListener);

        String availableCondition = CmmnJsonConverterUtil.getPropertyValueAsString(CmmnStencilConstants.PROPERTY_EVENT_LISTENER_AVAILABLE_CONDITION, elementNode);
        if (StringUtils.isNotEmpty(availableCondition)) {
            eventListener.setAvailableConditionExpression(availableCondition);
        }
    }

}
