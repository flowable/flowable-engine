package org.flowable.crystalball.simulator.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * This class tests ScriptEventHandler with multi instance simulation run.
 *
 * @author martin.grofcik
 */
public class MultiInstanceScriptEventHandlerTest extends ResourceFlowableTestCase {

    public MultiInstanceScriptEventHandlerTest() {
        super("org/flowable/crystalball/simulator/impl/MultiInstanceScriptEventHandlerTest.cfg.xml");
    }

    @Test
    @Deployment
    public void testSequentialSimulationRun() throws Exception {
        ProcessInstance simulationExperiment = runtimeService.startProcessInstanceByKey("multiInstanceResultVariablesSimulationRun");
        // all simulationManager executions are finished
        assertEquals(2, runtimeService.createExecutionQuery().count());

        // simulation run check - process variables has to be set to the value. "Hello worldX!"
        String simulationRunResult = (String) runtimeService.getVariable(simulationExperiment.getProcessInstanceId(), "simulationRunResult-0");
        assertThat(simulationRunResult, is("Hello world0!"));
        simulationRunResult = (String) runtimeService.getVariable(simulationExperiment.getProcessInstanceId(), "simulationRunResult-1");
        assertThat(simulationRunResult, is("Hello world1!"));
        simulationRunResult = (String) runtimeService.getVariable(simulationExperiment.getProcessInstanceId(), "simulationRunResult-2");
        assertThat(simulationRunResult, is("Hello world2!"));
        simulationRunResult = (String) runtimeService.getVariable(simulationExperiment.getProcessInstanceId(), "simulationRunResult-3");
        assertThat(simulationRunResult, is("Hello world3!"));
        simulationRunResult = (String) runtimeService.getVariable(simulationExperiment.getProcessInstanceId(), "simulationRunResult-4");
        assertThat(simulationRunResult, is("Hello world4!"));

        // process end
        runtimeService.trigger(runtimeService.createExecutionQuery()
                .onlyChildExecutions().singleResult().getId());
        // no process instance is running
        assertEquals(0, runtimeService.createExecutionQuery().count());
    }

}
