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
package org.flowable.crystalball.simulator.impl;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(2);

        // simulation run check - process variables has to be set to the value. "Hello worldX!"
        String simulationRunResult = (String) runtimeService.getVariable(simulationExperiment.getProcessInstanceId(), "simulationRunResult-0");
        assertThat(simulationRunResult).isEqualTo("Hello world0!");
        simulationRunResult = (String) runtimeService.getVariable(simulationExperiment.getProcessInstanceId(), "simulationRunResult-1");
        assertThat(simulationRunResult).isEqualTo("Hello world1!");
        simulationRunResult = (String) runtimeService.getVariable(simulationExperiment.getProcessInstanceId(), "simulationRunResult-2");
        assertThat(simulationRunResult).isEqualTo("Hello world2!");
        simulationRunResult = (String) runtimeService.getVariable(simulationExperiment.getProcessInstanceId(), "simulationRunResult-3");
        assertThat(simulationRunResult).isEqualTo("Hello world3!");
        simulationRunResult = (String) runtimeService.getVariable(simulationExperiment.getProcessInstanceId(), "simulationRunResult-4");
        assertThat(simulationRunResult).isEqualTo("Hello world4!");

        // process end
        runtimeService.trigger(runtimeService.createExecutionQuery()
                .onlyChildExecutions().singleResult().getId());
        // no process instance is running
        assertThat(runtimeService.createExecutionQuery().count()).isZero();
    }

}
