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
package org.flowable.engine.test.api.v6;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * These are the first tests ever written for Flowable 6. Keeping them here for nostalgic reasons.
 * 
 * @author Joram Barrez
 */
public class Flowable6Test extends PluggableFlowableTestCase {

    @Test
    public void testSimplestProcessPossible() {
        repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/api/v6/Flowable6Test.simplestProcessPossible.bpmn20.xml").deploy();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.isEnded()).isTrue();

        // Cleanup
        for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @org.flowable.engine.test.Deployment
    public void testOneTaskProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.isEnded()).isFalse();

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("The famous task");
        assertThat(task.getAssignee()).isEqualTo("kermit");

        taskService.complete(task.getId());
    }

    @Test
    @org.flowable.engine.test.Deployment(resources = "org/flowable/engine/test/api/v6/Flowable6Test.testOneTaskProcess.bpmn20.xml")
    public void testOneTaskProcessCleanupInMiddleOfProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.isEnded()).isFalse();

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("The famous task");
        assertThat(task.getAssignee()).isEqualTo("kermit");
    }

    @Test
    @org.flowable.engine.test.Deployment
    public void testSimpleParallelGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleParallelGateway");
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.isEnded()).isFalse();

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processDefinitionKey("simpleParallelGateway").orderByTaskName().asc().list();
        assertThat(tasks).hasSize(2);
        assertThat(tasks.get(0).getName()).isEqualTo("Task a");
        assertThat(tasks.get(1).getName()).isEqualTo("Task b");

        for (org.flowable.task.api.Task task : tasks) {
            taskService.complete(task.getId());
        }

        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
    }

    @Test
    @org.flowable.engine.test.Deployment
    public void testSimpleNestedParallelGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleParallelGateway");
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.isEnded()).isFalse();

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processDefinitionKey("simpleParallelGateway").orderByTaskName().asc().list();
        assertThat(tasks).hasSize(4);
        assertThat(tasks.get(0).getName()).isEqualTo("Task a");
        assertThat(tasks.get(1).getName()).isEqualTo("Task b1");
        assertThat(tasks.get(2).getName()).isEqualTo("Task b2");
        assertThat(tasks.get(3).getName()).isEqualTo("Task c");

        for (org.flowable.task.api.Task task : tasks) {
            taskService.complete(task.getId());
        }

        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
    }

    /*
     * This fails on Activiti 5
     */
    @Test
    @org.flowable.engine.test.Deployment
    public void testLongServiceTaskLoop() {
        CountingServiceTaskTestDelegate.CALL_COUNT.set(0); // needs to be reset (because build will retry tests on failure)

        int maxCount = 999; // You can make this as big as you want (as long as it still fits within transaction timeouts).
                             // Go on, try it!
        Map<String, Object> vars = new HashMap<>();
        vars.put("counter", 0);
        vars.put("maxCount", maxCount);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testLongServiceTaskLoop", vars);
        assertThat(processInstance).isNotNull();

        assertThat(CountingServiceTaskTestDelegate.CALL_COUNT.get()).isEqualTo(maxCount);
        assertThat(runtimeService.createActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityId("serviceTask")
                .count())
                .isEqualTo(maxCount);
    }

    @Test
    @org.flowable.engine.test.Deployment
    public void testScriptTask() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("a", 1);
        variableMap.put("b", 2);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variableMap);
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.isEnded()).isFalse();

        Number sumVariable = (Number) runtimeService.getVariable(processInstance.getId(), "sum");
        assertThat(sumVariable.intValue()).isEqualTo(3);

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult();
        assertThat(execution).isNotNull();

        runtimeService.trigger(execution.getId());

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult()).isNull();
    }

    @Test
    @org.flowable.engine.test.Deployment
    public void testSimpleTimerBoundaryEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleBoundaryTimer");
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.isEnded()).isFalse();

        Job job = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Task after timer");

        taskService.complete(task.getId());
        assertThat(runtimeService.createExecutionQuery().count()).isZero();
    }

    @Test
    @org.flowable.engine.test.Deployment
    public void testSimpleTimerBoundaryEventTimerDoesNotFire() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleBoundaryTimer");
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.isEnded()).isFalse();

        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("The famous task");
        taskService.complete(task.getId());

        assertThat(managementService.createTimerJobQuery().count()).isZero();
        assertThat(runtimeService.createExecutionQuery().count()).isZero();
    }

    @Test
    @org.flowable.engine.test.Deployment
    public void testSimpleNonInterruptingTimerBoundaryEvent() {

        // First test: first the task associated with the parent execution, then
        // the one with the child
        // (see the task name ordering in the query to get that specific order)

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleBoundaryTimer");
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.isEnded()).isFalse();

        Job job = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertThat(tasks).hasSize(2);

        // Completing them both should complete the process instance
        for (org.flowable.task.api.Task task : tasks) {
            taskService.complete(task.getId());
        }

        assertThat(runtimeService.createExecutionQuery().count()).isZero();

        // Second test: complete tasks: first task associated with child
        // execution, then parent execution (easier case)
        runtimeService.startProcessInstanceByKey("simpleBoundaryTimer");

        job = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        tasks = taskService.createTaskQuery().orderByTaskName().desc().list(); // Not the desc() here: org.flowable.task.service.Task B, org.flowable.task.service.Task A will be the result (task b being associated with the child execution)
        for (org.flowable.task.api.Task task : tasks) {
            taskService.complete(task.getId());
        }
        assertThat(runtimeService.createExecutionQuery().count()).isZero();
    }

    @Test
    @org.flowable.engine.test.Deployment
    public void testConditionsWithoutExclusiveGateway() {

        // 3 conditions are true for input = 2
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testConditions", CollectionUtil.singletonMap("input", 2));
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.isEnded()).isFalse();

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertThat(tasks).hasSize(3);
        assertThat(tasks.get(0).getName()).isEqualTo("A");
        assertThat(tasks.get(1).getName()).isEqualTo("B");
        assertThat(tasks.get(2).getName()).isEqualTo("C");

        for (org.flowable.task.api.Task t : tasks) {
            taskService.complete(t.getId());
        }

        // 2 conditions are true for input = 20
        runtimeService.startProcessInstanceByKey("testConditions", CollectionUtil.singletonMap("input", 20));
        task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertThat(tasks).hasSize(2);
        assertThat(tasks.get(0).getName()).isEqualTo("B");
        assertThat(tasks.get(1).getName()).isEqualTo("C");

        for (org.flowable.task.api.Task t : tasks) {
            taskService.complete(t.getId());
        }

        // 1 condition is true for input = 200
        runtimeService.startProcessInstanceByKey("testConditions", CollectionUtil.singletonMap("input", 200));
        task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getName()).isEqualTo("C");

        for (org.flowable.task.api.Task t : tasks) {
            taskService.complete(t.getId());
        }
    }
    
    @Test
    @org.flowable.engine.test.Deployment
    public void testNonInterruptingWithVariables() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nonInterrupting", Collections.singletonMap("testVar", "test"));
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.isEnded()).isTrue();
        Map<String, Object> varMap = ((ExecutionEntity) processInstance).getVariables();
        assertThat(varMap)
                .containsExactly(entry("testVar", "test"));
    }

    @Test
    @org.flowable.engine.test.Deployment
    public void testNonInterruptingMoreComplex() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nonInterruptingTimer");
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.isEnded()).isFalse();

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "B");

        // Triggering the timers cancels B, but A is not interrupted
        List<Job> jobs = managementService.createTimerJobQuery().list();
        assertThat(jobs).hasSize(2);
        for (Job job : jobs) {
            managementService.moveTimerToExecutableJob(job.getId());
            managementService.executeJob(job.getId());
        }

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "C", "D", "E", "F");

        // Firing timer shouldn't cancel anything, but create new task
        jobs = managementService.createTimerJobQuery().list();
        assertThat(jobs).hasSize(1);
        managementService.moveTimerToExecutableJob(jobs.get(0).getId());
        managementService.executeJob(jobs.get(0).getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "C", "D", "E", "F", "G");

        // Completing all tasks in this order should give the engine a bit
        // exercise (parent executions first)
        for (org.flowable.task.api.Task task : tasks) {
            taskService.complete(task.getId());
        }

        assertThat(runtimeService.createExecutionQuery().count()).isZero();
    }

    @Test
    @org.flowable.engine.test.Deployment
    public void testNonInterruptingMoreComplex2() {

        // Use case 1: no timers fire
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nonInterruptingWithInclusiveMerge");
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.isEnded()).isFalse();

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "B");
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(2);

        // Completing A
        taskService.complete(tasks.get(0).getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("B");
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);

        // Completing B should end the process
        taskService.complete(tasks.get(0).getId());
        assertThat(managementService.createTimerJobQuery().count()).isZero();
        assertThat(runtimeService.createExecutionQuery().count()).isZero();

        // Use case 2: The non interrupting timer on B fires
        processInstance = runtimeService.startProcessInstanceByKey("nonInterruptingWithInclusiveMerge");
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "B");
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(2);

        // Completing B
        taskService.complete(tasks.get(1).getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A");
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);

        // Firing the timer should activate E and F too
        String jobId = managementService.createTimerJobQuery().singleResult().getId();
        managementService.moveTimerToExecutableJob(jobId);
        managementService.executeJob(jobId);
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "C", "D");

        // Firing the timer on D
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);
        jobId = managementService.createTimerJobQuery().singleResult().getId();
        managementService.moveTimerToExecutableJob(jobId);
        managementService.executeJob(jobId);
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "C", "D", "G");

        // Completing C, D, A and G in that order to give the engine a bit of exercise
        taskService.complete(taskService.createTaskQuery().taskName("C").singleResult().getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "D", "G");

        taskService.complete(taskService.createTaskQuery().taskName("D").singleResult().getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "G");

        taskService.complete(taskService.createTaskQuery().taskName("A").singleResult().getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("G");

        taskService.complete(taskService.createTaskQuery().taskName("G").singleResult().getId());
        assertThat(runtimeService.createExecutionQuery().count()).isZero();

    }

    /**
     * Based on the process and use cases described in http://www.bp-3.com/blogs/2013/09/joins-and-ibm-bpm-diving-deeper/
     */
    @Test
    @org.flowable.engine.test.Deployment
    public void testInclusiveTrickyMerge() {

        // Use case 1 (easy):
        // "When C completes, depending on the data, we can immediately issue E no matter what the status is of A or B."
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("trickyInclusiveMerge");
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.isEnded()).isFalse();
        assertThat(taskService.createTaskQuery().count()).isEqualTo(3);

        org.flowable.task.api.Task taskC = taskService.createTaskQuery().taskName("C").singleResult();
        taskService.complete(taskC.getId());
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "B", "E");

        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());
        tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("D", "E");

        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());
        assertThat(runtimeService.createExecutionQuery().count()).isZero();

        // Use case 2 (tricky):
        // "If A and B are complete and C routes to E, D will be issued in Parallel to E"
        // It's tricky cause the inclusive gateway is not visited directly.
        // Instead, it's done by the InactivatedActivityBehavior

        processInstance = runtimeService.startProcessInstanceByKey("trickyInclusiveMerge");
        assertThat(taskService.createTaskQuery().count()).isEqualTo(3);

        tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("A", "B", "C");
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());

        // C should still be open
        tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("C");

        // If C is now completed, the inclusive gateway should also be completed
        // and D and E should be open tasks
        taskService.complete(tasks.get(0).getId());
        tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("D", "E");

        // Completing them should just end the process instance
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());
        assertThat(runtimeService.createExecutionQuery().count()).isZero();
    }

    /**
     * Simple test that checks if all databases have correctly added the process definition tag.
     */
    @Test
    @org.flowable.engine.test.Deployment(resources = "org/flowable/engine/test/api/v6/Flowable6Test.testOneTaskProcess.bpmn20.xml")
    public void testProcessDefinitionTagCreated() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        assertThat(((ProcessDefinitionEntity) processDefinition).getEngineVersion()).isNull();
    }

}
