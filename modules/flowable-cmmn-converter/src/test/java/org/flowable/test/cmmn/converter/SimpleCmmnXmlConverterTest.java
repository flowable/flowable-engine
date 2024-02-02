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
package org.flowable.test.cmmn.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Milestone;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryOnPart;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.Task;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Tijs Rademakers
 */
public class SimpleCmmnXmlConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/simple.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();
        assertThat(cmmnModel.getCases())
                .extracting(Case::getId, Case::getInitiatorVariableName)
                .containsExactly(tuple("myCase", "test"));

        // Case
        Case caze = cmmnModel.getCases().get(0);
        assertThat(caze.getCandidateStarterUsers())
                .containsExactlyInAnyOrder("test", "test2");
        assertThat(caze.getCandidateStarterGroups())
                .containsExactlyInAnyOrder("group", "group2");
        assertThat(caze.isAsync()).isFalse();

        // Plan model
        Stage planModel = caze.getPlanModel();
        assertThat(planModel).isNotNull();
        assertThat(planModel.getId()).isEqualTo("myPlanModel");
        assertThat(planModel.getName()).isEqualTo("My CasePlanModel");
        assertThat(planModel.getFormKey()).isEqualTo("formKey");
        assertThat(planModel.getValidateFormFields()).isEqualTo("validateFormFieldsValue");

        // Sentries
        assertThat(planModel.getSentries()).hasSize(3);
        for (Sentry sentry : planModel.getSentries()) {
            List<SentryOnPart> onParts = sentry.getOnParts();
            assertThat(onParts)
                    .hasSize(1)
                    .extracting(SentryOnPart::getId, SentryOnPart::getSourceRef, SentryOnPart::getSource, SentryOnPart::getStandardEvent)
                    .doesNotContainNull();
        }

        // Plan items definitions
        List<PlanItemDefinition> planItemDefinitions = planModel.getPlanItemDefinitions();
        assertThat(planItemDefinitions).hasSize(4);
        assertThat(planModel.findPlanItemDefinitionsOfType(Task.class, false)).hasSize(2);
        assertThat(planModel.findPlanItemDefinitionsOfType(Milestone.class, false)).hasSize(2);
        for (PlanItemDefinition planItemDefinition : planItemDefinitions) {
            assertThat(planItemDefinition.getId()).isNotNull();
            assertThat(planItemDefinition.getName()).isNotNull();
        }

        // Plan items
        List<PlanItem> planItems = planModel.getPlanItems();
        assertThat(planItems).hasSize(4);
        int nrOfTasks = 0;
        int nrOfMileStones = 0;
        for (PlanItem planItem : planItems) {
            assertThat(planItem)
                    .extracting(PlanItem::getId, PlanItem::getDefinitionRef, PlanItem::getPlanItemDefinition) // Verify plan item definition ref is resolved
                    .doesNotContainNull();

            PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
            if (planItemDefinition instanceof Milestone) {
                nrOfMileStones++;
            } else if (planItemDefinition instanceof Task) {
                nrOfTasks++;
            }

            if (!"planItemTaskA".equals(planItem.getId())) {
                assertThat(planItem.getEntryCriteria()).hasSize(1);
                assertThat(planItem.getEntryCriteria().get(0).getSentry()).isNotNull(); // Verify if sentry reference is resolved
            }

            if (planItem.getPlanItemDefinition() instanceof Task) {
                if ("planItemTaskB".equals(planItem.getId())) {
                    assertThat(((Task) planItem.getPlanItemDefinition()).isBlocking()).isFalse();
                } else {
                    assertThat(((Task) planItem.getPlanItemDefinition()).isBlocking()).isTrue();
                }
            }
        }

        assertThat(nrOfMileStones).isEqualTo(2);
        assertThat(nrOfTasks).isEqualTo(2);
    }

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/simple-async.cmmn")
    public void validateAsyncModel(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();
        assertThat(cmmnModel.getCases())
                .extracting(Case::getId, Case::getInitiatorVariableName)
                .containsExactly(tuple("myCase", "test"));

        // Case
        Case caze = cmmnModel.getCases().get(0);
        assertThat(caze.getCandidateStarterUsers())
                .containsExactlyInAnyOrder("test2");
        assertThat(caze.getCandidateStarterGroups())
                .containsExactlyInAnyOrder("group1");
        assertThat(caze.isAsync()).isTrue();
    }

}
