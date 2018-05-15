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
package org.flowable.cmmn.test.task;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Test;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

/**
 * @author Dennis Federico
 */
public class CmmnScriptTaskTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testSimpleScript() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("scriptCase")
                .start();
        assertNotNull(caseInstance);

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("planItemTaskA").singleResult();
        assertNotNull(planItemInstance);
        assertEquals("Plan Item One", planItemInstance.getName());
        assertEquals("taskA", planItemInstance.getPlanItemDefinitionId());
        assertCaseInstanceNotEnded(caseInstance);

        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("blockerPlanItem").singleResult();
        assertNotNull(planItemInstance);
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertCaseInstanceEnded(caseInstance);

        Map<String, Object> variables = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertNotNull(variables);
        assertTrue(variables.isEmpty());
    }

    @Test
    @CmmnDeployment
    public void testPlanItemInstanceVarScopeAndVarHistory() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("scriptCase")
                .start();
        assertNotNull(caseInstance);

        //Keep the ScriptTaskPlanItem id to check the historic scope later
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("planItemTaskA").singleResult();
        assertNotNull(planItemInstance);
        String scriptTaskPlanInstanceId = planItemInstance.getId();

        //No variables set and no variables created yet by the script, because it has not run
        assertTrue(cmmnRuntimeService.getVariables(caseInstance.getId()).isEmpty());
        assertTrue(cmmnRuntimeService.getLocalVariables(scriptTaskPlanInstanceId).isEmpty());

        //Initialize one of the script variables to set its scope local to the planItemInstance, otherwise it goes up in the hierarchy up to the case by default
        cmmnRuntimeService.setLocalVariable(scriptTaskPlanInstanceId, "aString", "VALUE TO OVERWRITE");
        assertEquals(1, cmmnRuntimeService.getLocalVariables(scriptTaskPlanInstanceId).size());
        assertTrue(cmmnRuntimeService.getVariables(caseInstance.getId()).isEmpty());

        //Trigger the task entry event
        PlanItemInstance blocker = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("taskBlocker").singleResult();
        assertNotNull(blocker);
        cmmnRuntimeService.triggerPlanItemInstance(blocker.getId());

        //The case has not ended yet
        assertCaseInstanceNotEnded(caseInstance);

        //Check the case variables, one will be created by the script execution
        Map<String, Object> caseVariables = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertNotNull(caseVariables);
        assertEquals(1, caseVariables.size());
        assertTrue(cmmnRuntimeService.hasVariable(caseInstance.getId(), "aInt"));
        Object integer = cmmnRuntimeService.getVariable(caseInstance.getId(), "aInt");
        assertThat(integer, instanceOf(Integer.class));
        assertEquals(5, integer);

        //On the other hand the variable with scope local to the planItem instance cannot be found since the instance is not in scope
        //only through the historyService
        Map<String, Object> localVariables = cmmnRuntimeService.getLocalVariables(scriptTaskPlanInstanceId);
        assertNotNull(localVariables);
        assertTrue(localVariables.isEmpty());

        //The planItemInstance scope variable is available on the history service
        List<HistoricVariableInstance> historicVariables = cmmnHistoryService.createHistoricVariableInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceId(scriptTaskPlanInstanceId)
                .list();
        assertNotNull(historicVariables);
        assertEquals(1, historicVariables.size());

        HistoricVariableInstance planItemInstanceVariable = historicVariables.get(0);
        assertNotNull(planItemInstanceVariable);
        assertEquals("aString", planItemInstanceVariable.getVariableName());
        assertEquals("string", planItemInstanceVariable.getVariableTypeName());
        assertEquals("value set in the script", planItemInstanceVariable.getValue());
        assertEquals(scriptTaskPlanInstanceId, planItemInstanceVariable.getSubScopeId());

        endTestCase();
        assertCaseInstanceEnded(caseInstance);

        //Both variables are still in the history
        historicVariables = cmmnHistoryService.createHistoricVariableInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertNotNull(historicVariables);
        assertEquals(2, historicVariables.size());

        HistoricVariableInstance caseScopeVariable = historicVariables.stream().filter(v -> v.getSubScopeId() == null).findFirst().get();
        assertEquals("aInt", caseScopeVariable.getVariableName());
        assertEquals("integer", caseScopeVariable.getVariableTypeName());
        assertEquals(5, caseScopeVariable.getValue());

        HistoricVariableInstance planItemScopeVariable = historicVariables.stream().filter(v -> v.getSubScopeId() != null).findFirst().get();
        assertEquals("aString", planItemScopeVariable.getVariableName());
        assertEquals("string", planItemScopeVariable.getVariableTypeName());
        assertEquals("value set in the script", planItemScopeVariable.getValue());
    }

    @Test
    @CmmnDeployment
    public void testSimpleResult() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("scriptCase")
                .start();
        assertNotNull(caseInstance);

        assertTrue(cmmnRuntimeService.hasVariable(caseInstance.getId(), "scriptResult"));
        Object result = cmmnRuntimeService.getVariable(caseInstance.getId(), "scriptResult");
        assertThat(result, instanceOf(Number.class));
        assertEquals(7, ((Number)result).intValue());

        assertCaseInstanceNotEnded(caseInstance);
        endTestCase();
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testVariableResult() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("scriptCase")
                .variable("a", 3)
                .variable("b", 7)
                .start();
        assertNotNull(caseInstance);

        assertTrue(cmmnRuntimeService.hasVariable(caseInstance.getId(), "scriptResult"));
        Object result = cmmnRuntimeService.getVariable(caseInstance.getId(), "scriptResult");
        assertThat(result, instanceOf(Number.class));
        assertEquals(10, ((Number)result).intValue());

        assertCaseInstanceNotEnded(caseInstance);
        endTestCase();
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testObjectVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("scriptCase")
                .variable("a", new IntValueHolder(3))
                .variable("b", 7)
                .start();
        assertNotNull(caseInstance);

        assertTrue(cmmnRuntimeService.hasVariable(caseInstance.getId(), "scriptResult"));
        Object result = cmmnRuntimeService.getVariable(caseInstance.getId(), "scriptResult");
        assertThat(result, instanceOf(Number.class));
        assertEquals(12, ((Number)result).intValue());

        assertTrue(cmmnRuntimeService.hasVariable(caseInstance.getId(), "a"));
        Object a = cmmnRuntimeService.getVariable(caseInstance.getId(), "a");
        assertThat(a, instanceOf(IntValueHolder.class));
        assertEquals(5, ((IntValueHolder) a).getValue());

        assertCaseInstanceNotEnded(caseInstance);
        endTestCase();
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testReturnJavaObject() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("scriptCase")
                .variable("a", new IntValueHolder(3))
                .variable("b", 7)
                .start();
        assertNotNull(caseInstance);

        assertTrue(cmmnRuntimeService.hasVariable(caseInstance.getId(), "scriptResult"));
        Object result = cmmnRuntimeService.getVariable(caseInstance.getId(), "scriptResult");
        assertThat(result, instanceOf(IntValueHolder.class));
        assertEquals(10, ((IntValueHolder) result).getValue());

        assertCaseInstanceNotEnded(caseInstance);
        endTestCase();
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testGroovyAutoStoreVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("scriptCase")
                .variable("inputArray", new int[] { 1, 2, 3, 4, 5 })
                .start();
        assertNotNull(caseInstance);

        Map<String, Object> variables = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertNotNull(variables);
        assertEquals(2,variables.size());

        assertTrue(cmmnRuntimeService.hasVariable(caseInstance.getId(), "sum"));
        Object result = cmmnRuntimeService.getVariable(caseInstance.getId(), "sum");
        assertThat(result, instanceOf(Integer.class));
        assertEquals(15, ((Number)result).intValue());

        assertCaseInstanceNotEnded(caseInstance);
        endTestCase();
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testGroovyNoAutoStoreVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("scriptCase")
                .variable("inputArray", new int[] { 1, 2, 3, 4, 5 })
                .start();
        assertNotNull(caseInstance);

        Map<String, Object> variables = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertNotNull(variables);
        assertEquals(1,variables.size());

        assertFalse(cmmnRuntimeService.hasVariable(caseInstance.getId(), "sum"));

        assertCaseInstanceNotEnded(caseInstance);
        endTestCase();
        assertCaseInstanceEnded(caseInstance);
    }

    private void endTestCase() {
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("blockerPlanItem").singleResult();
        assertNotNull(planItemInstance);
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
    }

    public static class IntValueHolder implements Serializable {
        private int value;

        public IntValueHolder(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "IntValueHolder{" +
                    "value=" + value +
                    '}';
        }
    }
}
