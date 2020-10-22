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
import org.flowable.cmmn.model.ParentCompletionRule;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Stage;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

public class ParentCompletionConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/parentCompletionRuleAtPlanItem.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();
        Stage planModel = cmmnModel.getPrimaryCase().getPlanModel();
        List<PlanItem> planItems = planModel.getPlanItems();
        assertThat(planItems)
                .extracting(planItem -> planItem.getItemControl(),
                        planItem -> planItem.getItemControl().getParentCompletionRule(),
                        planItem -> planItem.getItemControl().getParentCompletionRule().getType())
                .doesNotContainNull();

        assertThat(planItems)
                .extracting(planItem -> planItem.getItemControl().getParentCompletionRule().getType())
                .containsExactly(ParentCompletionRule.IGNORE, ParentCompletionRule.DEFAULT, ParentCompletionRule.IGNORE_IF_AVAILABLE,
                        ParentCompletionRule.IGNORE_IF_AVAILABLE_OR_ENABLED);

        Stage stageOne = (Stage) cmmnModel.getPrimaryCase().getPlanModel().findPlanItemDefinitionInStageOrDownwards("stageOne");
        List<PlanItem> planItems1 = stageOne.getPlanItems();
        assertThat(planItems1).hasSize(1);
        PlanItem planItem = planItems1.get(0);
        assertThat(planItem.getItemControl()).isNotNull();
        assertThat(planItem.getItemControl().getParentCompletionRule()).isNotNull();
        assertThat(planItem.getItemControl().getParentCompletionRule().getType()).isEqualTo(ParentCompletionRule.IGNORE_AFTER_FIRST_COMPLETION);
    }

}
