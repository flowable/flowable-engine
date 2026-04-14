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
package org.flowable.cmmn.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.common.engine.api.delegate.BusinessError;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * Tests for cross-engine BusinessError propagation between BPMN and CMMN.
 *
 * @author Joram Barrez
 */
public class CrossEngineBusinessErrorTest extends AbstractProcessEngineIntegrationTest {

    /**
     * CMMN Case
     * ┌────────────────────────────────────────┐
     * │  [Process Task]──fault sentry──▶[B]    │
     * │       │ starts                         │
     * │       ▼                                │
     * │  BPMN: start → [Throw Error] → end    │
     * │                 throws BpmnError        │
     * └────────────────────────────────────────┘
     * → BpmnError caught by fault sentry → B activates
     */
    @Test
    public void testProcessTaskBpmnErrorPropagatesAsFault() {
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CrossEngineBusinessErrorTest.testProcessTaskBpmnErrorPropagatesAsFault.cmmn")
                .deploy();
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/throwBpmnErrorProcess.bpmn20.xml")
                .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("testProcessTaskFault")
                    .start();

            // ProcessTask should be FAILED (BpmnError propagated as fault)
            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemInstanceState(PlanItemInstanceState.FAILED)
                    .includeEnded()
                    .list())
                    .extracting(PlanItemInstance::getName)
                    .containsExactly("Process Task");

