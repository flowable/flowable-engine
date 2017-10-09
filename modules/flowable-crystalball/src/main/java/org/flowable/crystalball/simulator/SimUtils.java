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
package org.flowable.crystalball.simulator;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngines;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.Execution;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static org.flowable.engine.impl.bpmn.behavior.SimulationSubProcessActivityBehavior.VIRTUAL_PROCESS_ENGINE_VARIABLE_NAME;

/**
 * Simulation utils used in the simulation run and scenario. e.g. random number generator should be centralized for all sim runs.
 *
 * @author martin.grofcik
 */

public class SimUtils {
    /**
     * main random number generator
     */
    private static volatile ThreadLocal<Random> randomGenerator = new ThreadLocal<>();

    public static void setSeed(long seed) {
        randomGenerator.set(new Random(seed));
    }

    public static int getRandomInt(int max) {
        if (randomGenerator.get() == null) {
            randomGenerator.set(new Random());
        }
        return randomGenerator.get().nextInt(max);
    }

    @SuppressWarnings("unused")
    public static List<String> getSubProcessInstanceIds(String rootProcessInstanceId, DelegateExecution execution) {
        List<Execution> processInstances = getVirtualProcessEngine(execution)
                .getRuntimeService().createExecutionQuery().rootProcessInstanceId(rootProcessInstanceId).onlyProcessInstanceExecutions().list();

        List<String> processInstanceIds = new ArrayList<>(processInstances.size());
        for (Execution processInstance : processInstances) {
            processInstanceIds.add(processInstance.getId());
        }
        return processInstanceIds;
    }

    public static ProcessEngine getVirtualProcessEngine(DelegateExecution execution) {
        return ProcessEngines.getProcessEngine((String) execution.getVariable(VIRTUAL_PROCESS_ENGINE_VARIABLE_NAME));
    }

    @SuppressWarnings("unused")
    public String getKeyFromProcessDefinitionId(String processDefinitionId, DelegateExecution execution) {
        return getVirtualProcessEngine(execution).getRepositoryService().
                createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult().getKey();
    }

    @SuppressWarnings("unused")
    public static void setVirtualTime(DelegateExecution execution, Date time) {
        getVirtualProcessEngine(execution).getProcessEngineConfiguration().getClock().setCurrentTime(time);
    }


}
