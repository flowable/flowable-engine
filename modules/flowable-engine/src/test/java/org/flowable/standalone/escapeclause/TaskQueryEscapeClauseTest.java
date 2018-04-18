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
package org.flowable.standalone.escapeclause;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.runtime.ProcessInstance;

public class TaskQueryEscapeClauseTest extends AbstractEscapeClauseTestCase {

    private String deploymentOneId;

    private String deploymentTwoId;

    private ProcessInstance processInstance1;

    private ProcessInstance processInstance2;

    private org.flowable.task.api.Task task1;

    private org.flowable.task.api.Task task2;

    @Override
    protected void setUp() throws Exception {
        deploymentOneId = repositoryService
                .createDeployment()
                .tenantId("One%")
                .addClasspathResource("org/flowable/standalone/escapeclause/oneTaskProcessEscapeClauseTest.bpmn20.xml")
                .deploy()
                .getId();

        deploymentTwoId = repositoryService
                .createDeployment()
                .tenantId("Two_")
                .addClasspathResource("org/flowable/standalone/escapeclause/oneTaskProcessEscapeClauseTest.bpmn20.xml")
                .deploy()
                .getId();

        processInstance1 = runtimeService.startProcessInstanceByKeyAndTenantId("oneTaskProcess", "One%", "One%");
        runtimeService.setProcessInstanceName(processInstance1.getId(), "One%");

        processInstance2 = runtimeService.startProcessInstanceByKeyAndTenantId("oneTaskProcess", "Two_", "Two_");
        runtimeService.setProcessInstanceName(processInstance2.getId(), "Two_");

        task1 = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        taskService.setAssignee(task1.getId(), "assignee%");
        taskService.setOwner(task1.getId(), "owner%");
        taskService.setVariableLocal(task1.getId(), "var1", "One%");

        task2 = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        task2.setName("my task_");
        task2.setDescription("documentation_");
        taskService.saveTask(task2);
        taskService.setAssignee(task2.getId(), "assignee_");
        taskService.setOwner(task2.getId(), "owner_");
        taskService.setVariableLocal(task2.getId(), "var1", "Two_");

        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        repositoryService.deleteDeployment(deploymentOneId, true);
        repositoryService.deleteDeployment(deploymentTwoId, true);
    }

    public void testQueryByNameLike() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // nameLike
            org.flowable.task.api.Task task = taskService.createTaskQuery().taskNameLike("%\\%%").singleResult();
            assertNotNull(task);
            assertEquals(task1.getId(), task.getId());

            task = taskService.createTaskQuery().taskNameLike("%\\_%").singleResult();
            assertNotNull(task);
            assertEquals(task2.getId(), task.getId());

            // orQuery
            task = taskService.createTaskQuery().or().taskNameLike("%\\%%").processDefinitionId("undefined").singleResult();
            assertNotNull(task);
            assertEquals(task1.getId(), task.getId());

