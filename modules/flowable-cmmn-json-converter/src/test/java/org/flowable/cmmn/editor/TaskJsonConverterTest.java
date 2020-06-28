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
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.Task;
import org.junit.Test;

/**
 * @author shareniu
 */
public class TaskJsonConverterTest extends AbstractConverterTest {

    @Override
    @Test
    public void convertJsonToModel() throws Exception {
        CmmnModel cmmnModel = readJsonFile();
        validateModel(cmmnModel);
    }

    @Override
    protected String getResource() {
        return "test.taskBlockingexpression.json";
    }

    @Override
    protected void validateModel(CmmnModel model) {
        Case caseModel = model.getPrimaryCase();
        assertThat(caseModel.getId()).isEqualTo("shareniu_test");
        assertThat(caseModel.getName()).isEqualTo("shareniu_test");

        Stage planModelStage = caseModel.getPlanModel();
        assertThat(planModelStage).isNotNull();

        PlanItem planItem = planModelStage.findPlanItemInPlanFragmentOrUpwards("planItem1");
        assertThat(planItem).isNotNull();
        assertThat(planItem.getId()).isEqualTo("planItem1");
        assertThat(planItem.getName()).isEqualTo("shareniu_task");

        PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
        assertThat(planItemDefinition).isInstanceOf(Task.class);

        Task task = (Task) planItemDefinition;
        assertThat(task.getId()).isEqualTo("shareniu_task");
        assertThat(task.getName()).isEqualTo("shareniu_task");
        assertThat(task.getBlockingExpression()).isEqualTo("${shareniu_task}");

        PlanItem planItem2 = planModelStage.findPlanItemInPlanFragmentOrUpwards("planItem2");
        assertThat(planItem2).isNotNull();
        assertThat(planItem2.getId()).isEqualTo("planItem2");
        assertThat(planItem2.getName()).isEqualTo("shareniu_human_task");

        PlanItemDefinition planItemDefinition2 = planItem2.getPlanItemDefinition();
        assertThat(planItemDefinition2).isInstanceOf(HumanTask.class);

        HumanTask humanTask = (HumanTask) planItemDefinition2;
        assertThat(humanTask.getName()).isEqualTo("shareniu_human_task");
        assertThat(humanTask.getBlockingExpression()).isEqualTo("${shareniu_human_task}");

    }

    protected FieldExtension createFieldExtension(String name, String value) {
        return new FieldExtension(name, value, null);
    }
}
