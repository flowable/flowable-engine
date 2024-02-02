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
package org.flowable.engine.test.api.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class TaskAndVariablesQueryTest extends PluggableFlowableTestCase {

    private List<String> taskIds;
    private List<String> multipleTaskIds;

    @BeforeEach
    public void setUp() throws Exception {

        identityService.saveUser(identityService.newUser("kermit"));
        identityService.saveUser(identityService.newUser("gonzo"));
        identityService.saveUser(identityService.newUser("fozzie"));

        identityService.saveGroup(identityService.newGroup("management"));
        identityService.saveGroup(identityService.newGroup("accountancy"));

        identityService.createMembership("kermit", "management");
        identityService.createMembership("kermit", "accountancy");
        identityService.createMembership("fozzie", "management");

        taskIds = generateTestTasks();
    }

    @AfterEach
    public void tearDown() throws Exception {
        identityService.deleteGroup("accountancy");
        identityService.deleteGroup("management");
        identityService.deleteUser("fozzie");
        identityService.deleteUser("gonzo");
        identityService.deleteUser("kermit");
        taskService.deleteTasks(taskIds, true);
    }

    @Test
    @Deployment
    public void testQuery() {
        Task task = taskService.createTaskQuery().includeTaskLocalVariables().taskAssignee("gonzo").singleResult();
        assertThat(task.getProcessVariables()).isEmpty();
        Map<String, Object> variableMap = task.getTaskLocalVariables();
        assertThat(variableMap)
                .hasSize(3)
                .contains(
                        entry("testVar", "someVariable"),
                        entry("testVar2", 123)
                );
        assertThat(new String((byte[]) variableMap.get("testVarBinary"))).isEqualTo("This is a binary variable");

        List<Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(3);

        task = taskService.createTaskQuery().includeProcessVariables().taskAssignee("gonzo").singleResult();
        assertThat(task.getProcessVariables()).isEmpty();
        assertThat(task.getTaskLocalVariables()).isEmpty();

        Map<String, Object> startMap = new HashMap<>();
        startMap.put("processVar", true);
        startMap.put("binaryVariable", "This is a binary process variable".getBytes());
        runtimeService.startProcessInstanceByKey("oneTaskProcess", startMap);

        task = taskService.createTaskQuery().includeProcessVariables().taskAssignee("kermit").singleResult();
        assertThat(task.getProcessVariables()).hasSize(2);
        assertThat(task.getTaskLocalVariables()).isEmpty();
        assertThat((Boolean) task.getProcessVariables().get("processVar")).isTrue();
        assertThat(new String((byte[]) task.getProcessVariables().get("binaryVariable"))).isEqualTo("This is a binary process variable");

        String taskId = task.getId();
        taskService.setVariable(taskId, "anotherProcessVar", 123);
        taskService.setVariableLocal(taskId, "localVar", "test");

        assertThatThrownBy(() -> taskService.setVariableLocal(taskId, null, null))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("variableName is null");

        task = taskService.createTaskQuery().includeTaskLocalVariables().taskAssignee("kermit").singleResult();
        assertThat(task.getProcessVariables()).isEmpty();
        assertThat(task.getTaskLocalVariables())
                .containsOnly(
                        entry("localVar", "test")
                );

        task = taskService.createTaskQuery().includeProcessVariables().taskAssignee("kermit").singleResult();
        assertThat(task.getTaskLocalVariables()).isEmpty();
        assertThat(task.getProcessVariables())
                .hasSize(3)
                .contains(
                        entry("processVar", true),
                        entry("anotherProcessVar", 123)
                );
        assertThat(new String((byte[]) task.getProcessVariables().get("binaryVariable"))).isEqualTo("This is a binary process variable");

        tasks = taskService.createTaskQuery().includeTaskLocalVariables().taskCandidateUser("kermit").list();
        assertThat(tasks).hasSize(2);
        assertThat(tasks.get(0).getTaskLocalVariables())
                .hasSize(2)
                .containsEntry("test", "test");
        assertThat(tasks.get(0).getProcessVariables()).isEmpty();

        tasks = taskService.createTaskQuery().includeProcessVariables().taskCandidateUser("kermit").list();
        assertThat(tasks).hasSize(2);
        assertThat(tasks.get(0).getProcessVariables()).isEmpty();
        assertThat(tasks.get(0).getTaskLocalVariables()).isEmpty();

        task = taskService.createTaskQuery().includeTaskLocalVariables().taskAssignee("kermit").taskVariableValueEquals("localVar", "test").singleResult();
        assertThat(task.getProcessVariables()).isEmpty();
        assertThat(task.getTaskLocalVariables())
                .containsOnly(
                        entry("localVar", "test")
                );

        task = taskService.createTaskQuery().includeProcessVariables().taskAssignee("kermit").taskVariableValueEquals("localVar", "test").singleResult();
        assertThat(task.getProcessVariables()).hasSize(3);
        assertThat(task.getTaskLocalVariables()).isEmpty();
        assertThat(task.getProcessVariables())
                .contains(
                        entry("processVar", true),
                        entry("anotherProcessVar", 123)
                );

        task = taskService.createTaskQuery().includeTaskLocalVariables().includeProcessVariables().taskAssignee("kermit").singleResult();
        assertThat(task.getProcessVariables()).hasSize(3);
        assertThat(task.getTaskLocalVariables())
                .containsOnly(
                        entry("localVar", "test")
                );
        assertThat(task.getProcessVariables())
                .contains(
                        entry("processVar", true),
                        entry("anotherProcessVar", 123)
                );
        assertThat(new String((byte[]) task.getProcessVariables().get("binaryVariable"))).isEqualTo("This is a binary process variable");
    }
    
    @Test
    @Deployment
    public void testVariableExistsQuery() {
        Map<String, Object> startMap = new HashMap<>();
        startMap.put("processVar", true);
        startMap.put("binaryVariable", "This is a binary process variable".getBytes());
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", startMap);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).processVariableExists("processVar").singleResult();
        assertThat(task).isNotNull();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).processVariableNotExists("processVar").singleResult();
        assertThat(task).isNull();

        task = taskService.createTaskQuery().or().processVariableExists("processVar").processVariableExists("test").endOr().singleResult();
        assertThat(task).isNotNull();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskVariableExists("processVar").singleResult();
        assertThat(task).isNull();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskVariableNotExists("processVar").singleResult();
        assertThat(task).isNotNull();

        taskService.setVariable(task.getId(), "anotherProcessVar", 123);
        taskService.setVariableLocal(task.getId(), "localVar", "test");

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskVariableExists("localVar").singleResult();
        assertThat(task).isNotNull();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskVariableNotExists("localVar").singleResult();
        assertThat(task).isNull();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).or().processVariableExists("processVar")
                .processVariableValueEquals("anotherProcessVar", 123).endOr().singleResult();
        assertThat(task).isNotNull();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).or().processVariableNotExists("processVar")
                .processVariableValueEquals("anotherProcessVar", 123).endOr().singleResult();
        assertThat(task).isNotNull();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).or().processVariableExists("processVar").endOr().or()
                .processVariableValueEquals("anotherProcessVar", 123).endOr().singleResult();
        assertThat(task).isNotNull();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).or().processVariableNotExists("processVar").endOr().or()
                .processVariableValueEquals("anotherProcessVar", 123).endOr().singleResult();
        assertThat(task).isNull();
    }

    @Test
    public void testQueryWithPagingAndVariables() {
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().includeProcessVariables().includeTaskLocalVariables().orderByTaskPriority()
                .desc().listPage(0, 1);
        assertThat(tasks).hasSize(1);
        org.flowable.task.api.Task task = tasks.get(0);
        Map<String, Object> variableMap = task.getTaskLocalVariables();
        assertThat(variableMap)
                .containsOnly(
                        entry("testVar", "someVariable"),
                        entry("testVar2", 123),
                        entry("testVarBinary", "This is a binary variable".getBytes())
                );

        tasks = taskService.createTaskQuery().includeProcessVariables().includeTaskLocalVariables().orderByTaskPriority().asc().listPage(1, 2);
        assertThat(tasks).hasSize(2);
        task = tasks.get(1);
        variableMap = task.getTaskLocalVariables();
        assertThat(variableMap)
                .containsOnly(
                        entry("testVar", "someVariable"),
                        entry("testVar2", 123),
                        entry("testVarBinary", "This is a binary variable".getBytes())
                );

        tasks = taskService.createTaskQuery().includeProcessVariables().includeTaskLocalVariables().orderByTaskPriority().asc().listPage(2, 4);
        assertThat(tasks).hasSize(1);
        task = tasks.get(0);
        variableMap = task.getTaskLocalVariables();
        assertThat(variableMap)
                .containsOnly(
                        entry("testVar", "someVariable"),
                        entry("testVar2", 123),
                        entry("testVarBinary", "This is a binary variable".getBytes())
                );

        tasks = taskService.createTaskQuery().includeProcessVariables().includeTaskLocalVariables().orderByTaskPriority().asc().listPage(4, 2);
        assertThat(tasks).isEmpty();
    }

    // Unit test for https://activiti.atlassian.net/browse/ACT-4152
    @Test
    public void testQueryWithIncludeTaskVariableAndTaskCategory() {
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskAssignee("gonzo").list();
        for (org.flowable.task.api.Task task : tasks) {
            assertThat(task.getCategory()).isEqualTo("testCategory");
        }

        tasks = taskService.createTaskQuery().taskAssignee("gonzo").includeTaskLocalVariables().list();
        for (org.flowable.task.api.Task task : tasks) {
            assertThat(task.getCategory()).isEqualTo("testCategory");
        }

        tasks = taskService.createTaskQuery().taskAssignee("gonzo").includeProcessVariables().list();
        for (org.flowable.task.api.Task task : tasks) {
            assertThat(task.getCategory()).isEqualTo("testCategory");
        }
    }

    @Test
    public void testQueryWithLimitAndVariables() throws Exception {

        try {
            // setup - create 100 tasks
            multipleTaskIds = generateMultipleTestTasks();

            // limit results to 2000 and set maxResults for paging to 200
            // please see MNT-16040
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery()
                    .includeProcessVariables()
                    .includeTaskLocalVariables()
                    .orderByTaskPriority()
                    .asc()
                    .listPage(0, 200);
            // 100 tasks created by generateMultipleTestTasks and 3 created previously at setUp
            assertThat(tasks).hasSize(103);

            tasks = taskService.createTaskQuery()
                    .includeProcessVariables()
                    .includeTaskLocalVariables()
                    .orderByTaskPriority()
                    .asc()
                    .listPage(50, 100);
            assertThat(tasks).hasSize(53);
        } finally {
            taskService.deleteTasks(multipleTaskIds, true);
        }
    }

    @Test
    @Deployment
    public void testOrQuery() {
        Map<String, Object> startMap = new HashMap<>();
        startMap.put("anotherProcessVar", 123);
        runtimeService.startProcessInstanceByKey("oneTaskProcess", startMap);

        org.flowable.task.api.Task task = taskService.createTaskQuery().includeProcessVariables().or().processVariableValueEquals("undefined", 999)
                .processVariableValueEquals("anotherProcessVar", 123).endOr().singleResult();
        assertThat(task.getProcessVariables())
                .containsOnly(
                        entry("anotherProcessVar", 123)
                );

        task = taskService.createTaskQuery().includeProcessVariables().or().processVariableValueEquals("undefined", 999).endOr().singleResult();
        assertThat(task).isNull();

        task = taskService.createTaskQuery().includeProcessVariables().or().processVariableValueEquals("anotherProcessVar", 123)
                .processVariableValueEquals("undefined", 999).endOr().singleResult();
        assertThat(task.getProcessVariables())
                .containsOnly(
                        entry("anotherProcessVar", 123)
                );

        task = taskService.createTaskQuery().includeProcessVariables().or().processVariableValueEquals("anotherProcessVar", 123).endOr().singleResult();
        assertThat(task.getProcessVariables())
                .containsOnly(
                        entry("anotherProcessVar", 123)
                );

        task = taskService.createTaskQuery().includeProcessVariables().or().processVariableValueEquals("anotherProcessVar", 999).endOr().singleResult();
        assertThat(task).isNull();

        task = taskService.createTaskQuery().includeProcessVariables().or().processVariableValueEquals("anotherProcessVar", 999)
                .processVariableValueEquals("anotherProcessVar", 123).endOr().singleResult();
        assertThat(task.getProcessVariables())
                .containsOnly(
                        entry("anotherProcessVar", 123)
                );
    }

    @Test
    @Deployment
    public void testOrQueryMultipleVariableValues() {
        Map<String, Object> startMap = new HashMap<>();
        startMap.put("aProcessVar", 1);
        startMap.put("anotherProcessVar", 123);
        runtimeService.startProcessInstanceByKey("oneTaskProcess", startMap);

        TaskQuery query0 = taskService.createTaskQuery().includeProcessVariables().or();
        for (int i = 0; i < 20; i++) {
            query0 = query0.processVariableValueEquals("anotherProcessVar", i);
        }
        query0 = query0.endOr();
        assertThat(query0.singleResult()).isNull();

        TaskQuery query1 = taskService.createTaskQuery().includeProcessVariables().or().processVariableValueEquals("anotherProcessVar", 123);
        for (int i = 0; i < 20; i++) {
            query1 = query1.processVariableValueEquals("anotherProcessVar", i);
        }
        query1 = query1.endOr();
        Task task = query1.singleResult();
        assertThat(task.getProcessVariables())
                .hasSize(2)
                .containsEntry("anotherProcessVar", 123);
    }

    @Test
    public void testQueryTaskDefinitionId() {
        Task taskWithDefinitionId = createTaskWithDefinitionId("testTaskId");
        try {
            this.taskService.saveTask(taskWithDefinitionId);

            Task updatedTask = taskService.createTaskQuery().taskDefinitionId("testTaskDefinitionId").singleResult();
            assertThat(updatedTask.getName()).isEqualTo("taskWithDefinitionId");
            assertThat(updatedTask.getTaskDefinitionId()).isEqualTo("testTaskDefinitionId");

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                HistoricTaskInstance updatedHistoricTask = historyService.createHistoricTaskInstanceQuery().taskDefinitionId("testTaskDefinitionId")
                        .singleResult();
                assertThat(updatedHistoricTask.getName()).isEqualTo("taskWithDefinitionId");
                assertThat(updatedHistoricTask.getTaskDefinitionId()).isEqualTo("testTaskDefinitionId");
            }
        } finally {
            this.taskService.deleteTask("testTaskId", true);
        }
    }
    
    @Test
    public void testQueryTaskDefinitionId_multipleResults() {
        Task taskWithDefinitionId1 = createTaskWithDefinitionId("testTaskId1");
        Task taskWithDefinitionId2 = createTaskWithDefinitionId("testTaskId2");
        try {
            this.taskService.saveTask(taskWithDefinitionId1);
            this.taskService.saveTask(taskWithDefinitionId2);

            List<Task> updatedTasks = taskService.createTaskQuery().taskDefinitionId("testTaskDefinitionId").list();
            assertThat(updatedTasks).hasSize(2);
            
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                List<HistoricTaskInstance> updatedHistoricTasks = historyService.createHistoricTaskInstanceQuery().taskDefinitionId("testTaskDefinitionId").list();
                assertThat(updatedHistoricTasks).hasSize(2);
            }
        } finally {
            this.taskService.deleteTask("testTaskId1", true);
            this.taskService.deleteTask("testTaskId2", true);
        }
    }

    public Task createTaskWithDefinitionId(String taskId) {
        return this.processEngineConfiguration.getCommandExecutor().execute((Command<Task>) commandContext -> {
                TaskEntity task = processEngineConfiguration.getTaskServiceConfiguration().getTaskService().createTask();
                task.setId(taskId);
                task.setRevision(0);
                task.setTaskDefinitionId("testTaskDefinitionId");
                task.setName("taskWithDefinitionId");
                return task;
            });
    }

    /**
     * Generates some test tasks. - 2 tasks where kermit is a candidate and 1 task where gonzo is assignee
     */
    private List<String> generateTestTasks() throws Exception {
        List<String> ids = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
        // 2 tasks for kermit
        processEngineConfiguration.getClock().setCurrentTime(sdf.parse("01/01/2001 01:01:01.000"));
        for (int i = 0; i < 2; i++) {
            org.flowable.task.api.Task task = taskService.newTask();
            task.setName("testTask");
            task.setDescription("testTask description");
            task.setPriority(3);
            taskService.saveTask(task);
            ids.add(task.getId());
            taskService.setVariableLocal(task.getId(), "test", "test");
            taskService.setVariableLocal(task.getId(), "testBinary", "This is a binary variable".getBytes());
            taskService.addCandidateUser(task.getId(), "kermit");
        }

        processEngineConfiguration.getClock().setCurrentTime(sdf.parse("02/02/2002 02:02:02.000"));
        // 1 task for gonzo
        org.flowable.task.api.Task task = taskService.newTask();
        task.setName("gonzoTask");
        task.setDescription("gonzo description");
        task.setPriority(4);
        task.setCategory("testCategory");
        taskService.saveTask(task);
        taskService.setAssignee(task.getId(), "gonzo");
        taskService.setVariableLocal(task.getId(), "testVar", "someVariable");
        taskService.setVariableLocal(task.getId(), "testVarBinary", "This is a binary variable".getBytes());
        taskService.setVariableLocal(task.getId(), "testVar2", 123);
        ids.add(task.getId());

        return ids;
    }

    /**
     * Generates 100 test tasks.
     */
    private List<String> generateMultipleTestTasks() throws Exception {
        List<String> ids = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
        processEngineConfiguration.getClock().setCurrentTime(sdf.parse("01/01/2001 01:01:01.000"));
        for (int i = 0; i < 100; i++) {
            org.flowable.task.api.Task task = taskService.newTask();
            task.setName("testTask");
            task.setDescription("testTask description");
            task.setPriority(3);
            taskService.saveTask(task);
            ids.add(task.getId());
            taskService.setVariableLocal(task.getId(), "test", "test");
            taskService.setVariableLocal(task.getId(), "testBinary", "This is a binary variable".getBytes());
            taskService.addCandidateUser(task.getId(), "kermit");
        }
        return ids;
    }

}
