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
import org.flowable.bpmn.model.FlowableCollectionHandler;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.MultiInstanceLoopCharacteristics;

/**
 * @author Lori Small
 */
public class FlowableCollectionParser extends BaseChildElementParser {

    public void parseChildElement(XMLStreamReader xtr, BaseElement parentElement, BpmnModel model) throws Exception {

        boolean hasCollectionHandler = false;
    	// if a collection handler is specified
    	FlowableCollectionHandler collectionHandler = new FlowableCollectionHandler();
        BpmnXMLUtil.addXMLLocation(collectionHandler, xtr);

        if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, ATTRIBUTE_MULTIINSTANCE_COLLECTION_CLASS))) {
        	collectionHandler.setImplementation(xtr.getAttributeValue(null, ATTRIBUTE_MULTIINSTANCE_COLLECTION_CLASS));
        	collectionHandler.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
        	hasCollectionHandler = true;
        } else if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, ATTRIBUTE_MULTIINSTANCE_COLLECTION_DELEGATEEXPRESSION))) {
        	collectionHandler.setImplementation(xtr.getAttributeValue(null, ATTRIBUTE_MULTIINSTANCE_COLLECTION_DELEGATEEXPRESSION));
        	collectionHandler.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
        	hasCollectionHandler = true;
        }

        if (hasCollectionHandler)  {
        	((MultiInstanceLoopCharacteristics) parentElement).setHandler(collectionHandler);
        }

        boolean readyWithCollection = false;
        try {
            while (!readyWithCollection && xtr.hasNext()) {
                xtr.next();
                if (xtr.isStartElement() && ELEMENT_MULTIINSTANCE_COLLECTION_STRING.equalsIgnoreCase(xtr.getLocalName())) {
            		// it is a string value
            		((MultiInstanceLoopCharacteristics) parentElement).setCollectionString(xtr.getElementText());
                } else if (xtr.isStartElement() && ELEMENT_MULTIINSTANCE_COLLECTION_EXPRESSION.equalsIgnoreCase(xtr.getLocalName())) {
            		// it is an expression
                	((MultiInstanceLoopCharacteristics) parentElement).setInputDataItem(xtr.getElementText());
                } else if (xtr.isEndElement() && getElementName().equalsIgnoreCase(xtr.getLocalName())) {
                	readyWithCollection = true;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error parsing collection child elements", e);
        }
    }

    @Override
    public String getElementName() {
        return ELEMENT_MULTIINSTANCE_COLLECTION;
    }
}
