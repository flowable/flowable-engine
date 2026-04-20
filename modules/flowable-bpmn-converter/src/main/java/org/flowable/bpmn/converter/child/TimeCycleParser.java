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
import org.flowable.bpmn.model.TimerEventDefinition;

/**
 * @author Tijs Rademakers
 */
public class TimeCycleParser extends BaseChildElementParser {

    @Override
    public String getElementName() {
        return ATTRIBUTE_TIMER_CYCLE;
    }

    @Override
    public void parseChildElement(XMLStreamReader xtr, BaseElement parentElement, BpmnModel model) throws Exception {
        if (!(parentElement instanceof TimerEventDefinition eventDefinition)) {
            return;
        }

        if (StringUtils.isNotEmpty(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_END_DATE, xtr))) {
            eventDefinition.setEndDate(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_END_DATE, xtr));
        }
        eventDefinition.setTimeCycle(xtr.getElementText());
    }
}
