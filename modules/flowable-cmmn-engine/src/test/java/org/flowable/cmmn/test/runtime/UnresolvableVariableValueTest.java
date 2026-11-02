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

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayEntity;
import org.flowable.task.api.Task;
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
public class UnresolvableVariableValueTest extends FlowableCmmnTestCase {

    // The variables of a query that includes variables are initialized while the query result is mapped, the failure is wrapped by MyBatis
    protected static final String UNRESOLVABLE_VALUE_MESSAGE = "Couldn't deserialize object in variable 'order'";

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    public void variableInstanceQueriesWithUnresolvableValue() {
        CaseInstance caseInstance = startCaseWithUnresolvableOrderVariable();

        assertThatThrownBy(() -> cmmnRuntimeService.createVariableInstanceQuery().caseInstanceId(caseInstance.getId()).list())
                .isInstanceOf(FlowableException.class);
        assertUnresolvableVariable(cmmnRuntimeService.createVariableInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .excludeVariableInitialization()
                .list(), "customer", "order");

        assertThatThrownBy(() -> cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).list())
                .isInstanceOf(FlowableException.class);
        assertUnresolvableHistoricVariable(cmmnHistoryService.createHistoricVariableInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .excludeVariableInitialization()
                .list(), "customer", "order");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    public void getVariablesFailsForVariableWithUnresolvableValue() {
        CaseInstance caseInstance = startCaseWithUnresolvableOrderVariable();

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
    public void queriesIncludingVariablesFailForVariableWithUnresolvableValue() {
        CaseInstance caseInstance = startCaseWithUnresolvableOrderVariable();

        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).includeCaseVariables().list())
                .hasMessageContaining(UNRESOLVABLE_VALUE_MESSAGE);
        assertThatThrownBy(() -> cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).includeCaseVariables().list())
                .hasMessageContaining(UNRESOLVABLE_VALUE_MESSAGE);
        assertThatThrownBy(() -> cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).includeCaseVariables().list())
                .hasMessageContaining(UNRESOLVABLE_VALUE_MESSAGE);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    public void queriesIncludingVariablesWithoutVariableInitialization() {
        CaseInstance caseInstance = startCaseWithUnresolvableOrderVariable();

        CaseInstance caseInstanceWithVariables = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .includeCaseVariables(true)
                .singleResult();
        assertUnresolvableVariable(((CaseInstanceEntity) caseInstanceWithVariables).getQueryVariables(), "customer", "order");
        // The value of a variable is resolved when it is read
        assertThatThrownBy(caseInstanceWithVariables::getCaseVariables).isInstanceOf(FlowableException.class);

        caseInstanceWithVariables = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .includeCaseVariables(Arrays.asList("customer", "order"), true)
                .singleResult();
        assertUnresolvableVariable(((CaseInstanceEntity) caseInstanceWithVariables).getQueryVariables(), "customer", "order");

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).includeCaseVariables(true).singleResult();
        assertUnresolvableVariable(((TaskEntity) task).getQueryVariables(), "customer", "order");

        HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .includeCaseVariables(true)
                .singleResult();
        assertUnresolvableHistoricVariable(((HistoricCaseInstanceEntity) historicCaseInstance).getQueryVariables(), "customer", "order");

        historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .includeCaseVariables(Arrays.asList("customer", "order"), true)
                .singleResult();
        assertUnresolvableHistoricVariable(((HistoricCaseInstanceEntity) historicCaseInstance).getQueryVariables(), "customer", "order");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    public void planItemInstanceQueriesIncludingLocalVariablesWithoutVariableInitialization() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();
        String planItemInstanceId = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getId();
        Map<String, Object> localVariables = new HashMap<>();
        localVariables.put("localCustomer", "Kermit");
        localVariables.put("localOrder", new TestSerializableVariable(1));
        cmmnRuntimeService.setLocalVariables(planItemInstanceId, localVariables);
        corruptRuntimeAndHistoricSerializedValue(caseInstance.getId(), "localOrder");

        assertThatThrownBy(() -> cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceId(planItemInstanceId).includeLocalVariables().list())
                .hasMessageContaining("Couldn't deserialize object in variable 'localOrder'");

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceId(planItemInstanceId)
                .includeLocalVariables(true)
                .singleResult();
        assertUnresolvableVariable(((PlanItemInstanceEntity) planItemInstance).getQueryVariables(), "localCustomer", "localOrder");

        HistoricPlanItemInstance historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceId(planItemInstanceId)
                .includeLocalVariables(true)
                .singleResult();
        assertUnresolvableHistoricVariable(((HistoricPlanItemInstanceEntity) historicPlanItemInstance).getQueryVariables(), "localCustomer",
                "localOrder");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    public void excludeVariableInitializationOnlyAppliesToTheQuery() {
        CaseInstance caseInstance = startCaseWithUnresolvableOrderVariable();

        cmmnEngineConfiguration.getCommandExecutor().execute(commandContext -> {
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).includeCaseVariables(true).list())
                    .hasSize(1);
            assertThat(VariableInitializingList.isVariableInitializationExcluded(commandContext)).isFalse();
            return null;
        });
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

    protected CaseInstance startCaseWithUnresolvableOrderVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variable("customer", "Kermit")
                .variable("order", new TestSerializableVariable(1))
                .start();
        corruptRuntimeAndHistoricSerializedValue(caseInstance.getId(), "order");
        return caseInstance;
    }

    protected void assertUnresolvableVariable(Collection<? extends VariableInstance> variableInstances, String resolvableName, String unresolvableName) {
        assertThat(variableInstances)
                .extracting(VariableInstance::getName)
                .containsExactlyInAnyOrder(resolvableName, unresolvableName);
        for (VariableInstance variableInstance : variableInstances) {
            if (resolvableName.equals(variableInstance.getName())) {
                assertThat(variableInstance.getValue()).isEqualTo("Kermit");
            } else {
                assertThatThrownBy(variableInstance::getValue).isInstanceOf(FlowableException.class);
            }
        }
    }

    protected void assertUnresolvableHistoricVariable(Collection<? extends HistoricVariableInstance> historicVariableInstances, String resolvableName,
            String unresolvableName) {

        assertThat(historicVariableInstances)
                .extracting(HistoricVariableInstance::getVariableName)
                .containsExactlyInAnyOrder(resolvableName, unresolvableName);
        for (HistoricVariableInstance historicVariableInstance : historicVariableInstances) {
            if (resolvableName.equals(historicVariableInstance.getVariableName())) {
                assertThat(historicVariableInstance.getValue()).isEqualTo("Kermit");
            } else {
                assertThatThrownBy(historicVariableInstance::getValue).isInstanceOf(FlowableException.class);
            }
        }
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
