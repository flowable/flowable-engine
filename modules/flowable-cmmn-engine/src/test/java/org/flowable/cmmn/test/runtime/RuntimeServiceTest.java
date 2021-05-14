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
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
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
import org.flowable.cmmn.engine.interceptor.StartCaseInstanceAfterContext;
import org.flowable.cmmn.engine.interceptor.StartCaseInstanceBeforeContext;
import org.flowable.cmmn.engine.interceptor.StartCaseInstanceInterceptor;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.junit.Test;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
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
        assertThat(caseDefinition.getKey()).isEqualTo("myCase");
        assertThat(caseDefinition.getResourceName()).isNotNull();
        assertThat(caseDefinition.getDeploymentId()).isNotNull();
        assertThat(caseDefinition.getVersion()).isPositive();

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionId(caseDefinition.getId()).start();
        assertThat(caseInstance).isNotNull();
        assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinition.getId());
        assertThat(caseInstance.getStartTime()).isNotNull();
        assertThat(caseInstance.getState()).isEqualTo(CaseInstanceState.COMPLETED);

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count()).isZero();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);

            List<HistoricMilestoneInstance> milestoneInstances = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId())
                .orderByMilestoneName().asc()
                .list();
            assertThat(milestoneInstances)
                .extracting(HistoricMilestoneInstance::getName)
                .containsExactly("PlanItem Milestone One", "PlanItem Milestone Two");
        }
    }

    @Test
    @CmmnDeployment
    public void testStartSimplePassthroughCaseWithBlockingTask() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().singleResult();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionId(caseDefinition.getId()).start();
        assertThat(caseInstance.getState()).isEqualTo(CaseInstanceState.ACTIVE);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isZero();
        }

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(4);

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance).isNotNull();
        assertThat(planItemInstance.getName()).isEqualTo("Task A");

        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        List<MilestoneInstance> mileStones = cmmnRuntimeService.createMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId())
                .list();
        assertThat(mileStones)
                .extracting(MilestoneInstance::getName)
                .containsExactly("PlanItem Milestone One");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricMilestoneInstance> historicMilestoneInstances = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId())
                .list();
            assertThat(historicMilestoneInstances)
                .extracting(HistoricMilestoneInstance::getName)
                .containsExactly("PlanItem Milestone One");
        }

        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance).isNotNull();
        assertThat(planItemInstance.getName()).isEqualTo("Task B");
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count()).isZero();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count()).isEqualTo(2);
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(historicCaseInstance).isNotNull();
            assertThat(historicCaseInstance.getStartTime()).isNotNull();
            assertThat(historicCaseInstance.getEndTime()).isNotNull();
            assertThat(historicCaseInstance.getState()).isEqualTo(CaseInstanceState.COMPLETED);
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/RuntimeServiceTest.testStartSimplePassthroughCaseWithBlockingTask.cmmn" })
    public void testBlockingTaskWithStartInterceptor() {
        TestStartCaseInstanceInterceptor testStartCaseInstanceInterceptor = new TestStartCaseInstanceInterceptor();
        cmmnEngineConfiguration.setStartCaseInstanceInterceptor(testStartCaseInstanceInterceptor);

        try {
            CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().singleResult();
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionId(caseDefinition.getId()).start();
            assertThat(caseInstance.getState()).isEqualTo(CaseInstanceState.ACTIVE);

            assertThat(testStartCaseInstanceInterceptor.getBeforeStartCaseInstanceCounter()).isEqualTo(1);
            assertThat(testStartCaseInstanceInterceptor.getAfterStartCaseInstanceCounter()).isEqualTo(1);

            assertThat(caseInstance.getBusinessKey()).isEqualTo("testKey");
            assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "beforeContextVar")).isEqualTo("test");

        } finally {
            cmmnEngineConfiguration.setStartCaseInstanceInterceptor(null);
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/RuntimeServiceTest.testStartSimplePassthroughCaseWithBlockingTask.cmmn" })
    public void testVariableQueryWithBlockingTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variables(VARIABLES)
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
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
        }

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance).isNotNull();
        assertThat(planItemInstance.getName()).isEqualTo("Task A");

        Map<String, Object> varMap = new HashMap<>();
        varMap.put("numberVar", 11);
        cmmnRuntimeService.setVariables(caseInstance.getId(), varMap);

        Map<String, Object> localVarMap = new HashMap<>();
        localVarMap.put("localVar", "test");
        cmmnRuntimeService.setLocalVariables(planItemInstance.getId(), localVarMap);

        Map<String, Object> updatedVariables = new HashMap<>(VARIABLES);
        updatedVariables.put("numberVar", 11);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
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
        }

        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE).singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertEmptyQuery(
                cmmnRuntimeService.createCaseInstanceQuery(),
                cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished()
            );
        }
    }

    private void assertNonEmptyQueryIncludeVariables(int expectedCount, Map<String, Object> expectedVariables,
            CaseInstanceQuery caseInstanceQuery,
            HistoricCaseInstanceQuery historicCaseInstanceQuery) {
        assertThat(caseInstanceQuery.count()).isEqualTo(expectedCount);
        if (historicCaseInstanceQuery != null) {
            assertThat(historicCaseInstanceQuery.count()).isEqualTo(expectedCount);
        }
        // assert singleResult
        if (expectedCount == 1) {
            CaseInstance fetchedCaseInstance = caseInstanceQuery.includeCaseVariables().singleResult();
            assertThat(fetchedCaseInstance.getCaseVariables()).isEqualTo(expectedVariables);
            if (historicCaseInstanceQuery != null) {
                HistoricCaseInstance fetchedHistoricCaseInstance = historicCaseInstanceQuery.includeCaseVariables().singleResult();
                assertThat(fetchedHistoricCaseInstance.getCaseVariables()).isEqualTo(expectedVariables);
            }
        } else if (expectedCount > 1) {
            assertThatThrownBy(() -> caseInstanceQuery.includeCaseVariables().singleResult())
                    .isInstanceOf(FlowableException.class)
                    .hasMessage("Query return " + expectedCount + " results instead of max 1");

            if (historicCaseInstanceQuery != null) {
                assertThatThrownBy(() -> historicCaseInstanceQuery.includeCaseVariables().singleResult())
                        .isInstanceOf(FlowableException.class)
                        .hasMessage("Query return " + expectedCount + " results instead of max 1");
            }
        }

        // assert query list
        List<CaseInstance> caseInstances = caseInstanceQuery.includeCaseVariables().list();
        assertThat(caseInstances).hasSize(expectedCount);
        for (CaseInstance caseInstance : caseInstances) {
            assertThat(caseInstance.getCaseVariables()).isEqualTo(expectedVariables);
        }
        if (historicCaseInstanceQuery != null) {
            List<HistoricCaseInstance> historicCaseInstances = historicCaseInstanceQuery.includeCaseVariables().list();
            assertThat(historicCaseInstances).hasSize(expectedCount);
            for (HistoricCaseInstance historicCaseInstance : historicCaseInstances) {
                assertThat(historicCaseInstance.getCaseVariables()).isEqualTo(expectedVariables);
            }
        }
    }

    private void assertPaginationQueryIncludeVariables(int expectedStart, int expectedCount, List<CaseInstance> caseInstances) {
        assertThat(caseInstances).hasSize(expectedCount);
        Map<String, Object> expectedVariables = new HashMap<>(VARIABLES);
        for (int i = 0; i < expectedCount; i++) {
            CaseInstance caseInstance = caseInstances.get(i);
            expectedVariables.put("counter", expectedStart + i);
            assertThat(caseInstance.getName()).isEqualTo("A" + (expectedStart + i));
            assertThat(caseInstance.getCaseVariables()).isEqualTo(expectedVariables);
        }
    }

    private void assertPaginationHistoricQueryIncludeVariables(int expectedStart, int expectedCount, List<HistoricCaseInstance> caseInstances) {
        assertThat(caseInstances).hasSize(expectedCount);
        Map<String, Object> expectedVariables = new HashMap<>(VARIABLES);
        for (int i = 0; i < expectedCount; i++) {
            HistoricCaseInstance caseInstance = caseInstances.get(i);
            expectedVariables.put("counter", expectedStart + i);
            assertThat(caseInstance.getName()).isEqualTo("A" + (expectedStart + i));
            assertThat(caseInstance.getCaseVariables()).isEqualTo(expectedVariables);
        }
    }

    private void assertEmptyQuery(CaseInstanceQuery caseInstanceQuery, HistoricCaseInstanceQuery historicCaseInstanceQuery) {
        assertThat(caseInstanceQuery.count()).isZero();
        assertThat(caseInstanceQuery.includeCaseVariables().singleResult()).isNull();
        List<CaseInstance> caseInstances = caseInstanceQuery.includeCaseVariables().list();
        assertThat(caseInstances).isEmpty();

        if (historicCaseInstanceQuery != null) {
            assertThat(historicCaseInstanceQuery.count()).isZero();
            assertThat(historicCaseInstanceQuery.includeCaseVariables().singleResult()).isNull();
            List<HistoricCaseInstance> historicCaseInstances = historicCaseInstanceQuery.includeCaseVariables().list();
            assertThat(historicCaseInstances).isEmpty();
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/RuntimeServiceTest.testStartSimplePassthroughCaseWithBlockingTask.cmmn" })
    public void testPlanItemVariableQueryWithBlockingTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("var", "test")
                .variable("numberVar", 10)
                .start();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueEquals("var", "test").count()).isEqualTo(4);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueEquals("var", "test2").count()).isZero();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueEqualsIgnoreCase("var", "TEST").count()).isEqualTo(4);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueEqualsIgnoreCase("var", "TEST2").count()).isZero();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueNotEquals("var", "test2").count()).isEqualTo(4);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueNotEquals("var", "test").count()).isZero();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueNotEqualsIgnoreCase("var", "TEST2").count()).isEqualTo(4);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueNotEqualsIgnoreCase("var", "TEST").count()).isZero();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueLike("var", "te%").count()).isEqualTo(4);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueLike("var", "te2%").count()).isZero();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueLikeIgnoreCase("var", "TE%").count()).isEqualTo(4);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueLikeIgnoreCase("var", "TE2%").count()).isZero();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueGreaterThan("numberVar", 5).count()).isEqualTo(4);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueGreaterThan("numberVar", 11).count()).isZero();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueGreaterThanOrEqual("numberVar", 10).count()).isEqualTo(4);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueGreaterThanOrEqual("numberVar", 11).count()).isZero();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueLessThan("numberVar", 20).count()).isEqualTo(4);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueLessThan("numberVar", 5).count()).isZero();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueLessThanOrEqual("numberVar", 10).count()).isEqualTo(4);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueLessThanOrEqual("numberVar", 9).count()).isZero();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance).isNotNull();
        assertThat(planItemInstance.getName()).isEqualTo("Task A");

        Map<String, Object> varMap = new HashMap<>();
        varMap.put("numberVar", 11);
        cmmnRuntimeService.setVariables(caseInstance.getId(), varMap);

        Map<String, Object> localVarMap = new HashMap<>();
        localVarMap.put("localVar", "test");
        localVarMap.put("localNumberVar", 15);
        cmmnRuntimeService.setLocalVariables(planItemInstance.getId(), localVarMap);

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueGreaterThan("numberVar", 10).count()).isEqualTo(4);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueGreaterThan("numberVar", 11).count()).isZero();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueGreaterThanOrEqual("numberVar", 11).count()).isEqualTo(4);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueGreaterThanOrEqual("numberVar", 12).count()).isZero();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseVariableValueEquals("localVar", "test").count()).isZero();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueEquals("localVar", "test").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueEquals("localVar", "test2").count()).isZero();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueEqualsIgnoreCase("localVar", "TEST").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueEqualsIgnoreCase("localVar", "TEST2").count()).isZero();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueNotEquals("localVar", "test2").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueNotEquals("localVar", "test").count()).isZero();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueNotEqualsIgnoreCase("localVar", "TEST2").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueNotEqualsIgnoreCase("localVar", "TEST").count()).isZero();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueLike("localVar", "te%").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueLike("localVar", "te2%").count()).isZero();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueLikeIgnoreCase("localVar", "TE%").count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueLikeIgnoreCase("localVar", "TE2%").count()).isZero();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueGreaterThan("localNumberVar", 5).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueGreaterThan("localNumberVar", 17).count()).isZero();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueGreaterThanOrEqual("localNumberVar", 15).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueGreaterThanOrEqual("localNumberVar", 16).count()).isZero();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueLessThan("localNumberVar", 20).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueLessThan("localNumberVar", 5).count()).isZero();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueLessThanOrEqual("localNumberVar", 15).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().variableValueLessThanOrEqual("localNumberVar", 9).count()).isZero();

        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE).singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/RuntimeServiceTest.testStartSimplePassthroughCaseWithBlockingTask.cmmn" })
    public void testLocalVariablesWithBlockingTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance).isNotNull();
        assertThat(planItemInstance.getName()).isEqualTo("Task A");

        Map<String, Object> varMap = new HashMap<>();
        varMap.put("localVar", "test");
        cmmnRuntimeService.setLocalVariables(planItemInstance.getId(), varMap);

        assertThat(cmmnRuntimeService.getLocalVariable(planItemInstance.getId(), "localVar")).isEqualTo("test");
        assertThat(cmmnRuntimeService.getLocalVariables(planItemInstance.getId())).hasSize(1);
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "localVar")).isNull();

        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance).isNotNull();
        assertThat(planItemInstance.getName()).isEqualTo("Task B");

        assertThat(cmmnRuntimeService.getLocalVariable(planItemInstance.getId(), "localVar")).isNull();

        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/RuntimeServiceTest.testStartSimplePassthroughCaseWithBlockingTask.cmmn" })
    public void testRemoveLocalVariablesWithBlockingTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance).isNotNull();
        assertThat(planItemInstance.getName()).isEqualTo("Task A");

        Map<String, Object> localVarMap = new HashMap<>();
        localVarMap.put("localVar", "test");
        cmmnRuntimeService.setLocalVariables(planItemInstance.getId(), localVarMap);

        Map<String, Object> varMap = new HashMap<>();
        varMap.put("var", "test");
        cmmnRuntimeService.setVariables(caseInstance.getId(), varMap);

        assertThat(cmmnRuntimeService.getLocalVariable(planItemInstance.getId(), "localVar")).isEqualTo("test");
        assertThat(cmmnRuntimeService.getLocalVariables(planItemInstance.getId())).hasSize(1);

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "var")).isEqualTo("test");
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).hasSize(1);

        cmmnRuntimeService.removeLocalVariable(planItemInstance.getId(), "localVar");
        assertThat(cmmnRuntimeService.getLocalVariable(planItemInstance.getId(), "localVar")).isNull();

        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                .singleResult();
        assertThat(planItemInstance).isNotNull();
        assertThat(planItemInstance.getName()).isEqualTo("Task B");

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "var")).isEqualTo("test");
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).hasSize(1);

        cmmnRuntimeService.removeVariable(caseInstance.getId(), "var");
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "var")).isNull();

        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().unfinished().count()).isZero();
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count()).isEqualTo(1);
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/RuntimeServiceTest.testStartSimplePassthroughCaseWithBlockingTask.cmmn" })
    public void testIncludeVariableSingleResultQueryWithBlockingTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variables(VARIABLES)
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertNonEmptyQueryIncludeVariables(1, VARIABLES,
                cmmnRuntimeService.createCaseInstanceQuery().includeCaseVariables(),
                cmmnHistoryService.createHistoricCaseInstanceQuery().includeCaseVariables()
            );
        }

        Map<String, Object> updatedVariables = new HashMap<>(VARIABLES);
        updatedVariables.put("newVar", 14.2);
        cmmnRuntimeService.setVariables(caseInstance.getId(), updatedVariables);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertNonEmptyQueryIncludeVariables(1, updatedVariables,
                cmmnRuntimeService.createCaseInstanceQuery().includeCaseVariables(),
                cmmnHistoryService.createHistoricCaseInstanceQuery().includeCaseVariables()
            );
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/RuntimeServiceTest.testStartSimplePassthroughCaseWithBlockingTask.cmmn" })
    public void testIncludeVariableListQueryWithBlockingTask() {
        Map<String, Object> variablesA = new HashMap<>();
        variablesA.put("var", "test");
        variablesA.put("numberVar", 10);
        Map<String, Object> variablesB = new HashMap<>();
        variablesB.put("var", "test");
        variablesB.put("numberVar", 10);
        variablesB.put("floatVar", 10.1);

        Instant now = setClockFixedToCurrentTime().toInstant();
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .name("A")
                .variables(variablesA)
                .start();
        setClockTo(Date.from(now.plusSeconds(1)));
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .name("B")
                .variables(variablesB)
                .start();
        setClockTo(Date.from(now.plusSeconds(2)));
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .name("C")
                .start();

        List<CaseInstance> caseInstancesWithVariables = cmmnRuntimeService.createCaseInstanceQuery().includeCaseVariables().orderByStartTime().asc().list();
        assertThat(caseInstancesWithVariables)
                .extracting(CaseInstance::getCaseVariables)
                .containsExactly(variablesA, variablesB, Collections.EMPTY_MAP);

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().includeCaseVariables().orderByStartTime().asc().count()).isEqualTo(3);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricCaseInstance> historicCaseInstancesWithVariables = cmmnHistoryService.createHistoricCaseInstanceQuery().includeCaseVariables()
                .orderByStartTime().asc().list();
            assertThat(historicCaseInstancesWithVariables)
                .extracting(HistoricCaseInstance::getCaseVariables)
                .containsExactly(variablesA, variablesB, Collections.EMPTY_MAP);

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().includeCaseVariables().orderByStartTime().asc().count()).isEqualTo(3);
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/RuntimeServiceTest.testStartSimplePassthroughCaseWithBlockingTask.cmmn" })
    public void testIncludeSameVariableListQueryWithBlockingTask() {
        Instant now = setClockFixedToCurrentTime().toInstant();
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .name("A")
                .variables(VARIABLES)
                .start();
        setClockTo(Date.from(now.plusSeconds(1)));
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .name("B")
                .variables(VARIABLES)
                .start();
        setClockTo(Date.from(now.plusSeconds(2)));
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variables(VARIABLES)
                .name("C")
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertNonEmptyQueryIncludeVariables(3, VARIABLES,
                cmmnRuntimeService.createCaseInstanceQuery().orderByStartTime().asc(),
                cmmnHistoryService.createHistoricCaseInstanceQuery().orderByStartTime().asc()
            );
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/RuntimeServiceTest.testStartSimplePassthroughCaseWithBlockingTask.cmmn" })
    public void includeVariablesWithPaginationQueries() {
        createCaseInstances();

        testIncludeVariablesWithPagination(cmmnRuntimeService.createCaseInstanceQuery().includeCaseVariables().orderByStartTime().asc());
        testIncludeVariablesWithPagination(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("var", "test").includeCaseVariables().orderByStartTime().asc());
        testIncludeVariablesWithPagination(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueEqualsIgnoreCase("var", "TEST").includeCaseVariables().orderByStartTime().asc());
        testIncludeVariablesWithPagination(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueNotEquals("var", "test2").includeCaseVariables().orderByStartTime().asc());
        testIncludeVariablesWithPagination(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueNotEqualsIgnoreCase("var", "TEST2").includeCaseVariables().orderByStartTime().asc());
        testIncludeVariablesWithPagination(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueLike("var", "te%").includeCaseVariables().orderByStartTime().asc());
        testIncludeVariablesWithPagination(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueLikeIgnoreCase("var", "TE%").includeCaseVariables().orderByStartTime().asc());
        testIncludeVariablesWithPagination(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThan("numberVar", 5).includeCaseVariables().orderByStartTime().asc());
        testIncludeVariablesWithPagination(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThanOrEqual("numberVar", 10).includeCaseVariables().orderByStartTime().asc());
        testIncludeVariablesWithPagination(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThan("numberVar", 20).includeCaseVariables().orderByStartTime().asc());
        testIncludeVariablesWithPagination(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThanOrEqual("numberVar", 10).includeCaseVariables().orderByStartTime().asc());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            testIncludeVariablesWithPagination(cmmnHistoryService.createHistoricCaseInstanceQuery().includeCaseVariables().orderByStartTime().asc());
            testIncludeVariablesWithPagination(
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEquals("var", "test").includeCaseVariables().orderByStartTime().asc());
            testIncludeVariablesWithPagination(
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEqualsIgnoreCase("var", "TEST").includeCaseVariables().orderByStartTime().asc());
            testIncludeVariablesWithPagination(
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueNotEquals("var", "test2").includeCaseVariables().orderByStartTime().asc());
            testIncludeVariablesWithPagination(
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLike("var", "te%").includeCaseVariables().orderByStartTime().asc());
            testIncludeVariablesWithPagination(
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLikeIgnoreCase("var", "TE%").includeCaseVariables().orderByStartTime().asc());
            testIncludeVariablesWithPagination(
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueGreaterThan("numberVar", 5).includeCaseVariables().orderByStartTime().asc());
            testIncludeVariablesWithPagination(
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueGreaterThanOrEqual("numberVar", 10).includeCaseVariables().orderByStartTime().asc());
            testIncludeVariablesWithPagination(
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLessThan("numberVar", 20).includeCaseVariables().orderByStartTime().asc());
            testIncludeVariablesWithPagination(
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLessThanOrEqual("numberVar", 10).includeCaseVariables().orderByStartTime().asc());
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/RuntimeServiceTest.testStartSimplePassthroughCaseWithBlockingTask.cmmn" })
    public void includeVariablesWithEmptyPaginationQueries() {
        createCaseInstances();

        testIncludeVariablesOnEmptyQueryWithPagination(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueEquals("var", "test2").includeCaseVariables().orderByStartTime().asc());
        testIncludeVariablesOnEmptyQueryWithPagination(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueEqualsIgnoreCase("var", "TEST2").includeCaseVariables().orderByStartTime().asc());
        testIncludeVariablesOnEmptyQueryWithPagination(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueNotEquals("var", "test").includeCaseVariables().orderByStartTime().asc());
        testIncludeVariablesOnEmptyQueryWithPagination(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueNotEqualsIgnoreCase("var", "TEST").includeCaseVariables().orderByStartTime().asc());
        testIncludeVariablesOnEmptyQueryWithPagination(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueLike("var", "te2%").includeCaseVariables().orderByStartTime().asc());
        testIncludeVariablesOnEmptyQueryWithPagination(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueLikeIgnoreCase("var", "TE2%").includeCaseVariables().orderByStartTime().asc());
        testIncludeVariablesOnEmptyQueryWithPagination(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThan("numberVar", 11).includeCaseVariables().orderByStartTime().asc());
        testIncludeVariablesOnEmptyQueryWithPagination(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueGreaterThanOrEqual("numberVar", 11).includeCaseVariables().orderByStartTime().asc());
        testIncludeVariablesOnEmptyQueryWithPagination(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThan("numberVar", 5).includeCaseVariables().orderByStartTime().asc());
        testIncludeVariablesOnEmptyQueryWithPagination(
                cmmnRuntimeService.createCaseInstanceQuery().variableValueLessThanOrEqual("numberVar", 9).includeCaseVariables().orderByStartTime().asc());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            testIncludeVariablesOnEmptyQueryWithPagination(
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEquals("var", "test2").includeCaseVariables().orderByStartTime().asc());
            testIncludeVariablesOnEmptyQueryWithPagination(
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueEqualsIgnoreCase("var", "TEST2").includeCaseVariables().orderByStartTime()
                    .asc());
            testIncludeVariablesOnEmptyQueryWithPagination(
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueNotEquals("var", "test").includeCaseVariables().orderByStartTime().asc());
            testIncludeVariablesOnEmptyQueryWithPagination(
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLike("var", "te2%").includeCaseVariables().orderByStartTime().asc());
            testIncludeVariablesOnEmptyQueryWithPagination(
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLikeIgnoreCase("var", "TE2%").includeCaseVariables().orderByStartTime()
                    .asc());
            testIncludeVariablesOnEmptyQueryWithPagination(
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueGreaterThan("numberVar", 11).includeCaseVariables().orderByStartTime().asc());
            testIncludeVariablesOnEmptyQueryWithPagination(
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueGreaterThanOrEqual("numberVar", 11).includeCaseVariables().orderByStartTime()
                    .asc());
            testIncludeVariablesOnEmptyQueryWithPagination(
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLessThan("numberVar", 5).includeCaseVariables().orderByStartTime().asc());
            testIncludeVariablesOnEmptyQueryWithPagination(
                cmmnHistoryService.createHistoricCaseInstanceQuery().variableValueLessThanOrEqual("numberVar", 9).includeCaseVariables().orderByStartTime()
                    .asc());
        }
    }

    private void createCaseInstances() {
        Map<String, Object> variablesWithCounter = new HashMap<>(VARIABLES);

        Instant now = Instant.now();
        for (int i = 0; i < 100; i++) {
            variablesWithCounter.put("counter", i);
            setClockTo(Date.from(now.plusSeconds(i)));
            cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .name("A" + i)
                    .variables(variablesWithCounter)
                    .start();
        }
        setClockTo(null);
    }

    private void testIncludeVariablesWithPagination(CaseInstanceQuery caseInstanceQuery) {
        assertPaginationQueryIncludeVariables(20, 10, caseInstanceQuery.listPage(20, 10));
        assertPaginationQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(0, 0));
        assertPaginationQueryIncludeVariables(0, 100, caseInstanceQuery.listPage(0, -1));
        assertPaginationQueryIncludeVariables(0, 100, caseInstanceQuery.listPage(-1, -1));
        assertPaginationQueryIncludeVariables(0, 100, caseInstanceQuery.listPage(-1, -2));
        assertPaginationQueryIncludeVariables(90, 10, caseInstanceQuery.listPage(90, 20));
        assertPaginationQueryIncludeVariables(90, 10, caseInstanceQuery.listPage(90, 10));
        assertPaginationQueryIncludeVariables(0, 20, caseInstanceQuery.listPage(-10, 20));
        assertPaginationQueryIncludeVariables(0, 100, caseInstanceQuery.listPage(0, Integer.MAX_VALUE));
        assertPaginationQueryIncludeVariables(20, 80, caseInstanceQuery.listPage(20, -1));
    }

    private void testIncludeVariablesWithPagination(HistoricCaseInstanceQuery caseInstanceQuery) {
        assertPaginationHistoricQueryIncludeVariables(20, 10, caseInstanceQuery.listPage(20, 10));
        assertPaginationHistoricQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(0, 0));
        assertPaginationHistoricQueryIncludeVariables(0, 100, caseInstanceQuery.listPage(0, -1));
        assertPaginationHistoricQueryIncludeVariables(0, 100, caseInstanceQuery.listPage(-1, -1));
        assertPaginationHistoricQueryIncludeVariables(0, 100, caseInstanceQuery.listPage(-1, -2));
        assertPaginationHistoricQueryIncludeVariables(90, 10, caseInstanceQuery.listPage(90, 20));
        assertPaginationHistoricQueryIncludeVariables(90, 10, caseInstanceQuery.listPage(90, 10));
        assertPaginationHistoricQueryIncludeVariables(0, 20, caseInstanceQuery.listPage(-10, 20));
        assertPaginationHistoricQueryIncludeVariables(0, 100, caseInstanceQuery.listPage(0, Integer.MAX_VALUE));
        assertPaginationHistoricQueryIncludeVariables(20, 80, caseInstanceQuery.listPage(20, -1));
    }

    private void testIncludeVariablesOnEmptyQueryWithPagination(CaseInstanceQuery caseInstanceQuery) {
        assertPaginationQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(20, 10));
        assertPaginationQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(0, 0));
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
        assertPaginationHistoricQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(20, 10));
        assertPaginationHistoricQueryIncludeVariables(0, 0, caseInstanceQuery.listPage(0, 0));
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

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId())
                    .singleResult();
            assertThat(historicCaseInstance).isNotNull();
            assertThat(historicCaseInstance.getStartTime()).isNotNull();
            assertThat(historicCaseInstance.getEndTime()).isNotNull();
            assertThat(historicCaseInstance.getState()).isEqualTo(CaseInstanceState.TERMINATED);
        }
    }
    
    @Test
    @CmmnDeployment
    public void testDeleteCaseInstance() {

        // Task A active
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        cmmnRuntimeService.deleteCaseInstance(caseInstance.getId());
        assertCaseInstanceEnded(caseInstance, 0);

        // Task B active
        caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        cmmnRuntimeService.triggerPlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId()).planItemInstanceState(PlanItemInstanceState.ACTIVE).singleResult().getId());
        cmmnRuntimeService.deleteCaseInstance(caseInstance.getId());
        assertCaseInstanceEnded(caseInstance, 1);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId())
                    .singleResult();
            assertThat(historicCaseInstance).isNotNull();
            assertThat(historicCaseInstance.getStartTime()).isNotNull();
            assertThat(historicCaseInstance.getEndTime()).isNotNull();
            assertThat(historicCaseInstance.getState()).isEqualTo(CaseInstanceState.TERMINATED);
        }
    }
    
    @Test
    @CmmnDeployment
    public void testDeleteCaseInstanceWithListener() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        cmmnRuntimeService.triggerPlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId()).planItemInstanceState(PlanItemInstanceState.ACTIVE).singleResult().getId());
        
        assertThatThrownBy(() -> {
            cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        }).isInstanceOf(FlowableException.class);
        
        cmmnRuntimeService.deleteCaseInstance(caseInstance.getId());
        assertCaseInstanceEnded(caseInstance, 1);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId())
                    .singleResult();
            assertThat(historicCaseInstance).isNotNull();
            assertThat(historicCaseInstance.getStartTime()).isNotNull();
            assertThat(historicCaseInstance.getEndTime()).isNotNull();
            assertThat(historicCaseInstance.getState()).isEqualTo(CaseInstanceState.TERMINATED);
        }
    }
    
    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/runtime/RuntimeServiceTest.testDeleteCaseInstanceWithListenerAndCaseTask.cmmn",
            "org/flowable/cmmn/test/runtime/RuntimeServiceTest.testDeleteCaseInstanceWithListener.cmmn"
    })
    public void testDeleteCaseInstanceWithListenerAndCaseTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCaseWithCaseTask").start();
        cmmnRuntimeService.triggerPlanItemInstance(cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId()).planItemInstanceState(PlanItemInstanceState.ACTIVE).singleResult().getId());
        
        CaseInstance subCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("myCase").singleResult();
        assertThat(subCaseInstance).isNotNull();
        
        assertThatThrownBy(() -> {
            cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        }).isInstanceOf(FlowableException.class);
        
        cmmnRuntimeService.deleteCaseInstance(caseInstance.getId());
        assertCaseInstanceEnded(caseInstance, 0);
        assertCaseInstanceEnded(subCaseInstance, 0);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .singleResult();
            assertThat(historicCaseInstance).isNotNull();
            assertThat(historicCaseInstance.getStartTime()).isNotNull();
            assertThat(historicCaseInstance.getEndTime()).isNotNull();
            assertThat(historicCaseInstance.getState()).isEqualTo(CaseInstanceState.TERMINATED);
            
            HistoricCaseInstance historicSubCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .caseInstanceId(subCaseInstance.getId())
                    .singleResult();
            assertThat(historicSubCaseInstance).isNotNull();
            assertThat(historicSubCaseInstance.getStartTime()).isNotNull();
            assertThat(historicSubCaseInstance.getEndTime()).isNotNull();
            assertThat(historicSubCaseInstance.getState()).isEqualTo(CaseInstanceState.TERMINATED);
        }
    }

    @Test
    @CmmnDeployment
    public void testTerminateCaseInstanceWithNestedStages() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(8);
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());
        assertCaseInstanceEnded(caseInstance, 0);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId())
                    .singleResult();
            assertThat(historicCaseInstance).isNotNull();
            assertThat(historicCaseInstance.getStartTime()).isNotNull();
            assertThat(historicCaseInstance.getEndTime()).isNotNull();
            assertThat(historicCaseInstance.getState()).isEqualTo(CaseInstanceState.TERMINATED);
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
        assertThat(caseInstance.getName()).isEqualTo("test name");
        assertThat(caseInstance.getBusinessKey()).isEqualTo("test business key");
    }

    @Test
    @CmmnDeployment
    public void testCaseInstanceStarterIdentityLink() {
        Authentication.setAuthenticatedUserId("testUser");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        Authentication.setAuthenticatedUserId(null);

        List<IdentityLink> caseIdentityLinks = cmmnRuntimeService.getIdentityLinksForCaseInstance(caseInstance.getId());
        assertThat(caseIdentityLinks)
                .extracting(IdentityLink::getScopeId, IdentityLink::getScopeType, IdentityLink::getType, IdentityLink::getUserId)
                .containsExactly(tuple(caseInstance.getId(), ScopeTypes.CMMN, IdentityLinkType.STARTER, "testUser"));

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricIdentityLink> historicIdentityLinks = cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId());
            assertThat(historicIdentityLinks)
                    .extracting(
                            HistoricIdentityLink::getScopeId,
                            HistoricIdentityLink::getScopeType,
                            HistoricIdentityLink::getType,
                            HistoricIdentityLink::getUserId)
                    .containsExactly(tuple(caseInstance.getId(), ScopeTypes.CMMN, IdentityLinkType.STARTER, "testUser"));
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/RuntimeServiceTest.testStartSimplePassthroughCaseWithBlockingTask.cmmn" })
    public void planItemQueryWithoutTenant() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("var", "test")
                .variable("numberVar", 10)
                .start();

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Task A").planItemInstanceWithoutTenantId().count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("Task A").planItemInstanceWithoutTenantId().list()).hasSize(1);
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/runtime/RuntimeServiceTest.testStartSimplePassthroughCaseWithBlockingTask.cmmn" })
    public void testCaseInstanceQueryWithOrderByStartTime() {
        Instant now = Instant.now();
        setClockTo(Date.from(now));

        CaseInstance case1 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .businessKey("firstCase")
                .start();

        setClockTo(Date.from(now.plusSeconds(10)));

        CaseInstance case2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .businessKey("secondCase")
                .start();

        setClockTo(Date.from(now.plusSeconds(40)));

        CaseInstance case3 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .businessKey("thirdCase")
                .start();

        setClockTo(Date.from(now.plusSeconds(70)));

        CaseInstance case4 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .businessKey("fourthCase")
                .start();

        setClockTo(Date.from(now.plusSeconds(100)));

        CaseInstance case5 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .businessKey("fifthCase")
                .start();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().orderByStartTime().asc().list())
                .extracting(CaseInstance::getId, CaseInstance::getBusinessKey)
                .containsExactly(
                        tuple(case1.getId(), "firstCase"),
                        tuple(case2.getId(), "secondCase"),
                        tuple(case3.getId(), "thirdCase"),
                        tuple(case4.getId(), "fourthCase"),
                        tuple(case5.getId(), "fifthCase")
                );

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().orderByStartTime().asc().listPage(0, 3))
                .extracting(CaseInstance::getId, CaseInstance::getBusinessKey)
                .containsExactly(
                        tuple(case1.getId(), "firstCase"),
                        tuple(case2.getId(), "secondCase"),
                        tuple(case3.getId(), "thirdCase")
                );

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().orderByStartTime().asc().listPage(3, 10))
                .extracting(CaseInstance::getId, CaseInstance::getBusinessKey)
                .containsExactly(
                        tuple(case4.getId(), "fourthCase"),
                        tuple(case5.getId(), "fifthCase")
                );

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().orderByStartTime().asc().listPage(10, 20))
                .extracting(CaseInstance::getId, CaseInstance::getBusinessKey)
                .isEmpty();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().orderByStartTime().asc().list())
                .extracting(HistoricCaseInstance::getId, HistoricCaseInstance::getBusinessKey)
                .containsExactly(
                    tuple(case1.getId(), "firstCase"),
                    tuple(case2.getId(), "secondCase"),
                    tuple(case3.getId(), "thirdCase"),
                    tuple(case4.getId(), "fourthCase"),
                    tuple(case5.getId(), "fifthCase")
                );

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().orderByStartTime().asc().listPage(0, 3))
                .extracting(HistoricCaseInstance::getId, HistoricCaseInstance::getBusinessKey)
                .containsExactly(
                    tuple(case1.getId(), "firstCase"),
                    tuple(case2.getId(), "secondCase"),
                    tuple(case3.getId(), "thirdCase")
                );

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().orderByStartTime().asc().listPage(3, 10))
                .extracting(HistoricCaseInstance::getId, HistoricCaseInstance::getBusinessKey)
                .containsExactly(
                    tuple(case4.getId(), "fourthCase"),
                    tuple(case5.getId(), "fifthCase")
                );

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().orderByStartTime().asc().listPage(10, 20))
                .extracting(HistoricCaseInstance::getId, HistoricCaseInstance::getBusinessKey)
                .isEmpty();
        }
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/runtime/RuntimeServiceTest.testStartSimplePassthroughCaseWithBlockingTask.cmmn",
            "org/flowable/cmmn/test/runtime/oneHumanTaskCase.cmmn"
    })
    public void testCaseInstanceQueryWithOrderByCaseDefinitionId() {
        CaseInstance case1 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .businessKey("Pass Through Case")
                .start();

        CaseInstance case2 = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .businessKey("One Human Task Case")
                .start();

        String caseBusinessKeyWithHigherCaseDefId;
        String caseBusinessKeyWithLowerCaseDefId;

        if (case1.getCaseDefinitionId().compareTo(case2.getCaseDefinitionId()) > 0) {
            // Case Definition Id 1 has higher id
            caseBusinessKeyWithHigherCaseDefId = case1.getBusinessKey();
            caseBusinessKeyWithLowerCaseDefId = case2.getBusinessKey();
        } else {
            caseBusinessKeyWithHigherCaseDefId = case2.getBusinessKey();
            caseBusinessKeyWithLowerCaseDefId = case1.getBusinessKey();
        }

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().orderByCaseDefinitionId().asc().list())
                .extracting(CaseInstance::getBusinessKey)
                .containsExactly(
                        caseBusinessKeyWithLowerCaseDefId,
                        caseBusinessKeyWithHigherCaseDefId
                );

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().orderByCaseDefinitionId().asc().listPage(0, 3))
                .extracting(CaseInstance::getBusinessKey)
                .containsExactly(
                        caseBusinessKeyWithLowerCaseDefId,
                        caseBusinessKeyWithHigherCaseDefId
                );

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().orderByCaseDefinitionId().asc().listPage(10, 20))
                .extracting(CaseInstance::getId, CaseInstance::getBusinessKey)
                .isEmpty();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().orderByCaseDefinitionId().desc().list())
                .extracting(CaseInstance::getBusinessKey)
                .containsExactly(
                        caseBusinessKeyWithHigherCaseDefId,
                        caseBusinessKeyWithLowerCaseDefId
                );

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().orderByCaseDefinitionId().asc().list())
                .extracting(HistoricCaseInstance::getBusinessKey)
                .containsExactly(
                        caseBusinessKeyWithLowerCaseDefId,
                        caseBusinessKeyWithHigherCaseDefId
                );

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().orderByCaseDefinitionId().asc().listPage(0, 3))
                .extracting(HistoricCaseInstance::getBusinessKey)
                .containsExactly(
                        caseBusinessKeyWithLowerCaseDefId,
                        caseBusinessKeyWithHigherCaseDefId
                );

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().orderByCaseDefinitionId().asc().listPage(10, 20))
                .extracting(HistoricCaseInstance::getBusinessKey)
                .isEmpty();

            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().orderByCaseDefinitionId().desc().list())
                    .extracting(HistoricCaseInstance::getBusinessKey)
                    .containsExactly(
                            caseBusinessKeyWithHigherCaseDefId,
                            caseBusinessKeyWithLowerCaseDefId
                    );
        }
    }

    protected class TestStartCaseInstanceInterceptor implements StartCaseInstanceInterceptor {

        protected int beforeStartCaseInstanceCounter = 0;
        protected int afterStartCaseInstanceCounter = 0;

        @Override
        public void beforeStartCaseInstance(StartCaseInstanceBeforeContext instanceContext) {
            beforeStartCaseInstanceCounter++;
            Map<String, Object> varMap = new HashMap<>();
            varMap.put("beforeContextVar", "test");
            instanceContext.setVariables(varMap);
            instanceContext.setBusinessKey("testKey");
        }

        @Override
        public void afterStartCaseInstance(StartCaseInstanceAfterContext instanceContext) {
            afterStartCaseInstanceCounter++;
        }

        public int getBeforeStartCaseInstanceCounter() {
            return beforeStartCaseInstanceCounter;
        }

        public int getAfterStartCaseInstanceCounter() {
            return afterStartCaseInstanceCounter;
        }
    }

}