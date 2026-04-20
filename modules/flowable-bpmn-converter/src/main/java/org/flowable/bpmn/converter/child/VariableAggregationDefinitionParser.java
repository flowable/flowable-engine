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
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;
import org.flowable.bpmn.model.VariableAggregationDefinition;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class VariableAggregationDefinitionParser extends BaseChildElementParser {

    @Override
    public void parseChildElement(XMLStreamReader xtr, BaseElement parentElement, BpmnModel model) throws Exception {
        if (!(parentElement instanceof MultiInstanceLoopCharacteristics multiInstanceLoopCharacteristics)) {
            return;
        }

        VariableAggregationDefinition aggregationDefinition = new VariableAggregationDefinition();

        if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, ATTRIBUTE_TASK_SERVICE_CLASS))) {
            aggregationDefinition.setImplementation(xtr.getAttributeValue(null, ATTRIBUTE_TASK_SERVICE_CLASS));
            aggregationDefinition.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);

        } else if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, ATTRIBUTE_TASK_SERVICE_DELEGATEEXPRESSION))) {
            aggregationDefinition.setImplementation(xtr.getAttributeValue(null, ATTRIBUTE_TASK_SERVICE_DELEGATEEXPRESSION));
            aggregationDefinition.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
        }

        aggregationDefinition.setTarget(xtr.getAttributeValue(null, ATTRIBUTE_IOPARAMETER_TARGET));
        aggregationDefinition.setTargetExpression(xtr.getAttributeValue(null, ATTRIBUTE_IOPARAMETER_TARGET_EXPRESSION));
        aggregationDefinition.setStoreAsTransientVariable(Boolean.parseBoolean(xtr.getAttributeValue(null, ATTRIBUTE_VARIABLE_AGGREGATION_STORE_AS_TRANSIENT_VARIABLE)));
        aggregationDefinition.setCreateOverviewVariable(Boolean.parseBoolean(xtr.getAttributeValue(null, ATTRIBUTE_VARIABLE_AGGREGATION_CREATE_OVERVIEW)));

        multiInstanceLoopCharacteristics.addAggregation(aggregationDefinition);

        boolean readyWithAggregation = false;
        try {
            while (!readyWithAggregation && xtr.hasNext()) {
                xtr.next();
                if (xtr.isStartElement() && ATTRIBUTE_VARIABLE_AGGREGATION_VARIABLE.equalsIgnoreCase(xtr.getLocalName())) {
                    VariableAggregationDefinition.Variable definition = new VariableAggregationDefinition.Variable();

                    definition.setSource(xtr.getAttributeValue(null, ATTRIBUTE_IOPARAMETER_SOURCE));
                    definition.setSourceExpression(xtr.getAttributeValue(null, ATTRIBUTE_IOPARAMETER_SOURCE_EXPRESSION));
                    definition.setTarget(xtr.getAttributeValue(null, ATTRIBUTE_IOPARAMETER_TARGET));
                    definition.setTargetExpression(xtr.getAttributeValue(null, ATTRIBUTE_IOPARAMETER_TARGET_EXPRESSION));

                    aggregationDefinition.addDefinition(definition);
                } else if (xtr.isEndElement() && getElementName().equalsIgnoreCase(xtr.getLocalName())) {
                    readyWithAggregation = true;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error parsing collection child elements", e);
        }
    }

    @Override
    public String getElementName() {
        return ELEMENT_VARIABLE_AGGREGATION;
    }

}
