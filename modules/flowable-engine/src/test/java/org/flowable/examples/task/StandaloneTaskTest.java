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
package org.flowable.examples.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class StandaloneTaskTest extends PluggableFlowableTestCase {

    @BeforeEach
    public void setUp() throws Exception {
        identityService.saveUser(identityService.newUser("kermit"));
        identityService.saveUser(identityService.newUser("gonzo"));
    }

    @AfterEach
    public void tearDown() throws Exception {
        identityService.deleteUser("kermit");
        identityService.deleteUser("gonzo");
    }

    @Test
    public void testCreateToComplete() {

        // Create and save task
        org.flowable.task.api.Task task = taskService.newTask();
        task.setName("testTask");
        taskService.saveTask(task);
        String taskId = task.getId();

        // Add user as candidate user
        taskService.addCandidateUser(taskId, "kermit");
        taskService.addCandidateUser(taskId, "gonzo");

        // Retrieve task list for kermit
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskCandidateUser("kermit").list();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getName()).isEqualTo("testTask");

        // Retrieve task list for gonzo
        tasks = taskService.createTaskQuery().taskCandidateUser("gonzo").list();
        assertThat(tasks).hasSize(1);

        task = tasks.get(0);
        assertThat(task.getName()).isEqualTo("testTask");

        task.setName("Update name");
        taskService.saveTask(task);
        tasks = taskService.createTaskQuery().taskCandidateUser("kermit").list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Update name");

        // Claim task
        taskService.claim(taskId, "kermit");

        // Tasks shouldn't appear in the candidate tasklists anymore
        assertThat(taskService.createTaskQuery().taskCandidateUser("kermit").list()).isEmpty();
        assertThat(taskService.createTaskQuery().taskCandidateUser("gonzo").list()).isEmpty();

        // Complete task
        taskService.deleteTask(taskId, true);

        // org.flowable.task.service.Task should be removed from runtime data
        // TODO: check for historic data when implemented!
        assertThat(taskService.createTaskQuery().taskId(taskId).singleResult()).isNull();
    }

    @Test
    public void testOptimisticLockingThrownOnMultipleUpdates() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);
        String taskId = task.getId();

        // first modification
        org.flowable.task.api.Task task1 = taskService.createTaskQuery().taskId(taskId).singleResult();
        org.flowable.task.api.Task task2 = taskService.createTaskQuery().taskId(taskId).singleResult();

        task1.setDescription("first modification");
        taskService.saveTask(task1);

        // second modification on the initial instance
        task2.setDescription("second modification");
        assertThatThrownBy(() -> taskService.saveTask(task2))
                .as("should get an exception here as the task was modified by someone else.")
                .isInstanceOf(FlowableOptimisticLockingException.class);

        taskService.deleteTask(taskId, true);
    }

    // See https://activiti.atlassian.net/browse/ACT-1290
    @Test
    public void testRevisionUpdatedOnSave() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);
        assertThat(((TaskEntity) task).getRevision()).isEqualTo(1);

        task.setDescription("first modification");
        taskService.saveTask(task);
        assertThat(((TaskEntity) task).getRevision()).isEqualTo(2);

        task.setDescription("second modification");
        taskService.saveTask(task);
        assertThat(((TaskEntity) task).getRevision()).isEqualTo(3);

        taskService.deleteTask(task.getId(), true);
    }

    // See https://activiti.atlassian.net/browse/ACT-1290
    @Test
    public void testRevisionUpdatedOnSaveWhenFetchedUsingQuery() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);
        assertThat(((TaskEntity) task).getRevision()).isEqualTo(1);

        task.setAssignee("kermit");
        taskService.saveTask(task);
        assertThat(((TaskEntity) task).getRevision()).isEqualTo(2);

        // Now fetch the task through the query api
        task = taskService.createTaskQuery().singleResult();
        assertThat(((TaskEntity) task).getRevision()).isEqualTo(2);
        task.setPriority(1);
        taskService.saveTask(task);

        assertThat(((TaskEntity) task).getRevision()).isEqualTo(3);

        taskService.deleteTask(task.getId(), true);
    }

    @Test
    public void testHistoricVariableOkOnUpdate() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            // 1. create a task
            org.flowable.task.api.Task task = taskService.newTask();
            task.setName("test execution");
            task.setOwner("josOwner");
            task.setAssignee("JosAssignee");
            taskService.saveTask(task);

            // 2. set task variables
            Map<String, Object> taskVariables = new HashMap<>();
            taskVariables.put("finishedAmount", 0);
            taskService.setVariables(task.getId(), taskVariables);

            // 3. complete this task with a new variable
            Map<String, Object> finishVariables = new HashMap<>();
            finishVariables.put("finishedAmount", 40);
            taskService.complete(task.getId(), finishVariables);

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            // 4. get completed variable
            List<HistoricVariableInstance> hisVarList = historyService.createHistoricVariableInstanceQuery().taskId(task.getId()).list();
            assertThat(hisVarList)
                    .extracting(HistoricVariableInstance::getValue)
                    .containsExactly(40);

            // Cleanup
            historyService.deleteHistoricTaskInstance(task.getId());
            managementService.executeCommand(commandContext -> {
                processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().deleteHistoricTaskLogEntriesForTaskId(task.getId());
                return null;
            });
        }
    }

}
