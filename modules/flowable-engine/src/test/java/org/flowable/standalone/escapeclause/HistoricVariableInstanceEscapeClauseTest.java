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
package org.flowable.standalone.escapeclause;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HistoricVariableInstanceEscapeClauseTest extends AbstractEscapeClauseTestCase {

    private String deploymentOneId;

    private String deploymentTwoId;

    private ProcessInstance processInstance1;

    private ProcessInstance processInstance2;

    @BeforeEach
    protected void setUp() throws Exception {
        deploymentOneId = repositoryService
                .createDeployment()
                .tenantId("One%")
                .addClasspathResource("org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
                .deploy()
                .getId();

        deploymentTwoId = repositoryService
                .createDeployment()
                .tenantId("Two_")
                .addClasspathResource("org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
                .deploy()
                .getId();

        Map<String, Object> vars = new HashMap<>();
        vars.put("var%", "One%");
        processInstance1 = runtimeService.startProcessInstanceByKeyAndTenantId("oneTaskProcess", vars, "One%");
        runtimeService.setProcessInstanceName(processInstance1.getId(), "One%");

        vars = new HashMap<>();
        vars.put("var_", "Two_");
        processInstance2 = runtimeService.startProcessInstanceByKeyAndTenantId("oneTaskProcess", vars, "Two_");
        runtimeService.setProcessInstanceName(processInstance2.getId(), "Two_");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        taskService.complete(task.getId());

    }

    @AfterEach
    protected void tearDown() throws Exception {
        repositoryService.deleteDeployment(deploymentOneId, true);
        repositoryService.deleteDeployment(deploymentTwoId, true);
    }

    @Test
    public void testQueryByVariableNameLike() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicVariable = historyService.createHistoricVariableInstanceQuery().variableNameLike("%|%%").singleResult();
            assertThat(historicVariable).isNotNull();
            assertThat(historicVariable.getProcessInstanceId()).isEqualTo(processInstance1.getId());
            assertThat(historicVariable.getValue()).isEqualTo("One%");

            historicVariable = historyService.createHistoricVariableInstanceQuery().variableNameLike("%|_%").singleResult();
            assertThat(historicVariable).isNotNull();
            assertThat(historicVariable.getProcessInstanceId()).isEqualTo(processInstance2.getId());
            assertThat(historicVariable.getValue()).isEqualTo("Two_");
        }
    }

    @Test
    public void testQueryLikeByQueryVariableValue() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicVariable = historyService.createHistoricVariableInstanceQuery().variableValueLike("var%", "%|%%").singleResult();
            assertThat(historicVariable).isNotNull();
            assertThat(historicVariable.getProcessInstanceId()).isEqualTo(processInstance1.getId());

            historicVariable = historyService.createHistoricVariableInstanceQuery().variableValueLike("var_", "%|_%").singleResult();
            assertThat(historicVariable).isNotNull();
            assertThat(historicVariable.getProcessInstanceId()).isEqualTo(processInstance2.getId());
        }
    }

    @Test
    public void testQueryLikeByQueryVariableValueIgnoreCase() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricVariableInstance historicVariable = historyService.createHistoricVariableInstanceQuery().variableValueLikeIgnoreCase("var%", "%|%%")
                    .singleResult();
            assertThat(historicVariable).isNotNull();
            assertThat(historicVariable.getProcessInstanceId()).isEqualTo(processInstance1.getId());

            historicVariable = historyService.createHistoricVariableInstanceQuery().variableValueLikeIgnoreCase("var_", "%|_%").singleResult();
            assertThat(historicVariable).isNotNull();
            assertThat(historicVariable.getProcessInstanceId()).isEqualTo(processInstance2.getId());
        }
    }
}
