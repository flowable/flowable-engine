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

import java.util.List;

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.ReactivationRule;
import org.flowable.cmmn.model.Stage;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

public class ReactivationRuleConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/stage-reactivation-test-case.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();
        Stage planModel = cmmnModel.getPrimaryCase().getPlanModel();
        assertThat(cmmnModel.getPrimaryCase().getReactivateEventListener()).isNotNull();
        assertThat(cmmnModel.getPrimaryCase().getReactivateEventListener().getDefaultReactivationRule()).isNotNull();
        assertThat(cmmnModel.getPrimaryCase().getReactivateEventListener().getDefaultReactivationRule().getType()).isEqualTo(ReactivationRule.IGNORE);

        List<PlanItem> planItems = planModel.getPlanItems();
        assertThat(planItems)
                .filteredOn(planItem -> (planItem.getPlanItemDefinition() instanceof Stage))
                .extracting(PlanItem::getItemControl,
                        planItem -> planItem.getItemControl().getReactivationRule(),
                        planItem -> planItem.getItemControl().getReactivationRule().getType())
                .doesNotContainNull();

        assertThat(planItems)
                .filteredOn(planItem -> (planItem.getPlanItemDefinition() instanceof Stage))
                .extracting(planItem -> planItem.getItemControl().getReactivationRule().getType())
                .containsExactly(ReactivationRule.ACTIVATE, ReactivationRule.DEFAULT);

        assertThat(planItems)
                .filteredOn(planItem -> (planItem.getPlanItemDefinition() instanceof Stage))
                .extracting(planItem -> planItem.getItemControl().getReactivationRule().getCondition())
                .containsExactly("${reactivateStageA}", null);

        Stage stageA = (Stage) cmmnModel.getPrimaryCase().getPlanModel().findPlanItemDefinitionInStageOrDownwards("stageA");
        assertThat(stageA.getPlanItems()).hasSize(1).extracting(PlanItem::getItemControl).containsOnlyNulls();

        Stage stageB = (Stage) cmmnModel.getPrimaryCase().getPlanModel().findPlanItemDefinitionInStageOrDownwards("stageB");
        assertThat(stageB.getPlanItems())
                .extracting(planItem -> planItem.getItemControl().getReactivationRule().getType())
                .containsExactly(ReactivationRule.IGNORE, ReactivationRule.IGNORE_IF_COMPLETED);
    }
}
