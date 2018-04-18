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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemJavaDelegate;
import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class VariablesTest extends FlowableCmmnTestCase {

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

    // Test helper classes

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

}
