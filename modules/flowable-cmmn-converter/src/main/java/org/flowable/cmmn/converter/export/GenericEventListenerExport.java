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

import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.GenericEventListener;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class GenericEventListenerExport extends AbstractPlanItemDefinitionExport<GenericEventListener> {

    @Override
    protected Class<? extends GenericEventListener> getExportablePlanItemDefinitionClass() {
        return GenericEventListener.class;
    }

    @Override
    protected String getPlanItemDefinitionXmlElementValue(GenericEventListener planItemDefinition) {
        return ELEMENT_GENERIC_EVENT_LISTENER;
    }

    @Override
    protected void writePlanItemDefinitionSpecificAttributes(GenericEventListener genericEventListener, XMLStreamWriter xtw) throws Exception {
        super.writePlanItemDefinitionSpecificAttributes(genericEventListener, xtw);

        if (StringUtils.isNotEmpty(genericEventListener.getAvailableConditionExpression())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_NAMESPACE,
                CmmnXmlConstants.ATTRIBUTE_EVENT_LISTENER_AVAILABLE_CONDITION,
                genericEventListener.getAvailableConditionExpression());
        }
        
        if (StringUtils.isNotEmpty(genericEventListener.getEventType()) && genericEventListener.getExtensionElements().get("eventType") == null) {
            ExtensionElement extensionElement = new ExtensionElement();
            extensionElement.setNamespace(FLOWABLE_EXTENSIONS_NAMESPACE);
            extensionElement.setNamespacePrefix(FLOWABLE_EXTENSIONS_PREFIX);
            extensionElement.setName("eventType");
            extensionElement.setElementText(genericEventListener.getEventType());
            genericEventListener.addExtensionElement(extensionElement);
        }
    }

}
