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
import org.flowable.cmmn.converter.util.CmmnXmlUtil;
import org.flowable.cmmn.model.CompletionNeutralRule;
import org.flowable.cmmn.model.ManualActivationRule;
import org.flowable.cmmn.model.ParentCompletionRule;
import org.flowable.cmmn.model.PlanItemControl;
import org.flowable.cmmn.model.ReactivationRule;
import org.flowable.cmmn.model.RepetitionRule;
import org.flowable.cmmn.model.RequiredRule;
import org.flowable.cmmn.model.VariableAggregationDefinition;
import org.flowable.cmmn.model.VariableAggregationDefinitions;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 * @author Micha Kiener
 * @author Filip Hrisafov
 */
public class PlanItemControlExport implements CmmnXmlConstants {

    public static void writeItemControl(PlanItemControl planItemControl, XMLStreamWriter xtw) throws Exception {
        xtw.writeStartElement(ELEMENT_ITEM_CONTROL);
        writeItemControlContent(planItemControl, xtw);
        xtw.writeEndElement();
    }

    public static void writeDefaultControl(PlanItemControl planItemControl, XMLStreamWriter xtw) throws Exception {
        xtw.writeStartElement(ELEMENT_DEFAULT_CONTROL);
        writeItemControlContent(planItemControl, xtw);
        xtw.writeEndElement();
    }

    protected static void writeItemControlContent(PlanItemControl planItemControl, XMLStreamWriter xtw) throws Exception {
        boolean hasWrittenExtensionElements = writeCompletionNeutralRule(planItemControl.getCompletionNeutralRule(), xtw);
        hasWrittenExtensionElements = writeParentCompletionRule(planItemControl.getParentCompletionRule(), hasWrittenExtensionElements, xtw);
        hasWrittenExtensionElements = writeReactivationRule(planItemControl.getReactivationRule(), hasWrittenExtensionElements, xtw);
        if (hasWrittenExtensionElements) {
            xtw.writeEndElement();
        }
        
        writeRepetitionRule(planItemControl.getRepetitionRule(), xtw);
        writeRequiredRule(planItemControl.getRequiredRule(), xtw);
        writeManualActivationRule(planItemControl.getManualActivationRule(), xtw);
    }

    public static void writeRequiredRule(RequiredRule requiredRule, XMLStreamWriter xtw) throws XMLStreamException {
        if (requiredRule != null) {
            xtw.writeStartElement(ELEMENT_REQUIRED_RULE);
            if (StringUtils.isNotEmpty(requiredRule.getCondition())) {
                xtw.writeStartElement(ELEMENT_CONDITION);
                xtw.writeCData(requiredRule.getCondition());
                xtw.writeEndElement();
            }
            xtw.writeEndElement();
        }
    }

