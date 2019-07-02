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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemJavaDelegate;
import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
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
        assertTrue(variablesFromGet.containsKey("stringVar"));
        assertEquals("Hello World", (String) variablesFromGet.get("stringVar"));
        assertTrue(variablesFromGet.containsKey("intVar"));
        assertEquals(42, ((Integer) variablesFromGet.get("intVar")).intValue());

        assertEquals("Hello World", (String) cmmnRuntimeService.getVariable(caseInstance.getId(), "stringVar"));
        assertEquals(42, ((Integer) cmmnRuntimeService.getVariable(caseInstance.getId(), "intVar")).intValue());
        assertNull(cmmnRuntimeService.getVariable(caseInstance.getId(), "doesNotExist"));
    }

    @Test
    @CmmnDeployment
    public void testSetVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        Map<String, Object> variables = new HashMap<>();
        variables.put("stringVar", "Hello World");
        variables.put("intVar", 42);
        cmmnRuntimeService.setVariables(caseInstance.getId(), variables);

        assertEquals("Hello World", (String) cmmnRuntimeService.getVariable(caseInstance.getId(), "stringVar"));
        assertEquals(42, ((Integer) cmmnRuntimeService.getVariable(caseInstance.getId(), "intVar")).intValue());
        assertNull(cmmnRuntimeService.getVariable(caseInstance.getId(), "doesNotExist"));
    }

    @Test
    @CmmnDeployment
    public void testRemoveVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("stringVar", "Hello World");
        variables.put("intVar", 42);
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").variables(variables).start();
        assertEquals(2, cmmnRuntimeService.getVariables(caseInstance.getId()).size());

        cmmnRuntimeService.removeVariable(caseInstance.getId(), "stringVar");
        assertEquals(1, cmmnRuntimeService.getVariables(caseInstance.getId()).size());
        assertNull(cmmnRuntimeService.getVariable(caseInstance.getId(), "StringVar"));
        assertNotNull(cmmnRuntimeService.getVariable(caseInstance.getId(), "intVar"));
    }
    
    @Test
    @CmmnDeployment
    public void testSerializableVariable() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("myVariable", new MyVariable("Hello World"));
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").variables(variables).start();
        
        MyVariable myVariable = (MyVariable) cmmnRuntimeService.getVariable(caseInstance.getId(), "myVariable");
        assertEquals("Hello World", myVariable.value);
    }
    
    @Test
    @CmmnDeployment
    public void testResolveMilestoneNameAsExpression() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("myVariable", "Hello from test");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase").variables(variables).start();
        assertCaseInstanceEnded(caseInstance);

        HistoricMilestoneInstance historicMilestoneInstance = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Milestone Hello from test and delegate", historicMilestoneInstance.getName());
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
        assertEquals("test", cmmnRuntimeService.getVariable(caseInstance.getId(), "stringVar"));
        HistoricVariableInstance historicVariableInstance = cmmnHistoryService.createHistoricVariableInstanceQuery().variableName("stringVar").singleResult();
        assertEquals(caseInstance.getId(), historicVariableInstance.getScopeId());
        assertEquals(ScopeTypes.CMMN, historicVariableInstance.getScopeType());
        assertEquals("test", historicVariableInstance.getValue());
        assertNull(historicVariableInstance.getSubScopeId());
        
        assertEquals(123, cmmnRuntimeService.getVariable(caseInstance.getId(), "intVar"));
        historicVariableInstance = cmmnHistoryService.createHistoricVariableInstanceQuery().variableName("intVar").singleResult();
        assertEquals(caseInstance.getId(), historicVariableInstance.getScopeId());
        assertEquals(ScopeTypes.CMMN, historicVariableInstance.getScopeType());
        assertEquals(123, historicVariableInstance.getValue());
        assertNull(historicVariableInstance.getSubScopeId());
        
        assertEquals(123.123, cmmnRuntimeService.getVariable(caseInstance.getId(), "doubleVar"));
        historicVariableInstance = cmmnHistoryService.createHistoricVariableInstanceQuery().variableName("doubleVar").singleResult();
        assertEquals(caseInstance.getId(), historicVariableInstance.getScopeId());
        assertEquals(ScopeTypes.CMMN, historicVariableInstance.getScopeType());
        assertEquals(123.123, historicVariableInstance.getValue());
        assertNull(historicVariableInstance.getSubScopeId());
        
        // Update variables
        Map<String, Object> newVariables = new HashMap<>();
        newVariables.put("stringVar", "newValue");
        newVariables.put("otherStringVar", "test number 2");        
        cmmnRuntimeService.setVariables(caseInstance.getId(), newVariables);
        
        assertEquals("newValue", cmmnRuntimeService.getVariable(caseInstance.getId(), "stringVar"));
        assertEquals("newValue", cmmnHistoryService.createHistoricVariableInstanceQuery().variableName("stringVar").singleResult().getValue());
        assertEquals("test number 2", cmmnHistoryService.createHistoricVariableInstanceQuery().variableName("otherStringVar").singleResult().getValue());
        
        // Delete variables
        cmmnRuntimeService.removeVariable(caseInstance.getId(), "stringVar");
        assertNull(cmmnRuntimeService.getVariable(caseInstance.getId(), "stringVar"));
        assertNull(cmmnHistoryService.createHistoricVariableInstanceQuery().variableName("stringVar").singleResult());
        assertNotNull(cmmnHistoryService.createHistoricVariableInstanceQuery().variableName("otherStringVar").singleResult());
    }
    
    @Test
    @CmmnDeployment
    public void testTransientVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("transientStartVar", "Hello from test");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase").transientVariables(variables).start();
        
        HistoricMilestoneInstance historicMilestoneInstance = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("Milestone Hello from test and delegate", historicMilestoneInstance.getName());
        
        // Variables should not be persisted
        assertEquals(0, cmmnRuntimeService.getVariables(caseInstance.getId()).size());
        assertEquals(0, cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).count());
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
        assertEquals(2, planItemInstances.size());
        assertEquals("B", planItemInstances.get(0).getName());
        assertEquals("First Task", planItemInstances.get(1).getName());
        
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
        assertEquals(1, planItemInstances.size());
        assertEquals("Second Task", planItemInstances.get(0).getName());
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
        assertThat(updatedCaseInstance.getCaseVariables().get("varToUpdate"), is("newValue"));
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
        assertThat("resolving variable name expressions does not make sense when it is set locally",
                updatedCaseInstance.getCaseVariables().get("varToUpdate"), is("newValue"));
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
        assertThat(updatedCaseInstance.getCaseVariables().get("varToUpdate"), is("newValue"));
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/task/CmmnTaskServiceTest.testOneHumanTaskCase.cmmn")
    public void testSetVariablesOnRootCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .variable("varToUpdate", "initialValue")
                .caseDefinitionKey("oneHumanTaskCase")
                .start();
        Map<String, Object> variables = Stream.of( new ImmutablePair<String, Object>("varToUpdate", "newValue")).collect(
                toMap(Pair::getKey, Pair::getValue)
        );
        cmmnRuntimeService.setVariables(caseInstance.getId(), variables);

        CaseInstance updatedCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().
                caseInstanceId(caseInstance.getId()).
                includeCaseVariables().
                singleResult();
        assertThat(updatedCaseInstance.getCaseVariables(), is(variables));
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
        assertThat(updatedCaseInstance.getCaseVariables(), is(variables));
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
        assertEquals("test", customTestVariable.someValue);
        assertEquals(123, customTestVariable.someInt);
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

        Assertions.assertThat(customVar.getProcessInstanceId())
            .as("custom var process instance id")
            .isNull();

        Assertions.assertThat(customVar.getExecutionId())
            .as("custom var execution id")
            .isNull();

        Assertions.assertThat(customVar.getTaskId())
            .as("custom var task id")
            .isNull();

        Assertions.assertThat(customVar.getScopeId())
            .as("custom var scope id")
            .isEqualTo(caseInstance.getId());

        Assertions.assertThat(customVar.getSubScopeId())
            .as("custom var sub scope id")
            .isNull();

        Assertions.assertThat(customVar.getScopeType())
            .as("custom var scope type")
            .isEqualTo(ScopeTypes.CMMN);

        customVar = (CustomAccessCaseType) cmmnRuntimeService.getVariable(caseInstance.getId(), "customVar");

        Assertions.assertThat(customVar.getProcessInstanceId())
            .as("custom var process instance id")
            .isNull();

        Assertions.assertThat(customVar.getExecutionId())
            .as("custom var execution id")
            .isNull();

        Assertions.assertThat(customVar.getTaskId())
            .as("custom var task id")
            .isNull();

        Assertions.assertThat(customVar.getScopeId())
            .as("custom var scope id")
            .isEqualTo(caseInstance.getId());

        Assertions.assertThat(customVar.getSubScopeId())
            .as("custom var sub scope id")
            .isNull();

        Assertions.assertThat(customVar.getScopeType())
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

        Assertions.assertThat(task).isNotNull();

        CustomAccessCaseType customVar = new CustomAccessCaseType();
        cmmnTaskService.setVariableLocal(task.getId(), "customTaskVar", customVar);

        Assertions.assertThat(customVar.getProcessInstanceId())
            .as("custom var process instance id")
            .isNull();

        Assertions.assertThat(customVar.getExecutionId())
            .as("custom var execution id")
            .isNull();

        Assertions.assertThat(customVar.getTaskId())
            .as("custom var task id")
            .isEqualTo(task.getId());

        Assertions.assertThat(customVar.getScopeId())
            .as("custom var scope id")
            .isEqualTo(caseInstance.getId());

        Assertions.assertThat(customVar.getSubScopeId())
            .as("custom var sub scope id")
            .isEqualTo(task.getSubScopeId());

        Assertions.assertThat(customVar.getScopeType())
            .as("custom var scope type")
            .isEqualTo(ScopeTypes.CMMN);

        customVar = (CustomAccessCaseType) cmmnTaskService.getVariableLocal(task.getId(), "customTaskVar");

        Assertions.assertThat(customVar.getProcessInstanceId())
            .as("custom var process instance id")
            .isNull();

        Assertions.assertThat(customVar.getExecutionId())
            .as("custom var execution id")
            .isNull();

        Assertions.assertThat(customVar.getTaskId())
            .as("custom var task id")
            .isEqualTo(task.getId());

        Assertions.assertThat(customVar.getScopeId())
            .as("custom var scope id")
            .isEqualTo(caseInstance.getId());

        Assertions.assertThat(customVar.getSubScopeId())
            .as("custom var sub scope id")
            .isEqualTo(task.getSubScopeId());

        Assertions.assertThat(customVar.getScopeType())
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

        Assertions.assertThat(planItemInstance).isNotNull();

        CustomAccessCaseType customVar = new CustomAccessCaseType();
        cmmnRuntimeService.setLocalVariable(planItemInstance.getId(), "customPlanItemVar", customVar);

        Assertions.assertThat(customVar.getProcessInstanceId())
            .as("custom var process instance id")
            .isNull();

        Assertions.assertThat(customVar.getExecutionId())
            .as("custom var execution id")
            .isNull();

        Assertions.assertThat(customVar.getTaskId())
            .as("custom var task id")
            .isNull();

        Assertions.assertThat(customVar.getScopeId())
            .as("custom var scope id")
            .isEqualTo(caseInstance.getId());

        Assertions.assertThat(customVar.getSubScopeId())
            .as("custom var sub scope id")
            .isEqualTo(planItemInstance.getId());

        Assertions.assertThat(customVar.getScopeType())
            .as("custom var scope type")
            .isEqualTo(ScopeTypes.CMMN);

        customVar = (CustomAccessCaseType) cmmnRuntimeService.getLocalVariable(planItemInstance.getId(), "customPlanItemVar");

        Assertions.assertThat(customVar.getProcessInstanceId())
            .as("custom var process instance id")
            .isNull();

        Assertions.assertThat(customVar.getExecutionId())
            .as("custom var execution id")
            .isNull();

        Assertions.assertThat(customVar.getTaskId())
            .as("custom var task id")
            .isNull();

        Assertions.assertThat(customVar.getScopeId())
            .as("custom var scope id")
            .isEqualTo(caseInstance.getId());

        Assertions.assertThat(customVar.getSubScopeId())
            .as("custom var sub scope id")
            .isEqualTo(planItemInstance.getId());

        Assertions.assertThat(customVar.getScopeType())
            .as("custom var scope type")
            .isEqualTo(ScopeTypes.CMMN);
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
