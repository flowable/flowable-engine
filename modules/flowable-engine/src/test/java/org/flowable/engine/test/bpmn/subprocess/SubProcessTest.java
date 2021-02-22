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

package org.flowable.engine.test.bpmn.subprocess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class SubProcessTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testSimpleSubProcess() {

        // After staring the process, the task in the subprocess should be active
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleSubProcess");
        org.flowable.task.api.Task subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

        assertThat(runtimeService.createActivityInstanceQuery().list())
            .extracting(ActivityInstance::getActivityType, ActivityInstance::getActivityId)
            .containsExactlyInAnyOrder(
                tuple("startEvent", "theStart"),
                tuple("sequenceFlow", "flow1"),
                tuple("subProcess", "subProcess"),
                tuple("startEvent", "subProcessStart"),
                tuple("sequenceFlow", "flow2"),
                tuple("userTask", "subProcessTask")
            );

        // After completing the task in the subprocess,
        // the subprocess scope is destroyed and the complete process ends
        taskService.complete(subProcessTask.getId());
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).singleResult()).isNull();
    }

    /**
     * Same test case as before, but now with all automatic steps
     */
    @Test
    @Deployment
    public void testSimpleAutomaticSubProcess() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleSubProcessAutomatic");
        assertThat(pi.isEnded()).isTrue();
        assertProcessEnded(pi.getId());
    }

    @Test
    @Deployment
    public void testSimpleSubProcessWithTimer() {

        // We need to make sure the time ends on .000, .003 or .007 due to SQL Server rounding to that
        Date startTime = Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS).plusMillis(677));

        // After staring the process, the task in the subprocess should be active
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleSubProcess");
        org.flowable.task.api.Task subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

        // Setting the clock forward 2 hours 1 second (timer fires in 2 hours) and fire up the job executor
        processEngineConfiguration.getClock().setCurrentTime(new Date(startTime.getTime() + (2 * 60 * 60 * 1000) + 1000));
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);
        waitForJobExecutorToProcessAllJobs(7000L, 500L);

        // The subprocess should be left, and the escalated task should be active
        org.flowable.task.api.Task escalationTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(escalationTask.getName()).isEqualTo("Fix escalated problem");

        // Verify history for task that was killed
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskName("Task in subprocess").singleResult();
            assertThat(historicTaskInstance.getEndTime()).isNotNull();

            HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("subProcessTask").singleResult();
            assertActivityInstancesAreSame(historicActivityInstance, runtimeService.createActivityInstanceQuery().activityInstanceId(historicActivityInstance.getId()).singleResult());
            assertThat(historicActivityInstance.getEndTime()).isNotNull();
        }
    }

    /**
     * A test case that has a timer attached to the subprocess, where 2 concurrent paths are defined when the timer fires.
     */
    @Deployment
    public void IGNORE_testSimpleSubProcessWithConcurrentTimer() {

        // After staring the process, the task in the subprocess should be active
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleSubProcessWithConcurrentTimer");
        TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(pi.getId()).orderByTaskName().asc();

        org.flowable.task.api.Task subProcessTask = taskQuery.singleResult();
        assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

        // When the timer is fired (after 2 hours), two concurrent paths should be created
        Job job = managementService.createJobQuery().singleResult();
        managementService.executeJob(job.getId());

        List<org.flowable.task.api.Task> tasksAfterTimer = taskQuery.list();
        assertThat(tasksAfterTimer).hasSize(2);
        org.flowable.task.api.Task taskAfterTimer1 = tasksAfterTimer.get(0);
        org.flowable.task.api.Task taskAfterTimer2 = tasksAfterTimer.get(1);
        assertThat(taskAfterTimer1.getName()).isEqualTo("Task after timer 1");
        assertThat(taskAfterTimer2.getName()).isEqualTo("Task after timer 2");

        // Completing the two tasks should end the process instance
        taskService.complete(taskAfterTimer1.getId());
        taskService.complete(taskAfterTimer2.getId());
        assertProcessEnded(pi.getId());
    }

    /**
     * Test case where the simple sub process of previous test cases is nested within another subprocess.
     */
    @Test
    @Deployment
    public void testNestedSimpleSubProcess() {

        // Start and delete a process with a nested subprocess when it is not yet ended
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("nestedSimpleSubProcess", CollectionUtil.singletonMap("someVar", "abc"));
        runtimeService.deleteProcessInstance(pi.getId(), "deleted");

        // After staring the process, the task in the inner subprocess must be active
        pi = runtimeService.startProcessInstanceByKey("nestedSimpleSubProcess");
        org.flowable.task.api.Task subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

        // After completing the task in the subprocess,
        // both subprocesses are destroyed and the task after the subprocess should be active
        taskService.complete(subProcessTask.getId());
        org.flowable.task.api.Task taskAfterSubProcesses = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(taskAfterSubProcesses).isNotNull();
        assertThat(taskAfterSubProcesses.getName()).isEqualTo("Task after subprocesses");
        taskService.complete(taskAfterSubProcesses.getId());

        assertProcessEnded(pi.getId());
    }

    @Test
    @Deployment
    public void testNestedSimpleSubprocessWithTimerOnInnerSubProcess() {
        // We need to make sure the time ends on .000, .003 or .007 due to SQL Server rounding to that
        Date startTime = Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS).plusMillis(370));

        // After staring the process, the task in the subprocess should be active
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("nestedSubProcessWithTimer");
        org.flowable.task.api.Task subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

        // Setting the clock forward 1 hour 5 second (timer fires in 1 hour) and
        // fire up the job executor
        processEngineConfiguration.getClock().setCurrentTime(new Date(startTime.getTime() + (60 * 60 * 1000) + 5000));
        waitForJobExecutorToProcessAllJobs(7000L, 50L);

        // The inner subprocess should be destroyed, and the escalated task should be active
        org.flowable.task.api.Task escalationTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(escalationTask.getName()).isEqualTo("Escalated task");

        // Completing the escalated task, destroys the outer scope and activates
        // the task after the subprocess
        taskService.complete(escalationTask.getId());
        org.flowable.task.api.Task taskAfterSubProcess = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(taskAfterSubProcess.getName()).isEqualTo("Task after subprocesses");
    }

    /**
     * Test case where the simple sub process of previous test cases is nested within two other sub processes
     */
    @Test
    @Deployment
    public void testDoubleNestedSimpleSubProcess() {
        // After staring the process, the task in the inner subprocess must be active
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("nestedSimpleSubProcess");
        org.flowable.task.api.Task subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

        // After completing the task in the subprocess,
        // both subprocesses are destroyed and the task after the subprocess
        // should be active
        taskService.complete(subProcessTask.getId());
        org.flowable.task.api.Task taskAfterSubProcesses = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(taskAfterSubProcesses.getName()).isEqualTo("Task after subprocesses");
    }

    @Test
    @Deployment
    public void testSimpleParallelSubProcess() {

        // After starting the process, the two task in the subprocess should be active
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleParallelSubProcess");
        List<org.flowable.task.api.Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(pi.getId()).orderByTaskName().asc().list();

        // Tasks are ordered by name (see query)
        org.flowable.task.api.Task taskA = subProcessTasks.get(0);
        org.flowable.task.api.Task taskB = subProcessTasks.get(1);
        assertThat(taskA.getName()).isEqualTo("Task A");
        assertThat(taskB.getName()).isEqualTo("Task B");

        // Completing both tasks, should destroy the subprocess and activate the
        // task after the subprocess
        taskService.complete(taskA.getId());
        taskService.complete(taskB.getId());
        org.flowable.task.api.Task taskAfterSubProcess = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(taskAfterSubProcess.getName()).isEqualTo("Task after sub process");
    }

    @Test
    @Deployment
    public void testSimpleParallelSubProcessWithTimer() {

        // After staring the process, the tasks in the subprocess should be active
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleParallelSubProcessWithTimer");
        List<org.flowable.task.api.Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();

        // Tasks are ordered by name (see query)
        org.flowable.task.api.Task taskA = subProcessTasks.get(0);
        org.flowable.task.api.Task taskB = subProcessTasks.get(1);
        assertThat(taskA.getName()).isEqualTo("Task A");
        assertThat(taskB.getName()).isEqualTo("Task B");

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();

        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        // The inner subprocess should be destroyed, and the task after the timer should be active
        org.flowable.task.api.Task taskAfterTimer = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(taskAfterTimer.getName()).isEqualTo("Task after timer");

        // Completing the task after the timer ends the process instance
        taskService.complete(taskAfterTimer.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    public void testTwoSubProcessInParallel() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("twoSubProcessInParallel");
        TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(pi.getId()).orderByTaskName().asc();
        List<org.flowable.task.api.Task> tasks = taskQuery.list();

        // After process start, both tasks in the subprocesses should be active
        assertThat(tasks.get(0).getName()).isEqualTo("Task in subprocess A");
        assertThat(tasks.get(1).getName()).isEqualTo("Task in subprocess B");

        // Completing both tasks should active the tasks outside the
        // subprocesses
        taskService.complete(tasks.get(0).getId());

        tasks = taskQuery.list();
        assertThat(tasks.get(0).getName()).isEqualTo("Task after subprocess A");
        assertThat(tasks.get(1).getName()).isEqualTo("Task in subprocess B");

        taskService.complete(tasks.get(1).getId());

        tasks = taskQuery.list();

        assertThat(tasks.get(0).getName()).isEqualTo("Task after subprocess A");
        assertThat(tasks.get(1).getName()).isEqualTo("Task after subprocess B");

        // Completing these tasks should end the process
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());

        assertProcessEnded(pi.getId());
    }

    @Test
    @Deployment
    public void testTwoSubProcessInParallelWithinSubProcess() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("twoSubProcessInParallelWithinSubProcess");
        TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(pi.getId()).orderByTaskName().asc();
        List<org.flowable.task.api.Task> tasks = taskQuery.list();

        // After process start, both tasks in the subprocesses should be active
        org.flowable.task.api.Task taskA = tasks.get(0);
        org.flowable.task.api.Task taskB = tasks.get(1);
        assertThat(taskA.getName()).isEqualTo("Task in subprocess A");
        assertThat(taskB.getName()).isEqualTo("Task in subprocess B");

        // Completing both tasks should active the tasks outside the subprocesses
        taskService.complete(taskA.getId());
        taskService.complete(taskB.getId());

        org.flowable.task.api.Task taskAfterSubProcess = taskQuery.singleResult();
        assertThat(taskAfterSubProcess.getName()).isEqualTo("Task after subprocess");

        // Completing this task should end the process
        taskService.complete(taskAfterSubProcess.getId());
        assertProcessEnded(pi.getId());
    }

    @Test
    @Deployment
    public void testTwoNestedSubProcessesInParallelWithTimer() {

        // Date startTime = new Date();

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("nestedParallelSubProcessesWithTimer");
        TaskQuery taskQuery = taskService.createTaskQuery().processInstanceId(pi.getId()).orderByTaskName().asc();
        List<org.flowable.task.api.Task> tasks = taskQuery.list();

        // After process start, both tasks in the subprocesses should be active
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task in subprocess A", "Task in subprocess B");

        // Firing the timer should destroy all three subprocesses and activate the task after the timer
        // processEngineConfiguration.getClock().setCurrentTime(new
        // Date(startTime.getTime() + (2 * 60 * 60 * 1000 ) + 1000));
        // waitForJobExecutorToProcessAllJobs(7000L, 50L);
        Job job = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        org.flowable.task.api.Task taskAfterTimer = taskQuery.singleResult();
        assertThat(taskAfterTimer.getName()).isEqualTo("Task after timer");

        // Completing the task should end the process instance
        taskService.complete(taskAfterTimer.getId());
        assertProcessEnded(pi.getId());
    }

    /**
     * @see <a href="https://activiti.atlassian.net/browse/ACT-1072">https://activiti.atlassian.net/browse/ACT-1072</a>
     */
    @Test
    @Deployment
    public void testNestedSimpleSubProcessWithoutEndEvent() {
        testNestedSimpleSubProcess();
    }

    /**
     * @see <a href="https://activiti.atlassian.net/browse/ACT-1072">https://activiti.atlassian.net/browse/ACT-1072</a>
     */
    @Test
    @Deployment
    public void testSimpleSubProcessWithoutEndEvent() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("testSimpleSubProcessWithoutEndEvent");
        assertProcessEnded(pi.getId());
    }

    /**
     * @see <a href="https://activiti.atlassian.net/browse/ACT-1072">https://activiti.atlassian.net/browse/ACT-1072</a>
     */
    @Test
    @Deployment
    public void testNestedSubProcessesWithoutEndEvents() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("testNestedSubProcessesWithoutEndEvents");
        assertProcessEnded(pi.getId());
    }

    /**
     * @see <a href="https://activiti.atlassian.net/browse/ACT-1847">https://activiti.atlassian.net/browse/ACT-1847</a>
     */
    @Test
    @Deployment
    public void testDataObjectScope() {

        // After staring the process, the task in the subprocess should be
        // active
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("dataObjectScope");

        // get main process task
        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();

        assertThat(currentTask.getName()).isEqualTo("Complete Task A");

        // verify main process scoped variables
        Map<String, Object> variables = runtimeService.getVariables(pi.getId());
        assertThat(variables)
                .containsOnly(
                        entry("StringTest123", "Testing123"),
                        entry("NoData123", null)
                );

        // After completing the task in the main process, the subprocess scope
        // initiates
        taskService.complete(currentTask.getId());

        // get subprocess task
        currentTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();

        assertThat(currentTask.getName()).isEqualTo("Complete SubTask");

        // verify current scoped variables - includes subprocess variables
        variables = runtimeService.getVariables(currentTask.getExecutionId());
        assertThat(variables)
                .containsOnly(
                        entry("StringTest123", "Testing123"),
                        entry("StringTest456", "Testing456"),
                        entry("NoData123", null)
                );

        // After completing the task in the subprocess, the subprocess scope is destroyed and the main process continues
        taskService.complete(currentTask.getId());

        // verify main process scoped variables
        variables = runtimeService.getVariables(pi.getId());
        assertThat(variables)
                .containsOnly(
                        entry("StringTest123", "Testing123"),
                        entry("NoData123", null)
                );

        currentTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        // Verify there are no local variables assigned to the current task. (subprocess variables are gone).
        variables = runtimeService.getVariablesLocal(currentTask.getExecutionId());
        assertThat(variables).isEmpty();

        // After completing the final task in the main process,
        // the process scope is destroyed and the process ends
        currentTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(currentTask.getName()).isEqualTo("Complete Task B");

        taskService.complete(currentTask.getId());
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).singleResult()).isNull();
    }

    @Test
    @Deployment
    public void testAsyncMiSequentialSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testAsyncMiSubProcess")
                .variable("myList", Arrays.asList("one", "two", "three"))
                .start();
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isZero();

        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        managementService.executeJob(job.getId());

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        String variable = (String) runtimeService.getVariable(task.getExecutionId(), "counter");
        assertThat(variable).isEqualTo("one");
        taskService.complete(task.getId());

        Job secondJob = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(secondJob.getId()).isNotSameAs(job.getId());
        managementService.executeJob(secondJob.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat((String) runtimeService.getVariable(task.getExecutionId(), "counter")).isEqualTo("two");
        taskService.complete(task.getId());

        Job thirdJob = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(thirdJob.getId()).isNotSameAs(secondJob.getId());
        managementService.executeJob(thirdJob.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat((String) runtimeService.getVariable(task.getExecutionId(), "counter")).isEqualTo("three");
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    public void testAsyncMiParallelSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testAsyncMiSubProcess")
                .variable("myList", Arrays.asList("one", "two", "three"))
                .start();
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isZero();

        List<Job> jobs = managementService.createJobQuery().processInstanceId(processInstance.getId()).list();
        assertThat(jobs).hasSize(3);
        jobs.forEach(job -> managementService.executeJob(job.getId()));

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);
        taskService.createTaskQuery().processInstanceId(processInstance.getId()).list().forEach(task -> taskService.complete(task.getId()));

        assertProcessEnded(processInstance.getId());
    }
}
