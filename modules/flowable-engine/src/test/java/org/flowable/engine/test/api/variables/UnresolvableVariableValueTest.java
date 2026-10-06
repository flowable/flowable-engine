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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayEntity;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntity;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.api.variables.SerializableVariableTest.TestSerializableVariable;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInitializingList;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.junit.jupiter.api.Test;

/**
 * A variable whose value cannot be resolved fails the queries that resolve the variable values. A query that includes variables can exclude
 * the variable initialization, the value of a variable is then only resolved when it is read.
 */
class UnresolvableVariableValueTest extends PluggableFlowableTestCase {

    // The variables of a query that includes variables are initialized while the query result is mapped, the failure is wrapped by MyBatis
    protected static final String UNRESOLVABLE_VALUE_MESSAGE = "Couldn't deserialize object in variable 'order'";

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void variableInstanceQueriesWithUnresolvableValue() {
        ProcessInstance processInstance = startProcessWithUnresolvableOrderVariable();

        assertThatThrownBy(() -> runtimeService.createVariableInstanceQuery().processInstanceId(processInstance.getId()).list())
                .isInstanceOf(FlowableException.class);
        assertUnresolvableOrderVariable(runtimeService.createVariableInstanceQuery()
                .processInstanceId(processInstance.getId())
                .excludeVariableInitialization()
                .list());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThatThrownBy(() -> historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).list())
                    .isInstanceOf(FlowableException.class);
            assertUnresolvableOrderHistoricVariable(historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .excludeVariableInitialization()
                    .list());
        }
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
    void queriesIncludingVariablesFailForVariableWithUnresolvableValue() {
        ProcessInstance processInstance = startProcessWithUnresolvableOrderVariable();

        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).includeProcessVariables().list())
                .hasMessageContaining(UNRESOLVABLE_VALUE_MESSAGE);
        assertThatThrownBy(() -> taskService.createTaskQuery().processInstanceId(processInstance.getId()).includeProcessVariables().list())
                .hasMessageContaining(UNRESOLVABLE_VALUE_MESSAGE);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThatThrownBy(() -> historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).includeProcessVariables()
                    .list())
                    .hasMessageContaining(UNRESOLVABLE_VALUE_MESSAGE);
            assertThatThrownBy(() -> historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).includeProcessVariables()
                    .list())
                    .hasMessageContaining(UNRESOLVABLE_VALUE_MESSAGE);
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void queriesIncludingVariablesWithoutVariableInitialization() {
        ProcessInstance processInstance = startProcessWithUnresolvableOrderVariable();

        ProcessInstance processInstanceWithVariables = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .includeProcessVariables(true)
                .singleResult();
        assertUnresolvableOrderVariable(((ExecutionEntity) processInstanceWithVariables).getQueryVariables());
        // The value of a variable is resolved when it is read
        assertThatThrownBy(processInstanceWithVariables::getProcessVariables).isInstanceOf(FlowableException.class);

        processInstanceWithVariables = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .includeProcessVariables(Arrays.asList("customer", "order"), true)
                .singleResult();
        assertUnresolvableOrderVariable(((ExecutionEntity) processInstanceWithVariables).getQueryVariables());

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).includeProcessVariables(true).singleResult();
        assertUnresolvableOrderVariable(((TaskEntity) task).getQueryVariables());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .includeProcessVariables(true)
                    .singleResult();
            assertUnresolvableOrderHistoricVariable(((HistoricProcessInstanceEntity) historicProcessInstance).getQueryVariables());

            historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .includeProcessVariables(Arrays.asList("customer", "order"), true)
                    .singleResult();
            assertUnresolvableOrderHistoricVariable(((HistoricProcessInstanceEntity) historicProcessInstance).getQueryVariables());

            HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .includeProcessVariables(true)
                    .singleResult();
            assertUnresolvableOrderHistoricVariable(((HistoricTaskInstanceEntity) historicTask).getQueryVariables());
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void excludeVariableInitializationOnlyAppliesToTheQuery() {
        ProcessInstance processInstance = startProcessWithUnresolvableOrderVariable();

        managementService.executeCommand(commandContext -> {
            assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).includeProcessVariables(true).list())
                    .hasSize(1);
            assertThat(VariableInitializingList.isVariableInitializationExcluded(commandContext)).isFalse();
            return null;
        });
    }

    protected void assertUnresolvableOrderVariable(Collection<? extends VariableInstance> variableInstances) {
        assertThat(variableInstances)
                .extracting(VariableInstance::getName)
                .containsExactlyInAnyOrder("customer", "order");
        for (VariableInstance variableInstance : variableInstances) {
            if ("customer".equals(variableInstance.getName())) {
                assertThat(variableInstance.getValue()).isEqualTo("Kermit");
            } else {
                assertThatThrownBy(variableInstance::getValue).isInstanceOf(FlowableException.class);
            }
        }
    }

    protected void assertUnresolvableOrderHistoricVariable(Collection<? extends HistoricVariableInstance> historicVariableInstances) {
        assertThat(historicVariableInstances)
                .extracting(HistoricVariableInstance::getVariableName)
                .containsExactlyInAnyOrder("customer", "order");
        for (HistoricVariableInstance historicVariableInstance : historicVariableInstances) {
            if ("customer".equals(historicVariableInstance.getVariableName())) {
                assertThat(historicVariableInstance.getValue()).isEqualTo("Kermit");
            } else {
                assertThatThrownBy(historicVariableInstance::getValue).isInstanceOf(FlowableException.class);
            }
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
