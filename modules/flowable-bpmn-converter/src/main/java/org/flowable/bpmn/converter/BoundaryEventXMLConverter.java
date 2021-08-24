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
package org.flowable.bpmn.converter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.converter.child.BaseChildElementParser;
import org.flowable.bpmn.converter.child.InParameterParser;
import org.flowable.bpmn.converter.child.VariableListenerEventDefinitionParser;
import org.flowable.bpmn.converter.util.BpmnXMLUtil;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.ExtensionElement;

/**
 * @author Tijs Rademakers
 */
public class BoundaryEventXMLConverter extends BaseBpmnXMLConverter {

    protected Map<String, BaseChildElementParser> childParserMap = new HashMap<>();

    public BoundaryEventXMLConverter() {
        InParameterParser inParameterParser = new InParameterParser();
        childParserMap.put(inParameterParser.getElementName(), inParameterParser);
        VariableListenerEventDefinitionParser variableListenerEventDefinitionParser = new VariableListenerEventDefinitionParser();
        childParserMap.put(variableListenerEventDefinitionParser.getElementName(), variableListenerEventDefinitionParser);
    }

    @Override
    public Class<? extends BaseElement> getBpmnElementType() {
        return BoundaryEvent.class;
    }

    @Override
    protected String getXMLElementName() {
        return ELEMENT_EVENT_BOUNDARY;
    }

    @Override
    protected BaseElement convertXMLToElement(XMLStreamReader xtr, BpmnModel model) throws Exception {
        BoundaryEvent boundaryEvent = new BoundaryEvent();
        BpmnXMLUtil.addXMLLocation(boundaryEvent, xtr);
        String elementId = xtr.getAttributeValue(null, ATTRIBUTE_ID);
        boundaryEvent.setId(elementId);
        
        if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, ATTRIBUTE_BOUNDARY_CANCELACTIVITY))) {
            String cancelActivity = xtr.getAttributeValue(null, ATTRIBUTE_BOUNDARY_CANCELACTIVITY);
            if (ATTRIBUTE_VALUE_FALSE.equalsIgnoreCase(cancelActivity)) {
                boundaryEvent.setCancelActivity(false);
            }
        }
        boundaryEvent.setAttachedToRefId(xtr.getAttributeValue(null, ATTRIBUTE_BOUNDARY_ATTACHEDTOREF));
        parseChildElements(getXMLElementName(), boundaryEvent, childParserMap, model, xtr);

        // Explicitly set cancel activity to false for error boundary events
        if (boundaryEvent.getEventDefinitions().size() == 1) {
            EventDefinition eventDef = boundaryEvent.getEventDefinitions().get(0);

            if (eventDef instanceof ErrorEventDefinition) {
                boundaryEvent.setCancelActivity(false);
            }
        }

        return boundaryEvent;
    }

    @Override
    protected void writeAdditionalAttributes(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception {
        BoundaryEvent boundaryEvent = (BoundaryEvent) element;
        if (boundaryEvent.getAttachedToRef() != null) {
            writeDefaultAttribute(ATTRIBUTE_BOUNDARY_ATTACHEDTOREF, boundaryEvent.getAttachedToRef().getId(), xtw);
        }

        if (boundaryEvent.getEventDefinitions().size() == 1) {
            EventDefinition eventDef = boundaryEvent.getEventDefinitions().get(0);

            if (!(eventDef instanceof ErrorEventDefinition)) {
                writeDefaultAttribute(ATTRIBUTE_BOUNDARY_CANCELACTIVITY, String.valueOf(boundaryEvent.isCancelActivity()).toLowerCase(), xtw);
            }

        } else if (!boundaryEvent.getExtensionElements().isEmpty()) {
            List<ExtensionElement> eventTypeExtensionElements = boundaryEvent.getExtensionElements().get(BpmnXMLConstants.ELEMENT_EVENT_TYPE);
            if (eventTypeExtensionElements != null && !eventTypeExtensionElements.isEmpty()) {
                String eventTypeValue = eventTypeExtensionElements.get(0).getElementText();
                if (StringUtils.isNotEmpty(eventTypeValue)) {
                    writeDefaultAttribute(ATTRIBUTE_BOUNDARY_CANCELACTIVITY, String.valueOf(boundaryEvent.isCancelActivity()).toLowerCase(), xtw);
                }
            }
        }
    }
    
    @Override
    protected boolean writeExtensionChildElements(BaseElement element, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
        BoundaryEvent boundaryEvent = (BoundaryEvent) element;
        didWriteExtensionStartElement = writeVariableListenerDefinition(boundaryEvent, didWriteExtensionStartElement, xtw);        
        return didWriteExtensionStartElement;
    }

    @Override
    protected void writeAdditionalChildElements(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception {
        BoundaryEvent boundaryEvent = (BoundaryEvent) element;
        writeEventDefinitions(boundaryEvent, boundaryEvent.getEventDefinitions(), model, xtw);
    }
}
