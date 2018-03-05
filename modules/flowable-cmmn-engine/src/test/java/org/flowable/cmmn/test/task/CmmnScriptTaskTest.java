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
import org.junit.Test;

import java.io.Serializable;

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

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertNotNull(planItemInstance);
        assertEquals("Plan Item One", planItemInstance.getName());
        assertEquals("taskA", planItemInstance.getPlanItemDefinitionId());
        assertFalse(planItemInstance.isCompleteable());
        Map<String, Object> variables = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertNotNull(variables);
        assertTrue(variables.isEmpty());
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
        assertThat(result, instanceOf(Double.class));
        assertEquals(7.0, result);
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
        assertThat(result, instanceOf(Double.class));
        assertEquals(10.0, result);
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
        assertThat(result, instanceOf(Double.class));
        assertEquals(12.0, result);

        assertTrue(cmmnRuntimeService.hasVariable(caseInstance.getId(), "a"));
        Object a = cmmnRuntimeService.getVariable(caseInstance.getId(), "a");
        assertThat(a, instanceOf(IntValueHolder.class));
        assertEquals(5, ((IntValueHolder) a).getValue());
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


/*
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertNotNull(planItemInstance);
        assertEquals("returnValueScriptTask", planItemInstance.getPlanItemDefinitionId());

        System.out.printf("=== PLAN INSTANCE STATE %s%n", planItemInstance.getState());
        System.out.printf("=== PLAN INSTANCE DEFINITION ID %s%n", planItemInstance.getPlanItemDefinitionId());
        System.out.printf("=== PLAN INSTANCE DEFINITION TYPE %s%n", planItemInstance.getPlanItemDefinitionType());
        System.out.printf("=== PLAN INSTANCE IS COMPLETABLE %s%n", planItemInstance.isCompleteable());


        //assertThat(planItemInstance, instanceOf(ScriptServiceTask.class));

        Map<String, Object> variablesBefore = cmmnRuntimeService.getVariables(caseInstance.getId());

        List<HistoricVariableInstance> listv = cmmnHistoryService.createHistoricVariableInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();

        listv.forEach(v -> System.out.println("===> VAR Name: " + v.getVariableName() + " Type:" + v.getVariableTypeName()));
*/

//        HistoricCaseInstance caseHistory = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
//        List<HistoricTaskInstance> list = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseHistory.getId()).list();
//        System.out.println("==== TASK HISTORY");
//        list.forEach(t -> System.out.printf("TASK ID:%s%n", t.getId()));
//        deploymentId = cmmnRepositoryService.createDeployment()
//                .addClasspathResource("org/flowable/cmmn/test/one-human-task-model.cmmn")
//                .deploy()
//                .getId();
//        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
//        assertNotNull(task);
//        assertEquals("Script Task Item", task.getName());
//        assertEquals("This is a test documentation", task.getDescription());
//        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
//            HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
//            assertNotNull(historicTaskInstance);
//            assertNull(historicTaskInstance.getEndTime());
//        }
//
//        cmmnTaskService.complete(task.getId());
//        assertCaseInstanceEnded(caseInstance);
//
//        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
//            HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
//            assertNotNull(historicTaskInstance);
//            assertEquals("The Task", historicTaskInstance.getName());
//            assertEquals("This is a test documentation", historicTaskInstance.getDescription());
//            assertNotNull(historicTaskInstance.getEndTime());
//        }

}
