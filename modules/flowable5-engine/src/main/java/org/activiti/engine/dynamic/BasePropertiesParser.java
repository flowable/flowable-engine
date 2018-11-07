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
package org.activiti.engine.dynamic;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.engine.DynamicBpmnConstants;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Created by Pardo David on 5/12/2016.
 */
public abstract class BasePropertiesParser implements PropertiesParser, DynamicBpmnConstants, PropertiesParserConstants {

    @Override
    public ObjectNode parseElement(FlowElement flowElement, ObjectNode flowElementNode, ObjectMapper mapper) {
        ObjectNode resultNode = mapper.createObjectNode();
        resultNode.put(ELEMENT_ID, flowElement.getId());
        resultNode.put(ELEMENT_TYPE, flowElement.getClass().getSimpleName());
        if (supports(flowElement)) {
            resultNode.set(ELEMENT_PROPERTIES, createPropertiesNode(flowElement, flowElementNode, mapper));
        }
        return resultNode;
    }

    protected void putPropertyValue(String key, String value, ObjectNode propertiesNode) {
        if (StringUtils.isNotBlank(value)) {
            propertiesNode.put(key, value);
        }
    }

    protected void putPropertyValue(String key, List<String> values, ObjectNode propertiesNode) {
        // we don't set a node value if the collection is null.
        // An empty collection is a indicator. if a task has candidate users you can only overrule it by putting an empty array as dynamic candidate users
        if (values != null) {
            ArrayNode arrayNode = propertiesNode.putArray(key);
            for (String value : values) {
                arrayNode.add(value);
            }
        }
    }

    protected void putPropertyValue(String key, JsonNode node, ObjectNode propertiesNode) {
        if (node != null) {
            if (!node.isMissingNode() && !node.isNull()) {
                propertiesNode.set(key, node);
            }
        }
    }

    protected abstract ObjectNode createPropertiesNode(FlowElement flowElement, ObjectNode flowElementNode, ObjectMapper objectMapper);

    @Override
    public abstract boolean supports(FlowElement flowElement);
}
