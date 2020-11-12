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

import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Milestone;
import org.flowable.cmmn.model.Stage;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Tijs Rademakers
 */
public class MilestoneCmmnXmlConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/milestone.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        Stage planModel = cmmnModel.getPrimaryCase().getPlanModel();
        List<Milestone> milestones = planModel.findPlanItemDefinitionsOfType(Milestone.class, false);
        assertThat(milestones)
                .extracting(Milestone::getName, Milestone::getDisplayOrder, Milestone::getIncludeInStageOverview)
                .containsExactly(tuple("Milestone 1", 5, "false"));

        Stage nestedStage = planModel.findPlanItemDefinitionsOfType(Stage.class, false).get(0);
        assertThat(nestedStage).isNotNull();
        assertThat(nestedStage.getName()).isEqualTo("Nested Stage");

        assertThat(nestedStage.getPlanItems()).hasSize(1);
        milestones = nestedStage.findPlanItemDefinitionsOfType(Milestone.class, false);
        assertThat(milestones)
                .extracting(Milestone::getName, Milestone::getDisplayOrder, Milestone::getIncludeInStageOverview)
                .containsExactly(tuple("Milestone 2", 3, "true"));
    }

}
