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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.model.ManualActivationRule;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.RepetitionRule;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class PlanItemExport implements CmmnXmlConstants {
    
    public static void writePlanItem(PlanItem planItem, XMLStreamWriter xtw) throws Exception {
        // start plan item element
        xtw.writeStartElement(ELEMENT_PLAN_ITEM);
        xtw.writeAttribute(ATTRIBUTE_ID, planItem.getId());

        if (StringUtils.isNotEmpty(planItem.getName())) {
            xtw.writeAttribute(ATTRIBUTE_NAME, planItem.getName());
        }
        
        if (StringUtils.isNotEmpty(planItem.getDefinitionRef())) {
            xtw.writeAttribute(ATTRIBUTE_DEFINITION_REF, planItem.getDefinitionRef());
        }

        if (StringUtils.isNotEmpty(planItem.getDocumentation())) {
            xtw.writeStartElement(ELEMENT_DOCUMENTATION);
            xtw.writeCharacters(planItem.getDocumentation());
            xtw.writeEndElement();
        }
        if (planItem.getItemControl() != null) {
            xtw.writeStartElement(ELEMENT_ITEM_CONTROL);
            writeRepetitionRule(planItem, xtw);
            writeManualActivationRule(planItem, xtw);
            xtw.writeEndElement();
        }
        
        CriteriaExport.writeCriteriaElements(planItem, xtw);
        
        // end plan item element
        xtw.writeEndElement();
    }

    public static void writeRepetitionRule(PlanItem planItem, XMLStreamWriter xtw) throws XMLStreamException {
        RepetitionRule repetitionRule = planItem.getItemControl().getRepetitionRule(); 
        if (repetitionRule != null) {
            xtw.writeStartElement(ELEMENT_REPETITION_RULE);
            if (StringUtils.isNotEmpty(repetitionRule.getRepetitionCounterVariableName())) {
                xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, 
                        ATTRIBUTE_REPETITION_COUNTER_VARIABLE_NAME, repetitionRule.getRepetitionCounterVariableName());
            }
            if (StringUtils.isNotEmpty(repetitionRule.getCondition())) {
                xtw.writeStartElement(ELEMENT_CONDITION);
                xtw.writeCData(repetitionRule.getCondition());
                xtw.writeEndElement();
            }
            xtw.writeEndElement();
        }
    }
    
    public static void writeManualActivationRule(PlanItem planItem, XMLStreamWriter xtw) throws XMLStreamException {
        ManualActivationRule manualActivationRule = planItem.getItemControl().getManualActivationRule();
        if (manualActivationRule != null) {
            xtw.writeStartElement(ELEMENT_MANUAL_ACTIVATION_RULE);
            if (StringUtils.isNotEmpty(manualActivationRule.getCondition())) {
                xtw.writeStartElement(ELEMENT_CONDITION);
                xtw.writeCData(manualActivationRule.getCondition());
                xtw.writeEndElement();
            }
            xtw.writeEndElement();
        }
    }
    
}
