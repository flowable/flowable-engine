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
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.ImplementationType;

/**
 * @author Tijs Rademakers
 * @author Yvo Swillens
 */
public abstract class FlowableListenerParser extends BaseChildElementParser {

    @Override
    public void parseChildElement(XMLStreamReader xtr, BaseElement parentElement, BpmnModel model) throws Exception {

        FlowableListener listener = new FlowableListener();
        BpmnXMLUtil.addXMLLocation(listener, xtr);
        if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_CLASS))) {
            listener.setImplementation(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_CLASS));
            listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
        } else if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_EXPRESSION))) {
            listener.setImplementation(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_EXPRESSION));
            listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION);
        } else if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_DELEGATEEXPRESSION))) {
            listener.setImplementation(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_DELEGATEEXPRESSION));
            listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
        }
        listener.setEvent(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_EVENT));
        listener.setOnTransaction(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_ON_TRANSACTION));

        if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_CUSTOM_PROPERTIES_RESOLVER_CLASS))) {
            listener.setCustomPropertiesResolverImplementation(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_CUSTOM_PROPERTIES_RESOLVER_CLASS));
            listener.setCustomPropertiesResolverImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
        } else if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_CUSTOM_PROPERTIES_RESOLVER_EXPRESSION))) {
            listener.setCustomPropertiesResolverImplementation(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_CUSTOM_PROPERTIES_RESOLVER_EXPRESSION));
            listener.setCustomPropertiesResolverImplementationType(ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION);
        } else if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_CUSTOM_PROPERTIES_RESOLVER_DELEGATEEXPRESSION))) {
            listener.setCustomPropertiesResolverImplementation(xtr.getAttributeValue(null, ATTRIBUTE_LISTENER_CUSTOM_PROPERTIES_RESOLVER_DELEGATEEXPRESSION));
            listener.setCustomPropertiesResolverImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
        }
        addListenerToParent(listener, parentElement);
        parseChildElements(xtr, listener, model, new FieldExtensionParser());
    }

    public abstract void addListenerToParent(FlowableListener listener, BaseElement parentElement);
}
