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
import org.flowable.cmmn.converter.util.CmmnXmlUtil;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemControl;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class PlanItemExport implements CmmnXmlConstants {
    
    public static void writePlanItem(CmmnModel model, PlanItem planItem, XMLStreamWriter xtw) throws Exception {
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
        
        boolean didWriteExtensionElement = CmmnXmlUtil.writeExtensionElements(planItem, false, model.getNamespaces(), xtw);
        if (didWriteExtensionElement) {
            xtw.writeEndElement();
        }
        
        if (planItem.getItemControl() != null) {
            PlanItemControl itemControl = planItem.getItemControl();
            PlanItemControlExport.writeItemControl(itemControl, xtw);
        }
        
        CriteriaExport.writeCriteriaElements(planItem, xtw);
        
        // end plan item element
        xtw.writeEndElement();
    }
}
