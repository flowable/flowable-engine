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

import java.io.Serializable;
import java.util.List;

import org.flowable.cmmn.engine.runtime.CaseInstance;
import org.flowable.cmmn.engine.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.engine.common.impl.util.CollectionUtil;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class IfPartTest extends FlowableCmmnTestCase {
    
    @Test
    @CmmnDeployment
    public void testIfPartOnly() {
        // Passing variable from the start
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testIfPartOnly")
                .variable("variable", true)
                .start();
        assertEquals(2, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count());
        
        // Passing variable after case instance start
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testIfPartOnly")
                .start();
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().list();
        assertEquals(1, planItemInstances.size());
        cmmnRuntimeService.setVariables(caseInstance.getId(), CollectionUtil.singletonMap("variable", true));
        assertEquals(2, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count());
        
        // Completing A after start should end the case instance
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testIfPartOnly")
                .start();
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().list();
        assertEquals(1, planItemInstances.size());
        assertEquals("A", planItemInstances.get(0).getName());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testOnAndIfPart() {
        // Passing the variable for the if part condition at start
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("testSimpleCondition")
            .variable("conditionVariable", true)
            .start();
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateActive().list();
        assertEquals(1, planItemInstances.size());
        assertEquals("A", planItemInstances.get(0).getName());
        
        // Completing plan item A should trigger B
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateActive().list();
        assertEquals(1, planItemInstances.size());
        assertEquals("B", planItemInstances.get(0).getName());
        
        // Completing B should end the case instance
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testIfPartConditionTriggerOnSetVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testSimpleCondition").start();
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateActive().list();
        assertEquals(1, planItemInstances.size());
        assertEquals("A", planItemInstances.get(0).getName());
        
        // Competing plan item A should not trigger B
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testManualEvaluateCriteria() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testManualEvaluateCriteria")
                .variable("someBean", new TestBean())
                .start();
        
        // Triggering the evaluation twice will satisfy the entry criterion for B
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count());
        TestBean.RETURN_VALUE = true;
        cmmnRuntimeService.evaluateCriteria(caseInstance.getId());
        assertEquals(2, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceStateActive().count());
    }
    
    @Test
    @CmmnDeployment
    public void testMultipleOnParts() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testMultipleOnParts")
                .variable("conditionVariable", true)
                .start();
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateActive().orderByName().asc().list();
        assertEquals(3, planItemInstances.size());
        assertEquals("A", planItemInstances.get(0).getName());
        assertEquals("B", planItemInstances.get(1).getName());
        assertEquals("C", planItemInstances.get(2).getName());
        
        for (PlanItemInstance planItemInstance : planItemInstances) {
            cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        }
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateActive().singleResult();
        assertEquals("D", planItemInstance.getName());
    }
    
    @Test
    @CmmnDeployment
    public void testEntryAndExitConditionBothSatisfied() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("testEntryAndExitConditionBothSatisfied")
            .start();
        assertNull(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateActive().planItemInstanceName("A").singleResult());
        assertNotNull(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateActive().planItemInstanceName("B").singleResult());
        
        // Setting the variable will trigger the entry condition of A and the exit condition of B
        cmmnRuntimeService.setVariables(caseInstance.getId(), CollectionUtil.singletonMap("conditionVariable", true));
        
        assertNotNull(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateActive().planItemInstanceName("A").singleResult());
        assertNull(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceStateActive().planItemInstanceName("B").singleResult());
    }
    
    public static class TestBean implements Serializable {
        
        public static boolean RETURN_VALUE;
        
        public boolean isSatisfied() {
            return RETURN_VALUE;
        }
        
    }

}
