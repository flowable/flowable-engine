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

package org.flowable.engine.test.bpmn.multiinstance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.jobexecutor.ParallelMultiInstanceWithNoWaitStatesAsyncLeaveJobHandler;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 * @author Joram Barrez
 */
public class MultiInstanceNoWaitStatesTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testParallelAsyncAndExclusiveServiceTasks() {
        int count = 25; // This can be increased to e.g. 2500 to test this with lots of service tasks. The QA system however, doesn't like this.
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("miParallelAsyncScriptTask")
                .variable("nrOfLoops", count)
                .start();
        List<Job> jobs = managementService.createJobQuery().list();
        assertThat(jobs).hasSize(count + 1); // 1 for each async service task + 1 for the job that does the leave

        waitForJobExecutorToProcessAllJobsAndAllTimerJobs(Duration.ofMinutes(5).toMillis(), 200);
        jobs = managementService.createJobQuery().list();
        assertThat(jobs).isEmpty();
        List<Job> timerJobs = managementService.createTimerJobQuery().list();
        assertThat(timerJobs).isEmpty();

        List<Job> deadLetterJobs = managementService.createDeadLetterJobQuery().list();
        assertThat(deadLetterJobs).isEmpty();

        List<Execution> executions = runtimeService.createExecutionQuery().list();
        assertThat(executions).hasSize(2);
        Execution processInstanceExecution = null;
        Execution waitStateExecution = null;
        for (Execution execution : executions) {
            if (execution.getId().equals(execution.getProcessInstanceId())) {
                processInstanceExecution = execution;
            } else {
                waitStateExecution = execution;
            }
        }
        assertThat(processInstanceExecution).isNotNull();
        assertThat(waitStateExecution).isNotNull();

        Map<String, VariableInstance> variableInstances = runtimeService.getVariableInstances(processInstanceExecution.getProcessInstanceId());
        assertThat(variableInstances).containsOnlyKeys("nrOfLoops");
        VariableInstance nrOfLoops = variableInstances.get("nrOfLoops");
        assertThat(nrOfLoops.getValue()).isEqualTo(count);

    }

    @Test
    @Deployment
    public void testParallelAsyncAndExclusiveServiceTasksWithBoundaryEvent() {

        // Boundary events add another execution as a child of the multi-instance.
        // This tests whether the logic takes that into account

        int count = 9;
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", count)
            .start();
        List<Job> jobs = managementService.createJobQuery().list();
        assertThat(jobs).hasSize(count + 1); // 1 for each async service task + 1 for the job that does the leave

        waitForJobExecutorToProcessAllJobs(Duration.ofMinutes(5).toMillis(), 200);
        assertNoJobsAndNoProcessInstances();
    }

//    @Test
//    @Deployment
//    public void testParallelAsyncAndExclusiveServiceTasksWithBoundaryEventFiring() {
//        ThrowErrorBoundaryDelegate.INSTANCES_BEFORE_THROW = 2;
//
//        int count = 5;
//        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
//            .processDefinitionKey("myProcess")
//            .variable("nrOfLoops", count)
//            .start();
//        List<Job> jobs = managementService.createJobQuery().list();
//        assertThat(jobs).hasSize(count + 1); // 1 for each async service task + 1 for the job that does the leave
//
//        waitForJobExecutorToProcessAllJobs(Duration.ofMinutes(5).toMillis(), 200);
//
//        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
//        assertThat(task.getId()).isEqualTo("escalate");
//        taskService.complete(task.getId());
//
//        assertNoJobsAndNoProcessInstances();
//    }

    @Test
    @Deployment
    public void testParallelAsyncAndExclusiveSubProcess() {
        int count = 10;
        runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", count)
            .start();
        List<Job> jobs = managementService.createJobQuery().list();
        assertThat(jobs).hasSize(count + 1);

        assertThat(jobs.stream()
            .filter(job -> job.getJobHandlerType().equals(ParallelMultiInstanceWithNoWaitStatesAsyncLeaveJobHandler.TYPE)).collect(Collectors.toList()))
        .hasSize(1);

        assertThat(jobs.stream()
            .filter(job -> !job.getJobHandlerType().equals(ParallelMultiInstanceWithNoWaitStatesAsyncLeaveJobHandler.TYPE)).collect(Collectors.toList()))
            .hasSize(count);

        waitForJobExecutorToProcessAllJobs(Duration.ofMinutes(5).toMillis(), 200);
        assertNoJobsAndNoProcessInstances();
    }



    @Test
    @Deployment
    public void testNestedParallelAsyncAndExclusiveSubProcess() {

        // Nested subprocesses, both with only wait states flag set.
        // Technically, this use doesn't make a lot of sense: it would be better if only making the root subprocess have the 'noWaitState' flag.
        // This test however validates that things don't break.

        int count = 3;
        runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", count)
            .start();
        List<Job> jobs = managementService.createJobQuery().list();
        assertThat(jobs).hasSize(count + 1);

        waitForJobExecutorToProcessAllJobs(Duration.ofMinutes(5).toMillis(), 200);
        assertNoJobsAndNoProcessInstances();
    }

    @Test
    @Deployment(extraResources = "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceNoWaitStatesTest.allServiceTasksProcess.bpmn20.xml")
    public void testNestedParallelAsyncAndExclusiveCallActivity() {
        int count = 4;
        runtimeService.createProcessInstanceBuilder()
            .processDefinitionKey("myProcess")
            .variable("nrOfLoops", count)
            .start();

        List<Job> jobs = managementService.createJobQuery().list();
        assertThat(jobs).hasSize(count + 1);

        waitForJobExecutorToProcessAllJobs(Duration.ofMinutes(5).toMillis(), 200);
        assertNoJobsAndNoProcessInstances();
    }

    protected void assertNoJobsAndNoProcessInstances() {
        assertThat(managementService.createJobQuery().count()).isEqualTo(0);
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(0);
        assertThat(managementService.createDeadLetterJobQuery().count()).isEqualTo(0);

        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("myProcess").count()).isZero();
    }

    public static class ThrowErrorBoundaryDelegate implements JavaDelegate {

        public static int INSTANCES_BEFORE_THROW = 0;

        public static AtomicInteger COUNTER = new AtomicInteger(0);

        @Override
        public void execute(DelegateExecution execution) {
            int count = COUNTER.incrementAndGet();
            if (count == INSTANCES_BEFORE_THROW) {
                throw new BpmnError("123");
            }
        }
    }

}
