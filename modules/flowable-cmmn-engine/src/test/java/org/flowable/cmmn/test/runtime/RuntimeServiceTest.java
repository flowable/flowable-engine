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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricCaseInstanceQuery;
import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceQuery;
import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.api.runtime.MilestoneInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class RuntimeServiceTest extends FlowableCmmnTestCase {

    private static final Map<String, Object> VARIABLES = new HashMap<>();
    static {
        VARIABLES.put("var", "test");
        VARIABLES.put("numberVar", 10);
    }

    @Test
    @CmmnDeployment
    public void testStartSimplePassthroughCase() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().singleResult();
        assertEquals("myCase", caseDefinition.getKey());
        assertNotNull(caseDefinition.getResourceName());
        assertNotNull(caseDefinition.getDeploymentId());
        assertTrue(caseDefinition.getVersion() > 0);

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionId(caseDefinition.getId()).start();
        assertNotNull(caseInstance);
        assertEquals(caseDefinition.getId(), caseInstance.getCaseDefinitionId());
        assertNotNull(caseInstance.getStartTime());
        assertEquals(CaseInstanceState.COMPLETED, caseInstance.getState());

        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());

        List<HistoricMilestoneInstance> milestoneInstances = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId())
                .orderByMilestoneName().asc()
                .list();
        assertEquals(2, milestoneInstances.size());
        assertEquals("PlanItem Milestone One", milestoneInstances.get(0).getName());
        assertEquals("PlanItem Milestone Two", milestoneInstances.get(1).getName());
    }

    @Test
    @CmmnDeployment
    public void testStartSimplePassthroughCaseWithBlockingTask() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().singleResult();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionId(caseDefinition.getId()).start();
        assertEquals(CaseInstanceState.ACTIVE, caseInstance.getState());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());

        assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count());

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertNotNull(planItemInstance);
        assertEquals("Task A", planItemInstance.getName());

        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        List<MilestoneInstance> mileStones = cmmnRuntimeService.createMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId())
                .list();
        assertEquals(1, mileStones.size());
        assertEquals("PlanItem Milestone One", mileStones.get(0).getName());

        List<HistoricMilestoneInstance> historicMilestoneInstances = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId())
                .list();
        assertEquals(1, historicMilestoneInstances.size());
        assertEquals("PlanItem Milestone One", historicMilestoneInstances.get(0).getName());

        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertNotNull(planItemInstance);
        assertEquals("Task B", planItemInstance.getName());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
        assertEquals(2, cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count());

        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNotNull(historicCaseInstance);
            assertNotNull(historicCaseInstance.getStartTime());
            assertNotNull(historicCaseInstance.getEndTime());
            assertEquals(CaseInstanceState.COMPLETED, historicCaseInstance.getState());
        }
    }

    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/runtime/RuntimeServiceTest.testStartSimplePassthroughCaseWithBlockingTask.cmmn"})
    public void testVariableQueryWithBlockingTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("myCase")
                        .variables(VARIABLES)
                        .start();

        assertNonEmptyQueryIncludeVariables(1, VARIABLES,
                cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("var", "test"),
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEquals("var", "test")
        );
        assertEmptyQuery(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("var", "test2"),
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEquals("var", "test2")
        );
        assertNonEmptyQueryIncludeVariables(1, VARIABLES,
                cmmnRuntimeService.createCaseInstanceQuery().variableValueEqualsIgnoreCase("var", "TEST"),
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEqualsIgnoreCase("var", "TEST")
        );
        assertEmptyQuery(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueEqualsIgnoreCase("var", "TEST2"),
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEqualsIgnoreCase("var", "TEST2")
        );
        assertNonEmptyQueryIncludeVariables(1, VARIABLES,
                cmmnRuntimeService.createCaseInstanceQuery().variableValueNotEquals("var", "test2"),
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueNotEquals("var", "test2")
        );
        assertEmptyQuery(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueNotEquals("var", "test"),
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueNotEquals("var", "test")
        );
        assertNonEmptyQueryIncludeVariables(1, VARIABLES,
                cmmnRuntimeService.createCaseInstanceQuery().variableValueNotEqualsIgnoreCase("var", "TEST2"),
                null
        );
        assertEmptyQuery(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueNotEqualsIgnoreCase("var", "TEST"),
                null
        );
        assertNonEmptyQueryIncludeVariables(1, VARIABLES,
                cmmnRuntimeService.createCaseInstanceQuery().variableValueLike("var", "te%"),
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLike("var", "te%")
        );
        assertEmptyQuery(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueLike("var", "te2%"),
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLike("var", "te2%")
        );
        assertNonEmptyQueryIncludeVariables(1, VARIABLES,
                cmmnRuntimeService.createCaseInstanceQuery().variableValueLikeIgnoreCase("var", "TE%"),
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLikeIgnoreCase("var", "TE%")
        );
        assertEmptyQuery(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueLikeIgnoreCase("var", "TE2%"),
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLikeIgnoreCase("var", "TE2%")
        );
        assertNonEmptyQueryIncludeVariables(1, VARIABLES,
                cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThan("numberVar", 5),
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueGreaterThan("numberVar", 5)
        );
        assertEmptyQuery(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThan("numberVar", 11),
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueGreaterThan("numberVar", 11)
        );
        assertNonEmptyQueryIncludeVariables(1, VARIABLES,
                cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThanOrEqual("numberVar", 10),
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueGreaterThanOrEqual("numberVar", 10)
        );
        assertEmptyQuery(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThanOrEqual("numberVar", 11),
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueGreaterThanOrEqual("numberVar", 11)
        );
        assertNonEmptyQueryIncludeVariables(1, VARIABLES,
                cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThan("numberVar", 20),
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLessThan("numberVar", 20)
        );
        assertEmptyQuery(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThan("numberVar", 5),
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLessThan("numberVar", 5)
        );
        assertNonEmptyQueryIncludeVariables(1, VARIABLES,
                cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThanOrEqual("numberVar", 10),
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLessThanOrEqual("numberVar", 10)
        );
        assertEmptyQuery(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThanOrEqual("numberVar", 9),
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLessThanOrEqual("numberVar", 9)
        );

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertNotNull(planItemInstance);
        assertEquals("Task A", planItemInstance.getName());

        Map<String, Object> varMap = new HashMap<>();
        varMap.put("numberVar", 11);
        cmmnRuntimeService.setVariables(caseInstance.getId(), varMap);

        Map<String, Object> localVarMap = new HashMap<>();
        localVarMap.put("localVar", "test");
        cmmnRuntimeService.setLocalVariables(planItemInstance.getId(), localVarMap);

        Map<String, Object> updatedVariables = new HashMap<>(VARIABLES);
        updatedVariables.put("numberVar", 11);

        assertNonEmptyQueryIncludeVariables(1, updatedVariables,
                cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThan("numberVar", 10),
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueGreaterThan("numberVar", 10)
        );
        assertEmptyQuery(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThan("numberVar", 11),
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueGreaterThan("numberVar", 11)
        );
        assertNonEmptyQueryIncludeVariables(1, updatedVariables,
                cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThanOrEqual("numberVar", 11),
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueGreaterThanOrEqual("numberVar", 11)
        );
        assertEmptyQuery(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThanOrEqual("numberVar", 12),
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueGreaterThanOrEqual("numberVar", 12)
        );

        assertEmptyQuery(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("localVar", "test"),
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEquals("localVar", "test")
        );

        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceState(PlanItemInstanceState.ACTIVE).singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        assertEmptyQuery(
                cmmnRuntimeService.createCaseInstanceQuery(),
                cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished()
        );
    }

    private void assertNonEmptyQueryIncludeVariables(int expectedCount, Map<String, Object> expectedVariables,
                                                     CaseInstanceQuery caseInstanceQuery,
                                                     HistoricCaseInstanceQuery historicCaseInstanceQuery) {
        assertEquals(expectedCount, caseInstanceQuery.count());
        if (historicCaseInstanceQuery != null) {
            assertEquals(expectedCount, historicCaseInstanceQuery.count());
        }
        // assert singleResult
        if (expectedCount == 1) {
            CaseInstance fetchedCaseInstance = caseInstanceQuery.includeCaseVariables().singleResult();
            assertThat(fetchedCaseInstance.getCaseVariables(), is(expectedVariables));
            if (historicCaseInstanceQuery != null) {
                HistoricCaseInstance fetchedHistoricCaseInstance = historicCaseInstanceQuery.includeCaseVariables().singleResult();
                assertThat(fetchedHistoricCaseInstance.getCaseVariables(), is(expectedVariables));
            }
        } else if (expectedCount > 1) {
            try {
                caseInstanceQuery.includeCaseVariables().singleResult();
                fail("Exception expected");
            } catch (FlowableException e) {
                assertThat(e.getMessage(), is("Query return " + expectedCount + " results instead of max 1"));
            }
            if (historicCaseInstanceQuery != null) {
                try {
                    historicCaseInstanceQuery.includeCaseVariables().singleResult();
                    fail("Exception expected");
                } catch (FlowableException e) {
                    assertThat(e.getMessage(), is("Query return " + expectedCount + " results instead of max 1"));
                }
            }
        }

        // assert query list
        List<CaseInstance> caseInstances = caseInstanceQuery.includeCaseVariables().list();
        assertThat(caseInstances.size(), is(expectedCount));
        for (CaseInstance caseInstance : caseInstances) {
            assertThat(caseInstance.getCaseVariables(), is(expectedVariables));
        }
        if (historicCaseInstanceQuery != null) {
            List<HistoricCaseInstance> historicCaseInstances = historicCaseInstanceQuery.includeCaseVariables().list();
            assertThat(historicCaseInstances.size(), is(expectedCount));
            for (HistoricCaseInstance historicCaseInstance : historicCaseInstances) {
                assertThat(historicCaseInstance.getCaseVariables(), is(expectedVariables));
            }
        }
    }


    private void assertPaginationQueryIncludeVariables(int expectedStart, int expectedCount, List<CaseInstance> caseInstances) {
        assertThat(caseInstances.size(), is(expectedCount));
        Map<String, Object> expectedVariables = new HashMap<>(VARIABLES);
        for (int i = 0; i < expectedCount; i++) {
            CaseInstance caseInstance = caseInstances.get(i);
            expectedVariables.put("counter", expectedStart + i);
            assertThat(caseInstance.getName(), is("A" + (expectedStart + i)));
            assertThat(caseInstance.getCaseVariables(), is(expectedVariables));
        }
    }

    private void assertPaginationHistoricQueryIncludeVariables(int expectedStart, int expectedCount, List<HistoricCaseInstance> caseInstances) {
        assertThat(caseInstances.size(), is(expectedCount));
        Map<String, Object> expectedVariables = new HashMap<>(VARIABLES);
        for (int i = 0; i < expectedCount; i++) {
            HistoricCaseInstance caseInstance = caseInstances.get(i);
            expectedVariables.put("counter", expectedStart + i);
            assertThat(caseInstance.getName(), is("A" + (expectedStart + i)));
            assertThat(caseInstance.getCaseVariables(), is(expectedVariables));
        }
    }

    private void assertEmptyQuery(CaseInstanceQuery caseInstanceQuery, HistoricCaseInstanceQuery historicCaseInstanceQuery) {
        assertEquals(0, caseInstanceQuery.count());
        assertThat(caseInstanceQuery.includeCaseVariables().singleResult(), is(nullValue()));
        List<CaseInstance> caseInstances = caseInstanceQuery.includeCaseVariables().list();
        assertThat(caseInstances.size(), is(0));

        if (historicCaseInstanceQuery != null) {
            assertEquals(0, historicCaseInstanceQuery.count());
            assertThat(historicCaseInstanceQuery.includeCaseVariables().singleResult(), is(nullValue()));
            List<HistoricCaseInstance> historicCaseInstances = historicCaseInstanceQuery.includeCaseVariables().list();
            assertThat(historicCaseInstances.size(), is(0));
        }
    }

    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/runtime/RuntimeServiceTest.testStartSimplePassthroughCaseWithBlockingTask.cmmn"})
    public void testPlanItemVariableQueryWithBlockingTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("myCase")
                        .variable("var", "test")
                        .variable("numberVar", 10)
                        .start();

        assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueEquals("var", "test").count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueEquals("var", "test2").count());
        assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueEqualsIgnoreCase("var", "TEST").count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueEqualsIgnoreCase("var", "TEST2").count());
        assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueNotEquals("var", "test2").count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueNotEquals("var", "test").count());
        assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueNotEqualsIgnoreCase("var", "TEST2").count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueNotEqualsIgnoreCase("var", "TEST").count());
        assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueLike("var", "te%").count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueLike("var", "te2%").count());
        assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueLikeIgnoreCase("var", "TE%").count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueLikeIgnoreCase("var", "TE2%").count());
        assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueGreaterThan("numberVar", 5).count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueGreaterThan("numberVar", 11).count());
        assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueGreaterThanOrEqual("numberVar", 10).count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueGreaterThanOrEqual("numberVar", 11).count());
        assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueLessThan("numberVar", 20).count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueLessThan("numberVar", 5).count());
        assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueLessThanOrEqual("numberVar", 10).count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueLessThanOrEqual("numberVar", 9).count());

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertNotNull(planItemInstance);
        assertEquals("Task A", planItemInstance.getName());

        Map<String, Object> varMap = new HashMap<>();
        varMap.put("numberVar", 11);
        cmmnRuntimeService.setVariables(caseInstance.getId(), varMap);

        Map<String, Object> localVarMap = new HashMap<>();
        localVarMap.put("localVar", "test");
        localVarMap.put("localNumberVar", 15);
        cmmnRuntimeService.setLocalVariables(planItemInstance.getId(), localVarMap);

        assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueGreaterThan("numberVar", 10).count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueGreaterThan("numberVar", 11).count());
        assertEquals(4, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueGreaterThanOrEqual("numberVar", 11).count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueGreaterThanOrEqual("numberVar", 12).count());

        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueEquals("localVar", "test").count());

        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().variableValueEquals("localVar", "test").count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().variableValueEquals("localVar", "test2").count());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().variableValueEqualsIgnoreCase("localVar", "TEST").count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().variableValueEqualsIgnoreCase("localVar", "TEST2").count());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().variableValueNotEquals("localVar", "test2").count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().variableValueNotEquals("localVar", "test").count());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().variableValueNotEqualsIgnoreCase("localVar", "TEST2").count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().variableValueNotEqualsIgnoreCase("localVar", "TEST").count());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().variableValueLike("localVar", "te%").count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().variableValueLike("localVar", "te2%").count());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().variableValueLikeIgnoreCase("localVar", "TE%").count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().variableValueLikeIgnoreCase("localVar", "TE2%").count());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().variableValueGreaterThan("localNumberVar", 5).count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().variableValueGreaterThan("localNumberVar", 17).count());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().variableValueGreaterThanOrEqual("localNumberVar", 15).count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().variableValueGreaterThanOrEqual("localNumberVar", 16).count());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().variableValueLessThan("localNumberVar", 20).count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().variableValueLessThan("localNumberVar", 5).count());
        assertEquals(1, cmmnRuntimeService.createPlanItemInstanceQuery().variableValueLessThanOrEqual("localNumberVar", 15).count());
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().variableValueLessThanOrEqual("localNumberVar", 9).count());

        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceState(PlanItemInstanceState.ACTIVE).singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
    }

    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/runtime/RuntimeServiceTest.testStartSimplePassthroughCaseWithBlockingTask.cmmn"})
    public void testLocalVariablesWithBlockingTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertNotNull(planItemInstance);
        assertEquals("Task A", planItemInstance.getName());

        Map<String, Object> varMap = new HashMap<>();
        varMap.put("localVar", "test");
        cmmnRuntimeService.setLocalVariables(planItemInstance.getId(), varMap);

        assertEquals("test", cmmnRuntimeService.getLocalVariable(planItemInstance.getId(), "localVar"));
        assertEquals(1, cmmnRuntimeService.getLocalVariables(planItemInstance.getId()).size());
        assertNull(cmmnRuntimeService.getVariable(caseInstance.getId(), "localVar"));

        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertNotNull(planItemInstance);
        assertEquals("Task B", planItemInstance.getName());

        assertNull(cmmnRuntimeService.getLocalVariable(planItemInstance.getId(), "localVar"));

        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
    }

    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/runtime/RuntimeServiceTest.testStartSimplePassthroughCaseWithBlockingTask.cmmn"})
    public void testRemoveLocalVariablesWithBlockingTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertNotNull(planItemInstance);
        assertEquals("Task A", planItemInstance.getName());

        Map<String, Object> localVarMap = new HashMap<>();
        localVarMap.put("localVar", "test");
        cmmnRuntimeService.setLocalVariables(planItemInstance.getId(), localVarMap);

        Map<String, Object> varMap = new HashMap<>();
        varMap.put("var", "test");
        cmmnRuntimeService.setVariables(caseInstance.getId(), varMap);

        assertEquals("test", cmmnRuntimeService.getLocalVariable(planItemInstance.getId(), "localVar"));
        assertEquals(1, cmmnRuntimeService.getLocalVariables(planItemInstance.getId()).size());

        assertEquals("test", cmmnRuntimeService.getVariable(caseInstance.getId(), "var"));
        assertEquals(1, cmmnRuntimeService.getVariables(caseInstance.getId()).size());

        cmmnRuntimeService.removeLocalVariable(planItemInstance.getId(), "localVar");
        assertNull(cmmnRuntimeService.getLocalVariable(planItemInstance.getId(), "localVar"));

        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertNotNull(planItemInstance);
        assertEquals("Task B", planItemInstance.getName());

        assertEquals("test", cmmnRuntimeService.getVariable(caseInstance.getId(), "var"));
        assertEquals(1, cmmnRuntimeService.getVariables(caseInstance.getId()).size());

        cmmnRuntimeService.removeVariable(caseInstance.getId(), "var");
        assertNull(cmmnRuntimeService.getVariable(caseInstance.getId(), "var"));

        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }

    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/runtime/RuntimeServiceTest.testStartSimplePassthroughCaseWithBlockingTask.cmmn"})
    public void testIncludeVariableSingleResultQueryWithBlockingTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variables(VARIABLES)
                .start();

        assertNonEmptyQueryIncludeVariables(1, VARIABLES,
                cmmnRuntimeService.createCaseInstanceQuery().includeCaseVariables(),
                cmmnHistoryService.createHistoricCaseInstanceQuery().includeCaseVariables()
        );

        Map<String, Object> updatedVariables = new HashMap<>(VARIABLES);
        updatedVariables.put("newVar", 14.2);
        cmmnRuntimeService.setVariables(caseInstance.getId(), updatedVariables);

        assertNonEmptyQueryIncludeVariables(1, updatedVariables,
                cmmnRuntimeService.createCaseInstanceQuery().includeCaseVariables(),
                cmmnHistoryService.createHistoricCaseInstanceQuery().includeCaseVariables()
        );
    }

    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/runtime/RuntimeServiceTest.testStartSimplePassthroughCaseWithBlockingTask.cmmn"})
    public void testIncludeVariableListQueryWithBlockingTask() {
        Map<String, Object> variablesA = new HashMap<>();
        variablesA.put("var", "test");
        variablesA.put("numberVar", 10);
        Map<String, Object> variablesB = new HashMap<>();
        variablesB.put("var", "test");
        variablesB.put("numberVar", 10);
        variablesB.put("floatVar", 10.1);

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .name("A")
                .variables(variablesA)
                .start();
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .name("B")
                .variables(variablesB)
                .start();
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .name("C")
                .start();

        List<CaseInstance> caseInstancesWithVariables = cmmnRuntimeService.createCaseInstanceQuery().includeCaseVariables().orderByStartTime().list();
        assertThat(caseInstancesWithVariables.size(), is(3));
        assertThat(caseInstancesWithVariables.get(0).getCaseVariables(), is(variablesA));
        assertThat(caseInstancesWithVariables.get(1).getCaseVariables(), is(variablesB));
        assertThat(caseInstancesWithVariables.get(2).getCaseVariables(), is(Collections.EMPTY_MAP));

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().includeCaseVariables().orderByStartTime().count(), is(3L));

        List<HistoricCaseInstance> historicCaseInstancesWithVariables = cmmnHistoryService.createHistoricCaseInstanceQuery().includeCaseVariables().orderByStartTime().list();
        assertThat(historicCaseInstancesWithVariables.size(), is(3));
        assertThat(historicCaseInstancesWithVariables.get(0).getCaseVariables(), is(variablesA));
        assertThat(historicCaseInstancesWithVariables.get(1).getCaseVariables(), is(variablesB));
        assertThat(historicCaseInstancesWithVariables.get(2).getCaseVariables(), is(Collections.EMPTY_MAP));

        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().includeCaseVariables().orderByStartTime().count(), is(3L));
    }

    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/runtime/RuntimeServiceTest.testStartSimplePassthroughCaseWithBlockingTask.cmmn"})
    public void testIncludeSameVariableListQueryWithBlockingTask() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .name("A")
                .variables(VARIABLES)
                .start();
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .name("B")
                .variables(VARIABLES)
                .start();
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variables(VARIABLES)
                .name("C")
                .start();

        assertNonEmptyQueryIncludeVariables(3, VARIABLES,
                cmmnRuntimeService.createCaseInstanceQuery().orderByStartTime(),
                cmmnHistoryService.createHistoricCaseInstanceQuery().orderByStartTime()
        );
    }

    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/runtime/RuntimeServiceTest.testStartSimplePassthroughCaseWithBlockingTask.cmmn"})
    public void includeVariablesWithPaginationQueries() {
        createCaseInstances();

        testIncludeVariablesWithPagination(cmmnRuntimeService.createCaseInstanceQuery().includeCaseVariables().orderByStartTime());
        testIncludeVariablesWithPagination(cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("var", "test").includeCaseVariables().orderByStartTime());
        testIncludeVariablesWithPagination(cmmnRuntimeService.createCaseInstanceQuery().variableValueEqualsIgnoreCase("var", "TEST").includeCaseVariables().orderByStartTime());
        testIncludeVariablesWithPagination(cmmnRuntimeService.createCaseInstanceQuery().variableValueNotEquals("var", "test2").includeCaseVariables().orderByStartTime());
        testIncludeVariablesWithPagination(cmmnRuntimeService.createCaseInstanceQuery().variableValueNotEqualsIgnoreCase("var", "TEST2").includeCaseVariables().orderByStartTime());
        testIncludeVariablesWithPagination(cmmnRuntimeService.createCaseInstanceQuery().variableValueLike("var", "te%").includeCaseVariables().orderByStartTime());
        testIncludeVariablesWithPagination(cmmnRuntimeService.createCaseInstanceQuery().variableValueLikeIgnoreCase("var", "TE%").includeCaseVariables().orderByStartTime());
        testIncludeVariablesWithPagination(cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThan("numberVar", 5).includeCaseVariables().orderByStartTime());
        testIncludeVariablesWithPagination(cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThanOrEqual("numberVar", 10).includeCaseVariables().orderByStartTime());
        testIncludeVariablesWithPagination(cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThan("numberVar", 20).includeCaseVariables().orderByStartTime());
        testIncludeVariablesWithPagination(cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThanOrEqual("numberVar", 10).includeCaseVariables().orderByStartTime());

        testIncludeVariablesWithPagination(cmmnHistoryService.createHistoricCaseInstanceQuery().includeCaseVariables().orderByStartTime());
        testIncludeVariablesWithPagination(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEquals("var", "test").includeCaseVariables().orderByStartTime());
        testIncludeVariablesWithPagination(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEqualsIgnoreCase("var", "TEST").includeCaseVariables().orderByStartTime());
        testIncludeVariablesWithPagination(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueNotEquals("var", "test2").includeCaseVariables().orderByStartTime());
        testIncludeVariablesWithPagination(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLike("var", "te%").includeCaseVariables().orderByStartTime());
        testIncludeVariablesWithPagination(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLikeIgnoreCase("var", "TE%").includeCaseVariables().orderByStartTime());
        testIncludeVariablesWithPagination(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueGreaterThan("numberVar", 5).includeCaseVariables().orderByStartTime());
        testIncludeVariablesWithPagination(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueGreaterThanOrEqual("numberVar", 10).includeCaseVariables().orderByStartTime());
        testIncludeVariablesWithPagination(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLessThan("numberVar", 20).includeCaseVariables().orderByStartTime());
        testIncludeVariablesWithPagination(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLessThanOrEqual("numberVar", 10).includeCaseVariables().orderByStartTime());
    }

    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/runtime/RuntimeServiceTest.testStartSimplePassthroughCaseWithBlockingTask.cmmn"})
    public void includeVariablesWithEmptyPaginationQueries() {
        createCaseInstances();

        testIncludeVariablesOnEmptyQueryWithPagination(cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("var", "test2").includeCaseVariables().orderByStartTime());
        testIncludeVariablesOnEmptyQueryWithPagination(cmmnRuntimeService.createCaseInstanceQuery().variableValueEqualsIgnoreCase("var", "TEST2").includeCaseVariables().orderByStartTime());
        testIncludeVariablesOnEmptyQueryWithPagination(cmmnRuntimeService.createCaseInstanceQuery().variableValueNotEquals("var", "test").includeCaseVariables().orderByStartTime());
        testIncludeVariablesOnEmptyQueryWithPagination(cmmnRuntimeService.createCaseInstanceQuery().variableValueNotEqualsIgnoreCase("var", "TEST").includeCaseVariables().orderByStartTime());
        testIncludeVariablesOnEmptyQueryWithPagination(cmmnRuntimeService.createCaseInstanceQuery().variableValueLike("var", "te2%").includeCaseVariables().orderByStartTime());
        testIncludeVariablesOnEmptyQueryWithPagination(cmmnRuntimeService.createCaseInstanceQuery().variableValueLikeIgnoreCase("var", "TE2%").includeCaseVariables().orderByStartTime());
        testIncludeVariablesOnEmptyQueryWithPagination(cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThan("numberVar", 11).includeCaseVariables().orderByStartTime());
        testIncludeVariablesOnEmptyQueryWithPagination(cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThanOrEqual("numberVar", 11).includeCaseVariables().orderByStartTime());
        testIncludeVariablesOnEmptyQueryWithPagination(cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThan("numberVar", 5).includeCaseVariables().orderByStartTime());
        testIncludeVariablesOnEmptyQueryWithPagination(cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThanOrEqual("numberVar", 9).includeCaseVariables().orderByStartTime());

        testIncludeVariablesOnEmptyQueryWithPagination(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEquals("var", "test2").includeCaseVariables().orderByStartTime());
        testIncludeVariablesOnEmptyQueryWithPagination(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEqualsIgnoreCase("var", "TEST2").includeCaseVariables().orderByStartTime());
        testIncludeVariablesOnEmptyQueryWithPagination(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueNotEquals("var", "test").includeCaseVariables().orderByStartTime());
        testIncludeVariablesOnEmptyQueryWithPagination(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLike("var", "te2%").includeCaseVariables().orderByStartTime());
        testIncludeVariablesOnEmptyQueryWithPagination(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLikeIgnoreCase("var", "TE2%").includeCaseVariables().orderByStartTime());
        testIncludeVariablesOnEmptyQueryWithPagination(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueGreaterThan("numberVar", 11).includeCaseVariables().orderByStartTime());
        testIncludeVariablesOnEmptyQueryWithPagination(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueGreaterThanOrEqual("numberVar", 11).includeCaseVariables().orderByStartTime());
        testIncludeVariablesOnEmptyQueryWithPagination(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLessThan("numberVar", 5).includeCaseVariables().orderByStartTime());
        testIncludeVariablesOnEmptyQueryWithPagination(cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLessThanOrEqual("numberVar", 9).includeCaseVariables().orderByStartTime());
    }

    private void createCaseInstances() {
        Map<String, Object> variablesWithCounter = new HashMap<>(VARIABLES);

        for (int i = 0; i < 100; i++) {
            variablesWithCounter.put("counter", i);
            cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .name("A" + i)
                    .variables(variablesWithCounter)
                    .start();
        }
    }

    private void testIncludeVariablesWithPagination(CaseInstanceQuery caseInstanceQuery) {
        assertPaginationQueryIncludeVariables(20, 10, caseInstanceQuery.listPage(20,10));
        assertPaginationQueryIncludeVariables(0, 100, caseInstanceQuery.listPage(0,0));
        assertPaginationQueryIncludeVariables(0, 100, caseInstanceQuery.listPage(0, -1));
        assertPaginationQueryIncludeVariables(0, 100, caseInstanceQuery.listPage(-1, -1));
        assertPaginationQueryIncludeVariables(0, 100, caseInstanceQuery.listPage(-1, -2));
        assertPaginationQueryIncludeVariables(90, 10, caseInstanceQuery.listPage(90, 20));
        assertPaginationQueryIncludeVariables(90, 10, caseInstanceQuery.listPage(90, 10));
        assertPaginationQueryIncludeVariables(0, 20, caseInstanceQuery.listPage(-10, 20));
        assertPaginationQueryIncludeVariables(0, 100, caseInstanceQuery.listPage(0, Integer.MAX_VALUE));

        try {
            caseInstanceQuery.listPage(20, -1);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("fromIndex(20) > toIndex(19)"));
        }
    }

    private void testIncludeVariablesWithPagination(HistoricCaseInstanceQuery caseInstanceQuery) {
        assertPaginationHistoricQueryIncludeVariables(20, 10, caseInstanceQuery.listPage(20,10));
        assertPaginationHistoricQueryIncludeVariables(0, 100, caseInstanceQuery.listPage(0,0));
        assertPaginationHistoricQueryIncludeVariables(0, 100, caseInstanceQuery.listPage(0, -1));
        assertPaginationHistoricQueryIncludeVariables(0, 100, caseInstanceQuery.listPage(-1, -1));
        assertPaginationHistoricQueryIncludeVariables(0, 100, caseInstanceQuery.listPage(-1, -2));
        assertPaginationHistoricQueryIncludeVariables(90, 10, caseInstanceQuery.listPage(90, 20));
        assertPaginationHistoricQueryIncludeVariables(90, 10, caseInstanceQuery.listPage(90, 10));
        assertPaginationHistoricQueryIncludeVariables(0, 20, caseInstanceQuery.listPage(-10, 20));
        assertPaginationHistoricQueryIncludeVariables(0, 100, caseInstanceQuery.listPage(0, Integer.MAX_VALUE));

        try {
            caseInstanceQuery.listPage(20, -1);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("fromIndex(20) > toIndex(19)"));
        }
    }

    private void testIncludeVariablesOnEmptyQueryWithPagination(CaseInstanceQuery caseInstanceQuery) {
        assertPaginationQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(20,10));
        assertPaginationQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(0,0));
        assertPaginationQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(0, -1));
        assertPaginationQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(-1, -1));
        assertPaginationQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(-1, -2));
        assertPaginationQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(90, 20));
        assertPaginationQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(90, 10));
        assertPaginationQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(-10, 20));
        assertPaginationQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(20, -1));
        assertPaginationQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(0, Integer.MAX_VALUE));
    }

    private void testIncludeVariablesOnEmptyQueryWithPagination(HistoricCaseInstanceQuery caseInstanceQuery) {
        assertPaginationHistoricQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(20,10));
        assertPaginationHistoricQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(0,0));
        assertPaginationHistoricQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(0, -1));
        assertPaginationHistoricQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(-1, -1));
        assertPaginationHistoricQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(-1, -2));
        assertPaginationHistoricQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(90, 20));
        assertPaginationHistoricQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(90, 10));
        assertPaginationHistoricQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(-10, 20));
        assertPaginationHistoricQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(20, -1));
        assertPaginationHistoricQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(0, Integer.MAX_VALUE));
    }

    @Test
    @CmmnDeployment
    public void testTerminateCaseInstance() {

        // Task A active
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertCaseInstanceEnded(caseInstance, 0);

        // Task B active
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        cmmnRuntimeService.triggerPlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId()).planItemInstanceState(PlanItemInstanceState.ACTIVE).singleResult().getId());
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertCaseInstanceEnded(caseInstance, 1);

        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNotNull(historicCaseInstance);
            assertNotNull(historicCaseInstance.getStartTime());
            assertNotNull(historicCaseInstance.getEndTime());
            assertEquals(CaseInstanceState.TERMINATED, historicCaseInstance.getState());
        }
    }

    @Test
    @CmmnDeployment
    public void testTerminateCaseInstanceWithNestedStages() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertEquals(8, cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId()).count());
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertCaseInstanceEnded(caseInstance, 0);

        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertNotNull(historicCaseInstance);
            assertNotNull(historicCaseInstance.getStartTime());
            assertNotNull(historicCaseInstance.getEndTime());
            assertEquals(CaseInstanceState.TERMINATED, historicCaseInstance.getState());
        }
    }

    @Test
    @CmmnDeployment
    public void testCaseInstanceProperties() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .name("test name")
                .businessKey("test business key")
                .start();

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertEquals("test name", caseInstance.getName());
        assertEquals("test business key", caseInstance.getBusinessKey());
    }

}
