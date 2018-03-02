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

import java.util.List;
import java.util.Map;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.engine.common.impl.history.HistoryLevel;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.type.VariableScopeType;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class CmmnScriptTaskTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testSimpleScriptTaskCase() {
        System.out.println("==== START testSimpleScriptTaskCase"); //TODO remove

//        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
//                .caseDefinitionKey("scriptCase")
//                .start();
//        assertNotNull(caseInstance);

//        System.out.println("==== GET PLANITEM INSTANCE"); //TODO remove
//        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
//                .caseInstanceId(caseInstance.getId())
//                .planItemInstanceStateActive()
//                .singleResult();
//        assertNotNull(planItemInstance);
//        assertEquals("Plan Item One", planItemInstance.getName());
        
//        System.out.println("==== START PLANITEM EXECUTION??"); //TODO remove
//        //cmmnRuntimeService.startPlanItemInstance(planItemInstance.getId());
//
//        System.out.println("==== GET PLANITEM INSTANCE"); //TODO remove
//        Map<String, Object> variablesBefore = cmmnRuntimeService.getVariables(caseInstance.getId());
//
//        
//        List<HistoricVariableInstance> listv = cmmnHistoryService.createHistoricVariableInstanceQuery()
//                .caseInstanceId(caseInstance.getId())
//                .list();
//
//        listv.forEach(v -> System.out.println("Name: " + v.getVariableName() + " Type:" + v.getVariableTypeName()));

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
//    
//    @Test
//    @CmmnDeployment
//    public void testOneHumanTaskExpressionCase() {
//        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
//                        .caseDefinitionKey("oneHumanTaskCase")
//                        .variable("var1", "A")
//                        .variable("var2", "YES")
//                        .start();
//        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
//        assertNotNull(task);
//        assertEquals("The Task A", task.getName());
//        assertEquals("This is a test YES", task.getDescription());
//        assertEquals("johnDoe", task.getAssignee());
//        
//        cmmnTaskService.complete(task.getId());
//        assertCaseInstanceEnded(caseInstance);
//        
//        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
//            HistoricTaskInstance historicTaskInstance = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
//            assertNotNull(historicTaskInstance);
//            assertEquals("The Task A", historicTaskInstance.getName());
//            assertEquals("This is a test YES", historicTaskInstance.getDescription());
//            assertNotNull(historicTaskInstance.getEndTime());
//        }
//    }
//    
//    @Test
//    @CmmnDeployment
//    public void testTriggerOneHumanTaskCaseProgrammatically() {
//        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
//        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
//        
//        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateActive().singleResult();
//        assertEquals(planItemInstance.getId(), task.getSubScopeId());
//        assertEquals(planItemInstance.getCaseInstanceId(), task.getScopeId());
//        assertEquals(planItemInstance.getCaseDefinitionId(), task.getScopeDefinitionId());
//        assertEquals(VariableScopeType.CMMN, task.getScopeType());
//        
//        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
//        assertEquals(0, cmmnTaskService.createTaskQuery().count());
//        assertCaseInstanceEnded(caseInstance);
//    }
//
}
