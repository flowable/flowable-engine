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
package org.flowable.bpmn.converter;

import java.util.Arrays;
import java.util.List;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.flowable.bpmn.converter.util.BpmnXMLUtil;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExclusiveGateway;
import org.flowable.bpmn.model.ExtensionAttribute;

/**
 * @author Tijs Rademakers
 */
public class ExclusiveGatewayXMLConverter extends BaseBpmnXMLConverter {

    /** default attributes taken from bpmn spec and from extension namespace */
    protected static final List<ExtensionAttribute> defaultExclusiveGatewayAttributes = Arrays.asList(
            new ExtensionAttribute(ATTRIBUTE_GATEWAY_EXCLUSIVE_ISMARKERVISIBLE));

    @Override
    public Class<? extends BaseElement> getBpmnElementType() {
        return ExclusiveGateway.class;
    }

    @Override
    protected String getXMLElementName() {
        return ELEMENT_GATEWAY_EXCLUSIVE;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected BaseElement convertXMLToElement(XMLStreamReader xtr, BpmnModel model) throws Exception {
        ExclusiveGateway gateway = new ExclusiveGateway();
        BpmnXMLUtil.addXMLLocation(gateway, xtr);
        
        gateway.setMarkerVisible(Boolean.valueOf(BpmnXMLUtil.getAttributeValue(ATTRIBUTE_GATEWAY_EXCLUSIVE_ISMARKERVISIBLE, xtr)));

        BpmnXMLUtil.addCustomAttributes(xtr, gateway, defaultElementAttributes, defaultActivityAttributes, defaultExclusiveGatewayAttributes);
        
        parseChildElements(getXMLElementName(), gateway, model, xtr);
        return gateway;
    }

    @Override
    protected void writeAdditionalAttributes(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception {
        ExclusiveGateway gateway = (ExclusiveGateway) element;
        if (gateway.isMarkerVisible()) {
            writeQualifiedAttribute(ATTRIBUTE_GATEWAY_EXCLUSIVE_ISMARKERVISIBLE, "true", xtw);
        }
    }

    @Override
    protected void writeAdditionalChildElements(BaseElement element, BpmnModel model, XMLStreamWriter xtw) throws Exception {

    }
}
