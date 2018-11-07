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

/**
 * @author Joram Barrez
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
        if (conversionHelper.getCurrentCmmnElement() instanceof PlanItemControl) {
            
            RepetitionRule repetitionRule = new RepetitionRule();
            repetitionRule.setName(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_NAME));
            repetitionRule.setRepetitionCounterVariableName(xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, 
                    CmmnXmlConstants.ATTRIBUTE_REPETITION_COUNTER_VARIABLE_NAME));
            
            PlanItemControl planItemControl = (PlanItemControl) conversionHelper.getCurrentCmmnElement();
            planItemControl.setRepetitionRule(repetitionRule);
            
            return repetitionRule;
        }
        return null;
    }
    
}