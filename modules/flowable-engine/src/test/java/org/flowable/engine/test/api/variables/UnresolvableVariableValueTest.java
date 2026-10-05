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
package org.flowable.engine.test.api.variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.nio.charset.StandardCharsets;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayEntity;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.api.variables.SerializableVariableTest.TestSerializableVariable;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.util.VariableValueUtil;
import org.junit.jupiter.api.Test;

/**
 * A variable whose value cannot be resolved is returned with a null value by the variable queries and the queries that include variables,
 * instead of failing them. The variable is marked as having an unresolvable value. Getting the variables through the services still fails.
 */
class UnresolvableVariableValueTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void variableInstanceQueryReturnsVariableWithUnresolvableValue() {
        ProcessInstance processInstance = startProcessWithUnresolvableOrderVariable();

        assertThat(runtimeService.createVariableInstanceQuery().processInstanceId(processInstance.getId()).list())
                .extracting(VariableInstance::getName, VariableInstance::getValue, VariableValueUtil::isValueUnresolvable)
                .containsExactlyInAnyOrder(
                        tuple("customer", "Kermit", false),
                        tuple("order", null, true)
                );
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void getVariablesFailsForVariableWithUnresolvableValue() {
        ProcessInstance processInstance = startProcessWithUnresolvableOrderVariable();

        assertThatThrownBy(() -> runtimeService.getVariables(processInstance.getId())).isInstanceOf(FlowableException.class);
        assertThatThrownBy(() -> runtimeService.getVariablesLocal(processInstance.getId())).isInstanceOf(FlowableException.class);
        assertThatThrownBy(() -> runtimeService.getVariable(processInstance.getId(), "order")).isInstanceOf(FlowableException.class);
        assertThat(runtimeService.getVariable(processInstance.getId(), "customer")).isEqualTo("Kermit");

        // The variable instances can be fetched, the value of the variable is only resolved when it is read
        assertThat(runtimeService.getVariableInstances(processInstance.getId())).containsOnlyKeys("customer", "order");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void getTaskVariablesFailsForVariableWithUnresolvableValue() {
        ProcessInstance processInstance = startProcessWithUnresolvableOrderVariable();
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        assertThatThrownBy(() -> taskService.getVariables(task.getId())).isInstanceOf(FlowableException.class);
        assertThatThrownBy(() -> taskService.getVariable(task.getId(), "order")).isInstanceOf(FlowableException.class);
        assertThat(taskService.getVariable(task.getId(), "customer")).isEqualTo("Kermit");
        assertThat(taskService.getVariableInstances(task.getId())).containsOnlyKeys("customer", "order");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void historicVariableInstanceQueryReturnsVariableWithUnresolvableValue() {
        if (!HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            return;
        }

        ProcessInstance processInstance = startProcessWithUnresolvableOrderVariable();

        assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).list())
                .extracting(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue, VariableValueUtil::isValueUnresolvable)
                .containsExactlyInAnyOrder(
                        tuple("customer", "Kermit", false),
                        tuple("order", null, true)
                );
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void queriesIncludingVariablesReturnVariableWithUnresolvableValue() {
        ProcessInstance processInstance = startProcessWithUnresolvableOrderVariable();

        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).includeProcessVariables().singleResult()
                .getProcessVariables())
                .containsOnly(entry("customer", "Kermit"), entry("order", null));

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).includeProcessVariables().singleResult()
                .getProcessVariables())
                .containsOnly(entry("customer", "Kermit"), entry("order", null));

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).includeProcessVariables()
                    .singleResult().getProcessVariables())
                    .containsOnly(entry("customer", "Kermit"), entry("order", null));
        }
    }

    protected ProcessInstance startProcessWithUnresolvableOrderVariable() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .variable("customer", "Kermit")
                .variable("order", new TestSerializableVariable(1))
                .start();

        corruptSerializedValue(processInstance.getId(), "order");
        return processInstance;
    }

    protected void corruptSerializedValue(String processInstanceId, String variableName) {
        managementService.executeCommand(commandContext -> {
            VariableInstanceEntity variableInstance = (VariableInstanceEntity) runtimeService.createVariableInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .variableName(variableName)
                    .excludeVariableInitialization()
                    .singleResult();
            corruptByteArray(variableInstance.getByteArrayRef().getId());

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                HistoricVariableInstanceEntity historicVariableInstance = (HistoricVariableInstanceEntity) historyService
                        .createHistoricVariableInstanceQuery()
                        .processInstanceId(processInstanceId)
                        .variableName(variableName)
                        .excludeVariableInitialization()
                        .singleResult();
                corruptByteArray(historicVariableInstance.getByteArrayRef().getId());
            }
            return null;
        });
    }

    protected void corruptByteArray(String byteArrayId) {
        ByteArrayEntity byteArray = processEngineConfiguration.getByteArrayEntityManager().findById(byteArrayId);
        byteArray.setBytes("not a serialized object".getBytes(StandardCharsets.UTF_8));
    }
}
