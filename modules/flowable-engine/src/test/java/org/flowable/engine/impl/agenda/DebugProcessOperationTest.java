package org.flowable.engine.impl.agenda;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.runtime.Job;
import org.flowable.engine.runtime.ProcessDebugger;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;

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

    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    public void testDebugOneTaskProcess() {
        ProcessInstance oneTaskProcess = this.runtimeService.startProcessInstanceByKey("oneTaskProcess");

        assertProcessActivityId("The execution must stop on the start node.", oneTaskProcess, "theStart");
        triggerBreakPoint();

        assertProcessActivityId("The execution must stop on the user task node before it's execution.", oneTaskProcess, "theTask");
        triggerBreakPoint();

        assertEquals("User task has to be created.", 1, this.taskService.createTaskQuery().count());
        assertProcessActivityId("The execution is still on the user task.", oneTaskProcess, "theTask");
        String taskId = this.taskService.createTaskQuery().processInstanceId(oneTaskProcess.getProcessInstanceId()).singleResult().getId();
        this.taskService.complete(taskId);

        assertProcessActivityId("The execution must stop on the end event.", oneTaskProcess, "theEnd");
        triggerBreakPoint();

        assertThat("No process instance is running.", this.runtimeService.createExecutionQuery().count(), is(0L));
    }

    protected void triggerBreakPoint() {
        Job job = managementService.createDeadLetterJobQuery().handlerType("breakpoint").singleResult();
        assertNotNull(job);
        Job deadLetterJob = managementService.moveDeadLetterJobToExecutableJob(job.getId(), 3);
        managementService.executeJob(deadLetterJob.getId());
    }

    protected void assertProcessActivityId(String message, ProcessInstance process, String activityId) {
        assertThat(message, this.runtimeService.createExecutionQuery().parentId(process.getId()).singleResult().getActivityId(),
                is(activityId));
    }

}
