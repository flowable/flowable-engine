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
 * Companion to DeleteReasonTest#testInterruptingBoundaryEvent.
 *
 * An interrupting error event sub-process terminates the executions in its
 * scope with a null delete reason, so an activity it killed is indistinguishable
 * in history from one that completed normally. Every other interrupting event
 * sub-process start type records DeleteReason.EVENT_SUBPROCESS_INTERRUPTING.
 */
public class ErrorEventSubProcessDeleteReasonTest extends PluggableFlowableTestCase {

    /**
     * A forked process: one branch parks on a user task, the other throws a
     * BpmnError from a start execution listener. The interrupting error event
     * sub-process catches it and terminates the whole scope.
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

        // It has an end time and a called process instance, exactly as a call
        // activity that ran to completion does. By the documented contract of
        // getDeleteReason() -- "if completed normally, no delete reason is set"
        // -- a null reason here says this activity finished. It did not.
        assertThat(parkedBranch.getEndTime()).isNotNull();
        assertThat(parkedBranch.getCalledProcessInstanceId()).isNotNull();

        // FAILS: actual is null.
        assertThat(parkedBranch.getDeleteReason())
                .as("delete reason on an activity terminated by the interrupting error event sub-process")
                .isEqualTo(DeleteReason.EVENT_SUBPROCESS_INTERRUPTING + "(errorHandlerStart)");
    }

    /**
     * The same shape with an interrupting *boundary* event rather than an event
     * sub-process. This one passes: BoundaryEventActivityBehavior records
     * DeleteReason.BOUNDARY_EVENT_INTERRUPTING.
     */
    @Test
    @Deployment(resources = "org/flowable/engine/test/api/deletereason/ErrorEventSubProcessDeleteReasonTest.boundary.bpmn20.xml")
    public void testInterruptingBoundaryEventForComparison() {
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
