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

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.Milestone;

/**
 * @author Joram Barrez
 */
public class MilestoneXmlConverter extends PlanItemDefinitionXmlConverter {
    
    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_MILESTONE;
    }
    
    @Override
    public boolean hasChildElements() {
        return true;
    }

    @Override
    protected CmmnElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        Milestone mileStone = new Milestone();
        mileStone.setName(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_NAME));
        
        String displayOrderString = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_DISPLAY_ORDER);
        if (StringUtils.isNotEmpty(displayOrderString)) {
            mileStone.setDisplayOrder(Integer.valueOf(displayOrderString));
        }

        String includeInStageOverviewString = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_INCLUDE_IN_STAGE_OVERVIEW);
        if (StringUtils.isNotEmpty(includeInStageOverviewString)) {
            mileStone.setIncludeInStageOverview(includeInStageOverviewString);
        } else {
            mileStone.setIncludeInStageOverview("true");  // True by default
        }

        String milestoneVariable = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_MILESTONE_VARIABLE);
        if (StringUtils.isNotEmpty(milestoneVariable)) {
            mileStone.setMilestoneVariable(milestoneVariable);
        }
        
        return mileStone;
    }
    
}