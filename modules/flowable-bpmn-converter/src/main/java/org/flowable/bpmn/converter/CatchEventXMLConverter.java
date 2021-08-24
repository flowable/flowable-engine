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
import java.util.Map;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.flowable.bpmn.converter.child.BaseChildElementParser;
import org.flowable.bpmn.converter.child.InParameterParser;
import org.flowable.bpmn.converter.child.VariableListenerEventDefinitionParser;
import org.flowable.bpmn.converter.util.BpmnXMLUtil;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.IntermediateCatchEvent;

/**
 * @author Tijs Rademakers
 */
public class CatchEventXMLConverter extends BaseBpmnXMLConverter {
    
    protected Map<String, BaseChildElementParser> childParserMap = new HashMap<>();

    public CatchEventXMLConverter() {
        InParameterParser inParameterParser = new InParameterParser();
        childParserMap.put(inParameterParser.getElementName(), inParameterParser);
        VariableListenerEventDefinitionParser variableListenerEventDefinitionParser = new VariableListenerEventDefinitionParser();
        childParserMap.put(variableListenerEventDefinitionParser.getElementName(), variableListenerEventDefinitionParser);
    }

    @Override
    public Class<? extends BaseElement> getBpmnElementType() {
        return IntermediateCatchEvent.class;
    }

    @Override
    protected String getXMLElementName() {
        return ELEMENT_EVENT_CATCH;
    }

    @Override
    protected BaseElement convertXMLToElement(XMLStreamReader xtr, BpmnModel model) throws Exception {
        IntermediateCatchEvent catchEvent = new IntermediateCatchEvent();
        BpmnXMLUtil.addXMLLocation(catchEvent, xtr);
        String elementId = xtr.getAttributeValue(null, ATTRIBUTE_ID);
        catchEvent.setId(elementId);
        parseChildElements(getXMLElementName(), catchEvent, childParserMap, model, xtr);
        return catchEvent;
    }

    @Override
    protected void writeAdditionalAttributes(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception {

    }
    
    @Override
    protected boolean writeExtensionChildElements(BaseElement element, boolean didWriteExtensionStartElement, XMLStreamWriter xtw) throws Exception {
        IntermediateCatchEvent catchEvent = (IntermediateCatchEvent) element;
        didWriteExtensionStartElement = writeVariableListenerDefinition(catchEvent, didWriteExtensionStartElement, xtw);        
        return didWriteExtensionStartElement;
    }

    @Override
    protected void writeAdditionalChildElements(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception {
        IntermediateCatchEvent catchEvent = (IntermediateCatchEvent) element;
        writeEventDefinitions(catchEvent, catchEvent.getEventDefinitions(), model, xtw);
    }
}