            task = taskService.createTaskQuery().or().taskNameLike("%\\_%").processDefinitionId("undefined").singleResult();
            assertNotNull(task);
            assertEquals(task2.getId(), task.getId());
        }
    }

    public void testQueryByNameLikeIgnoreCase() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // nameLikeIgnoreCase
            org.flowable.task.api.Task task = taskService.createTaskQuery().taskNameLikeIgnoreCase("%\\%%").singleResult();
            assertNotNull(task);
            assertEquals(task1.getId(), task.getId());

            task = taskService.createTaskQuery().taskNameLikeIgnoreCase("%\\_%").singleResult();
            assertNotNull(task);
            assertEquals(task2.getId(), task.getId());

            // orQuery
            task = taskService.createTaskQuery().or().taskNameLikeIgnoreCase("%\\%%").processDefinitionId("undefined").singleResult();
            assertNotNull(task);
            assertEquals(task1.getId(), task.getId());

            task = taskService.createTaskQuery().or().taskNameLikeIgnoreCase("%\\_%").processDefinitionId("undefined").singleResult();
            assertNotNull(task);
            assertEquals(task2.getId(), task.getId());
        }
    }

    public void testQueryByDescriptionLike() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // descriptionLike
            org.flowable.task.api.Task task = taskService.createTaskQuery().taskDescriptionLike("%\\%%").singleResult();
            assertNotNull(task);
            assertEquals(task1.getId(), task.getId());

            task = taskService.createTaskQuery().taskDescriptionLike("%\\_%").singleResult();
            assertNotNull(task);
            assertEquals(task2.getId(), task.getId());

            // orQuery
            task = taskService.createTaskQuery().or().taskDescriptionLike("%\\%%").processDefinitionId("undefined").singleResult();
            assertNotNull(task);
            assertEquals(task1.getId(), task.getId());

            task = taskService.createTaskQuery().or().taskDescriptionLike("%\\_%").processDefinitionId("undefined").singleResult();
            assertNotNull(task);
            assertEquals(task2.getId(), task.getId());
        }
    }

    public void testQueryByDescriptionLikeIgnoreCase() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // descriptionLikeIgnoreCase
            org.flowable.task.api.Task task = taskService.createTaskQuery().taskDescriptionLikeIgnoreCase("%\\%%").singleResult();
            assertNotNull(task);
            assertEquals(task1.getId(), task.getId());

            task = taskService.createTaskQuery().taskDescriptionLikeIgnoreCase("%\\_%").singleResult();
            assertNotNull(task);
            assertEquals(task2.getId(), task.getId());

            // orQuery
            task = taskService.createTaskQuery().or().taskDescriptionLikeIgnoreCase("%\\%%").processDefinitionId("undefined").singleResult();
            assertNotNull(task);
            assertEquals(task1.getId(), task.getId());

            task = taskService.createTaskQuery().or().taskDescriptionLikeIgnoreCase("%\\_%").processDefinitionId("undefined").singleResult();
            assertNotNull(task);
            assertEquals(task2.getId(), task.getId());
        }
    }

    public void testQueryByAssigneeLike() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // assigneeLike
            org.flowable.task.api.Task task = taskService.createTaskQuery().taskAssigneeLike("%\\%%").singleResult();
            assertNotNull(task);
            assertEquals(task1.getId(), task.getId());

            task = taskService.createTaskQuery().taskAssigneeLike("%\\_%").singleResult();
            assertNotNull(task);
            assertEquals(task2.getId(), task.getId());

            // orQuery
            /*
             * task = taskService.createTaskQuery().or().taskAssigneeLike("%\\%%").processDefinitionId("undefined").singleResult(); assertNotNull(task); assertEquals(task1.getId(), task.getId());
             * 
             * task = taskService.createTaskQuery().or().taskAssigneeLike("%\\_%").processDefinitionId("undefined").singleResult(); assertNotNull(task); assertEquals(task2.getId(), task.getId());
             */
        }
    }

    public void testQueryByAssigneeLikeIgnoreCase() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // assigneeLikeIgnoreCase
            org.flowable.task.api.Task task = taskService.createTaskQuery().taskAssigneeLike("%\\%%").singleResult();
            assertNotNull(task);
            assertEquals(task1.getId(), task.getId());

            task = taskService.createTaskQuery().taskAssigneeLike("%\\_%").singleResult();
            assertNotNull(task);
            assertEquals(task2.getId(), task.getId());

            // orQuery
            /*
             * task = taskService.createTaskQuery().or().taskAssigneeLike("%\\%%").processDefinitionId("undefined").singleResult(); assertNotNull(task); assertEquals(task1.getId(), task.getId());
             * 
             * task = taskService.createTaskQuery().or().taskAssigneeLike("%\\_%").processDefinitionId("undefined").singleResult(); assertNotNull(task); assertEquals(task2.getId(), task.getId());
             */
        }
    }

    public void testQueryByOwnerLike() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // taskOwnerLike
            org.flowable.task.api.Task task = taskService.createTaskQuery().taskOwnerLike("%\\%%").singleResult();
            assertNotNull(task);
            assertEquals(task1.getId(), task.getId());

            task = taskService.createTaskQuery().taskOwnerLike("%\\_%").singleResult();
            assertNotNull(task);
            assertEquals(task2.getId(), task.getId());

            // orQuery
            task = taskService.createTaskQuery().or().taskOwnerLike("%\\%%").processDefinitionId("undefined").singleResult();
            assertNotNull(task);
            assertEquals(task1.getId(), task.getId());

            task = taskService.createTaskQuery().or().taskOwnerLike("%\\_%").processDefinitionId("undefined").singleResult();
            assertNotNull(task);
            assertEquals(task2.getId(), task.getId());
        }
    }

    public void testQueryByOwnerLikeIgnoreCase() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // taskOwnerLikeIgnoreCase
            org.flowable.task.api.Task task = taskService.createTaskQuery().taskOwnerLikeIgnoreCase("%\\%%").singleResult();
            assertNotNull(task);
            assertEquals(task1.getId(), task.getId());

            task = taskService.createTaskQuery().taskOwnerLikeIgnoreCase("%\\_%").singleResult();
            assertNotNull(task);
            assertEquals(task2.getId(), task.getId());

            // orQuery
            task = taskService.createTaskQuery().or().taskOwnerLikeIgnoreCase("%\\%%").processDefinitionId("undefined").singleResult();
            assertNotNull(task);
            assertEquals(task1.getId(), task.getId());

            task = taskService.createTaskQuery().or().taskOwnerLikeIgnoreCase("%\\_%").processDefinitionId("undefined").singleResult();
            assertNotNull(task);
            assertEquals(task2.getId(), task.getId());
        }
    }

    public void testQueryByProcessInstanceBusinessKeyLike() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // processInstanceBusinessKeyLike
            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceBusinessKeyLike("%\\%%").singleResult();
            assertNotNull(task);
            assertEquals(task1.getId(), task.getId());

            task = taskService.createTaskQuery().processInstanceBusinessKeyLike("%\\_%").singleResult();
            assertNotNull(task);
            assertEquals(task2.getId(), task.getId());

            // orQuery
            task = taskService.createTaskQuery().or().processInstanceBusinessKeyLike("%\\%%").processDefinitionId("undefined").singleResult();
            assertNotNull(task);
            assertEquals(task1.getId(), task.getId());

            task = taskService.createTaskQuery().or().processInstanceBusinessKeyLike("%\\_%").processDefinitionId("undefined").singleResult();
            assertNotNull(task);
            assertEquals(task2.getId(), task.getId());
        }
    }

    public void testQueryByProcessInstanceBusinessKeyLikeIgnoreCase() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // processInstanceBusinessKeyLike
            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceBusinessKeyLikeIgnoreCase("%\\%%").singleResult();
            assertNotNull(task);
            assertEquals(task1.getId(), task.getId());

            task = taskService.createTaskQuery().processInstanceBusinessKeyLikeIgnoreCase("%\\_%").singleResult();
            assertNotNull(task);
            assertEquals(task2.getId(), task.getId());

            // orQuery
            /*
             * task = taskService.createTaskQuery().or().processInstanceBusinessKeyLikeIgnoreCase("%\\%%").processDefinitionId("undefined").singleResult(); assertNotNull(task);
             * assertEquals(task1.getId(), task.getId());
             * 
             * task = taskService.createTaskQuery().or().processInstanceBusinessKeyLikeIgnoreCase("%\\_%").processDefinitionId("undefined").singleResult(); assertNotNull(task);
             * assertEquals(task2.getId(), task.getId());
             */
        }
    }

    public void testQueryByKeyLike() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // taskDefinitionKeyLike
            org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKeyLike("%\\%%").singleResult();
            assertNull(task);

            task = taskService.createTaskQuery().taskDefinitionKeyLike("%\\_%").singleResult();
            assertNull(task);

            // orQuery
            task = taskService.createTaskQuery().or().taskDefinitionKeyLike("%\\%%").processDefinitionId("undefined").singleResult();
            assertNull(task);

            task = taskService.createTaskQuery().or().taskDefinitionKeyLike("%\\_%").processDefinitionId("undefined").singleResult();
            assertNull(task);
        }
    }

    public void testQueryByProcessDefinitionKeyLike() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // processDefinitionKeyLike
            org.flowable.task.api.Task task = taskService.createTaskQuery().processDefinitionKeyLike("%\\%%").singleResult();
            assertNull(task);

            task = taskService.createTaskQuery().processDefinitionKeyLike("%\\_%").singleResult();
            assertNull(task);

            // orQuery
            task = taskService.createTaskQuery().or().processDefinitionKeyLike("%\\%%").processDefinitionId("undefined").singleResult();
            assertNull(task);

            task = taskService.createTaskQuery().or().processDefinitionKeyLike("%\\_%").processDefinitionId("undefined").singleResult();
            assertNull(task);
        }
    }

    public void testQueryByProcessDefinitionKeyLikeIgnoreCase() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // processDefinitionKeyLikeIgnoreCase
            org.flowable.task.api.Task task = taskService.createTaskQuery().processDefinitionKeyLikeIgnoreCase("%\\%%").singleResult();
            assertNull(task);

            task = taskService.createTaskQuery().processDefinitionKeyLikeIgnoreCase("%\\_%").singleResult();
            assertNull(task);

            // orQuery
            task = taskService.createTaskQuery().or().processDefinitionKeyLikeIgnoreCase("%\\%%").processDefinitionId("undefined").singleResult();
            assertNull(task);

            task = taskService.createTaskQuery().or().processDefinitionKeyLikeIgnoreCase("%\\_%").processDefinitionId("undefined").singleResult();
            assertNull(task);
        }
    }

    public void testQueryByProcessDefinitionNameLike() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // processDefinitionNameLike
            List<org.flowable.task.api.Task> list = taskService.createTaskQuery().processDefinitionNameLike("%\\%%").orderByTaskCreateTime().asc().list();
            // only check for existence and assume that the SQL processing has ordered the values correctly
            // see https://github.com/flowable/flowable-engine/issues/8
            ArrayList tasks = new ArrayList(2);
            tasks.add(list.get(0).getId());
            tasks.add(list.get(1).getId());
            assertTrue(tasks.contains(task1.getId()));
            assertTrue(tasks.contains(task2.getId()));

            // orQuery
            list = taskService.createTaskQuery().or().processDefinitionNameLike("%\\%%").processDefinitionId("undefined").orderByTaskCreateTime().asc().list();
            assertEquals(2, list.size());
            // only check for existence and assume that the SQL processing has ordered the values correctly
            // see https://github.com/flowable/flowable-engine/issues/8
            tasks = new ArrayList(2);
            tasks.add(list.get(0).getId());
            tasks.add(list.get(1).getId());
            assertTrue(tasks.contains(task1.getId()));
            assertTrue(tasks.contains(task2.getId()));
        }
    }

    public void testQueryLikeByQueryVariableValue() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // taskVariableValueLike
            org.flowable.task.api.Task task = taskService.createTaskQuery().taskVariableValueLike("var1", "%\\%%").singleResult();
            assertNotNull(task);
            assertEquals(task1.getId(), task.getId());

            task = taskService.createTaskQuery().taskVariableValueLike("var1", "%\\_%").singleResult();
            assertNotNull(task);
            assertEquals(task2.getId(), task.getId());

            // orQuery
            task = taskService.createTaskQuery().or().taskVariableValueLike("var1", "%\\%%").processDefinitionId("undefined").singleResult();
            assertNotNull(task);
            assertEquals(task1.getId(), task.getId());

            task = taskService.createTaskQuery().or().taskVariableValueLike("var1", "%\\_%").processDefinitionId("undefined").singleResult();
            assertNotNull(task);
            assertEquals(task2.getId(), task.getId());
        }
    }

    public void testQueryLikeIgnoreCaseByQueryVariableValue() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // taskVariableValueLikeIgnoreCase
            org.flowable.task.api.Task task = taskService.createTaskQuery().taskVariableValueLikeIgnoreCase("var1", "%\\%%").singleResult();
            assertNotNull(task);
            assertEquals(task1.getId(), task.getId());

            task = taskService.createTaskQuery().taskVariableValueLikeIgnoreCase("var1", "%\\_%").singleResult();
            assertNotNull(task);
            assertEquals(task2.getId(), task.getId());

            // orQuery
            task = taskService.createTaskQuery().or().taskVariableValueLikeIgnoreCase("var1", "%\\%%").processDefinitionId("undefined").singleResult();
            assertNotNull(task);
            assertEquals(task1.getId(), task.getId());

            task = taskService.createTaskQuery().or().taskVariableValueLikeIgnoreCase("var1", "%\\_%").processDefinitionId("undefined").singleResult();
            assertNotNull(task);
            assertEquals(task2.getId(), task.getId());
        }
    }
}
