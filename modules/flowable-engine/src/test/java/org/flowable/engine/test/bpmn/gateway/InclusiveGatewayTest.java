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
package org.flowable.engine.test.bpmn.gateway;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.impl.EventSubscriptionQueryImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.AbstractFlowableTestCase;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.EventSubscription;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 * @author Tom Van Buskirk
 * @author Tijs Rademakers
 * @author Saeid Mirzaei
 */
public class InclusiveGatewayTest extends PluggableFlowableTestCase {

    private static final String TASK1_NAME = "Task 1";
    private static final String TASK2_NAME = "Task 2";
    private static final String TASK3_NAME = "Task 3";

    private static final String BEAN_TASK1_NAME = "Basic service";
    private static final String BEAN_TASK2_NAME = "Standard service";
    private static final String BEAN_TASK3_NAME = "Gold Member service";

    @Test
    @Deployment
    public void testDivergingInclusiveGateway() {
        for (int i = 1; i <= 3; i++) {
            ProcessInstance pi = runtimeService.startProcessInstanceByKey("inclusiveGwDiverging", CollectionUtil.singletonMap("input", i));
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
            List<String> expectedNames = new ArrayList<>();
            if (i == 1) {
                expectedNames.add(TASK1_NAME);
            }
            if (i <= 2) {
                expectedNames.add(TASK2_NAME);
            }
            expectedNames.add(TASK3_NAME);
            assertEquals(4 - i, tasks.size());
            for (org.flowable.task.api.Task task : tasks) {
                expectedNames.remove(task.getName());
            }
            assertEquals(0, expectedNames.size());
            runtimeService.deleteProcessInstance(pi.getId(), "testing deletion");
        }
    }

    @Test
    @Deployment
    public void testMergingInclusiveGateway() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("inclusiveGwMerging", CollectionUtil.singletonMap("input", 2));
        assertEquals(1, taskService.createTaskQuery().count());

