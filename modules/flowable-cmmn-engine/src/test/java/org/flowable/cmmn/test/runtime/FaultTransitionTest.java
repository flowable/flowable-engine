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

import java.util.List;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.common.engine.api.delegate.BusinessError;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

public class FaultTransitionTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testBasicFaultTransitionFiresSentry() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testBasicFault")
                .variable("faultCode", "BUSINESS_ERROR")
                .start();

        // Task A should have faulted, task B should be active (sentry fired)
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.FAILED)
                .includeEnded()
                .list())
                .extracting(PlanItemInstance::getName)
                .containsExactly("A");

        // B should be active
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).extracting(Task::getName).containsExactly("B");

        // Complete B -> case should complete
        cmmnTaskService.complete(tasks.get(0).getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }

    @Test
    @CmmnDeployment
    public void testFaultCodeMatchingViaIfPart() {
        // Throw INSUFFICIENT_FUNDS -> only B should activate, as it checks the fault code
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testFaultCodeMatching")
                .variable("faultCode", "INSUFFICIENT_FUNDS")
                .start();

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).extracting(Task::getName).containsExactly("B");

        cmmnTaskService.complete(tasks.get(0).getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/FaultTransitionTest.testFaultCodeMatchingViaIfPart.cmmn")
    public void testFaultCodeMatchingViaIfPartDifferentCode() {
        // Throw ACCOUNT_LOCKED -> only C should activate, not B (like in the previous test)
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testFaultCodeMatching")
                .variable("faultCode", "ACCOUNT_LOCKED")
                .start();

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).extracting(Task::getName).containsExactly("C");

        cmmnTaskService.complete(tasks.get(0).getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }

    @Test
    @CmmnDeployment
    public void testFaultWithoutSentry() {
        // No fault sentry exists → BusinessError re-thrown (like BPMN when no boundary event catches)
        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testFaultNoSentry")
                .variable("faultCode", "SOME_ERROR")
                .start())
                .isInstanceOf(BusinessError.class);
    }

    @Test
    @CmmnDeployment
    public void testRegularExceptionNotCaughtAsFault() {
        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testRegularException")
                .start())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Technical error");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/FaultTransitionTest.testNonPropagationChildrenUnaffected.cmmn")
    public void testUncaughtFaultInStageRethrows() {
        // Stage has no fault sentry, nothing catches at any level → BusinessError re-thrown
        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testNonPropagation")
                .variable("faultCode", "BUSINESS_ERROR")
                .start())
                .isInstanceOf(BusinessError.class);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/FaultTransitionTest.testBasicFaultTransitionFiresSentry.cmmn")
    public void testHistoryVerification() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testBasicFault")
                .variable("faultCode", "BUSINESS_ERROR")
                .start();

        if (cmmnEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            HistoricPlanItemInstance historicA = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .planItemInstanceName("A")
                    .singleResult();

            assertThat(historicA).isNotNull();
            assertThat(historicA.getState()).isEqualTo(PlanItemInstanceState.FAILED);
            assertThat(historicA.getFailedTime()).isNotNull();
            assertThat(historicA.getEndedTime()).isNotNull();

            // Complete B to end the case
            List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
            cmmnTaskService.complete(tasks.get(0).getId());
        }

    }

    @Test
    @CmmnDeployment
    public void testMultipleSentriesOnSameFaultSource() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testMultipleSentries")
                .variable("faultCode", "BUSINESS_ERROR")
                .start();

        // A faults, B, C, D should all activate
        List<Task> tasks = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance.getId())
                .orderByTaskName().asc()
                .list();
        assertThat(tasks).extracting(Task::getName).containsExactly("B", "C", "D");

        for (Task task : tasks) {
            cmmnTaskService.complete(task.getId());
        }
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }

    @Test
    @CmmnDeployment
    public void testExitFromFailedState() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testExitFromFailed")
                .variable("faultCode", "BUSINESS_ERROR")
                .start();

        // A should be FAILED, B should be active (from fault sentry)
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.FAILED)
                .includeEnded()
                .list())
                .extracting(PlanItemInstance::getName)
                .containsExactly("A");

        // Trigger the user event listener to fire exit criterion on the stage
        PlanItemInstance userEventListener = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemDefinitionType("usereventlistener")
                .singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListener.getId());

        // Case should be ended
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }

    // Required plan item faults with no fault sentry -> BusinessError re-thrown
    @Test
    @CmmnDeployment
    public void testRequiredPlanItemFaults() {
        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testRequiredFault")
                .variable("faultCode", "BUSINESS_ERROR")
                .start())
                .isInstanceOf(BusinessError.class);
    }

    @Test
    @CmmnDeployment
    public void testScriptTaskFault() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testScriptFault")
                .start();

        // Script task A should be FAILED
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.FAILED)
                .includeEnded()
                .list())
                .extracting(PlanItemInstance::getName)
                .containsExactly("A");

        // B should be active (fault sentry fired)
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).extracting(Task::getName).containsExactly("B");

        cmmnTaskService.complete(tasks.get(0).getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }

    // Fault sentry crosses stage boundaries — sentry at outer level references task inside a stage
    @Test
    @CmmnDeployment
    public void testFaultCaughtAcrossStageBoundary() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testFaultAcrossStageBoundary")
                .variable("faultCode", "BUSINESS_ERROR")
                .start();

        // A faults inside Stage S, C at the outer level has a fault sentry on A → C activates
        // B inside the stage is unaffected (still active)
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).extracting(Task::getName).containsExactly("B", "C");

        // Complete both → stage completes (A=FAILED + B=COMPLETED) → case completes
        for (Task task : tasks) {
            cmmnTaskService.complete(task.getId());
        }
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }

    // Fault caught at task level inside a stage → stage stays ACTIVE, siblings unaffected
    @Test
    @CmmnDeployment
    public void testFaultCaughtInsideStageDoesNotAffectStage() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testFaultCaughtInsideStage")
                .variable("faultCode", "BUSINESS_ERROR")
                .start();

        // A faults, B catches it (fault sentry on A). C is unaffected sibling.
        // Stage should stay ACTIVE — fault was caught locally.
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).extracting(Task::getName).containsExactly("B", "C");

        // Complete B and C → stage completes → case completes
        for (Task task : tasks) {
            cmmnTaskService.complete(task.getId());
        }
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }

    @Test
    @CmmnDeployment
    public void testTaskListenerThrowsFaultOnComplete() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testListenerFault")
                .start();

        // A should be active (human task)
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).extracting(Task::getName).containsExactly("A");

        // Complete A → task listener throws CmmnFault → A goes to FAILED → B activates
        cmmnTaskService.complete(tasks.get(0).getId());

        // A should be FAILED
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.FAILED)
                .includeEnded()
                .list())
                .extracting(PlanItemInstance::getName)
                .containsExactly("A");

        // B should be active (fault sentry fired)
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).extracting(Task::getName).containsExactly("B");

        cmmnTaskService.complete(tasks.get(0).getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }

    // Async leave with fault: error data (faultCode) must be preserved through the async job
    @Test
    @CmmnDeployment
    public void testAsyncLeaveFaultPreservesErrorData() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testAsyncLeaveFault")
                .variable("faultCode", "ASYNC_ERROR")
                .start();

        // A should be in ASYNC_ACTIVE_LEAVE state (async leave configured)
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.ASYNC_ACTIVE_LEAVE)
                .list())
                .extracting(PlanItemInstance::getName)
                .containsExactly("A");

        // Execute the async job
        waitForJobExecutorToProcessAllJobs();

        // A should be FAILED (fault transition completed via async job)
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.FAILED)
                .includeEnded()
                .list())
                .extracting(PlanItemInstance::getName)
                .containsExactly("A");

        // B should be active — the sentry if-part ${faultCode == 'ASYNC_ERROR'} matched
        // because the error data was preserved through the async job serialization
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).extracting(Task::getName).containsExactly("B");

        cmmnTaskService.complete(tasks.get(0).getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }

    // Non-CmmnFault BusinessError (e.g. BpmnError) thrown from CMMN service task → plan item should still fault
    @Test
    @CmmnDeployment
    public void testNonCmmnFaultBusinessErrorTriggersFault() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testBusinessError")
                .start();

        // A threw a BusinessError subclass (not CmmnFault, e.g. BpmnError) → should still trigger the fault transition
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.FAILED)
                .includeEnded()
                .list())
                .extracting(PlanItemInstance::getName)
                .containsExactly("A");

        // B should be active (fault sentry fired)
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).extracting(Task::getName).containsExactly("B");

        cmmnTaskService.complete(tasks.get(0).getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }
}
