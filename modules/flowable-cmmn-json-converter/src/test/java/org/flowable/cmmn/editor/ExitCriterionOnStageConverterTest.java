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
package org.flowable.cmmn.editor;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;

public class ExitCriterionOnStageConverterTest extends AbstractConverterTest {

    @Override
    protected String getResource() {
        return "test.exitCriterionOnStageModel.json";
    }

    @Override
    protected void validateModel(CmmnModel model) {
        Case caseModel = model.getPrimaryCase();
        Stage planModelStage = caseModel.getPlanModel();
        PlanItemDefinition planItemDefinition = planModelStage.findPlanItemDefinitionInStageOrDownwards("sid-46EAD2FE-4D89-42ED-9B1E-5005AE5BF2F7");
        assertThat(planItemDefinition).isInstanceOf(Stage.class);

        PlanItem planItem = planModelStage.findPlanItemInPlanFragmentOrDownwards(planItemDefinition.getPlanItemRef());
        assertThat(planItem.getEntryCriteria()).hasSize(1);
        Criterion entryCriterion = planItem.getEntryCriteria().get(0);
        assertThat(entryCriterion.getSentryRef()).isNotNull();

        assertThat(planItem.getExitCriteria()).hasSize(1);
        Criterion exitCriterion = planItem.getExitCriteria().get(0);
        assertThat(exitCriterion.getSentryRef()).isNotNull();
    }
}
