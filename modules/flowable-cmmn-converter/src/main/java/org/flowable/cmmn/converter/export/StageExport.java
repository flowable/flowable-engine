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

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.Stage;

import javax.xml.stream.XMLStreamWriter;

public class StageExport extends AbstractPlanItemDefinitionExport<Stage> {

    private static final StageExport instance = new StageExport();

    public static StageExport getInstance() {
        return instance;
    }

    private StageExport() {
    }

    @Override
    protected Class<Stage> getExportablePlanItemDefinitionClass() {
        return Stage.class;
    }

    @Override
    protected String getPlanItemDefinitionXmlElementValue(Stage stage) {
        // start plan model or stage element
        if (stage.isPlanModel()) {
            return ELEMENT_PLAN_MODEL;
        } else {
            return ELEMENT_STAGE;
        }
    }

    @Override
    protected void writePlanItemDefinitionSpecificAttributes(Stage stage, XMLStreamWriter xtw) throws Exception {
        super.writePlanItemDefinitionSpecificAttributes(stage, xtw);
        if (StringUtils.isNotEmpty(stage.getFormKey())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_FORM_KEY, stage.getFormKey());
        }

        if (stage.isAutoComplete()) {
            xtw.writeAttribute(ATTRIBUTE_IS_AUTO_COMPLETE, Boolean.toString(stage.isAutoComplete()));
        }
        if (StringUtils.isNotEmpty(stage.getAutoCompleteCondition())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_AUTO_COMPLETE_CONDITION, stage.getAutoCompleteCondition());
        }
    }

    @Override
    protected void writePlanItemDefinitionBody(Stage stage, XMLStreamWriter xtw) throws Exception {
        super.writePlanItemDefinitionBody(stage, xtw);
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
    }
}
