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
import org.flowable.cmmn.model.ReactivateEventListener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author aplatakis
 */
public class ReactivateEventListenerJsonConverter extends AbstractEventListenerJsonConverter {

    public static void fillTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToCmmnMap,
            Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
        fillJsonTypes(convertersToCmmnMap);
        fillCmmnTypes(convertersToJsonMap);
    }

    public static void fillJsonTypes(Map<String, Class<? extends BaseCmmnJsonConverter>> convertersToCmmnMap) {
        convertersToCmmnMap.put(CmmnStencilConstants.STENCIL_REACTIVATE_EVENT_LISTENER, ReactivateEventListenerJsonConverter.class);
    }

    public static void fillCmmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseCmmnJsonConverter>> convertersToJsonMap) {
        convertersToJsonMap.put(ReactivateEventListener.class, ReactivateEventListenerJsonConverter.class);
    }

    /**
     * {@inheritDoc}
     * Converts reactivate event listener to json by setting reactive listener specific properties
     */
    protected void convertElementToJson(ObjectNode elementNode, ObjectNode propertiesNode, ActivityProcessor processor, BaseElement baseElement,
            CmmnModel cmmnModel, CmmnJsonConverterContext converterContext) {
        this.convertCommonElementToJson(elementNode, propertiesNode, baseElement);
        ReactivateEventListener reactivateEventListener = (ReactivateEventListener) ((PlanItem) baseElement).getPlanItemDefinition();
        if (StringUtils.isNotEmpty(reactivateEventListener.getEventType())) {
            propertiesNode.put(CmmnStencilConstants.PROPERTY_EVENT_REGISTRY_EVENT_KEY, reactivateEventListener.getEventType());
        }
    }

    /**
     * {@inheritDoc}
     * Reactivate event listener is mapped to the same stencilId as generic event listener so to convert to correct element reactivate specific properties are checked
     */
    protected BaseElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, ActivityProcessor processor, BaseElement parentElement,
            Map<String, JsonNode> shapeMap, CmmnModel cmmnModel, CmmnJsonConverterContext converterContext, CmmnModelIdHelper cmmnModelIdHelper) {

        ReactivateEventListener reactivateEventListener = new ReactivateEventListener();
        this.convertCommonJsonToElement(elementNode, reactivateEventListener);
        String eventType = CmmnJsonConverterUtil.getPropertyValueAsString(CmmnStencilConstants.PROPERTY_EVENT_REGISTRY_EVENT_KEY, elementNode);
        if (StringUtils.isNotEmpty(eventType)) {
            reactivateEventListener.setEventType(eventType);
        }

        return reactivateEventListener;
    }

    @Override
    protected String getStencilId(BaseElement baseElement) {
        return CmmnStencilConstants.STENCIL_REACTIVATE_EVENT_LISTENER;
    }

}
