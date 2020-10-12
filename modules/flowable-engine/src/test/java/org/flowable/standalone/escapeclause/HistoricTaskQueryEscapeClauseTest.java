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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.TaskCompletionBuilder;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HistoricTaskQueryEscapeClauseTest extends AbstractEscapeClauseTestCase {

    private String deploymentOneId;

    private String deploymentTwoId;

    private ProcessInstance processInstance1;

    private ProcessInstance processInstance2;

    private org.flowable.task.api.Task task1;

    private org.flowable.task.api.Task task2;

    private org.flowable.task.api.Task task3;

    private org.flowable.task.api.Task task4;

    @BeforeEach
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

        Map<String, Object> vars1 = new HashMap<>();
        vars1.put("var1", "One%");
        Map<String, Object> vars2 = new HashMap<>();
        vars2.put("var1", "Two_");

        task1 = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        taskService.setAssignee(task1.getId(), "assignee%");
        taskService.setOwner(task1.getId(), "owner%");

        TaskCompletionBuilder taskCompletionBuilder = taskService.createTaskCompletionBuilder();
        taskCompletionBuilder.variablesLocal(vars1);
        taskCompletionBuilder.taskId(task1.getId());
        taskCompletionBuilder.complete();

        task2 = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        taskService.setAssignee(task2.getId(), "assignee_");
        taskService.setOwner(task2.getId(), "owner_");

        taskCompletionBuilder = taskService.createTaskCompletionBuilder();
        taskCompletionBuilder.variablesLocal(vars2);
        taskCompletionBuilder.taskId(task2.getId());
        taskCompletionBuilder.complete();

        task3 = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        taskService.setAssignee(task3.getId(), "assignee%");
        taskService.setOwner(task3.getId(), "owner%");

        taskCompletionBuilder = taskService.createTaskCompletionBuilder();
        taskCompletionBuilder.variablesLocal(vars1);
        taskCompletionBuilder.taskId(task3.getId());
        taskCompletionBuilder.complete();

        task4 = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        taskService.setAssignee(task4.getId(), "assignee_");
        taskService.setOwner(task4.getId(), "owner_");

        taskCompletionBuilder = taskService.createTaskCompletionBuilder();
        taskCompletionBuilder.variablesLocal(vars2);
        taskCompletionBuilder.taskId(task4.getId());
        taskCompletionBuilder.complete();
    }

    @AfterEach
    protected void tearDown() throws Exception {
        repositoryService.deleteDeployment(deploymentOneId, true);
        repositoryService.deleteDeployment(deploymentTwoId, true);
    }

    @Test
    public void testQueryByProcessDefinitionKeyLike() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // processDefinitionKeyLike
            List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().processDefinitionKeyLike("%|%%").list();
            assertThat(list).isEmpty();

            list = historyService.createHistoricTaskInstanceQuery().processDefinitionKeyLike("%|_%").list();
            assertThat(list).isEmpty();

            // orQuery
            list = historyService.createHistoricTaskInstanceQuery().or().processDefinitionKeyLike("%|%%").processDefinitionId("undefined").list();
            assertThat(list).isEmpty();

            list = historyService.createHistoricTaskInstanceQuery().or().processDefinitionKeyLike("%|_%").processDefinitionId("undefined").list();
            assertThat(list).isEmpty();
        }
    }

    @Test
    public void testQueryByProcessDefinitionKeyLikeIgnoreCase() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // processDefinitionKeyLikeIgnoreCase
            List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().processDefinitionKeyLikeIgnoreCase("%|%%").list();
            assertThat(list).isEmpty();

            list = historyService.createHistoricTaskInstanceQuery().processDefinitionKeyLikeIgnoreCase("%|_%").list();
            assertThat(list).isEmpty();

            // orQuery
            list = historyService.createHistoricTaskInstanceQuery().or().processDefinitionKeyLikeIgnoreCase("%|%%").processDefinitionId("undefined").list();
            assertThat(list).isEmpty();

            list = historyService.createHistoricTaskInstanceQuery().or().processDefinitionKeyLikeIgnoreCase("%|_%").processDefinitionId("undefined").list();
            assertThat(list).isEmpty();
        }
    }

    @Test
    public void testQueryByProcessDefinitionNameLike() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // processDefinitionNameLike
            List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().processDefinitionNameLike("%|%%")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            // only check for existence and assume that the SQL processing has ordered the values correctly
            // see https://github.com/flowable/flowable-engine/issues/8
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task2.getId(), task3.getId(), task4.getId());

            // orQuery
            list = historyService.createHistoricTaskInstanceQuery().or().processDefinitionNameLike("%|%%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task2.getId(), task3.getId(), task4.getId());
        }
    }

    @Test
    public void testQueryByProcessInstanceBusinessKeyLike() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // processInstanceBusinessKeyLike
            List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().processInstanceBusinessKeyLike("%|%%")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            // only check for existence and assume that the SQL processing has ordered the values correctly
            // see https://github.com/flowable/flowable-engine/issues/8
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task2.getId());

            list = historyService.createHistoricTaskInstanceQuery().processInstanceBusinessKeyLike("%|_%").orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task3.getId(), task4.getId());

            // orQuery
            list = historyService.createHistoricTaskInstanceQuery().or().processInstanceBusinessKeyLike("%|%%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task2.getId());

            list = historyService.createHistoricTaskInstanceQuery().or().processInstanceBusinessKeyLike("%|_%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task3.getId(), task4.getId());
        }
    }

    @Test
    public void testQueryByProcessInstanceBusinessKeyLikeIgnoreCase() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // processInstanceBusinessKeyLikeIgnoreCase
            List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().processInstanceBusinessKeyLikeIgnoreCase("%|%%")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list).hasSize(2);
            // only check for existence and assume that the SQL processing has ordered the values correctly
            // see https://github.com/flowable/flowable-engine/issues/8
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task2.getId());

            list = historyService.createHistoricTaskInstanceQuery().processInstanceBusinessKeyLikeIgnoreCase("%|_%").orderByHistoricTaskInstanceStartTime()
                    .asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task3.getId(), task4.getId());

            // orQuery
            list = historyService.createHistoricTaskInstanceQuery().or().processInstanceBusinessKeyLikeIgnoreCase("%|%%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task2.getId());

            list = historyService.createHistoricTaskInstanceQuery().or().processInstanceBusinessKeyLikeIgnoreCase("%|_%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task3.getId(), task4.getId());
        }
    }

    @Test
    public void testQueryByTaskDefinitionKeyLike() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // taskDefinitionKeyLike
            List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().taskDefinitionKeyLike("%|%%").list();
            assertThat(list).isEmpty();

            list = historyService.createHistoricTaskInstanceQuery().taskDefinitionKeyLike("%|_%").list();
            assertThat(list).isEmpty();

            // orQuery
            list = historyService.createHistoricTaskInstanceQuery().or().taskDefinitionKeyLike("%|%%").processDefinitionId("undefined").list();
            assertThat(list).isEmpty();

            list = historyService.createHistoricTaskInstanceQuery().or().taskDefinitionKeyLike("%|_%").processDefinitionId("undefined").list();
            assertThat(list).isEmpty();
        }
    }

    @Test
    public void testQueryByTaskNameLike() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // taskNameLike
            List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().taskNameLike("%|%%").orderByHistoricTaskInstanceStartTime().asc()
                    .list();
            // only check for existence and assume that the SQL processing has ordered the values correctly
            // see https://github.com/flowable/flowable-engine/issues/8
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task3.getId());

            list = historyService.createHistoricTaskInstanceQuery().taskNameLike("%|_%").orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task2.getId(), task4.getId());

            // orQuery
            list = historyService.createHistoricTaskInstanceQuery().or().taskNameLike("%|%%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task3.getId());

            list = historyService.createHistoricTaskInstanceQuery().or().taskNameLike("%|_%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task2.getId(), task4.getId());
        }
    }

    @Test
    public void testQueryByTaskNameLikeIgnoreCase() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // taskNameLikeIgnoreCase
            List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().taskNameLikeIgnoreCase("%|%%")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            // only check for existence and assume that the SQL processing has ordered the values correctly
            // see https://github.com/flowable/flowable-engine/issues/8
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task3.getId());

            list = historyService.createHistoricTaskInstanceQuery().taskNameLikeIgnoreCase("%|_%").orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task2.getId(), task4.getId());

            // orQuery
            list = historyService.createHistoricTaskInstanceQuery().or().taskNameLikeIgnoreCase("%|%%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task3.getId());

            list = historyService.createHistoricTaskInstanceQuery().or().taskNameLikeIgnoreCase("%|_%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task2.getId(), task4.getId());
        }
    }

    @Test
    public void testQueryByTaskDescriptionLike() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // taskDescriptionLike
            List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().taskDescriptionLike("%|%%")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            // only check for existence and assume that the SQL processing has ordered the values correctly
            // see https://github.com/flowable/flowable-engine/issues/8
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task3.getId());

            list = historyService.createHistoricTaskInstanceQuery().taskDescriptionLike("%|_%").orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task2.getId(), task4.getId());

            // orQuery
            list = historyService.createHistoricTaskInstanceQuery().or().taskDescriptionLike("%|%%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task3.getId());

            list = historyService.createHistoricTaskInstanceQuery().or().taskDescriptionLike("%|_%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task2.getId(), task4.getId());
        }
    }

    @Test
    public void testQueryByTaskDescriptionLikeIgnoreCase() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // taskDescriptionLikeIgnoreCase
            List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().taskDescriptionLikeIgnoreCase("%|%%")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            // only check for existence and assume that the SQL processing has ordered the values correctly
            // see https://github.com/flowable/flowable-engine/issues/8
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task3.getId());

            list = historyService.createHistoricTaskInstanceQuery().taskDescriptionLikeIgnoreCase("%|_%").orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task2.getId(), task4.getId());

            // orQuery
            list = historyService.createHistoricTaskInstanceQuery().or().taskDescriptionLikeIgnoreCase("%|%%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task3.getId());

            list = historyService.createHistoricTaskInstanceQuery().or().taskDescriptionLikeIgnoreCase("%|_%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task2.getId(), task4.getId());
        }
    }

    @Test
    public void testQueryByTaskDeleteReasonLike() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // make test data
            org.flowable.task.api.Task task5 = taskService.newTask("task5");
            taskService.saveTask(task5);
            taskService.deleteTask(task5.getId(), "deleteReason%");
            org.flowable.task.api.Task task6 = taskService.newTask("task6");
            taskService.saveTask(task6);
            taskService.deleteTask(task6.getId(), "deleteReason_");

            // taskDeleteReasonLike
            HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery().taskDeleteReasonLike("%|%%").singleResult();
            assertThat(historicTask).isNotNull();
            assertThat(historicTask.getId()).isEqualTo(task5.getId());

            historicTask = historyService.createHistoricTaskInstanceQuery().taskDeleteReasonLike("%|_%").singleResult();
            assertThat(historicTask).isNotNull();
            assertThat(historicTask.getId()).isEqualTo(task6.getId());

            // orQuery
            historicTask = historyService.createHistoricTaskInstanceQuery().or().taskDeleteReasonLike("%|%%").processDefinitionId("undefined").singleResult();
            assertThat(historicTask).isNotNull();
            assertThat(historicTask.getId()).isEqualTo(task5.getId());

            historicTask = historyService.createHistoricTaskInstanceQuery().or().taskDeleteReasonLike("%|_%").processDefinitionId("undefined").singleResult();
            assertThat(historicTask).isNotNull();
            assertThat(historicTask.getId()).isEqualTo(task6.getId());

            // clean
            historyService.deleteHistoricTaskInstance(task5.getId());
            managementService.executeCommand(commandContext -> {
                processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().deleteHistoricTaskLogEntriesForTaskId(task5.getId());
                return null;
            });
            historyService.deleteHistoricTaskInstance(task6.getId());
            managementService.executeCommand(commandContext -> {
                processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().deleteHistoricTaskLogEntriesForTaskId(task6.getId());
                return null;
            });
        }
    }

    @Test
    public void testQueryByTaskOwnerLike() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // taskOwnerLike
            List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().taskOwnerLike("%|%%").orderByHistoricTaskInstanceStartTime()
                    .asc().list();
            // only check for existence and assume that the SQL processing has ordered the values correctly
            // see https://github.com/flowable/flowable-engine/issues/8
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task3.getId());

            list = historyService.createHistoricTaskInstanceQuery().taskOwnerLike("%|_%").orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task2.getId(), task4.getId());

            // orQuery
            list = historyService.createHistoricTaskInstanceQuery().or().taskOwnerLike("%|%%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list).hasSize(2);
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task3.getId());

            list = historyService.createHistoricTaskInstanceQuery().or().taskOwnerLike("%|_%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task2.getId(), task4.getId());
        }
    }

    @Test
    public void testQueryByTaskOwnerLikeIgnoreCase() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // taskOwnerLikeIgnoreCase
            List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().taskOwnerLikeIgnoreCase("%|%%")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            // only check for existence and assume that the SQL processing has ordered the values correctly
            // see https://github.com/flowable/flowable-engine/issues/8
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task3.getId());

            list = historyService.createHistoricTaskInstanceQuery().taskOwnerLikeIgnoreCase("%|_%").orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task2.getId(), task4.getId());

            // orQuery
            list = historyService.createHistoricTaskInstanceQuery().or().taskOwnerLikeIgnoreCase("%|%%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task3.getId());

            list = historyService.createHistoricTaskInstanceQuery().or().taskOwnerLikeIgnoreCase("%|_%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task2.getId(), task4.getId());
        }
    }

    @Test
    public void testQueryByTaskAssigneeLike() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // taskAssigneeLike
            List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().taskAssigneeLike("%|%%").orderByHistoricTaskInstanceStartTime()
                    .asc().list();
            // only check for existence and assume that the SQL processing has ordered the values correctly
            // see https://github.com/flowable/flowable-engine/issues/8
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task3.getId());

            list = historyService.createHistoricTaskInstanceQuery().taskAssigneeLike("%|_%").orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task2.getId(), task4.getId());

            // orQuery
            list = historyService.createHistoricTaskInstanceQuery().or().taskAssigneeLike("%|%%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task3.getId());

            list = historyService.createHistoricTaskInstanceQuery().or().taskAssigneeLike("%|_%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task2.getId(), task4.getId());
        }
    }

    @Test
    public void testQueryByTaskAssigneeLikeIgnoreCase() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // taskAssigneeLikeIgnoreCase
            List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().taskAssigneeLikeIgnoreCase("%|%%")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            // only check for existence and assume that the SQL processing has ordered the values correctly
            // see https://github.com/flowable/flowable-engine/issues/8
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task3.getId());

            list = historyService.createHistoricTaskInstanceQuery().taskAssigneeLikeIgnoreCase("%|_%").orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task2.getId(), task4.getId());

            // orQuery
            list = historyService.createHistoricTaskInstanceQuery().or().taskAssigneeLikeIgnoreCase("%|%%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task3.getId());

            list = historyService.createHistoricTaskInstanceQuery().or().taskAssigneeLikeIgnoreCase("%|_%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task2.getId(), task4.getId());
        }
    }

    @Test
    public void testQueryByTenantIdLike() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // tenantIdLike
            List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().taskTenantIdLike("%|%%").orderByHistoricTaskInstanceStartTime()
                    .asc().list();
            // only check for existence and assume that the SQL processing has ordered the values correctly
            // see https://github.com/flowable/flowable-engine/issues/8
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task2.getId());

            list = historyService.createHistoricTaskInstanceQuery().taskTenantIdLike("%|_%").orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task3.getId(), task4.getId());

            // orQuery
            list = historyService.createHistoricTaskInstanceQuery().or().taskTenantIdLike("%|%%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task2.getId());

            list = historyService.createHistoricTaskInstanceQuery().or().taskTenantIdLike("%|_%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task3.getId(), task4.getId());
        }
    }

    @Test
    public void testQueryLikeByQueryVariableValue() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // variableValueLike
            List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().taskVariableValueLike("var1", "%|%%")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            // only check for existence and assume that the SQL processing has ordered the values correctly
            // see https://github.com/flowable/flowable-engine/issues/8
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task3.getId());

            list = historyService.createHistoricTaskInstanceQuery().taskVariableValueLike("var1", "%|_%").orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task2.getId(), task4.getId());

            // orQuery
            list = historyService.createHistoricTaskInstanceQuery().or().taskVariableValueLike("var1", "%|%%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task3.getId());

            list = historyService.createHistoricTaskInstanceQuery().or().taskVariableValueLike("var1", "%|_%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task2.getId(), task4.getId());
        }
    }

    @Test
    public void testQueryLikeIgnoreCaseByQueryVariableValue() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // variableValueLikeIgnoreCase
            List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery().taskVariableValueLikeIgnoreCase("var1", "%|%%")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            // only check for existence and assume that the SQL processing has ordered the values correctly
            // see https://github.com/flowable/flowable-engine/issues/8
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task3.getId());

            list = historyService.createHistoricTaskInstanceQuery().taskVariableValueLikeIgnoreCase("var1", "%|_%").orderByHistoricTaskInstanceStartTime().asc()
                    .list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task2.getId(), task4.getId());

            // orQuery
            list = historyService.createHistoricTaskInstanceQuery().or().taskVariableValueLikeIgnoreCase("var1", "%|%%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task1.getId(), task3.getId());

            list = historyService.createHistoricTaskInstanceQuery().or().taskVariableValueLikeIgnoreCase("var1", "%|_%").processDefinitionId("undefined")
                    .orderByHistoricTaskInstanceStartTime().asc().list();
            assertThat(list)
                    .extracting(HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(task2.getId(), task4.getId());
        }
    }
}
