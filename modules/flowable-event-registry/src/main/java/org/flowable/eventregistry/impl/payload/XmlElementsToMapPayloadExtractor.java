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
package org.flowable.eventregistry.impl.payload;

import java.util.Collection;
import java.util.stream.Collectors;

import org.flowable.eventregistry.api.InboundEventPayloadExtractor;
import org.flowable.eventregistry.api.definition.EventDefinition;
import org.flowable.eventregistry.api.definition.EventPayloadTypes;
import org.flowable.eventregistry.api.runtime.EventCorrelationParameterInstance;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.runtime.EventCorrelationParameterInstanceImpl;
import org.flowable.eventregistry.impl.runtime.EventPayloadInstanceImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Joram Barrez
 */
public class XmlElementsToMapPayloadExtractor implements InboundEventPayloadExtractor<Document> {

    @Override
    public Collection<EventCorrelationParameterInstance> extractCorrelationParameters(EventDefinition eventDefinition, Document event) {
        return eventDefinition.getCorrelationParameterDefinitions().stream()
            .filter(parameterDefinition -> getChildNode(event, parameterDefinition.getName()) != null)
            .map(parameterDefinition -> new EventCorrelationParameterInstanceImpl(parameterDefinition, getPayloadValue(event, parameterDefinition.getName(), parameterDefinition.getType())))
            .collect(Collectors.toList());
    }

    @Override
    public Collection<EventPayloadInstance> extractPayload(EventDefinition eventDefinition, Document event) {
        return eventDefinition.getEventPayloadDefinitions().stream()
            .filter(parameterDefinition -> getChildNode(event, parameterDefinition.getName()) != null)
            .map(payloadDefinition -> new EventPayloadInstanceImpl(payloadDefinition, getPayloadValue(event, payloadDefinition.getName(), payloadDefinition.getType())))
            .collect(Collectors.toList());
    }

    protected Object getPayloadValue(Document document, String definitionName, String definitionType) {

        Node childNode = getChildNode(document, definitionName);
        if (childNode != null) {
            String textContent = childNode.getTextContent();

            if (EventPayloadTypes.STRING.equals(definitionType)) {
                return textContent;

            } else if (EventPayloadTypes.BOOLEAN.equals(definitionType)) {
                return Boolean.valueOf(textContent);

            } else if (EventPayloadTypes.INTEGER.equals(definitionType)) {
                return Integer.valueOf(textContent);

            } else if (EventPayloadTypes.DOUBLE.equals(definitionType)) {
                return Double.valueOf(textContent);

            } else {
                // TODO: handle type not matching

            }

        }

        return null;
    }

    protected Node getChildNode(Document document, String elementName) {
        NodeList childNodes = null;
        if (document.getChildNodes().getLength() == 1) {
            childNodes = document.getFirstChild().getChildNodes();
        } else {
            childNodes = document.getChildNodes();
        }

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (elementName.equals(node.getNodeName())) {
                return node;
            }
        }
        return null;
    }


}
