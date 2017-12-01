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
package org.flowable.bpmn.converter.child;

import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.converter.util.BpmnXMLUtil;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.SignalEventDefinition;

/**
 * @author Tijs Rademakers
 */
public class SignalEventDefinitionParser extends BaseChildElementParser {

    @Override
    public String getElementName() {
        return ELEMENT_EVENT_SIGNALDEFINITION;
    }

    @Override
    public void parseChildElement(XMLStreamReader xtr, BaseElement parentElement, BpmnModel model) throws Exception {
        if (!(parentElement instanceof Event))
            return;

        SignalEventDefinition eventDefinition = new SignalEventDefinition();
        BpmnXMLUtil.addXMLLocation(eventDefinition, xtr);
        eventDefinition.setSignalRef(xtr.getAttributeValue(null, ATTRIBUTE_SIGNAL_REF));
        eventDefinition.setSignalExpression(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_SIGNAL_EXPRESSION, xtr));
        if (StringUtils.isNotEmpty(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_ACTIVITY_ASYNCHRONOUS, xtr))) {
            eventDefinition.setAsync(Boolean.parseBoolean(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_ACTIVITY_ASYNCHRONOUS, xtr)));
        }

        BpmnXMLUtil.parseChildElements(ELEMENT_EVENT_SIGNALDEFINITION, eventDefinition, xtr, model);

        ((Event) parentElement).getEventDefinitions().add(eventDefinition);
    }
}
