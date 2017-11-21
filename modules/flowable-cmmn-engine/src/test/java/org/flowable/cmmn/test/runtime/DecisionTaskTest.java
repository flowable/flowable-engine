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

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnRule;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author martin.grofcik
 */
public class DecisionTaskTest {

    @Rule
    public FlowableCmmnRule cmmnRule = new FlowableCmmnRule("org/flowable/cmmn/test/runtime/DecisionTaskTest.cfg.xml");
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    @CmmnDeployment(
            resources = { "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.cmmn",
                          "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.dmn"
            }
    )
    public void testDecisionServiceTask() {
        CaseInstance caseInstance = cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                        .caseDefinitionKey("myCase")
                        .variable("test", "test2")
                        .start();

        assertResultVariable(caseInstance);
    }

    @Test
    @CmmnDeployment(
            resources = { "org/flowable/cmmn/test/runtime/DecisionTaskTest.testExpressionReferenceKey.cmmn",
                          "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.dmn"
            }
    )
    public void testExpressionReferenceKey() {
        CaseInstance caseInstance = cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                        .caseDefinitionKey("myCase")
                        .variable("test", "test2")
                        .variable("referenceKey", "decisionTable")
                        .start();

        assertResultVariable(caseInstance);
    }

    @Test
    @CmmnDeployment(
            resources = { "org/flowable/cmmn/test/runtime/DecisionTaskTest.testExpressionReferenceKey.cmmn"}
    )
    public void testNullReferenceKey() {
        this.expectedException.expect(FlowableIllegalArgumentException.class);
        this.expectedException.expectMessage("Reference key expression must not be resolved to null");

        cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                        .caseDefinitionKey("myCase")
                        .variable("test", "test2")
                        .variable("referenceKey", null)
                        .start();
    }

    @Test
    @CmmnDeployment(
            resources = { "org/flowable/cmmn/test/runtime/DecisionTaskTest.testExpressionReferenceKey.cmmn"}
    )
    public void testNonStringReferenceKey() {
        this.expectedException.expect(FlowableIllegalArgumentException.class);
        this.expectedException.expectMessage("Reference key expression must be resolved to String.");

        cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                        .caseDefinitionKey("myCase")
                        .variable("test", "test2")
                        .variable("referenceKey", 1)
                        .start();
    }

    @Test
    @CmmnDeployment(
            resources = { "org/flowable/cmmn/test/runtime/DecisionTaskTest.testDecisionServiceTask.cmmn"}
    )
    public void testNonExistingReferenceKey() {
        this.expectedException.expect(FlowableObjectNotFoundException.class);
        this.expectedException.expectMessage("no decisions deployed with key 'decisionTable'");

        cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                        .caseDefinitionKey("myCase")
                        .start();
    }

    protected void assertResultVariable(CaseInstance caseInstance) {
        assertNotNull(caseInstance);

        PlanItemInstance planItemInstance = cmmnRule.getCmmnRuntimeService().createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertNotNull(planItemInstance);

        assertEquals("executed", cmmnRule.getCmmnRuntimeService().getVariable(caseInstance.getId(), "resultVar"));

        // Triggering the task should end the case instance
        cmmnRule.getCmmnRuntimeService().triggerPlanItemInstance(planItemInstance.getId());
        assertEquals(0, cmmnRule.getCmmnRuntimeService().createCaseInstanceQuery().count());

        assertEquals("executed", cmmnRule.getCmmnHistoryService().createHistoricVariableInstanceQuery()
                        .caseInstanceId(caseInstance.getId())
                        .variableName("resultVar")
                        .singleResult().getValue());
    }

}