    public static void writeRepetitionRule(RepetitionRule repetitionRule, XMLStreamWriter xtw) throws XMLStreamException {
        if (repetitionRule != null) {
            xtw.writeStartElement(ELEMENT_REPETITION_RULE);
            if (StringUtils.isNotEmpty(repetitionRule.getRepetitionCounterVariableName())) {
                xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE,
                        ATTRIBUTE_REPETITION_COUNTER_VARIABLE_NAME, repetitionRule.getRepetitionCounterVariableName());
            }
            if (repetitionRule.getMaxInstanceCount() != null) {
                if (repetitionRule.getMaxInstanceCount() == -1) {
                    xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE,
                        ATTRIBUTE_REPETITION_MAX_INSTANCE_COUNT_NAME, RepetitionRule.MAX_INSTANCE_COUNT_UNLIMITED_VALUE);
                } else {
                    xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE,
                        ATTRIBUTE_REPETITION_MAX_INSTANCE_COUNT_NAME, Integer.toString(repetitionRule.getMaxInstanceCount()));
                }
            }
            if (StringUtils.isNotEmpty(repetitionRule.getCollectionVariableName())) {
                xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE,
                    ATTRIBUTE_REPETITION_COLLECTION_VARIABLE_NAME, repetitionRule.getCollectionVariableName());
            }
            if (StringUtils.isNotEmpty(repetitionRule.getElementVariableName())) {
                xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE,
                    ATTRIBUTE_REPETITION_ELEMENT_VARIABLE_NAME, repetitionRule.getElementVariableName());
            }
            if (StringUtils.isNotEmpty(repetitionRule.getElementIndexVariableName())) {
                xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE,
                    ATTRIBUTE_REPETITION_ELEMENT_INDEX_VARIABLE_NAME, repetitionRule.getElementIndexVariableName());
            }

            boolean hasWrittenExtensionElements = writeVariableAggregations(repetitionRule.getAggregations(), xtw);

            if (hasWrittenExtensionElements) {
                xtw.writeEndElement();
            }

            if (StringUtils.isNotEmpty(repetitionRule.getCondition())) {
                xtw.writeStartElement(ELEMENT_CONDITION);
                xtw.writeCData(repetitionRule.getCondition());
                xtw.writeEndElement();
            }
            xtw.writeEndElement();
        }
    }

    protected static boolean writeVariableAggregations(VariableAggregationDefinitions aggregationDefinitions, XMLStreamWriter xtw) throws XMLStreamException {
        boolean hasWrittenExtensionElements = false;
        if (aggregationDefinitions != null) {
            xtw.writeStartElement(ELEMENT_EXTENSION_ELEMENTS);
            hasWrittenExtensionElements = true;

            for (VariableAggregationDefinition aggregation : aggregationDefinitions.getAggregations()) {
                // start variable aggregation element
                xtw.writeStartElement(FLOWABLE_EXTENSIONS_NAMESPACE, ELEMENT_VARIABLE_AGGREGATION);


                CmmnXmlUtil.writeDefaultAttribute(ATTRIBUTE_IOPARAMETER_TARGET, aggregation.getTarget(), xtw);
                CmmnXmlUtil.writeDefaultAttribute(ATTRIBUTE_IOPARAMETER_TARGET_EXPRESSION, aggregation.getTargetExpression(), xtw);
                if (aggregation.isStoreAsTransientVariable()) {
                    CmmnXmlUtil.writeDefaultAttribute(ATTRIBUTE_VARIABLE_AGGREGATION_STORE_AS_TRANSIENT_VARIABLE, "true", xtw);
                }
                if (aggregation.isCreateOverviewVariable()) {
                    CmmnXmlUtil.writeDefaultAttribute(ATTRIBUTE_VARIABLE_AGGREGATION_CREATE_OVERVIEW, "true", xtw);
                }
                if (StringUtils.isNotEmpty(aggregation.getImplementationType())) {
                    CmmnXmlUtil.writeDefaultAttribute(aggregation.getImplementationType(), aggregation.getImplementation(), xtw);
                }

                for (VariableAggregationDefinition.Variable definition : aggregation.getDefinitions()) {
                    // start variable element
                    xtw.writeStartElement(ATTRIBUTE_VARIABLE_AGGREGATION_VARIABLE);

                    CmmnXmlUtil.writeDefaultAttribute(ATTRIBUTE_IOPARAMETER_SOURCE, definition.getSource(), xtw);
                    CmmnXmlUtil.writeDefaultAttribute(ATTRIBUTE_IOPARAMETER_SOURCE_EXPRESSION, definition.getSourceExpression(), xtw);
                    CmmnXmlUtil.writeDefaultAttribute(ATTRIBUTE_IOPARAMETER_TARGET, definition.getTarget(), xtw);
                    CmmnXmlUtil.writeDefaultAttribute(ATTRIBUTE_IOPARAMETER_TARGET_EXPRESSION, definition.getTargetExpression(), xtw);

                    // end variable element
                    xtw.writeEndElement();
                }

                // end variable aggregation element
                xtw.writeEndElement();
            }
        }

        return hasWrittenExtensionElements;
    }

    public static void writeManualActivationRule(ManualActivationRule manualActivationRule, XMLStreamWriter xtw) throws XMLStreamException {
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

    public static boolean writeCompletionNeutralRule(CompletionNeutralRule completionNeutralRule, XMLStreamWriter xtw) throws XMLStreamException {
        boolean hasWrittenExtensionElements = false;
        if (completionNeutralRule != null) {
            xtw.writeStartElement(ELEMENT_EXTENSION_ELEMENTS);
            xtw.writeStartElement(FLOWABLE_EXTENSIONS_PREFIX, ELEMENT_COMPLETION_NEUTRAL_RULE, FLOWABLE_EXTENSIONS_NAMESPACE);
            if (StringUtils.isNotBlank(completionNeutralRule.getCondition())) {

                xtw.writeStartElement(ELEMENT_CONDITION);
                xtw.writeCData(completionNeutralRule.getCondition());
                xtw.writeEndElement();
            }
            xtw.writeEndElement();
            
            hasWrittenExtensionElements = true;
        }
        
        return hasWrittenExtensionElements;
    }
    
    public static boolean writeParentCompletionRule(ParentCompletionRule parentCompletionRule, boolean hasWrittenExtensionElements, XMLStreamWriter xtw) throws XMLStreamException {
        if (parentCompletionRule != null) {
            if (!hasWrittenExtensionElements) {
                xtw.writeStartElement(ELEMENT_EXTENSION_ELEMENTS);
            }
            
            xtw.writeStartElement(FLOWABLE_EXTENSIONS_PREFIX, ELEMENT_PARENT_COMPLETION_RULE, FLOWABLE_EXTENSIONS_NAMESPACE);
            if (StringUtils.isNotEmpty(parentCompletionRule.getType())) {
                xtw.writeAttribute(ATTRIBUTE_TYPE, parentCompletionRule.getType());
            }
            xtw.writeEndElement();
            
            hasWrittenExtensionElements = true;
        }
        
        return hasWrittenExtensionElements;
    }

    public static boolean writeReactivationRule(ReactivationRule reactivationRule, boolean hasWrittenExtensionElements, XMLStreamWriter xtw) throws XMLStreamException {
        if (reactivationRule != null) {
            if (!hasWrittenExtensionElements) {
                xtw.writeStartElement(ELEMENT_EXTENSION_ELEMENTS);
            }

            xtw.writeStartElement(FLOWABLE_EXTENSIONS_PREFIX, ELEMENT_REACTIVATION_RULE, FLOWABLE_EXTENSIONS_NAMESPACE);
            writeReactivationRuleAttributes(reactivationRule, xtw);
            xtw.writeEndElement();

            hasWrittenExtensionElements = true;
        }

        return hasWrittenExtensionElements;
    }

    public static void writeReactivationRuleAttributes(ReactivationRule reactivationRule, XMLStreamWriter xtw) throws XMLStreamException {
        if (StringUtils.isNotEmpty(reactivationRule.getActivateCondition())) {
            xtw.writeAttribute(ATTRIBUTE_ACTIVATE_CONDITION, reactivationRule.getActivateCondition());
        }
        if (StringUtils.isNotEmpty(reactivationRule.getIgnoreCondition())) {
            xtw.writeAttribute(ATTRIBUTE_IGNORE_CONDITION, reactivationRule.getIgnoreCondition());
        }
        if (StringUtils.isNotEmpty(reactivationRule.getDefaultCondition())) {
            xtw.writeAttribute(ATTRIBUTE_DEFAULT_CONDITION, reactivationRule.getDefaultCondition());
        }
    }
    
}
