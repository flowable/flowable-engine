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
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;

/**
 * @author Tijs Rademakers
 */
public class HistoricTaskAndVariablesQueryTest extends PluggableFlowableTestCase {

    private List<String> taskIds;

    @Override
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

    @Override
    public void tearDown() throws Exception {
        identityService.deleteGroup("accountancy");
        identityService.deleteGroup("management");
        identityService.deleteUser("fozzie");
        identityService.deleteUser("gonzo");
        identityService.deleteUser("kermit");
        taskService.deleteTasks(taskIds, true);
    }

    @Deployment
    public void testQuery() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricTaskInstance task = historyService.createHistoricTaskInstanceQuery().includeTaskLocalVariables().taskAssignee("gonzo").singleResult();
            Map<String, Object> variableMap = task.getTaskLocalVariables();
            assertEquals(2, variableMap.size());
            assertEquals(0, task.getProcessVariables().size());
            assertNotNull(variableMap.get("testVar"));
            assertEquals("someVariable", variableMap.get("testVar"));
            assertNotNull(variableMap.get("testVar2"));
            assertEquals(123, variableMap.get("testVar2"));

            List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery().list();
            assertEquals(3, tasks.size());

            task = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().taskAssignee("gonzo").singleResult();
            assertEquals(0, task.getProcessVariables().size());
            assertEquals(0, task.getTaskLocalVariables().size());

