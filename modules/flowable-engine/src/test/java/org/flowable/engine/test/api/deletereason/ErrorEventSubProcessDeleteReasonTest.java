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
package org.flowable.engine.test.api.deletereason;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.engine.history.DeleteReason;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * Companion to {@link DeleteReasonTest#testInterruptingBoundaryEvent()}, for the
 * interrupting error event sub-process.
 *
 * An activity terminated by an interrupting start event is recorded with
 * DeleteReason.EVENT_SUBPROCESS_INTERRUPTING, so history can tell it apart from
 * one that completed normally.
 */
public class ErrorEventSubProcessDeleteReasonTest extends PluggableFlowableTestCase {

    /**
     * A forked process: one branch parks on a user task inside a call activity,
     * the other throws a BpmnError from a start execution listener. The
     * interrupting error event sub-process catches it and terminates the whole
     * scope, including the parked branch.
     */
    @Test
    @Deployment(resources = "org/flowable/engine/test/api/deletereason/ErrorEventSubProcessDeleteReasonTest.bpmn20.xml")
    public void testInterruptingErrorEventSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("errorEventSubProcessDeleteReason");

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        // The parked branch's user task was destroyed by the interruption.
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isZero();
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();

        HistoricActivityInstance parkedBranch = historicActivity(processInstance.getId(), "waitingCall");

        // An end time and a called process instance are also what a call
        // activity that ran to completion records, so the delete reason is the
        // only thing separating the two.
        assertThat(parkedBranch.getEndTime()).isNotNull();
        assertThat(parkedBranch.getCalledProcessInstanceId()).isNotNull();

        assertThat(parkedBranch.getDeleteReason())
                .as("delete reason on an activity terminated by the interrupting error event sub-process")
                .isEqualTo(DeleteReason.EVENT_SUBPROCESS_INTERRUPTING + "(errorHandlerStart)");
    }

    /**
     * The same BpmnError caught by an interrupting boundary event instead, which
     * records DeleteReason.BOUNDARY_EVENT_INTERRUPTING. The two paths differ only
     * in which vehicle catches the error.
     */
    @Test
    @Deployment(resources = "org/flowable/engine/test/api/deletereason/ErrorEventSubProcessDeleteReasonTest.boundary.bpmn20.xml")
    public void testInterruptingBoundaryEvent() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("boundaryDeleteReason");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        HistoricActivityInstance subProcess = historicActivity(processInstance.getId(), "theSubProcess");
        assertThat(subProcess.getDeleteReason()).contains(DeleteReason.BOUNDARY_EVENT_INTERRUPTING);
    }

    private HistoricActivityInstance historicActivity(String processInstanceId, String activityId) {
        List<HistoricActivityInstance> instances = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .activityId(activityId)
                .list();
        assertThat(instances).hasSize(1);
        return instances.get(0);
    }

}
