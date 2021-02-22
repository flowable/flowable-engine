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

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Stage;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Tijs Rademakers
 */
public class NestedStagesCmmnXmlConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/nested-stages.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        Stage planModel = cmmnModel.getPrimaryCase().getPlanModel();
        assertThat(planModel.getPlanItems()).hasSize(2);

        Stage nestedStage = null;
        for (PlanItem planItem : planModel.getPlanItems()) {
            assertThat(planItem.getPlanItemDefinition()).isNotNull();
            if (planItem.getPlanItemDefinition() instanceof Stage) {
                nestedStage = (Stage) planItem.getPlanItemDefinition();
            }
        }
        assertThat(nestedStage).isNotNull();
        assertThat(nestedStage.getName()).isEqualTo("Nested Stage");

        // Nested stage has 3 plan items, and one of them references the rootTask from the plan model
        assertThat(nestedStage.getPlanItems()).hasSize(3);
        Stage nestedNestedStage = null;
        for (PlanItem planItem : nestedStage.getPlanItems()) {
            assertThat(planItem.getPlanItemDefinition()).isNotNull();
            if (planItem.getPlanItemDefinition() instanceof Stage) {
                nestedNestedStage = (Stage) planItem.getPlanItemDefinition();
            }
        }
        assertThat(nestedNestedStage).isNotNull();
        assertThat(nestedNestedStage.getName()).isEqualTo("Nested Stage 2");
        assertThat(nestedNestedStage.getPlanItems()).hasSize(1);
        assertThat(nestedNestedStage.getPlanItems().get(0).getPlanItemDefinition().getId()).isEqualTo("rootTask");
    }

}
