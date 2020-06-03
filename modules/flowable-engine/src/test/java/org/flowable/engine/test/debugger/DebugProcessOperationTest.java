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
package org.flowable.engine.test.debugger;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.engine.impl.agenda.DebugContinueProcessOperation;
import org.flowable.engine.impl.agenda.DebugFlowableEngineAgenda;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.runtime.ProcessDebugger;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.Test;

/**
 * This class tests {@link DebugContinueProcessOperation}, {@link ProcessDebugger} and {@link DebugFlowableEngineAgenda}
 * implementation
 *
 * @author martin.grofcik
 */
public class DebugProcessOperationTest extends ResourceFlowableTestCase {

    public DebugProcessOperationTest() {
        super("/org/flowable/engine/impl/agenda/DebugProcessOperationTest.flowable.cfg.xml");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    public void testDebugOneTaskProcess() {
        ProcessInstance oneTaskProcess = this.runtimeService.startProcessInstanceByKey("oneTaskProcess");

        assertProcessActivityId("The execution must stop on the start node.", oneTaskProcess, "theStart");
        triggerBreakPoint();

        assertProcessActivityId("The execution must stop on the user task node before it's execution.", oneTaskProcess, "theTask");
        triggerBreakPoint();

        assertThat(this.taskService.createTaskQuery().count()).as("User task has to be created.").isEqualTo(1);
        assertProcessActivityId("The execution is still on the user task.", oneTaskProcess, "theTask");
        String taskId = this.taskService.createTaskQuery().processInstanceId(oneTaskProcess.getProcessInstanceId()).singleResult().getId();
        this.taskService.complete(taskId);

        assertProcessActivityId("The execution must stop on the end event.", oneTaskProcess, "theEnd");
        triggerBreakPoint();

        assertThat(this.runtimeService.createExecutionQuery().count()).as("No process instance is running.").isZero();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/impl/agenda/oneFailureScriptTask.bpmn20.xml")
    public void testDebuggerExecutionFailure() {
        ProcessInstance oneTaskProcess = this.runtimeService.startProcessInstanceByKey("oneTaskFailingProcess");

        assertProcessActivityId("The execution must stop on the start node.", oneTaskProcess, "theStart");
        triggerBreakPoint();

        assertProcessActivityId("The execution must stop on the user task node before it's execution.", oneTaskProcess, "theTask");
        Job job = managementService.createSuspendedJobQuery().handlerType("breakpoint").singleResult();
        assertThat(job).isNotNull();
        managementService.moveSuspendedJobToExecutableJob(job.getId());
        JobTestHelper.waitForJobExecutorToProcessAllJobs(this.processEngineConfiguration, this.managementService, 10000, 500);
        Job updatedJob = managementService.createSuspendedJobQuery().handlerType("breakpoint").singleResult();
        assertThat(updatedJob).as("Triggering breakpoint and failure must reassign breakpoint to suspended jobs again").isNotNull();
    }

    protected void triggerBreakPoint() {
        Job job = managementService.createSuspendedJobQuery().handlerType("breakpoint").singleResult();
        assertThat(job).isNotNull();
        Job activatedJob = managementService.moveSuspendedJobToExecutableJob(job.getId());
        managementService.executeJob(activatedJob.getId());
    }

    protected void assertProcessActivityId(String message, ProcessInstance process, String activityId) {
        assertThat(this.runtimeService.createExecutionQuery().parentId(process.getId()).singleResult().getActivityId()).as(message)
                .isEqualTo(activityId);
    }

}
