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
package org.flowable.cmmn.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.task.api.Task;
import org.junit.Before;
import org.junit.Test;

/**
 * An integration test between CMMN and BPMN for the stage instance id delegation over the execution to user tasks.
 *
 * @author Micha Kiener
 */
public class StagePropagationTest extends AbstractProcessEngineIntegrationTest {

    @Before
    public void deployProcessModels() {
        if (processEngineRepositoryService.createDeploymentQuery().count() == 0) {
            processEngineRepositoryService.createDeployment().addClasspathResource("org/flowable/cmmn/test/StagePropagationTestProcess.bpmn20.xml").deploy();
            processEngineRepositoryService.createDeployment().addClasspathResource("org/flowable/cmmn/test/StagePropagationTestSubProcess.bpmn20.xml").deploy();
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/StagePropagationTest.multipleTests.cmmn")
    public void testStageOnTaskPropagation() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("stagePropagationTest")
            .start();

        List<PlanItemInstance> stages = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .onlyStages()
            .orderByName().asc()
            .planItemInstanceStateActive()
            .list();

        assertNotNull(stages);
        assertEquals(2, stages.size());

        List<Task> tasks = cmmnTaskService.createTaskQuery().active().list();
        assertNotNull(tasks);
        assertEquals(5, tasks.size());

        assertStageInstanceId(tasks, "Task A", stages.get(0).getId());
        assertStageInstanceId(tasks, "Task B", stages.get(0).getId());
        assertStageInstanceId(tasks, "Task C", stages.get(0).getId());
        assertStageInstanceId(tasks, "Task D", stages.get(1).getId());
        assertStageInstanceId(tasks, "Task E", null);
    }

    protected void assertStageInstanceId(List<Task> tasks, String taskName, String stageInstanceId) {
        Task taskToAssert = null;
        for (Task task : tasks) {
            if (taskName.equals(task.getName())) {
                taskToAssert = task;
                break;
            }
        }
        assertNotNull(taskToAssert);
        assertEquals(stageInstanceId, taskToAssert.getStageInstanceId());
    }
}
