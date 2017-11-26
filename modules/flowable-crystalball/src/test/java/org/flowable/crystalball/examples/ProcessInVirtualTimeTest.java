package org.flowable.crystalball.examples;

import org.flowable.engine.ProcessEngines;
import org.flowable.engine.event.EventLogEntry;
import org.flowable.engine.impl.event.logger.EventLogger;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;

import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

/**
 * This class shows how process test can behave in virtual time
 */
public class ProcessInVirtualTimeTest extends ResourceFlowableTestCase {

    public static final Date EPOCH_START = new Date(0);
    public static final Date EPOCH_1 = new Date(1000);
    public static final Date EPOCH_2 = new Date(2000);
    protected EventLogger databaseEventLogger;

    public ProcessInVirtualTimeTest() {
        super("org/flowable/crystalball/examples/ProcessInVirtualTimeTest.cfg.xml");
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Database event logger setup
        databaseEventLogger = new EventLogger(processEngineConfiguration.getClock(), processEngineConfiguration.getObjectMapper(), null);
        runtimeService.addEventListener(databaseEventLogger);
    }

    @Override
    protected void tearDown() throws Exception {
        // Cleanup
        for (EventLogEntry eventLogEntry : managementService.getEventLogEntries(null, null)) {
            managementService.deleteEventLogEntry(eventLogEntry.getLogNumber());
        }

        // Database event logger teardown
        runtimeService.removeEventListener(databaseEventLogger);

        super.tearDown();
    }

    @Deployment(resources = {"org/flowable/crystalball/examples/oneTaskProcessWithEscalation.bpmn20.xml"})
    public void testProcessExecutionInVirtualTime() {
        // set time to the EPOCH start and start the process
        this.processEngineConfiguration.getClock().setCurrentTime(EPOCH_START);
        ProcessInstance oneTaskProcess = this.runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess").start();
        Task task = this.taskService.createTaskQuery().processInstanceId(oneTaskProcess.getId()).singleResult();
        assertThat(oneTaskProcess.getStartTime(), is(EPOCH_START));
        assertThat(task.getCreateTime(), is(EPOCH_START));

        // increase the time to 1 sec and complete task
        this.processEngineConfiguration.getClock().setCurrentTime(EPOCH_1);
        this.taskService.claim(task.getId(), "kermit");
        task = this.taskService.createTaskQuery().processInstanceId(oneTaskProcess.getId()).singleResult();
        assertThat(task.getClaimTime(), is(EPOCH_1));

        this.processEngineConfiguration.getClock().setCurrentTime(EPOCH_2);
        this.taskService.complete(task.getId());
        assertThat(getEvent(oneTaskProcess.getId(), "TASK_COMPLETED").getTimeStamp(), is(EPOCH_2));
        assertThat(getEvent(oneTaskProcess.getId(), "PROCESSINSTANCE_END").getTimeStamp(), is(EPOCH_2));

        assertThat("The test has side effect - engine's time is set to the EPOCH_2", this.processEngineConfiguration.getClock().getCurrentTime(), is(EPOCH_2));
    }

    @Deployment(resources = {"org/flowable/crystalball/examples/oneTaskProcessSimulation.bpmn20.xml"})
    public void testProcessExecutionInVirtualTimeInSimulationSubProcess() {
        //@TODO: possible bug: Why Process engines are not initialized when there is already one stored in?
        ProcessEngines.setInitialized(true);
        this.runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcessSimulationTest").start();
        JobTestHelper.waitForJobExecutorToProcessAllJobs(this.processEngineConfiguration, this.managementService, 30000, 500);
        assertThat("The simulation lives in the virtual world. It has no side effect on the real engine.",
                this.processEngineConfiguration.getClock().getCurrentTime(), greaterThan(EPOCH_2));
    }

    @Deployment(resources = {"org/flowable/crystalball/examples/oneTaskProcessSimulationTimers.bpmn20.xml"})
    public void testProcessExecutionWithEscalation() {
        //@TODO: possible bug: Why Process engines are not initialized when there is already one stored in?
        ProcessEngines.setInitialized(true);
        this.runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcessSimulationTest").start();
        JobTestHelper.waitForJobExecutorToProcessAllJobs(this.processEngineConfiguration, this.managementService, 30000, 500);
        assertThat("The simulation lives in the virtual world. It has no side effect on the real engine.",
                this.processEngineConfiguration.getClock().getCurrentTime(), greaterThan(EPOCH_2));
    }


    private EventLogEntry getEvent(String processId, String eventType) {
        List<EventLogEntry> events = this.managementService.getEventLogEntriesByProcessInstanceId(processId);
        for (EventLogEntry event : events) {
            if(eventType.equals(event.getType())) {
                return event;
            }
        }
        throw new RuntimeException("Event with type " +eventType + " not found.");
    }
}
