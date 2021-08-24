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

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanFragment;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Sentry;

/**
 * @author Joram Barrez
 */
public class PlanFragmentExport extends AbstractPlanItemDefinitionExport<PlanFragment> {

    private static final PlanFragmentExport instance = new PlanFragmentExport();

    public static PlanFragmentExport getInstance() {
        return instance;
    }

    private PlanFragmentExport() {
    }

    @Override
    protected Class<PlanFragment> getExportablePlanItemDefinitionClass() {
        return PlanFragment.class;
    }

    @Override
    protected String getPlanItemDefinitionXmlElementValue(PlanFragment planFragment) {
        return ELEMENT_PLAN_FRAGMENT;
    }

    @Override
    protected void writePlanItemDefinitionSpecificAttributes(PlanFragment planFragment, XMLStreamWriter xtw) throws Exception {
        super.writePlanItemDefinitionSpecificAttributes(planFragment, xtw);
    }

    @Override
    protected void writePlanItemDefinitionBody(CmmnModel model, PlanFragment planFragment, XMLStreamWriter xtw) throws Exception {
        super.writePlanItemDefinitionBody(model, planFragment, xtw);
        for (PlanItem planItem : planFragment.getPlanItems()) {
            PlanItemExport.writePlanItem(model, planItem, xtw);
        }

        for (Sentry sentry : planFragment.getSentries()) {
            SentryExport.writeSentry(model, sentry, xtw);
        }

    }
}
