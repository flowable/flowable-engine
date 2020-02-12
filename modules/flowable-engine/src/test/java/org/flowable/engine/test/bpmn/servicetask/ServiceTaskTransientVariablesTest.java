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

package org.flowable.engine.test.bpmn.servicetask;

import java.util.List;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.Test;

/**
 * 
 * @author Matthias Stöckli
 */
public class ServiceTaskTransientVariablesTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testStoreTransientVariable() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

        Object transientResult = runtimeService.getVariable(processInstance.getId(), "transientResult");
        Object persistentResult = runtimeService.getVariable(processInstance.getId(), "persistentResult");

        assertNull(transientResult);
        assertEquals(persistentResult, "Result is: test");
    }

    @Test
    @Deployment
    public void testStoreLocalTransientVariable() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

        List<HistoricVariableInstance> variablesInstances = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(variablesInstances.size(), 1);
        assertEquals(variablesInstances.get(0).getValue(), "Result is: test");
    }



}
