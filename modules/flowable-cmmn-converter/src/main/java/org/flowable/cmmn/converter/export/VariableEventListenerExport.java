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
import org.flowable.cmmn.model.VariableEventListener;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class VariableEventListenerExport extends AbstractPlanItemDefinitionExport<VariableEventListener> {

    @Override
    protected Class<? extends VariableEventListener> getExportablePlanItemDefinitionClass() {
        return VariableEventListener.class;
    }

    @Override
    protected String getPlanItemDefinitionXmlElementValue(VariableEventListener variableEventListener) {
        return ELEMENT_GENERIC_EVENT_LISTENER;
    }

    @Override
    protected void writePlanItemDefinitionSpecificAttributes(VariableEventListener variableEventListener, XMLStreamWriter xtw) throws Exception {
        super.writePlanItemDefinitionSpecificAttributes(variableEventListener, xtw);
        
        xtw.writeAttribute(FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_EVENT_LISTENER_TYPE, "variable");
        
        if (StringUtils.isNotEmpty(variableEventListener.getVariableName())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_EVENT_LISTENER_VARIABLE_NAME, variableEventListener.getVariableName());
        }
        
        if (StringUtils.isNotEmpty(variableEventListener.getVariableChangeType())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_EVENT_LISTENER_VARIABLE_CHANGE_TYPE, variableEventListener.getVariableChangeType());
        }

        if (StringUtils.isNotEmpty(variableEventListener.getAvailableConditionExpression())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_NAMESPACE,
                CmmnXmlConstants.ATTRIBUTE_EVENT_LISTENER_AVAILABLE_CONDITION,
                variableEventListener.getAvailableConditionExpression());
        }
    }

}
