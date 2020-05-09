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
import static org.assertj.core.api.Assertions.fail;

import java.util.List;

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanFragment;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Stage;

/**
 * @author Joram Barrez
 */
public class PlanItemControlConverterTest extends AbstractConverterTest {

    @Override
    protected String getResource() {
        return "test.planItemControl.json";
    }

    @Override
    protected void validateModel(CmmnModel cmmnModel) {

        // The plan model should have auto complete enabled
        Stage planModel = cmmnModel.getPrimaryCase().getPlanModel();
        assertThat(planModel.isAutoComplete()).isTrue();

        // Stage A should not have auto complete disabled
        Stage stageA = findStageByName(planModel, "Stage A");
        assertThat(stageA.isAutoComplete()).isFalse();
        assertThat(stageA.getAutoCompleteCondition()).isNull();

        // Stage A, Task A, Task B and Task D should not have any item control set
        assertNoItemControlsSet(cmmnModel, "Stage A");
        assertNoItemControlsSet(cmmnModel, "A");
        assertNoItemControlsSet(cmmnModel, "B");
        assertNoItemControlsSet(cmmnModel, "D");

        // Stage B should have everything enabled
        Stage stageB = findStageByName(planModel, "Stage B");
        assertThat(stageB.isAutoComplete()).isTrue();
        assertThat(stageB.getAutoCompleteCondition()).isNull();

        PlanItem stageBPlanItem = findPlanItemByName(planModel, "Stage B");
        assertThat(stageBPlanItem.getItemControl().getRequiredRule()).isNotNull();
        assertThat(stageBPlanItem.getItemControl().getRequiredRule().getCondition()).isNull();
        assertThat(stageBPlanItem.getItemControl().getManualActivationRule()).isNotNull();
        assertThat(stageBPlanItem.getItemControl().getManualActivationRule().getCondition()).isNull();
        assertThat(stageBPlanItem.getItemControl().getRepetitionRule()).isNotNull();
        assertThat(stageBPlanItem.getItemControl().getRepetitionRule().getCondition()).isNull();

        // Stage C should have an autocomplete condition set
        Stage stageC = findStageByName(planModel, "Stage C");
        assertThat(stageC.isAutoComplete()).isTrue();
        assertThat(stageC.getAutoCompleteCondition()).isEqualTo("${someVariable}");
        assertNoItemControlsSet(cmmnModel, "Stage C");

        // Task C should have all item controls enabled
        assertAllItemControlsSet(cmmnModel, "C");

        PlanItem taskCPlanItem = findPlanItemByName(planModel, "C");
        assertThat(taskCPlanItem.getItemControl().getRequiredRule().getCondition()).isNull();
        assertThat(taskCPlanItem.getItemControl().getRepetitionRule().getCondition()).isNull();
        assertThat(taskCPlanItem.getItemControl().getManualActivationRule().getCondition()).isNull();

        // Task E should have a condition set for all item controls
        assertAllItemControlsSet(cmmnModel, "C");

        PlanItem taskEPlanItem = findPlanItemByName(planModel, "E");
        assertThat(taskEPlanItem.getItemControl().getRequiredRule().getCondition()).isEqualTo("${requiredCondition}");
        assertThat(taskEPlanItem.getItemControl().getRepetitionRule().getCondition()).isEqualTo("${repetitionCondition}");
        assertThat(taskEPlanItem.getItemControl().getManualActivationRule().getCondition()).isEqualTo("${manualActivationCondition}");
    }

    protected void assertNoItemControlsSet(CmmnModel cmmnModel, String planItemName) {
        PlanItem planItem = findPlanItemByName(cmmnModel.getPrimaryCase().getPlanModel(), planItemName);
        assertThat(planItem).as("No plan item found with name " + planItemName).isNotNull();
        if (planItem.getItemControl() != null) {
            assertThat(planItem.getItemControl().getRequiredRule()).isNull();
            assertThat(planItem.getItemControl().getRepetitionRule()).isNull();
            assertThat(planItem.getItemControl().getManualActivationRule()).isNull();
        } else {
            assertThat(planItem.getItemControl()).isNull();
        }
    }

    protected void assertAllItemControlsSet(CmmnModel cmmnModel, String planItemName) {
        PlanItem planItem = findPlanItemByName(cmmnModel.getPrimaryCase().getPlanModel(), planItemName);
        assertThat(planItem).as("No plan item found with name " + planItemName).isNotNull();
        assertThat(planItem.getItemControl().getRequiredRule()).isNotNull();
        assertThat(planItem.getItemControl().getRepetitionRule()).isNotNull();
        assertThat(planItem.getItemControl().getManualActivationRule()).isNotNull();
    }

    protected Stage findStageByName(Stage planModel, String stageName) {
        List<Stage> stages = planModel.findPlanItemDefinitionsOfType(Stage.class, true);
        for (Stage stage : stages) {
            if (stageName.equals(stage.getName())) {
                return stage;
            }
        }
        fail("No stage found with name " + stageName);
        return null;
    }

    protected PlanItem findPlanItemByName(PlanFragment planFragment, String planItemName) {
        List<PlanItem> planItems = planFragment.getPlanItems();
        if (planItems != null) {
            for (PlanItem planItem : planItems) {

                if (planItemName.equals(planItem.getName())) {
                    return planItem;
                }

                if (planItem.getPlanItemDefinition() instanceof PlanFragment) {
                    PlanItem p = findPlanItemByName((PlanFragment) planItem.getPlanItemDefinition(), planItemName);
                    if (p != null) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

}
