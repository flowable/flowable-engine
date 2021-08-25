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
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ENABLED;
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
 * Testing ending delegation for case pages as they implement the optional {@link org.flowable.cmmn.engine.impl.behavior.OnParentEndDependantActivityBehavior}
 * interface to overwrite the default behavior.
 *
 * @author Micha Kiener
 */
public class PropagatedCasePageEndingStateTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/Case_Page_Ending_State_Test_Case.cmmn.xml")
    public void testCasePageEndingStateOnStageComplete() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("JohnDoe");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("casePageEndingStateTestCase")
                .start();

            List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
            assertThat(planItemInstances).hasSize(7);
            assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
            assertPlanItemInstanceState(planItemInstances, "Case page A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
            assertPlanItemInstanceState(planItemInstances, "Case page B", ACTIVE);

            // complete Task A and B to complete Stage A
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));
            cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));

            planItemInstances = getAllPlanItemInstances(caseInstance.getId());
            assertThat(planItemInstances).hasSize(7);
            assertPlanItemInstanceState(planItemInstances, "Stage A", COMPLETED);
            assertPlanItemInstanceState(planItemInstances, "Task A", COMPLETED);
            assertPlanItemInstanceState(planItemInstances, "Task B", COMPLETED);
            assertPlanItemInstanceState(planItemInstances, "Case page A", COMPLETED);
            assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
            assertPlanItemInstanceState(planItemInstances, "Case page B", ACTIVE);
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/Case_Page_Ending_State_Test_Case.cmmn.xml")
    public void testCasePageEndingStateOnCaseComplete() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("JohnDoe");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("casePageEndingStateTestCase")
                .start();

            waitForAsyncHistoryExecutorToProcessAllJobs();

            List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
            assertThat(planItemInstances).hasSize(7);
            assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
            assertPlanItemInstanceState(planItemInstances, "Case page A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
            assertPlanItemInstanceState(planItemInstances, "Case page B", ACTIVE);

            // complete Task A and B to complete Stage A
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));
            cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));

            waitForAsyncHistoryExecutorToProcessAllJobs();
            
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));
            
            waitForAsyncHistoryExecutorToProcessAllJobs();

            // now trigger Task C and D to complete the case
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task C"));
            
            waitForAsyncHistoryExecutorToProcessAllJobs();
            
            cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task D"));
            
            waitForAsyncHistoryExecutorToProcessAllJobs();
            
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task D"));

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                // check the historic plan item instances, as the case is already terminated and no longer in the runtime tables
                List<HistoricPlanItemInstance> historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).list();
                assertThat(historicPlanItems).hasSize(7);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Stage A", COMPLETED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Task A", COMPLETED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Task B", COMPLETED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Case page A", COMPLETED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Task C", COMPLETED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Task D", COMPLETED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Case page B", COMPLETED);
            }

            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
            assertCaseInstanceEnded(caseInstance);
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/Case_Page_Ending_State_Test_Case.cmmn.xml")
    public void testCasePageEndingStateOnStageExitBySentry() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("JohnDoe");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("casePageEndingStateTestCase")
                .start();

            List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
            assertThat(planItemInstances).hasSize(7);
            assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
            assertPlanItemInstanceState(planItemInstances, "Case page A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
            assertPlanItemInstanceState(planItemInstances, "Case page B", ACTIVE);

            // exit Stage A to also terminate the task and the case page
            cmmnRuntimeService.setVariable(caseInstance.getId(), "exitStageA", true);

            planItemInstances = getAllPlanItemInstances(caseInstance.getId());
            assertThat(planItemInstances).hasSize(7);
            assertPlanItemInstanceState(planItemInstances, "Stage A", TERMINATED);
            assertPlanItemInstanceState(planItemInstances, "Task A", TERMINATED);
            assertPlanItemInstanceState(planItemInstances, "Task B", TERMINATED);
            assertPlanItemInstanceState(planItemInstances, "Case page A", TERMINATED);
            assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
            assertPlanItemInstanceState(planItemInstances, "Case page B", ACTIVE);
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/Case_Page_Ending_State_Test_Case.cmmn.xml")
    public void testCasePageEndingStateOnCaseExitBySentry() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("JohnDoe");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("casePageEndingStateTestCase")
                .start();

            waitForAsyncHistoryExecutorToProcessAllJobs();

            List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
            assertThat(planItemInstances).hasSize(7);
            assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
            assertPlanItemInstanceState(planItemInstances, "Case page A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
            assertPlanItemInstanceState(planItemInstances, "Case page B", ACTIVE);

            // exit Stage A to also terminate the task and the case page
            cmmnRuntimeService.setVariable(caseInstance.getId(), "exitCase", true);

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                // check the historic plan item instances, as the case is already terminated and no longer in the runtime tables
                List<HistoricPlanItemInstance> historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).list();
                assertThat(historicPlanItems).hasSize(7);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Stage A", TERMINATED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Task A", TERMINATED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Task B", TERMINATED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Case page A", TERMINATED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Task C", TERMINATED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Task D", TERMINATED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Case page B", TERMINATED);
            }

            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
            assertCaseInstanceEnded(caseInstance);
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/Case_Page_Ending_State_Test_Case.cmmn.xml")
    public void testCasePageEndingStateOnStageCompleteBySentry() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("JohnDoe");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("casePageEndingStateTestCase")
                .start();

            List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
            assertThat(planItemInstances).hasSize(7);
            assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
            assertPlanItemInstanceState(planItemInstances, "Case page A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
            assertPlanItemInstanceState(planItemInstances, "Case page B", ACTIVE);

            // complete Task A to make the stage completable
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));

            // complete Stage A to also end the task and the case page
            cmmnRuntimeService.setVariable(caseInstance.getId(), "completeStageA", true);

            planItemInstances = getAllPlanItemInstances(caseInstance.getId());
            assertThat(planItemInstances).hasSize(7);
            assertPlanItemInstanceState(planItemInstances, "Stage A", COMPLETED);
            assertPlanItemInstanceState(planItemInstances, "Task A", COMPLETED);
            assertPlanItemInstanceState(planItemInstances, "Task B", TERMINATED);
            assertPlanItemInstanceState(planItemInstances, "Case page A", COMPLETED);
            assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
            assertPlanItemInstanceState(planItemInstances, "Case page B", ACTIVE);
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/Case_Page_Ending_State_Test_Case.cmmn.xml")
    public void testCasePageEndingStateOnCaseCompleteBySentry() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("JohnDoe");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("casePageEndingStateTestCase")
                .start();

            waitForAsyncHistoryExecutorToProcessAllJobs();

            List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
            assertThat(planItemInstances).hasSize(7);
            assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
            assertPlanItemInstanceState(planItemInstances, "Case page A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
            assertPlanItemInstanceState(planItemInstances, "Case page B", ACTIVE);

            // complete Task A and B to complete Stage A, then Task C to make the case completable
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task A"));
            cmmnRuntimeService.startPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));
            
            waitForAsyncHistoryExecutorToProcessAllJobs();
            
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task B"));
            cmmnRuntimeService.triggerPlanItemInstance(getPlanItemInstanceIdByName(planItemInstances, "Task C"));
            
            waitForAsyncHistoryExecutorToProcessAllJobs();

            // exit Stage A to also terminate the task and the case page
            cmmnRuntimeService.setVariable(caseInstance.getId(), "completeCase", true);

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                // check the historic plan item instances, as the case is already terminated and no longer in the runtime tables
                List<HistoricPlanItemInstance> historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).list();
                assertThat(historicPlanItems).hasSize(7);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Stage A", COMPLETED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Task A", COMPLETED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Task B", COMPLETED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Case page A", COMPLETED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Task C", COMPLETED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Task D", TERMINATED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Case page B", COMPLETED);
            }

            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
            assertCaseInstanceEnded(caseInstance);
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/Case_Page_Ending_State_Test_Case.cmmn.xml")
    public void testCasePageEndingStateOnStageForceCompleteBySentry() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("JohnDoe");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("casePageEndingStateTestCase")
                .start();

            List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
            assertThat(planItemInstances).hasSize(7);
            assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
            assertPlanItemInstanceState(planItemInstances, "Case page A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
            assertPlanItemInstanceState(planItemInstances, "Case page B", ACTIVE);

            // complete Stage A to also end the task and the case page
            cmmnRuntimeService.setVariable(caseInstance.getId(), "forceCompleteStageA", true);

            planItemInstances = getAllPlanItemInstances(caseInstance.getId());
            assertThat(planItemInstances).hasSize(7);
            assertPlanItemInstanceState(planItemInstances, "Stage A", COMPLETED);
            assertPlanItemInstanceState(planItemInstances, "Task A", TERMINATED);
            assertPlanItemInstanceState(planItemInstances, "Task B", TERMINATED);
            assertPlanItemInstanceState(planItemInstances, "Case page A", COMPLETED);
            assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
            assertPlanItemInstanceState(planItemInstances, "Case page B", ACTIVE);
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/Case_Page_Ending_State_Test_Case.cmmn.xml")
    public void testCasePageEndingStateOnCaseForceCompleteBySentry() {
        String previousUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("JohnDoe");
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("casePageEndingStateTestCase")
                .start();

            waitForAsyncHistoryExecutorToProcessAllJobs();

            List<PlanItemInstance> planItemInstances = getPlanItemInstances(caseInstance.getId());
            assertThat(planItemInstances).hasSize(7);
            assertPlanItemInstanceState(planItemInstances, "Stage A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task B", ENABLED);
            assertPlanItemInstanceState(planItemInstances, "Case page A", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task C", ACTIVE);
            assertPlanItemInstanceState(planItemInstances, "Task D", ENABLED);
            assertPlanItemInstanceState(planItemInstances, "Case page B", ACTIVE);

            // exit Stage A to also terminate the task and the case page
            cmmnRuntimeService.setVariable(caseInstance.getId(), "forceCompleteCase", true);

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                // check the historic plan item instances, as the case is already terminated and no longer in the runtime tables
                List<HistoricPlanItemInstance> historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).list();
                assertThat(historicPlanItems).hasSize(7);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Stage A", TERMINATED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Task A", TERMINATED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Task B", TERMINATED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Case page A", TERMINATED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Task C", TERMINATED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Task D", TERMINATED);
                assertHistoricPlanItemInstanceState(historicPlanItems, "Case page B", COMPLETED);
            }

            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isZero();
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
            assertCaseInstanceEnded(caseInstance);
        } finally {
            Authentication.setAuthenticatedUserId(previousUserId);
        }
    }
}
