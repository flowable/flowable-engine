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
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.Stage;

public class StageExport implements CmmnXmlConstants {
    
    public static void writeStage(Stage stage, XMLStreamWriter xtw) throws Exception {
        // start plan model or stage element
        if (stage.isPlanModel()) {
            xtw.writeStartElement(ELEMENT_PLAN_MODEL);
        } else {
            xtw.writeStartElement(ELEMENT_STAGE);
        }
        
        xtw.writeAttribute(ATTRIBUTE_ID, stage.getId());

        if (StringUtils.isNotEmpty(stage.getName())) {
            xtw.writeAttribute(ATTRIBUTE_NAME, stage.getName());
        }

        if (StringUtils.isNotEmpty(stage.getDocumentation())) {

            xtw.writeStartElement(ELEMENT_DOCUMENTATION);
            xtw.writeCharacters(stage.getDocumentation());
            xtw.writeEndElement();
        }
        
        for (PlanItem planItem : stage.getPlanItems()) {
            PlanItemExport.writePlanItem(planItem, xtw);
        }
        
        for (Sentry sentry : stage.getSentries()) {
            SentryExport.writeSentry(sentry, xtw);
        }
        
        for (PlanItemDefinition planItemDefinition : stage.getPlanItemDefinitions()) {
            PlanItemDefinitionExport.writePlanItemDefinition(planItemDefinition, xtw);
        }
        
        if (stage.isPlanModel() && stage.getExitCriteria() != null && !stage.getExitCriteria().isEmpty()) {
            for (Criterion exitCriterion : stage.getExitCriteria()) {
                xtw.writeStartElement(ELEMENT_EXIT_CRITERION);
                xtw.writeAttribute(ATTRIBUTE_ID, exitCriterion.getId());

                if (StringUtils.isNotEmpty(exitCriterion.getName())) {
                    xtw.writeAttribute(ATTRIBUTE_NAME, exitCriterion.getName());
                }

                if (StringUtils.isNotEmpty(exitCriterion.getSentryRef())) {
                    xtw.writeAttribute(ATTRIBUTE_SENTRY_REF, exitCriterion.getSentryRef());
                }
                
                // end entry criterion element
                xtw.writeEndElement();
            }
        }
        
        // end plan model or stage element
        xtw.writeEndElement();
    }
}
