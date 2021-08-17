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
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.COMPLETED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.TERMINATED;

import java.util.List;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.identity.Authentication;
import org.junit.Test;

/**
 * Testing child plan items termination on autocompletion of a case or stage to leave them in an end state and making sure they are not completed, but rather
 * terminated.
 *
 * @author Micha Kiener
 */
public class TerminateChildPlanItemsOnAutoCompletionTest extends FlowableCmmnTestCase {
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/Case_Completion_With_Child_Items_Test.cmmn.xml")
    public void terminateChildPlanItemsOnCaseCompletionTest() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("JohnDoe");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseCompletionWithChildItemsTest")
                .start();

            waitForAsyncHistoryExecutorToProcessAllJobs();

            List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
            assertThat(planItemInstances).hasSize(5);
            assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

            waitForAsyncHistoryExecutorToProcessAllJobs();

            assertPlanItemInstanceState(planItemInstances, "Ignore after first completion task", ACTIVE);
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Ignore after first completion task"));

            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
            assertCaseInstanceEnded(caseInstance);

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                List<HistoricPlanItemInstance> historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                        .planItemInstanceCaseInstanceId(caseInstance.getId())
                        .list();

                assertThat(historicPlanItems).hasSize(6);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Stage A", COMPLETED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Task A", COMPLETED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Manual task", TERMINATED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Ignored task", TERMINATED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Ignore after first completion task", COMPLETED, TERMINATED);
            }
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/Stage_Completion_With_Child_Items_Test.cmmn.xml")
    public void terminateChildPlanItemsOnStageCompletionTest() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("JohnDoe");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("stageCompletionWithChildItemsTest")
                .start();

            waitForAsyncHistoryExecutorToProcessAllJobs();

            List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
            assertThat(planItemInstances).hasSize(5);
            assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

            waitForAsyncHistoryExecutorToProcessAllJobs();

            assertPlanItemInstanceState(planItemInstances, "Ignore after first completion task", ACTIVE);
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Ignore after first completion task"));

            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
            assertCaseInstanceEnded(caseInstance);

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                List<HistoricPlanItemInstance> historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                        .planItemInstanceCaseInstanceId(caseInstance.getId())
                        .list();

                assertThat(historicPlanItems).hasSize(6);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Stage A", COMPLETED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Task A", COMPLETED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Manual task", TERMINATED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Ignored task", TERMINATED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Ignore after first completion task", COMPLETED, TERMINATED);
            }
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/Case_And_Stage_Completion_With_Child_Items_Test.cmmn.xml")
    public void terminateChildPlanItemsOnStageAndCaseCompletionTest() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("JohnDoe");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseAndStageCompletionWithChildItemsTest")
                .start();

            waitForAsyncHistoryExecutorToProcessAllJobs();

            List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
            assertThat(planItemInstances).hasSize(8);
            assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

            waitForAsyncHistoryExecutorToProcessAllJobs();

            assertPlanItemInstanceState(planItemInstances, "Ignore after first completion stage task", ACTIVE);
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Ignore after first completion stage task"));

            waitForAsyncHistoryExecutorToProcessAllJobs();

            planItemInstances = getPlanItemInstances(caseInstance.getId());
            assertPlanItemInstanceState(planItemInstances, "Ignore after first completion task", ACTIVE);
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Ignore after first completion task"));

            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
            assertCaseInstanceEnded(caseInstance);

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                List<HistoricPlanItemInstance> historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                        .planItemInstanceCaseInstanceId(caseInstance.getId())
                        .list();

                assertThat(historicPlanItems).hasSize(10);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Stage A", COMPLETED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Task A", COMPLETED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Manual stage task", TERMINATED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Ignored stage task", TERMINATED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Ignore after first completion stage task", COMPLETED, TERMINATED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Manual task", TERMINATED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Ignored task", TERMINATED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Ignore after first completion task", COMPLETED, TERMINATED);
            }
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

}
