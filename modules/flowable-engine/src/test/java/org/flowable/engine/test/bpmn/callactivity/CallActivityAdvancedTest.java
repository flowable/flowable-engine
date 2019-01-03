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

package org.flowable.engine.test.bpmn.callactivity;

import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.history.DeleteReason;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.entitylink.api.EntityLinkType;
import org.flowable.entitylink.api.HierarchyType;
import org.flowable.entitylink.api.history.HistoricEntityLink;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 * @author Nils Preusker
 * @author Bernd Ruecker
 * @author Joram Barrez
 */
public class CallActivityAdvancedTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcess.bpmn20.xml", 
                    "org/flowable/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml" })
    public void testCallSimpleSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callSimpleSubProcess");

        // one task in the subprocess should be active after starting the process instance
        TaskQuery taskQuery = taskService.createTaskQuery();
        Task taskBeforeSubProcess = taskQuery.singleResult();
        assertEquals("Task before subprocess", taskBeforeSubProcess.getName());
        
        Task childTask = taskService.createTaskQuery().processInstanceIdWithChildren(processInstance.getId()).singleResult();
        assertEquals(taskBeforeSubProcess.getId(), childTask.getId());

        // Completing the task continues the process which leads to calling the subprocess
        taskService.complete(taskBeforeSubProcess.getId());
        Task taskInSubProcess = taskQuery.singleResult();
        assertEquals("Task in subprocess", taskInSubProcess.getName());
        Execution execution = runtimeService.createExecutionQuery().executionId(taskInSubProcess.getExecutionId()).singleResult();
        assertEquals(processInstance.getId(), execution.getRootProcessInstanceId());
        assertNotEquals(execution.getProcessInstanceId(), execution.getRootProcessInstanceId());
        managementService.executeCommand(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                ExecutionEntity rootProcessInstance = ((ExecutionEntity) execution).getRootProcessInstance();
                assertNotNull(rootProcessInstance);
                assertEquals(processInstance.getId(), rootProcessInstance.getId());
                return null;
            }
            
        });
        
        List<EntityLink> entityLinks = runtimeService.getEntityLinkChildrenForProcessInstance(processInstance.getId());
        assertEquals(3, entityLinks.size());
        EntityLink entityLinkSubProcess = null;
        EntityLink entityLinkTask = null;
        EntityLink entityLinkSubTask = null;
        for (EntityLink entityLink : entityLinks) {
            if (ScopeTypes.TASK.equals(entityLink.getReferenceScopeType())) {
                if (taskBeforeSubProcess.getId().equals(entityLink.getReferenceScopeId())) {
                    entityLinkTask = entityLink;
                } else {
                    entityLinkSubTask = entityLink;
                }
                
            } else if (ScopeTypes.BPMN.equals(entityLink.getReferenceScopeType())) {
                entityLinkSubProcess = entityLink;
            }
        }
        assertNotNull(entityLinkSubProcess);
        assertNotNull(entityLinkTask);
        assertNotNull(entityLinkSubTask);
        
        assertEquals(processInstance.getId(), entityLinkSubProcess.getScopeId());
        assertEquals(ScopeTypes.BPMN, entityLinkSubProcess.getScopeType());
        assertNull(entityLinkSubProcess.getScopeDefinitionId());
        assertEquals(execution.getProcessInstanceId(), entityLinkSubProcess.getReferenceScopeId());
        assertEquals(ScopeTypes.BPMN, entityLinkSubProcess.getReferenceScopeType());
        assertNull(entityLinkSubProcess.getReferenceScopeDefinitionId());
        assertEquals(EntityLinkType.CHILD, entityLinkSubProcess.getLinkType());
        assertNotNull(entityLinkSubProcess.getCreateTime());
        assertEquals(HierarchyType.ROOT, entityLinkSubProcess.getHierarchyType());
        
        assertEquals(processInstance.getId(), entityLinkTask.getScopeId());
        assertEquals(ScopeTypes.BPMN, entityLinkTask.getScopeType());
        assertNull(entityLinkTask.getScopeDefinitionId());
        assertEquals(taskBeforeSubProcess.getId(), entityLinkTask.getReferenceScopeId());
        assertEquals(ScopeTypes.TASK, entityLinkTask.getReferenceScopeType());
        assertNull(entityLinkTask.getReferenceScopeDefinitionId());
        assertEquals(EntityLinkType.CHILD, entityLinkTask.getLinkType());
        assertNotNull(entityLinkTask.getCreateTime());
        assertEquals(HierarchyType.ROOT, entityLinkTask.getHierarchyType());
        
        assertEquals(processInstance.getId(), entityLinkSubTask.getScopeId());
        assertEquals(ScopeTypes.BPMN, entityLinkSubTask.getScopeType());
        assertNull(entityLinkSubTask.getScopeDefinitionId());
        assertEquals(taskInSubProcess.getId(), entityLinkSubTask.getReferenceScopeId());
        assertEquals(ScopeTypes.TASK, entityLinkSubTask.getReferenceScopeType());
        assertNull(entityLinkSubTask.getReferenceScopeDefinitionId());
        assertEquals(EntityLinkType.CHILD, entityLinkSubTask.getLinkType());
        assertNotNull(entityLinkSubTask.getCreateTime());
        assertEquals(HierarchyType.ROOT, entityLinkSubTask.getHierarchyType());
        
        entityLinks = runtimeService.getEntityLinkChildrenForProcessInstance(execution.getProcessInstanceId());
        assertEquals(1, entityLinks.size());
        EntityLink entityLink = entityLinks.get(0);
        
        assertEquals(execution.getProcessInstanceId(), entityLink.getScopeId());
        assertEquals(ScopeTypes.BPMN, entityLink.getScopeType());
        assertNull(entityLink.getScopeDefinitionId());
        assertEquals(taskInSubProcess.getId(), entityLink.getReferenceScopeId());
        assertEquals(ScopeTypes.TASK, entityLink.getReferenceScopeType());
        assertNull(entityLink.getReferenceScopeDefinitionId());
        assertEquals(EntityLinkType.CHILD, entityLink.getLinkType());
        assertNotNull(entityLink.getCreateTime());
        assertEquals(HierarchyType.PARENT, entityLink.getHierarchyType());
        
        childTask = taskService.createTaskQuery().processInstanceIdWithChildren(processInstance.getId()).singleResult();
        assertEquals(taskInSubProcess.getId(), childTask.getId());
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricTaskInstance> childHistoricTasks = historyService.createHistoricTaskInstanceQuery()
                            .processInstanceIdWithChildren(processInstance.getId())
                            .list();
            assertEquals(2, childHistoricTasks.size());
            List<String> taskIds = new ArrayList<>();
            for (HistoricTaskInstance task : childHistoricTasks) {
                taskIds.add(task.getId());
            }
            assertTrue(taskIds.contains(taskBeforeSubProcess.getId()));
            assertTrue(taskIds.contains(taskInSubProcess.getId()));
        }
        
        childTask = taskService.createTaskQuery().processInstanceIdWithChildren(execution.getProcessInstanceId()).singleResult();
        assertEquals(taskInSubProcess.getId(), childTask.getId());

        // Completing the task in the subprocess, finishes the subprocess
        taskService.complete(taskInSubProcess.getId());
        Task taskAfterSubProcess = taskQuery.singleResult();
        assertEquals("Task after subprocess", taskAfterSubProcess.getName());
        
        childTask = taskService.createTaskQuery().processInstanceIdWithChildren(processInstance.getId()).singleResult();
        assertEquals(taskAfterSubProcess.getId(), childTask.getId());

        // Completing this task end the process instance
        taskService.complete(taskAfterSubProcess.getId());
        assertProcessEnded(processInstance.getId());

        // Validate subprocess history
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // Subprocess should have initial activity set
            HistoricProcessInstance historicProcess = historyService.createHistoricProcessInstanceQuery().processInstanceId(taskInSubProcess.getProcessInstanceId()).singleResult();
            assertNotNull(historicProcess);
            assertEquals("theStart", historicProcess.getStartActivityId());

            List<HistoricActivityInstance> subProcesshistoricInstances = historyService.createHistoricActivityInstanceQuery().processInstanceId(taskInSubProcess.getProcessInstanceId()).list();

            // Should contain a start-event, the task and an end-event
            assertEquals(5L, subProcesshistoricInstances.size());
            Set<String> expectedActivities = new HashSet<>(Arrays.asList(new String[]{"theStart", "flow1", "task", "flow2", "theEnd"}));

            for (HistoricActivityInstance act : subProcesshistoricInstances) {
                expectedActivities.remove(act.getActivityId());
            }
            assertTrue("Not all expected activities were found in the history", expectedActivities.isEmpty());

            List<HistoricActivityInstance> historicInstances = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getProcessInstanceId()).list();

            assertEquals(9L, historicInstances.size());
            expectedActivities = new HashSet<>(Arrays.asList(new String[]{"theStart", "flow1", "taskBeforeSubProcess", "flow2", "callSubProcess", "flow3",
                "taskAfterSubProcess", "flow4", "theEnd"
            }));

            for (HistoricActivityInstance act : historicInstances) {
                expectedActivities.remove(act.getActivityId());
            }
            assertTrue("Not all expected activities were found in the history", expectedActivities.isEmpty());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                List<HistoricEntityLink> historicEntityLinks = historyService.getHistoricEntityLinkChildrenForProcessInstance(processInstance.getId());
                assertEquals(4, historicEntityLinks.size());
                HistoricEntityLink historicEntityLinkSubProcess = null;
                HistoricEntityLink historicEntityLinkTask = null;
                HistoricEntityLink historicEntityLinkSubTask = null;
                HistoricEntityLink historicEntityLinkAfterTask = null;
                for (HistoricEntityLink historicEntityLink : historicEntityLinks) {
                    if (ScopeTypes.TASK.equals(historicEntityLink.getReferenceScopeType())) {
                        if (taskBeforeSubProcess.getId().equals(historicEntityLink.getReferenceScopeId())) {
                            historicEntityLinkTask = historicEntityLink;
                        } else if (taskAfterSubProcess.getId().equals(historicEntityLink.getReferenceScopeId())) {
                            historicEntityLinkAfterTask = historicEntityLink;
                        } else {
                            historicEntityLinkSubTask = historicEntityLink;
                        }
                        
                    } else if (ScopeTypes.BPMN.equals(historicEntityLink.getReferenceScopeType())) {
                        historicEntityLinkSubProcess = historicEntityLink;
                    }
                }
                assertNotNull(historicEntityLinkSubProcess);
                assertNotNull(historicEntityLinkTask);
                assertNotNull(historicEntityLinkSubTask);
                
                assertEquals(processInstance.getId(), historicEntityLinkSubProcess.getScopeId());
                assertEquals(ScopeTypes.BPMN, historicEntityLinkSubProcess.getScopeType());
                assertNull(historicEntityLinkSubProcess.getScopeDefinitionId());
                assertEquals(execution.getProcessInstanceId(), historicEntityLinkSubProcess.getReferenceScopeId());
                assertEquals(ScopeTypes.BPMN, historicEntityLinkSubProcess.getReferenceScopeType());
                assertNull(historicEntityLinkSubProcess.getReferenceScopeDefinitionId());
                assertEquals(EntityLinkType.CHILD, historicEntityLinkSubProcess.getLinkType());
                assertNotNull(historicEntityLinkSubProcess.getCreateTime());
                assertEquals(HierarchyType.ROOT, historicEntityLinkSubProcess.getHierarchyType());
                
                assertEquals(processInstance.getId(), historicEntityLinkTask.getScopeId());
                assertEquals(ScopeTypes.BPMN, historicEntityLinkTask.getScopeType());
                assertNull(historicEntityLinkTask.getScopeDefinitionId());
                assertEquals(taskBeforeSubProcess.getId(), historicEntityLinkTask.getReferenceScopeId());
                assertEquals(ScopeTypes.TASK, historicEntityLinkTask.getReferenceScopeType());
                assertNull(historicEntityLinkTask.getReferenceScopeDefinitionId());
                assertEquals(EntityLinkType.CHILD, historicEntityLinkTask.getLinkType());
                assertNotNull(historicEntityLinkTask.getCreateTime());
                assertEquals(HierarchyType.ROOT, historicEntityLinkTask.getHierarchyType());
                
                assertEquals(processInstance.getId(), historicEntityLinkSubTask.getScopeId());
                assertEquals(ScopeTypes.BPMN, historicEntityLinkSubTask.getScopeType());
                assertNull(historicEntityLinkSubTask.getScopeDefinitionId());
                assertEquals(taskInSubProcess.getId(), historicEntityLinkSubTask.getReferenceScopeId());
                assertEquals(ScopeTypes.TASK, historicEntityLinkSubTask.getReferenceScopeType());
                assertNull(historicEntityLinkSubTask.getReferenceScopeDefinitionId());
                assertEquals(EntityLinkType.CHILD, historicEntityLinkSubTask.getLinkType());
                assertNotNull(historicEntityLinkSubTask.getCreateTime());
                assertEquals(HierarchyType.ROOT, historicEntityLinkSubTask.getHierarchyType());
                
                assertEquals(processInstance.getId(), historicEntityLinkAfterTask.getScopeId());
                assertEquals(ScopeTypes.BPMN, historicEntityLinkAfterTask.getScopeType());
                assertNull(historicEntityLinkAfterTask.getScopeDefinitionId());
                assertEquals(taskAfterSubProcess.getId(), historicEntityLinkAfterTask.getReferenceScopeId());
                assertEquals(ScopeTypes.TASK, historicEntityLinkAfterTask.getReferenceScopeType());
                assertNull(historicEntityLinkAfterTask.getReferenceScopeDefinitionId());
                assertEquals(EntityLinkType.CHILD, historicEntityLinkAfterTask.getLinkType());
                assertNotNull(historicEntityLinkAfterTask.getCreateTime());
                assertEquals(HierarchyType.ROOT, historicEntityLinkAfterTask.getHierarchyType());
                
                historicEntityLinks = historyService.getHistoricEntityLinkChildrenForProcessInstance(execution.getProcessInstanceId());
                assertEquals(1, historicEntityLinks.size());
                HistoricEntityLink historicEntityLink = historicEntityLinks.get(0);
                
                assertEquals(execution.getProcessInstanceId(), historicEntityLink.getScopeId());
                assertEquals(ScopeTypes.BPMN, historicEntityLink.getScopeType());
                assertNull(historicEntityLink.getScopeDefinitionId());
                assertEquals(taskInSubProcess.getId(), historicEntityLink.getReferenceScopeId());
                assertEquals(ScopeTypes.TASK, historicEntityLink.getReferenceScopeType());
                assertNull(historicEntityLink.getReferenceScopeDefinitionId());
                assertEquals(EntityLinkType.CHILD, historicEntityLink.getLinkType());
                assertNotNull(historicEntityLink.getCreateTime());
                assertEquals(HierarchyType.PARENT, historicEntityLink.getHierarchyType());
                
                List<HistoricTaskInstance> childHistoricTasks = historyService.createHistoricTaskInstanceQuery()
                                .processInstanceIdWithChildren(processInstance.getId())
                                .list();
                assertEquals(3, childHistoricTasks.size());
                List<String> taskIds = new ArrayList<>();
                for (HistoricTaskInstance task : childHistoricTasks) {
                    taskIds.add(task.getId());
                }
                assertTrue(taskIds.contains(taskBeforeSubProcess.getId()));
                assertTrue(taskIds.contains(taskInSubProcess.getId()));
                assertTrue(taskIds.contains(taskAfterSubProcess.getId()));
                
                HistoricTaskInstance childHistoricTask = historyService.createHistoricTaskInstanceQuery()
                                .processInstanceIdWithChildren(execution.getProcessInstanceId())
                                .singleResult();
                assertEquals(taskInSubProcess.getId(), childHistoricTask.getId());
            }
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcessWithExpressions.bpmn20.xml",
            "org/flowable/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml" })
    public void testCallSimpleSubProcessWithExpressions() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callSimpleSubProcess");

        // one task in the subprocess should be active after starting the process instance
        TaskQuery taskQuery = taskService.createTaskQuery();
        Task taskBeforeSubProcess = taskQuery.singleResult();
        assertEquals("Task before subprocess", taskBeforeSubProcess.getName());

        // Completing the task continues the process which leads to calling the
        // subprocess. The sub process we want to call is passed in as a variable into this task
        taskService.setVariable(taskBeforeSubProcess.getId(), "simpleSubProcessExpression", "simpleSubProcess");
        taskService.complete(taskBeforeSubProcess.getId());
        Task taskInSubProcess = taskQuery.singleResult();
        assertEquals("Task in subprocess", taskInSubProcess.getName());

        // Completing the task in the subprocess, finishes the subprocess
        taskService.complete(taskInSubProcess.getId());
        Task taskAfterSubProcess = taskQuery.singleResult();
        assertEquals("Task after subprocess", taskAfterSubProcess.getName());

        // Completing this task end the process instance
        taskService.complete(taskAfterSubProcess.getId());
        assertProcessEnded(processInstance.getId());
    }

    /**
     * Test case for a possible tricky case: reaching the end event of the subprocess leads to an end event in the super process instance.
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/callactivity/CallActivity.testSubProcessEndsSuperProcess.bpmn20.xml",
            "org/flowable/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml" })
    public void testSubProcessEndsSuperProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessEndsSuperProcess");

        // one task in the subprocess should be active after starting the process instance
        TaskQuery taskQuery = taskService.createTaskQuery();
        Task taskBeforeSubProcess = taskQuery.singleResult();
        assertEquals("Task in subprocess", taskBeforeSubProcess.getName());

        // Completing this task ends the subprocess which leads to the end of the whole process instance
        taskService.complete(taskBeforeSubProcess.getId());
        assertProcessEnded(processInstance.getId());
        assertEquals(0, runtimeService.createExecutionQuery().list().size());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/callactivity/CallActivity.testCallParallelSubProcess.bpmn20.xml",
            "org/flowable/engine/test/bpmn/callactivity/simpleParallelSubProcess.bpmn20.xml" })
    public void testCallParallelSubProcess() {
        runtimeService.startProcessInstanceByKey("callParallelSubProcess");

        // The two tasks in the parallel subprocess should be active
        TaskQuery taskQuery = taskService.createTaskQuery().orderByTaskName().asc();
        List<Task> tasks = taskQuery.list();
        assertEquals(2, tasks.size());

        Task taskA = tasks.get(0);
        Task taskB = tasks.get(1);
        assertEquals("Task A", taskA.getName());
        assertEquals("Task B", taskB.getName());

        // Completing the first task should not end the subprocess
        taskService.complete(taskA.getId());
        assertEquals(1, taskQuery.list().size());

        // Completing the second task should end the subprocess and end the whole process instance
        taskService.complete(taskB.getId());
        assertEquals(0, runtimeService.createExecutionQuery().count());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/callactivity/CallActivity.testCallSequentialSubProcess.bpmn20.xml",
            "org/flowable/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcessWithExpressions.bpmn20.xml", "org/flowable/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml",
            "org/flowable/engine/test/bpmn/callactivity/simpleSubProcess2.bpmn20.xml" })
    public void testCallSequentialSubProcessWithExpressions() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callSequentialSubProcess");

        // FIRST sub process calls simpleSubProcess

        // one task in the subprocess should be active after starting the process instance
        TaskQuery taskQuery = taskService.createTaskQuery();
        Task taskBeforeSubProcess = taskQuery.singleResult();
        assertEquals("Task before subprocess", taskBeforeSubProcess.getName());

        // Completing the task continues the process which leads to calling the
        // subprocess. The sub process we want to call is passed in as a variable into this task
        taskService.setVariable(taskBeforeSubProcess.getId(), "simpleSubProcessExpression", "simpleSubProcess");
        taskService.complete(taskBeforeSubProcess.getId());
        Task taskInSubProcess = taskQuery.singleResult();
        assertEquals("Task in subprocess", taskInSubProcess.getName());

        // Completing the task in the subprocess, finishes the subprocess
        taskService.complete(taskInSubProcess.getId());
        Task taskAfterSubProcess = taskQuery.singleResult();
        assertEquals("Task after subprocess", taskAfterSubProcess.getName());

        // Completing this task end the process instance
        taskService.complete(taskAfterSubProcess.getId());

        // SECOND sub process calls simpleSubProcess2

        // one task in the subprocess should be active after starting the process instance
        taskQuery = taskService.createTaskQuery();
        taskBeforeSubProcess = taskQuery.singleResult();
        assertEquals("Task before subprocess", taskBeforeSubProcess.getName());

        // Completing the task continues the process which leads to calling the
        // subprocess. The sub process we want to call is passed in as a variable into this task
        taskService.setVariable(taskBeforeSubProcess.getId(), "simpleSubProcessExpression", "simpleSubProcess2");
        taskService.complete(taskBeforeSubProcess.getId());
        taskInSubProcess = taskQuery.singleResult();
        assertEquals("Task in subprocess 2", taskInSubProcess.getName());

        // Completing the task in the subprocess, finishes the subprocess
        taskService.complete(taskInSubProcess.getId());
        taskAfterSubProcess = taskQuery.singleResult();
        assertEquals("Task after subprocess", taskAfterSubProcess.getName());

        // Completing this task end the process instance
        taskService.complete(taskAfterSubProcess.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/callactivity/CallActivity.testTimerOnCallActivity.bpmn20.xml", "org/flowable/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml" })
    public void testTimerOnCallActivity() {
        Date startTime = processEngineConfiguration.getClock().getCurrentTime();

        // After process start, the task in the subprocess should be active
        ProcessInstance pi1 = runtimeService.startProcessInstanceByKey("timerOnCallActivity");
        TaskQuery taskQuery = taskService.createTaskQuery();
        Task taskInSubProcess = taskQuery.singleResult();
        assertEquals("Task in subprocess", taskInSubProcess.getName());

        ProcessInstance pi2 = runtimeService.createProcessInstanceQuery().superProcessInstanceId(pi1.getId()).singleResult();

        // When the timer on the subprocess is fired, the complete subprocess is destroyed
        processEngineConfiguration.getClock().setCurrentTime(new Date(startTime.getTime() + (6 * 60 * 1000))); // + 6 minutes, timer fires on 5 minutes
        waitForJobExecutorToProcessAllJobs(10000, 7000L);

        Task escalatedTask = taskQuery.singleResult();
        assertEquals("Escalated Task", escalatedTask.getName());

        // Completing the task ends the complete process
        taskService.complete(escalatedTask.getId());
        assertEquals(0, runtimeService.createExecutionQuery().list().size());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertTrue(historyService.createHistoricProcessInstanceQuery().processInstanceId(pi2.getId()).singleResult()
                    .getDeleteReason().startsWith(DeleteReason.BOUNDARY_EVENT_INTERRUPTING));
            assertHistoricTasksDeleteReason(pi2, DeleteReason.BOUNDARY_EVENT_INTERRUPTING, "Task in subprocess");
            assertHistoricActivitiesDeleteReason(pi1, DeleteReason.BOUNDARY_EVENT_INTERRUPTING, "callSubProcess");
            assertHistoricActivitiesDeleteReason(pi2, DeleteReason.BOUNDARY_EVENT_INTERRUPTING, "task");
        }
    }

    /**
     * Test case for handing over process variables to a sub process
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/callactivity/CallActivity.testSubProcessDataInputOutput.bpmn20.xml",
            "org/flowable/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml" })
    public void testSubProcessWithDataInputOutput() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("superVariable", "Hello from the super process.");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessDataInputOutput", vars);

        // one task in the subprocess should be active after starting the
        // process instance
        TaskQuery taskQuery = taskService.createTaskQuery();
        Task taskBeforeSubProcess = taskQuery.singleResult();
        assertEquals("Task in subprocess", taskBeforeSubProcess.getName());
        assertEquals("Hello from the super process.", runtimeService.getVariable(taskBeforeSubProcess.getProcessInstanceId(), "subVariable"));
        assertEquals("Hello from the super process.", taskService.getVariable(taskBeforeSubProcess.getId(), "subVariable"));

        runtimeService.setVariable(taskBeforeSubProcess.getProcessInstanceId(), "subVariable", "Hello from sub process.");

        // super variable is unchanged
        assertEquals("Hello from the super process.", runtimeService.getVariable(processInstance.getId(), "superVariable"));

        // Completing this task ends the subprocess which leads to a task in the
        // super process
        taskService.complete(taskBeforeSubProcess.getId());

        // one task in the subprocess should be active after starting the
        // process instance
        Task taskAfterSubProcess = taskQuery.singleResult();
        assertEquals("Task in super process", taskAfterSubProcess.getName());
        assertEquals("Hello from sub process.", runtimeService.getVariable(processInstance.getId(), "superVariable"));
        assertEquals("Hello from sub process.", taskService.getVariable(taskAfterSubProcess.getId(), "superVariable"));

        vars.clear();
        vars.put("x", 5l);

        // Completing this task ends the super process which leads to a task in
        // the super process
        taskService.complete(taskAfterSubProcess.getId(), vars);

        // now we are the second time in the sub process but passed variables
        // via expressions
        Task taskInSecondSubProcess = taskQuery.singleResult();
        assertEquals("Task in subprocess", taskInSecondSubProcess.getName());
        assertEquals(10l, runtimeService.getVariable(taskInSecondSubProcess.getProcessInstanceId(), "y"));
        assertEquals(10l, taskService.getVariable(taskInSecondSubProcess.getId(), "y"));

        // Completing this task ends the subprocess which leads to a task in the super process
        taskService.complete(taskInSecondSubProcess.getId());

        // one task in the subprocess should be active after starting the process instance
        Task taskAfterSecondSubProcess = taskQuery.singleResult();
        assertEquals("Task in super process", taskAfterSecondSubProcess.getName());
        assertEquals(15l, runtimeService.getVariable(taskAfterSecondSubProcess.getProcessInstanceId(), "z"));
        assertEquals(15l, taskService.getVariable(taskAfterSecondSubProcess.getId(), "z"));

        // and end last task in Super process
        taskService.complete(taskAfterSecondSubProcess.getId());

        assertProcessEnded(processInstance.getId());
        assertEquals(0, runtimeService.createExecutionQuery().list().size());
    }

    /**
     * Test case for deleting a sub process
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/callactivity/CallActivity.testTwoSubProcesses.bpmn20.xml", "org/flowable/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml" })
    public void testTwoSubProcesses() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callTwoSubProcesses");

        List<ProcessInstance> instanceList = runtimeService.createProcessInstanceQuery().list();
        assertNotNull(instanceList);
        assertEquals(3, instanceList.size());

        List<Task> taskList = taskService.createTaskQuery().list();
        assertNotNull(taskList);
        assertEquals(2, taskList.size());

        runtimeService.deleteProcessInstance(processInstance.getId(), "Test cascading");

        instanceList = runtimeService.createProcessInstanceQuery().list();
        assertNotNull(instanceList);
        assertEquals(0, instanceList.size());

        taskList = taskService.createTaskQuery().list();
        assertNotNull(taskList);
        assertEquals(0, taskList.size());
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/callactivity/CallActivity.testStartUserIdSetWhenLooping.bpmn20.xml",
            "org/flowable/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
    })
    public void testStartUserIdSetWhenLooping() {
        identityService.setAuthenticatedUserId("kermit");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("loopingCallActivity", CollectionUtil.singletonMap("input", 0));
        for (int i = 1; i < 4; i++) {
            Task task = taskService.createTaskQuery().singleResult();
            assertEquals("Task in subprocess", task.getName());
            identityService.setAuthenticatedUserId("kermit");
            taskService.complete(task.getId(), CollectionUtil.singletonMap("input", i));
        }
        identityService.setAuthenticatedUserId(null);

        Task task = taskService.createTaskQuery().singleResult();
        assertEquals("Final task", task.getName());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery()
                    .superProcessInstanceId(processInstance.getId()).list();
            assertEquals(3, historicProcessInstances.size());
            for (HistoricProcessInstance historicProcessInstance : historicProcessInstances) {
                assertNotNull(historicProcessInstance.getStartUserId());
                assertNotNull(historicProcessInstance.getStartTime());
                assertNotNull(historicProcessInstance.getEndTime());
            }
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcess.bpmn20.xml", "org/flowable/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml" })
    public void testAuthenticatedStartUserInCallActivity() {
        final String authenticatedUser = "user1";
        identityService.setAuthenticatedUserId(authenticatedUser);
        final ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callSimpleSubProcess");

        TaskQuery taskQuery = taskService.createTaskQuery();
        Task taskBeforeSubProcess = taskQuery.singleResult();
        assertEquals("Task before subprocess", taskBeforeSubProcess.getName());
        // Completing the task continues the process which leads to calling the subprocess
        taskService.complete(taskBeforeSubProcess.getId());

        ProcessInstance subProcess = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();

        assertEquals(authenticatedUser, subProcess.getStartUserId());
        List<IdentityLink> subProcessIdentityLinks = runtimeService.getIdentityLinksForProcessInstance(subProcess.getId());
        assertEquals(1, subProcessIdentityLinks.size());
        assertEquals(IdentityLinkType.STARTER, subProcessIdentityLinks.get(0).getType());
        assertEquals(authenticatedUser, subProcessIdentityLinks.get(0).getUserId());
    }
    
    @Test
    @Deployment(resources = { 
            "org/flowable/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcess.bpmn20.xml", 
            "org/flowable/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml" 
            })
    public void testDeleteProcessInstance() {
        // Bring process instance to task in child process instance
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callSimpleSubProcess");
        Task taskBeforeSubProcess = taskService.createTaskQuery().singleResult();
        assertEquals("Task before subprocess", taskBeforeSubProcess.getName());
        taskService.complete(taskBeforeSubProcess.getId());
        Task taskInSubProcess = taskService.createTaskQuery().singleResult();
        assertEquals("Task in subprocess", taskInSubProcess.getName());
        
        // Delete child process instance: parent process instance should continue
        assertNotEquals(processInstance.getId(), taskInSubProcess.getProcessInstanceId());
        runtimeService.deleteProcessInstance(taskInSubProcess.getProcessInstanceId(), null);
        
        Task taskAfterSubProcess = taskService.createTaskQuery().singleResult();
        assertNotNull(taskAfterSubProcess);
        assertEquals("Task after subprocess", taskAfterSubProcess.getName());
        
        taskService.complete(taskAfterSubProcess.getId());
        assertEquals(0, runtimeService.createExecutionQuery().count());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcessWithFallback.bpmn20.xml"},
        tenantId = "flowable"
    )
    public void testCallSubProcessWithFallbackToDefaultTenant() {
        assertCallActivityToFallback();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcessWithFallbackWrongNonBoolean.bpmn20.xml",
        "org/flowable/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"},
        tenantId = "flowable"
    )
    public void testCallSubProcessWithFallbackToDefaultTenantWithWrongExpressionOnSameTenant() {
        assertProcessExecuted();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcessWithFallbackFalse.bpmn20.xml",
        "org/flowable/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"},
        tenantId = "flowable"
    )
    public void testCallSubProcessWithFallbackToDefaultTenantFalseInSameTenant() {
        assertProcessExecuted();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcessWithFallbackFalse.bpmn20.xml"},
        tenantId = "flowable"
    )
    public void testCallSubProcessWithFallbackToDefaultTenantFalse() {
        assertThrows(
            FlowableObjectNotFoundException.class,
            () -> assertCallActivityToFallback()
        );
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcessWithFallbackWrongNonBoolean.bpmn20.xml"},
        tenantId = "flowable"
    )
    public void testCallSubProcessWithFallbackToDefaultTenantNonBooleanValue() {
        assertThrows(
            FlowableException.class,
            () -> assertCallActivityToFallback(),
            "Unable to recognize fallbackToDefaultTenant value 1"
        );
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcessWithFallback.bpmn20.xml",
        "org/flowable/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"},
        tenantId = "defaultFlowable"
    )
    public void testCallSimpleSubProcessWithDefaultTenantFallback() {
        String originalDefaultTenantValue = processEngineConfiguration.getDefaultTenantValue();
        processEngineConfiguration.setDefaultTenantValue("defaultFlowable");
        try {
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                            .processDefinitionKey("callSimpleSubProcess")
                            .tenantId("someTenant")
                            .fallbackToDefaultTenant()
                            .start();
            
            assertEquals("someTenant", processInstance.getTenantId());
    
            // one task in the subprocess should be active after starting the process instance
            TaskQuery taskQuery = taskService.createTaskQuery().taskTenantId("someTenant");
            Task taskInSubProcess = taskQuery.singleResult();
            assertEquals("Task in subprocess", taskInSubProcess.getName());
            assertEquals("someTenant", taskInSubProcess.getTenantId());
    
            // Completing the task in the subprocess, finishes the subprocess
            taskService.complete(taskInSubProcess.getId());
            assertProcessEnded(processInstance.getId());
            
        } finally {
            processEngineConfiguration.setDefaultTenantValue(originalDefaultTenantValue);
        }
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcess.bpmn20.xml",
        "org/flowable/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"},
        tenantId = "defaultFlowable"
    )
    public void testCallSimpleSubProcessWithGlobalDefaultTenantFallback() {
        String originalDefaultTenantValue = processEngineConfiguration.getDefaultTenantValue();
        processEngineConfiguration.setDefaultTenantValue("defaultFlowable");
        processEngineConfiguration.setFallbackToDefaultTenant(true);
        try {
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                            .processDefinitionKey("callSimpleSubProcess")
                            .tenantId("someTenant")
                            .fallbackToDefaultTenant()
                            .start();
            
            assertEquals("someTenant", processInstance.getTenantId());
    
            // one task in the subprocess should be active after starting the process instance
            Task taskBeforeSubProcess = taskService.createTaskQuery().singleResult();
            assertEquals("Task before subprocess", taskBeforeSubProcess.getName());
            assertEquals("someTenant", taskBeforeSubProcess.getTenantId());
            taskService.complete(taskBeforeSubProcess.getId());
            
            Task taskInSubProcess = taskService.createTaskQuery().singleResult();
            assertEquals("Task in subprocess", taskInSubProcess.getName());
            assertEquals("someTenant", taskInSubProcess.getTenantId());
            
            // Delete child process instance: parent process instance should continue
            assertNotEquals(processInstance.getId(), taskInSubProcess.getProcessInstanceId());
            runtimeService.deleteProcessInstance(taskInSubProcess.getProcessInstanceId(), null);
            
            Task taskAfterSubProcess = taskService.createTaskQuery().singleResult();
            assertNotNull(taskAfterSubProcess);
            assertEquals("Task after subprocess", taskAfterSubProcess.getName());
            
            taskService.complete(taskAfterSubProcess.getId());
            
            assertProcessEnded(processInstance.getId());
            
        } finally {
            processEngineConfiguration.setDefaultTenantValue(originalDefaultTenantValue);
            processEngineConfiguration.setFallbackToDefaultTenant(false);
        }
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcessWithFallback.bpmn20.xml",
        "org/flowable/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"},
        tenantId = "defaultFlowable"
    )
    public void testCallSimpleSubProcessWithDefaultTenantFallbackAndEmptyDefaultTenant() {
        try {
            runtimeService.createProcessInstanceBuilder()
                            .processDefinitionKey("callSimpleSubProcess")
                            .tenantId("someTenant")
                            .fallbackToDefaultTenant()
                            .start();
            fail("Expected process definition not found");
            
        } catch (FlowableObjectNotFoundException e) {
            // expected exception
        }
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcess.bpmn20.xml",
        "org/flowable/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"},
        tenantId = "defaultFlowable"
    )
    public void testCallSimpleSubProcessWithGlobalDefaultTenantFallbackAndEmptyDefaultTenant() {
        processEngineConfiguration.setFallbackToDefaultTenant(true);
        try {
            runtimeService.createProcessInstanceBuilder()
                            .processDefinitionKey("callSimpleSubProcess")
                            .tenantId("someTenant")
                            .fallbackToDefaultTenant()
                            .start();
            fail("Expected process definition not found");
            
        } catch (FlowableObjectNotFoundException e) {
            // expected exception
        } finally {
            processEngineConfiguration.setFallbackToDefaultTenant(false);
        }
    }

    protected void assertCallActivityToFallback() {
        org.flowable.engine.repository.Deployment deployment = this.repositoryService.createDeployment().
            addClasspathResource("org/flowable/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml").
            tenantId(ProcessEngineConfiguration.NO_TENANT_ID).
            deploy();

        try {
            assertProcessExecuted();
        } finally {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    protected void assertProcessExecuted() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKeyAndTenantId("callSimpleSubProcess", "flowable");

        Task taskInSubProcess = taskService.createTaskQuery().singleResult();
        assertEquals("Task in subprocess", taskInSubProcess.getName());
        assertEquals("flowable", taskInSubProcess.getTenantId());

        // Completing the task in the subprocess, finishes the processes
        taskService.complete(taskInSubProcess.getId());
        assertProcessEnded(processInstance.getId());
    }

}
