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
package org.flowable.crystalball.examples.tutorial.step01;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * This class provides the first insight into simulation run driven by process definition
 *
 * @author martin.grofcik
 */
public class FirstSimulationRunTest extends ResourceFlowableTestCase {

    public FirstSimulationRunTest() {
        super("org/flowable/crystalball/examples/tutorial/step01/FirstSimulationRunTest.cfg.xml");
    }

    @Test
    @Deployment
    public void testSimulationRun() {
        runtimeService.startProcessInstanceByKey("basicSimulationRun");
        // all simulationManager executions are finished
        assertThat(runtimeService.createExecutionQuery().count()).isZero();

        // simulation run check (Simulation run has side effect. The counter value is increased)
        assertThat(Counter.value.get()).isEqualTo(1);
    }

}
