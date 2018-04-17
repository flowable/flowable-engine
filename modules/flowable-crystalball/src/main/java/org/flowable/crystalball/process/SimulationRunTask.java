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
package org.flowable.crystalball.process;

import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.crystalball.simulator.SimulationRun;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

/**
 * This class implement task which runs simulation experiment
 * 
 * @author martin.grofcik
 */
public class SimulationRunTask implements JavaDelegate {

    private Expression simulationRunExpression;

    @Override
    public void execute(DelegateExecution execution) {
        SimulationRun simulationRun = (SimulationRun) simulationRunExpression.getValue(execution);
        simulationRun.execute(execution);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setSimulationRun(Expression simulationRun) {
        this.simulationRunExpression = simulationRun;
    }
}
