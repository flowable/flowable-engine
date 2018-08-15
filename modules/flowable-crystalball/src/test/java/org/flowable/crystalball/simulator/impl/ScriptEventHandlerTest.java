package org.flowable.crystalball.simulator.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * This class tests ScriptEventHandler
 *
 * @author martin.grofcik
 */
public class ScriptEventHandlerTest extends ResourceFlowableTestCase {

    public ScriptEventHandlerTest() {
        super("org/flowable/crystalball/simulator/impl/ScriptEventHandlerTest.cfg.xml");
    }

    @Test
    @Deployment
    public void testSimpleScriptExecution() throws Exception {
        ProcessInstance simulationExperiment = runtimeService.startProcessInstanceByKey("resultVariableSimulationRun");
        // all simulationManager executions are finished
        assertEquals(2, runtimeService.createExecutionQuery().count());

        String simulationRunResult = (String) runtimeService.getVariable(simulationExperiment.getProcessInstanceId(), "simulationRunResult");
        // simulation run check - process variable has to be set to the value.
        assertThat(simulationRunResult, is("Hello world!"));

        // process end
        runtimeService.trigger(runtimeService.createExecutionQuery().processInstanceId(simulationExperiment.getId())
                .onlyChildExecutions().singleResult().getId());
        // no process instance is running
        assertEquals(0, runtimeService.createExecutionQuery().count());
    }

}
