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

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamReader;

import org.flowable.bpmn.converter.util.BpmnXMLUtil;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;

/**
 * @author Tijs Rademakers
 */
public class MultiInstanceParser extends BaseChildElementParser {

    @Override
    public String getElementName() {
        return ELEMENT_MULTIINSTANCE;
    }

    @Override
    public void parseChildElement(XMLStreamReader xtr, BaseElement parentElement, BpmnModel model) throws Exception {
        if (!(parentElement instanceof Activity)) {
            return;
        }

        MultiInstanceLoopCharacteristics multiInstanceDef = new MultiInstanceLoopCharacteristics();
        BpmnXMLUtil.addXMLLocation(multiInstanceDef, xtr);
        if (xtr.getAttributeValue(null, ATTRIBUTE_MULTIINSTANCE_SEQUENTIAL) != null) {
            multiInstanceDef.setSequential(Boolean.valueOf(xtr.getAttributeValue(null, ATTRIBUTE_MULTIINSTANCE_SEQUENTIAL)));
        }
        if (xtr.getAttributeValue(FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_MULTIINSTANCE_NO_WAIT_STATES_ASYNC_LEAVE) != null) {
            multiInstanceDef.setNoWaitStatesAsyncLeave(Boolean.valueOf(xtr.getAttributeValue(FLOWABLE_EXTENSIONS_NAMESPACE,
                ATTRIBUTE_MULTIINSTANCE_NO_WAIT_STATES_ASYNC_LEAVE)));
        }
        multiInstanceDef.setInputDataItem(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_MULTIINSTANCE_COLLECTION, xtr));
        multiInstanceDef.setElementVariable(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_MULTIINSTANCE_VARIABLE, xtr));
        multiInstanceDef.setElementIndexVariable(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_MULTIINSTANCE_INDEX_VARIABLE, xtr));

        boolean readyWithMultiInstance = false;
        try {
            while (!readyWithMultiInstance && xtr.hasNext()) {
                xtr.next();
                if (xtr.isStartElement() && ELEMENT_MULTIINSTANCE_CARDINALITY.equalsIgnoreCase(xtr.getLocalName())) {
                    multiInstanceDef.setLoopCardinality(xtr.getElementText());

                } else if (xtr.isStartElement() && ELEMENT_MULTIINSTANCE_DATAINPUT.equalsIgnoreCase(xtr.getLocalName())) {
                    multiInstanceDef.setInputDataItem(xtr.getElementText());

                } else if (xtr.isStartElement() && ELEMENT_MULTIINSTANCE_DATAITEM.equalsIgnoreCase(xtr.getLocalName())) {
                    if (xtr.getAttributeValue(null, ATTRIBUTE_NAME) != null) {
                        multiInstanceDef.setElementVariable(xtr.getAttributeValue(null, ATTRIBUTE_NAME));
                    }

                } else if (xtr.isStartElement() && ELEMENT_MULTIINSTANCE_CONDITION.equalsIgnoreCase(xtr.getLocalName())) {
                    multiInstanceDef.setCompletionCondition(xtr.getElementText());

                } else if (xtr.isStartElement() && ELEMENT_EXTENSIONS.equalsIgnoreCase(xtr.getLocalName())) {
                    // parse extension elements
                    // initialize collection element parser in case it exists
                    Map<String, BaseChildElementParser> childParserMap = new HashMap<>();
                    childParserMap.put(ELEMENT_MULTIINSTANCE_COLLECTION, new FlowableCollectionParser());
                    childParserMap.put(ELEMENT_VARIABLE_AGGREGATION, new VariableAggregationDefinitionParser());
                    BpmnXMLUtil.parseChildElements(ELEMENT_EXTENSIONS, multiInstanceDef, xtr, childParserMap, model);

                } else if (xtr.isEndElement() && getElementName().equalsIgnoreCase(xtr.getLocalName())) {
                    readyWithMultiInstance = true;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error parsing multi instance definition", e);
        }

        ((Activity) parentElement).setLoopCharacteristics(multiInstanceDef);
    }
}
