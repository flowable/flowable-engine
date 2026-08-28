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
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.WAITING_FOR_REPETITION;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * Reproduction, across the process and case engines, of the Design "Case complete" app.
 *
 * <p>{@code caseCompleteOne} has a {@code Start} milestone (occurs immediately) that activates stage one,
 * whose human task is the single wait state. Completing it activates stage two, which holds a blocking
 * process task ({@code processCompleteOne}, producing the single-element {@code caseList} collection
 * variable) whose completion gates a collection-repeating case task over {@code caseCompleteTwo}. The
 * child case - and its own two blocking process tasks - complete synchronously within the same command.
 * Once the repeating case task has processed its single collection element, stage two must complete and
 * stage three's {@code Completed} human task must become active. Every plan item container is
 * {@code autoComplete=false}, matching the Design models.
 *
 * <p>Regression test: the collection-repeating case task, being gated by an entry criterion, keeps an
 * {@code AVAILABLE} repetition template behind after its single element completes so the criterion could
 * trigger the collection again. That leftover template used to keep the non-auto-complete stage two
 * {@code ACTIVE} indefinitely, so stage two never fired its {@code complete} event, stage three's entry
 * sentry never triggered and {@code Completed} was never reached. Completion evaluation now treats an
 * available collection repetition template that is gated by an entry criterion as completion-neutral (see
 * {@code PlanItemInstanceContainerUtil}), so the stage completes once its actual work is done. This is the
 * non-auto-complete counterpart of the
 * agenda ordering issue fixed for auto-complete containers in {@code RepeatingCaseTaskSyncChildTest}
 * (flowable-cmmn-engine).
 */
