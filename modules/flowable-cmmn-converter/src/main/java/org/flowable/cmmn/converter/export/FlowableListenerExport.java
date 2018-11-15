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
package org.flowable.cmmn.converter.export;

import java.util.List;

import javax.xml.stream.XMLStreamWriter;

import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.converter.util.CmmnXmlUtil;
import org.flowable.cmmn.model.FlowableListener;
import org.flowable.cmmn.model.ImplementationType;

/**
 * @author Joram Barrez
 */
public class FlowableListenerExport {

    public static boolean writeFlowableListeners(XMLStreamWriter xtw, String xmlElementName, List<FlowableListener> listeners, boolean didWriteExtensionStartElement) throws Exception {
        if (listeners != null) {

            for (FlowableListener listener : listeners) {

                if (!didWriteExtensionStartElement) {
                    xtw.writeStartElement(CmmnXmlConstants.ELEMENT_EXTENSION_ELEMENTS);
                    didWriteExtensionStartElement = true;
                }

                xtw.writeStartElement(CmmnXmlConstants.FLOWABLE_EXTENSIONS_PREFIX, xmlElementName, CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE);
                CmmnXmlUtil.writeDefaultAttribute(CmmnXmlConstants.ATTRIBUTE_LISTENER_EVENT, listener.getEvent(), xtw);
                CmmnXmlUtil.writeDefaultAttribute(CmmnXmlConstants.ATTRIBUTE_LISTENER_SOURCE_STATE, listener.getSourceState(), xtw);
                CmmnXmlUtil.writeDefaultAttribute(CmmnXmlConstants.ATTRIBUTE_LISTENER_TARGET_STATE, listener.getTargetState(), xtw);

                if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equals(listener.getImplementationType())) {
                    CmmnXmlUtil.writeDefaultAttribute(CmmnXmlConstants.ATTRIBUTE_LISTENER_CLASS, listener.getImplementation(), xtw);
                } else if (ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION.equals(listener.getImplementationType())) {
                    CmmnXmlUtil.writeDefaultAttribute(CmmnXmlConstants.ATTRIBUTE_LISTENER_EXPRESSION, listener.getImplementation(), xtw);
                } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equals(listener.getImplementationType())) {
                    CmmnXmlUtil.writeDefaultAttribute(CmmnXmlConstants.ATTRIBUTE_LISTENER_DELEGATEEXPRESSION, listener.getImplementation(), xtw);
                }

                CmmnXmlUtil.writeDefaultAttribute(CmmnXmlConstants.ATTRIBUTE_LISTENER_ON_TRANSACTION, listener.getOnTransaction(), xtw);

                FieldExport.writeFieldExtensions(listener.getFieldExtensions(), true, xtw);

                xtw.writeEndElement();

            }
        }
        return didWriteExtensionStartElement;
    }

}
