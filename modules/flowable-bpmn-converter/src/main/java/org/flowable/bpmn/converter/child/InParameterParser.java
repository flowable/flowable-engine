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
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.CaseServiceTask;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.IOParameter;

public class InParameterParser extends BaseChildElementParser {

    @Override
    public String getElementName() {
        return ELEMENT_IN_PARAMETERS;
    }

    @Override
    public void parseChildElement(XMLStreamReader xtr, BaseElement parentElement, BpmnModel model) throws Exception {
        String source = xtr.getAttributeValue(null, ATTRIBUTE_IOPARAMETER_SOURCE);
        String sourceExpression = xtr.getAttributeValue(null, ATTRIBUTE_IOPARAMETER_SOURCE_EXPRESSION);
        String target = xtr.getAttributeValue(null, ATTRIBUTE_IOPARAMETER_TARGET);
        if ((StringUtils.isNotEmpty(source) || StringUtils.isNotEmpty(sourceExpression)) && StringUtils.isNotEmpty(target)) {

            IOParameter parameter = new IOParameter();
            if (StringUtils.isNotEmpty(sourceExpression)) {
                parameter.setSourceExpression(sourceExpression);
            } else {
                parameter.setSource(source);
            }

            parameter.setTarget(target);
            
            String transientValue = xtr.getAttributeValue(null, ATTRIBUTE_IOPARAMETER_TRANSIENT);
            if ("true".equalsIgnoreCase(transientValue)) {
                parameter.setTransient(true);
            }

            if (parentElement instanceof CallActivity) {
                ((CallActivity) parentElement).getInParameters().add(parameter);
            
            } else if (parentElement instanceof CaseServiceTask) {
                ((CaseServiceTask) parentElement).getInParameters().add(parameter);
            
            } else if (parentElement instanceof Event) {
                ((Event) parentElement).getInParameters().add(parameter);
            }
        }
    }
}