public class RepeatingCaseTaskProcessSyncChildTest extends AbstractProcessEngineIntegrationTest {

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteOne.cmmn",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteTwo.cmmn"
    })
    @org.flowable.engine.test.Deployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteOne.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteTwo.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteThree.bpmn20.xml"
    })
    public void testCompletedTaskIsReachedAfterSynchronousRepeatingCaseTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseCompleteOne")
                .start();

        // The Start milestone occurs immediately and activates stage one, whose human task waits.
        Task humanTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(humanTask).as("the stage one human task should be active").isNotNull();
        assertThat(humanTask.getName()).isEqualTo("Human task");

        // Completing it runs the synchronous cascade: process task -> repeating case task -> child case
        // (with its own process tasks), all completing within this command.
        cmmnTaskService.complete(humanTask.getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            // The child case (the case task repetition) must actually have run to completion; this guards against
            // the repetition being skipped entirely while stage two still reaches 'Completed'.
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("caseCompleteTwo").finished().count())
                    .as("the child case (repetition) should have run to completion").isEqualTo(1);
        }

        // Stage two must have completed and stage three's 'Completed' human task must be active.
        assertThat(getActivePlanItemInstances(caseInstance, "Completed"))
                .as("'Completed' should be active once the case task repetition completed")
                .hasSize(1);

        // The case must still be running: it must not have completed before reaching stage three.
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult())
                .as("the case should still be active").isNotNull();
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(0);
    }
    
    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteOne.cmmn",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseTaskCompleteTwo.cmmn"
    })
    @org.flowable.engine.test.Deployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteOne.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteThree.bpmn20.xml"
    })
    public void testCompletedTaskIsReachedAfterRepeatingCaseTaskWithTaskInSubCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseCompleteOne")
                .start();

        // The Start milestone occurs immediately and activates stage one, whose human task waits.
        Task humanTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(humanTask).as("the stage one human task should be active").isNotNull();
        assertThat(humanTask.getName()).isEqualTo("Human task");

        // Completing it activates stage two: the process task completes synchronously and triggers the
        // collection-repeating case task, which starts the child case. Unlike the other repro, this child case
        // has its own human task, so it does NOT complete synchronously - the case task stays active.
        cmmnTaskService.complete(humanTask.getId());

        // Exactly one child case (a single collection element) has been created and is waiting on its human task.
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().parentCaseInstanceId(caseInstance.getId()).list())
                .as("the single child case (case task repetition) should be running").hasSize(1);
        
        Task subCaseTask = cmmnTaskService.createTaskQuery().taskName("Task complete two").singleResult();
        assertThat(subCaseTask).as("the child case human task should be active").isNotNull();
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(4);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("caseTaskTwo").list()).hasSize(2);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("caseTaskTwo").planItemInstanceStateActive().list()).hasSize(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("caseTaskTwo").planItemInstanceStateAvailable().list()).hasSize(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage2").planItemInstanceStateActive().list()).hasSize(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage3").planItemInstanceStateAvailable().list()).hasSize(1);

        cmmnTaskService.complete(subCaseTask.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().parentCaseInstanceId(caseInstance.getId()).list()).hasSize(0);
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(2);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage3").planItemInstanceStateActive().list()).hasSize(1);
        
        assertThat(getActivePlanItemInstances(caseInstance, "Completed"))
            .as("'Completed' should be active once the case task repetition completed")
            .hasSize(1);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(0);
    }
    
    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteOneWithRepeatingHumanTask.cmmn",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteTwo.cmmn"
    })
    @org.flowable.engine.test.Deployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteOne.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteTwo.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteThree.bpmn20.xml"
    })
    public void testCompletedTaskIsReachedWithRepetitionAfterSynchronous() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseCompleteOne")
                .start();

        // The Start milestone occurs immediately and activates stage one, whose human task waits.
        Task humanTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(humanTask).as("the stage one human task should be active").isNotNull();
        assertThat(humanTask.getName()).isEqualTo("Human task");

        cmmnTaskService.complete(humanTask.getId());
        
        Task repeatingTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(repeatingTask.getName()).isEqualTo("Repeating task");
        
        cmmnTaskService.complete(repeatingTask.getId());
        
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            // The child case (the case task repetition) must actually have run to completion; this guards against
            // the repetition being skipped entirely while stage two still reaches 'Completed'.
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("caseCompleteTwo").finished().count())
                    .as("the child case (repetition) should have run to completion").isEqualTo(1);
        }

        // Stage two must have completed and stage three's 'Completed' human task must be active.
        assertThat(getActivePlanItemInstances(caseInstance, "Completed"))
                .as("'Completed' should be active once the case task repetition completed")
                .hasSize(1);

        // The case must still be running: it must not have completed before reaching stage three.
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult())
                .as("the case should still be active").isNotNull();
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(0);
    }
    
    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteOneWithRepeatingHumanTask.cmmn",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseTaskCompleteTwo.cmmn"
    })
    @org.flowable.engine.test.Deployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteOne.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteThree.bpmn20.xml"
    })
    public void testCompletedTaskIsReachedWithRepetitionAfterRepeatingCaseTaskWithTaskInSubCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseCompleteOne")
                .start();

        // The Start milestone occurs immediately and activates stage one, whose human task waits.
        Task humanTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(humanTask).as("the stage one human task should be active").isNotNull();
        assertThat(humanTask.getName()).isEqualTo("Human task");

        // Completing it activates stage two: the process task completes synchronously and triggers the
        // collection-repeating case task, which starts the child case. Unlike the other repro, this child case
        // has its own human task, so it does NOT complete synchronously - the case task stays active.
        cmmnTaskService.complete(humanTask.getId());
        
        Task repeatingTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(repeatingTask.getName()).isEqualTo("Repeating task");
        
        cmmnTaskService.complete(repeatingTask.getId());

        // Exactly one child case (a single collection element) has been created and is waiting on its human task.
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().parentCaseInstanceId(caseInstance.getId()).list())
                .as("the single child case (case task repetition) should be running").hasSize(1);
        
        Task subCaseTask = cmmnTaskService.createTaskQuery().taskName("Task complete two").singleResult();
        assertThat(subCaseTask).as("the child case human task should be active").isNotNull();
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(5);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("caseTaskTwo").list()).hasSize(2);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("caseTaskTwo").planItemInstanceStateActive().list()).hasSize(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("caseTaskTwo").planItemInstanceStateAvailable().list()).hasSize(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage2").planItemInstanceStateActive().list()).hasSize(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage3").planItemInstanceStateAvailable().list()).hasSize(1);

        cmmnTaskService.complete(subCaseTask.getId());
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().parentCaseInstanceId(caseInstance.getId()).list()).hasSize(0);
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(2);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stage3").planItemInstanceStateActive().list()).hasSize(1);
        
        assertThat(getActivePlanItemInstances(caseInstance, "Completed"))
            .as("'Completed' should be active once the case task repetition completed")
            .hasSize(1);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(0);
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteOneWithRepeatingUserEventListener.cmmn",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteTwo.cmmn"
    })
    @org.flowable.engine.test.Deployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteOne.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteTwo.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteThree.bpmn20.xml"
    })
    public void testUserEventListenerRepeatedlyTriggersRepeatingCaseTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseCompleteOne")
                .start();

        // The Start milestone occurs immediately and activates stage one, whose human task waits.
        Task humanTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(humanTask).as("the stage one human task should be active").isNotNull();
        assertThat(humanTask.getName()).isEqualTo("Human task");

        // Completing it activates stage two: the process task completes synchronously (producing 'caseList') and
        // the repeating user event listener 'Trigger' becomes available. No repeating task or case task runs yet.
        cmmnTaskService.complete(humanTask.getId());
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list()).isEmpty();

        // Trigger the user event listener; because it is repeating it re-arms after each occurrence.
        UserEventListenerInstance trigger = cmmnRuntimeService.createUserEventListenerInstanceQuery()
                .caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(trigger).as("the repeating user event listener should be available").isNotNull();
        cmmnRuntimeService.triggerPlanItemInstance(trigger.getId());

        // Each occurrence activates exactly one fresh 'Repeating task'.
        assertThat(getActivePlanItemInstances(caseInstance, "Repeating task"))
                .as("a repeating task should be active after triggering the event listener").hasSize(1);

        // Completing it fires the case task, whose child case runs and completes synchronously.
        Task repeatingTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId())
                .taskName("Repeating task").singleResult();
        cmmnTaskService.complete(repeatingTask.getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("caseCompleteTwo").finished().count())
                    .as("one child case should have completed per trigger").isEqualTo(1);
        }

        // Between triggers no child case is left running, and both repetition templates sit in the
        // completion-neutral 'waiting for repetition' state, ready to be triggered again.
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().parentCaseInstanceId(caseInstance.getId()).count()).isZero();
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("caseTaskTwo").planItemInstanceStateWaitingForRepetition().count())
                .as("the case task repetition template should be waiting for repetition").isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("repeatingTask").planItemInstanceStateWaitingForRepetition().count())
                .as("the repeating task template should be waiting for repetition").isEqualTo(1);

        // The available (repeating) user event listener keeps the non-auto-complete stage two open, so the case is
        // still running and 'Completed' has not been reached: the repetition can be driven any number of times.
        assertThat(getActivePlanItemInstances(caseInstance, "Completed")).isEmpty();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult())
                .as("the case should still be active, waiting for more triggers").isNotNull();
        
        trigger = cmmnRuntimeService.createUserEventListenerInstanceQuery()
                .caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(trigger).as("the repeating user event listener should be available").isNotNull();
        cmmnRuntimeService.triggerPlanItemInstance(trigger.getId());
        
        repeatingTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId())
                .taskName("Repeating task").singleResult();
        cmmnTaskService.complete(repeatingTask.getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("caseCompleteTwo").finished().count())
                    .as("one child case should have completed per trigger").isEqualTo(2);
        }
        
        assertThat(getActivePlanItemInstances(caseInstance, "Completed")).isEmpty();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult())
                .as("the case should still be active, waiting for more triggers").isNotNull();
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteOneWithCompletionNeutralUserEventListener.cmmn",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteTwo.cmmn"
    })
    @org.flowable.engine.test.Deployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteOne.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteTwo.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteThree.bpmn20.xml"
    })
    public void testCompletionNeutralUserEventListenerDoesNotBlockCompletion() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseCompleteOne")
                .start();

        // The Start milestone occurs immediately and activates stage one, whose human task waits.
        Task humanTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(humanTask).as("the stage one human task should be active").isNotNull();
        assertThat(humanTask.getName()).isEqualTo("Human task");

        // Completing it activates stage two: the process task completes and the completion-neutral 'Trigger' user
        // event listener becomes available. Stage two does NOT complete yet, though: the (non-neutral) repeating
        // task and case task templates are still available work, so they keep the stage open.
        cmmnTaskService.complete(humanTask.getId());
        assertThat(getActivePlanItemInstances(caseInstance, "Completed"))
                .as("'Completed' must not be reached before any repeating task has run").isEmpty();

        // Trigger the completion-neutral user event listener once and complete the resulting repeating task. Its
        // case task repetition runs and completes its child case synchronously.
        UserEventListenerInstance trigger = cmmnRuntimeService.createUserEventListenerInstanceQuery()
                .caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(trigger).as("the completion-neutral user event listener should be available").isNotNull();
        cmmnRuntimeService.triggerPlanItemInstance(trigger.getId());

        Task repeatingTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId())
                .taskName("Repeating task").singleResult();
        assertThat(repeatingTask).as("a repeating task should be active after triggering the event listener").isNotNull();
        cmmnTaskService.complete(repeatingTask.getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("caseCompleteTwo").finished().count())
                    .as("the triggered case task repetition should have run its child case to completion").isEqualTo(1);
        }

        // Unlike the plain repeating-listener variant (where the available listener keeps stage two open forever),
        // the completion-neutral listener does NOT block completion. With the repeating task and case task templates
        // now waiting for repetition and only the neutral listener left available, stage two completes and stage
        // three's 'Completed' human task becomes active - even though the listener is still available.
        assertThat(getActivePlanItemInstances(caseInstance, "Completed"))
                .as("'Completed' should be reached: a completion-neutral listener does not hold the stage open")
                .hasSize(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult())
                .as("the case should still be active at stage three").isNotNull();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(0);
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteOne.cmmn",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseTaskCompleteTwo.cmmn"
    })
    @org.flowable.engine.test.Deployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteOneThreeElements.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteThree.bpmn20.xml"
    })
    public void testMultiElementCollectionReachesCompletedOnlyAfterLastChildCompletes() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseCompleteOne")
                .start();

        // The Start milestone occurs immediately and activates stage one, whose human task waits.
        Task humanTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(humanTask).as("the stage one human task should be active").isNotNull();
        assertThat(humanTask.getName()).isEqualTo("Human task");

        // Completing it activates stage two: the process task produces a three-element 'caseList', so the
        // collection-repeating case task creates three child cases, each waiting on its own human task.
        cmmnTaskService.complete(humanTask.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().parentCaseInstanceId(caseInstance.getId()).count())
                .as("three child cases (one per collection element) should be running").isEqualTo(3);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("caseTaskTwo").planItemInstanceStateActive().count())
                .as("three active case task repetitions").isEqualTo(3);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("caseTaskTwo").planItemInstanceStateAvailable().count())
                .as("one leftover available repetition template").isEqualTo(1);

        List<Task> subCaseTasks = cmmnTaskService.createTaskQuery().taskName("Task complete two").list();
        assertThat(subCaseTasks).as("one human task per child case").hasSize(3);

        // Completing the child human tasks one by one: 'Completed' must not be reached until the LAST child case
        // completes. While any child case is still active, stage two must stay open - this exercises the last-child
        // guard of the leftover-template cleanup with more than one collection element.
        for (int i = 0; i < subCaseTasks.size(); i++) {
            cmmnTaskService.complete(subCaseTasks.get(i).getId());
            if (i < subCaseTasks.size() - 1) {
                assertThat(getActivePlanItemInstances(caseInstance, "Completed"))
                        .as("'Completed' must not be reached while %s child case(s) are still active",
                                subCaseTasks.size() - (i + 1))
                        .isEmpty();
            }
        }

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().parentCaseInstanceId(caseInstance.getId()).count())
                .as("all child cases should have completed").isZero();
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("caseCompleteTwo").finished().count())
                    .as("all three child cases (repetitions) should have run to completion").isEqualTo(3);
        }

        // Only now, after the last child case completed, should stage two complete and 'Completed' become active.
        assertThat(getActivePlanItemInstances(caseInstance, "Completed"))
                .as("'Completed' should be active once the last child case completed").hasSize(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult())
                .as("the case should still be active at stage three").isNotNull();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(0);
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteOne.cmmn",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteTwo.cmmn"
    })
    @org.flowable.engine.test.Deployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteOneEmpty.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteTwo.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteThree.bpmn20.xml"
    })
    public void testEmptyCollectionRepeatingCaseTaskDoesNotBlockCompletion() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseCompleteOne")
                .start();

        // The Start milestone occurs immediately and activates stage one, whose human task waits.
        Task humanTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(humanTask).as("the stage one human task should be active").isNotNull();
        assertThat(humanTask.getName()).isEqualTo("Human task");

        // Completing it activates stage two: the process task produces an EMPTY 'caseList', so the
        // collection-repeating case task has zero elements to process and creates no child case.
        cmmnTaskService.complete(humanTask.getId());

        // No child case should ever run for an empty collection.
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().parentCaseInstanceId(caseInstance.getId()).count())
                .as("an empty collection must not create any child case").isZero();

        // Nothing is left to do in stage two, so it must complete and stage three's 'Completed' task must activate.
        // (A collection repetition template that never processed an element must not keep the stage open.)
        assertThat(getActivePlanItemInstances(caseInstance, "Completed"))
                .as("'Completed' should be reached even though the collection was empty").hasSize(1);
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult())
                .as("the case should still be active at stage three").isNotNull();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(0);
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteOneWithRepeatingUserEventListener.cmmn",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteTwo.cmmn"
    })
    @org.flowable.engine.test.Deployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteOne.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteTwo.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteThree.bpmn20.xml"
    })
    public void testLeftoverCaseTaskTemplateIsWaitingForRepetitionInRuntimeAndHistory() {
        // Uses the repeating user event listener model so stage two stays open after a repetition completes,
        // letting us observe the leftover template's state at rest (in the synchronous variants stage two completes
        // in the same command and the template is terminated immediately).
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseCompleteOne")
                .start();

        Task humanTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(humanTask.getName()).isEqualTo("Human task");
        cmmnTaskService.complete(humanTask.getId());

        UserEventListenerInstance trigger = cmmnRuntimeService.createUserEventListenerInstanceQuery()
                .caseInstanceId(caseInstance.getId()).singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(trigger.getId());

        Task repeatingTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId())
                .taskName("Repeating task").singleResult();
        cmmnTaskService.complete(repeatingTask.getId());

        // At runtime the leftover collection repetition template has been moved to WAITING_FOR_REPETITION (the
        // completion-neutral, still-re-fireable state) by the last-child cleanup.
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("caseTaskTwo").planItemInstanceState(WAITING_FOR_REPETITION).count())
                .as("the runtime case task template should be waiting for repetition").isEqualTo(1);

        // That state change must also be synced to history (guards recordPlanItemInstanceWaitingForRepetition):
        // recordPlanItemInstanceUpdated deliberately does not sync state, so without the dedicated record method the
        // historic entity would be stuck on AVAILABLE and runtime/history would diverge.
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .planItemInstanceCaseInstanceId(caseInstance.getId())
                    .planItemInstanceDefinitionId("caseTaskTwo")
                    .planItemInstanceState(WAITING_FOR_REPETITION).count())
                    .as("history should record the case task template as waiting for repetition").isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .planItemInstanceCaseInstanceId(caseInstance.getId())
                    .planItemInstanceDefinitionId("repeatingTask")
                    .planItemInstanceState(WAITING_FOR_REPETITION).count())
                    .as("history should record the repeating task template as waiting for repetition").isEqualTo(1);
        }
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteRoot.cmmn",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteTwo.cmmn"
    })
    @org.flowable.engine.test.Deployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteOne.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteTwo.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteThree.bpmn20.xml"
    })
    public void testRootLevelSynchronousRepeatingCaseTaskCompletesCase() {
        // Root-level counterpart: the collection-repeating case task sits directly in the non-auto-complete case plan
        // model (no enclosing stage), so the leftover template is cleaned up through the case-instance container fallback.
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseCompleteRoot")
                .start();

        Task humanTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(humanTask).as("the initial human task should be active").isNotNull();
        assertThat(humanTask.getName()).isEqualTo("Human task");

        // Completing it runs the synchronous cascade: process task -> collection-repeating case task -> child case, all
        // completing within this command.
        cmmnTaskService.complete(humanTask.getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("caseCompleteTwo").finished().count())
                    .as("the child case (repetition) should have run to completion").isEqualTo(1);
        }

        // With its work done and the leftover root-level template no longer blocking, the non-auto-complete case plan
        // model must have completed and the case instance must be gone.
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count())
                .as("the non-auto-complete case plan model should have completed").isZero();
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).finished().count())
                    .as("the root case should be recorded as finished").isEqualTo(1);
        }
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteRoot.cmmn",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseTaskCompleteTwo.cmmn"
    })
    @org.flowable.engine.test.Deployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteOneThreeElements.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteThree.bpmn20.xml"
    })
    public void testRootLevelMultiElementWaitStateCompletesCaseOnlyAfterLastChild() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseCompleteRoot")
                .start();

        Task humanTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(humanTask.getName()).isEqualTo("Human task");

        // Completing it produces a three-element 'caseList', so the root-level collection-repeating case task creates
        // three child cases, each waiting on its own human task.
        cmmnTaskService.complete(humanTask.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().parentCaseInstanceId(caseInstance.getId()).count())
                .as("three child cases should be running").isEqualTo(3);

        List<Task> subCaseTasks = cmmnTaskService.createTaskQuery().taskName("Task complete two").list();
        assertThat(subCaseTasks).as("one human task per child case").hasSize(3);

        // The case must stay active until the LAST child case completes: the leftover root-level template must not, by
        // itself, complete the case while children are still running.
        for (int i = 0; i < subCaseTasks.size(); i++) {
            cmmnTaskService.complete(subCaseTasks.get(i).getId());
            if (i < subCaseTasks.size() - 1) {
                assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count())
                        .as("the case must stay active while %s child case(s) are still running", subCaseTasks.size() - (i + 1))
                        .isEqualTo(1);
            }
        }

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("caseCompleteTwo").finished().count())
                    .as("all three child cases should have completed").isEqualTo(3);
        }

        // Only after the last child case completed should the case plan model complete.
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count())
                .as("the case should complete once the last child case completed").isZero();
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteRoot.cmmn",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteTwo.cmmn"
    })
    @org.flowable.engine.test.Deployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteOneEmpty.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteTwo.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteThree.bpmn20.xml"
    })
    public void testRootLevelEmptyCollectionCompletesCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseCompleteRoot")
                .start();

        Task humanTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(humanTask.getName()).isEqualTo("Human task");

        // Completing it produces an EMPTY 'caseList', so the root-level collection-repeating case task creates no child
        // case; the leftover template must not keep the non-auto-complete case plan model active.
        cmmnTaskService.complete(humanTask.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().parentCaseInstanceId(caseInstance.getId()).count())
                .as("an empty collection must not create any child case").isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count())
                .as("the case should complete even though the collection was empty").isZero();
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteRoot.cmmn",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseTaskCompleteTwo.cmmn"
    })
    @org.flowable.engine.test.Deployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteOne.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteThree.bpmn20.xml"
    })
    public void testRootLevelRepeatingCaseTaskWithTaskInSubCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseCompleteRoot")
                .start();

        Task humanTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(humanTask.getName()).isEqualTo("Human task");

        // Completing it activates the root-level process task and then the collection-repeating case task, whose single
        // child case has its own human task and so does NOT complete synchronously.
        cmmnTaskService.complete(humanTask.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().parentCaseInstanceId(caseInstance.getId()).count())
                .as("the single child case should be running").isEqualTo(1);
        Task subCaseTask = cmmnTaskService.createTaskQuery().taskName("Task complete two").singleResult();
        assertThat(subCaseTask).as("the child case human task should be active").isNotNull();

        // The case must stay active while the child case runs; completing the sub-case task then lets the case complete.
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count())
                .as("the case must stay active while the child case runs").isEqualTo(1);
        cmmnTaskService.complete(subCaseTask.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().parentCaseInstanceId(caseInstance.getId()).count()).isZero();
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("caseCompleteTwo").finished().count()).isEqualTo(1);
        }
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count())
                .as("the case should complete once the child case completed").isZero();
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteRootWithRepeatingHumanTask.cmmn",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteTwo.cmmn"
    })
    @org.flowable.engine.test.Deployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteOne.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteTwo.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteThree.bpmn20.xml"
    })
    public void testRootLevelWithRepeatingHumanTaskAfterSynchronous() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseCompleteRoot")
                .start();

        Task humanTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(humanTask.getName()).isEqualTo("Human task");
        cmmnTaskService.complete(humanTask.getId());

        // The process activates the repeating human task; completing it triggers the collection-repeating case task
        // whose child case completes synchronously.
        Task repeatingTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(repeatingTask.getName()).isEqualTo("Repeating task");
        cmmnTaskService.complete(repeatingTask.getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("caseCompleteTwo").finished().count()).isEqualTo(1);
        }
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count())
                .as("the case should complete once the repetition completed").isZero();
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteRootWithRepeatingHumanTask.cmmn",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseTaskCompleteTwo.cmmn"
    })
    @org.flowable.engine.test.Deployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteOne.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteThree.bpmn20.xml"
    })
    public void testRootLevelWithRepeatingHumanTaskAndTaskInSubCase() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseCompleteRoot")
                .start();

        Task humanTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(humanTask.getName()).isEqualTo("Human task");
        cmmnTaskService.complete(humanTask.getId());

        Task repeatingTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(repeatingTask.getName()).isEqualTo("Repeating task");
        cmmnTaskService.complete(repeatingTask.getId());

        // The child case has its own human task, so it stays active until completed.
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().parentCaseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        Task subCaseTask = cmmnTaskService.createTaskQuery().taskName("Task complete two").singleResult();
        assertThat(subCaseTask).isNotNull();
        cmmnTaskService.complete(subCaseTask.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().parentCaseInstanceId(caseInstance.getId()).count()).isZero();
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("caseCompleteTwo").finished().count()).isEqualTo(1);
        }
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count())
                .as("the case should complete once the child case completed").isZero();
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteRootWithRepeatingUserEventListener.cmmn",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteTwo.cmmn"
    })
    @org.flowable.engine.test.Deployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteOne.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteTwo.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteThree.bpmn20.xml"
    })
    public void testRootLevelUserEventListenerRepeatedlyTriggersRepeatingCaseTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseCompleteRoot")
                .start();

        Task humanTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(humanTask.getName()).isEqualTo("Human task");
        // Completing it runs the process (producing 'caseList'); no repeating task runs until the listener is triggered.
        cmmnTaskService.complete(humanTask.getId());
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list()).isEmpty();

        for (int i = 1; i <= 2; i++) {
            UserEventListenerInstance trigger = cmmnRuntimeService.createUserEventListenerInstanceQuery()
                    .caseInstanceId(caseInstance.getId()).singleResult();
            assertThat(trigger).as("the repeating user event listener should be available for round %s", i).isNotNull();
            cmmnRuntimeService.triggerPlanItemInstance(trigger.getId());

            Task repeatingTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId())
                    .taskName("Repeating task").singleResult();
            assertThat(repeatingTask).as("a repeating task should be active in round %s", i).isNotNull();
            cmmnTaskService.complete(repeatingTask.getId());

            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("caseCompleteTwo").finished().count())
                        .as("one child case should have completed per trigger").isEqualTo(i);
            }

            // Between triggers both repetition templates sit in waiting for repetition, and the available (non-neutral)
            // listener keeps the non-auto-complete case plan model open.
            assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                    .planItemDefinitionId("caseTaskTwo").planItemInstanceState(WAITING_FOR_REPETITION).count()).isEqualTo(1);
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count())
                    .as("the case should stay active, waiting for more triggers").isEqualTo(1);
        }
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteRootWithCompletionNeutralUserEventListener.cmmn",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteTwo.cmmn"
    })
    @org.flowable.engine.test.Deployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteOne.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteTwo.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteThree.bpmn20.xml"
    })
    public void testRootLevelCompletionNeutralUserEventListenerDoesNotBlockCompletion() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseCompleteRoot")
                .start();

        Task humanTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(humanTask.getName()).isEqualTo("Human task");
        cmmnTaskService.complete(humanTask.getId());

        // The (non-neutral) repeating task and case task templates still block, so the case stays open until triggered.
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count())
                .as("the case must not complete before any repeating task has run").isEqualTo(1);

        UserEventListenerInstance trigger = cmmnRuntimeService.createUserEventListenerInstanceQuery()
                .caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(trigger).isNotNull();
        cmmnRuntimeService.triggerPlanItemInstance(trigger.getId());

        Task repeatingTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId())
                .taskName("Repeating task").singleResult();
        cmmnTaskService.complete(repeatingTask.getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseDefinitionKey("caseCompleteTwo").finished().count()).isEqualTo(1);
        }

        // With only the completion-neutral listener still available, the non-auto-complete case plan model completes.
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count())
                .as("a completion-neutral listener must not keep the case open").isZero();
    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteRootWithRepeatingUserEventListener.cmmn",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.caseCompleteTwo.cmmn"
    })
    @org.flowable.engine.test.Deployment(resources = {
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteOne.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteTwo.bpmn20.xml",
            "org/flowable/cmmn/test/RepeatingCaseTaskProcessSyncChildTest.processCompleteThree.bpmn20.xml"
    })
    public void testRootLevelLeftoverCaseTaskTemplateIsWaitingForRepetitionInRuntimeAndHistory() {
        // The available repeating listener keeps the root case open, so the leftover template can be observed at rest.
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("caseCompleteRoot")
                .start();

        Task humanTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(humanTask.getName()).isEqualTo("Human task");
        cmmnTaskService.complete(humanTask.getId());

        UserEventListenerInstance trigger = cmmnRuntimeService.createUserEventListenerInstanceQuery()
                .caseInstanceId(caseInstance.getId()).singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(trigger.getId());

        Task repeatingTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId())
                .taskName("Repeating task").singleResult();
        cmmnTaskService.complete(repeatingTask.getId());

        // The leftover collection repetition template - directly in the case plan model - is at rest in
        // WAITING_FOR_REPETITION at runtime, cleaned up via the case-instance container fallback.
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId())
                .planItemDefinitionId("caseTaskTwo").planItemInstanceState(WAITING_FOR_REPETITION).count())
                .as("the runtime root-level case task template should be waiting for repetition").isEqualTo(1);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .planItemInstanceCaseInstanceId(caseInstance.getId())
                    .planItemInstanceDefinitionId("caseTaskTwo")
                    .planItemInstanceState(WAITING_FOR_REPETITION).count())
                    .as("history should record the root-level case task template as waiting for repetition").isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .planItemInstanceCaseInstanceId(caseInstance.getId())
                    .planItemInstanceDefinitionId("repeatingTask")
                    .planItemInstanceState(WAITING_FOR_REPETITION).count())
                    .as("history should record the root-level repeating task template as waiting for repetition").isEqualTo(1);
        }
    }

    protected List<PlanItemInstance> getActivePlanItemInstances(CaseInstance caseInstance, String name) {
        return cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceName(name)
                .planItemInstanceState(ACTIVE)
                .list();
    }
}
