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
package org.flowable.engine.test.api.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class HistoricTaskAndVariablesQueryTest extends PluggableFlowableTestCase {

    private List<String> taskIds;

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
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricTaskInstance task = historyService.createHistoricTaskInstanceQuery().includeTaskLocalVariables().taskAssignee("gonzo").singleResult();
            Map<String, Object> variableMap = task.getTaskLocalVariables();
            assertThat(task.getProcessVariables()).isEmpty();
            assertThat(variableMap)
                    .containsOnly(
                            entry("testVar", "someVariable"),
                            entry("testVar2", 123)
                    );

            List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery().list();
            assertThat(tasks).hasSize(3);

            task = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().taskAssignee("gonzo").singleResult();
            assertThat(task.getProcessVariables()).isEmpty();
            assertThat(task.getTaskLocalVariables()).isEmpty();

            Map<String, Object> startMap = new HashMap<>();
            startMap.put("processVar", true);
            runtimeService.startProcessInstanceByKey("oneTaskProcess", startMap);
            String taskId = taskService.createTaskQuery().taskAssignee("kermit").singleResult().getId();
            taskService.addGroupIdentityLink(
                    taskId,
                    "testGroup",
                    IdentityLinkType.PARTICIPANT
            );
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            task = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().taskAssignee("kermit").singleResult();
            assertThat(task.getProcessVariables()).hasSize(1);
            assertThat(task.getTaskLocalVariables()).isEmpty();
            assertThat((Boolean) task.getProcessVariables().get("processVar")).isTrue();

            taskService.setVariable(task.getId(), "anotherProcessVar", 123);
            taskService.setVariableLocal(task.getId(), "localVar", "test");

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            task = historyService.createHistoricTaskInstanceQuery().includeTaskLocalVariables().taskAssignee("kermit").singleResult();
            assertThat(task.getProcessVariables()).isEmpty();
            assertThat(task.getTaskLocalVariables())
                    .containsOnly(entry("localVar", "test"));

            task = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().taskAssignee("kermit").singleResult();
            assertThat(task.getTaskLocalVariables()).isEmpty();
            assertThat(task.getProcessVariables())
                    .containsOnly(
                            entry("processVar", true),
                            entry("anotherProcessVar", 123)
                    );

            task = historyService.createHistoricTaskInstanceQuery().taskVariableValueLike("testVar", "someVaria%").singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getName()).isEqualTo("gonzoTask");

            task = historyService.createHistoricTaskInstanceQuery().taskVariableValueLikeIgnoreCase("testVar", "somevaria%").singleResult();
            assertThat(task).isNotNull();
            assertThat(task.getName()).isEqualTo("gonzoTask");

            task = historyService.createHistoricTaskInstanceQuery().taskVariableValueLikeIgnoreCase("testVar", "somevaria2%").singleResult();
            assertThat(task).isNull();

            tasks = historyService.createHistoricTaskInstanceQuery().includeTaskLocalVariables().taskInvolvedUser("kermit").orderByTaskCreateTime().asc()
                    .list();
            assertThat(tasks).hasSize(3);
            assertThat(tasks.get(0).getTaskLocalVariables())
                    .containsOnly(entry("test", "test"));
            assertThat(tasks.get(0).getProcessVariables()).isEmpty();

            tasks = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().taskInvolvedUser("kermit").orderByTaskCreateTime().asc().list();
            assertThat(tasks).hasSize(3);
            assertThat(tasks.get(0).getProcessVariables()).isEmpty();
            assertThat(tasks.get(0).getTaskLocalVariables()).isEmpty();

            task = historyService.createHistoricTaskInstanceQuery().includeTaskLocalVariables().taskAssignee("kermit")
                    .taskVariableValueEquals("localVar", "test").singleResult();
            assertThat(task.getProcessVariables()).isEmpty();
            assertThat(task.getTaskLocalVariables())
                    .containsOnly(entry("localVar", "test"));

            task = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().taskAssignee("kermit").taskVariableValueEquals("localVar", "test")
                    .singleResult();
            assertThat(task.getTaskLocalVariables()).isEmpty();
            assertThat(task.getProcessVariables())
                    .containsOnly(
                            entry("processVar", true),
                            entry("anotherProcessVar", 123)
                    );

            task = historyService.createHistoricTaskInstanceQuery().includeTaskLocalVariables().includeProcessVariables().taskAssignee("kermit").singleResult();
            assertThat(task.getTaskLocalVariables())
                    .containsOnly(entry("localVar", "test"));
            assertThat(task.getProcessVariables())
                    .containsOnly(
                            entry("processVar", true),
                            entry("anotherProcessVar", 123)
                    );

            task = historyService.createHistoricTaskInstanceQuery().taskAssignee("gonzo").singleResult();
            taskService.complete(task.getId());

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            task = (HistoricTaskInstance) historyService.createHistoricTaskInstanceQuery().includeTaskLocalVariables().finished().singleResult();
            variableMap = task.getTaskLocalVariables();
            assertThat(task.getProcessVariables()).isEmpty();
            assertThat(variableMap)
                    .containsOnly(
                            entry("testVar", "someVariable"),
                            entry("testVar2", 123)
                    );

            assertThat(historyService.createHistoricTaskInstanceQuery().taskInvolvedGroups(Collections.singleton("testGroup")).count()).isEqualTo(1);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskInvolvedGroups(Collections.singleton("testGroup")).list().get(0).getId())
                    .isEqualTo(taskId);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskInvolvedGroups(Collections.singleton("testGroup")).singleResult().getId())
                    .isEqualTo(taskId);
            assertThat(historyService.createHistoricTaskInstanceQuery()
                    .or().taskInvolvedGroups(Collections.singleton("testGroup")).taskInvolvedUser("kermit").endOr().count()).isEqualTo(3);
            assertThat(historyService.createHistoricTaskInstanceQuery()
                    .or().taskInvolvedGroups(Collections.singleton("testGroup")).processInstanceId("undefined").endOr().count()).isEqualTo(1);
        }
    }

    @Test
    @Deployment
    public void testOrQuery() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricTaskInstance task = historyService.createHistoricTaskInstanceQuery()
                    .includeTaskLocalVariables()
                    .or()
                    .taskAssignee("gonzo")
                    .endOr()
                    .singleResult();

            Map<String, Object> variableMap = task.getTaskLocalVariables();
            assertThat(task.getProcessVariables()).isEmpty();
            assertThat(variableMap)
                    .containsOnly(
                            entry("testVar", "someVariable"),
                            entry("testVar2", 123)
                    );

            List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery().list();
            assertThat(tasks).hasSize(3);

            task = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().or().taskAssignee("gonzo")
                    .taskVariableValueEquals("localVar", "nonExisting").endOr().singleResult();
            assertThat(task.getProcessVariables()).isEmpty();
            assertThat(task.getTaskLocalVariables()).isEmpty();

            Map<String, Object> startMap = new HashMap<>();
            startMap.put("processVar", true);
            runtimeService.startProcessInstanceByKey("oneTaskProcess", startMap);

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            task = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().or().taskAssignee("kermit")
                    .taskVariableValueEquals("localVar", "nonExisting").endOr().singleResult();
            assertThat(task.getProcessVariables()).hasSize(1);
            assertThat(task.getTaskLocalVariables()).isEmpty();
            assertThat((Boolean) task.getProcessVariables().get("processVar")).isTrue();

            task = historyService.createHistoricTaskInstanceQuery()
                    .includeProcessVariables()
                    .or()
                    .taskAssignee("kermit")
                    .taskVariableValueEquals("localVar", "nonExisting")
                    .endOr()
                    .or()
                    .processDefinitionKey("oneTaskProcess")
                    .taskVariableValueEquals("localVar", "nonExisting")
                    .endOr()
                    .singleResult();

            assertThat(task).isNotNull();
            assertThat(task.getProcessVariables()).hasSize(1);
            assertThat(task.getTaskLocalVariables()).isEmpty();
            assertThat((Boolean) task.getProcessVariables().get("processVar")).isTrue();

            taskService.setVariable(task.getId(), "anotherProcessVar", 123);
            taskService.setVariableLocal(task.getId(), "localVar", "test");

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            task = historyService.createHistoricTaskInstanceQuery().includeTaskLocalVariables().or().taskAssignee("kermit")
                    .taskVariableValueEquals("localVar", "nonExisting").endOr().singleResult();
            assertThat(task.getProcessVariables()).isEmpty();
            assertThat(task.getTaskLocalVariables())
                    .containsOnly(entry("localVar", "test"));

            task = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().or().taskAssignee("kermit")
                    .taskVariableValueEquals("localVar", "nonExisting").endOr().singleResult();
            assertThat(task.getTaskLocalVariables()).isEmpty();
            assertThat(task.getProcessVariables())
                    .containsOnly(
                            entry("processVar", true),
                            entry("anotherProcessVar", 123)
                    );

            task = historyService.createHistoricTaskInstanceQuery()
                    .includeTaskLocalVariables()
                    .or()
                    .taskAssignee("nonexisting")
                    .taskVariableValueLike("testVar", "someVar%")
                    .endOr()
                    .singleResult();
            assertThat(task.getProcessVariables()).isEmpty();
            assertThat(task.getTaskLocalVariables())
                    .containsOnly(
                            entry("testVar", "someVariable"),
                            entry("testVar2", 123)
                    );

            task = historyService.createHistoricTaskInstanceQuery()
                    .includeTaskLocalVariables()
                    .or()
                    .taskAssignee("nonexisting")
                    .taskVariableValueLikeIgnoreCase("testVar", "somevar%")
                    .endOr()
                    .singleResult();
            assertThat(task.getProcessVariables()).isEmpty();
            assertThat(task.getTaskLocalVariables())
                    .containsOnly(
                            entry("testVar", "someVariable"),
                            entry("testVar2", 123)
                    );

            task = historyService.createHistoricTaskInstanceQuery()
                    .includeTaskLocalVariables()
                    .or()
                    .taskAssignee("nonexisting")
                    .taskVariableValueLike("testVar", "someVar2%")
                    .endOr()
                    .singleResult();
            assertThat(task).isNull();

            tasks = historyService.createHistoricTaskInstanceQuery().includeTaskLocalVariables()
                    .or()
                    .taskInvolvedUser("kermit")
                    .taskVariableValueEquals("localVar", "nonExisting")
                    .endOr()
                    .orderByTaskCreateTime().asc().list();
            assertThat(tasks).hasSize(3);
            assertThat(tasks.get(0).getProcessVariables()).isEmpty();
            assertThat(tasks.get(0).getTaskLocalVariables())
                    .containsOnly(entry("test", "test"));

            tasks = historyService.createHistoricTaskInstanceQuery().includeProcessVariables()
                    .or()
                    .taskInvolvedUser("kermit")
                    .taskVariableValueEquals("localVar", "nonExisting")
                    .endOr()
                    .orderByTaskCreateTime().asc().list();
            assertThat(tasks).hasSize(3);
            assertThat(tasks.get(0).getProcessVariables()).isEmpty();
            assertThat(tasks.get(0).getTaskLocalVariables()).isEmpty();

            task = historyService.createHistoricTaskInstanceQuery().includeTaskLocalVariables().taskAssignee("kermit").or()
                    .taskVariableValueEquals("localVar", "test")
                    .taskVariableValueEquals("localVar", "nonExisting").endOr().singleResult();
            assertThat(task.getProcessVariables()).isEmpty();
            assertThat(task.getTaskLocalVariables())
                    .containsOnly(entry("localVar", "test"));

            task = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().taskAssignee("kermit").or()
                    .taskVariableValueEquals("localVar", "test")
                    .taskVariableValueEquals("localVar", "nonExisting").endOr().singleResult();
            assertThat(task.getTaskLocalVariables()).isEmpty();
            assertThat(task.getProcessVariables())
                    .containsOnly(
                            entry("processVar", true),
                            entry("anotherProcessVar", 123)
                    );

            task = historyService.createHistoricTaskInstanceQuery().includeTaskLocalVariables().includeProcessVariables().or().taskAssignee("kermit")
                    .taskVariableValueEquals("localVar", "nonExisting")
                    .endOr().singleResult();
            assertThat(task.getTaskLocalVariables())
                    .containsOnly(entry("localVar", "test"));
            assertThat(task.getProcessVariables())
                    .containsOnly(
                            entry("processVar", true),
                            entry("anotherProcessVar", 123)
                    );

            task = historyService.createHistoricTaskInstanceQuery().taskAssignee("gonzo").singleResult();
            taskService.complete(task.getId());

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            task = historyService.createHistoricTaskInstanceQuery().includeTaskLocalVariables().or().finished()
                    .taskVariableValueEquals("localVar", "nonExisting").endOr().singleResult();
            variableMap = task.getTaskLocalVariables();
            assertThat(task.getProcessVariables()).isEmpty();
            assertThat(variableMap)
                    .containsOnly(
                            entry("testVar", "someVariable"),
                            entry("testVar2", 123)
                    );
        }
    }

    @Test
    @Deployment
    public void testOrQueryMultipleVariableValues() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            Map<String, Object> startMap = new HashMap<>();
            startMap.put("processVar", true);
            startMap.put("anotherProcessVar", 123);
            runtimeService.startProcessInstanceByKey("oneTaskProcess", startMap);

            startMap.put("anotherProcessVar", 999);
            runtimeService.startProcessInstanceByKey("oneTaskProcess", startMap);

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            HistoricTaskInstanceQuery query0 = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().or();
            for (int i = 0; i < 20; i++) {
                query0 = query0.processVariableValueEquals("anotherProcessVar", i);
            }
            query0 = query0.endOr();
            assertThat(query0.singleResult()).isNull();

            HistoricTaskInstanceQuery query1 = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().or()
                    .processVariableValueEquals("anotherProcessVar", 123);
            for (int i = 0; i < 20; i++) {
                query1 = query1.processVariableValueEquals("anotherProcessVar", i);
            }
            query1 = query1.endOr();
            HistoricTaskInstance task = query1.singleResult();
            assertThat(task.getProcessVariables())
                    .containsOnly(
                            entry("processVar", true),
                            entry("anotherProcessVar", 123)
                    );
        }
    }

    @Test
    @Deployment
    public void testCandidate() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateUser("kermit").list();
            assertThat(tasks).hasSize(3);

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateUser("gonzo").list();
            assertThat(tasks).isEmpty();

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateUser("fozzie").list();
            assertThat(tasks).hasSize(1);

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateGroup("management").list();
            assertThat(tasks).hasSize(1);
            List<String> groups = new ArrayList<>();
            groups.add("management");
            groups.add("accountancy");
            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateGroupIn(groups).list();
            assertThat(tasks).hasSize(1);

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateUser("kermit").taskCandidateGroupIn(groups).list();
            assertThat(tasks).hasSize(3);

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateUser("gonzo").taskCandidateGroupIn(groups).list();
            assertThat(tasks).hasSize(1);

            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            taskService.complete(task.getId());

            assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isZero();

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateUser("kermit").list();
            assertThat(tasks).hasSize(3);

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateUser("gonzo").list();
            assertThat(tasks).isEmpty();

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateUser("fozzie").list();
            assertThat(tasks).hasSize(1);

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateGroup("management").list();
            assertThat(tasks).hasSize(1);

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateUser("kermit").taskCandidateGroup("management").list();
            assertThat(tasks).hasSize(3);

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateUser("gonzo").taskCandidateGroup("management").list();
            assertThat(tasks).hasSize(1);

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateUser("gonzo").taskCandidateGroup("invalid").list();
            assertThat(tasks).isEmpty();

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateGroupIn(groups).list();
            assertThat(tasks).hasSize(1);
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/history/HistoricTaskAndVariablesQueryTest.testCandidate.bpmn20.xml" })
    public void testIgnoreAssigneeValue() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            String taskId = taskService
                    .createTaskQuery()
                    .processInstanceId(processInstance.getProcessInstanceId())
                    .list()
                    .get(0)
                    .getId();
            taskService.setAssignee(taskId, "kermit");
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            List<HistoricTaskInstance> tasks = historyService
                    .createHistoricTaskInstanceQuery()
                    .taskCandidateUser("kermit")
                    .list();
            assertThat(tasks).hasSize(2);

            tasks = historyService
                    .createHistoricTaskInstanceQuery()
                    .taskCandidateUser("kermit")
                    .ignoreAssigneeValue()
                    .list();
            assertThat(tasks).hasSize(3);

            tasks = historyService
                    .createHistoricTaskInstanceQuery()
                    .taskCandidateGroup("management")
                    .list();
            assertThat(tasks).isEmpty();

            tasks = historyService
                    .createHistoricTaskInstanceQuery()
                    .taskCandidateGroup("management")
                    .ignoreAssigneeValue()
                    .list();
            assertThat(tasks).hasSize(1);
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/history/HistoricTaskAndVariablesQueryTest.testCandidate.bpmn20.xml" })
    public void testIgnoreAssigneeValueOr() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            String taskId = taskService
                    .createTaskQuery()
                    .processInstanceId(processInstance.getProcessInstanceId())
                    .list()
                    .get(0)
                    .getId();
            taskService.setAssignee(taskId, "kermit");
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                    .or()
                    .taskCandidateUser("kermit")
                    .taskAssignee("gonzo")
                    .ignoreAssigneeValue()
                    .endOr()
                    .list();

            assertThat(tasks).hasSize(4);

            tasks = historyService.createHistoricTaskInstanceQuery()
                    .or()
                    .taskCandidateUser("kermit")
                    .taskAssignee("gonzo")
                    .endOr()
                    .list();
            assertThat(tasks).hasSize(3);
        }
    }

    @Test
    public void testQueryWithPagingAndVariables() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().includeTaskLocalVariables()
                    .orderByTaskPriority().desc().listPage(0, 1);
            assertThat(tasks).hasSize(1);
            HistoricTaskInstance task = tasks.get(0);
            Map<String, Object> variableMap = task.getTaskLocalVariables();
            assertThat(variableMap)
                    .containsOnly(
                            entry("testVar", "someVariable"),
                            entry("testVar2", 123)
                    );

            tasks = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().includeTaskLocalVariables().orderByTaskPriority().asc()
                    .listPage(1, 2);
            assertThat(tasks).hasSize(2);
            task = tasks.get(1);
            variableMap = task.getTaskLocalVariables();
            assertThat(variableMap)
                    .containsOnly(
                            entry("testVar", "someVariable"),
                            entry("testVar2", 123)
                    );

            tasks = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().includeTaskLocalVariables().orderByTaskPriority().asc()
                    .listPage(2, 4);
            assertThat(tasks).hasSize(1);
            task = tasks.get(0);
            variableMap = task.getTaskLocalVariables();
            assertThat(variableMap)
                    .containsOnly(
                            entry("testVar", "someVariable"),
                            entry("testVar2", 123)
                    );

            tasks = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().includeTaskLocalVariables().orderByTaskPriority().asc()
                    .listPage(4, 2);
            assertThat(tasks).isEmpty();
        }
    }

    @Test
    public void testQueryWithPagingVariablesAndIdentityLinks() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {

            List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().includeTaskLocalVariables()
                    .includeIdentityLinks().orderByTaskPriority().desc().listPage(0, 1);
            assertThat(tasks).hasSize(1);
            HistoricTaskInstance task = tasks.get(0);
            Map<String, Object> variableMap = task.getTaskLocalVariables();
            assertThat(variableMap)
                    .containsOnly(
                            entry("testVar", "someVariable"),
                            entry("testVar2", 123)
                    );
            assertThat(task.getIdentityLinks()).hasSize(1);
            IdentityLinkInfo identityLink = task.getIdentityLinks().get(0);
            assertThat(identityLink.getProcessInstanceId()).isNull();
            assertThat(identityLink.getType()).isEqualTo("assignee");
            assertThat(identityLink.getGroupId()).isNull();
            assertThat(identityLink.getUserId()).isEqualTo("gonzo");
            assertThat(identityLink.getTaskId()).isEqualTo(task.getId());

            tasks = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().includeTaskLocalVariables().includeIdentityLinks()
                    .orderByTaskPriority().asc().listPage(1, 2);
            assertThat(tasks).hasSize(2);
            task = tasks.get(1);
            variableMap = task.getTaskLocalVariables();
            assertThat(variableMap)
                    .containsOnly(
                            entry("testVar", "someVariable"),
                            entry("testVar2", 123)
                    );
            assertThat(task.getIdentityLinks()).hasSize(1);
            identityLink = task.getIdentityLinks().get(0);
            assertThat(identityLink.getProcessInstanceId()).isNull();
            assertThat(identityLink.getType()).isEqualTo("assignee");
            assertThat(identityLink.getGroupId()).isNull();
            assertThat(identityLink.getUserId()).isEqualTo("gonzo");
            assertThat(identityLink.getTaskId()).isEqualTo(task.getId());

            tasks = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().includeTaskLocalVariables().includeIdentityLinks()
                    .orderByTaskPriority().asc().listPage(2, 4);
            assertThat(tasks).hasSize(1);
            task = tasks.get(0);
            variableMap = task.getTaskLocalVariables();
            assertThat(variableMap)
                    .containsOnly(
                            entry("testVar", "someVariable"),
                            entry("testVar2", 123)
                    );
            assertThat(task.getIdentityLinks()).hasSize(1);
            identityLink = task.getIdentityLinks().get(0);
            assertThat(identityLink.getProcessInstanceId()).isNull();
            assertThat(identityLink.getType()).isEqualTo("assignee");
            assertThat(identityLink.getGroupId()).isNull();
            assertThat(identityLink.getUserId()).isEqualTo("gonzo");
            assertThat(identityLink.getTaskId()).isEqualTo(task.getId());

            tasks = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().includeTaskLocalVariables().includeIdentityLinks()
                    .orderByTaskPriority().asc().listPage(4, 2);
            assertThat(tasks).isEmpty();
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/history/HistoricTaskAndVariablesQueryTest.testCandidate.bpmn20.xml" })
    public void testQueryVariableExists() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricTaskInstance task = historyService.createHistoricTaskInstanceQuery().taskVariableExists("testVar").singleResult();
            assertThat(task).isNotNull();

            List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery().taskVariableNotExists("testVar").list();
            assertThat(tasks).hasSize(2);

            tasks = historyService.createHistoricTaskInstanceQuery().or().taskVariableNotExists("testVar").processDefinitionId("unexisting").list();
            assertThat(tasks).hasSize(2);

            tasks = historyService.createHistoricTaskInstanceQuery().or().taskVariableExists("testVar").processDefinitionId("unexisting").list();
            assertThat(tasks).hasSize(1);

            tasks = historyService.createHistoricTaskInstanceQuery().or().taskVariableNotExists("testVar").endOr().or().processDefinitionId("unexisting")
                    .list();
            assertThat(tasks).isEmpty();

            tasks = historyService.createHistoricTaskInstanceQuery().or().taskVariableExists("testVar").endOr().or().processDefinitionId("unexisting").list();
            assertThat(tasks).isEmpty();

            Map<String, Object> varMap = Collections.singletonMap("processVar", (Object) "test");
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", varMap);

            waitForHistoryJobExecutorToProcessAllJobs(7000, 250);

            tasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).processVariableExists("processVar").list();
            assertThat(tasks).hasSize(1);

            tasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).processVariableNotExists("processVar").list();
            assertThat(tasks).isEmpty();

            tasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId())
                    .or().processVariableExists("processVar").processDefinitionId("undexisting").endOr().list();
            assertThat(tasks).hasSize(1);

            tasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId())
                    .or().processVariableNotExists("processVar").processInstanceId(processInstance.getId()).endOr().list();
            assertThat(tasks).hasSize(1);

            runtimeService.setVariable(processInstance.getId(), "processVar2", "test2");

            waitForHistoryJobExecutorToProcessAllJobs(7000, 250);

            tasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId())
                    .processVariableExists("processVar").processVariableValueEquals("processVar2", "test2").list();
            assertThat(tasks).hasSize(1);

            tasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId())
                    .processVariableNotExists("processVar").processVariableValueEquals("processVar2", "test2").list();
            assertThat(tasks).isEmpty();

            tasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId())
                    .or().processVariableExists("processVar").processVariableValueEquals("processVar2", "test2").endOr().list();
            assertThat(tasks).hasSize(1);

            tasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId())
                    .or().processVariableNotExists("processVar").processVariableValueEquals("processVar2", "test2").endOr().list();
            assertThat(tasks).hasSize(1);
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/history/HistoricTaskAndVariablesQueryTest.testCandidate.bpmn20.xml" })
    public void testQueryVariableExistsForCompletedTasks() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            taskService.createTaskQuery().list().forEach(
                    task -> taskService.complete(task.getId())
            );
            testQueryVariableExists();
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testWithoutDueDateQuery() throws Exception {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).withoutTaskDueDate()
                    .singleResult();
            assertThat(historicTask).isNotNull();
            assertThat(historicTask.getDueDate()).isNull();

            // Set due-date on task
            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            Date dueDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("01/02/2003 01:12:13");
            task.setDueDate(dueDate);
            taskService.saveTask(task);

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).withoutTaskDueDate().count()).isZero();

            task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

            // Clear due-date on task
            task.setDueDate(null);
            taskService.saveTask(task);

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).withoutTaskDueDate().count()).isEqualTo(1);
        }
    }

    // Unit test for https://activiti.atlassian.net/browse/ACT-4152
    @Test
    public void testQueryWithIncludeTaskVariableAndTaskCategory() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery().taskAssignee("gonzo").list();
            assertThat(tasks)
                    .extracting(HistoricTaskInstance::getCategory)
                    .containsOnly("testCategory");

            tasks = historyService.createHistoricTaskInstanceQuery().taskAssignee("gonzo").includeTaskLocalVariables().list();
            assertThat(tasks)
                    .extracting(HistoricTaskInstance::getCategory)
                    .containsOnly("testCategory");

            tasks = historyService.createHistoricTaskInstanceQuery().taskAssignee("gonzo").includeProcessVariables().list();
            assertThat(tasks)
                    .extracting(HistoricTaskInstance::getCategory)
                    .containsOnly("testCategory");
        }
    }

    @Test
    public void testQueryVariableValueEqualsAndNotEquals() {
        Task taskWithStringValue = taskService.createTaskBuilder()
                .name("With string value")
                .create();
        taskIds.add(taskWithStringValue.getId());
        taskService.setVariable(taskWithStringValue.getId(), "var", "TEST");

        Task taskWithNullValue = taskService.createTaskBuilder()
                .name("With null value")
                .create();
        taskIds.add(taskWithNullValue.getId());
        taskService.setVariable(taskWithNullValue.getId(), "var", null);

        Task taskWithLongValue = taskService.createTaskBuilder()
                .name("With long value")
                .create();
        taskIds.add(taskWithLongValue.getId());
        taskService.setVariable(taskWithLongValue.getId(), "var", 100L);

        Task taskWithDoubleValue = taskService.createTaskBuilder()
                .name("With double value")
                .create();
        taskIds.add(taskWithDoubleValue.getId());
        taskService.setVariable(taskWithDoubleValue.getId(), "var", 45.55);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {

            assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueNotEquals("var", "TEST").list())
                    .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(
                            tuple("With null value", taskWithNullValue.getId()),
                            tuple("With long value", taskWithLongValue.getId()),
                            tuple("With double value", taskWithDoubleValue.getId())
                    );

            assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("var", "TEST").list())
                    .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(
                            tuple("With string value", taskWithStringValue.getId())
                    );

            assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueNotEquals("var", 100L).list())
                    .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(
                            tuple("With string value", taskWithStringValue.getId()),
                            tuple("With null value", taskWithNullValue.getId()),
                            tuple("With double value", taskWithDoubleValue.getId())
                    );

            assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("var", 100L).list())
                    .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(
                            tuple("With long value", taskWithLongValue.getId())
                    );

            assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueNotEquals("var", 45.55).list())
                    .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(
                            tuple("With string value", taskWithStringValue.getId()),
                            tuple("With null value", taskWithNullValue.getId()),
                            tuple("With long value", taskWithLongValue.getId())
                    );

            assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("var", 45.55).list())
                    .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(
                            tuple("With double value", taskWithDoubleValue.getId())
                    );

            assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueNotEquals("var", "test").list())
                    .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(
                            tuple("With string value", taskWithStringValue.getId()),
                            tuple("With null value", taskWithNullValue.getId()),
                            tuple("With long value", taskWithLongValue.getId()),
                            tuple("With double value", taskWithDoubleValue.getId())
                    );

            assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueNotEqualsIgnoreCase("var", "test").list())
                    .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(
                            tuple("With null value", taskWithNullValue.getId()),
                            tuple("With long value", taskWithLongValue.getId()),
                            tuple("With double value", taskWithDoubleValue.getId())
                    );

            assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEquals("var", "test").list())
                    .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getId)
                    .isEmpty();

            assertThat(historyService.createHistoricTaskInstanceQuery().taskVariableValueEqualsIgnoreCase("var", "test").list())
                    .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getId)
                    .containsExactlyInAnyOrder(
                            tuple("With string value", taskWithStringValue.getId())
                    );
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryProcessVariableValueEqualsAndNotEquals() {
        ProcessInstance processWithStringValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With string value")
                .variable("var", "TEST")
                .start();

        ProcessInstance processWithNullValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With null value")
                .variable("var", null)
                .start();

        ProcessInstance processWithLongValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With long value")
                .variable("var", 100L)
                .start();

        ProcessInstance processWithDoubleValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With double value")
                .variable("var", 45.55)
                .start();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {

            assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueNotEquals("var", "TEST").list())
                    .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getProcessInstanceId)
                    .containsExactlyInAnyOrder(
                            tuple("my task", processWithNullValue.getId()),
                            tuple("my task", processWithLongValue.getId()),
                            tuple("my task", processWithDoubleValue.getId())
                    );

            assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("var", "TEST").list())
                    .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getProcessInstanceId)
                    .containsExactlyInAnyOrder(
                            tuple("my task", processWithStringValue.getId())
                    );

            assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueNotEquals("var", 100L).list())
                    .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getProcessInstanceId)
                    .containsExactlyInAnyOrder(
                            tuple("my task", processWithStringValue.getId()),
                            tuple("my task", processWithNullValue.getId()),
                            tuple("my task", processWithDoubleValue.getId())
                    );

            assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("var", 100L).list())
                    .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getProcessInstanceId)
                    .containsExactlyInAnyOrder(
                            tuple("my task", processWithLongValue.getId())
                    );

            assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueNotEquals("var", 45.55).list())
                    .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getProcessInstanceId)
                    .containsExactlyInAnyOrder(
                            tuple("my task", processWithStringValue.getId()),
                            tuple("my task", processWithNullValue.getId()),
                            tuple("my task", processWithLongValue.getId())
                    );

            assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("var", 45.55).list())
                    .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getProcessInstanceId)
                    .containsExactlyInAnyOrder(
                            tuple("my task", processWithDoubleValue.getId())
                    );

            assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueNotEquals("var", "test").list())
                    .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getProcessInstanceId)
                    .containsExactlyInAnyOrder(
                            tuple("my task", processWithStringValue.getId()),
                            tuple("my task", processWithNullValue.getId()),
                            tuple("my task", processWithLongValue.getId()),
                            tuple("my task", processWithDoubleValue.getId())
                    );

            assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueNotEqualsIgnoreCase("var", "test").list())
                    .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getProcessInstanceId)
                    .containsExactlyInAnyOrder(
                            tuple("my task", processWithNullValue.getId()),
                            tuple("my task", processWithLongValue.getId()),
                            tuple("my task", processWithDoubleValue.getId())
                    );

            assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEquals("var", "test").list())
                    .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getProcessInstanceId)
                    .isEmpty();

            assertThat(historyService.createHistoricTaskInstanceQuery().processVariableValueEqualsIgnoreCase("var", "test").list())
                    .extracting(HistoricTaskInstance::getName, HistoricTaskInstance::getProcessInstanceId)
                    .containsExactlyInAnyOrder(
                            tuple("my task", processWithStringValue.getId())
                    );
        }
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
        taskService.setVariableLocal(task.getId(), "testVar2", 123);
        ids.add(task.getId());

        return ids;
    }

}
