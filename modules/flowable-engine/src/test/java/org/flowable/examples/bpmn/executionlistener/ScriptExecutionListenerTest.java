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
package org.flowable.examples.bpmn.executionlistener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class ScriptExecutionListenerTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/examples/bpmn/executionlistener/ScriptExecutionListenerTest.bpmn20.xml" })
    public void testScriptExecutionListener() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("scriptExecutionListenerProcess");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricVariableInstance> historicVariables = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).list();
            Map<String, Object> varMap = new HashMap<>();
            for (HistoricVariableInstance historicVariableInstance : historicVariables) {
                varMap.put(historicVariableInstance.getVariableName(), historicVariableInstance.getValue());
            }

            assertTrue(varMap.containsKey("foo"));
            assertEquals("FOO", varMap.get("foo"));
            assertTrue(varMap.containsKey("var1"));
            assertEquals("test", varMap.get("var1"));
            assertFalse(varMap.containsKey("bar"));
            assertTrue(varMap.containsKey("myVar"));
            assertEquals("BAR", varMap.get("myVar"));
        }
    }

}
