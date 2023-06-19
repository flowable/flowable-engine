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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.migration.PlanItemDefinitionMappingBuilder;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntity;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.DefaultTenantProvider;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Valentin Zickner
 */
public class CaseInstanceMigrationTest extends AbstractCaseMigrationTest {

    @Test
    void withSimpleOneTaskCase() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");

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
        assertThat(caseInstanceAfterMigration.getCaseDefinitionName()).isEqualTo("Two Task Test Case");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(2);
        assertThat(caseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(destinationDefinition.getDeploymentId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(2);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1", "Task 2");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        for (Task task : tasks) {
            assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            cmmnTaskService.complete(task.getId());
        }
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                .isEqualTo(destinationDefinition.getId());

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItemInstances).hasSize(2);
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
    void withNoChanges() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .migrateCaseInstances("testCase", 1, "");

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(caseInstanceAfterMigration.getCaseDefinitionKey()).isEqualTo("testCase");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionName()).isEqualTo("One Task Test Case");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(2);
        assertThat(caseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(destinationDefinition.getDeploymentId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactly("Task 1");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(1);
        Task task = tasks.get(0);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        cmmnTaskService.complete(task.getId());
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                .isEqualTo(destinationDefinition.getId());

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItemInstances).hasSize(1);
            for (HistoricPlanItemInstance historicPlanItemInstance : historicPlanItemInstances) {
                assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            }

            List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(historicTasks).hasSize(1);
            for (HistoricTaskInstance historicTask : historicTasks) {
                assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            }
        }
    }

    @Test
    void withSimpleOneTaskCaseWithMappingToSecondNewTask() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("humanTask2"))
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("humanTask1"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances).hasSize(2);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1", "Task 2");
        assertThat(planItemInstances)
                .filteredOn("name", "Task 2")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        assertThat(planItemInstances)
                .filteredOn("name", "Task 1")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.TERMINATED);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
    
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                .isEqualTo(destinationDefinition.getId());

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItemInstances).hasSize(2);
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
    void withSimpleOneTaskCaseIntroducingNewTaskWithSentry() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/second-task-with-sentry.cmmn.xml");

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
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(2);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1", "Task 2");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        for (Task task : tasks) {
            assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            cmmnTaskService.complete(task.getId());
        }
    
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                .isEqualTo(destinationDefinition.getId());

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItemInstances).hasSize(2);
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
    void withSimpleOneTaskCaseIntroducingNewTaskWithSentryLinkedToFirstTask() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/second-task-linked-with-sentry.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("humanTask2"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(2);
        Map<String, List<PlanItemInstance>> planItemsByElementId = planItemInstances.stream()
                .collect(Collectors.groupingBy(PlanItemInstance::getElementId));
        PlanItemInstance planItem1 = planItemsByElementId.get("planItem1").get(0);
        assertThat(planItem1.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(planItem1.getName()).isEqualTo("Task 1");
        assertThat(planItem1.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        PlanItemInstance planItem2 = planItemsByElementId.get("planItem2").get(0);
        assertThat(planItem2.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(planItem2.getName()).isEqualTo("Task 2");
        assertThat(planItem2.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("humanTask1");
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        cmmnTaskService.complete(task.getId());
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("humanTask2");
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        cmmnTaskService.complete(task.getId());
    
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                .isEqualTo(destinationDefinition.getId());

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItemInstances).hasSize(2);
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
    void withTwoTasksIntroducingANewStageAroundSecondTask() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/second-task-linked-with-sentry.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-linked-with-sentry.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("humanTask2"))
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("expandedStage1"))
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("humanTask2"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(3);
        Map<String, List<PlanItemInstance>> planItemsByElementId = planItemInstances.stream()
                .collect(Collectors.groupingBy(PlanItemInstance::getElementId));
        PlanItemInstance planItem1 = planItemsByElementId.get("planItem1").get(0);
        assertThat(planItem1.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(planItem1.getName()).isEqualTo("Task 1");
        assertThat(planItem1.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        PlanItemInstance planItem2 = planItemsByElementId.get("planItem2").get(0);
        assertThat(planItem2.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(planItem2.getName()).isEqualTo("Task 2");
        assertThat(planItem2.getStageInstanceId()).isNotNull();
        assertThat(planItem2.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        PlanItemInstance planItem3 = planItemsByElementId.get("planItem3").get(0);
        assertThat(planItem3.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(planItem3.getPlanItemDefinitionId()).isEqualTo("expandedStage1");
        assertThat(planItem3.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("humanTask1");
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        cmmnTaskService.complete(task.getId());
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("humanTask2");
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        cmmnTaskService.complete(task.getId());
    
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                .isEqualTo(destinationDefinition.getId());

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItemInstances).hasSize(4);
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
    void withTwoTasksIntroducingANewStageAroundSecondTaskAndSecondTaskActive() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/second-task-linked-with-sentry.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-linked-with-sentry.cmmn.xml");
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        cmmnTaskService.complete(task.getId());

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("humanTask2"))
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("expandedStage1"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(2);
        Map<String, List<PlanItemInstance>> planItemsByElementId = planItemInstances.stream()
                .collect(Collectors.groupingBy(PlanItemInstance::getElementId));
        PlanItemInstance planItem2 = planItemsByElementId.get("planItem2").get(0);
        assertThat(planItem2.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(planItem2.getName()).isEqualTo("Task 2");
        assertThat(planItem2.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        PlanItemInstance planItem3 = planItemsByElementId.get("planItem3").get(0);
        assertThat(planItem3.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(planItem3.getPlanItemDefinitionId()).isEqualTo("expandedStage1");
        assertThat(planItem3.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
    }

    @Test
    void withSimpleOneTaskCaseIntroducingNewTaskWithConditionalSentryNotActivated() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/second-task-with-conditional-sentry.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .withCaseInstanceVariable("activate", false)
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("humanTask2"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(2);
        Map<String, List<PlanItemInstance>> planItemsByElementId = planItemInstances.stream()
                .collect(Collectors.groupingBy(PlanItemInstance::getElementId));
        PlanItemInstance planItem1 = planItemsByElementId.get("planItem1").get(0);
        assertThat(planItem1.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(planItem1.getName()).isEqualTo("Task 1");
        assertThat(planItem1.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        PlanItemInstance planItem2 = planItemsByElementId.get("planItem2").get(0);
        assertThat(planItem2.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(planItem2.getName()).isEqualTo("Task 2");
        assertThat(planItem2.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
    }

    @Test
    void withSimpleOneTaskCaseIntroducingNewTaskWithConditionalSentryActivated() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/second-task-with-conditional-sentry.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .withCaseInstanceVariable("activate", true)
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("humanTask2"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(2);
        Map<String, List<PlanItemInstance>> planItemsByElementId = planItemInstances.stream()
                .collect(Collectors.groupingBy(PlanItemInstance::getElementId));
        PlanItemInstance planItem1 = planItemsByElementId.get("planItem1").get(0);
        assertThat(planItem1.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(planItem1.getName()).isEqualTo("Task 1");
        assertThat(planItem1.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        PlanItemInstance planItem2 = planItemsByElementId.get("planItem2").get(0);
        assertThat(planItem2.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(planItem2.getName()).isEqualTo("Task 2");
        assertThat(planItem2.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
    }

    @Test
    void withChangingTheAssignee() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                    .isNotEqualTo(destinationDefinition.getId());
        }

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("humanTask2", "kermit", null))
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("humanTask1"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(1);
        PlanItemInstance task2PlanItemInstance = planItemInstances.get(0);
        assertThat(task2PlanItemInstance).isNotNull();
        assertThat(task2PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(task2PlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("humanTask2");
        assertThat(task.getAssignee()).isEqualTo("kermit");
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        cmmnTaskService.complete(task.getId());
    
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                .isEqualTo(destinationDefinition.getId());

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItemInstances).hasSize(2);
            for (HistoricPlanItemInstance historicPlanItemInstance : historicPlanItemInstances) {
                assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            }

            List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(historicTasks).hasSize(2);
            for (HistoricTaskInstance historicTask : historicTasks) {
                assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            }

            HistoricTaskInstance historicTask = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).taskAssignee("kermit")
                .singleResult();
            assertThat(historicTask.getTaskDefinitionKey()).isEqualTo("humanTask2");
            assertThat(historicTask.getAssignee()).isEqualTo("kermit");
        }
    }

    @Test
    void withTwoCaseTasksToThreeTaskTasks() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/three-task.cmmn.xml");

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
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .list();
        assertThat(planItemInstances).hasSize(3);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1", "Task 2", "Task 3");
    }
    
    @Test
    void withThreeCaseTasksToTwoTaskTasks() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/three-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("humanTask3"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .list();
        assertThat(planItemInstances).hasSize(2);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1", "Task 2");
    }

    @Test
    void withMappingOldTaskToCompleteNewTask() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/three-task.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("humanTask3"))
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("humanTask2"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().list();
        assertThat(planItemInstances).hasSize(2);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1", "Task 3");
    }
    
    @Test
    void stageWithListener() {
        // Arrange
        CaseDefinition originalDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-and-listener.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().list();
        assertThat(planItemInstances).hasSize(5);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(originalDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage1", "expandedStage2", "humanTask1", "humanTask4", "userEventListener1");
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-and-listener2.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("userEventListener2"))
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("expandedStage3"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().list();
        assertThat(planItemInstances).hasSize(7);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage1", "expandedStage2", "expandedStage3", "humanTask1",
                        "humanTask4", "userEventListener1", "userEventListener2");
    }
    
    @Test
    void terminateListener() {
        // Arrange
        CaseDefinition originalDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listener.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(2);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(originalDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("humanTask1", "userEventListener1");
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listener.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("userEventListener1"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("humanTask1");
    }
    
    @Test
    void listenerAndTasksWithoutMapping() {
        // Arrange
        CaseDefinition originalDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listener.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(2);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(originalDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("humanTask1", "userEventListener1");
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listener-and-2tasks.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(2);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("humanTask1", "userEventListener1");
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("humanTask1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener1").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
    }
    
    @Test
    void listenerAndTasksWithMapping() {
        // Arrange
        CaseDefinition originalDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listener.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(2);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(originalDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("humanTask1", "userEventListener1");
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listener-and-2tasks.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("humanTask2"))
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("userEventListener2"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(4);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("humanTask1", "humanTask2", "userEventListener1", "userEventListener2");
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("humanTask1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("humanTask2").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener1").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener2").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
    }
    
    @Test
    void listenerAndTaskInStages() {
        // Arrange
        CaseDefinition originalDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listener-in-stage.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(3);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(originalDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage1", "humanTask1", "userEventListener1");
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listeners-in-stages.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("expandedStage2"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(6);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage1", "expandedStage2", "humanTask1", "humanTask2", "userEventListener1", "userEventListener2");
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("humanTask1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener1").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage2").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("humanTask2").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener2").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
    }
    
    @Test
    void listenerAndTaskInStagesWithSentry() {
        // Arrange
        CaseDefinition originalDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listener-in-stage.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(3);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(originalDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage1", "humanTask1", "userEventListener1");
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listeners-in-stages-withsentry.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("expandedStage2"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(4);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage1", "expandedStage2", "humanTask1", "userEventListener1");
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("humanTask1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener1").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage2").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("humanTask1");
        cmmnTaskService.complete(task.getId());
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(3);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage2", "humanTask2", "userEventListener2");
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("humanTask2").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener2").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage2").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
    }
    
    @Test
    void listenerAndTaskInStagesWithSentryAndIfPart() {
        // Arrange
        CaseDefinition originalDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listeners-in-3stages-withsentry-and-ifpart.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(5);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(originalDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage1", "expandedStage2", "expandedStage3", "humanTask1", "userEventListener1");
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listeners-in-stages-withsentry-and-ifpart.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("expandedStage3"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(4);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage1", "expandedStage2", "humanTask1", "userEventListener1");
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("humanTask1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener1").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage2").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("humanTask1");
        cmmnTaskService.complete(task.getId());
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(3);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage2", "humanTask2", "userEventListener2");
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("humanTask2").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener2").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage2").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
    }
    
    @Test
    void activateNewStageWithSentry() {
        // Arrange
        CaseDefinition originalDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listener-in-stage.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(3);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(originalDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage1", "humanTask1", "userEventListener1");
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listeners-in-stages-withsentry.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("humanTask1"))
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("userEventListener1"))
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("expandedStage1"))
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("expandedStage2"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(3);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage2", "humanTask2", "userEventListener2");
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("humanTask2").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener2").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage2").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("humanTask2");
        cmmnTaskService.complete(task.getId());
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(0);
    }
    
    @Test
    void terminateExistingActiveStateActivateNewOtherState() {
        // Arrange
        CaseDefinition originalDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listener-in-stage.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(3);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(originalDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage1", "humanTask1", "userEventListener1");
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/another-listener-in-stage.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("humanTask1"))
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("userEventListener1"))
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("expandedStage1"))
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("expandedStage2"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(3);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage2", "humanTask2", "userEventListener2");
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("humanTask2").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener2").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage2").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("humanTask1").count()).isZero();
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("humanTask2").count()).isEqualTo(1);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("humanTask2");
        cmmnTaskService.complete(task.getId());
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(0);
        
        if (!cmmnEngineConfiguration.isAsyncHistoryEnabled()) {
            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItemInstances).hasSize(6);
            assertThat(historicPlanItemInstances)
                    .extracting(HistoricPlanItemInstance::getCaseDefinitionId)
                    .containsOnly(destinationDefinition.getId());
            assertThat(historicPlanItemInstances)
                    .extracting(HistoricPlanItemInstance::getPlanItemDefinitionId)
                    .containsExactlyInAnyOrder("expandedStage1", "humanTask1", "userEventListener1", "expandedStage2", "humanTask2", "userEventListener2");
            
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("humanTask1").singleResult().getState()).isEqualTo(PlanItemInstanceState.TERMINATED);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("userEventListener1").singleResult().getState()).isEqualTo(PlanItemInstanceState.TERMINATED);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("expandedStage1").singleResult().getState()).isEqualTo(PlanItemInstanceState.TERMINATED);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("humanTask2").singleResult().getState()).isEqualTo(PlanItemInstanceState.COMPLETED);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("userEventListener2").singleResult().getState()).isEqualTo(PlanItemInstanceState.TERMINATED);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("expandedStage2").singleResult().getState()).isEqualTo(PlanItemInstanceState.COMPLETED);
        }
    }
    
    @Test
    void listenerAndTaskInStagesWithCompletedRootTask() {
        // Arrange
        CaseDefinition originalDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listeners-in-stages-and-root-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(7);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(originalDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage1", "expandedStage2", "humanTask1", "humanTask2", "userEventListener1", "userEventListener2", "rootTask");
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("rootTask").singleResult();
        cmmnTaskService.complete(task.getId());
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listeners-in-stages.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(6);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage1", "expandedStage2", "humanTask1", "humanTask2", "userEventListener1", "userEventListener2");
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("humanTask1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener1").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage2").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("humanTask2").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener2").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("humanTask1").count()).isEqualTo(1);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("humanTask2").count()).isEqualTo(1);
    }
    
    @Test
    void repetitionInMigratedStage() {
        // Arrange
        CaseDefinition originalDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listener-in-stage-userlistener.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(2);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(originalDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("userEventListener1", "expandedStage1");
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listener-in-stage-repetition.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(2);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("userEventListener1", "expandedStage1");
        
        cmmnRuntimeService.completeUserEventListenerInstance(
                cmmnRuntimeService.createUserEventListenerInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .singleResult().getId());
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(5);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("userEventListener1", "expandedStage1", "expandedStage1", "humanTask1", "userEventListener2");
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener1").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("humanTask1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener2").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage1").planItemInstanceState(PlanItemInstanceState.WAITING_FOR_REPETITION).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage1").planItemInstanceState(PlanItemInstanceState.ACTIVE).count()).isEqualTo(1);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        
        cmmnRuntimeService.completeUserEventListenerInstance(
                cmmnRuntimeService.createUserEventListenerInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .singleResult().getId());
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(5);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("userEventListener1", "expandedStage1", "expandedStage1", "humanTask1", "userEventListener2");
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener1").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("humanTask1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener2").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage1").planItemInstanceState(PlanItemInstanceState.WAITING_FOR_REPETITION).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage1").planItemInstanceState(PlanItemInstanceState.ACTIVE).count()).isEqualTo(1);
    }
    
    @Test
    void repetitionInActiveStage() {
        // Arrange
        CaseDefinition originalDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listener-in-stage-userlistener.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        
        cmmnRuntimeService.completeUserEventListenerInstance(
                cmmnRuntimeService.createUserEventListenerInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .singleResult().getId());
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(3);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(originalDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage1", "userEventListener2", "humanTask1");
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listener-in-stage-repetition.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("userEventListener1"))
                .addWaitingForRepetitionPlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createWaitingForRepetitionPlanItemDefinitionMappingFor("expandedStage1"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(5);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("userEventListener1", "expandedStage1", "expandedStage1", "userEventListener2", "humanTask1");
        
        if (!cmmnEngineConfiguration.isAsyncHistoryEnabled()) {
            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItemInstances).hasSize(6);
            assertThat(historicPlanItemInstances)
                    .extracting(HistoricPlanItemInstance::getCaseDefinitionId)
                    .containsOnly(destinationDefinition.getId());
            assertThat(historicPlanItemInstances)
                    .extracting(HistoricPlanItemInstance::getPlanItemDefinitionId)
                    .containsExactlyInAnyOrder("userEventListener1", "userEventListener1", "expandedStage1", "expandedStage1", "userEventListener2", "humanTask1");
            
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("userEventListener1").planItemInstanceState(PlanItemInstanceState.COMPLETED).singleResult().getState()).isEqualTo(PlanItemInstanceState.COMPLETED);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("userEventListener1").planItemInstanceState(PlanItemInstanceState.AVAILABLE).singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("humanTask1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("expandedStage1").planItemInstanceState(PlanItemInstanceState.ACTIVE).singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("expandedStage1").planItemInstanceState(PlanItemInstanceState.WAITING_FOR_REPETITION).singleResult().getState()).isEqualTo(PlanItemInstanceState.WAITING_FOR_REPETITION);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("userEventListener2").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        }
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        
        cmmnRuntimeService.completeUserEventListenerInstance(
                cmmnRuntimeService.createUserEventListenerInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .singleResult().getId());
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(5);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("userEventListener1", "expandedStage1", "expandedStage1", "humanTask1", "userEventListener2");
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener1").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("humanTask1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener2").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage1").planItemInstanceState(PlanItemInstanceState.WAITING_FOR_REPETITION).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage1").planItemInstanceState(PlanItemInstanceState.ACTIVE).count()).isEqualTo(1);
    }
    
    @Test
    void repetitionInCompletedStage() {
        // Arrange
        CaseDefinition originalDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/task-and-listener-in-stage-userlistener.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        
        cmmnRuntimeService.completeUserEventListenerInstance(
                cmmnRuntimeService.createUserEventListenerInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .singleResult().getId());
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("humanTask1").singleResult();
        cmmnTaskService.complete(task.getId());
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(originalDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("rootTask");
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/task-and-listener-in-stage-repetition.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("userEventListener1"))
                .addWaitingForRepetitionPlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createWaitingForRepetitionPlanItemDefinitionMappingFor("expandedStage1"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(3);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("userEventListener1", "expandedStage1", "rootTask");
        
        cmmnRuntimeService.completeUserEventListenerInstance(
                cmmnRuntimeService.createUserEventListenerInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .singleResult().getId());
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(6);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("userEventListener1", "expandedStage1", "expandedStage1", "humanTask1", "userEventListener2", "rootTask");
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener1").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("humanTask1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener2").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage1").planItemInstanceState(PlanItemInstanceState.WAITING_FOR_REPETITION).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage1").planItemInstanceState(PlanItemInstanceState.ACTIVE).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("rootTask").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("humanTask1").singleResult();
        cmmnTaskService.complete(task.getId());
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(3);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("userEventListener1", "expandedStage1", "rootTask");
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener1").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("rootTask").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage1").planItemInstanceState(PlanItemInstanceState.WAITING_FOR_REPETITION).count()).isEqualTo(1);
    }
    
    @Test
    void removeActiveStageWithRepetition() {
        // Arrange
        CaseDefinition originalDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/task-and-listener-in-stage-repetition.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        
        cmmnRuntimeService.completeUserEventListenerInstance(
                cmmnRuntimeService.createUserEventListenerInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .singleResult().getId());
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(6);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(originalDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("userEventListener1", "expandedStage1", "expandedStage1", "humanTask1", "userEventListener2", "rootTask");
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/task-and-listener.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("expandedStage1"))
                .removeWaitingForRepetitionPlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createRemoveWaitingForRepetitionPlanItemDefinitionMappingFor("expandedStage1"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(2);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("userEventListener1", "rootTask");
        
        if (!cmmnEngineConfiguration.isAsyncHistoryEnabled()) {
            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItemInstances).hasSize(7);
            assertThat(historicPlanItemInstances)
                    .extracting(HistoricPlanItemInstance::getCaseDefinitionId)
                    .containsOnly(destinationDefinition.getId());
            assertThat(historicPlanItemInstances)
                    .extracting(HistoricPlanItemInstance::getPlanItemDefinitionId)
                    .containsExactlyInAnyOrder("userEventListener1", "userEventListener1", "expandedStage1", "expandedStage1", "humanTask1", "userEventListener2", "rootTask");
            
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("userEventListener1").planItemInstanceState(PlanItemInstanceState.COMPLETED).singleResult().getState()).isEqualTo(PlanItemInstanceState.COMPLETED);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("userEventListener1").planItemInstanceState(PlanItemInstanceState.AVAILABLE).singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("humanTask1").singleResult().getState()).isEqualTo(PlanItemInstanceState.TERMINATED);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("expandedStage1").planItemInstanceState(PlanItemInstanceState.TERMINATED).count()).isEqualTo(2);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("userEventListener2").singleResult().getState()).isEqualTo(PlanItemInstanceState.TERMINATED);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("rootTask").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        }
        
        cmmnRuntimeService.completeUserEventListenerInstance(
                cmmnRuntimeService.createUserEventListenerInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .singleResult().getId());
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(2);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("userEventListener1", "rootTask");
    }
    
    @Test
    void removeEventListenerWithRepetitionFromActiveStage() {
        // Arrange
        CaseDefinition originalDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listener-in-stage-with-repetition-eventlistener.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(4);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(originalDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage1", "userEventListener1", "humanTask1", "eventListener1");
        
        EventSubscription eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(eventSubscription.getEventType()).isEqualTo("TEST_E001");
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listener-in-stage.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("eventListener1"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(3);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage1", "userEventListener1", "humanTask1");
        
        assertThat(cmmnRuntimeService.createEventSubscriptionQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(0);
    }
    
    @Test
    void addNewTaskInAdditionToNewStage() {
        // Arrange
        CaseDefinition originalDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listener-in-stage.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(3);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(originalDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage1", "humanTask1", "userEventListener1");
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listener-in-stage-with-new-tasks.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("newTask2"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(5);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage1", "humanTask1", "userEventListener1", "newExpandedStage", "newSubTask");
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("humanTask1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener1").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("newExpandedStage").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("newSubTask").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        
        if (!cmmnEngineConfiguration.isAsyncHistoryEnabled()) {
            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItemInstances).hasSize(6);
            assertThat(historicPlanItemInstances)
                    .extracting(HistoricPlanItemInstance::getCaseDefinitionId)
                    .containsOnly(destinationDefinition.getId());
            assertThat(historicPlanItemInstances)
                    .extracting(HistoricPlanItemInstance::getPlanItemDefinitionId)
                    .containsExactlyInAnyOrder("expandedStage1", "humanTask1", "userEventListener1", "newExpandedStage", "newSubTask", "newTask2");
            
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("expandedStage1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("userEventListener1").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("humanTask1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("newExpandedStage").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("newSubTask").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("newTask2").singleResult().getState()).isEqualTo(PlanItemInstanceState.COMPLETED);
        }
    }
    
    @Test
    void addNewTasksInAdditionToNewStage() {
        // Arrange
        CaseDefinition originalDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listener-in-stage.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(3);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(originalDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage1", "humanTask1", "userEventListener1");
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listener-in-stage-with-new-tasks.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("newTask"))
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("newTask2"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(5);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("expandedStage1", "humanTask1", "userEventListener1", "newExpandedStage", "newSubTask");
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("humanTask1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener1").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("newExpandedStage").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("newSubTask").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        
        if (!cmmnEngineConfiguration.isAsyncHistoryEnabled()) {
            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItemInstances).hasSize(7);
            assertThat(historicPlanItemInstances)
                    .extracting(HistoricPlanItemInstance::getCaseDefinitionId)
                    .containsOnly(destinationDefinition.getId());
            assertThat(historicPlanItemInstances)
                    .extracting(HistoricPlanItemInstance::getPlanItemDefinitionId)
                    .containsExactlyInAnyOrder("expandedStage1", "humanTask1", "userEventListener1", "newExpandedStage", "newSubTask", "newTask", "newTask2");
            
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("expandedStage1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("userEventListener1").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("humanTask1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("newExpandedStage").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("newSubTask").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("newTask").singleResult().getState()).isEqualTo(PlanItemInstanceState.COMPLETED);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("newTask2").singleResult().getState()).isEqualTo(PlanItemInstanceState.COMPLETED);
        }
    }
    
    @Test
    void addSentryForNewTaskInActiveStageWithRepetition() {
        // Arrange
        CaseDefinition originalDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listener-in-stage-repetition.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        
        cmmnRuntimeService.completeUserEventListenerInstance(
                cmmnRuntimeService.createUserEventListenerInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .singleResult().getId());
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(5);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(originalDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("userEventListener1", "expandedStage1", "expandedStage1", "humanTask1", "userEventListener2");
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/listener-and-2tasks-in-stage-repetition.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("humanTask2"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(6);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("userEventListener1", "expandedStage1", "expandedStage1", "humanTask1", "userEventListener2", "humanTask2");
        
        cmmnRuntimeService.completeUserEventListenerInstance(
                cmmnRuntimeService.createUserEventListenerInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemDefinitionId("userEventListener2")
                    .singleResult().getId());
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(5);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("userEventListener1", "expandedStage1", "expandedStage1", "humanTask1", "humanTask2");
        
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener1").singleResult().getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("humanTask1").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("humanTask2").singleResult().getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage1").planItemInstanceState(PlanItemInstanceState.WAITING_FOR_REPETITION).count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("expandedStage1").planItemInstanceState(PlanItemInstanceState.ACTIVE).count()).isEqualTo(1);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("humanTask2").singleResult();
        assertThat(task).isNotNull();
    }
    
    @Test
    void testMultiTenantCaseInstanceMigrationWithDefaultTenantDefinition() {
        DefaultTenantProvider originalDefaultTenantValue = cmmnEngineConfiguration.getDefaultTenantProvider();
        cmmnEngineConfiguration.setDefaultTenantValue("default");
        cmmnEngineConfiguration.setFallbackToDefaultTenant(true);
        
        try {
            // Arrange
            CmmnDeployment deployment = cmmnRepositoryService.createDeployment()
                    .name("my deploy")
                    .addClasspathResource("org/flowable/cmmn/test/migration/one-task.cmmn.xml")
                    .tenantId("default")
                    .deploy();
            
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("testCase")
                    .tenantId("tenant1")
                    .start();
            
            deployment = cmmnRepositoryService.createDeployment()
                    .name("my deploy")
                    .addClasspathResource("org/flowable/cmmn/test/migration/two-task.cmmn.xml")
                    .tenantId("default")
                    .deploy();
            
            CaseDefinition destinationDefinition = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deployment.getId()).singleResult();
    
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
            assertThat(caseInstanceAfterMigration.getCaseDefinitionName()).isEqualTo("Two Task Test Case");
            assertThat(caseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(2);
            assertThat(caseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(destinationDefinition.getDeploymentId());
            List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .list();
            assertThat(planItemInstances).hasSize(2);
            assertThat(planItemInstances)
                    .extracting(PlanItemInstance::getCaseDefinitionId)
                    .containsOnly(destinationDefinition.getId());
            assertThat(planItemInstances)
                    .extracting(PlanItemInstance::getName)
                    .containsExactlyInAnyOrder("Task 1", "Task 2");
            assertThat(planItemInstances)
                    .extracting(PlanItemInstance::getState)
                    .containsOnly(PlanItemInstanceState.ACTIVE);
            
            List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(tasks).hasSize(2);
            for (Task task : tasks) {
                assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
                cmmnTaskService.complete(task.getId());
            }
            
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    
            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                    .isEqualTo(destinationDefinition.getId());
    
                List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .planItemInstanceCaseInstanceId(caseInstance.getId()).list();
                assertThat(historicPlanItemInstances).hasSize(2);
                for (HistoricPlanItemInstance historicPlanItemInstance : historicPlanItemInstances) {
                    assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
                }
    
                List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).list();
                assertThat(historicTasks).hasSize(2);
                for (HistoricTaskInstance historicTask : historicTasks) {
                    assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
                }
            }
            
        } finally {
            cmmnEngineConfiguration.setFallbackToDefaultTenant(false);
            cmmnEngineConfiguration.setDefaultTenantProvider(originalDefaultTenantValue);
        }
    }
    
    @Test
    void testMultiTenantCaseInstanceMigrationWithTargetDefaultTenantDefinition() {
        DefaultTenantProvider originalDefaultTenantValue = cmmnEngineConfiguration.getDefaultTenantProvider();
        cmmnEngineConfiguration.setDefaultTenantValue("default");
        cmmnEngineConfiguration.setFallbackToDefaultTenant(true);
        
        try {
            // Arrange
            CmmnDeployment deployment = cmmnRepositoryService.createDeployment()
                    .name("my deploy")
                    .addClasspathResource("org/flowable/cmmn/test/migration/one-task.cmmn.xml")
                    .tenantId("tenant1")
                    .deploy();
            
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("testCase")
                    .tenantId("tenant1")
                    .start();
            
            deployment = cmmnRepositoryService.createDeployment()
                    .name("my deploy")
                    .addClasspathResource("org/flowable/cmmn/test/migration/two-task.cmmn.xml")
                    .tenantId("default")
                    .deploy();
            
            CaseDefinition destinationDefinition = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deployment.getId()).singleResult();
    
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
            assertThat(caseInstanceAfterMigration.getCaseDefinitionName()).isEqualTo("Two Task Test Case");
            assertThat(caseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(1);
            assertThat(caseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(destinationDefinition.getDeploymentId());
            List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .list();
            assertThat(planItemInstances).hasSize(2);
            assertThat(planItemInstances)
                    .extracting(PlanItemInstance::getCaseDefinitionId)
                    .containsOnly(destinationDefinition.getId());
            assertThat(planItemInstances)
                    .extracting(PlanItemInstance::getName)
                    .containsExactlyInAnyOrder("Task 1", "Task 2");
            assertThat(planItemInstances)
                    .extracting(PlanItemInstance::getState)
                    .containsOnly(PlanItemInstanceState.ACTIVE);
            
            List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(tasks).hasSize(2);
            for (Task task : tasks) {
                assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
                cmmnTaskService.complete(task.getId());
            }
            
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    
            if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
                assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                    .isEqualTo(destinationDefinition.getId());
    
                List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .planItemInstanceCaseInstanceId(caseInstance.getId()).list();
                assertThat(historicPlanItemInstances).hasSize(2);
                for (HistoricPlanItemInstance historicPlanItemInstance : historicPlanItemInstances) {
                    assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
                }
    
                List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).list();
                assertThat(historicTasks).hasSize(2);
                for (HistoricTaskInstance historicTask : historicTasks) {
                    assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
                }
            }
            
        } finally {
            cmmnEngineConfiguration.setFallbackToDefaultTenant(false);
            cmmnEngineConfiguration.setDefaultTenantProvider(originalDefaultTenantValue);
        }
    }
    
    @Test
    void testMultiTenantCaseInstanceMigrationWithDefaultTenantDefinitionFailsWithNoFallback() {
        DefaultTenantProvider originalDefaultTenantValue = cmmnEngineConfiguration.getDefaultTenantProvider();
        cmmnEngineConfiguration.setDefaultTenantValue("default");
        
        try {
            // Arrange
            CmmnDeployment deployment = cmmnRepositoryService.createDeployment()
                    .name("my deploy")
                    .addClasspathResource("org/flowable/cmmn/test/migration/one-task.cmmn.xml")
                    .tenantId("tenant1")
                    .deploy();
            
            CaseDefinition sourceDefinition = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deployment.getId()).singleResult();
            
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("testCase")
                    .tenantId("tenant1")
                    .start();
            
            deployment = cmmnRepositoryService.createDeployment()
                    .name("my deploy")
                    .addClasspathResource("org/flowable/cmmn/test/migration/two-task.cmmn.xml")
                    .tenantId("default")
                    .deploy();
            
            CaseDefinition destinationDefinition = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deployment.getId()).singleResult();
    
            assertThatThrownBy(() -> {
                cmmnMigrationService.createCaseInstanceMigrationBuilder()
                    .migrateToCaseDefinition(destinationDefinition.getId())
                    .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("humanTask2"))
                    .migrate(caseInstance.getId());
            }).isInstanceOf(FlowableException.class).hasMessage("Tenant mismatch between Case Instance ('tenant1') and Case Definition ('default') to migrate to");
            
            CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .singleResult();
            assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(sourceDefinition.getId());
            assertThat(caseInstanceAfterMigration.getCaseDefinitionKey()).isEqualTo("testCase");
            assertThat(caseInstanceAfterMigration.getCaseDefinitionName()).isEqualTo("One Task Test Case");
            assertThat(caseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(1);
            assertThat(caseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(sourceDefinition.getDeploymentId());
            
        } finally {
            cmmnEngineConfiguration.setDefaultTenantProvider(originalDefaultTenantValue);
        }
    }

    @Test
    void withChangedTaskName() {
        // Arrange
        CaseDefinition originalDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-expression-name.cmmn.xml");

        Map<String, Object> variables = new HashMap<>();
        variables.put("myVar1", "foo");
        variables.put("myVar2", "bar");

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionId(originalDefinition.getId())
                .variables(variables)
                .start();

        // Assert
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Human Task: foo");
        assertThat(task.getDescription()).isEqualTo("Description: foo");

        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-expression-name-v2.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("humanTask1"))
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("humanTask1"))
                .migrate(caseInstance.getId());

        // Assert
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Human Task: bar");
        assertThat(task.getDescription()).isEqualTo("Description: bar");
    }
    
    @Test
    void migrateCaseInstancesWithSimpleOneTaskCaseWithMappingToSecondNewTask() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("humanTask2"))
                .migrateCaseInstances("testCase", 1, "");

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances).hasSize(2);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1", "Task 2");
        assertThat(planItemInstances)
                .filteredOn("name", "Task 2")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        assertThat(planItemInstances)
                .filteredOn("name", "Task 1")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        for (Task task : tasks) {
            cmmnTaskService.complete(task.getId());
        }
    
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                .isEqualTo(destinationDefinition.getId());

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItemInstances).hasSize(2);
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
    void migrateCaseInstancesWithSimpleOneTaskMultipleCaseWithMappingsToSecondNewTask() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("humanTask1"))
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("humanTask2"))
                .migrateCaseInstances("testCase", 1, "");

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances).hasSize(2);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1", "Task 2");
        assertThat(planItemInstances)
                .filteredOn("name", "Task 2")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        assertThat(planItemInstances)
                .filteredOn("name", "Task 1")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.TERMINATED);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
    
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                .isEqualTo(destinationDefinition.getId());

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItemInstances).hasSize(2);
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
    void withTwoSentriesOnPartEventDeferred() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/twosentry-onpart-eventdeferred.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/twosentry-onpart-eventdeferred-extratask.cmmn.xml");

        Task firstTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("humanTask1").singleResult();
        cmmnTaskService.complete(firstTask.getId());
        
        List<SentryPartInstanceEntity> sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<List<SentryPartInstanceEntity>>() {

            @Override
            public List<SentryPartInstanceEntity> execute(CommandContext commandContext) {
                return cmmnEngineConfiguration.getSentryPartInstanceEntityManager().findSentryPartInstancesByCaseInstanceId(caseInstance.getId());
            }
            
        });
        
        assertThat(sentryPartInstances).hasSize(1);
        assertThat(sentryPartInstances.get(0).getOnPartId()).isEqualTo("sentryOnPartcmmnEntrySentry_2");
        
        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("humanTask4"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(caseInstanceAfterMigration.getCaseDefinitionKey()).isEqualTo("testCase");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionName()).isEqualTo("Sentry On Part Test Case");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(2);
        assertThat(caseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(destinationDefinition.getDeploymentId());
        
        sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<List<SentryPartInstanceEntity>>() {

            @Override
            public List<SentryPartInstanceEntity> execute(CommandContext commandContext) {
                return cmmnEngineConfiguration.getSentryPartInstanceEntityManager().findSentryPartInstancesByCaseInstanceId(caseInstance.getId());
            }
            
        });
        
        assertThat(sentryPartInstances).hasSize(1);
        assertThat(sentryPartInstances.get(0).getOnPartId()).isEqualTo("sentryOnPartcmmnEntrySentry_2");
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(3);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 2", "Task 3", "Task 4");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        for (Task task : tasks) {
            assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            cmmnTaskService.complete(task.getId());
        }
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        
        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 3");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                .isEqualTo(destinationDefinition.getId());

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItemInstances).hasSize(4);
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
    
    @Test
    void withTwoSentriesOnPartChangeIdEventDeferred() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/twosentry-onpart-id-eventdeferred.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/twosentry-onpart-eventdeferred-extratask.cmmn.xml");

        Task firstTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("humanTask1").singleResult();
        cmmnTaskService.complete(firstTask.getId());
        
        List<SentryPartInstanceEntity> sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<List<SentryPartInstanceEntity>>() {

            @Override
            public List<SentryPartInstanceEntity> execute(CommandContext commandContext) {
                return cmmnEngineConfiguration.getSentryPartInstanceEntityManager().findSentryPartInstancesByCaseInstanceId(caseInstance.getId());
            }
            
        });
        
        assertThat(sentryPartInstances).hasSize(1);
        assertThat(sentryPartInstances.get(0).getOnPartId()).isEqualTo("sentryOnPart1cmmnEntrySentry_2");
        
        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("humanTask4"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(caseInstanceAfterMigration.getCaseDefinitionKey()).isEqualTo("testCase");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionName()).isEqualTo("Sentry On Part Test Case");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(2);
        assertThat(caseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(destinationDefinition.getDeploymentId());
        
        sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<List<SentryPartInstanceEntity>>() {

            @Override
            public List<SentryPartInstanceEntity> execute(CommandContext commandContext) {
                return cmmnEngineConfiguration.getSentryPartInstanceEntityManager().findSentryPartInstancesByCaseInstanceId(caseInstance.getId());
            }
            
        });
        
        assertThat(sentryPartInstances).hasSize(1);
        assertThat(sentryPartInstances.get(0).getOnPartId()).isEqualTo("sentryOnPartcmmnEntrySentry_2");
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(3);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 2", "Task 3", "Task 4");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        for (Task task : tasks) {
            assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            cmmnTaskService.complete(task.getId());
        }
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        
        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 3");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                .isEqualTo(destinationDefinition.getId());

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItemInstances).hasSize(4);
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
    
    @Test
    void withTwoSentriesOnPartChangePlanItemEventDeferred() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/twosentry-onpart-planitem-eventdeferred.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/twosentry-onpart-eventdeferred-extratask.cmmn.xml");

        Task zeroTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("humanTask0").singleResult();
        cmmnTaskService.complete(zeroTask.getId());
        
        List<SentryPartInstanceEntity> sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<List<SentryPartInstanceEntity>>() {

            @Override
            public List<SentryPartInstanceEntity> execute(CommandContext commandContext) {
                return cmmnEngineConfiguration.getSentryPartInstanceEntityManager().findSentryPartInstancesByCaseInstanceId(caseInstance.getId());
            }
            
        });
        
        assertThat(sentryPartInstances).hasSize(1);
        assertThat(sentryPartInstances.get(0).getOnPartId()).isEqualTo("sentryOnPart1cmmnEntrySentry_2");
        
        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("humanTask1"))
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("humanTask4"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(caseInstanceAfterMigration.getCaseDefinitionKey()).isEqualTo("testCase");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionName()).isEqualTo("Sentry On Part Test Case");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(2);
        assertThat(caseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(destinationDefinition.getDeploymentId());
        
        sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<List<SentryPartInstanceEntity>>() {

            @Override
            public List<SentryPartInstanceEntity> execute(CommandContext commandContext) {
                return cmmnEngineConfiguration.getSentryPartInstanceEntityManager().findSentryPartInstancesByCaseInstanceId(caseInstance.getId());
            }
            
        });
        
        assertThat(sentryPartInstances).hasSize(0);
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(4);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1", "Task 2", "Task 3", "Task 4");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(3);
        
        Task secondTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("humanTask2").singleResult();
        assertThat(secondTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        cmmnTaskService.complete(secondTask.getId());
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        
        Task firstTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("humanTask1").singleResult();
        assertThat(firstTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        cmmnTaskService.complete(firstTask.getId());
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        
        for (Task task : tasks) {
            assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            cmmnTaskService.complete(task.getId());
        }
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        
        assertThat(planItemInstances).hasSize(0);
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

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
            assertThat(historicTasks).hasSize(5);
            for (HistoricTaskInstance historicTask : historicTasks) {
                assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            }
        }
    }
    
    @Test
    void withTwoSentriesOnPartEventDeferredToOnEvent() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/twosentry-ifpart-eventdeferred.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCase")
                .variable("var1", "test2")
                .start();
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task 1");
        
        cmmnTaskService.complete(task.getId());
        
        List<SentryPartInstanceEntity> sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<List<SentryPartInstanceEntity>>() {

            @Override
            public List<SentryPartInstanceEntity> execute(CommandContext commandContext) {
                return cmmnEngineConfiguration.getSentryPartInstanceEntityManager().findSentryPartInstancesByCaseInstanceId(caseInstance.getId());
            }
            
        });
        
        assertThat(sentryPartInstances).hasSize(1);
        assertThat(sentryPartInstances.get(0).getOnPartId()).isEqualTo("sentryOnPartcmmnEntrySentry_2");
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/twosentry-ifpart-onevent.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(caseInstanceAfterMigration.getCaseDefinitionKey()).isEqualTo("testCase");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionName()).isEqualTo("Sentry If Part Test Case");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(2);
        assertThat(caseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(destinationDefinition.getDeploymentId());
        
        sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<List<SentryPartInstanceEntity>>() {

            @Override
            public List<SentryPartInstanceEntity> execute(CommandContext commandContext) {
                return cmmnEngineConfiguration.getSentryPartInstanceEntityManager().findSentryPartInstancesByCaseInstanceId(caseInstance.getId());
            }
            
        });
        
        assertThat(sentryPartInstances).hasSize(0);
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 2");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.AVAILABLE);
        
        cmmnRuntimeService.createChangePlanItemStateBuilder().caseInstanceId(caseInstance.getId())
                .activatePlanItemDefinitionId("humanTask1")
                .changeState();
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task 1");
        
        cmmnTaskService.complete(task.getId());
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        
        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances)
            .extracting(PlanItemInstance::getName)
            .containsExactlyInAnyOrder("Task 2");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.AVAILABLE);
        
        cmmnRuntimeService.createChangePlanItemStateBuilder().caseInstanceId(caseInstance.getId())
            .activatePlanItemDefinitionId("humanTask1")
            .changeState();
        
        cmmnRuntimeService.setVariable(caseInstance.getId(), "var1", "test");
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        
        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances)
            .extracting(PlanItemInstance::getName)
            .containsExactlyInAnyOrder("Task 2");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                .isEqualTo(destinationDefinition.getId());

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItemInstances).hasSize(4);
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
    
    @Test
    void withTwoSentriesIfPartEventDeferred() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/twosentry-ifpart-eventdeferred.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCase")
                .variable("var1", "test")
                .start();
        
        cmmnRuntimeService.setVariable(caseInstance.getId(), "var1", "test2");
        
        List<SentryPartInstanceEntity> sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<List<SentryPartInstanceEntity>>() {

            @Override
            public List<SentryPartInstanceEntity> execute(CommandContext commandContext) {
                return cmmnEngineConfiguration.getSentryPartInstanceEntityManager().findSentryPartInstancesByCaseInstanceId(caseInstance.getId());
            }
            
        });
        
        assertThat(sentryPartInstances).hasSize(1);
        assertThat(sentryPartInstances.get(0).getIfPartId()).isEqualTo("ifpart1");
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/twosentry-ifpart-eventdeferred-extratask.cmmn.xml");

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
        assertThat(caseInstanceAfterMigration.getCaseDefinitionName()).isEqualTo("Sentry If Part Test Case");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(2);
        assertThat(caseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(destinationDefinition.getDeploymentId());
        
        sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<List<SentryPartInstanceEntity>>() {

            @Override
            public List<SentryPartInstanceEntity> execute(CommandContext commandContext) {
                return cmmnEngineConfiguration.getSentryPartInstanceEntityManager().findSentryPartInstancesByCaseInstanceId(caseInstance.getId());
            }
            
        });
        
        assertThat(sentryPartInstances).hasSize(1);
        assertThat(sentryPartInstances.get(0).getIfPartId()).isEqualTo("ifpart2");
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(3);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1", "Task 2", "Task 3");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        for (Task task : tasks) {
            assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            cmmnTaskService.complete(task.getId());
        }
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        
        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 2");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        
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
            assertThat(historicTasks).hasSize(3);
            for (HistoricTaskInstance historicTask : historicTasks) {
                assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            }
        }
    }
    
    @Test
    void withTwoSentriesIfPartChangedConditionEventDeferred() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/twosentry-ifpart-condition-eventdeferred.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCase")
                .variable("var1", "test2")
                .start();
        
        cmmnRuntimeService.setVariable(caseInstance.getId(), "var1", "test3");
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/twosentry-ifpart-eventdeferred-extratask.cmmn.xml");

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
        assertThat(caseInstanceAfterMigration.getCaseDefinitionName()).isEqualTo("Sentry If Part Test Case");
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
                .containsExactlyInAnyOrder("Task 1", "Task 2", "Task 3");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        for (Task task : tasks) {
            assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            cmmnTaskService.complete(task.getId());
        }
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        
        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 2");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.AVAILABLE);
        
        cmmnRuntimeService.setVariable(caseInstance.getId(), "var1", "test");
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        
        assertThat(planItemInstances)
            .extracting(PlanItemInstance::getState)
            .containsOnly(PlanItemInstanceState.ACTIVE);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        
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
            assertThat(historicTasks).hasSize(3);
            for (HistoricTaskInstance historicTask : historicTasks) {
                assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            }
        }
    }
    
    @Test
    void withTwoSentriesIfPartEventDeferredToOnEvent() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/twosentry-ifpart-eventdeferred.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCase")
                .variable("var1", "test")
                .start();
        
        cmmnRuntimeService.setVariable(caseInstance.getId(), "var1", "test2");
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/twosentry-ifpart-onevent.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(caseInstanceAfterMigration.getCaseDefinitionKey()).isEqualTo("testCase");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionName()).isEqualTo("Sentry If Part Test Case");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(2);
        assertThat(caseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(destinationDefinition.getDeploymentId());
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(2);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1", "Task 2");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        cmmnTaskService.complete(tasks.get(0).getId());
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        
        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 2");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.AVAILABLE);
        
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        
        cmmnRuntimeService.setVariable(caseInstance.getId(), "var1", "test");
        
        cmmnRuntimeService.createChangePlanItemStateBuilder().caseInstanceId(caseInstance.getId())
                .activatePlanItemDefinitionId("humanTask1")
                .changeState();
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Task 1");
        
        cmmnTaskService.complete(task.getId());
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        
        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances)
            .extracting(PlanItemInstance::getName)
            .containsExactlyInAnyOrder("Task 2");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        
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
            assertThat(historicTasks).hasSize(3);
            for (HistoricTaskInstance historicTask : historicTasks) {
                assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            }
        }
    }
    
    @Test
    void withExitSentryOnPartEventDeferred() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/exitsentry-onpart-eventdeferred.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/exitsentry-onpart-eventdeferred-extratask.cmmn.xml");

        Task firstTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("humanTask1").singleResult();
        cmmnTaskService.complete(firstTask.getId());
        
        List<SentryPartInstanceEntity> sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<List<SentryPartInstanceEntity>>() {

            @Override
            public List<SentryPartInstanceEntity> execute(CommandContext commandContext) {
                return cmmnEngineConfiguration.getSentryPartInstanceEntityManager().findSentryPartInstancesByCaseInstanceId(caseInstance.getId());
            }
            
        });
        
        assertThat(sentryPartInstances).hasSize(1);
        assertThat(sentryPartInstances.get(0).getOnPartId()).isEqualTo("sentryOnPartcmmnExitSentry_1");
        
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
        assertThat(caseInstanceAfterMigration.getCaseDefinitionName()).isEqualTo("Sentry On Part Test Case");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(2);
        assertThat(caseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(destinationDefinition.getDeploymentId());
        
        sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<List<SentryPartInstanceEntity>>() {

            @Override
            public List<SentryPartInstanceEntity> execute(CommandContext commandContext) {
                return cmmnEngineConfiguration.getSentryPartInstanceEntityManager().findSentryPartInstancesByCaseInstanceId(caseInstance.getId());
            }
            
        });
        
        assertThat(sentryPartInstances).hasSize(1);
        assertThat(sentryPartInstances.get(0).getOnPartId()).isEqualTo("sentryOnPartcmmnExitSentry_1");
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(2);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 2", "Task 3");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("humanTask2").singleResult();
        cmmnTaskService.complete(task.getId());
        
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
            assertThat(historicTasks).hasSize(3);
            for (HistoricTaskInstance historicTask : historicTasks) {
                assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            }
        }
    }
    
    @Test
    void withExitSentryOnPartChangedIdEventDeferred() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/exitsentry-onpart-id-eventdeferred.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/exitsentry-onpart-eventdeferred-extratask.cmmn.xml");

        Task firstTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("humanTask1").singleResult();
        cmmnTaskService.complete(firstTask.getId());
        
        List<SentryPartInstanceEntity> sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<List<SentryPartInstanceEntity>>() {

            @Override
            public List<SentryPartInstanceEntity> execute(CommandContext commandContext) {
                return cmmnEngineConfiguration.getSentryPartInstanceEntityManager().findSentryPartInstancesByCaseInstanceId(caseInstance.getId());
            }
            
        });
        
        assertThat(sentryPartInstances).hasSize(1);
        assertThat(sentryPartInstances.get(0).getOnPartId()).isEqualTo("sentryOnPart1cmmnExitSentry_1");
        
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
        assertThat(caseInstanceAfterMigration.getCaseDefinitionName()).isEqualTo("Sentry On Part Test Case");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(2);
        assertThat(caseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(destinationDefinition.getDeploymentId());
        
        sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<List<SentryPartInstanceEntity>>() {

            @Override
            public List<SentryPartInstanceEntity> execute(CommandContext commandContext) {
                return cmmnEngineConfiguration.getSentryPartInstanceEntityManager().findSentryPartInstancesByCaseInstanceId(caseInstance.getId());
            }
            
        });
        
        assertThat(sentryPartInstances).hasSize(1);
        assertThat(sentryPartInstances.get(0).getOnPartId()).isEqualTo("sentryOnPartcmmnExitSentry_1");
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(2);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 2", "Task 3");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("humanTask2").singleResult();
        cmmnTaskService.complete(task.getId());
        
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
            assertThat(historicTasks).hasSize(3);
            for (HistoricTaskInstance historicTask : historicTasks) {
                assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            }
        }
    }
    
    @Test
    void withExitSentryOnPartEventDeferredOnStage() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/exitsentry-stage-onpart-eventdeferred.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/exitsentry-stage-onpart-eventdeferred-extratask.cmmn.xml");

        Task firstTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("stageHumanTask1").singleResult();
        cmmnTaskService.complete(firstTask.getId());
        
        List<SentryPartInstanceEntity> sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<List<SentryPartInstanceEntity>>() {

            @Override
            public List<SentryPartInstanceEntity> execute(CommandContext commandContext) {
                return cmmnEngineConfiguration.getSentryPartInstanceEntityManager().findSentryPartInstancesByCaseInstanceId(caseInstance.getId());
            }
            
        });
        
        assertThat(sentryPartInstances).hasSize(1);
        assertThat(sentryPartInstances.get(0).getOnPartId()).isEqualTo("sentryOnPartcmmnExitSentry_1");
        
        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("stageHumanTask3"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(caseInstanceAfterMigration.getCaseDefinitionKey()).isEqualTo("testCase");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionName()).isEqualTo("Sentry On Part Test Case");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(2);
        assertThat(caseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(destinationDefinition.getDeploymentId());
        
        sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<List<SentryPartInstanceEntity>>() {

            @Override
            public List<SentryPartInstanceEntity> execute(CommandContext commandContext) {
                return cmmnEngineConfiguration.getSentryPartInstanceEntityManager().findSentryPartInstancesByCaseInstanceId(caseInstance.getId());
            }
            
        });
        
        assertThat(sentryPartInstances).hasSize(1);
        assertThat(sentryPartInstances.get(0).getOnPartId()).isEqualTo("sentryOnPartcmmnExitSentry_1");
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(4);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1", "Stage", "Stage task 2", "Stage task 3");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(3);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("stageHumanTask2").singleResult();
        cmmnTaskService.complete(task.getId());
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        
        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("humanTask1").singleResult();
        cmmnTaskService.complete(task.getId());
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

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
            assertThat(historicTasks).hasSize(4);
            for (HistoricTaskInstance historicTask : historicTasks) {
                assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            }
        }
    }
    
    @Test
    void withSentryIfPartEventDeferred() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/sentry-ifpart-eventdeferred.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCase")
                .variable("var1", "test2")
                .start();
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/sentry-ifpart-eventdeferred-extratask.cmmn.xml");

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
        assertThat(caseInstanceAfterMigration.getCaseDefinitionName()).isEqualTo("Sentry If Part Test Case");
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
                .containsExactlyInAnyOrder("Task 1", "Task 2", "Task 3");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        for (Task task : tasks) {
            assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            cmmnTaskService.complete(task.getId());
        }
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        
        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 2");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.AVAILABLE);
        
        cmmnRuntimeService.setVariable(caseInstance.getId(), "var1", "test");
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        
        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 2");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        
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
            assertThat(historicTasks).hasSize(3);
            for (HistoricTaskInstance historicTask : historicTasks) {
                assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            }
        }
    }

    // with sentries
    // with stages
    // with new expected case variables

}
