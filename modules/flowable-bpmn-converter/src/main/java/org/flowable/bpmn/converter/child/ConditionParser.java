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

import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ConditionalEventDefinition;

/**
 * @author Tijs Rademakers
 */
public class ConditionParser extends BaseChildElementParser {

    @Override
    public String getElementName() {
        return ELEMENT_CONDITION;
    }

    @Override
    public void parseChildElement(XMLStreamReader xtr, BaseElement parentElement, BpmnModel model) throws Exception {
        if (!(parentElement instanceof ConditionalEventDefinition conditionalEventDefinition)) {
            return;
        }

        conditionalEventDefinition.setConditionLanguage(xtr.getAttributeValue(null, ATTRIBUTE_SCRIPT_LANGUAGE));
        conditionalEventDefinition.setConditionExpression(xtr.getElementText().trim());
    }

    @Override
    public boolean accepts(BaseElement element) {
        return element instanceof ConditionalEventDefinition;
    }
}
