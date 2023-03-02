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
package org.flowable.engine.test.bpmn.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.jobexecutor.AsyncContinuationJobHandler;
import org.flowable.engine.impl.jobexecutor.AsyncLeaveJobHandler;
import org.flowable.engine.impl.jobexecutor.ParallelMultiInstanceActivityCompletionJobHandler;
import org.flowable.engine.impl.jobexecutor.ParallelMultiInstanceWithNoWaitStatesAsyncLeaveJobHandler;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.task.api.TaskInfo;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class AsyncLeaveTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testStartAsyncLeave() {
        String processInstanceId = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("process")
                .start()
                .getId();

        Job job = managementService.createJobQuery().processInstanceId(processInstanceId).singleResult();
        assertThat(job.getJobHandlerType()).isEqualTo(AsyncLeaveJobHandler.TYPE);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).count()).isOne();
        assertThat(taskService.createTaskQuery().processInstanceId(processInstanceId).count()).isZero();
        assertThat(runtimeService.createActivityInstanceQuery().processInstanceId(processInstanceId).count()).isOne(); // start event activity is recor

        ActivityInstance activityInstance = runtimeService.createActivityInstanceQuery().singleResult();
        assertThat(activityInstance.getActivityId()).isEqualTo("theStart");
        assertThat(activityInstance.getStartTime()).isNotNull();
        assertThat(activityInstance.getEndTime()).isNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.INSTANCE, processEngineConfiguration)) {
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).count()).isOne();
        }

        managementService.executeJob(job.getId());

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).count()).isOne();
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.INSTANCE, processEngineConfiguration)) {
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).count()).isOne();
        }

        assertThat(taskService.createTaskQuery().processInstanceId(processInstanceId).count()).isOne();
        assertThat(runtimeService.createActivityInstanceQuery().processInstanceId(processInstanceId).count()).isEqualTo(5); // 3 activities (start, service, userTask), 2 seq flow

        activityInstance = runtimeService.createActivityInstanceQuery().activityId("theStart").singleResult();
        assertThat(activityInstance.getStartTime()).isNotNull();
        assertThat(activityInstance.getEndTime()).isNotNull();
    }

    @Test
    @Deployment
    public void testServiceTaskAsyncLeave() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("process")
                .start();

        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job.getJobHandlerType()).isEqualTo(AsyncLeaveJobHandler.TYPE);

        assertThat(runtimeService.getVariable(processInstance.getId(), "myResultVar")).isEqualTo("Hello World");
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isZero();

        ActivityInstance activityInstance = runtimeService.createActivityInstanceQuery().activityId("service1").singleResult();
        assertThat(activityInstance.getActivityId()).isEqualTo("service1");
        assertThat(activityInstance.getStartTime()).isNotNull();
        assertThat(activityInstance.getEndTime()).isNull();

        managementService.executeJob(job.getId());

        assertThat(runtimeService.getVariable(processInstance.getId(), "myResultVar")).isEqualTo("Hello World");
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isOne();

        activityInstance = runtimeService.createActivityInstanceQuery().activityId("service1").singleResult();
        assertThat(activityInstance.getStartTime()).isNotNull();
        assertThat(activityInstance.getEndTime()).isNotNull();

    }

    @Test
    @Deployment
    public void testExclusiveGatewayAsyncLeave() {
        // This test is intended to test the condition evaluation (which is a job parameter)

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("process")
                .variable("input", 2)
                .start();

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isZero();

        managementService.executeJob(managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getName()).isEqualTo("Task B");
    }

    @Test
    @Deployment
    public void testParallelGatewayAsyncLeave() {
        // This test has a parallel gw with conditions, which should be ignored when leaving async

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("process")
                .variable("input", 2)
                .start();

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isZero();

        managementService.executeJob(managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(2);
    }

    @Test
    @Deployment
    public void testUserTaskWithMultipleOutgoingSequenceFlowAsyncLeave() {
        // This test has a parallel gw with conditions, which should be ignored when leaving async

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("process")
                .variable("input", 3)
                .start();

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isOne(); // the first task
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        managementService.executeJob(managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list())
                .extracting(TaskInfo::getName)
                .containsOnly("Task A", "Task B");
    }

    @Test
    @Deployment
    public void testBoundaryEventAsyncLeave() {
        // This test has a parallel gw with conditions, which should be ignored when leaving async

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("testBoundaryAsyncLeave")
                .start();

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isOne();

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        Job executableJob = managementService.moveTimerToExecutableJob(timerJob.getId());
        managementService.executeJob(executableJob.getId());

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isZero();

        managementService.executeJob(managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list())
                .extracting(TaskInfo::getName)
                .containsOnly("C");
    }

    @Test
    @Deployment
    public void testExecutionListenersAsyncLeave() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("process")
                .start();

        String processInstanceId = processInstance.getId();
        assertThat(runtimeService.getVariables(processInstanceId)).isEmpty();
        assertThat(taskService.createTaskQuery().processInstanceId(processInstanceId).count()).isZero();

        // async="true"
        managementService.executeJob(managementService.createJobQuery().processInstanceId(processInstanceId).singleResult().getId());
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult().getId());

        assertThat(runtimeService.getVariables(processInstanceId)).containsOnly(entry("start", true));

        // asyncLeave="true"
        Job job = managementService.createJobQuery().processInstanceId(processInstanceId).singleResult();
        assertThat(job.getJobHandlerType()).isEqualTo(AsyncLeaveJobHandler.TYPE);
        managementService.executeJob(job.getId());

        assertThat(runtimeService.getVariables(processInstanceId)).containsOnly(
                entry("start", true),
                entry("end", true)
        );

    }

    @Test
    @Deployment
    public void testEndWithAsyncLeave() {
        String processInstanceId = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("process")
                .start()
                .getId();

        assertThat(runtimeService.getVariables(processInstanceId)).isEmpty();
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).count()).isOne();

        // async="true"
        managementService.executeJob(managementService.createJobQuery().processInstanceId(processInstanceId).singleResult().getId());
        assertThat(runtimeService.getVariables(processInstanceId)).containsOnly(entry("start", true));

        // asyncLeave="true"
        Job job = managementService.createJobQuery().processInstanceId(processInstanceId).singleResult();
        assertThat(job.getJobHandlerType()).isEqualTo(AsyncLeaveJobHandler.TYPE);
        managementService.executeJob(job.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceId).list())
                    .extracting(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue)
                    .containsOnly(
                        tuple("start", true),
                        tuple("end", true)
                    );
        }


        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).count()).isZero();
    }

    @Test
    @Deployment
    public void testSequentialMultiInstanceWithAsyncLeave() {
        String processInstanceId = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("process")
                .variable("counter", 0)
                .start()
                .getId();

        // The async leave is for the whole activity, so not each individually.

        assertThat(runtimeService.getVariable(processInstanceId, "counter")).isEqualTo(0);
        for (int i = 0; i < 5; i++) {
            managementService.executeJob(managementService.createJobQuery().processInstanceId(processInstanceId).singleResult().getId());
            assertThat(((Number)runtimeService.getVariable(processInstanceId, "counter")).intValue()).isEqualTo(i + 1);
        }

        assertThat(taskService.createTaskQuery().processInstanceId(processInstanceId).count()).isZero();

        managementService.executeJob(managementService.createJobQuery().processInstanceId(processInstanceId).singleResult().getId());
        assertThat(taskService.createTaskQuery().processInstanceId(processInstanceId).count()).isOne();
    }

    @Test
    @Deployment
    public void testParallelMultiInstanceWithAsyncLeave() {
        String processInstanceId = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("process")
                .variable("counter", 0)
                .start()
                .getId();

        // The async leave is for the whole activity, so not each individually.

        assertThat(runtimeService.getVariable(processInstanceId, "counter")).isEqualTo(0);

        List<Job> jobs = managementService.createJobQuery().processInstanceId(processInstanceId).list();
        assertThat(jobs).hasSize(5);

        for (int i = 0; i < 5; i++) {
            managementService.executeJob(jobs.get(i).getId());
            assertThat(((Number)runtimeService.getVariable(processInstanceId, "counter")).intValue()).isEqualTo(i + 1);

            // Parallel MI always uses an exclusive async job to leave
            if (i < 4) { // last one will leave synchronously and delete the executions
                assertThat(managementService.createJobQuery().handlerType(ParallelMultiInstanceActivityCompletionJobHandler.TYPE).count()).isEqualTo(i + 1);
            }
        }

        assertThat(managementService.createJobQuery().handlerType(ParallelMultiInstanceActivityCompletionJobHandler.TYPE).count()).isZero();

        managementService.executeJob(managementService.createJobQuery().processInstanceId(processInstanceId).singleResult().getId());
        assertThat(taskService.createTaskQuery().processInstanceId(processInstanceId).count()).isOne();
    }

    @Test
    @Deployment
    public void testParallelMultiInstanceAsyncNoWaitStatesWithAsyncLeave() {
        String processInstanceId = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("process")
                .variable("counter", 0)
                .start()
                .getId();

        // Contrary to testParallelMultiInstanceWithAsyncLeave,
        // there won't be a job for each leave, but one overarching job that will complete the MI activity

        assertThat(runtimeService.getVariable(processInstanceId, "counter")).isEqualTo(0);

        assertThat(managementService.createJobQuery().processInstanceId(processInstanceId).list()).hasSize(6); // 5 async executions, 1 for the no wait states completion
        assertThat(managementService.createJobQuery().handlerType(AsyncContinuationJobHandler.TYPE).count()).isEqualTo(5);
        assertThat(managementService.createJobQuery().handlerType(ParallelMultiInstanceWithNoWaitStatesAsyncLeaveJobHandler.TYPE).count()).isOne();

        List<Job> jobs = managementService.createJobQuery().handlerType(AsyncContinuationJobHandler.TYPE).list();
        for (int i = 0; i < 5; i++) {
            managementService.executeJob(jobs.get(i).getId());
            assertThat(((Number)runtimeService.getVariable(processInstanceId, "counter")).intValue()).isEqualTo(i + 1);

            // Parallel MI always uses an exclusive async job to leave
            if (i < 4) { // last one will leave synchronously and delete the executions
                assertThat(managementService.createJobQuery().handlerType(ParallelMultiInstanceActivityCompletionJobHandler.TYPE).count()).isZero();
                assertThat(managementService.createJobQuery().handlerType(ParallelMultiInstanceWithNoWaitStatesAsyncLeaveJobHandler.TYPE).count()).isOne();
            }
        }

        // Al async service tasks have been executed, the completion of the multi instance is the only job available
        assertThat(managementService.createJobQuery().handlerType(ParallelMultiInstanceActivityCompletionJobHandler.TYPE).count()).isZero();
        assertThat(managementService.createJobQuery().handlerType(ParallelMultiInstanceWithNoWaitStatesAsyncLeaveJobHandler.TYPE).count()).isOne();

        managementService.executeJob(managementService.createJobQuery().processInstanceId(processInstanceId).singleResult().getId());

        // After executing the multi instance completion, the async leave job is available
        assertThat(managementService.createJobQuery().processInstanceId(processInstanceId).list()).hasSize(1);
        assertThat(managementService.createJobQuery().handlerType(AsyncLeaveJobHandler.TYPE).count()).isOne();

        // Executing the async leave job now moves the instance to the task
        managementService.executeJob(managementService.createJobQuery().processInstanceId(processInstanceId).singleResult().getId());

        assertThat(taskService.createTaskQuery().processInstanceId(processInstanceId).count()).isOne();
    }

}
