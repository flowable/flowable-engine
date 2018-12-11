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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
public class TaskJsonConverterTest  extends AbstractConverterTest{

    @Test
    public void convertJsonToModel() throws Exception {
        CmmnModel cmmnModel = readJsonFile();
        validateModel(cmmnModel);
    }

    protected String getResource() {
        return "test.taskBlockingexpression.json";
    }

    protected void validateModel(CmmnModel model) {
        Case caseModel = model.getPrimaryCase();
        assertEquals("shareniu_test", caseModel.getId());
        assertEquals("shareniu_test", caseModel.getName());

        Stage planModelStage = caseModel.getPlanModel();
        assertNotNull(planModelStage);

        PlanItem planItem = planModelStage.findPlanItemInPlanFragmentOrUpwards("planItem1");
        assertNotNull(planItem);
        assertEquals("planItem1", planItem.getId());
        assertEquals("shareniu_task", planItem.getName());

        PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
        assertNotNull(planItemDefinition);
        assertTrue(planItemDefinition instanceof Task);

        Task task = (Task) planItemDefinition;
        assertEquals("shareniu_task", task.getId());
        assertEquals("shareniu_task", task.getName());
        assertEquals("${shareniu_task}",task.getBlockingExpression());

        PlanItem planItem2 = planModelStage.findPlanItemInPlanFragmentOrUpwards("planItem2");
        assertNotNull(planItem2);
        assertEquals("planItem2", planItem2.getId());
        assertEquals("shareniu_human_task", planItem2.getName());

        PlanItemDefinition planItemDefinition2 = planItem2.getPlanItemDefinition();
        assertNotNull(planItemDefinition2);
        assertTrue(planItemDefinition2 instanceof HumanTask);

        HumanTask humanTask = (HumanTask) planItemDefinition2;
        assertEquals("shareniu_human_task", humanTask.getName());
        assertEquals("${shareniu_human_task}",humanTask.getBlockingExpression());

    }

    protected FieldExtension createFieldExtension(String name, String value) {
        return new FieldExtension(name, value, null);
    }
}
