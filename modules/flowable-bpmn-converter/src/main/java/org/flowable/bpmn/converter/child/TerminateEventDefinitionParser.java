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

import org.flowable.bpmn.converter.util.BpmnXMLUtil;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.TerminateEventDefinition;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class TerminateEventDefinitionParser extends BaseChildElementParser {

    @Override
    public String getElementName() {
        return ELEMENT_EVENT_TERMINATEDEFINITION;
    }

    @Override
    public void parseChildElement(XMLStreamReader xtr, BaseElement parentElement, BpmnModel model) throws Exception {
        if (!(parentElement instanceof EndEvent)) {
            return;
        }

        TerminateEventDefinition eventDefinition = new TerminateEventDefinition();

        parseTerminateAllAttribute(xtr, eventDefinition);
        parseTerminateMultiInstanceAttribute(xtr, eventDefinition);

        BpmnXMLUtil.addXMLLocation(eventDefinition, xtr);
        BpmnXMLUtil.parseChildElements(ELEMENT_EVENT_TERMINATEDEFINITION, eventDefinition, xtr, model);

        ((Event) parentElement).getEventDefinitions().add(eventDefinition);
    }

    protected void parseTerminateAllAttribute(XMLStreamReader xtr, TerminateEventDefinition eventDefinition) {
        String terminateAllValue = BpmnXMLUtil.getAttributeValue(ATTRIBUTE_TERMINATE_ALL, xtr);
        if ("true".equals(terminateAllValue)) {
            eventDefinition.setTerminateAll(true);
        } else {
            eventDefinition.setTerminateAll(false);
        }
    }

    protected void parseTerminateMultiInstanceAttribute(XMLStreamReader xtr, TerminateEventDefinition eventDefinition) {
        String terminateMiValue = BpmnXMLUtil.getAttributeValue(ATTRIBUTE_TERMINATE_MULTI_INSTANCE, xtr);
        if ("true".equals(terminateMiValue)) {
            eventDefinition.setTerminateMultiInstance(true);
        } else {
            eventDefinition.setTerminateMultiInstance(false);
        }
    }
}
