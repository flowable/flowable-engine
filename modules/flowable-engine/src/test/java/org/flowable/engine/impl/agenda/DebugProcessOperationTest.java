package org.flowable.engine.impl.agenda;

import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.runtime.Job;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
        this.taskService.complete(
                this.taskService.createTaskQuery().processInstanceId(oneTaskProcess.getProcessInstanceId()).singleResult().getId()
        );

        assertProcessActivityId("The execution must stop on the end event.", oneTaskProcess, "theEnd");
        triggerBreakPoint();

        assertThat("No process instance is running.", this.runtimeService.createExecutionQuery().count(), is(0L));
    }

    private void triggerBreakPoint() {
        Job job = managementService.createJobQuery().handlerType("breakPoint").singleResult();
        assertNotNull(job);
        managementService.executeJob(job.getId());
    }

    private void assertProcessActivityId(String message, ProcessInstance process, String activityId) {
        assertThat(message, this.runtimeService.createExecutionQuery().parentId(process.getId()).singleResult().getActivityId(),
                is(activityId));
    }

}
