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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HistoricProcessInstanceQueryVersionTest extends PluggableFlowableTestCase {

    private static final String PROCESS_DEFINITION_KEY = "oneTaskProcess";
    private static final String DEPLOYMENT_FILE_PATH = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml";

    @BeforeEach
    protected void setUp() throws Exception {
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

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
    }

    @AfterEach
    protected void tearDown() throws Exception {
        deleteDeployments();
    }

    @Test
    public void testHistoricProcessInstanceQueryByProcessDefinitionVersion() {
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(1).list().get(0).getProcessDefinitionVersion().intValue())
                .isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(2).list().get(0).getProcessDefinitionVersion().intValue())
                .isEqualTo(2);
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(1).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(2).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(3).count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(1).count()).isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(2).list()).hasSize(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionVersion(3).list()).isEmpty();

        // Variables Case
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .variableValueEquals("test", 123).processDefinitionVersion(1).singleResult();
            assertThat(processInstance.getProcessDefinitionVersion().intValue()).isEqualTo(1);
            Map<String, Object> variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsOnly(entry("test", 123));

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .variableValueEquals("anothertest", 456).processDefinitionVersion(1).singleResult();
            assertThat(processInstance).isNull();

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .variableValueEquals("anothertest", 456).processDefinitionVersion(2).singleResult();
            assertThat(processInstance.getProcessDefinitionVersion().intValue()).isEqualTo(2);
            variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsOnly(entry("anothertest", 456));
        }
    }

    @Test
    public void testHistoricProcessInstanceQueryByProcessDefinitionVersionAndKey() {
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(1).count())
                .isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(2).count())
                .isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("undefined").processDefinitionVersion(1).count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("undefined").processDefinitionVersion(2).count()).isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(1).list())
                .hasSize(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).processDefinitionVersion(2).list())
                .hasSize(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("undefined").processDefinitionVersion(1).list()).isEmpty();
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("undefined").processDefinitionVersion(2).list()).isEmpty();
    }

    @Test
    public void testHistoricProcessInstanceOrQueryByProcessDefinitionVersion() {
        assertThat(historyService.createHistoricProcessInstanceQuery().or().processDefinitionVersion(1).processDefinitionId("undefined").endOr().count())
                .isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().or().processDefinitionVersion(2).processDefinitionId("undefined").endOr().count())
                .isEqualTo(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().or().processDefinitionVersion(3).processDefinitionId("undefined").endOr().count())
                .isZero();
        assertThat(historyService.createHistoricProcessInstanceQuery().or().processDefinitionVersion(1).processDefinitionId("undefined").endOr().list())
                .hasSize(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().or().processDefinitionVersion(2).processDefinitionId("undefined").endOr().list())
                .hasSize(1);
        assertThat(historyService.createHistoricProcessInstanceQuery().or().processDefinitionVersion(3).processDefinitionId("undefined").endOr().list())
                .isEmpty();

        // Variables Case
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .or().variableValueEquals("test", "invalid").processDefinitionVersion(1).endOr().singleResult();
            assertThat(processInstance.getProcessDefinitionVersion().intValue()).isEqualTo(1);
            Map<String, Object> variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsOnly(entry("test", 123));

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .or().variableValueEquals("anothertest", "invalid").processDefinitionVersion(2).endOr().singleResult();
            assertThat(processInstance.getProcessDefinitionVersion().intValue()).isEqualTo(2);
            variableMap = processInstance.getProcessVariables();
            assertThat(variableMap)
                    .containsOnly(entry("anothertest", 456));

            processInstance = historyService.createHistoricProcessInstanceQuery().includeProcessVariables()
                    .variableValueEquals("anothertest", "invalid").processDefinitionVersion(3).singleResult();
            assertThat(processInstance).isNull();
        }
    }
}