            Map<String, Object> startMap = new HashMap<>();
            startMap.put("processVar", true);
            runtimeService.startProcessInstanceByKey("oneTaskProcess", startMap);
            
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);

            task = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().taskAssignee("kermit").singleResult();
            assertEquals(1, task.getProcessVariables().size());
            assertEquals(0, task.getTaskLocalVariables().size());
            assertTrue((Boolean) task.getProcessVariables().get("processVar"));

            taskService.setVariable(task.getId(), "anotherProcessVar", 123);
            taskService.setVariableLocal(task.getId(), "localVar", "test");
            
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);

            task = historyService.createHistoricTaskInstanceQuery().includeTaskLocalVariables().taskAssignee("kermit").singleResult();
            assertEquals(0, task.getProcessVariables().size());
            assertEquals(1, task.getTaskLocalVariables().size());
            assertEquals("test", task.getTaskLocalVariables().get("localVar"));

            task = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().taskAssignee("kermit").singleResult();
            assertEquals(2, task.getProcessVariables().size());
            assertEquals(0, task.getTaskLocalVariables().size());
            assertEquals(true, task.getProcessVariables().get("processVar"));
            assertEquals(123, task.getProcessVariables().get("anotherProcessVar"));

            task = historyService.createHistoricTaskInstanceQuery().taskVariableValueLike("testVar", "someVaria%").singleResult();
            assertNotNull(task);
            assertEquals("gonzoTask", task.getName());

            task = historyService.createHistoricTaskInstanceQuery().taskVariableValueLikeIgnoreCase("testVar", "somevaria%").singleResult();
            assertNotNull(task);
            assertEquals("gonzoTask", task.getName());

            task = historyService.createHistoricTaskInstanceQuery().taskVariableValueLikeIgnoreCase("testVar", "somevaria2%").singleResult();
            assertNull(task);

            tasks = historyService.createHistoricTaskInstanceQuery().includeTaskLocalVariables().taskInvolvedUser("kermit").orderByTaskCreateTime().asc().list();
            assertEquals(3, tasks.size());
            assertEquals(1, tasks.get(0).getTaskLocalVariables().size());
            assertEquals("test", tasks.get(0).getTaskLocalVariables().get("test"));
            assertEquals(0, tasks.get(0).getProcessVariables().size());

            tasks = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().taskInvolvedUser("kermit").orderByTaskCreateTime().asc().list();
            assertEquals(3, tasks.size());
            assertEquals(0, tasks.get(0).getProcessVariables().size());
            assertEquals(0, tasks.get(0).getTaskLocalVariables().size());

            task = historyService.createHistoricTaskInstanceQuery().includeTaskLocalVariables().taskAssignee("kermit").taskVariableValueEquals("localVar", "test").singleResult();
            assertEquals(0, task.getProcessVariables().size());
            assertEquals(1, task.getTaskLocalVariables().size());
            assertEquals("test", task.getTaskLocalVariables().get("localVar"));

            task = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().taskAssignee("kermit").taskVariableValueEquals("localVar", "test").singleResult();
            assertEquals(2, task.getProcessVariables().size());
            assertEquals(0, task.getTaskLocalVariables().size());
            assertEquals(true, task.getProcessVariables().get("processVar"));
            assertEquals(123, task.getProcessVariables().get("anotherProcessVar"));

            task = historyService.createHistoricTaskInstanceQuery().includeTaskLocalVariables().includeProcessVariables().taskAssignee("kermit").singleResult();
            assertEquals(2, task.getProcessVariables().size());
            assertEquals(1, task.getTaskLocalVariables().size());
            assertEquals("test", task.getTaskLocalVariables().get("localVar"));
            assertEquals(true, task.getProcessVariables().get("processVar"));
            assertEquals(123, task.getProcessVariables().get("anotherProcessVar"));

            task = historyService.createHistoricTaskInstanceQuery().taskAssignee("gonzo").singleResult();
            taskService.complete(task.getId());
            
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);
            
            task = (HistoricTaskInstance) historyService.createHistoricTaskInstanceQuery().includeTaskLocalVariables().finished().singleResult();
            variableMap = task.getTaskLocalVariables();
            assertEquals(2, variableMap.size());
            assertEquals(0, task.getProcessVariables().size());
            assertNotNull(variableMap.get("testVar"));
            assertEquals("someVariable", variableMap.get("testVar"));
            assertNotNull(variableMap.get("testVar2"));
            assertEquals(123, variableMap.get("testVar2"));
        }
    }

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
            assertEquals(2, variableMap.size());
            assertEquals(0, task.getProcessVariables().size());
            assertNotNull(variableMap.get("testVar"));
            assertEquals("someVariable", variableMap.get("testVar"));
            assertNotNull(variableMap.get("testVar2"));
            assertEquals(123, variableMap.get("testVar2"));

            List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery().list();
            assertEquals(3, tasks.size());

            task = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().or().taskAssignee("gonzo").taskVariableValueEquals("localVar", "nonExisting").endOr().singleResult();
            assertEquals(0, task.getProcessVariables().size());
            assertEquals(0, task.getTaskLocalVariables().size());

            Map<String, Object> startMap = new HashMap<>();
            startMap.put("processVar", true);
            runtimeService.startProcessInstanceByKey("oneTaskProcess", startMap);
            
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);

            task = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().or().taskAssignee("kermit").taskVariableValueEquals("localVar", "nonExisting").endOr().singleResult();
            assertEquals(1, task.getProcessVariables().size());
            assertEquals(0, task.getTaskLocalVariables().size());
            assertTrue((Boolean) task.getProcessVariables().get("processVar"));

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

            assertNotNull(task);
            assertEquals(1, task.getProcessVariables().size());
            assertEquals(0, task.getTaskLocalVariables().size());
            assertTrue((Boolean) task.getProcessVariables().get("processVar"));

            taskService.setVariable(task.getId(), "anotherProcessVar", 123);
            taskService.setVariableLocal(task.getId(), "localVar", "test");
            
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);

            task = historyService.createHistoricTaskInstanceQuery().includeTaskLocalVariables().or().taskAssignee("kermit").taskVariableValueEquals("localVar", "nonExisting").endOr().singleResult();
            assertEquals(0, task.getProcessVariables().size());
            assertEquals(1, task.getTaskLocalVariables().size());
            assertEquals("test", task.getTaskLocalVariables().get("localVar"));

            task = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().or().taskAssignee("kermit").taskVariableValueEquals("localVar", "nonExisting").endOr().singleResult();
            assertEquals(2, task.getProcessVariables().size());
            assertEquals(0, task.getTaskLocalVariables().size());
            assertEquals(true, task.getProcessVariables().get("processVar"));
            assertEquals(123, task.getProcessVariables().get("anotherProcessVar"));

            task = historyService.createHistoricTaskInstanceQuery()
                    .includeTaskLocalVariables()
                    .or()
                    .taskAssignee("nonexisting")
                    .taskVariableValueLike("testVar", "someVar%")
                    .endOr()
                    .singleResult();
            assertEquals(2, task.getTaskLocalVariables().size());
            assertEquals(0, task.getProcessVariables().size());
            assertEquals("someVariable", task.getTaskLocalVariables().get("testVar"));
            assertEquals(123, task.getTaskLocalVariables().get("testVar2"));

            task = historyService.createHistoricTaskInstanceQuery()
                    .includeTaskLocalVariables()
                    .or()
                    .taskAssignee("nonexisting")
                    .taskVariableValueLikeIgnoreCase("testVar", "somevar%")
                    .endOr()
                    .singleResult();
            assertEquals(2, task.getTaskLocalVariables().size());
            assertEquals(0, task.getProcessVariables().size());
            assertEquals("someVariable", task.getTaskLocalVariables().get("testVar"));
            assertEquals(123, task.getTaskLocalVariables().get("testVar2"));

            task = historyService.createHistoricTaskInstanceQuery()
                    .includeTaskLocalVariables()
                    .or()
                    .taskAssignee("nonexisting")
                    .taskVariableValueLike("testVar", "someVar2%")
                    .endOr()
                    .singleResult();
            assertNull(task);

            tasks = historyService.createHistoricTaskInstanceQuery().includeTaskLocalVariables()
                    .or()
                    .taskInvolvedUser("kermit")
                    .taskVariableValueEquals("localVar", "nonExisting")
                    .endOr()
                    .orderByTaskCreateTime().asc().list();
            assertEquals(3, tasks.size());
            assertEquals(1, tasks.get(0).getTaskLocalVariables().size());
            assertEquals("test", tasks.get(0).getTaskLocalVariables().get("test"));
            assertEquals(0, tasks.get(0).getProcessVariables().size());

            tasks = historyService.createHistoricTaskInstanceQuery().includeProcessVariables()
                    .or()
                    .taskInvolvedUser("kermit")
                    .taskVariableValueEquals("localVar", "nonExisting")
                    .endOr()
                    .orderByTaskCreateTime().asc().list();
            assertEquals(3, tasks.size());
            assertEquals(0, tasks.get(0).getProcessVariables().size());
            assertEquals(0, tasks.get(0).getTaskLocalVariables().size());

            task = historyService.createHistoricTaskInstanceQuery().includeTaskLocalVariables().taskAssignee("kermit").or().taskVariableValueEquals("localVar", "test")
                    .taskVariableValueEquals("localVar", "nonExisting").endOr().singleResult();
            assertEquals(0, task.getProcessVariables().size());
            assertEquals(1, task.getTaskLocalVariables().size());
            assertEquals("test", task.getTaskLocalVariables().get("localVar"));

            task = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().taskAssignee("kermit").or().taskVariableValueEquals("localVar", "test")
                    .taskVariableValueEquals("localVar", "nonExisting").endOr().singleResult();
            assertEquals(2, task.getProcessVariables().size());
            assertEquals(0, task.getTaskLocalVariables().size());
            assertEquals(true, task.getProcessVariables().get("processVar"));
            assertEquals(123, task.getProcessVariables().get("anotherProcessVar"));

            task = historyService.createHistoricTaskInstanceQuery().includeTaskLocalVariables().includeProcessVariables().or().taskAssignee("kermit").taskVariableValueEquals("localVar", "nonExisting")
                    .endOr().singleResult();
            assertEquals(2, task.getProcessVariables().size());
            assertEquals(1, task.getTaskLocalVariables().size());
            assertEquals("test", task.getTaskLocalVariables().get("localVar"));
            assertEquals(true, task.getProcessVariables().get("processVar"));
            assertEquals(123, task.getProcessVariables().get("anotherProcessVar"));

            task = historyService.createHistoricTaskInstanceQuery().taskAssignee("gonzo").singleResult();
            taskService.complete(task.getId());
            
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);
            
            task = historyService.createHistoricTaskInstanceQuery().includeTaskLocalVariables().or().finished().taskVariableValueEquals("localVar", "nonExisting").endOr().singleResult();
            variableMap = task.getTaskLocalVariables();
            assertEquals(2, variableMap.size());
            assertEquals(0, task.getProcessVariables().size());
            assertNotNull(variableMap.get("testVar"));
            assertEquals("someVariable", variableMap.get("testVar"));
            assertNotNull(variableMap.get("testVar2"));
            assertEquals(123, variableMap.get("testVar2"));
        }
    }

    @Deployment
    public void testOrQueryMultipleVariableValues() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            Map<String, Object> startMap = new HashMap<>();
            startMap.put("processVar", true);
            startMap.put("anotherProcessVar", 123);
            runtimeService.startProcessInstanceByKey("oneTaskProcess", startMap);

            startMap.put("anotherProcessVar", 999);
            runtimeService.startProcessInstanceByKey("oneTaskProcess", startMap);
            
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);

            HistoricTaskInstanceQuery query0 = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().or();
            for (int i = 0; i < 20; i++) {
                query0 = query0.processVariableValueEquals("anotherProcessVar", i);
            }
            query0 = query0.endOr();
            assertNull(query0.singleResult());

            HistoricTaskInstanceQuery query1 = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().or().processVariableValueEquals("anotherProcessVar", 123);
            for (int i = 0; i < 20; i++) {
                query1 = query1.processVariableValueEquals("anotherProcessVar", i);
            }
            query1 = query1.endOr();
            HistoricTaskInstance task = query1.singleResult();
            assertEquals(2, task.getProcessVariables().size());
            assertEquals(true, task.getProcessVariables().get("processVar"));
            assertEquals(123, task.getProcessVariables().get("anotherProcessVar"));
        }
    }

    @Deployment
    public void testCandidate() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);
            
            List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateUser("kermit").list();
            assertEquals(3, tasks.size());

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateUser("gonzo").list();
            assertEquals(0, tasks.size());

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateUser("fozzie").list();
            assertEquals(1, tasks.size());

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateGroup("management").list();
            assertEquals(1, tasks.size());
            List<String> groups = new ArrayList<>();
            groups.add("management");
            groups.add("accountancy");
            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateGroupIn(groups).list();
            assertEquals(1, tasks.size());

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateUser("kermit").taskCandidateGroupIn(groups).list();
            assertEquals(3, tasks.size());

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateUser("gonzo").taskCandidateGroupIn(groups).list();
            assertEquals(1, tasks.size());

            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            taskService.complete(task.getId());

            assertEquals(0, taskService.createTaskQuery().processInstanceId(processInstance.getId()).count());

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateUser("kermit").list();
            assertEquals(3, tasks.size());

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateUser("gonzo").list();
            assertEquals(0, tasks.size());

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateUser("fozzie").list();
            assertEquals(1, tasks.size());

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateGroup("management").list();
            assertEquals(1, tasks.size());

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateUser("kermit").taskCandidateGroup("management").list();
            assertEquals(3, tasks.size());

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateUser("gonzo").taskCandidateGroup("management").list();
            assertEquals(1, tasks.size());

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateUser("gonzo").taskCandidateGroup("invalid").list();
            assertEquals(0, tasks.size());

            tasks = historyService.createHistoricTaskInstanceQuery().taskCandidateGroupIn(groups).list();
            assertEquals(1, tasks.size());
        }
    }
    
    @Deployment(resources = { "org/flowable/engine/test/api/history/HistoricTaskAndVariablesQueryTest.testCandidate.bpmn20.xml" })
    public void testIgnoreAssigneeValue() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);

            String taskId = taskService
                    .createTaskQuery()
                    .processInstanceId(processInstance.getProcessInstanceId())
                    .list()
                    .get(0)
                    .getId();
            taskService.setAssignee(taskId, "kermit");
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);

            List<HistoricTaskInstance> tasks = historyService
                    .createHistoricTaskInstanceQuery()
                    .taskCandidateUser("kermit")
                    .list();
            assertEquals(2, tasks.size());

            tasks = historyService
                    .createHistoricTaskInstanceQuery()
                    .taskCandidateUser("kermit")
                    .ignoreAssigneeValue()
                    .list();
            assertEquals(3, tasks.size());

            tasks = historyService
                    .createHistoricTaskInstanceQuery()
                    .taskCandidateGroup("management")
                    .list();
            assertEquals(0, tasks.size());

            tasks = historyService
                    .createHistoricTaskInstanceQuery()
                    .taskCandidateGroup("management")
                    .ignoreAssigneeValue()
                    .list();
            assertEquals(1, tasks.size());
        }
    }

    @Deployment(resources = {"org/flowable/engine/test/api/history/HistoricTaskAndVariablesQueryTest.testCandidate.bpmn20.xml"})
    public void testIgnoreAssigneeValueOr() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);

            String taskId = taskService
                    .createTaskQuery()
                    .processInstanceId(processInstance.getProcessInstanceId())
                    .list()
                    .get(0)
                    .getId();
            taskService.setAssignee(taskId, "kermit");
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);

            List<HistoricTaskInstance>  tasks = historyService.createHistoricTaskInstanceQuery()
                    .or()
                    .taskCandidateUser("kermit")
                    .taskAssignee("gonzo")
                    .ignoreAssigneeValue()
                    .endOr()
                    .list();

            assertEquals(4, tasks.size());

            tasks = historyService.createHistoricTaskInstanceQuery()
                    .or()
                    .taskCandidateUser("kermit")
                    .taskAssignee("gonzo")
                    .endOr()
                    .list();
            assertEquals(3, tasks.size());
        }
    }

    public void testQueryWithPagingAndVariables() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().includeTaskLocalVariables().orderByTaskPriority().desc().listPage(0, 1);
            assertEquals(1, tasks.size());
            HistoricTaskInstance task = tasks.get(0);
            Map<String, Object> variableMap = task.getTaskLocalVariables();
            assertEquals(2, variableMap.size());
            assertEquals("someVariable", variableMap.get("testVar"));
            assertEquals(123, variableMap.get("testVar2"));

            tasks = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().includeTaskLocalVariables().orderByTaskPriority().asc().listPage(1, 2);
            assertEquals(2, tasks.size());
            task = tasks.get(1);
            variableMap = task.getTaskLocalVariables();
            assertEquals(2, variableMap.size());
            assertEquals("someVariable", variableMap.get("testVar"));
            assertEquals(123, variableMap.get("testVar2"));

            tasks = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().includeTaskLocalVariables().orderByTaskPriority().asc().listPage(2, 4);
            assertEquals(1, tasks.size());
            task = tasks.get(0);
            variableMap = task.getTaskLocalVariables();
            assertEquals(2, variableMap.size());
            assertEquals("someVariable", variableMap.get("testVar"));
            assertEquals(123, variableMap.get("testVar2"));

            tasks = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().includeTaskLocalVariables().orderByTaskPriority().asc().listPage(4, 2);
            assertEquals(0, tasks.size());
        }
    }

    public void testQueryWithPagingVariablesAndIdentityLinks() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            
            List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().includeTaskLocalVariables().includeIdentityLinks().orderByTaskPriority().desc().listPage(0, 1);
            assertEquals(1, tasks.size());
            HistoricTaskInstance task = tasks.get(0);
            Map<String, Object> variableMap = task.getTaskLocalVariables();
            assertEquals(2, variableMap.size());
            assertEquals("someVariable", variableMap.get("testVar"));
            assertEquals(123, variableMap.get("testVar2"));
            assertEquals(1, task.getIdentityLinks().size());
            IdentityLinkInfo identityLink = task.getIdentityLinks().get(0);
            assertNull(identityLink.getProcessInstanceId());
            assertEquals("assignee", identityLink.getType());
            assertNull(identityLink.getGroupId());
            assertEquals("gonzo", identityLink.getUserId());
            assertEquals(task.getId(), identityLink.getTaskId());

            tasks = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().includeTaskLocalVariables().includeIdentityLinks().orderByTaskPriority().asc().listPage(1, 2);
            assertEquals(2, tasks.size());
            task = tasks.get(1);
            variableMap = task.getTaskLocalVariables();
            assertEquals(2, variableMap.size());
            assertEquals("someVariable", variableMap.get("testVar"));
            assertEquals(123, variableMap.get("testVar2"));
            assertEquals(1, task.getIdentityLinks().size());
            identityLink = task.getIdentityLinks().get(0);
            assertNull(identityLink.getProcessInstanceId());
            assertEquals("assignee", identityLink.getType());
            assertNull(identityLink.getGroupId());
            assertEquals("gonzo", identityLink.getUserId());
            assertEquals(task.getId(), identityLink.getTaskId());

            tasks = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().includeTaskLocalVariables().includeIdentityLinks().orderByTaskPriority().asc().listPage(2, 4);
            assertEquals(1, tasks.size());
            task = tasks.get(0);
            variableMap = task.getTaskLocalVariables();
            assertEquals(2, variableMap.size());
            assertEquals("someVariable", variableMap.get("testVar"));
            assertEquals(123, variableMap.get("testVar2"));
            assertEquals(1, task.getIdentityLinks().size());
            identityLink = task.getIdentityLinks().get(0);
            assertNull(identityLink.getProcessInstanceId());
            assertEquals("assignee", identityLink.getType());
            assertNull(identityLink.getGroupId());
            assertEquals("gonzo", identityLink.getUserId());
            assertEquals(task.getId(), identityLink.getTaskId());

            tasks = historyService.createHistoricTaskInstanceQuery().includeProcessVariables().includeTaskLocalVariables().includeIdentityLinks().orderByTaskPriority().asc().listPage(4, 2);
            assertEquals(0, tasks.size());
        }
    }
    
    @Deployment(resources = { "org/flowable/engine/test/api/history/HistoricTaskAndVariablesQueryTest.testCandidate.bpmn20.xml" })
    public void testQueryVariableExists() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricTaskInstance task = historyService.createHistoricTaskInstanceQuery().taskVariableExists("testVar").singleResult();
            assertNotNull(task);
            
            List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery().taskVariableNotExists("testVar").list();
            assertEquals(2, tasks.size());
            
            tasks = historyService.createHistoricTaskInstanceQuery().or().taskVariableNotExists("testVar").processDefinitionId("unexisting").list();
            assertEquals(2, tasks.size());
            
            tasks = historyService.createHistoricTaskInstanceQuery().or().taskVariableExists("testVar").processDefinitionId("unexisting").list();
            assertEquals(1, tasks.size());
            
            tasks = historyService.createHistoricTaskInstanceQuery().or().taskVariableNotExists("testVar").endOr().or().processDefinitionId("unexisting").list();
            assertEquals(0, tasks.size());
            
            tasks = historyService.createHistoricTaskInstanceQuery().or().taskVariableExists("testVar").endOr().or().processDefinitionId("unexisting").list();
            assertEquals(0, tasks.size());
            
            Map<String, Object> varMap = Collections.singletonMap("processVar", (Object) "test");
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", varMap);
            
            waitForHistoryJobExecutorToProcessAllJobs(5000, 250);
            
            tasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).processVariableExists("processVar").list();
            assertEquals(1, tasks.size());
            
            tasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).processVariableNotExists("processVar").list();
            assertEquals(0, tasks.size());
            
            tasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId())
                            .or().processVariableExists("processVar").processDefinitionId("undexisting").endOr().list();
            assertEquals(1, tasks.size());
            
            tasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId())
                            .or().processVariableNotExists("processVar").processInstanceId(processInstance.getId()).endOr().list();
            assertEquals(1, tasks.size());
            
            runtimeService.setVariable(processInstance.getId(), "processVar2", "test2");
            
            waitForHistoryJobExecutorToProcessAllJobs(5000, 250);
            
            tasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId())
                            .processVariableExists("processVar").processVariableValueEquals("processVar2", "test2").list();
            assertEquals(1, tasks.size());
            
            tasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId())
                            .processVariableNotExists("processVar").processVariableValueEquals("processVar2", "test2").list();
            assertEquals(0, tasks.size());
            
            tasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId())
                            .or().processVariableExists("processVar").processVariableValueEquals("processVar2", "test2").endOr().list();
            assertEquals(1, tasks.size());
            
            tasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId())
                            .or().processVariableNotExists("processVar").processVariableValueEquals("processVar2", "test2").endOr().list();
            assertEquals(1, tasks.size());
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml" })
    public void testWithoutDueDateQuery() throws Exception {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);
            
            HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).withoutTaskDueDate().singleResult();
            assertNotNull(historicTask);
            assertNull(historicTask.getDueDate());

            // Set due-date on task
            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            Date dueDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("01/02/2003 01:12:13");
            task.setDueDate(dueDate);
            taskService.saveTask(task);
            
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);

            assertEquals(0, historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).withoutTaskDueDate().count());

            task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

            // Clear due-date on task
            task.setDueDate(null);
            taskService.saveTask(task);
            
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);

            assertEquals(1, historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).withoutTaskDueDate().count());
        }
    }

    // Unit test for https://activiti.atlassian.net/browse/ACT-4152
    public void testQueryWithIncludeTaskVariableAndTaskCategory() {
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery().taskAssignee("gonzo").list();
        for (HistoricTaskInstance task : tasks) {
            assertNotNull(task.getCategory());
            assertEquals("testCategory", task.getCategory());
        }

        tasks = historyService.createHistoricTaskInstanceQuery().taskAssignee("gonzo").includeTaskLocalVariables().list();
        for (HistoricTaskInstance task : tasks) {
            assertNotNull(task.getCategory());
            assertEquals("testCategory", task.getCategory());
        }

        tasks = historyService.createHistoricTaskInstanceQuery().taskAssignee("gonzo").includeProcessVariables().list();
        for (HistoricTaskInstance task : tasks) {
            assertNotNull(task.getCategory());
            assertEquals("testCategory", task.getCategory());
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
