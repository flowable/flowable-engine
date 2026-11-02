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
import org.flowable.bpmn.model.VariableListenerEventDefinition;
import org.flowable.common.engine.api.FlowableException;

/**
 * @author Tijs Rademakers
 */
public class VariableListenerEventDefinitionParser extends BaseChildElementParser {

    @Override
    public String getElementName() {
        return ELEMENT_EVENT_VARIABLELISTENERDEFINITION;
    }

    @Override
    public void parseChildElement(XMLStreamReader xtr, BaseElement parentElement, BpmnModel model) throws Exception {
        if (!(parentElement instanceof Event event)) {
            throw new FlowableException("variableListenerEventDefinition is only supported for events, not for activity id " + parentElement.getId());
        }
        
        VariableListenerEventDefinition eventDefinition = new VariableListenerEventDefinition();
        BpmnXMLUtil.addXMLLocation(eventDefinition, xtr);
        
        String variableName = xtr.getAttributeValue(null, ATTRIBUTE_VARIABLE_NAME);
        if (StringUtils.isEmpty(variableName)) {
            LOGGER.warn("variable name is required for variable listener with activity id {}", parentElement.getId());
        }
        eventDefinition.setVariableName(variableName);
        
        String variableChangeType = xtr.getAttributeValue(null, ATTRIBUTE_VARIABLE_CHANGE_TYPE);
        eventDefinition.setVariableChangeType(variableChangeType);

        event.addEventDefinition(eventDefinition);
        
        model.addActivityIdForVariableListenerName(variableName, parentElement.getId());
    }
}
