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

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.ProcessTask;
import org.flowable.cmmn.model.Stage;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Tijs Rademakers
 */
public class ProcessTask2CmmnXmlConverterTest {

    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/process-task2.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();

        // Case
        assertThat(cmmnModel.getCases())
                .extracting(Case::getId)
                .containsExactly("processTaskModel");

        // Plan model
        Stage planModel = cmmnModel.getCases().get(0).getPlanModel();
        assertThat(planModel)
                .extracting(Stage::getId, Stage::getName)
                .containsExactly("onecaseplanmodel1", "Case plan model");

        PlanItem planItemTask1 = cmmnModel.findPlanItem("planItem2");
        PlanItemDefinition planItemDefinition = planItemTask1.getPlanItemDefinition();
        assertThat(planItemDefinition)
                .isInstanceOfSatisfying(ProcessTask.class, task1 -> {
                    assertThat(task1.getProcessRefExpression()).isEqualTo("myTestProcess");
                    assertThat((task1.isSameDeployment())).isTrue();

                    assertThat(task1.getInParameters()).isEmpty();
                    assertThat(task1.getOutParameters()).isEmpty();
                });
        PlanItemDefinition taskDefinition = cmmnModel.findPlanItemDefinition("onehumantask1");
        assertThat(taskDefinition)
                .isInstanceOfSatisfying(HumanTask.class, humanTask -> {
                    assertThat(humanTask.getName()).isEqualTo("Human task");
                    assertThat(humanTask.getAssignee()).isEqualTo("admin");
                    assertThat(humanTask.getOwner()).isEqualTo("admin");
                    assertThat(humanTask.getFormKey()).isEqualTo("aHumanTaskForm");
                    assertThat(humanTask.getPriority()).isEqualTo("50");
                    assertThat(humanTask.getDueDate()).isEqualTo("2019-01-01");
                    assertThat(humanTask.getCategory()).isEqualTo("testCategory");
                    assertThat(humanTask.getValidateFormFields()).isEqualTo("validateFormFieldsValue");
                });
    }

}
