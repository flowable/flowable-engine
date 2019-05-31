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
package org.flowable.cmmn.converter;

import javax.xml.stream.XMLStreamReader;

import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.EventListener;
import org.flowable.cmmn.model.GenericEventListener;
import org.flowable.cmmn.model.SignalEventListener;

/**
 * @author Tijs Rademakers
 */
public class GenericEventListenerXmlConverter extends PlanItemDefinitionXmlConverter {

    @Override
    public boolean hasChildElements() {
        return true;
    }

    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_GENERIC_EVENT_LISTENER;
    }

    @Override
    protected BaseElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        String eventType = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_EVENT_LISTENER_TYPE);
        if ("signal".equals(eventType)) {
            SignalEventListener signalEventListener = new SignalEventListener();
            signalEventListener.setSignalRef(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_EVENT_LISTENER_SIGNAL_REF));
            return convertCommonAttributes(xtr, signalEventListener);
        } else {
            return convertCommonAttributes(xtr, new GenericEventListener());
        }
    }

    protected EventListener convertCommonAttributes(XMLStreamReader xtr, EventListener listener) {
        listener.setName(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_NAME));
        listener.setAvailableConditionExpression(xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE,
            CmmnXmlConstants.ATTRIBUTE_EVENT_LISTENER_AVAILABLE_CONDITION));
        return listener;
    }
}
