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
import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.migration.PlanItemDefinitionMappingBuilder;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;
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

    // with sentries
    // with stages
    // with new expected case variables

}
