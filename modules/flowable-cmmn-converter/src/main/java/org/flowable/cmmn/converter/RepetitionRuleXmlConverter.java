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

import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.PlanItemControl;
import org.flowable.cmmn.model.RepetitionRule;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;

/**
 * @author Joram Barrez
 * @author Micha Kiener
 */
public class RepetitionRuleXmlConverter extends CaseElementXmlConverter {
    
    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_REPETITION_RULE;
    }
    
    @Override
    public boolean hasChildElements() {
        return true;
    }

    @Override
    protected CmmnElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        if (conversionHelper.getCurrentCmmnElement() instanceof PlanItemControl planItemControl) {
            
            RepetitionRule repetitionRule = new RepetitionRule();
            repetitionRule.setName(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_NAME));

            repetitionRule.setRepetitionCounterVariableName(xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, 
                    CmmnXmlConstants.ATTRIBUTE_REPETITION_COUNTER_VARIABLE_NAME));
            
            
            String ignoreRepetitionCounterVariableValue = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_IGNORE_REPETITION_COUNTER_VARIABLE);
            if ("true".equalsIgnoreCase(ignoreRepetitionCounterVariableValue)) {
                repetitionRule.setIgnoreRepetitionCounterVariable(true);
            }

            String maxInstanceCountValue = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_REPETITION_MAX_INSTANCE_COUNT_NAME);
            if (maxInstanceCountValue == null) {
                repetitionRule.setMaxInstanceCount(null);
            } else {
                if (RepetitionRule.MAX_INSTANCE_COUNT_UNLIMITED_VALUE.equals(maxInstanceCountValue)) {
                    repetitionRule.setMaxInstanceCount(RepetitionRule.MAX_INSTANCE_COUNT_UNLIMITED);
                } else {
                    int maxInstanceCount = Integer.parseInt(maxInstanceCountValue);
                    if (maxInstanceCount == 0) {
                        throw new FlowableIllegalArgumentException("A 'maxInstanceCount' on a repetition rule with value '0' is not allowed, either set it to '-1' or 'unlimited' or any other positive value..");
                    }
                    repetitionRule.setMaxInstanceCount(maxInstanceCount);
                }
            }

            repetitionRule.setCollectionVariableName(xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE,
                CmmnXmlConstants.ATTRIBUTE_REPETITION_COLLECTION_VARIABLE_NAME));

            repetitionRule.setElementVariableName(xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE,
                CmmnXmlConstants.ATTRIBUTE_REPETITION_ELEMENT_VARIABLE_NAME));

            repetitionRule.setElementIndexVariableName(xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE,
                CmmnXmlConstants.ATTRIBUTE_REPETITION_ELEMENT_INDEX_VARIABLE_NAME));

            planItemControl.setRepetitionRule(repetitionRule);
            
            return repetitionRule;
        }
        return null;
    }

    protected Integer parseInt(String value) {
        if (value == null) {
            return null;
        }
        return Integer.parseInt(value);
    }
    
}
