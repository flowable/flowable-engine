package org.flowable.crystalball.examples;

import org.flowable.engine.ProcessEngines;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;

/**
 * This test shows the simplest simulation of the timer and escalation
 */
public class EscalationProbabilitySimulationTest extends ResourceFlowableTestCase {
    public EscalationProbabilitySimulationTest() {
        super("org/flowable/crystalball/examples/EscalationProbabilitySimulationTest-realProcessEngine.cfg.xml");
    }

    @Deployment(resources = {"org/flowable/crystalball/examples/EscalationProbability-simulationRun.bpmn20.xml"})
    public void testSimulationRun() {
        ProcessEngines.setInitialized(true);
        this.runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcessSimulationTest").start();
        JobTestHelper.waitForJobExecutorToProcessAllJobs(this.processEngineConfiguration, this.managementService, 30000, 500);
    }

    @Deployment(resources = { "org/flowable/crystalball/examples/EscalationProbability-simulationExperiment.bpmn20.xml",
            "org/flowable/crystalball/examples/EscalationProbability-simulationRun.bpmn20.xml"})
    public void testSimulationExperiment() {
        ProcessEngines.setInitialized(true);
        ProcessInstance simulationExperiment = this.runtimeService.createProcessInstanceBuilder().processDefinitionKey("simulationExperiment").start();
        JobTestHelper.waitForJobExecutorToProcessAllJobs(this.processEngineConfiguration, this.managementService, 300000, 500);
        assertThat("The probability that task is escalated must be greater than 30%",
                runtimeService.getVariable(simulationExperiment.getId(), "escalated", Integer.class), greaterThan(30));
    }

}
