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

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemJavaDelegate;
import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Joram Barrez
 */
public class VariablesTest extends FlowableCmmnTestCase {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    @CmmnDeployment
    public void testGetVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("stringVar", "Hello World");
        variables.put("intVar", 42);
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").variables(variables).start();

        Map<String, Object> variablesFromGet = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertThat(variablesFromGet)
                .containsKeys("stringVar", "intVar")
                .containsEntry("stringVar", "Hello World");
        assertThat(((Integer) variablesFromGet.get("intVar")).intValue()).isEqualTo(42);

        Map<String, VariableInstance> variableInstancesFromGet = cmmnRuntimeService.getVariableInstances(caseInstance.getId());
        assertThat(variableInstancesFromGet).containsKey("stringVar");
        VariableInstance variableInstance = variableInstancesFromGet.get("stringVar");
        assertThat(variableInstance.getValue()).isEqualTo("Hello World");
        assertThat(variableInstance.getTypeName()).isEqualTo("string");
        assertThat(variableInstancesFromGet).containsKey("intVar");
        variableInstance = variableInstancesFromGet.get("intVar");
        assertThat(((Integer) variableInstance.getValue()).intValue()).isEqualTo(42);
        assertThat(variableInstance.getTypeName()).isEqualTo("integer");

        assertThat((String) cmmnRuntimeService.getVariable(caseInstance.getId(), "stringVar")).isEqualTo("Hello World");
        assertThat(((Integer) cmmnRuntimeService.getVariable(caseInstance.getId(), "intVar")).intValue()).isEqualTo(42);
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "doesNotExist")).isNull();

        variableInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "stringVar");
        assertThat(variableInstance.getValue()).isEqualTo("Hello World");
        assertThat(variableInstance.getTypeName()).isEqualTo("string");
        variableInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "intVar");
        assertThat(((Integer) variableInstance.getValue()).intValue()).isEqualTo(42);
        assertThat(variableInstance.getTypeName()).isEqualTo("integer");
        variableInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "doesNotExist");
        assertThat(variableInstance).isNull();
    }

    @Test
    @CmmnDeployment
    public void testGetLocalVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("stringVar", "Hello World");
        variables.put("intVar", 42);
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").variables(variables).start();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("nestedTask")
                .caseInstanceId(caseInstance.getId()).singleResult();
        cmmnRuntimeService.setLocalVariable(planItemInstance.getId(), "stringVar", "Changed value");
        cmmnRuntimeService.setLocalVariable(planItemInstance.getId(), "intVar", 21);

        Map<String, Object> variablesFromGet = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertThat(variablesFromGet)
                .containsKeys("stringVar", "intVar")
                .containsEntry("stringVar", "Hello World");
        assertThat(((Integer) variablesFromGet.get("intVar")).intValue()).isEqualTo(42);

        Map<String, VariableInstance> variableInstancesFromGet = cmmnRuntimeService.getVariableInstances(caseInstance.getId());
        assertThat(variableInstancesFromGet).containsKey("stringVar");
        VariableInstance variableInstance = variableInstancesFromGet.get("stringVar");
        assertThat(variableInstance.getValue()).isEqualTo("Hello World");
        assertThat(variableInstance.getTypeName()).isEqualTo("string");
        assertThat(variableInstancesFromGet).containsKey("intVar");
        variableInstance = variableInstancesFromGet.get("intVar");
        assertThat(((Integer) variableInstance.getValue()).intValue()).isEqualTo(42);
        assertThat(variableInstance.getTypeName()).isEqualTo("integer");

        Map<String, Object> localVariablesFromGet = cmmnRuntimeService.getLocalVariables(planItemInstance.getId());
        assertThat(localVariablesFromGet)
                .containsKeys("stringVar", "intVar")
                .containsEntry("stringVar", "Changed value");
        assertThat(((Integer) localVariablesFromGet.get("intVar")).intValue()).isEqualTo(21);

        Map<String, VariableInstance> localVariableInstancesFromGet = cmmnRuntimeService.getLocalVariableInstances(planItemInstance.getId());
        assertThat(localVariableInstancesFromGet).containsKey("stringVar");
        variableInstance = localVariableInstancesFromGet.get("stringVar");
        assertThat(variableInstance.getValue()).isEqualTo("Changed value");
        assertThat(variableInstance.getTypeName()).isEqualTo("string");
        assertThat(variableInstancesFromGet).containsKey("intVar");
        variableInstance = localVariableInstancesFromGet.get("intVar");
        assertThat(((Integer) variableInstance.getValue()).intValue()).isEqualTo(21);
        assertThat(variableInstance.getTypeName()).isEqualTo("integer");
    }

    @Test
    @CmmnDeployment
    public void testSetVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        Map<String, Object> variables = new HashMap<>();
        variables.put("stringVar", "Hello World");
        variables.put("intVar", 42);
        cmmnRuntimeService.setVariables(caseInstance.getId(), variables);

        assertThat((String) cmmnRuntimeService.getVariable(caseInstance.getId(), "stringVar")).isEqualTo("Hello World");
        assertThat(((Integer) cmmnRuntimeService.getVariable(caseInstance.getId(), "intVar")).intValue()).isEqualTo(42);
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "doesNotExist")).isNull();
    }

    @Test
    @CmmnDeployment
    public void testRemoveVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("stringVar", "Hello World");
        variables.put("intVar", 42);
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").variables(variables).start();
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).hasSize(2);

        cmmnRuntimeService.removeVariable(caseInstance.getId(), "stringVar");
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).hasSize(1);
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "StringVar")).isNull();
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "intVar")).isNotNull();
    }

    @Test
    @CmmnDeployment
    public void testSerializableVariable() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("myVariable", new MyVariable("Hello World"));
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").variables(variables).start();

        MyVariable myVariable = (MyVariable) cmmnRuntimeService.getVariable(caseInstance.getId(), "myVariable");
        assertThat(myVariable.value).isEqualTo("Hello World");

        cmmnEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                VariableInstance variableInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "myVariable");
                MyVariable myVariable = (MyVariable) variableInstance.getValue();
                assertThat(myVariable.value).isEqualTo("Hello World");

                return null;
            }
        });
    }

    @Test
    @CmmnDeployment
    public void testResolveMilestoneNameAsExpression() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("myVariable", "Hello from test");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase").variables(variables).start();
        assertCaseInstanceEnded(caseInstance);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricMilestoneInstance historicMilestoneInstance = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId()).singleResult();
            assertThat(historicMilestoneInstance.getName()).isEqualTo("Milestone Hello from test and delegate");
        }
    }

    @Test
    @CmmnDeployment
    public void testHistoricVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("stringVar", "test");
        variables.put("intVar", 123);
        variables.put("doubleVar", 123.123);
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").variables(variables).start();

        // verify variables
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "stringVar")).isEqualTo("test");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicVariableInstance = cmmnHistoryService.createHistoricVariableInstanceQuery().variableName("stringVar")
                .singleResult();
            assertThat(historicVariableInstance.getScopeId()).isEqualTo(caseInstance.getId());
            assertThat(historicVariableInstance.getScopeType()).isEqualTo(ScopeTypes.CMMN);
            assertThat(historicVariableInstance.getValue()).isEqualTo("test");
            assertThat(historicVariableInstance.getSubScopeId()).isNull();
        }

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "intVar")).isEqualTo(123);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicVariableInstance = cmmnHistoryService.createHistoricVariableInstanceQuery().variableName("intVar").singleResult();
            assertThat(historicVariableInstance.getScopeId()).isEqualTo(caseInstance.getId());
            assertThat(historicVariableInstance.getScopeType()).isEqualTo(ScopeTypes.CMMN);
            assertThat(historicVariableInstance.getValue()).isEqualTo(123);
            assertThat(historicVariableInstance.getSubScopeId()).isNull();
        }

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "doubleVar")).isEqualTo(123.123);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicVariableInstance = cmmnHistoryService.createHistoricVariableInstanceQuery().variableName("doubleVar").singleResult();
            assertThat(historicVariableInstance.getScopeId()).isEqualTo(caseInstance.getId());
            assertThat(historicVariableInstance.getScopeType()).isEqualTo(ScopeTypes.CMMN);
            assertThat(historicVariableInstance.getValue()).isEqualTo(123.123);
            assertThat(historicVariableInstance.getSubScopeId()).isNull();
        }

        // Update variables
        Map<String, Object> newVariables = new HashMap<>();
        newVariables.put("stringVar", "newValue");
        newVariables.put("otherStringVar", "test number 2");
        cmmnRuntimeService.setVariables(caseInstance.getId(), newVariables);

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "stringVar")).isEqualTo("newValue");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().variableName("stringVar").singleResult().getValue()).isEqualTo("newValue");
            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().variableName("otherStringVar").singleResult().getValue())
                .isEqualTo("test number 2");
        }

        // Delete variables
        cmmnRuntimeService.removeVariable(caseInstance.getId(), "stringVar");
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "stringVar")).isNull();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().variableName("stringVar").singleResult()).isNull();
            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().variableName("otherStringVar").singleResult()).isNotNull();
        }
    }

    @Test
    @CmmnDeployment
    public void testTransientVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("transientStartVar", "Hello from test");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase").transientVariables(variables).start();


        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricMilestoneInstance historicMilestoneInstance = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId()).singleResult();
            assertThat(historicMilestoneInstance.getName()).isEqualTo("Milestone Hello from test and delegate");

            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        }

        // Variables should not be persisted
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).isEmpty();
    }

    @Test
    @CmmnDeployment
    public void testBlockingExpressionBasedOnVariable() {
        // Blocking
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testBlockingExpression")
                .variable("nameVar", "First Task")
                .variable("blockB", true)
                .start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceStateActive()
                .orderByName().asc()
                .list();
        assertThat(planItemInstances).hasSize(2);
        assertThat(planItemInstances.get(0).getName()).isEqualTo("B");
        assertThat(planItemInstances.get(1).getName()).isEqualTo("First Task");

        // Non-blocking
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testBlockingExpression")
                .variable("nameVar", "Second Task")
                .variable("blockB", false)
                .start();

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceStateActive()
                .list();
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly("Second Task");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void testSetVariableOnRootCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .variable("varToUpdate", "initialValue")
                .caseDefinitionKey("oneHumanTaskCase")
                .start();

        cmmnRuntimeService.setVariable(caseInstance.getId(), "varToUpdate", "newValue");

        CaseInstance updatedCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().
                caseInstanceId(caseInstance.getId()).
                includeCaseVariables().
                singleResult();
        assertThat(updatedCaseInstance.getCaseVariables()).containsEntry("varToUpdate", "newValue");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void testSetVariableOnNonExistingCase() {
        this.expectedException.expect(FlowableObjectNotFoundException.class);
        this.expectedException.expectMessage("No case instance found for id NON-EXISTING-CASE");

        cmmnRuntimeService.setVariable("NON-EXISTING-CASE", "varToUpdate", "newValue");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void testSetVariableWithoutName() {
        this.expectedException.expect(FlowableIllegalArgumentException.class);
        this.expectedException.expectMessage("variable name is null");

        cmmnRuntimeService.setVariable("NON-EXISTING-CASE", null, "newValue");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void testSetVariableOnRootCaseWithExpression() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .variable("varToUpdate", "initialValue")
                .caseDefinitionKey("oneHumanTaskCase")
                .start();

        cmmnRuntimeService.setVariable(caseInstance.getId(), "${varToUpdate}", "newValue");

        CaseInstance updatedCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().
                caseInstanceId(caseInstance.getId()).
                includeCaseVariables().
                singleResult();
        assertThat(updatedCaseInstance.getCaseVariables())
                .as("resolving variable name expressions does not make sense when it is set locally")
                .containsEntry("varToUpdate", "newValue");
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn",
            "org/flowable/cmmn/test/runtime/VariablesTest.rootProcess.cmmn"
    })
    public void testSetVariableOnSubCase() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("rootCase")
                .start();
        CaseInstance subCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("oneHumanTaskCase").singleResult();

        cmmnRuntimeService.setVariable(subCaseInstance.getId(), "varToUpdate", "newValue");

        CaseInstance updatedCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().
                caseInstanceId(subCaseInstance.getId()).
                includeCaseVariables().
                singleResult();
        assertThat(updatedCaseInstance.getCaseVariables()).containsEntry("varToUpdate", "newValue");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void testSetVariablesOnRootCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .variable("varToUpdate", "initialValue")
                .caseDefinitionKey("oneHumanTaskCase")
                .start();
        Map<String, Object> variables = Stream.of(new ImmutablePair<String, Object>("varToUpdate", "newValue")).collect(
                toMap(Pair::getKey, Pair::getValue)
        );
        cmmnRuntimeService.setVariables(caseInstance.getId(), variables);

        CaseInstance updatedCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().
                caseInstanceId(caseInstance.getId()).
                includeCaseVariables().
                singleResult();
        assertThat(updatedCaseInstance.getCaseVariables()).isEqualTo(variables);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void testSetVariablesOnNonExistingCase() {
        this.expectedException.expect(FlowableObjectNotFoundException.class);
        this.expectedException.expectMessage("No case instance found for id NON-EXISTING-CASE");
        Map<String, Object> variables = Stream.of(new ImmutablePair<String, Object>("varToUpdate", "newValue")).collect(
                toMap(Pair::getKey, Pair::getValue)
        );

        cmmnRuntimeService.setVariables("NON-EXISTING-CASE", variables);
    }

    @SuppressWarnings("unchecked")
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void testSetVariablesWithEmptyMap() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .variable("varToUpdate", "initialValue")
                .caseDefinitionKey("oneHumanTaskCase")
                .start();

        this.expectedException.expect(FlowableIllegalArgumentException.class);
        this.expectedException.expectMessage("variables is empty");

        cmmnRuntimeService.setVariables(caseInstance.getId(), Collections.EMPTY_MAP);
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn",
            "org/flowable/cmmn/test/runtime/VariablesTest.rootProcess.cmmn"
    })
    public void testSetVariablesOnSubCase() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("rootCase")
                .start();
        CaseInstance subCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("oneHumanTaskCase").singleResult();
        Map<String, Object> variables = CollectionUtil.singletonMap("varToUpdate", "newValue");
        cmmnRuntimeService.setVariables(subCaseInstance.getId(), variables);

        CaseInstance updatedCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().
                caseInstanceId(subCaseInstance.getId()).
                includeCaseVariables().
                singleResult();
        assertThat(updatedCaseInstance.getCaseVariables()).isEqualTo(variables);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void testIncludeVariablesWithSerializableVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .variable("myVar", new CustomTestVariable("test", 123))
                .start();

        CaseInstance caseInstanceWithVariables = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .includeCaseVariables()
                .singleResult();

        CustomTestVariable customTestVariable = (CustomTestVariable) caseInstanceWithVariables.getCaseVariables().get("myVar");
        assertThat(customTestVariable.someValue).isEqualTo("test");
        assertThat(customTestVariable.someInt).isEqualTo(123);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void testAccessToScopeIdWhenSettingVariable() {
        addVariableTypeIfNotExists(CustomAccessCaseInstanceVariableType.INSTANCE);

        CustomAccessCaseType customVar = new CustomAccessCaseType();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .variable("customVar", customVar)
                .start();

        assertThat(customVar.getProcessInstanceId())
                .as("custom var process instance id")
                .isNull();

        assertThat(customVar.getExecutionId())
                .as("custom var execution id")
                .isNull();

        assertThat(customVar.getTaskId())
                .as("custom var task id")
                .isNull();

        assertThat(customVar.getScopeId())
                .as("custom var scope id")
                .isEqualTo(caseInstance.getId());

        assertThat(customVar.getSubScopeId())
                .as("custom var sub scope id")
                .isNull();

        assertThat(customVar.getScopeType())
                .as("custom var scope type")
                .isEqualTo(ScopeTypes.CMMN);

        customVar = (CustomAccessCaseType) cmmnRuntimeService.getVariable(caseInstance.getId(), "customVar");

        assertThat(customVar.getProcessInstanceId())
                .as("custom var process instance id")
                .isNull();

        assertThat(customVar.getExecutionId())
                .as("custom var execution id")
                .isNull();

        assertThat(customVar.getTaskId())
                .as("custom var task id")
                .isNull();

        assertThat(customVar.getScopeId())
                .as("custom var scope id")
                .isEqualTo(caseInstance.getId());

        assertThat(customVar.getSubScopeId())
                .as("custom var sub scope id")
                .isNull();

        assertThat(customVar.getScopeType())
                .as("custom var scope type")
                .isEqualTo(ScopeTypes.CMMN);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void testAccessToTaskIdWhenSettingLocalVariableOnTask() {
        addVariableTypeIfNotExists(CustomAccessCaseInstanceVariableType.INSTANCE);

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .start();

        Task task = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();

        assertThat(task).isNotNull();

        CustomAccessCaseType customVar = new CustomAccessCaseType();
        cmmnTaskService.setVariableLocal(task.getId(), "customTaskVar", customVar);

        assertThat(customVar.getProcessInstanceId())
                .as("custom var process instance id")
                .isNull();

        assertThat(customVar.getExecutionId())
                .as("custom var execution id")
                .isNull();

        assertThat(customVar.getTaskId())
                .as("custom var task id")
                .isEqualTo(task.getId());

        assertThat(customVar.getScopeId())
                .as("custom var scope id")
                .isEqualTo(caseInstance.getId());

        assertThat(customVar.getSubScopeId())
                .as("custom var sub scope id")
                .isEqualTo(task.getSubScopeId());

        assertThat(customVar.getScopeType())
                .as("custom var scope type")
                .isEqualTo(ScopeTypes.CMMN);

        customVar = (CustomAccessCaseType) cmmnTaskService.getVariableLocal(task.getId(), "customTaskVar");

        assertThat(customVar.getProcessInstanceId())
                .as("custom var process instance id")
                .isNull();

        assertThat(customVar.getExecutionId())
                .as("custom var execution id")
                .isNull();

        assertThat(customVar.getTaskId())
                .as("custom var task id")
                .isEqualTo(task.getId());

        assertThat(customVar.getScopeId())
                .as("custom var scope id")
                .isEqualTo(caseInstance.getId());

        assertThat(customVar.getSubScopeId())
                .as("custom var sub scope id")
                .isEqualTo(task.getSubScopeId());

        assertThat(customVar.getScopeType())
                .as("custom var scope type")
                .isEqualTo(ScopeTypes.CMMN);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void testAccessToSubScopeIdWhenSettingLocalVariableOnExecution() {
        addVariableTypeIfNotExists(CustomAccessCaseInstanceVariableType.INSTANCE);

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .start();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionId("theTask")
                .singleResult();

        assertThat(planItemInstance).isNotNull();

        CustomAccessCaseType customVar = new CustomAccessCaseType();
        cmmnRuntimeService.setLocalVariable(planItemInstance.getId(), "customPlanItemVar", customVar);

        assertThat(customVar.getProcessInstanceId())
                .as("custom var process instance id")
                .isNull();

        assertThat(customVar.getExecutionId())
                .as("custom var execution id")
                .isNull();

        assertThat(customVar.getTaskId())
                .as("custom var task id")
                .isNull();

        assertThat(customVar.getScopeId())
                .as("custom var scope id")
                .isEqualTo(caseInstance.getId());

        assertThat(customVar.getSubScopeId())
                .as("custom var sub scope id")
                .isEqualTo(planItemInstance.getId());

        assertThat(customVar.getScopeType())
                .as("custom var scope type")
                .isEqualTo(ScopeTypes.CMMN);

        customVar = (CustomAccessCaseType) cmmnRuntimeService.getLocalVariable(planItemInstance.getId(), "customPlanItemVar");

        assertThat(customVar.getProcessInstanceId())
                .as("custom var process instance id")
                .isNull();

        assertThat(customVar.getExecutionId())
                .as("custom var execution id")
                .isNull();

        assertThat(customVar.getTaskId())
                .as("custom var task id")
                .isNull();

        assertThat(customVar.getScopeId())
                .as("custom var scope id")
                .isEqualTo(caseInstance.getId());

        assertThat(customVar.getSubScopeId())
                .as("custom var sub scope id")
                .isEqualTo(planItemInstance.getId());

        assertThat(customVar.getScopeType())
                .as("custom var scope type")
                .isEqualTo(ScopeTypes.CMMN);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void testImmutableEmptyCollectionVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .variable("listVar", Collections.emptyList())
                .variable("setVar", Collections.emptySet())
                .start();

        VariableInstance variableInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "listVar");

        assertThat(variableInstance.getTypeName()).isEqualTo("emptyCollection");
        assertThat(variableInstance.getValue()).asList().isEmpty();

        variableInstance = cmmnRuntimeService   .getVariableInstance(caseInstance.getId(), "setVar");

        assertThat(variableInstance.getTypeName()).isEqualTo("emptyCollection");
        assertThat(variableInstance.getValue())
                .isInstanceOfSatisfying(Set.class, set -> assertThat(set).isEmpty());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void testEmptyCollectionVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .variable("listVar", new ArrayList<>())
                .variable("setVar", new HashSet<>())
                .start();

        VariableInstance variableInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "listVar");

        assertThat(variableInstance.getTypeName()).isEqualTo("serializable");
        assertThat(variableInstance.getValue()).asList().isEmpty();

        variableInstance = cmmnRuntimeService   .getVariableInstance(caseInstance.getId(), "setVar");

        assertThat(variableInstance.getTypeName()).isEqualTo("serializable");
        assertThat(variableInstance.getValue())
                .isInstanceOfSatisfying(Set.class, set -> assertThat(set).isEmpty());
    }

    protected void addVariableTypeIfNotExists(VariableType variableType) {
        // We can't remove the VariableType after every test since it would cause the test
        // to fail due to not being able to get the variable value during deleting
        if (cmmnEngineConfiguration.getVariableTypes().getTypeIndex(variableType) == -1) {
            cmmnEngineConfiguration.getVariableTypes().addType(variableType);
        }
    }

    // Test helper classes

    static class CustomAccessCaseType {

        protected String processInstanceId;
        protected String executionId;
        protected String taskId;
        protected String scopeId;
        protected String subScopeId;
        protected String scopeType;

        public String getProcessInstanceId() {
            return processInstanceId;
        }

        public void setProcessInstanceId(String processInstanceId) {
            this.processInstanceId = processInstanceId;
        }

        public String getExecutionId() {
            return executionId;
        }

        public void setExecutionId(String executionId) {
            this.executionId = executionId;
        }

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getScopeId() {
            return scopeId;
        }

        public void setScopeId(String scopeId) {
            this.scopeId = scopeId;
        }

        public String getSubScopeId() {
            return subScopeId;
        }

        public void setSubScopeId(String subScopeId) {
            this.subScopeId = subScopeId;
        }

        public String getScopeType() {
            return scopeType;
        }

        public void setScopeType(String scopeType) {
            this.scopeType = scopeType;
        }
    }

    static class CustomAccessCaseInstanceVariableType implements VariableType {

        static final CustomAccessCaseInstanceVariableType INSTANCE = new CustomAccessCaseInstanceVariableType();

        @Override
        public String getTypeName() {
            return "CustomAccessCaseInstanceVariableType";
        }

        @Override
        public boolean isCachable() {
            return true;
        }

        @Override
        public boolean isAbleToStore(Object value) {
            return value instanceof CustomAccessCaseType;
        }

        @Override
        public void setValue(Object value, ValueFields valueFields) {
            CustomAccessCaseType customValue = (CustomAccessCaseType) value;

            customValue.setProcessInstanceId(valueFields.getProcessInstanceId());
            customValue.setExecutionId(valueFields.getExecutionId());
            customValue.setTaskId(valueFields.getTaskId());
            customValue.setScopeId(valueFields.getScopeId());
            customValue.setSubScopeId(valueFields.getSubScopeId());
            customValue.setScopeType(valueFields.getScopeType());

            String textValue = new StringJoiner(",")
                    .add(customValue.getProcessInstanceId())
                    .add(customValue.getExecutionId())
                    .add(customValue.getTaskId())
                    .add(customValue.getScopeId())
                    .add(customValue.getSubScopeId())
                    .add(customValue.getScopeType())
                    .toString();
            valueFields.setTextValue(textValue);
        }

        @Override
        public Object getValue(ValueFields valueFields) {
            String textValue = valueFields.getTextValue();
            String[] values = textValue.split(",");

            CustomAccessCaseType customValue = new CustomAccessCaseType();
            customValue.setProcessInstanceId(valueAt(values, 0));
            customValue.setExecutionId(valueAt(values, 1));
            customValue.setTaskId(valueAt(values, 2));
            customValue.setScopeId(valueAt(values, 3));
            customValue.setSubScopeId(valueAt(values, 4));
            customValue.setScopeType(valueAt(values, 5));

            return customValue;
        }

        protected String valueAt(String[] array, int index) {
            if (array.length > index) {
                return getValue(array[index]);
            }

            return null;
        }

        protected String getValue(String value) {
            return "null".equals(value) ? null : value;
        }
    }

    public static class MyVariable implements Serializable {

        private static final long serialVersionUID = 1L;

        private String value;

        public MyVariable(String value) {
            this.value = value;
        }

    }

    public static class SetVariableDelegate implements PlanItemJavaDelegate {

        @Override
        public void execute(DelegatePlanItemInstance planItemInstance) {
            String variableValue = (String) planItemInstance.getVariable("myVariable");
            planItemInstance.setVariable("myVariable", variableValue + " and delegate");
        }

    }

    public static class SetTransientVariableDelegate implements PlanItemJavaDelegate {

        @Override
        public void execute(DelegatePlanItemInstance planItemInstance) {
            String variableValue = (String) planItemInstance.getVariable("transientStartVar");
            planItemInstance.setTransientVariable("transientVar", variableValue + " and delegate");
        }

    }

    public static class CustomTestVariable implements Serializable {

        private String someValue;
        private int someInt;

        public CustomTestVariable(String someValue, int someInt) {
            this.someValue = someValue;
            this.someInt = someInt;
        }
    }

}
