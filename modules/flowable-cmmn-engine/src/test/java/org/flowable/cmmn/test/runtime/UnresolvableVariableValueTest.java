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
package org.flowable.cmmn.test.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayEntity;
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
public class UnresolvableVariableValueTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    public void variableInstanceQueriesReturnVariableWithUnresolvableValue() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variable("customer", "Kermit")
                .variable("order", new TestSerializableVariable(1))
                .start();
        corruptRuntimeAndHistoricSerializedValue(caseInstance.getId(), "order");

        assertThat(cmmnRuntimeService.createVariableInstanceQuery().caseInstanceId(caseInstance.getId()).list())
                .extracting(VariableInstance::getName, VariableInstance::getValue, VariableValueUtil::isValueUnresolvable)
                .containsExactlyInAnyOrder(
                        tuple("customer", "Kermit", false),
                        tuple("order", null, true)
                );

        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).list())
                .extracting(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue, VariableValueUtil::isValueUnresolvable)
                .containsExactlyInAnyOrder(
                        tuple("customer", "Kermit", false),
                        tuple("order", null, true)
                );
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    public void getVariablesFailsForVariableWithUnresolvableValue() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variable("customer", "Kermit")
                .variable("order", new TestSerializableVariable(1))
                .start();
        corruptRuntimeAndHistoricSerializedValue(caseInstance.getId(), "order");

        assertThatThrownBy(() -> cmmnRuntimeService.getVariables(caseInstance.getId())).isInstanceOf(FlowableException.class);
        assertThatThrownBy(() -> cmmnRuntimeService.getVariable(caseInstance.getId(), "order")).isInstanceOf(FlowableException.class);
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "customer")).isEqualTo("Kermit");

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThatThrownBy(() -> cmmnTaskService.getVariables(task.getId())).isInstanceOf(FlowableException.class);
        assertThatThrownBy(() -> cmmnTaskService.getVariable(task.getId(), "order")).isInstanceOf(FlowableException.class);

        // The variable instances can be fetched, the value of the variable is only resolved when it is read
        assertThat(cmmnRuntimeService.getVariableInstances(caseInstance.getId())).containsOnlyKeys("customer", "order");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    public void queriesIncludingVariablesReturnVariableWithUnresolvableValue() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variable("customer", "Kermit")
                .variable("order", new TestSerializableVariable(1))
                .start();
        corruptRuntimeAndHistoricSerializedValue(caseInstance.getId(), "order");

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).includeCaseVariables().singleResult()
                .getCaseVariables())
                .containsOnly(entry("customer", "Kermit"), entry("order", null));

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).includeCaseVariables().singleResult()
                .getCaseVariables())
                .containsOnly(entry("customer", "Kermit"), entry("order", null));

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).includeCaseVariables().singleResult()
                .getCaseVariables())
                .containsOnly(entry("customer", "Kermit"), entry("order", null));
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Simple_Reactivation_Test_Case.cmmn.xml")
    public void caseReactivationFailsForVariableWithUnresolvableValue() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("simpleReactivationTestCase")
                .variable("customer", "Kermit")
                .variable("order", new TestSerializableVariable(1))
                .start();
        cmmnRuntimeService.triggerPlanItemInstance(getActivePlanItemInstanceId(caseInstance.getId(), "Task A"));
        cmmnRuntimeService.triggerPlanItemInstance(getActivePlanItemInstanceId(caseInstance.getId(), "Task B"));

        HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId())
                .finished().singleResult();
        assertThat(historicCaseInstance).isNotNull();
        corruptHistoricSerializedValue(caseInstance.getId(), "order");

        // The variable must not be lost by reactivating the case without it
        assertThatThrownBy(() -> cmmnHistoryService.createCaseReactivationBuilder(historicCaseInstance.getId()).reactivate())
                .isInstanceOf(FlowableException.class);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }

    protected String getActivePlanItemInstanceId(String caseInstanceId, String name) {
        return cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstanceId)
                .planItemInstanceName(name)
                .planItemInstanceStateActive()
                .singleResult()
                .getId();
    }

    protected void corruptRuntimeAndHistoricSerializedValue(String caseInstanceId, String variableName) {
        cmmnEngineConfiguration.getCommandExecutor().execute(commandContext -> {
            VariableInstanceEntity variableInstance = (VariableInstanceEntity) cmmnRuntimeService.createVariableInstanceQuery()
                    .caseInstanceId(caseInstanceId)
                    .variableName(variableName)
                    .excludeVariableInitialization()
                    .singleResult();
            corruptByteArray(variableInstance.getByteArrayRef().getId());
            return null;
        });
        corruptHistoricSerializedValue(caseInstanceId, variableName);
    }

    protected void corruptHistoricSerializedValue(String caseInstanceId, String variableName) {
        cmmnEngineConfiguration.getCommandExecutor().execute(commandContext -> {
            HistoricVariableInstanceEntity historicVariableInstance = (HistoricVariableInstanceEntity) cmmnHistoryService
                    .createHistoricVariableInstanceQuery()
                    .caseInstanceId(caseInstanceId)
                    .variableName(variableName)
                    .excludeVariableInitialization()
                    .singleResult();
            corruptByteArray(historicVariableInstance.getByteArrayRef().getId());
            return null;
        });
    }

    protected void corruptByteArray(String byteArrayId) {
        ByteArrayEntity byteArray = cmmnEngineConfiguration.getByteArrayEntityManager().findById(byteArrayId);
        byteArray.setBytes("not a serialized object".getBytes(StandardCharsets.UTF_8));
    }

    public static class TestSerializableVariable implements Serializable {

        private static final long serialVersionUID = 1L;

        protected int number;

        public TestSerializableVariable(int number) {
            this.number = number;
        }

        public int getNumber() {
            return number;
        }
    }
}
