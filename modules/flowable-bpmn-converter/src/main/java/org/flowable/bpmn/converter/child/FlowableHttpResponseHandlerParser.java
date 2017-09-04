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
import org.flowable.bpmn.model.FlowableHttpResponseHandler;
import org.flowable.bpmn.model.HttpServiceTask;
import org.flowable.bpmn.model.ImplementationType;

/**
 * @author Tijs Rademakers
 */
public class FlowableHttpResponseHandlerParser extends BaseChildElementParser {

    @Override
    public void parseChildElement(XMLStreamReader xtr, BaseElement parentElement, BpmnModel model) throws Exception {

        FlowableHttpResponseHandler responseHandler = new FlowableHttpResponseHandler();
        BpmnXMLUtil.addXMLLocation(responseHandler, xtr);
        if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_CLASS))) {
            responseHandler.setImplementation(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_CLASS));
            responseHandler.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
            
        } else if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_DELEGATEEXPRESSION))) {
            responseHandler.setImplementation(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_DELEGATEEXPRESSION));
            responseHandler.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
        }
        
        if (parentElement instanceof HttpServiceTask) {
            ((HttpServiceTask) parentElement).setHttpResponseHandler(responseHandler);
            parseChildElements(xtr, responseHandler, model, new FieldExtensionParser());
        }
    }

    @Override
    public String getElementName() {
        return ELEMENT_HTTP_RESPONSE_HANDLER;
    }
}
