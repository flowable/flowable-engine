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
package org.flowable.bpmn.converter.parser;

import java.util.ArrayList;

import javax.xml.stream.XMLStreamReader;

import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.converter.child.BaseChildElementParser;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.VariableAggregationDefinition;

/**
 * @author Joram Barrez
 */
public class VariableAggregationDefinitionParser extends BaseChildElementParser {

    @Override
    public String getElementName() {
        return BpmnXMLConstants.ELEMENT_VARIABLE_AGGREGATION;
    }

    @Override
    public void parseChildElement(XMLStreamReader xtr, BaseElement parentElement, BpmnModel model) throws Exception {
        if (!(parentElement instanceof FlowNode)) {
            return;
        }

        FlowNode flowNode = (FlowNode) parentElement;
        VariableAggregationDefinition variableAggregationDefinition = new VariableAggregationDefinition();

        if (flowNode.getVariableAggregationDefinitions() == null) {
            flowNode.setVariableAggregationDefinitions(new ArrayList<>());
        }
        flowNode.getVariableAggregationDefinitions().add(variableAggregationDefinition);

        variableAggregationDefinition.setTargetArrayVariable(xtr.getAttributeValue(null, BpmnXMLConstants.ATTRIBUTE_TARGET_ARRAY_VARIABLE));
        variableAggregationDefinition.setTargetArrayVariableExpression(xtr.getAttributeValue(null, BpmnXMLConstants.ATTRIBUTE_TARGET_ARRAY_VARIABLE_EXPRESSION));
        variableAggregationDefinition.setSource(xtr.getAttributeValue(null, BpmnXMLConstants.ATTRIBUTE_IOPARAMETER_SOURCE));
        variableAggregationDefinition.setSourceExpression(xtr.getAttributeValue(null, BpmnXMLConstants.ATTRIBUTE_IOPARAMETER_SOURCE_EXPRESSION));
        variableAggregationDefinition.setTarget(xtr.getAttributeValue(null, BpmnXMLConstants.ATTRIBUTE_IOPARAMETER_TARGET));
        variableAggregationDefinition.setTargetExpression(xtr.getAttributeValue(null, BpmnXMLConstants.ATTRIBUTE_IOPARAMETER_TARGET_EXPRESSION));
    }

}
