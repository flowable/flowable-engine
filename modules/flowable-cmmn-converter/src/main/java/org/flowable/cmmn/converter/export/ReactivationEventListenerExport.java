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
import org.flowable.cmmn.model.ReactivateEventListener;
import org.flowable.cmmn.model.ReactivationRule;

/**
 * Exports a reactivation event listener and all of its attributes and elements.
 *
 * @author Micha Kiener
 */
public class ReactivationEventListenerExport extends AbstractPlanItemDefinitionExport<ReactivateEventListener> {

    @Override
    protected Class<? extends ReactivateEventListener> getExportablePlanItemDefinitionClass() {
        return ReactivateEventListener.class;
    }

    @Override
    protected String getPlanItemDefinitionXmlElementValue(ReactivateEventListener reactivationEventListener) {
        return ELEMENT_GENERIC_EVENT_LISTENER;
    }

    @Override
    protected void writePlanItemDefinitionSpecificAttributes(ReactivateEventListener reactivationEventListener, XMLStreamWriter xtw) throws Exception {
        super.writePlanItemDefinitionSpecificAttributes(reactivationEventListener, xtw);

        xtw.writeAttribute(FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_EVENT_LISTENER_TYPE, "reactivate");

        ReactivationRule reactivationRule = reactivationEventListener.getDefaultReactivationRule();
        if (reactivationRule != null) {
            xtw.writeStartElement(ELEMENT_EXTENSION_ELEMENTS);
            xtw.writeStartElement(FLOWABLE_EXTENSIONS_PREFIX, ELEMENT_DEFAULT_REACTIVATION_RULE, FLOWABLE_EXTENSIONS_NAMESPACE);
            PlanItemControlExport.writeReactivationRuleAttributes(reactivationRule, xtw);
            xtw.writeEndElement();
            xtw.writeEndElement();
        }

        if (StringUtils.isNotEmpty(reactivationEventListener.getAvailableConditionExpression())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_NAMESPACE,
                CmmnXmlConstants.ATTRIBUTE_EVENT_LISTENER_AVAILABLE_CONDITION,
                reactivationEventListener.getAvailableConditionExpression());
        }
    }
}
