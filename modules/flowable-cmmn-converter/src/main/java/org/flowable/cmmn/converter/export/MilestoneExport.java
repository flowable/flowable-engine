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
import org.flowable.cmmn.model.Milestone;

public class MilestoneExport extends AbstractPlanItemDefinitionExport<Milestone> {

    @Override
    protected Class<Milestone> getExportablePlanItemDefinitionClass() {
        return Milestone.class;
    }

    @Override
    protected String getPlanItemDefinitionXmlElementValue(Milestone planItemDefinition) {
        return ELEMENT_MILESTONE;
    }
    
    @Override
    protected void writePlanItemDefinitionSpecificAttributes(Milestone milestone, XMLStreamWriter xtw) throws Exception {
        super.writePlanItemDefinitionSpecificAttributes(milestone, xtw);
        
        if (milestone.getDisplayOrder() != null) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_DISPLAY_ORDER, String.valueOf(milestone.getDisplayOrder()));
        }
        if (StringUtils.isNotEmpty(milestone.getIncludeInStageOverview()) && !"true".equalsIgnoreCase(milestone.getIncludeInStageOverview())) { // if it's missing, it's true by default
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_INCLUDE_IN_STAGE_OVERVIEW, milestone.getIncludeInStageOverview());
        }
        if (StringUtils.isNotEmpty(milestone.getMilestoneVariable())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_MILESTONE_VARIABLE, milestone.getMilestoneVariable());
        }
    }
}
