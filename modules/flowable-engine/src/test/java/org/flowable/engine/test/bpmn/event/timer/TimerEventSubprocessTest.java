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

package org.flowable.engine.test.bpmn.event.timer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 * @author Filip Hrisafov
 */
public class TimerEventSubprocessTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testInterruptingUnderProcessDefinition() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

        // the process instance must have a timer job:
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();

        // if we trigger the usertask, the process terminates and the timer job is removed:
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("task");
        taskService.complete(task.getId());

        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isZero();
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
        assertProcessEnded(processInstance.getId());

        // now we start a new instance but this time we trigger the timer job:
        processInstance = runtimeService.startProcessInstanceByKey("process");
        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        job = managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("eventSubProcessTask");
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isZero();
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }

    @Test
    @Deployment
    public void testNonInterruptingUnderProcessDefinition() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

        // the process instance must have a timer job:
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        // if we trigger the usertask, the process terminates and the event subscription is removed:
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("task");
        taskService.complete(task.getId());
        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isZero();
        assertThat(runtimeService.createExecutionQuery().count()).isZero();

        // now we start a new instance but this time we trigger the event subprocess:
        processInstance = runtimeService.startProcessInstanceByKey("process");
        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        job = managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

        // now let's first complete the task in the main flow:
        task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
        taskService.complete(task.getId());

        // we still have 3 executions:
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        // now let's complete the task in the event subprocess
        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
        taskService.complete(task.getId());

        // done!
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();

        // #################### again, the other way around:

        processInstance = runtimeService.startProcessInstanceByKey("process");
        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        job = managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);

        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
        taskService.complete(task.getId());

        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isZero();

        // we still have 3 executions:
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
        taskService.complete(task.getId());

        // done!
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }

    @Test
    @Deployment
    public void testNonInterruptingSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        job = managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(6);

        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);

        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        job = managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(9);

        assertThat(taskService.createTaskQuery().count()).isEqualTo(3);
        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);

        // now let's first complete the task in the main flow:
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
        taskService.complete(task.getId());

        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isZero();

        // we still have 7 executions:
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(7);

        // now let's complete the first task in the event subprocess
        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").list().get(0);
        taskService.complete(task.getId());

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(4);

        // complete the second task in the event subprocess
        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
        taskService.complete(task.getId());

        // done!
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }

    @Test
    @Deployment
    public void testInterruptingSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        job = managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(5);

        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isZero();

        // now let's complete the task in the event subprocess
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").list().get(0);
        taskService.complete(task.getId());

        // done!
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }
    
    @Test
    @Deployment
    public void testStartingAdditionalTasks() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startingAdditionalTasks");
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        String firstTimerJobId = job.getId();

        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);

        // now let's first complete the task in the main flow:
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task1").singleResult();
        taskService.complete(task.getId());

        List<Job> jobs = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).list();
        assertThat(jobs).hasSize(2);
        
        String secondTimerJobId = null;
        for (Job timerJob : jobs) {
            if (!timerJob.getId().equals(firstTimerJobId)) {
                secondTimerJobId = timerJob.getId();
            }
        }

        job = managementService.moveTimerToExecutableJob(secondTimerJobId);
        managementService.executeJob(job.getId());
        
        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
        
        job = managementService.moveTimerToExecutableJob(firstTimerJobId);
        managementService.executeJob(job.getId());
        
        assertThat(taskService.createTaskQuery().count()).isEqualTo(3);
        
        task = taskService.createTaskQuery().taskDefinitionKey("additionalTask").singleResult();
        taskService.complete(task.getId());

        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
        
        task = taskService.createTaskQuery().taskDefinitionKey("subTask1").singleResult();
        taskService.complete(task.getId());
        
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        
        task = taskService.createTaskQuery().taskDefinitionKey("additionalSubTask").singleResult();
        taskService.complete(task.getId());
        
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        
        task = taskService.createTaskQuery().taskDefinitionKey("task2").singleResult();
        taskService.complete(task.getId());

        // done!
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/event/timer/TimerEventSubprocessTest.testStartingAdditionalTasks.bpmn20.xml")
    public void testStartingAdditionalTasksNoNestedEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startingAdditionalTasks");
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();

        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);

        // now let's first complete the task in the main flow:
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task1").singleResult();
        taskService.complete(task.getId());

        List<Job> jobs = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).list();
        assertThat(jobs).hasSize(2);
        
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        
        task = taskService.createTaskQuery().taskDefinitionKey("subTask1").singleResult();
        taskService.complete(task.getId());
        
        jobs = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).list();
        assertThat(jobs).hasSize(1);
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        
        task = taskService.createTaskQuery().taskDefinitionKey("task2").singleResult();
        taskService.complete(task.getId());

        // done!
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/event/timer/TimerEventSubprocessTest.testStartingAdditionalTasks.bpmn20.xml")
    public void testStartingAdditionalTasksWithNestedEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startingAdditionalTasks");
        
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        String firstTimerJobId = job.getId();

        // now let's first complete the task in the main flow:
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task1").singleResult();
        taskService.complete(task.getId());

        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        List<Job> jobs = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).list();
        assertThat(jobs).hasSize(2);
        
        String secondTimerJobId = null;
        for (Job timerJob : jobs) {
            if (!timerJob.getId().equals(firstTimerJobId)) {
                secondTimerJobId = timerJob.getId();
            }
        }

        job = managementService.moveTimerToExecutableJob(secondTimerJobId);
        managementService.executeJob(job.getId());
        
        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
        
        task = taskService.createTaskQuery().taskDefinitionKey("subTask1").singleResult();
        taskService.complete(task.getId());
        
        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        
        task = taskService.createTaskQuery().taskDefinitionKey("additionalSubTask").singleResult();
        taskService.complete(task.getId());
        
        job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        
        job = managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());
        
        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
        
        task = taskService.createTaskQuery().taskDefinitionKey("task2").singleResult();
        taskService.complete(task.getId());
        
        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isZero();
        
        task = taskService.createTaskQuery().taskDefinitionKey("additionalTask").singleResult();
        taskService.complete(task.getId());

        // done!
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }
    
    @Test
    @Deployment
    public void testStartingAdditionalTasksInterrupting() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startingAdditionalTasks");
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        String firstTimerJobId = job.getId();

        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);

        // now let's first complete the task in the main flow:
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task1").singleResult();
        taskService.complete(task.getId());

        List<Job> jobs = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).list();
        assertThat(jobs).hasSize(2);

        String secondTimerJobId = null;
        for (Job timerJob : jobs) {
            if (!timerJob.getId().equals(firstTimerJobId)) {
                secondTimerJobId = timerJob.getId();
            }
        }

        job = managementService.moveTimerToExecutableJob(secondTimerJobId);
        managementService.executeJob(job.getId());
        
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        
        task = taskService.createTaskQuery().taskDefinitionKey("additionalSubTask").singleResult();
        taskService.complete(task.getId());
        
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        
        task = taskService.createTaskQuery().taskDefinitionKey("task2").singleResult();
        taskService.complete(task.getId());

        // done!
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/event/timer/TimerEventSubprocessTest.testStartingAdditionalTasksInterrupting.bpmn20.xml")
    public void testStartingAdditionalTasksInterruptingWithMainEventSubProcessInterrupt() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startingAdditionalTasks");
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        String firstTimerJobId = job.getId();

        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        
        // now let's first complete the task in the main flow:
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task1").singleResult();
        taskService.complete(task.getId());

        List<Job> jobs = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).list();
        assertThat(jobs).hasSize(2);

        String secondTimerJobId = null;
        for (Job timerJob : jobs) {
            if (!timerJob.getId().equals(firstTimerJobId)) {
                secondTimerJobId = timerJob.getId();
            }
        }

        job = managementService.moveTimerToExecutableJob(secondTimerJobId);
        managementService.executeJob(job.getId());
        
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        
        job = managementService.moveTimerToExecutableJob(firstTimerJobId);
        managementService.executeJob(job.getId());
        
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        
        task = taskService.createTaskQuery().taskDefinitionKey("additionalTask").singleResult();
        taskService.complete(task.getId());

        // done!
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }

    @Test
    @Deployment
    public void testInterruptingSimpleActivities() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testSimpleTimerEventSubProcess");

        assertThat(runtimeService.createActivityInstanceQuery().list())
            .extracting(ActivityInstance::getActivityType, ActivityInstance::getActivityId)
            .containsExactlyInAnyOrder(
                tuple("startEvent", "start"),
                tuple("sequenceFlow", "flow1"),
                tuple("userTask", "task")
            );

        // Trigger the Event sub process
        Job job = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        managementService.moveTimerToExecutableJob(job.getId());
        managementService.executeJob(job.getId());

        assertThat(runtimeService.createActivityInstanceQuery().list())
            .extracting(ActivityInstance::getActivityType, ActivityInstance::getActivityId)
            .containsExactlyInAnyOrder(
                tuple("startEvent", "start"),
                tuple("sequenceFlow", "flow1"),
                tuple("userTask", "task"),
                tuple("eventSubProcess", "timerEventSubProcess"),
                tuple("startEvent", "eventSubProcessTimerStart"),
                tuple("sequenceFlow", "eventSubProcessFlow1"),
                tuple("userTask", "eventSubProcessTask1")
            );

        // Complete the user task in the event sub process
        Task eventSubProcessTask = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask1").singleResult();
        assertThat(eventSubProcessTask).isNotNull();
        taskService.complete(eventSubProcessTask.getId());

        assertThat(runtimeService.createActivityInstanceQuery().list())
            .extracting(ActivityInstance::getActivityType, ActivityInstance::getActivityId)
            .containsExactlyInAnyOrder(
                tuple("startEvent", "start"),
                tuple("sequenceFlow", "flow1"),
                tuple("userTask", "task"),
                tuple("eventSubProcess", "timerEventSubProcess"),
                tuple("startEvent", "eventSubProcessTimerStart"),
                tuple("sequenceFlow", "eventSubProcessFlow1"),
                tuple("userTask", "eventSubProcessTask1"),
                tuple("sequenceFlow", "eventSubProcessFlow2"),
                tuple("endEvent", "eventSubProcessEnd")
            );

        ActivityInstance eventSubProcessActivity = runtimeService.createActivityInstanceQuery()
            .activityType("eventSubProcess")
            .activityId("timerEventSubProcess")
            .singleResult();
        assertThat(eventSubProcessActivity).isNotNull();
        assertThat(eventSubProcessActivity.getEndTime()).isNotNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricActivityInstanceQuery().list())
                .extracting(HistoricActivityInstance::getActivityType, HistoricActivityInstance::getActivityId)
                .containsExactlyInAnyOrder(
                    tuple("startEvent", "start"),
                    tuple("sequenceFlow", "flow1"),
                    tuple("userTask", "task"),
                    tuple("eventSubProcess", "timerEventSubProcess"),
                    tuple("startEvent", "eventSubProcessTimerStart"),
                    tuple("sequenceFlow", "eventSubProcessFlow1"),
                    tuple("userTask", "eventSubProcessTask1"),
                    tuple("sequenceFlow", "eventSubProcessFlow2"),
                    tuple("endEvent", "eventSubProcessEnd")
                );
        }
    }
}
