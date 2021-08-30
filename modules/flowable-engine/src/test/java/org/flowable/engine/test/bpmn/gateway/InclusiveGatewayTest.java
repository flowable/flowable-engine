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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.delegate.MapBasedFlowableFutureJavaDelegate;
import org.flowable.engine.delegate.ReadOnlyDelegateExecution;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.AbstractFlowableTestCase;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryImpl;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

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
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("inclusiveGwDiverging", CollectionUtil.singletonMap("input", 1));
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactlyInAnyOrder(TASK1_NAME, TASK2_NAME, TASK3_NAME);
        runtimeService.deleteProcessInstance(pi.getId(), "testing deletion");

        pi = runtimeService.startProcessInstanceByKey("inclusiveGwDiverging", CollectionUtil.singletonMap("input", 2));
        tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactlyInAnyOrder(TASK2_NAME, TASK3_NAME);
        runtimeService.deleteProcessInstance(pi.getId(), "testing deletion");

        pi = runtimeService.startProcessInstanceByKey("inclusiveGwDiverging", CollectionUtil.singletonMap("input", 3));
        tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactlyInAnyOrder(TASK3_NAME);
        runtimeService.deleteProcessInstance(pi.getId(), "testing deletion");
    }

    @Test
    @Deployment
    public void testMergingInclusiveGateway() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("inclusiveGwMerging", CollectionUtil.singletonMap("input", 2));
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);

        runtimeService.deleteProcessInstance(pi.getId(), "testing deletion");
    }

    @Test
    @Deployment
    public void testMergeWithEndedExecution() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("myProcess");
        Task task1 = taskService.createTaskQuery().processInstanceId(pi.getId()).taskName("Task 1").singleResult();
        Task task2 = taskService.createTaskQuery().processInstanceId(pi.getId()).taskName("Task 2").singleResult();

        taskService.complete(task1.getId());
        taskService.complete(task2.getId(), CollectionUtil.singletonMap("decision", "goDown"));

        assertProcessEnded(pi.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<String> activityNames = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(pi.getId())
                .list()
                .stream()
                .map(HistoricActivityInstance::getActivityName)
                .collect(Collectors.toList());

            assertThat(activityNames).contains("Other end"); // the path downwards needs to be followed
        }
    }

    @Test
    @Deployment
    public void testMergeWithEndedExecutionNestedCommand() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("myProcess");
        Task task1 = taskService.createTaskQuery().processInstanceId(pi.getId()).taskName("Task 1").singleResult();
        Task task2 = taskService.createTaskQuery().processInstanceId(pi.getId()).taskName("Task 2").singleResult();

        taskService.complete(task1.getId());

        // Testing a bug: when the command is nested, the reuse flag gets set to true for the inner command context, never triggering the InactiveBehavior
        managementService.executeCommand(new Command<Object>() {

            @Override
            public Object execute(CommandContext commandContext) {
                taskService.complete(task2.getId(), CollectionUtil.singletonMap("decision", "goDown"));

                return null;
            }

        });

        assertProcessEnded(pi.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<String> activityNames = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(pi.getId())
                .list()
                .stream()
                .map(HistoricActivityInstance::getActivityName)
                .collect(Collectors.toList());

            assertThat(activityNames).contains("Other end"); // the path downwards needs to be followed
        }
    }

    @Test
    @Deployment(extraResources = "org/flowable/engine/test/bpmn/gateway/InclusiveGatewayTest.testProcessInstanceStartedThroughRuntimeService2.bpmn20.xml")
    public void testProcessInstanceStartedThroughRuntimeService() {

        // A slightly odd unit test: the process is started through the runtime service (and that one has an inclusive gateway).
        // This is because of a bugfix that fixes a bug in the handling of nested command context that happened before.

        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);

        // After starting the process instance, two process instances should be fully finished.
        // Before the bugfix, only one was finished, the one with the inclusive gateway wasn't.
        runtimeService.startProcessInstanceByKey("oneServiceTaskProcess");

        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<String> activityNames = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(InclusiveGatewayTestDelegate01.PROCESS_INSTANCE_ID)
                .list()
                .stream()
                .map(HistoricActivityInstance::getActivityName)
                .collect(Collectors.toList());

            assertThat(activityNames).contains("Other end"); // the path downwards needs to be followed
        }
    }

    public static class InclusiveGatewayTestDelegate01 implements JavaDelegate  {

        public static String PROCESS_INSTANCE_ID;

        @Override
        public void execute(DelegateExecution execution) {
            ProcessInstance processInstance = CommandContextUtil.getProcessEngineConfiguration().getRuntimeService()
                .startProcessInstanceByKey("myProcess", CollectionUtil.singletonMap("decision", "goDown"));
            PROCESS_INSTANCE_ID = processInstance.getId();
        }

    }

    @Test
    @Deployment
    public void testPartialMergingInclusiveGateway() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("partialInclusiveGwMerging", CollectionUtil.singletonMap("input", 3));
        org.flowable.task.api.Task partialTask = taskService.createTaskQuery().singleResult();
        assertThat(partialTask.getTaskDefinitionKey()).isEqualTo("partialTask");

        taskService.complete(partialTask.getId());

        org.flowable.task.api.Task fullTask = taskService.createTaskQuery().singleResult();
        assertThat(fullTask.getTaskDefinitionKey()).isEqualTo("theTask");

        runtimeService.deleteProcessInstance(pi.getId(), "testing deletion");
    }

    @Test
    @Deployment
    public void testNoSequenceFlowSelected() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("inclusiveGwNoSeqFlowSelected", CollectionUtil.singletonMap("input", 4)))
                .isInstanceOf(FlowableException.class);
    }

    /**
     * Test for ACT-1216: When merging a concurrent execution the parent is not activated correctly
     */
    @Test
    @Deployment
    public void testParentActivationOnNonJoiningEnd() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parentActivationOnNonJoiningEnd");

        List<Execution> executionsBefore = runtimeService.createExecutionQuery().list();
        assertThat(executionsBefore).hasSize(3);

        // start first round of tasks
        List<org.flowable.task.api.Task> firstTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(firstTasks).hasSize(2);

        for (org.flowable.task.api.Task t : firstTasks) {
            taskService.complete(t.getId());
        }

        // start second round of tasks
        List<org.flowable.task.api.Task> secondTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(secondTasks).hasSize(2);

        // complete one task
        org.flowable.task.api.Task task = secondTasks.get(0);
        taskService.complete(task.getId());

        List<Execution> executionsAfter = runtimeService.createExecutionQuery().list();
        assertThat(executionsAfter).hasSize(2);

        Execution execution = null;
        for (Execution e : executionsAfter) {
            if (e.getParentId() != null) {
                execution = e;
            }
        }

        // and should have one active activity
        List<String> activeActivityIds = runtimeService.getActiveActivityIds(execution.getId());
        assertThat(activeActivityIds).hasSize(1);

        // Completing last task should finish the process instance

        org.flowable.task.api.Task lastTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(lastTask.getId());

        assertThat(runtimeService.createProcessInstanceQuery().active().count()).isZero();
    }

    /**
     * Test for bug ACT-10: whitespaces/newlines in expressions lead to exceptions
     */
    @Test
    @Deployment
    public void testWhitespaceInExpression() {
        // Starting a process instance will lead to an exception if whitespace
        // are incorrectly handled
        runtimeService.startProcessInstanceByKey("inclusiveWhiteSpaceInExpression", CollectionUtil.singletonMap("input", 1));
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/gateway/InclusiveGatewayTest.testDivergingInclusiveGateway.bpmn20.xml" })
    public void testUnknownVariableInExpression() {
        // Instead of 'input' we're starting a process instance with the name
        // 'iinput' (ie. a typo)
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("inclusiveGwDiverging", CollectionUtil.singletonMap("iinput", 1)))
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("Unknown property used in expression");
    }

    @Test
    @Deployment
    public void testDecideBasedOnBeanProperty() {
        runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnBeanProperty", CollectionUtil.singletonMap("order", new InclusiveGatewayTestOrder(150)));
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactlyInAnyOrder(BEAN_TASK2_NAME, BEAN_TASK3_NAME);
    }

    @Test
    @Deployment
    public void testDecideBasedOnListOrArrayOfBeans() {
        List<InclusiveGatewayTestOrder> orders = new ArrayList<>();
        orders.add(new InclusiveGatewayTestOrder(50));
        orders.add(new InclusiveGatewayTestOrder(300));
        orders.add(new InclusiveGatewayTestOrder(175));

        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnListOrArrayOfBeans", CollectionUtil.singletonMap("orders", orders)))
                .isInstanceOf(FlowableException.class);

        orders.set(1, new InclusiveGatewayTestOrder(175));
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnListOrArrayOfBeans", CollectionUtil.singletonMap("orders", orders));
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo(BEAN_TASK3_NAME);

        orders.set(1, new InclusiveGatewayTestOrder(125));
        pi = runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnListOrArrayOfBeans", CollectionUtil.singletonMap("orders", orders));
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertThat(tasks).isNotNull();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactlyInAnyOrder(BEAN_TASK2_NAME, BEAN_TASK3_NAME);

        // Arrays are usable in exactly the same way
        InclusiveGatewayTestOrder[] orderArray = orders.toArray(new InclusiveGatewayTestOrder[orders.size()]);
        orderArray[1].setPrice(10);
        pi = runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnListOrArrayOfBeans", CollectionUtil.singletonMap("orders", orderArray));
        tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertThat(tasks).isNotNull();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactlyInAnyOrder(BEAN_TASK1_NAME, BEAN_TASK2_NAME, BEAN_TASK3_NAME);
    }

    @Test
    @Deployment
    public void testDecideBasedOnBeanMethod() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnBeanMethod", CollectionUtil.singletonMap("order", new InclusiveGatewayTestOrder(200)));
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo(BEAN_TASK3_NAME);

        pi = runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnBeanMethod", CollectionUtil.singletonMap("order", new InclusiveGatewayTestOrder(125)));
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactlyInAnyOrder(BEAN_TASK2_NAME, BEAN_TASK3_NAME);

        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("inclusiveDecisionBasedOnBeanMethod", CollectionUtil.singletonMap("order", new InclusiveGatewayTestOrder(300))))
                .isInstanceOf(FlowableException.class);
    }

    @Test
    @Deployment
    public void testInvalidMethodExpression() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("inclusiveInvalidMethodExpression", CollectionUtil.singletonMap("order", new InclusiveGatewayTestOrder(50))))
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("Unknown method used in expression");
    }

    @Test
    @Deployment
    public void testDefaultSequenceFlow() {
        // Input == 1 -> default is not selected, other 2 tasks are selected
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("inclusiveGwDefaultSequenceFlow", CollectionUtil.singletonMap("input", 1));
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactlyInAnyOrder("Input is one", "Input is three or one");

        runtimeService.deleteProcessInstance(pi.getId(), null);

        // Input == 3 -> default is not selected, "one or three" is selected
        pi = runtimeService.startProcessInstanceByKey("inclusiveGwDefaultSequenceFlow", CollectionUtil.singletonMap("input", 3));
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Input is three or one");

        // Default input
        pi = runtimeService.startProcessInstanceByKey("inclusiveGwDefaultSequenceFlow", CollectionUtil.singletonMap("input", 5));
        task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Default input");
    }

    @Test
    @Deployment
    public void testNoIdOnSequenceFlow() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("inclusiveNoIdOnSequenceFlow", CollectionUtil.singletonMap("input", 3));
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Input is more than one");

        // Both should be enabled on 1
        pi = runtimeService.startProcessInstanceByKey("inclusiveNoIdOnSequenceFlow", CollectionUtil.singletonMap("input", 1));
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactlyInAnyOrder("Input is one", "Input is more than one");
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
        assertThat(task.getName()).isEqualTo("task C");

        taskService.complete(task.getId());
        assertThat(taskService.createTaskQuery().count()).isZero();
        
        assertThat(runtimeService.createExecutionQuery().count())
                .as("Found executions: " + runtimeService.createExecutionQuery().list())
                .isZero();
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).count()).isZero();
    }

    @Test
    @Deployment
    public void testJoinAfterSubprocesses() {
        // Test case to test act-1204
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("a", 1);
        variableMap.put("b", 1);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("InclusiveGateway", variableMap);
        assertThat(processInstance.getId()).isNotNull();

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

        taskService.complete(tasks.get(0).getId());
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);

        taskService.complete(tasks.get(1).getId());

        org.flowable.task.api.Task task = taskService.createTaskQuery().taskAssignee("c").singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());

        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(processInstance).isNull();

        variableMap = new HashMap<>();
        variableMap.put("a", 1);
        variableMap.put("b", 2);
        processInstance = runtimeService.startProcessInstanceByKey("InclusiveGateway", variableMap);
        assertThat(processInstance.getId()).isNotNull();

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        assertThat(tasks)
                .extracting(Task::getAssignee)
                .containsExactly("a");
        taskService.complete(tasks.get(0).getId());

        task = taskService.createTaskQuery().taskAssignee("c").singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());

        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(processInstance).isNull();

        Map<String, Object> newVariableMap = new HashMap<>();
        newVariableMap.put("a", 2);
        newVariableMap.put("b", 2);
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("InclusiveGateway", newVariableMap))
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("No outgoing sequence flow");
    }

    @Test
    @Deployment
    public void testJoinAfterParallelGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("InclusiveGateway");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getName()).isEqualTo("Task1");

        taskService.complete(task.getId());

        Execution execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .activityId("receiveTask1")
                .singleResult();

        assertThat(execution).isNotNull();
        runtimeService.trigger(execution.getId());

        execution = runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .activityId("receiveTask1")
                .singleResult();

        assertThat(execution).isNotNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/gateway/InclusiveGatewayTest.testJoinAfterCall.bpmn20.xml",
            "org/flowable/engine/test/bpmn/gateway/InclusiveGatewayTest.testJoinAfterCallSubProcess.bpmn20.xml" })
    public void testJoinAfterCall() {
        // Test case to test act-1026
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("InclusiveGatewayAfterCall");
        assertThat(processInstance.getId()).isNotNull();
        assertThat(taskService.createTaskQuery().count()).isEqualTo(3);

        // now complete task A and check number of remaining tasks.
        // inclusive gateway should wait for the "Task B" and "Task C"
        org.flowable.task.api.Task taskA = taskService.createTaskQuery().taskName("Task A").singleResult();
        assertThat(taskA).isNotNull();
        taskService.complete(taskA.getId());
        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

        // now complete task B and check number of remaining tasks
        // inclusive gateway should wait for "Task C"
        org.flowable.task.api.Task taskB = taskService.createTaskQuery().taskName("Task B").singleResult();
        assertThat(taskB).isNotNull();
        taskService.complete(taskB.getId());
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);

        // now complete task C. Gateway activates and "Task C" remains
        org.flowable.task.api.Task taskC = taskService.createTaskQuery().taskName("Task C").singleResult();
        assertThat(taskC).isNotNull();
        taskService.complete(taskC.getId());
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);

        // check that remaining task is in fact task D
        org.flowable.task.api.Task taskD = taskService.createTaskQuery().taskName("Task D").singleResult();
        assertThat(taskD).isNotNull();
        assertThat(taskD.getName()).isEqualTo("Task D");
        taskService.complete(taskD.getId());

        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(processInstance).isNull();
    }

    @Test
    @Deployment
    public void testAsyncBehavior() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("async");
        waitForJobExecutorToProcessAllJobs(10000L, 250);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }

    @Test
    @Deployment
    public void testAsyncTasks() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("testAsyncTasks")
            .variable("counter", 0L)
            .start();

        List<Job> jobs = managementService.createJobQuery().processInstanceId(processInstance.getId()).list();
        assertThat(jobs).hasSize(2);

        for (Job job : jobs) {
            managementService.executeJob(job.getId());
        }

        // There should be 2 jobs, one for each excution arriving in the join
        jobs = managementService.createJobQuery().processInstanceId(processInstance.getId()).list();
        assertThat(jobs).hasSize(2);

        for (Job job : jobs) {
            managementService.executeJob(job.getId());
        }

        // There was a bug that async inclusive gw joins would lead to two executions leaving the gateway
        assertThat(runtimeService.getVariable(processInstance.getId(), "counter")).isEqualTo(1L);
    }

    @Test
    @Deployment
    public void testDirectSequenceFlow() {
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("input", 1);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("inclusiveGwDirectSequenceFlow", varMap);
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("theTask1");
        taskService.complete(task.getId());
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();

        varMap = new HashMap<>();
        varMap.put("input", 3);
        processInstance = runtimeService.startProcessInstanceByKey("inclusiveGwDirectSequenceFlow", varMap);
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(2);
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();

        varMap = new HashMap<>();
        varMap.put("input", 0);
        processInstance = runtimeService.startProcessInstanceByKey("inclusiveGwDirectSequenceFlow", varMap);
        assertThat(processInstance.isEnded()).isTrue();
    }

    @Test
    @Deployment
    public void testSkipExpression() {
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", true);
        varMap.put("input", 10);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("inclusiveGwSkipExpression", varMap);
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("theTask1");
        taskService.complete(task.getId());
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();

        varMap = new HashMap<>();
        varMap.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", true);
        varMap.put("input", 30);
        processInstance = runtimeService.startProcessInstanceByKey("inclusiveGwSkipExpression", varMap);
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertThat(tasks).hasSize(2);
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();

        varMap = new HashMap<>();
        varMap.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", true);
        varMap.put("input", 3);
        processInstance = runtimeService.startProcessInstanceByKey("inclusiveGwSkipExpression", varMap);
        assertThat(processInstance.isEnded()).isTrue();
    }
    
    @Test
    @Deployment
    public void testSkipExpressionWithDefinitionInfo() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("inclusiveGwSkipExpression").singleResult();
        ObjectNode infoNode = dynamicBpmnService.enableSkipExpression();
        dynamicBpmnService.saveProcessDefinitionInfo(processDefinition.getId(), infoNode);
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("input", 10);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("inclusiveGwSkipExpression", varMap);
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("theTask1");

        varMap = new HashMap<>();
        varMap.put("input", 30);
        processInstance = runtimeService.startProcessInstanceByKey("inclusiveGwSkipExpression", varMap);
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(2);

        dynamicBpmnService.removeEnableSkipExpression(infoNode);
        dynamicBpmnService.saveProcessDefinitionInfo(processDefinition.getId(), infoNode);
        varMap = new HashMap<>();
        varMap.put("input", 10);
        processInstance = runtimeService.startProcessInstanceByKey("inclusiveGwSkipExpression", varMap);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("theTask2");
        
        dynamicBpmnService.enableSkipExpression(infoNode);
        dynamicBpmnService.changeSkipExpression("flow2", "${input < 30}", infoNode);
        dynamicBpmnService.changeSkipExpression("flow3", "${input >= 30}", infoNode);
        dynamicBpmnService.saveProcessDefinitionInfo(processDefinition.getId(), infoNode);
        varMap = new HashMap<>();
        varMap.put("input", 30);
        processInstance = runtimeService.startProcessInstanceByKey("inclusiveGwSkipExpression", varMap);
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        for (Task taskObject : tasks) {
            if (!"theTask2".equals(taskObject.getTaskDefinitionKey()) && !"theTask3".equals(taskObject.getTaskDefinitionKey())) {
                fail("expected theTask2 and theTask3 only");
            }
        }
        assertThat(tasks).hasSize(2);
    }

    @Test
    @Deployment
    public void testMultipleProcessInstancesMergedBug() {

        // Start first process instance, continue A. Process instance should be in C
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("testMultipleProcessInstancesMergedBug");
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance1.getId()).taskName("A").singleResult().getId());
        org.flowable.task.api.Task taskCInPi1 = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        assertThat(taskCInPi1).isNotNull();

        // Start second process instance, continue A. Process instance should be in B
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("testMultipleProcessInstancesMergedBug", CollectionUtil.singletonMap("var", "goToB"));
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance2.getId()).taskName("A").singleResult().getId());
        org.flowable.task.api.Task taskBInPi2 = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        assertThat(taskBInPi2).isNotNull();

        // Verify there is an inactive execution in the inclusive gateway before the task complete of process instance 1
        // (cannot combine activityId and inactive together, hence the workaround)
        assertThat(getInactiveExecutionsInActivityId("inclusiveGw")).hasSize(2);

        // Completing C of PI 1 should not trigger C
        taskService.complete(taskCInPi1.getId());

        // Verify structure after complete.
        // Before bugfix: in BOTH process instances the inactive execution was removed (result was 0)
        assertThat(getInactiveExecutionsInActivityId("inclusiveGw")).hasSize(1);

        assertThat(taskService.createTaskQuery().taskName("After Merge").count()).isEqualTo(1);

        // Finish both processes

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        while (tasks.size() > 0) {
            for (org.flowable.task.api.Task task : tasks) {
                taskService.complete(task.getId());
            }
            tasks = taskService.createTaskQuery().list();
        }
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

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

        assertThat(tasks).hasSize(1);

        String executionId = processEngine.getManagementService().executeCommand(new Command<String>() {
            @Override
            public String execute(CommandContext commandContext) {
                EventSubscriptionQueryImpl q = new EventSubscriptionQueryImpl(commandContext, processEngineConfiguration.getEventSubscriptionServiceConfiguration());
                q.processInstanceId(instance.getProcessInstanceId());

                List<EventSubscription> subs = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService()
                        .findEventSubscriptionsByQueryCriteria(q);
                assertThat(subs)
                        .extracting(EventSubscription::getEventName)
                        .containsExactly("test");

                return subs.get(0).getExecutionId();
            }
        });

        //send the message, after this we are inside the event subprocess
        runtimeService.messageEventReceived("test", executionId);
        tasks = taskService.createTaskQuery()
                .processDefinitionId(instance.getProcessDefinitionId())
                .list();

        //since it is non interrupting, we now expect 3 tasks to be present
        assertThat(tasks).hasSize(3);

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
        assertThat(childExecutions).hasSize(13);
        Map<String, List<Execution>> classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertThat(classifiedExecutions)
                .containsKeys("multiInstanceSubProcess", "taskInclusive1", "taskInclusive2", "taskInclusive3");
        assertThat(classifiedExecutions.get("multiInstanceSubProcess")).hasSize(4);
        assertThat(classifiedExecutions.get("taskInclusive1")).hasSize(3);
        assertThat(classifiedExecutions.get("taskInclusive2")).hasSize(3);
        assertThat(classifiedExecutions.get("taskInclusive3")).hasSize(3);

        //9x UserTasks
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<Task>> classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertThat(classifiedTasks)
                .containsOnlyKeys("taskInclusive1", "taskInclusive2", "taskInclusive3");
        assertThat(classifiedTasks.get("taskInclusive1")).hasSize(3);
        assertThat(classifiedTasks.get("taskInclusive2")).hasSize(3);
        assertThat(classifiedTasks.get("taskInclusive3")).hasSize(3);

        //Finish a couple of tasks
        taskService.complete(classifiedTasks.get("taskInclusive2").get(0).getId());
        taskService.complete(classifiedTasks.get("taskInclusive3").get(1).getId());

        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(childExecutions).hasSize(13);
        classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertThat(classifiedExecutions)
                .containsKeys("multiInstanceSubProcess", "taskInclusive1", "taskInclusive2", "taskInclusive3", "inclusiveJoin");
        assertThat(classifiedExecutions.get("multiInstanceSubProcess")).hasSize(4);
        assertThat(classifiedExecutions.get("taskInclusive1")).hasSize(3);
        assertThat(classifiedExecutions.get("taskInclusive2")).hasSize(2);
        assertThat(classifiedExecutions.get("taskInclusive3")).hasSize(2);
        assertThat(classifiedExecutions.get("inclusiveJoin")).hasSize(2);

        //7x pending User Tasks
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertThat(classifiedTasks)
                .containsOnlyKeys("taskInclusive1", "taskInclusive2", "taskInclusive3");
        assertThat(classifiedTasks.get("taskInclusive1")).hasSize(3);
        assertThat(classifiedTasks.get("taskInclusive2")).hasSize(2);
        assertThat(classifiedTasks.get("taskInclusive3")).hasSize(2);

        //Finish the rest of the tasks
        classifiedTasks.values().stream().flatMap(List::stream).forEach(this::completeTask);

        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(childExecutions).hasSize(7);
        classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertThat(classifiedExecutions)
                .containsKeys("multiInstanceSubProcess", "postForkTask");
        assertThat(classifiedExecutions.get("multiInstanceSubProcess")).hasSize(4);
        assertThat(classifiedExecutions.get("postForkTask")).hasSize(3);

        //3x pending User Tasks
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(3);
        tasks.forEach(task -> assertThat(task.getTaskDefinitionKey()).isEqualTo("postForkTask"));

        //Finish the remaining tasks in the SubProcess
        tasks.forEach(this::completeTask);

        //MultiInstance subProcess ended, only the last task of the process remains
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(childExecutions).hasSize(1);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("lastTask");

        //Finish the process
        taskService.complete(task.getId());

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/gateway/InclusiveGatewayTest.insideMultiInstanceSequentialSubProcess.bpmn20.xml" })
    public void testInclusiveGatewayInclusiveGatewayInsideSequentialMultiInstanceSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("inclusiveGatewayInsideSequentialMultiInstanceSubProcess");

        List<Execution> childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //1x MultiInstance root, 1x Sequential MultiInstance and 3x UserTasks executions
        assertThat(childExecutions).hasSize(5);
        Map<String, List<Execution>> classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertThat(classifiedExecutions)
                .containsKeys("multiInstanceSubProcess", "taskInclusive1", "taskInclusive2", "taskInclusive3");
        assertThat(classifiedExecutions.get("multiInstanceSubProcess")).hasSize(2);
        assertThat(classifiedExecutions.get("taskInclusive1")).hasSize(1);
        assertThat(classifiedExecutions.get("taskInclusive2")).hasSize(1);
        assertThat(classifiedExecutions.get("taskInclusive3")).hasSize(1);

        //3x UserTasks
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<Task>> classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertThat(classifiedTasks)
                .containsOnlyKeys("taskInclusive1", "taskInclusive2", "taskInclusive3");
        assertThat(classifiedTasks.get("taskInclusive1")).hasSize(1);
        assertThat(classifiedTasks.get("taskInclusive2")).hasSize(1);
        assertThat(classifiedTasks.get("taskInclusive3")).hasSize(1);

        //Finish one of the activities
        taskService.complete(classifiedTasks.get("taskInclusive3").get(0).getId());

        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(childExecutions).hasSize(5);
        classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertThat(classifiedExecutions)
                .containsKeys("multiInstanceSubProcess", "taskInclusive1", "taskInclusive2", "inclusiveJoin")
                .doesNotContainKey("taskInclusive3");
        assertThat(classifiedExecutions.get("multiInstanceSubProcess")).hasSize(2);
        assertThat(classifiedExecutions.get("taskInclusive1")).hasSize(1);
        assertThat(classifiedExecutions.get("taskInclusive2")).hasSize(1);
        assertThat(classifiedExecutions.get("inclusiveJoin")).hasSize(1);

        //2x pending User Tasks
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertThat(classifiedTasks)
                .containsOnlyKeys("taskInclusive1", "taskInclusive2")
                .doesNotContainKeys("taskInclusive3");

        //Finish the rest of the tasks
        Stream.concat(classifiedTasks.get("taskInclusive1").stream(), classifiedTasks.get("taskInclusive2").stream())
                .forEach(this::completeTask);

        //1x MultiInstance root, 1x Sequential MultiInstance, 1x User Task after the gateway join
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(childExecutions).hasSize(3);
        classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertThat(classifiedExecutions)
                .containsKeys("multiInstanceSubProcess", "postForkTask");
        assertThat(classifiedExecutions.get("multiInstanceSubProcess")).hasSize(2);
        assertThat(classifiedExecutions.get("postForkTask")).hasSize(1);

        //Last task of this multiInstance subProcess instance
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("postForkTask");
        taskService.complete(task.getId());

        //The next sequence should start
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //1x MultiInstance root, 1x Sequential MultiInstance and 3x UserTasks executions
        assertThat(childExecutions).hasSize(5);
        classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertThat(classifiedExecutions)
                .containsKeys("multiInstanceSubProcess", "taskInclusive1", "taskInclusive2", "taskInclusive3");
        assertThat(classifiedExecutions.get("multiInstanceSubProcess")).hasSize(2);
        assertThat(classifiedExecutions.get("taskInclusive1")).hasSize(1);
        assertThat(classifiedExecutions.get("taskInclusive2")).hasSize(1);
        assertThat(classifiedExecutions.get("taskInclusive3")).hasSize(1);
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertThat(classifiedTasks)
                .containsOnlyKeys("taskInclusive1", "taskInclusive2", "taskInclusive3");
        assertThat(classifiedTasks.get("taskInclusive1")).hasSize(1);
        assertThat(classifiedTasks.get("taskInclusive2")).hasSize(1);
        assertThat(classifiedTasks.get("taskInclusive3")).hasSize(1);

        //Finish the inclusive gateway tasks
        tasks.forEach(this::completeTask);

        //last task of the sequence
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("postForkTask");
        taskService.complete(task.getId());

        //Last Sequence
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(childExecutions).hasSize(5);
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(3);
        tasks.forEach(this::completeTask);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("postForkTask");
        taskService.complete(task.getId());

        //last task of the process, after the multiInstance subProcess
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(childExecutions).hasSize(1);
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("lastTask");

        //Finish the process
        taskService.complete(task.getId());

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }


    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/gateway/InclusiveGatewayTest.inSubProcessNestedInMultiInstanceParallelSubProcess.bpmn20.xml" })
    public void testInSubProcessNestedInMultiInstanceParallelSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("inclusiveGatewayInsideSubProcessNestedInMultiInstanceParallelSubProcess");

        List<Execution> childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //1x MultiInstance root, 3x parallel MultiInstance, 3x NestedSubProcess and 9x UserTasks executions
        assertThat(childExecutions).hasSize(16);
        Map<String, List<Execution>> classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertThat(classifiedExecutions)
                .containsKeys("multiInstanceSubProcess", "nestedSubProcess", "taskInclusive1", "taskInclusive2", "taskInclusive3");
        assertThat(classifiedExecutions.get("multiInstanceSubProcess")).hasSize(4);
        assertThat(classifiedExecutions.get("nestedSubProcess")).hasSize(3);
        assertThat(classifiedExecutions.get("taskInclusive1")).hasSize(3);
        assertThat(classifiedExecutions.get("taskInclusive2")).hasSize(3);
        assertThat(classifiedExecutions.get("taskInclusive3")).hasSize(3);

        //9x UserTasks
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<Task>> classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertThat(classifiedTasks)
                .containsOnlyKeys("taskInclusive1", "taskInclusive2", "taskInclusive3");
        assertThat(classifiedTasks.get("taskInclusive1")).hasSize(3);
        assertThat(classifiedTasks.get("taskInclusive2")).hasSize(3);
        assertThat(classifiedTasks.get("taskInclusive3")).hasSize(3);

        //Finish a couple of Tasks
        taskService.complete(classifiedTasks.get("taskInclusive1").get(1).getId());
        taskService.complete(classifiedTasks.get("taskInclusive3").get(2).getId());

        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //1x MultiInstance root, 3x parallel MultiInstance, 3x NestedSubProcess and 7x UserTasks executions, 2x Gw Join executions
        assertThat(childExecutions).hasSize(16);
        classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertThat(classifiedExecutions)
                .containsKeys("multiInstanceSubProcess", "nestedSubProcess", "taskInclusive1", "taskInclusive2", "taskInclusive3", "inclusiveJoin");
        assertThat(classifiedExecutions.get("multiInstanceSubProcess")).hasSize(4);
        assertThat(classifiedExecutions.get("nestedSubProcess")).hasSize(3);
        assertThat(classifiedExecutions.get("taskInclusive1")).hasSize(2);
        assertThat(classifiedExecutions.get("taskInclusive2")).hasSize(3);
        assertThat(classifiedExecutions.get("taskInclusive3")).hasSize(2);
        assertThat(classifiedExecutions.get("inclusiveJoin")).hasSize(2);

        //7x UserTasks
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertThat(classifiedTasks)
                .containsOnlyKeys("taskInclusive1", "taskInclusive2", "taskInclusive3");
        assertThat(classifiedTasks.get("taskInclusive1")).hasSize(2);
        assertThat(classifiedTasks.get("taskInclusive2")).hasSize(3);
        assertThat(classifiedTasks.get("taskInclusive3")).hasSize(2);

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
        assertThat(childExecutions).hasSize(14);
        classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertThat(classifiedExecutions)
                .containsKeys("multiInstanceSubProcess", "nestedSubProcess", "taskInclusive1", "taskInclusive2", "taskInclusive3", "inclusiveJoin", "postForkTask");
        assertThat(classifiedExecutions.get("multiInstanceSubProcess")).hasSize(4);
        assertThat(classifiedExecutions.get("nestedSubProcess")).hasSize(3);
        assertThat(classifiedExecutions.get("taskInclusive1")).hasSize(1);
        assertThat(classifiedExecutions.get("taskInclusive2")).hasSize(2);
        assertThat(classifiedExecutions.get("taskInclusive3")).hasSize(1);
        assertThat(classifiedExecutions.get("inclusiveJoin")).hasSize(2);
        assertThat(classifiedExecutions.get("postForkTask")).hasSize(1);

        //5x UserTasks
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertThat(classifiedTasks)
                .containsOnlyKeys("taskInclusive1", "taskInclusive2", "taskInclusive3", "postForkTask");
        assertThat(classifiedTasks.get("taskInclusive1")).hasSize(1);
        assertThat(classifiedTasks.get("taskInclusive2")).hasSize(2);
        assertThat(classifiedTasks.get("taskInclusive3")).hasSize(1);
        assertThat(classifiedTasks.get("postForkTask")).hasSize(1);

        //Finish all gateWayTasks
        tasks.stream().filter(t-> !"postForkTask".equals(t.getTaskDefinitionKey())).forEach(this::completeTask);

        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //1x MultiInstance root, 3x parallel MultiInstance, 3x NestedSubProcess, 4x postFork task Execution
        assertThat(childExecutions).hasSize(10);
        classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertThat(classifiedExecutions)
                .containsKeys("multiInstanceSubProcess", "nestedSubProcess", "postForkTask");
        assertThat(classifiedExecutions.get("multiInstanceSubProcess")).hasSize(4);
        assertThat(classifiedExecutions.get("nestedSubProcess")).hasSize(3);
        assertThat(classifiedExecutions.get("postForkTask")).hasSize(3);

        //3x UserTasks
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertThat(classifiedTasks)
                .containsOnlyKeys("postForkTask");
        assertThat(classifiedTasks.get("postForkTask")).hasSize(3);

        //Finish the nested subprocess tasks
        tasks.forEach(this::completeTask);

        //MultiInstance subProcesses finish as the nested subProcesses end
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //1x User task execution
        assertThat(childExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("lastTask");
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks)
                .extracting(Task::getTaskDefinitionKey)
                .containsExactly("lastTask");

        //Finish the process
        tasks.forEach(this::completeTask);
        
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }

    @Test
    @Deployment(resources = {"org/flowable/engine/test/bpmn/gateway/InclusiveGatewayTest.inCalledActivityNestedInMultiInstanceParallelSubProcess.bpmn20.xml",
    "org/flowable/engine/test/bpmn/gateway/InclusiveGatewayTest.simpleParallelFlow.bpmn20.xml"})
    public void testInCalledActivityNestedInMultiInstanceSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("inclusiveGatewayInsideCalledActivityNestedInMultiInstanceParallelSubProcess");

        //1x Process Root, 3x Call activity roots
        List<Execution> processExecutionRoots = runtimeService.createExecutionQuery().onlyProcessInstanceExecutions().list();
        assertThat(processExecutionRoots).hasSize(4);
        Map<String, List<Execution>> classifiedRoots = processExecutionRoots.stream()
            .collect(Collectors.toMap(e -> e.getSuperExecutionId() != null ? "callActivity" : null, Collections::singletonList, AbstractFlowableTestCase::mergeLists));
        assertThat(classifiedRoots.get(null)).hasSize(1);
        assertThat(classifiedRoots.get("callActivity")).hasSize(3);

        //1x MultiInstance root, 3x parallel MultiInstance, 3x CalledActivitySubProcesses and 9x UserTasks executions
        List<Execution> childExecutions = processExecutionRoots.stream()
            .flatMap(rootProcess -> runtimeService.createExecutionQuery().processInstanceId(rootProcess.getId()).onlyChildExecutions().list().stream())
            .collect(Collectors.toList());
        assertThat(childExecutions).hasSize(16);
        Map<String, List<Execution>> classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertThat(classifiedExecutions)
                .containsKeys("multiInstanceSubProcess", "callActivity", "taskInclusive1", "taskInclusive2", "taskInclusive3");
        assertThat(classifiedExecutions.get("multiInstanceSubProcess")).hasSize(4);
        assertThat(classifiedExecutions.get("callActivity")).hasSize(3);
        assertThat(classifiedExecutions.get("taskInclusive1")).hasSize(3);
        assertThat(classifiedExecutions.get("taskInclusive2")).hasSize(3);
        assertThat(classifiedExecutions.get("taskInclusive3")).hasSize(3);

        //9x UserTasks
        List<Task> tasks = taskService.createTaskQuery().list();
        Map<String, List<Task>> classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertThat(classifiedTasks)
                .containsOnlyKeys("taskInclusive1", "taskInclusive2", "taskInclusive3");
        assertThat(classifiedTasks.get("taskInclusive1")).hasSize(3);
        assertThat(classifiedTasks.get("taskInclusive2")).hasSize(3);
        assertThat(classifiedTasks.get("taskInclusive3")).hasSize(3);

        //Finish a couple of Tasks
        taskService.complete(classifiedTasks.get("taskInclusive1").get(1).getId());
        taskService.complete(classifiedTasks.get("taskInclusive3").get(2).getId());



        childExecutions = processExecutionRoots.stream()
            .flatMap(rootProcess -> runtimeService.createExecutionQuery().processInstanceId(rootProcess.getId()).onlyChildExecutions().list().stream())
            .collect(Collectors.toList());
        //1x MultiInstance root, 3x parallel MultiInstance, 3x CalledActivitySubProcesses and 7x UserTasks executions
        assertThat(childExecutions).hasSize(14);
        classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertThat(classifiedExecutions)
                .containsKeys("multiInstanceSubProcess", "callActivity", "taskInclusive1", "taskInclusive2", "taskInclusive3");
        assertThat(classifiedExecutions.get("multiInstanceSubProcess")).hasSize(4);
        assertThat(classifiedExecutions.get("callActivity")).hasSize(3);
        assertThat(classifiedExecutions.get("taskInclusive1")).hasSize(2);
        assertThat(classifiedExecutions.get("taskInclusive2")).hasSize(3);
        assertThat(classifiedExecutions.get("taskInclusive3")).hasSize(2);

        //7x UserTasks
        tasks = taskService.createTaskQuery().list();
        classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertThat(classifiedTasks)
                .containsOnlyKeys("taskInclusive1", "taskInclusive2", "taskInclusive3");
        assertThat(classifiedTasks.get("taskInclusive1")).hasSize(2);
        assertThat(classifiedTasks.get("taskInclusive2")).hasSize(3);
        assertThat(classifiedTasks.get("taskInclusive3")).hasSize(2);

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
        assertThat(childExecutions).hasSize(9);
        classifiedExecutions = childExecutions.stream().collect(Collectors.groupingBy(Execution::getActivityId));
        assertThat(classifiedExecutions)
                .containsKeys("multiInstanceSubProcess", "callActivity", "taskInclusive1", "taskInclusive2", "taskInclusive3");
        assertThat(classifiedExecutions.get("multiInstanceSubProcess")).hasSize(3);
        assertThat(classifiedExecutions.get("callActivity")).hasSize(2);
        assertThat(classifiedExecutions.get("taskInclusive1")).hasSize(1);
        assertThat(classifiedExecutions.get("taskInclusive2")).hasSize(2);
        assertThat(classifiedExecutions.get("taskInclusive3")).hasSize(1);

        //4x UserTasks
        tasks = taskService.createTaskQuery().list();
        classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertThat(classifiedTasks)
                .containsOnlyKeys("taskInclusive1", "taskInclusive2", "taskInclusive3");
        assertThat(classifiedTasks.get("taskInclusive1")).hasSize(1);
        assertThat(classifiedTasks.get("taskInclusive2")).hasSize(2);
        assertThat(classifiedTasks.get("taskInclusive3")).hasSize(1);

        //Finish pending tasks
        tasks.stream().forEach(this::completeTask);

        //Called process should have ended, only the initial root process should remain
        //1x Process Root, 3x Call activity roots
        processExecutionRoots = runtimeService.createExecutionQuery().onlyProcessInstanceExecutions().list();
        assertThat(processExecutionRoots).hasSize(1);
        assertThat(processExecutionRoots.get(0).getSuperExecutionId()).isNull();

        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(childExecutions)
                .extracting(Execution::getActivityId)
                .containsExactly("lastTask");

        //1x UserTasks
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("lastTask");

        //Finish the process
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    void testWithFutureDelegates() {
        // the setup of the test is the following:
        // there are 3 delegate executions:
        // delegate1_1 -> delegate1_2
        // delegate2_1
        // for delegate 1_1 to complete delegate2_1 should start executing
        // for delegate 1_2 to complete delegate2_1 should start executing and 1_1 should be done
        // for delegate2_1 to complete delegate1_2 should complete

        CountDownLatch delegate1_1Done = new CountDownLatch(1);
        CountDownLatch delegate1_2Done = new CountDownLatch(1);
        CountDownLatch delegate2_1Done = new CountDownLatch(1);
        CountDownLatch delegate2_1Start = new CountDownLatch(1);

        MapBasedFlowableFutureJavaDelegate futureDelegate1_1 = new MapBasedFlowableFutureJavaDelegate() {

            @Override
            public Map<String, Object> execute(ReadOnlyDelegateExecution inputData) {

                try {

                    if (delegate2_1Start.await(2, TimeUnit.SECONDS)) {
                        AtomicInteger counter = (AtomicInteger) inputData.getVariable("counter");
                        return Collections.singletonMap("counterDelegate1_1", counter.incrementAndGet());
                    }

                    throw new FlowableException("Delegate 2_1 did not start");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new FlowableException("Thread was interrupted");
                }
            }

            @Override
            public void afterExecution(DelegateExecution execution, Map<String, Object> executionData) {
                MapBasedFlowableFutureJavaDelegate.super.afterExecution(execution, executionData);
                delegate1_1Done.countDown();
            }
        };

        MapBasedFlowableFutureJavaDelegate futureDelegate1_2 = new MapBasedFlowableFutureJavaDelegate() {

            @Override
            public Map<String, Object> execute(ReadOnlyDelegateExecution inputData) {
                assertThat(inputData.getVariable("counterDelegate1_1")).isEqualTo(1);
                assertThat(inputData.hasVariable("counterDelegate1_2")).isFalse();
                assertThat(inputData.hasVariable("counterDelegate2_1")).isFalse();

                AtomicInteger counter = (AtomicInteger) inputData.getVariable("counter");
                return Collections.singletonMap("counterDelegate1_2", counter.incrementAndGet());
            }

            @Override
            public void afterExecution(DelegateExecution execution, Map<String, Object> executionData) {
                MapBasedFlowableFutureJavaDelegate.super.afterExecution(execution, executionData);
                delegate1_2Done.countDown();
            }
        };

        MapBasedFlowableFutureJavaDelegate futureDelegate2_1 = new MapBasedFlowableFutureJavaDelegate() {

            @Override
            public Map<String, Object> execute(ReadOnlyDelegateExecution inputData) {
                delegate2_1Start.countDown();

                try {
                    if (delegate1_2Done.await(2, TimeUnit.SECONDS)) {
                        AtomicInteger counter = (AtomicInteger) inputData.getVariable("counter");
                        return Collections.singletonMap("counterDelegate2_1", counter.incrementAndGet());
                    }

                    throw new FlowableException("Delegate 1_2 did not complete");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new FlowableException("Thread was interrupted");
                }
            }

            @Override
            public void afterExecution(DelegateExecution execution, Map<String, Object> executionData) {
                assertThat(execution.getVariables())
                        .contains(
                                entry("counterDelegate1_1", 1),
                                entry("counterDelegate1_2", 2)
                        )
                        .doesNotContainKeys("counterDelegate2_1");
                MapBasedFlowableFutureJavaDelegate.super.afterExecution(execution, executionData);
                delegate2_1Done.countDown();
            }
        };

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("myProcess")
                .transientVariable("futureDelegate1_1", futureDelegate1_1)
                .transientVariable("futureDelegate1_2", futureDelegate1_2)
                .transientVariable("futureDelegate2_1", futureDelegate2_1)
                .transientVariable("counter", new AtomicInteger(0))
                .start();

        assertProcessEnded(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery().list();
            Map<String, Object> historicVariables = historicVariableInstances.stream()
                    .filter(variable -> !"initiator".equals(variable.getVariableName()))
                    .collect(Collectors.toMap(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue));

            assertThat(historicVariables)
                    .containsOnly(
                            entry("counterDelegate1_1", 1),
                            entry("counterDelegate1_2", 2),
                            entry("counterDelegate2_1", 3)
                    );
        }
    }

}