            // The child BPMN process instance should be cleaned up
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().list()).isEmpty();

            // B should be active (fault sentry fired)
            List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(tasks).extracting(Task::getName).containsExactly("B");

            cmmnTaskService.complete(tasks.get(0).getId());
            assertCaseInstanceEnded(caseInstance);

        } finally {
            deleteDeployments(cmmnDeployment, bpmnDeployment);
        }
    }

    /**
     * BPMN Process
     * ┌─────────────────────────────────────────┐
     * │  start → [Case Task]──────────→ end     │
     * │               │                         │
     * │          (boundary error)                │
     * │               ▼                         │
     * │         [Error Handler] → end           │
     * └─────────────────────────────────────────┘
     *          starts ↓
     * CMMN Case
     * ┌──────────────────────┐
     * │  [A] throws CmmnFault │
     * │  (no sentry catches)  │
     * └──────────────────────┘
     * → CmmnFault caught by catch-all boundary error event → Error Handler activates
     */
    @Test
    public void testCaseTaskCmmnFaultCaughtByBoundaryEvent() {
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CrossEngineBusinessErrorTest.testCaseTaskCmmnFaultCaughtByBoundaryEvent.cmmn")
                .deploy();
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CrossEngineBusinessErrorTest.testCaseTaskCmmnFaultCaughtByBoundaryEvent.bpmn20.xml")
                .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("testCaseTaskFault");

            // The CmmnFault should have propagated to BPMN → caught by boundary error event
            // → "Error Handler" task should be active
            List<Task> tasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertThat(tasks).extracting(Task::getName).containsExactly("Error Handler");

            // The child CMMN case instance should be cleaned up
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().list()).isEmpty();

            processEngineTaskService.complete(tasks.get(0).getId());
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();

        } finally {
            deleteDeployments(cmmnDeployment, bpmnDeployment);
        }
    }

    /**
     * CMMN Case
     * ┌───────────────────────────────────────┐
     * │  [Process Task]  (no fault sentry)     │
     * │       │ starts                         │
     * │       ▼                                │
     * │  BPMN: start → [Throw Error] → end    │
     * └───────────────────────────────────────┘
     * → No fault sentry → BusinessError re-thrown to caller
     */
    @Test
    public void testProcessTaskBpmnErrorNotCaughtRethrows() {
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CrossEngineBusinessErrorTest.testProcessTaskBpmnErrorNotCaughtRethrows.cmmn")
                .deploy();
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/throwBpmnErrorProcess.bpmn20.xml")
                .deploy();

        try {
            assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("testProcessTaskNoFaultSentry")
                    .start())
                    .isInstanceOf(BusinessError.class);

        } finally {
            deleteDeployments(cmmnDeployment, bpmnDeployment);
        }
    }

    /**
     * CMMN Case
     * ┌──────────────────────────────────────────────┐
     * │  [Process Task]──complete sentry──▶[After Process] │
     * │       │ starts                                │
     * │       ▼                                       │
     * │  BPMN Process                                 │
     * │  ┌──────────────────────────────────┐         │
     * │  │ start → [Throw Error]────→ end   │         │
     * │  │              │                   │         │
     * │  │         (boundary error)         │         │
     * │  │              └──────────→ end    │         │
     * │  └──────────────────────────────────┘         │
     * └──────────────────────────────────────────────┘
     * → Error caught INSIDE BPMN process → process completes normally → After Process activates
     */
    @Test
    public void testProcessTaskBpmnErrorCaughtInsideProcess() {
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CrossEngineBusinessErrorTest.testProcessTaskBpmnErrorCaughtInsideProcess.cmmn")
                .deploy();
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CrossEngineBusinessErrorTest.testProcessTaskBpmnErrorCaughtInsideProcess.bpmn20.xml")
                .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("testProcessTaskErrorCaughtInside")
                    .start();

            // The error is caught inside the BPMN process, so the ProcessTask should complete normally
            // "After Process" task should be active
            List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(tasks).extracting(Task::getName).containsExactly("After Process");

            cmmnTaskService.complete(tasks.get(0).getId());
            assertCaseInstanceEnded(caseInstance);

        } finally {
            deleteDeployments(cmmnDeployment, bpmnDeployment);
        }
    }

    /**
     * BPMN Process
     * ┌──────────────────────────────────┐
     * │  start → [Case Task] → end      │
     * │          (no boundary event)     │
     * └──────────────────────────────────┘
     *          starts ↓
     * CMMN Case
     * ┌──────────────────────┐
     * │  [A] throws CmmnFault │
     * └──────────────────────┘
     * → No boundary event → BusinessError re-thrown to caller
     */
    @Test
    public void testCaseTaskCmmnFaultNotCaughtRethrows() {
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CrossEngineBusinessErrorTest.testCaseTaskCmmnFaultCaughtByBoundaryEvent.cmmn")
                .deploy();
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CrossEngineBusinessErrorTest.testCaseTaskCmmnFaultNotCaughtRethrows.bpmn20.xml")
                .deploy();

        try {
            assertThatThrownBy(() -> processEngineRuntimeService.startProcessInstanceByKey("testCaseTaskNoBoundary"))
                    .isInstanceOf(BusinessError.class);

        } finally {
            deleteDeployments(cmmnDeployment, bpmnDeployment);
        }
    }

    /**
     * BPMN Process
     * ┌────────────────────────────────────────────┐
     * │  start → [Case Task] → [After Case] → end │
     * └────────────────────────────────────────────┘
     *          starts ↓
     * CMMN Case (autoComplete)
     * ┌──────────────────────────────────────┐
     * │  [A] throws CmmnFault                │
     * │    └─fault sentry─▶ [B] (human task) │
     * └──────────────────────────────────────┘
     * → Fault caught INSIDE case by sentry → B activates → complete B → case completes → After Case activates
     */
    @Test
    public void testCaseTaskCmmnFaultCaughtInsideCase() {
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CrossEngineBusinessErrorTest.testCaseTaskCmmnFaultCaughtInsideCase.cmmn")
                .deploy();
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CrossEngineBusinessErrorTest.testCaseTaskCmmnFaultCaughtInsideCase.bpmn20.xml")
                .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("testCaseTaskFaultCaughtInside");

            // The fault is caught inside the CMMN case by a fault sentry → human task B is activated
            // Find the case instance started by the CaseTask
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                    .caseDefinitionKey("throwCmmnFaultCaughtInsideCase")
                    .singleResult();
            assertThat(caseInstance).isNotNull();

            // Task B should be active in the case (fault sentry fired)
            List<Task> caseTasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(caseTasks).extracting(Task::getName).containsExactly("B");

            // Complete B → case completes → CaseTask completes → "After Case" task activates
            cmmnTaskService.complete(caseTasks.get(0).getId());

            List<Task> processTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertThat(processTasks).extracting(Task::getName).containsExactly("After Case");

            processEngineTaskService.complete(processTasks.get(0).getId());
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();

        } finally {
            deleteDeployments(cmmnDeployment, bpmnDeployment);
        }
    }

    /**
     * BPMN Process
     * ┌─────────────────────────────────────────┐
     * │  start → [Case Task]──────────→ end     │
     * │               │                         │
     * │          (boundary error                 │
     * │           errorRef="CMMN_FAULT")         │
     * │               ▼                         │
     * │         [Error Handler] → end           │
     * └─────────────────────────────────────────┘
     *          starts ↓
     * CMMN Case
     * ┌────────────────────────────────────┐
     * │  [A] throws CmmnFault("CMMN_FAULT")│
     * └────────────────────────────────────┘
     * → Error code "CMMN_FAULT" matches boundary event → Error Handler activates
     */
    @Test
    public void testCaseTaskCmmnFaultCaughtBySpecificErrorCode() {
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CrossEngineBusinessErrorTest.testCaseTaskCmmnFaultCaughtByBoundaryEvent.cmmn")
                .deploy();
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CrossEngineBusinessErrorTest.testCaseTaskCmmnFaultCaughtBySpecificErrorCode.bpmn20.xml")
                .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("testCaseTaskSpecificErrorCode");

            // The CmmnFault with code "CMMN_FAULT" should be caught by the boundary error event with matching errorRef
            List<Task> tasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertThat(tasks).extracting(Task::getName).containsExactly("Error Handler");

            processEngineTaskService.complete(tasks.get(0).getId());
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();

        } finally {
            deleteDeployments(cmmnDeployment, bpmnDeployment);
        }
    }

    /**
     * BPMN Process
     * ┌─────────────────────────────────────────┐
     * │  start → [Case Task]──────────→ end     │
     * │               │                         │
     * │          (boundary error                 │
     * │           errorRef="WRONG_CODE")         │
     * │               ▼                         │
     * │         [Error Handler] → end           │
     * └─────────────────────────────────────────┘
     *          starts ↓
     * CMMN Case
     * ┌────────────────────────────────────┐
     * │  [A] throws CmmnFault("CMMN_FAULT")│
     * └────────────────────────────────────┘
     * → Error code "CMMN_FAULT" does NOT match "WRONG_CODE" → BusinessError re-thrown
     */
    @Test
    public void testCaseTaskCmmnFaultNotCaughtByMismatchedErrorCode() {
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CrossEngineBusinessErrorTest.testCaseTaskCmmnFaultCaughtByBoundaryEvent.cmmn")
                .deploy();
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CrossEngineBusinessErrorTest.testCaseTaskCmmnFaultNotCaughtByMismatchedErrorCode.bpmn20.xml")
                .deploy();

        try {
            assertThatThrownBy(() -> processEngineRuntimeService.startProcessInstanceByKey("testCaseTaskMismatchedErrorCode"))
                    .isInstanceOf(BusinessError.class);

        } finally {
            deleteDeployments(cmmnDeployment, bpmnDeployment);
        }
    }

    /**
     * CMMN Case
     * ┌──────────────────────────────────────────────────────┐
     * │  [Process Task]──fault sentry──▶[B]                  │
     * │       │ starts                                       │
     * │       ▼                                              │
     * │  BPMN: start → [Wait Task] → [Throw Error] → end   │
     * │                 (user task)    throws BpmnError       │
     * └──────────────────────────────────────────────────────┘
     * Transaction 1: start case → process reaches Wait Task (stops)
     * Transaction 2: complete Wait Task → Throw Error → BpmnError
     *                → onError callback → fault sentry fires → B activates
     */
    @Test
    public void testProcessTaskBpmnErrorAfterWaitState() {
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CrossEngineBusinessErrorTest.testProcessTaskBpmnErrorPropagatesAsFault.cmmn")
                .deploy();
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CrossEngineBusinessErrorTest.testProcessTaskBpmnErrorAfterWaitState.bpmn20.xml")
                .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("testProcessTaskFault")
                    .start();

            // The BPMN process should be at the user task (wait state)
            List<Task> bpmnTasks = processEngineTaskService.createTaskQuery().list();
            assertThat(bpmnTasks).extracting(Task::getName).containsExactly("Wait Task");

            // ProcessTask should still be ACTIVE (waiting for child process to complete)
            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                    .list())
                    .extracting(PlanItemInstance::getName)
                    .containsExactly("Process Task");

            // Complete the user task → next service task throws BpmnError
            // This happens in a new transaction — the error should propagate via callback to CMMN
            processEngineTaskService.complete(bpmnTasks.get(0).getId());

            // ProcessTask should be FAILED (BpmnError propagated as fault via callback)
            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemInstanceState(PlanItemInstanceState.FAILED)
                    .includeEnded()
                    .list())
                    .extracting(PlanItemInstance::getName)
                    .containsExactly("Process Task");

            // B should be active (fault sentry fired)
            List<Task> cmmnTasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(cmmnTasks).extracting(Task::getName).containsExactly("B");

            cmmnTaskService.complete(cmmnTasks.get(0).getId());
            assertCaseInstanceEnded(caseInstance);

        } finally {
            deleteDeployments(cmmnDeployment, bpmnDeployment);
        }
    }

    /**
     * BPMN Process
     * ┌─────────────────────────────────────────┐
     * │  start → [Case Task]──────────→ end     │
     * │               │                         │
     * │          (boundary error)                │
     * │               ▼                         │
     * │         [Error Handler] → end           │
     * └─────────────────────────────────────────┘
     *          starts ↓
     * CMMN Case (autoComplete)
     * ┌──────────────────────────────────────────────┐
     * │  [Wait Task]──complete sentry──▶[Throw Fault] │
     * │  (human task)                   throws CmmnFault│
     * └──────────────────────────────────────────────┘
     * Transaction 1: start process → case reaches Wait Task (stops)
     * Transaction 2: complete Wait Task → Throw Fault → CmmnFault
     *                → onError callback → boundary error event fires → Error Handler activates
     */
    @Test
    public void testCaseTaskCmmnFaultAfterWaitState() {
        CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CrossEngineBusinessErrorTest.testCaseTaskCmmnFaultAfterWaitState.cmmn")
                .deploy();
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CrossEngineBusinessErrorTest.testCaseTaskCmmnFaultAfterWaitState.bpmn20.xml")
                .deploy();

        try {
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("testCaseTaskFaultAfterWait");

            // The CMMN case should be at the human task (wait state)
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                    .caseDefinitionKey("throwCmmnFaultAfterWaitState")
                    .singleResult();
            assertThat(caseInstance).isNotNull();

            List<Task> cmmnTasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(cmmnTasks).extracting(Task::getName).containsExactly("Wait Task");

            // Complete the human task → service task B throws CmmnFault (no sentry catches it)
            // This happens in a new transaction — the fault should propagate via callback to BPMN
            cmmnTaskService.complete(cmmnTasks.get(0).getId());

            // The child CMMN case should be cleaned up (autocomplete: both plan items in terminal state)
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseDefinitionKey("throwCmmnFaultAfterWaitState").count()).isZero();

            // The CmmnFault should have propagated to BPMN → caught by boundary error event
            // → "Error Handler" task should be active
            List<Task> bpmnTasks = processEngineTaskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
            assertThat(bpmnTasks).extracting(Task::getName).containsExactly("Error Handler");

            processEngineTaskService.complete(bpmnTasks.get(0).getId());
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();

        } finally {
            deleteDeployments(cmmnDeployment, bpmnDeployment);
        }
    }

    private void deleteDeployments(CmmnDeployment cmmnDeployment, Deployment bpmnDeployment) {
        // Clean up CMMN first (ends case instances), then BPMN.
        cmmnRepositoryService.deleteDeployment(cmmnDeployment.getId(), true);
        try {
            processEngineRepositoryService.deleteDeployment(bpmnDeployment.getId(), true);
        } catch (Exception e) {
            // Ignore — cascading cleanup may fail if cross-engine references are already cleaned
        }
    }
}