        runtimeService.deleteProcessInstance(pi.getId(), "testing deletion");
    }

    @Test
    @Deployment
    public void testPartialMergingInclusiveGateway() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("partialInclusiveGwMerging", CollectionUtil.singletonMap("input", 2));
        org.flowable.task.api.Task partialTask = taskService.createTaskQuery().singleResult();
        assertEquals("partialTask", partialTask.getTaskDefinitionKey());

        taskService.complete(partialTask.getId());

        org.flowable.task.api.Task fullTask = taskService.createTaskQuery().singleResult();
        assertEquals("theTask", fullTask.getTaskDefinitionKey());

        runtimeService.deleteProcessInstance(pi.getId(), "testing deletion");
    }

    @Test
    @Deployment
    public void testNoSequenceFlowSelected() {
        try {
            runtimeService.startProcessInstanceByKey("inclusiveGwNoSeqFlowSelected", CollectionUtil.singletonMap("input", 4));
            fail();
        } catch (FlowableException e) {
            // Exception expected
        }
    }

    /**
     * Test for ACT-1216: When merging a concurrent execution the parent is not activated correctly
     */
    @Test
    @Deployment
    public void testParentActivationOnNonJoiningEnd() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parentActivationOnNonJoiningEnd");

        List<Execution> executionsBefore = runtimeService.createExecutionQuery().list();
        assertEquals(3, executionsBefore.size());

        // start first round of tasks
        List<org.flowable.task.api.Task> firstTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, firstTasks.size());

        for (org.flowable.task.api.Task t : firstTasks) {
            taskService.complete(t.getId());
        }

        // start second round of tasks
        List<org.flowable.task.api.Task> secondTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, secondTasks.size());

        // complete one task
        org.flowable.task.api.Task task = secondTasks.get(0);
        taskService.complete(task.getId());

        List<Execution> executionsAfter = runtimeService.createExecutionQuery().list();
        assertEquals(2, executionsAfter.size());

        Execution execution = null;
        for (Execution e : executionsAfter) {
            if (e.getParentId() != null) {
                execution = e;
            }
        }

        // and should have one active activity
        List<String> activeActivityIds = runtimeService.getActiveActivityIds(execution.getId());
        assertEquals(1, activeActivityIds.size());

        // Completing last task should finish the process instance

        org.flowable.task.api.Task lastTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(lastTask.getId());

        assertEquals(0l, runtimeService.createProcessInstanceQuery().active().count());
    }

    /**
     * Test for bug ACT-10: whitespaces/newlines in expressions lead to exceptions
     */
    @Test
    @Deployment
    public void testWhitespaceInExpression() {
        // Starting a process instance will lead to an exception if whitespace
        // are
        // incorrectly handled
        runtimeService.startProcessInstanceByKey("inclusiveWhiteSpaceInExpression", CollectionUtil.singletonMap("input", 1));
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/gateway/InclusiveGatewayTest.testDivergingInclusiveGateway.bpmn20.xml" })
    public void testUnknownVariableInExpression() {
        // Instead of 'input' we're starting a process instance with the name
        // 'iinput' (ie. a typo)
        try {
            runtimeService.startProcessInstanceByKey("inclusiveGwDiverging", CollectionUtil.singletonMap("iinput", 1));
            fail();
        } catch (FlowableException e) {
            assertTextPresent("Unknown property used in expression", e.getMessage());
        }
    }

    @Test
    @Deployment
    public void testDecideBasedOnBeanProperty() {
        runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnBeanProperty", CollectionUtil.singletonMap("order", new InclusiveGatewayTestOrder(150)));
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(2, tasks.size());
        Map<String, String> expectedNames = new HashMap<>();
        expectedNames.put(BEAN_TASK2_NAME, BEAN_TASK2_NAME);
        expectedNames.put(BEAN_TASK3_NAME, BEAN_TASK3_NAME);
        for (org.flowable.task.api.Task task : tasks) {
            expectedNames.remove(task.getName());
        }
        assertEquals(0, expectedNames.size());
    }

    @Test
    @Deployment
    public void testDecideBasedOnListOrArrayOfBeans() {
        List<InclusiveGatewayTestOrder> orders = new ArrayList<>();
        orders.add(new InclusiveGatewayTestOrder(50));
        orders.add(new InclusiveGatewayTestOrder(300));
        orders.add(new InclusiveGatewayTestOrder(175));

        try {
            runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnListOrArrayOfBeans", CollectionUtil.singletonMap("orders", orders));
            fail();
        } catch (FlowableException e) {
            // expect an exception to be thrown here as there is
        }

        orders.set(1, new InclusiveGatewayTestOrder(175));
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnListOrArrayOfBeans", CollectionUtil.singletonMap("orders", orders));
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertNotNull(task);
        assertEquals(BEAN_TASK3_NAME, task.getName());

        orders.set(1, new InclusiveGatewayTestOrder(125));
        pi = runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnListOrArrayOfBeans", CollectionUtil.singletonMap("orders", orders));
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertNotNull(tasks);
        assertEquals(2, tasks.size());
        List<String> expectedNames = new ArrayList<>();
        expectedNames.add(BEAN_TASK2_NAME);
        expectedNames.add(BEAN_TASK3_NAME);
        for (org.flowable.task.api.Task t : tasks) {
            expectedNames.remove(t.getName());
        }
        assertEquals(0, expectedNames.size());

        // Arrays are usable in exactly the same way
        InclusiveGatewayTestOrder[] orderArray = orders.toArray(new InclusiveGatewayTestOrder[orders.size()]);
        orderArray[1].setPrice(10);
        pi = runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnListOrArrayOfBeans", CollectionUtil.singletonMap("orders", orderArray));
        tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertNotNull(tasks);
        assertEquals(3, tasks.size());
        expectedNames.clear();
        expectedNames.add(BEAN_TASK1_NAME);
        expectedNames.add(BEAN_TASK2_NAME);
        expectedNames.add(BEAN_TASK3_NAME);
        for (org.flowable.task.api.Task t : tasks) {
            expectedNames.remove(t.getName());
        }
        assertEquals(0, expectedNames.size());
    }

    @Test
    @Deployment
    public void testDecideBasedOnBeanMethod() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnBeanMethod", CollectionUtil.singletonMap("order", new InclusiveGatewayTestOrder(200)));
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertNotNull(task);
        assertEquals(BEAN_TASK3_NAME, task.getName());

        pi = runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnBeanMethod", CollectionUtil.singletonMap("order", new InclusiveGatewayTestOrder(125)));
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertEquals(2, tasks.size());
        List<String> expectedNames = new ArrayList<>();
        expectedNames.add(BEAN_TASK2_NAME);
        expectedNames.add(BEAN_TASK3_NAME);
        for (org.flowable.task.api.Task t : tasks) {
            expectedNames.remove(t.getName());
        }
        assertEquals(0, expectedNames.size());

        try {
            runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnBeanMethod", CollectionUtil.singletonMap("order", new InclusiveGatewayTestOrder(300)));
            fail();
        } catch (FlowableException e) {
            // Should get an exception indicating that no path could be taken
        }

    }

    @Test
    @Deployment
    public void testInvalidMethodExpression() {
        try {
            runtimeService.startProcessInstanceByKey("inclusiveInvalidMethodExpression", CollectionUtil.singletonMap("order", new InclusiveGatewayTestOrder(50)));
            fail();
        } catch (FlowableException e) {
            assertTextPresent("Unknown method used in expression", e.getMessage());
        }
    }

    @Test
    @Deployment
    public void testDefaultSequenceFlow() {
        // Input == 1 -> default is not selected, other 2 tasks are selected
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("inclusiveGwDefaultSequenceFlow", CollectionUtil.singletonMap("input", 1));
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertEquals(2, tasks.size());
        Map<String, String> expectedNames = new HashMap<>();
        expectedNames.put("Input is one", "Input is one");
        expectedNames.put("Input is three or one", "Input is three or one");
        for (org.flowable.task.api.Task t : tasks) {
            expectedNames.remove(t.getName());
        }
        assertEquals(0, expectedNames.size());
        runtimeService.deleteProcessInstance(pi.getId(), null);

        // Input == 3 -> default is not selected, "one or three" is selected
        pi = runtimeService.startProcessInstanceByKey("inclusiveGwDefaultSequenceFlow", CollectionUtil.singletonMap("input", 3));
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertEquals("Input is three or one", task.getName());

        // Default input
        pi = runtimeService.startProcessInstanceByKey("inclusiveGwDefaultSequenceFlow", CollectionUtil.singletonMap("input", 5));
        task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertEquals("Default input", task.getName());
    }

    @Test
    @Deployment
    public void testNoIdOnSequenceFlow() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("inclusiveNoIdOnSequenceFlow", CollectionUtil.singletonMap("input", 3));
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertEquals("Input is more than one", task.getName());

        // Both should be enabled on 1
        pi = runtimeService.startProcessInstanceByKey("inclusiveNoIdOnSequenceFlow", CollectionUtil.singletonMap("input", 1));
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertEquals(2, tasks.size());
        Map<String, String> expectedNames = new HashMap<>();
        expectedNames.put("Input is one", "Input is one");
        expectedNames.put("Input is more than one", "Input is more than one");
        for (org.flowable.task.api.Task t : tasks) {
            expectedNames.remove(t.getName());
        }
        assertEquals(0, expectedNames.size());
    }

    /**
     * This test the isReachable() check that is done to check if upstream tokens can reach the inclusive gateway.
     *
     * In case of loops, special care needs to be taken in the algorithm, or else stackoverflows will happen very quickly.
     */
    @Test
    @Deployment
    public void testLoop() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("inclusiveTestLoop", CollectionUtil.singletonMap("counter", 1));

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("task C", task.getName());

        taskService.complete(task.getId());
        assertEquals(0, taskService.createTaskQuery().count());

        assertEquals("Found executions: " + runtimeService.createExecutionQuery().list(), 0, runtimeService.createExecutionQuery().count());
        assertProcessEnded(pi.getId());
    }

    @Test
    @Deployment
    public void testJoinAfterSubprocesses() {
        // Test case to test act-1204
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("a", 1);
        variableMap.put("b", 1);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("InclusiveGateway", variableMap);
        assertNotNull(processInstance.getId());

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, taskService.createTaskQuery().count());

        taskService.complete(tasks.get(0).getId());
        assertEquals(1, taskService.createTaskQuery().count());

        taskService.complete(tasks.get(1).getId());

        org.flowable.task.api.Task task = taskService.createTaskQuery().taskAssignee("c").singleResult();
        assertNotNull(task);
        taskService.complete(task.getId());

        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(processInstance);

        variableMap = new HashMap<>();
        variableMap.put("a", 1);
        variableMap.put("b", 2);
        processInstance = runtimeService.startProcessInstanceByKey("InclusiveGateway", variableMap);
        assertNotNull(processInstance.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, taskService.createTaskQuery().count());

        task = tasks.get(0);
        assertEquals("a", task.getAssignee());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().taskAssignee("c").singleResult();
        assertNotNull(task);
        taskService.complete(task.getId());

        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(processInstance);

        variableMap = new HashMap<>();
        variableMap.put("a", 2);
        variableMap.put("b", 2);
        try {
            runtimeService.startProcessInstanceByKey("InclusiveGateway", variableMap);
            fail();
        } catch (FlowableException e) {
            assertTrue(e.getMessage().contains("No outgoing sequence flow"));
        }
    }

    @Test
    @Deployment
    public void testJoinAfterParallelGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("InclusiveGateway");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);
        assertEquals("Task1", task.getName());

        taskService.complete(task.getId());

        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .activityId("receiveTask1")
                .singleResult();

        assertNotNull(execution);
        runtimeService.trigger(execution.getId());

        execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .activityId("receiveTask1")
                .singleResult();

        assertNotNull(execution);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/gateway/InclusiveGatewayTest.testJoinAfterCall.bpmn20.xml",
            "org/flowable/engine/test/bpmn/gateway/InclusiveGatewayTest.testJoinAfterCallSubProcess.bpmn20.xml" })
    public void testJoinAfterCall() {
        // Test case to test act-1026
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("InclusiveGatewayAfterCall");
        assertNotNull(processInstance.getId());
        assertEquals(3, taskService.createTaskQuery().count());

        // now complete task A and check number of remaining tasks.
        // inclusive gateway should wait for the "Task B" and "Task C"
        org.flowable.task.api.Task taskA = taskService.createTaskQuery().taskName("Task A").singleResult();
        assertNotNull(taskA);
        taskService.complete(taskA.getId());
        assertEquals(2, taskService.createTaskQuery().count());

        // now complete task B and check number of remaining tasks
        // inclusive gateway should wait for "Task C"
        org.flowable.task.api.Task taskB = taskService.createTaskQuery().taskName("Task B").singleResult();
        assertNotNull(taskB);
        taskService.complete(taskB.getId());
        assertEquals(1, taskService.createTaskQuery().count());

        // now complete task C. Gateway activates and "Task C" remains
        org.flowable.task.api.Task taskC = taskService.createTaskQuery().taskName("Task C").singleResult();
        assertNotNull(taskC);
        taskService.complete(taskC.getId());
        assertEquals(1, taskService.createTaskQuery().count());

        // check that remaining task is in fact task D
        org.flowable.task.api.Task taskD = taskService.createTaskQuery().taskName("Task D").singleResult();
        assertNotNull(taskD);
        assertEquals("Task D", taskD.getName());
        taskService.complete(taskD.getId());

        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(processInstance);
    }

    @Test
    @Deployment
    public void testAsyncBehavior() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("async");
        waitForJobExecutorToProcessAllJobs(7000L, 250);
        assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count());
    }

    @Test
    @Deployment
    public void testDirectSequenceFlow() {
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("input", 1);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("inclusiveGwDirectSequenceFlow", varMap);
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertNotNull(task);
        assertEquals("theTask1", task.getTaskDefinitionKey());
        taskService.complete(task.getId());
        assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count());

        varMap = new HashMap<>();
        varMap.put("input", 3);
        processInstance = runtimeService.startProcessInstanceByKey("inclusiveGwDirectSequenceFlow", varMap);
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(2, tasks.size());
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());
        assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count());

        varMap = new HashMap<>();
        varMap.put("input", 0);
        processInstance = runtimeService.startProcessInstanceByKey("inclusiveGwDirectSequenceFlow", varMap);
        assertTrue(processInstance.isEnded());
    }

    @Test
    @Deployment
    public void testSkipExpression() {
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", true);
        varMap.put("input", 10);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("inclusiveGwSkipExpression", varMap);
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertNotNull(task);
        assertEquals("theTask1", task.getTaskDefinitionKey());
        taskService.complete(task.getId());
        assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count());

        varMap = new HashMap<>();
        varMap.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", true);
        varMap.put("input", 30);
        processInstance = runtimeService.startProcessInstanceByKey("inclusiveGwSkipExpression", varMap);
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(2, tasks.size());
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());
        assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count());

        varMap = new HashMap<>();
        varMap.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", true);
        varMap.put("input", 3);
        processInstance = runtimeService.startProcessInstanceByKey("inclusiveGwSkipExpression", varMap);
        assertTrue(processInstance.isEnded());
    }

    @Test
    @Deployment
    public void testMultipleProcessInstancesMergedBug() {

        // Start first process instance, continue A. Process instance should be in C
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("testMultipleProcessInstancesMergedBug");
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance1.getId()).taskName("A").singleResult().getId());
        org.flowable.task.api.Task taskCInPi1 = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        assertNotNull(taskCInPi1);

        // Start second process instance, continue A. Process instance should be in B
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("testMultipleProcessInstancesMergedBug", CollectionUtil.singletonMap("var", "goToB"));
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance2.getId()).taskName("A").singleResult().getId());
        org.flowable.task.api.Task taskBInPi2 = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        assertNotNull(taskBInPi2);

        // Verify there is an inactive execution in the inclusive gateway before the task complete of process instance 1
        // (cannot combine activityId and inactive together, hence the workaround)
        assertEquals(2, getInactiveExecutionsInActivityId("inclusiveGw").size());

        // Completing C of PI 1 should not trigger C
        taskService.complete(taskCInPi1.getId());

        // Verify structure after complete.
        // Before bugfix: in BOTH process instances the inactive execution was removed (result was 0)
        assertEquals(1, getInactiveExecutionsInActivityId("inclusiveGw").size());

        assertEquals(1L, taskService.createTaskQuery().taskName("After Merge").count());

        // Finish both processes

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        while (tasks.size() > 0) {
            for (org.flowable.task.api.Task task : tasks) {
                taskService.complete(task.getId());
            }
            tasks = taskService.createTaskQuery().list();
        }
        assertEquals(0L, runtimeService.createProcessInstanceQuery().count());

    }

    // See https://github.com/flowable/flowable-engine/issues/582
    @Test
    @Deployment
    public void testInclusiveGatewayInEventSubProcess() {

        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery()
                .processDefinitionKey("b92d819d-481f-4001-834e-cbdfa6ee0fad")
                .singleResult();

        //make sure both conditions are true for the sequence flows of the inclusive gateway
        final ProcessInstance instance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionId(processDefinition.getId())
                .variable("test", true)
                .variable("test2", true)
                .start();

        List<Task> tasks = taskService
                .createTaskQuery()
                .processDefinitionId(instance.getProcessDefinitionId())
                .list();

        assertEquals(1, tasks.size());

        String executionId = processEngine.getManagementService().executeCommand(new Command<String>() {
            @Override
            public String execute(CommandContext commandContext) {
                EventSubscriptionQueryImpl q = new EventSubscriptionQueryImpl(commandContext);
                q.processInstanceId(instance.getProcessInstanceId());

                List<EventSubscription> subs = CommandContextUtil
                        .getEventSubscriptionEntityManager()
                        .findEventSubscriptionsByQueryCriteria(q);

                assertEquals(1, subs.size());
                EventSubscription sub = subs.get(0);
                assertEquals("test", sub.getEventName());

                return sub.getExecutionId();
            }
        });

        //send the message, after this we are inside the event subprocess
        runtimeService.messageEventReceived("test", executionId);
        tasks = taskService.createTaskQuery()
                .processDefinitionId(instance.getProcessDefinitionId())
                .list();

        //since it is non interupting, we now expect 3 tasks to be present
        assertEquals(3, tasks.size());

    }

    protected List<Execution> getInactiveExecutionsInActivityId(String activityId) {
        List<Execution> result = new ArrayList<>();
        List<Execution> executions = runtimeService.createExecutionQuery().list();
        Iterator<Execution> iterator = executions.iterator();
        while (iterator.hasNext()) {
            Execution execution = iterator.next();
            if (execution.getActivityId() != null
                    && execution.getActivityId().equals(activityId)
                    && !((ExecutionEntity) execution).isActive()) {
                result.add(execution);
            }
        }
        return result;
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/gateway/InclusiveGatewayTest.insideMultiInstanceParallelSubProcess.bpmn20.xml" })
    public void testInclusiveGatewayInclusiveGatewayInsideParallelMultiInstanceSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("inclusiveGatewayInsideParallelMultiInstanceSubProcess");

        List<Execution> childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //1x MultiInstance root, 3x parallel MultiInstance and 9x UserTasks executions (3 task executions per parallel multiInstance subProcess)
        assertEquals(13, childExecutions.size());
        Map<String, List<Execution>> classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertNotNull(classifiedExecutions.get("multiInstanceSubProcess"));
        assertEquals(4, classifiedExecutions.get("multiInstanceSubProcess").size());
        assertNotNull(classifiedExecutions.get("taskInclusive1"));
        assertEquals(3, classifiedExecutions.get("taskInclusive1").size());
        assertNotNull(classifiedExecutions.get("taskInclusive2"));
        assertEquals(3, classifiedExecutions.get("taskInclusive2").size());
        assertNotNull(classifiedExecutions.get("taskInclusive3"));
        assertEquals(3, classifiedExecutions.get("taskInclusive3").size());

        //9x UserTasks
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<Task>> classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(3, classifiedTasks.size());
        assertTrue(classifiedTasks.containsKey("taskInclusive1"));
        assertEquals(3, classifiedTasks.get("taskInclusive1").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive2"));
        assertEquals(3, classifiedTasks.get("taskInclusive2").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive3"));
        assertEquals(3, classifiedTasks.get("taskInclusive3").size());

        //Finish a couple of tasks
        taskService.complete(classifiedTasks.get("taskInclusive2").get(0).getId());
        taskService.complete(classifiedTasks.get("taskInclusive3").get(1).getId());

        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(13, childExecutions.size());
        classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertNotNull(classifiedExecutions.get("multiInstanceSubProcess"));
        assertEquals(4, classifiedExecutions.get("multiInstanceSubProcess").size());
        assertNotNull(classifiedExecutions.get("taskInclusive1"));
        assertEquals(3, classifiedExecutions.get("taskInclusive1").size());
        assertNotNull(classifiedExecutions.get("taskInclusive2"));
        assertEquals(2, classifiedExecutions.get("taskInclusive2").size());
        assertNotNull(classifiedExecutions.get("taskInclusive3"));
        assertEquals(2, classifiedExecutions.get("taskInclusive3").size());
        assertNotNull(classifiedExecutions.get("inclusiveJoin"));
        assertEquals(2, classifiedExecutions.get("inclusiveJoin").size());

        //7x pending User Tasks
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(3, classifiedTasks.size());
        assertTrue(classifiedTasks.containsKey("taskInclusive1"));
        assertEquals(3, classifiedTasks.get("taskInclusive1").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive2"));
        assertEquals(2, classifiedTasks.get("taskInclusive2").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive3"));
        assertEquals(2, classifiedTasks.get("taskInclusive3").size());

        //Finish the rest of the tasks
        classifiedTasks.values().stream().flatMap(List::stream).forEach(this::completeTask);

        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(7, childExecutions.size());
        classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertNotNull(classifiedExecutions.get("multiInstanceSubProcess"));
        assertEquals(4, classifiedExecutions.get("multiInstanceSubProcess").size());
        assertNotNull(classifiedExecutions.get("postForkTask"));
        assertEquals(3, classifiedExecutions.get("postForkTask").size());

        //3x pending User Tasks
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());
        tasks.forEach(task-> assertEquals("postForkTask", task.getTaskDefinitionKey()));

        //Finish the remaining tasks in the SubProcess
        tasks.forEach(this::completeTask);

        //MultiInstance subProcess ended, only the last task of the process remains
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, childExecutions.size());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());

        //Finish the process
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/gateway/InclusiveGatewayTest.insideMultiInstanceSequentialSubProcess.bpmn20.xml" })
    public void testInclusiveGatewayInclusiveGatewayInsideSequentialMultiInstanceSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("inclusiveGatewayInsideSequentialMultiInstanceSubProcess");

        List<Execution> childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //1x MultiInstance root, 1x Sequential MultiInstance and 3x UserTasks executions
        assertEquals(5, childExecutions.size());
        Map<String, List<Execution>> classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertNotNull(classifiedExecutions.get("multiInstanceSubProcess"));
        assertEquals(2, classifiedExecutions.get("multiInstanceSubProcess").size());
        assertNotNull(classifiedExecutions.get("taskInclusive1"));
        assertEquals(1, classifiedExecutions.get("taskInclusive1").size());
        assertNotNull(classifiedExecutions.get("taskInclusive2"));
        assertEquals(1, classifiedExecutions.get("taskInclusive2").size());
        assertNotNull(classifiedExecutions.get("taskInclusive3"));
        assertEquals(1, classifiedExecutions.get("taskInclusive3").size());

        //3x UserTasks
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<Task>> classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(3, classifiedTasks.size());
        assertTrue(classifiedTasks.containsKey("taskInclusive1"));
        assertEquals(1, classifiedTasks.get("taskInclusive1").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive2"));
        assertEquals(1, classifiedTasks.get("taskInclusive2").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive3"));
        assertEquals(1, classifiedTasks.get("taskInclusive3").size());

        //Finish one of the activities
        taskService.complete(classifiedTasks.get("taskInclusive3").get(0).getId());

        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(5, childExecutions.size());
        classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertNotNull(classifiedExecutions.get("multiInstanceSubProcess"));
        assertEquals(2, classifiedExecutions.get("multiInstanceSubProcess").size());
        assertNotNull(classifiedExecutions.get("taskInclusive1"));
        assertEquals(1, classifiedExecutions.get("taskInclusive1").size());
        assertNotNull(classifiedExecutions.get("taskInclusive2"));
        assertEquals(1, classifiedExecutions.get("taskInclusive2").size());
        assertNull(classifiedExecutions.get("taskInclusive3"));
        assertNotNull(classifiedExecutions.get("inclusiveJoin"));
        assertEquals(1, classifiedExecutions.get("inclusiveJoin").size());

        //2x pending User Tasks
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(2, classifiedTasks.size());
        assertTrue(classifiedTasks.containsKey("taskInclusive1"));
        assertTrue(classifiedTasks.containsKey("taskInclusive2"));
        assertFalse(classifiedTasks.containsKey("taskInclusive3"));

        //Finish the rest of the tasks
        Stream.concat(classifiedTasks.get("taskInclusive1").stream(), classifiedTasks.get("taskInclusive2").stream())
            .forEach(this::completeTask);

        //1x MultiInstance root, 1x Sequential MultiInstance, 1x User Task after the gateway join
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(3, childExecutions.size());
        classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertNotNull(classifiedExecutions.get("multiInstanceSubProcess"));
        assertEquals(2, classifiedExecutions.get("multiInstanceSubProcess").size());
        assertNotNull(classifiedExecutions.get("postForkTask"));
        assertEquals(1, classifiedExecutions.get("postForkTask").size());

        //Last task of this multiInstance subProcess instance
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("postForkTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        //The next sequence should start
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //1x MultiInstance root, 1x Sequential MultiInstance and 3x UserTasks executions
        assertEquals(5, childExecutions.size());
        classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertNotNull(classifiedExecutions.get("multiInstanceSubProcess"));
        assertEquals(2, classifiedExecutions.get("multiInstanceSubProcess").size());
        assertNotNull(classifiedExecutions.get("taskInclusive1"));
        assertEquals(1, classifiedExecutions.get("taskInclusive1").size());
        assertNotNull(classifiedExecutions.get("taskInclusive2"));
        assertEquals(1, classifiedExecutions.get("taskInclusive2").size());
        assertNotNull(classifiedExecutions.get("taskInclusive3"));
        assertEquals(1, classifiedExecutions.get("taskInclusive3").size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(3, classifiedTasks.size());
        assertTrue(classifiedTasks.containsKey("taskInclusive1"));
        assertEquals(1, classifiedTasks.get("taskInclusive1").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive2"));
        assertEquals(1, classifiedTasks.get("taskInclusive2").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive3"));
        assertEquals(1, classifiedTasks.get("taskInclusive3").size());

        //Finish the inclusive gateway tasks
        tasks.forEach(this::completeTask);

        //last task of the sequence
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("postForkTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        //Last Sequence
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(5, childExecutions.size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());
        tasks.forEach(this::completeTask);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("postForkTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        //last task of the process, after the multiInstance subProcess
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, childExecutions.size());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());

        //Finish the process
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }


    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/gateway/InclusiveGatewayTest.inSubProcessNestedInMultiInstanceParallelSubProcess.bpmn20.xml" })
    public void testInSubProcessNestedInMultiInstanceParallelSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("inclusiveGatewayInsideSubProcessNestedInMultiInstanceParallelSubProcess");

        List<Execution> childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //1x MultiInstance root, 3x parallel MultiInstance, 3x NestedSubProcess and 9x UserTasks executions
        assertEquals(16, childExecutions.size());
        Map<String, List<Execution>> classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertNotNull(classifiedExecutions.get("multiInstanceSubProcess"));
        assertEquals(4, classifiedExecutions.get("multiInstanceSubProcess").size());
        assertNotNull(classifiedExecutions.get("nestedSubProcess"));
        assertEquals(3, classifiedExecutions.get("nestedSubProcess").size());
        assertNotNull(classifiedExecutions.get("taskInclusive1"));
        assertEquals(3, classifiedExecutions.get("taskInclusive1").size());
        assertNotNull(classifiedExecutions.get("taskInclusive2"));
        assertEquals(3, classifiedExecutions.get("taskInclusive2").size());
        assertNotNull(classifiedExecutions.get("taskInclusive3"));
        assertEquals(3, classifiedExecutions.get("taskInclusive3").size());

        //9x UserTasks
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<Task>> classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(3, classifiedTasks.size());
        assertTrue(classifiedTasks.containsKey("taskInclusive1"));
        assertEquals(3, classifiedTasks.get("taskInclusive1").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive2"));
        assertEquals(3, classifiedTasks.get("taskInclusive2").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive3"));
        assertEquals(3, classifiedTasks.get("taskInclusive3").size());

        //Finish a couple of Tasks
        taskService.complete(classifiedTasks.get("taskInclusive1").get(1).getId());
        taskService.complete(classifiedTasks.get("taskInclusive3").get(2).getId());

        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //1x MultiInstance root, 3x parallel MultiInstance, 3x NestedSubProcess and 7x UserTasks executions, 2x Gw Join executions
        assertEquals(16, childExecutions.size());
        classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertNotNull(classifiedExecutions.get("multiInstanceSubProcess"));
        assertEquals(4, classifiedExecutions.get("multiInstanceSubProcess").size());
        assertNotNull(classifiedExecutions.get("nestedSubProcess"));
        assertEquals(3, classifiedExecutions.get("nestedSubProcess").size());
        assertNotNull(classifiedExecutions.get("taskInclusive1"));
        assertEquals(2, classifiedExecutions.get("taskInclusive1").size());
        assertNotNull(classifiedExecutions.get("taskInclusive2"));
        assertEquals(3, classifiedExecutions.get("taskInclusive2").size());
        assertNotNull(classifiedExecutions.get("taskInclusive3"));
        assertEquals(2, classifiedExecutions.get("taskInclusive3").size());
        assertNotNull(classifiedExecutions.get("inclusiveJoin"));
        assertEquals(2, classifiedExecutions.get("inclusiveJoin").size());

        //7x UserTasks
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(3, classifiedTasks.size());
        assertTrue(classifiedTasks.containsKey("taskInclusive1"));
        assertEquals(2, classifiedTasks.get("taskInclusive1").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive2"));
        assertEquals(3, classifiedTasks.get("taskInclusive2").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive3"));
        assertEquals(2, classifiedTasks.get("taskInclusive3").size());

        //Finish one "multiInstance subProcess"
        Stream<Execution> tempStream = Stream.concat(classifiedExecutions.get("taskInclusive1").stream(), classifiedExecutions.get("taskInclusive2").stream());
        Map<String, List<Execution>> taskExecutionsByParent = Stream.concat(tempStream, classifiedExecutions.get("taskInclusive3").stream())
            .collect(Collectors.groupingBy(Execution::getParentId));
        //Get the execution Ids of one with 3 task executions
        boolean doneFlag = false;
        for (List<Execution> executions : taskExecutionsByParent.values()) {
            if (executions.size() == 3) {
                List<String> executionIds = executions.stream().map(Execution::getId).collect(Collectors.toList());
                tasks.stream().filter(t -> executionIds.contains(t.getExecutionId())).forEach(this::completeTask);
                doneFlag = true;
                break;
            }
        }
        if (!doneFlag) {
            fail("Invalid test state, there should be subProcess instance with embedded gateway with all parallel tasks pending for execution");
        }

        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //1x MultiInstance root, 3x parallel MultiInstance, 3x NestedSubProcess, 4x UserTasks executions, 2x Gw Join executions, 1 postFork task Execution
        assertEquals(14, childExecutions.size());
        classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertNotNull(classifiedExecutions.get("multiInstanceSubProcess"));
        assertEquals(4, classifiedExecutions.get("multiInstanceSubProcess").size());
        assertNotNull(classifiedExecutions.get("nestedSubProcess"));
        assertEquals(3, classifiedExecutions.get("nestedSubProcess").size());
        assertNotNull(classifiedExecutions.get("taskInclusive1"));
        assertEquals(1, classifiedExecutions.get("taskInclusive1").size());
        assertNotNull(classifiedExecutions.get("taskInclusive2"));
        assertEquals(2, classifiedExecutions.get("taskInclusive2").size());
        assertNotNull(classifiedExecutions.get("taskInclusive3"));
        assertEquals(1, classifiedExecutions.get("taskInclusive3").size());
        assertNotNull(classifiedExecutions.get("inclusiveJoin"));
        assertEquals(2, classifiedExecutions.get("inclusiveJoin").size());
        assertNotNull(classifiedExecutions.get("postForkTask"));
        assertEquals(1, classifiedExecutions.get("postForkTask").size());

        //5x UserTasks
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(4, classifiedTasks.size());
        assertTrue(classifiedTasks.containsKey("taskInclusive1"));
        assertEquals(1, classifiedTasks.get("taskInclusive1").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive2"));
        assertEquals(2, classifiedTasks.get("taskInclusive2").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive3"));
        assertEquals(1, classifiedTasks.get("taskInclusive3").size());
        assertTrue(classifiedTasks.containsKey("postForkTask"));
        assertEquals(1, classifiedTasks.get("postForkTask").size());

        //Finish all gateWayTasks
        tasks.stream().filter(t-> !t.getTaskDefinitionKey().equals("postForkTask")).forEach(this::completeTask);

        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //1x MultiInstance root, 3x parallel MultiInstance, 3x NestedSubProcess, 4x postFork task Execution
        assertEquals(10, childExecutions.size());
        classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertNotNull(classifiedExecutions.get("multiInstanceSubProcess"));
        assertEquals(4, classifiedExecutions.get("multiInstanceSubProcess").size());
        assertNotNull(classifiedExecutions.get("nestedSubProcess"));
        assertEquals(3, classifiedExecutions.get("nestedSubProcess").size());
        assertNotNull(classifiedExecutions.get("postForkTask"));
        assertEquals(3, classifiedExecutions.get("postForkTask").size());

        //3x UserTasks
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(1, classifiedTasks.size());
        assertTrue(classifiedTasks.containsKey("postForkTask"));
        assertEquals(3, classifiedTasks.get("postForkTask").size());

        //Finish the nested subprocess tasks
        tasks.forEach(this::completeTask);

        //MultiInstance subProcesses finish as the nested subProcesses end
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //1x User task execution
        assertEquals(1, childExecutions.size());
        assertEquals("lastTask", childExecutions.get(0).getActivityId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, tasks.size());
        assertEquals("lastTask", tasks.get(0).getTaskDefinitionKey());

        //Finish the process
        tasks.forEach(this::completeTask);

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = {"org/flowable/engine/test/bpmn/gateway/InclusiveGatewayTest.inCalledActivityNestedInMultiInstanceParallelSubProcess.bpmn20.xml",
    "org/flowable/engine/test/bpmn/gateway/InclusiveGatewayTest.simpleParallelFlow.bpmn20.xml"})
    public void testInCalledActivityNestedInMultiInstanceSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("inclusiveGatewayInsideCalledActivityNestedInMultiInstanceParallelSubProcess");

        //1x Process Root, 3x Call activity roots
        List<Execution> processExecutionRoots = runtimeService.createExecutionQuery().onlyProcessInstanceExecutions().list();
        assertEquals(4, processExecutionRoots.size());
        Map<String, List<Execution>> classifiedRoots = processExecutionRoots.stream()
            .collect(Collectors.toMap(e -> e.getSuperExecutionId() != null ? "callActivity" : null, Collections::singletonList, AbstractFlowableTestCase::mergeLists));
        assertEquals(1, classifiedRoots.get(null).size());
        assertEquals(3, classifiedRoots.get("callActivity").size());

        //1x MultiInstance root, 3x parallel MultiInstance, 3x CalledActivitySubProcesses and 9x UserTasks executions
        List<Execution> childExecutions = processExecutionRoots.stream()
            .flatMap(rootProcess -> runtimeService.createExecutionQuery().processInstanceId(rootProcess.getId()).onlyChildExecutions().list().stream())
            .collect(Collectors.toList());
        assertEquals(16, childExecutions.size());
        Map<String, List<Execution>> classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertNotNull(classifiedExecutions.get("multiInstanceSubProcess"));
        assertEquals(4, classifiedExecutions.get("multiInstanceSubProcess").size());
        assertNotNull(classifiedExecutions.get("callActivity"));
        assertEquals(3, classifiedExecutions.get("callActivity").size());
        assertNotNull(classifiedExecutions.get("taskInclusive1"));
        assertEquals(3, classifiedExecutions.get("taskInclusive1").size());
        assertNotNull(classifiedExecutions.get("taskInclusive2"));
        assertEquals(3, classifiedExecutions.get("taskInclusive2").size());
        assertNotNull(classifiedExecutions.get("taskInclusive3"));
        assertEquals(3, classifiedExecutions.get("taskInclusive3").size());

        //9x UserTasks
        List<Task> tasks = taskService.createTaskQuery().list();
        Map<String, List<Task>> classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(3, classifiedTasks.size());
        assertTrue(classifiedTasks.containsKey("taskInclusive1"));
        assertEquals(3, classifiedTasks.get("taskInclusive1").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive2"));
        assertEquals(3, classifiedTasks.get("taskInclusive2").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive3"));
        assertEquals(3, classifiedTasks.get("taskInclusive3").size());

        //Finish a couple of Tasks
        taskService.complete(classifiedTasks.get("taskInclusive1").get(1).getId());
        taskService.complete(classifiedTasks.get("taskInclusive3").get(2).getId());



        childExecutions = processExecutionRoots.stream()
            .flatMap(rootProcess -> runtimeService.createExecutionQuery().processInstanceId(rootProcess.getId()).onlyChildExecutions().list().stream())
            .collect(Collectors.toList());
        //1x MultiInstance root, 3x parallel MultiInstance, 3x CalledActivitySubProcesses and 7x UserTasks executions
        assertEquals(14, childExecutions.size());
        classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertNotNull(classifiedExecutions.get("multiInstanceSubProcess"));
        assertEquals(4, classifiedExecutions.get("multiInstanceSubProcess").size());
        assertNotNull(classifiedExecutions.get("callActivity"));
        assertEquals(3, classifiedExecutions.get("callActivity").size());
        assertNotNull(classifiedExecutions.get("taskInclusive1"));
        assertEquals(2, classifiedExecutions.get("taskInclusive1").size());
        assertNotNull(classifiedExecutions.get("taskInclusive2"));
        assertEquals(3, classifiedExecutions.get("taskInclusive2").size());
        assertNotNull(classifiedExecutions.get("taskInclusive3"));
        assertEquals(2, classifiedExecutions.get("taskInclusive3").size());

        //7x UserTasks
        tasks = taskService.createTaskQuery().list();
        classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(3, classifiedTasks.size());
        assertTrue(classifiedTasks.containsKey("taskInclusive1"));
        assertEquals(2, classifiedTasks.get("taskInclusive1").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive2"));
        assertEquals(3, classifiedTasks.get("taskInclusive2").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive3"));
        assertEquals(2, classifiedTasks.get("taskInclusive3").size());

        //Finish one "multiInstance subProcess"
        Stream<Execution> tempStream = Stream.concat(classifiedExecutions.get("taskInclusive1").stream(), classifiedExecutions.get("taskInclusive2").stream());
        Map<String, List<Execution>> taskExecutionsByParent = Stream.concat(tempStream, classifiedExecutions.get("taskInclusive3").stream())
            .collect(Collectors.groupingBy(Execution::getParentId));
        //Get the execution Ids of one with 3 task executions
        Optional<List<Execution>> completeSubProcessExecutions = taskExecutionsByParent.values().stream()
            .filter(l -> l.size() == 3)
            .findFirst();

        if (completeSubProcessExecutions.isPresent()) {
            List<String> executionIds = completeSubProcessExecutions.get().stream()
                .map(Execution::getId).collect(Collectors.toList());
            tasks.stream().filter(t -> executionIds.contains(t.getExecutionId())).forEach(this::completeTask);
        } else {
            fail("Invalid test state, there should be subProcess instance with embedded gateway with all parallel tasks pending for execution");
        }

        childExecutions = processExecutionRoots.stream()
            .flatMap(rootProcess -> runtimeService.createExecutionQuery().processInstanceId(rootProcess.getId()).onlyChildExecutions().list().stream())
            .collect(Collectors.toList());
        //1x MultiInstance root, 2x parallel MultiInstance, 2x CalledActivitySubProcesses and 4x UserTasks executions
        assertEquals(9, childExecutions.size());
        classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertNotNull(classifiedExecutions.get("multiInstanceSubProcess"));
        assertEquals(3, classifiedExecutions.get("multiInstanceSubProcess").size());
        assertNotNull(classifiedExecutions.get("callActivity"));
        assertEquals(2, classifiedExecutions.get("callActivity").size());
        assertNotNull(classifiedExecutions.get("taskInclusive1"));
        assertEquals(1, classifiedExecutions.get("taskInclusive1").size());
        assertNotNull(classifiedExecutions.get("taskInclusive2"));
        assertEquals(2, classifiedExecutions.get("taskInclusive2").size());
        assertNotNull(classifiedExecutions.get("taskInclusive3"));
        assertEquals(1, classifiedExecutions.get("taskInclusive3").size());

        //4x UserTasks
        tasks = taskService.createTaskQuery().list();
        classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(3, classifiedTasks.size());
        assertTrue(classifiedTasks.containsKey("taskInclusive1"));
        assertEquals(1, classifiedTasks.get("taskInclusive1").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive2"));
        assertEquals(2, classifiedTasks.get("taskInclusive2").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive3"));
        assertEquals(1, classifiedTasks.get("taskInclusive3").size());

        //Finish pending tasks
        tasks.stream().forEach(this::completeTask);

        //Called process should have ended, only the initial root process should remain
        //1x Process Root, 3x Call activity roots
        processExecutionRoots = runtimeService.createExecutionQuery().onlyProcessInstanceExecutions().list();
        assertEquals(1, processExecutionRoots.size());
        assertNull(processExecutionRoots.get(0).getSuperExecutionId());

        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, childExecutions.size());
        assertEquals("lastTask", childExecutions.get(0).getActivityId());

        //1x UserTasks
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());

        //Finish the process
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    /*
     * @Test
     * @Deployment public void testAsyncBehavior() { for (int i = 0; i < 100; i++) { ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("async"); } assertEquals(200,
     * managementService.createJobQuery().count()); waitForJobExecutorToProcessAllJobs(120000, 5000); assertEquals(0, managementService.createJobQuery().count()); assertEquals(0,
     * runtimeService.createProcessInstanceQuery().count()); }
     */

    // /* This test case is related to ACT-1877 */
    //
    // @Test
    // @Deployment(resources={"org/flowable/engine/test/bpmn/gateway/InclusiveGatewayTest.testWithSignalBoundaryEvent.bpmn20.xml"})
    // public void testJoinAfterBoudarySignalEvent() {
    //
    //
    // ProcessInstance processInstanceId =
    // runtimeService.startProcessInstanceByKey("InclusiveGatewayAfterSignalBoundaryEvent");
    //
    // /// Gets the execution waiting for a message notification*/
    // String subcriptedExecutionId =
    // runtimeService.createExecutionQuery().processInstanceId(processInstanceId.getId()).messageEventSubscriptionName("MyMessage").singleResult().getId();
    //
    // /*Notify message received: this makes one execution to go on*/
    // runtimeService.messageEventReceived("MyMessage", subcriptedExecutionId);
    //
    // /*The other execution goes on*/
    // org.flowable.task.service.Task userTask =
    // taskService.createTaskQuery().processInstanceId(processInstanceId.getId()).singleResult();
    // assertEquals("There's still an active execution waiting in the first task",
    // "usertask1",userTask.getTaskDefinitionKey());
    //
    // taskService.complete( userTask.getId());
    //
    // /*The two executions become one because of Inclusive Gateway*/
    // /*The process ends*/
    // userTask =
    // taskService.createTaskQuery().processInstanceId(processInstanceId.getId()).singleResult();
    // assertEquals("Only when both executions reach the inclusive gateway, flow arrives to the last user task",
    // "usertask2",userTask.getTaskDefinitionKey());
    // taskService.complete(userTask.getId());
    //
    // long nExecutions =
    // runtimeService.createExecutionQuery().processInstanceId(processInstanceId.getId()).count();
    // assertEquals(0, nExecutions);
    //
    // }
}
