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

package org.flowable.cmmn.test.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.migration.PlanItemDefinitionMappingBuilder;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;

public class CaseInstanceEventSubscriptionMigrationTest extends AbstractCaseMigrationTest {

    @Test
    void withBasicEventRegistryEvent() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/event-registry-listener.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/event-registry-listener-extratask.cmmn.xml");

        List<EventSubscription> eventSubscriptions = cmmnRuntimeService.createEventSubscriptionQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(eventSubscriptions).hasSize(1);
        
        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("humanTask2"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(caseInstanceAfterMigration.getCaseDefinitionKey()).isEqualTo("testCase");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(2);
        assertThat(caseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(destinationDefinition.getDeploymentId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(3);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Event", "My new taskname 1", "Task 2");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        
        eventSubscriptions = cmmnRuntimeService.createEventSubscriptionQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(eventSubscriptions).hasSize(1);
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        for (Task task : tasks) {
            assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            cmmnTaskService.complete(task.getId());
        }
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(1);
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                .isEqualTo(destinationDefinition.getId());

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItemInstances).hasSize(3);
            for (HistoricPlanItemInstance historicPlanItemInstance : historicPlanItemInstances) {
                assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            }

            List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(historicTasks).hasSize(2);
            for (HistoricTaskInstance historicTask : historicTasks) {
                assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            }
        }
    }
    
    @Test
    void withEventRegistryEventWithRepetition() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/event-registry-listener-repetition.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER).singleResult();
        assertThat(planItemInstance).isNotNull();
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/event-registry-listener-repetition-extratask.cmmn.xml");

        List<EventSubscription> eventSubscriptions = cmmnRuntimeService.createEventSubscriptionQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(eventSubscriptions).hasSize(1);
        
        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("humanTask2"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(caseInstanceAfterMigration.getCaseDefinitionKey()).isEqualTo("testCase");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(2);
        assertThat(caseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(destinationDefinition.getDeploymentId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(3);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Event", "My new taskname 1", "Task 2");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        
        eventSubscriptions = cmmnRuntimeService.createEventSubscriptionQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(eventSubscriptions).hasSize(1);
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        for (Task task : tasks) {
            assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            cmmnTaskService.complete(task.getId());
        }
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(1);
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                .isEqualTo(destinationDefinition.getId());

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItemInstances).hasSize(5);
            for (HistoricPlanItemInstance historicPlanItemInstance : historicPlanItemInstances) {
                assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            }

            List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(historicTasks).hasSize(2);
            for (HistoricTaskInstance historicTask : historicTasks) {
                assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            }
        }
    }
    
    @Test
    void withEventRegistryEventWithRepetitionInStage() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/event-registry-listener-stage-repetition.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER).singleResult();
        assertThat(planItemInstance).isNotNull();
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/event-registry-listener-stage-repetition-extratask.cmmn.xml");

        List<EventSubscription> eventSubscriptions = cmmnRuntimeService.createEventSubscriptionQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(eventSubscriptions).hasSize(1);
        
        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("humanTask3"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(caseInstanceAfterMigration.getCaseDefinitionKey()).isEqualTo("testCase");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(2);
        assertThat(caseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(destinationDefinition.getDeploymentId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(6);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Stage", "Event", "Task 1", "Task 2", "Task 1", "Task 3");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.WAITING_FOR_REPETITION, PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        
        eventSubscriptions = cmmnRuntimeService.createEventSubscriptionQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(eventSubscriptions).hasSize(1);
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(3);
        for (Task task : tasks) {
            assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            cmmnTaskService.complete(task.getId());
        }
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER).list();
        assertThat(planItemInstances).hasSize(1);
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(0).getId());
        
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                .isEqualTo(destinationDefinition.getId());

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItemInstances).hasSize(9);
            for (HistoricPlanItemInstance historicPlanItemInstance : historicPlanItemInstances) {
                assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            }

            List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(historicTasks).hasSize(4);
            for (HistoricTaskInstance historicTask : historicTasks) {
                assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            }
        }
    }
}
