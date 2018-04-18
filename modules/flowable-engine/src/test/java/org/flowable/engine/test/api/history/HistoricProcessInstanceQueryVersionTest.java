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
package org.flowable.engine.test.api.history;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;

public class HistoricProcessInstanceQueryVersionTest extends PluggableFlowableTestCase {

    private static final String PROCESS_DEFINITION_KEY = "oneTaskProcess";
    private static final String DEPLOYMENT_FILE_PATH = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        repositoryService.createDeployment()
                .addClasspathResource(DEPLOYMENT_FILE_PATH)
                .deploy();

        Map<String, Object> startMap = new HashMap<>();
        startMap.put("test", 123);
        runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY, startMap);

        repositoryService.createDeployment()
                .addClasspathResource(DEPLOYMENT_FILE_PATH)
                .deploy();

        startMap.clear();
        startMap.put("anothertest", 456);
        runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY, startMap);
        
        waitForHistoryJobExecutorToProcessAllJobs(5000, 100);
    }

    @Override
    protected void tearDown() throws Exception {
        deleteDeployments();
    }

    public void testHistoricProcessInstanceQueryByProcessDefinitionVersion() {
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(1).list().get(0).getProcessDefinitionVersion().intValue());
        assertEquals(2, historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(2).list().get(0).getProcessDefinitionVersion().intValue());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(1).count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(2).count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(3).count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(1).count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(2).list().size());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(3).list().size());

        // Variables Case
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .variableValueEquals("test", 123).processDefinitionVersion(1).singleResult();
            assertEquals(1, processInstance.getProcessDefinitionVersion().intValue());
            Map<String, Object> variableMap = processInstance.getProcessVariables();
            assertEquals(123, variableMap.get("test"));

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .variableValueEquals("anothertest", 456).processDefinitionVersion(1).singleResult();
            assertNull(processInstance);

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .variableValueEquals("anothertest", 456).processDefinitionVersion(2).singleResult();
            assertEquals(2, processInstance.getProcessDefinitionVersion().intValue());
            variableMap = processInstance.getProcessVariables();
            assertEquals(456, variableMap.get("anothertest"));
        }
    }

    public void testHistoricProcessInstanceQueryByProcessDefinitionVersionAndKey() {
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(1).count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(2).count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().processDefinitionKey("undefined").processDefinitionVersion(1).count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().processDefinitionKey("undefined").processDefinitionVersion(2).count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(1).list().size());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(2).list().size());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().processDefinitionKey("undefined").processDefinitionVersion(1).list().size());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().processDefinitionKey("undefined").processDefinitionVersion(2).list().size());
    }

    public void testHistoricProcessInstanceOrQueryByProcessDefinitionVersion() {
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().processDefinitionVersion(1).processDefinitionId("undefined").endOr().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().processDefinitionVersion(2).processDefinitionId("undefined").endOr().count());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().or().processDefinitionVersion(3).processDefinitionId("undefined").endOr().count());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().processDefinitionVersion(1).processDefinitionId("undefined").endOr().list().size());
        assertEquals(1, historyService.createHistoricProcessInstanceQuery().or().processDefinitionVersion(2).processDefinitionId("undefined").endOr().list().size());
        assertEquals(0, historyService.createHistoricProcessInstanceQuery().or().processDefinitionVersion(3).processDefinitionId("undefined").endOr().list().size());

        // Variables Case
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .or().variableValueEquals("test", "invalid").processDefinitionVersion(1).endOr().singleResult();
            assertEquals(1, processInstance.getProcessDefinitionVersion().intValue());
            Map<String, Object> variableMap = processInstance.getProcessVariables();
            assertEquals(123, variableMap.get("test"));

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .or().variableValueEquals("anothertest", "invalid").processDefinitionVersion(2).endOr().singleResult();
            assertEquals(2, processInstance.getProcessDefinitionVersion().intValue());
            variableMap = processInstance.getProcessVariables();
            assertEquals(456, variableMap.get("anothertest"));

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .variableValueEquals("anothertest", "invalid").processDefinitionVersion(3).singleResult();
            assertNull(processInstance);
        }
    }
}
