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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.flowable.cmmn.converter.CmmnXmlConstants.ELEMENT_STAGE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.assertj.core.groups.Tuple;
import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.migration.ActivatePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationDocument;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationValidationResult;
import org.flowable.cmmn.api.migration.ChangePlanItemDefinitionWithNewTargetIdsMapping;
import org.flowable.cmmn.api.migration.ChangePlanItemIdMapping;
import org.flowable.cmmn.api.migration.ChangePlanItemIdWithDefinitionIdMapping;
import org.flowable.cmmn.api.migration.MoveToAvailablePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.PlanItemDefinitionMappingBuilder;
import org.flowable.cmmn.api.migration.TerminatePlanItemDefinitionMapping;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.MilestoneInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.engine.impl.migration.CaseInstanceMigrationDocumentConverter;
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
    void withSimpleOneTaskCaseChangingOnlyTaskProperties() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-new-properties.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
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
        Map<String, List<PlanItemInstance>> planItemsByElementId = planItemInstances.stream()
                .collect(Collectors.groupingBy(PlanItemInstance::getElementId));
        PlanItemInstance planItem1 = planItemsByElementId.get("planItem1").get(0);
        assertThat(planItem1.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(planItem1.getName()).isEqualTo("Task 1");
        assertThat(planItem1.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("humanTask1");
        assertThat(task.getName()).isEqualTo("Task 2");
        assertThat(task.getFormKey()).isEqualTo("myForm");
        assertThat(task.getCategory()).isEqualTo("myCategory");
        assertThat(task.getDescription()).isEqualTo("Example description");
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
            assertThat(historicPlanItemInstances.get(0).getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());

            List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(historicTasks).hasSize(1);
            for (HistoricTaskInstance historicTask : historicTasks) {
                assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
                assertThat(historicTask.getName()).isEqualTo("Task 2");
                assertThat(historicTask.getFormKey()).isEqualTo("myForm");
                assertThat(historicTask.getCategory()).isEqualTo("myCategory");
                assertThat(historicTask.getDescription()).isEqualTo("Example description");
            }
        }
    }
    
    @Test
    void withAutomatedMigrationFromTwoTasksToOneTask() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
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
                .containsExactlyInAnyOrder("Task 1");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        cmmnTaskService.complete(tasks.get(0).getId());
        
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
    void withActivateTaskFromTwoTasksToOneTask() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("humanTask1").singleResult();
        cmmnTaskService.complete(task.getId());
        
        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("humanTask1"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
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
                .containsExactlyInAnyOrder("Task 1");
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        cmmnTaskService.complete(tasks.get(0).getId());
        
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
        assertThat(planItem2.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        PlanItemInstance planItem3 = planItemsByElementId.get("planItem3").get(0);
        assertThat(planItem3.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(planItem3.getPlanItemDefinitionId()).isEqualTo("expandedStage1");
        assertThat(planItem3.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("humanTask1").singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("humanTask1");
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        cmmnTaskService.complete(task.getId());
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("humanTask2").singleResult();
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
    void withTwoTasksCaseChangingTaskProperties() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task-new-properties.cmmn.xml");

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
        Map<String, List<PlanItemInstance>> planItemsByElementId = planItemInstances.stream()
                .collect(Collectors.groupingBy(PlanItemInstance::getElementId));
        PlanItemInstance planItem1 = planItemsByElementId.get("planItem1").get(0);
        assertThat(planItem1.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(planItem1.getName()).isEqualTo("Task 1");
        assertThat(planItem1.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("humanTask1").singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("humanTask1");
        assertThat(task.getName()).isEqualTo("Task 2");
        assertThat(task.getFormKey()).isEqualTo("myForm");
        assertThat(task.getCategory()).isEqualTo("myCategory");
        assertThat(task.getDescription()).isEqualTo("Example description");
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        cmmnTaskService.complete(task.getId());
        
        PlanItemInstance planItem2 = planItemsByElementId.get("planItem2").get(0);
        assertThat(planItem2.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(planItem2.getName()).isEqualTo("Task 3");
        assertThat(planItem2.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("humanTask2");
        assertThat(task.getName()).isEqualTo("Task 3");
        assertThat(task.getFormKey()).isEqualTo("myForm2");
        assertThat(task.getCategory()).isEqualTo("myCategory2");
        assertThat(task.getDescription()).isEqualTo("Example description2");
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
            assertThat(historicPlanItemInstances.get(0).getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicPlanItemInstances.get(1).getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());

            List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(historicTasks).hasSize(2);
            for (HistoricTaskInstance historicTask : historicTasks) {
                assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
                
                if ("humanTask1".equals(historicTask.getTaskDefinitionKey())) {
                    assertThat(historicTask.getName()).isEqualTo("Task 2");
                    assertThat(historicTask.getFormKey()).isEqualTo("myForm");
                    assertThat(historicTask.getCategory()).isEqualTo("myCategory");
                    assertThat(historicTask.getDescription()).isEqualTo("Example description");
                    
                } else {
                    assertThat(historicTask.getName()).isEqualTo("Task 3");
                    assertThat(historicTask.getFormKey()).isEqualTo("myForm2");
                    assertThat(historicTask.getCategory()).isEqualTo("myCategory2");
                    assertThat(historicTask.getDescription()).isEqualTo("Example description2");
                }
            }
        }
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
    void withConditionalMoveToAvailableOnTwoTaskProcessWithSentryAndTaskIsActive() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/second-task-linked-with-sentry.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        cmmnTaskService.complete(task.getId());

        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/second-task-linked-with-sentry.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("humanTask2", "${false}"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .list();
        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName, PlanItemInstance::getState)
                .containsExactlyInAnyOrder(
                        Tuple.tuple("Task 2", PlanItemInstanceState.ACTIVE)
                );
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
    void withNoNewPlanItemForPlanItemInstance() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
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
                .filteredOn("name", "Task 1")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        assertThat(planItemInstances)
                .filteredOn("name", "Task 2")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.TERMINATED);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("humanTask1").singleResult();
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
            
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("humanTask1").singleResult().getState()).isEqualTo(PlanItemInstanceState.COMPLETED);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).planItemInstanceDefinitionId("humanTask2").singleResult().getState()).isEqualTo(PlanItemInstanceState.TERMINATED);

            List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(historicTasks).hasSize(2);
            for (HistoricTaskInstance historicTask : historicTasks) {
                assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
                assertThat(historicTask.getEndTime()).isNotNull();
            }
        }
    }
    
    @Test
    void withChangingPlanItemId() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-other-planitem-id.cmmn.xml");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                    .isNotEqualTo(destinationDefinition.getId());
        }

        // Act
        assertThatThrownBy(() -> {
            cmmnMigrationService.createCaseInstanceMigrationBuilder()
                    .migrateToCaseDefinition(destinationDefinition.getId())
                    .migrate(caseInstance.getId());
        }).isInstanceOf(FlowableException.class)
                .hasMessageStartingWith("Plan item could not be found for PlanItemInstance with id: ")
                .hasMessageContainingAll("name: Task 1", "definitionId: humanTask1", "state: active", "elementId: planItem1",
                        "caseInstanceId: " + caseInstance.getId(),
                        "caseDefinitionId: " + destinationDefinition.getId());

        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addChangePlanItemIdMapping(new ChangePlanItemIdMapping("planItem1", "planItem2"))
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
        PlanItemInstance task1PlanItemInstance = planItemInstances.get(0);
        assertThat(task1PlanItemInstance).isNotNull();
        assertThat(task1PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(task1PlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task1PlanItemInstance.getElementId()).isEqualTo("planItem2");
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("humanTask1");
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
            HistoricPlanItemInstance historicPlanItemInstance = historicPlanItemInstances.get(0);
            assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicPlanItemInstance.getElementId()).isEqualTo("planItem2");

            List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(historicTasks).hasSize(1);
            HistoricTaskInstance historicTask = historicTasks.get(0);
            assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicTask.getTaskDefinitionKey()).isEqualTo("humanTask1");
        }
    }
    
    @Test
    void withChangingPlanItemIdWithDefinitionId() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-other-planitem-id.cmmn.xml");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                    .isNotEqualTo(destinationDefinition.getId());
        }

        // Act
        assertThatThrownBy(() -> {
            cmmnMigrationService.createCaseInstanceMigrationBuilder()
                    .migrateToCaseDefinition(destinationDefinition.getId())
                    .migrate(caseInstance.getId());
        }).isInstanceOf(FlowableException.class)
                .hasMessageStartingWith("Plan item could not be found for PlanItemInstance with id: ")
                .hasMessageContainingAll("name: Task 1", "definitionId: humanTask1", "state: active", "elementId: planItem1",
                        "caseInstanceId: " + caseInstance.getId(),
                        "caseDefinitionId: " + destinationDefinition.getId());
        
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addChangePlanItemIdWithDefinitionIdMapping(new ChangePlanItemIdWithDefinitionIdMapping("humanTask1", "humanTask1"))
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
        PlanItemInstance task1PlanItemInstance = planItemInstances.get(0);
        assertThat(task1PlanItemInstance).isNotNull();
        assertThat(task1PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(task1PlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task1PlanItemInstance.getElementId()).isEqualTo("planItem2");
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("humanTask1");
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
            HistoricPlanItemInstance historicPlanItemInstance = historicPlanItemInstances.get(0);
            assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicPlanItemInstance.getElementId()).isEqualTo("planItem2");

            List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(historicTasks).hasSize(1);
            HistoricTaskInstance historicTask = historicTasks.get(0);
            assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicTask.getTaskDefinitionKey()).isEqualTo("humanTask1");
        }
    }
    
    @Test
    void withChangingPlanItemIdWithNewTargetIds() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-other-target-ids.cmmn.xml");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                    .isNotEqualTo(destinationDefinition.getId());
        }

        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addChangePlanItemDefinitionWithNewTargetIdsMapping(new ChangePlanItemDefinitionWithNewTargetIdsMapping("humanTask1", "planItem2", "humanTask2"))
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
        PlanItemInstance task1PlanItemInstance = planItemInstances.get(0);
        assertThat(task1PlanItemInstance).isNotNull();
        assertThat(task1PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(task1PlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task1PlanItemInstance.getElementId()).isEqualTo("planItem2");
        assertThat(task1PlanItemInstance.getPlanItemDefinitionId()).isEqualTo("humanTask2");
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
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
            assertThat(historicPlanItemInstances).hasSize(1);
            HistoricPlanItemInstance historicPlanItemInstance = historicPlanItemInstances.get(0);
            assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicPlanItemInstance.getElementId()).isEqualTo("planItem2");
            assertThat(historicPlanItemInstance.getPlanItemDefinitionId()).isEqualTo("humanTask2");

            List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(historicTasks).hasSize(1);
            HistoricTaskInstance historicTask = historicTasks.get(0);
            assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicTask.getTaskDefinitionKey()).isEqualTo("humanTask2");
        }
    }
    
    @Test
    void withChangingPlanItemIdAndTerminateDefinition() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task-other-planitem-id.cmmn.xml");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                    .isNotEqualTo(destinationDefinition.getId());
        }

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addTerminatePlanItemDefinitionMapping(new TerminatePlanItemDefinitionMapping("humanTask1"))
                .addActivatePlanItemDefinitionMapping(new ActivatePlanItemDefinitionMapping("humanTask2"))
                .addChangePlanItemIdMapping(new ChangePlanItemIdMapping("planItem1", "planItem3"))
                .addChangePlanItemIdMapping(new ChangePlanItemIdMapping("planItem2", "planItem4"))
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
        PlanItemInstance task1PlanItemInstance = planItemInstances.get(0);
        assertThat(task1PlanItemInstance).isNotNull();
        assertThat(task1PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(task1PlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task1PlanItemInstance.getElementId()).isEqualTo("planItem4");
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("humanTask2");
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        
        cmmnTaskService.complete(task.getId());
    
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                .isEqualTo(destinationDefinition.getId());

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId())
                .orderByCreateTime()
                .asc()
                .list();
            
            assertThat(historicPlanItemInstances).hasSize(2);
            HistoricPlanItemInstance historicPlanItemInstance = historicPlanItemInstances.get(0);
            assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicPlanItemInstance.getElementId()).isEqualTo("planItem3");
            
            historicPlanItemInstance = historicPlanItemInstances.get(1);
            assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicPlanItemInstance.getElementId()).isEqualTo("planItem4");

            List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .caseInstanceId(caseInstance.getId()).orderByTaskCreateTime().asc().list();
            assertThat(historicTasks).hasSize(2);
            HistoricTaskInstance historicTask = historicTasks.get(0);
            assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicTask.getTaskDefinitionKey()).isEqualTo("humanTask1");
            historicTask = historicTasks.get(1);
            assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicTask.getTaskDefinitionKey()).isEqualTo("humanTask2");
        }
    }
    
    @Test
    void withMilestone() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-milestone.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-milestone.cmmn.xml");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                    .isNotEqualTo(destinationDefinition.getId());
        }
        
        MilestoneInstance milestoneInstance = cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).singleResult();
        assertThat(milestoneInstance).isNotNull();
        
        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
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
        PlanItemInstance task1PlanItemInstance = planItemInstances.get(0);
        assertThat(task1PlanItemInstance).isNotNull();
        assertThat(task1PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(task1PlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task1PlanItemInstance.getElementId()).isEqualTo("planItem1");
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("humanTask1");
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        
        List<MilestoneInstance> milestoneInstances = cmmnRuntimeService.createMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId())
                .list();
        assertThat(milestoneInstances).hasSize(1);
        MilestoneInstance milestonePlanItemInstance = milestoneInstances.get(0);
        assertThat(milestonePlanItemInstance).isNotNull();
        assertThat(milestonePlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(milestonePlanItemInstance.getElementId()).isEqualTo("planItem2");
        
        cmmnTaskService.complete(task.getId());
    
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                .isEqualTo(destinationDefinition.getId());

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId())
                .orderByName()
                .desc()
                .list();
            assertThat(historicPlanItemInstances).hasSize(2);
            HistoricPlanItemInstance historicPlanItemInstance = historicPlanItemInstances.get(0);
            assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicPlanItemInstance.getElementId()).isEqualTo("planItem1");
            
            historicPlanItemInstance = historicPlanItemInstances.get(1);
            assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicPlanItemInstance.getElementId()).isEqualTo("planItem2");
            
            List<HistoricMilestoneInstance> historicMilestoneInstances = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                    .milestoneInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicMilestoneInstances).hasSize(1);
            HistoricMilestoneInstance historicMilestoneInstance = historicMilestoneInstances.get(0);
            assertThat(historicMilestoneInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicMilestoneInstance.getElementId()).isEqualTo("planItem2");

            List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(historicTasks).hasSize(1);
            HistoricTaskInstance historicTask = historicTasks.get(0);
            assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicTask.getTaskDefinitionKey()).isEqualTo("humanTask1");
        }
    }
    
    @Test
    void withMilestoneAndChangingPlanItemId() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-milestone.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-milestone-other-planitem-id.cmmn.xml");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                    .isNotEqualTo(destinationDefinition.getId());
        }

        // Act
        assertThatThrownBy(() -> {
            cmmnMigrationService.createCaseInstanceMigrationBuilder()
                    .migrateToCaseDefinition(destinationDefinition.getId())
                    .migrate(caseInstance.getId());
        }).isInstanceOf(FlowableException.class)
                .hasMessageStartingWith("Plan item could not be found for PlanItemInstance with id: ")
                .hasMessageContainingAll("name: Task 1", "definitionId: humanTask1", "state: active", "elementId: planItem1",
                        "caseInstanceId: " + caseInstance.getId(),
                        "caseDefinitionId: " + destinationDefinition.getId());

        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addChangePlanItemIdMapping(new ChangePlanItemIdMapping("planItem1", "planItem3"))
                .addChangePlanItemIdMapping(new ChangePlanItemIdMapping("planItem2", "planItem4"))
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
        PlanItemInstance task1PlanItemInstance = planItemInstances.get(0);
        assertThat(task1PlanItemInstance).isNotNull();
        assertThat(task1PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(task1PlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task1PlanItemInstance.getElementId()).isEqualTo("planItem3");
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("humanTask1");
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        
        List<MilestoneInstance> milestoneInstances = cmmnRuntimeService.createMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId())
                .list();
        assertThat(milestoneInstances).hasSize(1);
        MilestoneInstance milestonePlanItemInstance = milestoneInstances.get(0);
        assertThat(milestonePlanItemInstance).isNotNull();
        assertThat(milestonePlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(milestonePlanItemInstance.getElementId()).isEqualTo("planItem4");
        
        cmmnTaskService.complete(task.getId());
    
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                .isEqualTo(destinationDefinition.getId());

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId())
                .orderByName()
                .desc()
                .list();
            assertThat(historicPlanItemInstances).hasSize(2);
            HistoricPlanItemInstance historicPlanItemInstance = historicPlanItemInstances.get(0);
            assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicPlanItemInstance.getElementId()).isEqualTo("planItem3");
            
            historicPlanItemInstance = historicPlanItemInstances.get(1);
            assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicPlanItemInstance.getElementId()).isEqualTo("planItem4");
            
            List<HistoricMilestoneInstance> historicMilestoneInstances = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                    .milestoneInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicMilestoneInstances).hasSize(1);
            HistoricMilestoneInstance historicMilestoneInstance = historicMilestoneInstances.get(0);
            assertThat(historicMilestoneInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicMilestoneInstance.getElementId()).isEqualTo("planItem4");

            List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(historicTasks).hasSize(1);
            HistoricTaskInstance historicTask = historicTasks.get(0);
            assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicTask.getTaskDefinitionKey()).isEqualTo("humanTask1");
        }
    }
    
    @Test
    void withMilestoneAndChangingPlanItemIdWithDefinitionId() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-milestone.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-milestone-other-planitem-id.cmmn.xml");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                    .isNotEqualTo(destinationDefinition.getId());
        }

        // Act
        assertThatThrownBy(() -> {
            cmmnMigrationService.createCaseInstanceMigrationBuilder()
                    .migrateToCaseDefinition(destinationDefinition.getId())
                    .migrate(caseInstance.getId());
        }).isInstanceOf(FlowableException.class)
                .hasMessageStartingWith("Plan item could not be found for PlanItemInstance with id: ")
                .hasMessageContainingAll("name: Task 1", "definitionId: humanTask1", "state: active", "elementId: planItem1",
                        "caseInstanceId: " + caseInstance.getId(),
                        "caseDefinitionId: " + destinationDefinition.getId());

        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addChangePlanItemIdWithDefinitionIdMapping(new ChangePlanItemIdWithDefinitionIdMapping("humanTask1", "humanTask1"))
                .addChangePlanItemIdWithDefinitionIdMapping(new ChangePlanItemIdWithDefinitionIdMapping("milestone1", "milestone1"))
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
        PlanItemInstance task1PlanItemInstance = planItemInstances.get(0);
        assertThat(task1PlanItemInstance).isNotNull();
        assertThat(task1PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(task1PlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task1PlanItemInstance.getElementId()).isEqualTo("planItem3");
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("humanTask1");
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        
        List<MilestoneInstance> milestoneInstances = cmmnRuntimeService.createMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId())
                .list();
        assertThat(milestoneInstances).hasSize(1);
        MilestoneInstance milestonePlanItemInstance = milestoneInstances.get(0);
        assertThat(milestonePlanItemInstance).isNotNull();
        assertThat(milestonePlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(milestonePlanItemInstance.getElementId()).isEqualTo("planItem4");
        
        cmmnTaskService.complete(task.getId());
    
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                .isEqualTo(destinationDefinition.getId());

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId())
                .orderByName()
                .desc()
                .list();
            assertThat(historicPlanItemInstances).hasSize(2);
            HistoricPlanItemInstance historicPlanItemInstance = historicPlanItemInstances.get(0);
            assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicPlanItemInstance.getElementId()).isEqualTo("planItem3");
            
            historicPlanItemInstance = historicPlanItemInstances.get(1);
            assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicPlanItemInstance.getElementId()).isEqualTo("planItem4");
            
            List<HistoricMilestoneInstance> historicMilestoneInstances = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                    .milestoneInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicMilestoneInstances).hasSize(1);
            HistoricMilestoneInstance historicMilestoneInstance = historicMilestoneInstances.get(0);
            assertThat(historicMilestoneInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicMilestoneInstance.getElementId()).isEqualTo("planItem4");

            List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).list();
            assertThat(historicTasks).hasSize(1);
            HistoricTaskInstance historicTask = historicTasks.get(0);
            assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicTask.getTaskDefinitionKey()).isEqualTo("humanTask1");
        }
    }
    
    @Test
    void withIfSentryEventDeferredAndChangingPlanItemId() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/twosentry-ifpart-eventdeferred.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCase")
                .variable("var1", "test")
                .start();
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/twosentry-ifpart-eventdeferred-other-planitem-id.cmmn.xml");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                    .isNotEqualTo(destinationDefinition.getId());
        }

        // Act
        assertThatThrownBy(() -> {
            cmmnMigrationService.createCaseInstanceMigrationBuilder()
                    .migrateToCaseDefinition(destinationDefinition.getId())
                    .migrate(caseInstance.getId());
        }).isInstanceOf(FlowableException.class)
                .hasMessageStartingWith("Plan item could not be found for PlanItemInstance with id: ")
                .hasMessageContainingAll("name: Task 1", "definitionId: humanTask1", "state: active", "elementId: planItem1",
                        "caseInstanceId: " + caseInstance.getId(),
                        "caseDefinitionId: " + destinationDefinition.getId());

        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addChangePlanItemIdMapping(new ChangePlanItemIdMapping("planItem1", "planItem3"))
                .addChangePlanItemIdMapping(new ChangePlanItemIdMapping("planItem2", "planItem4"))
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
        
        PlanItemInstance task1PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("humanTask1").singleResult();
        assertThat(task1PlanItemInstance).isNotNull();
        assertThat(task1PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(task1PlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task1PlanItemInstance.getElementId()).isEqualTo("planItem3");
        
        PlanItemInstance task2PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("humanTask2").singleResult();
        assertThat(task2PlanItemInstance).isNotNull();
        assertThat(task2PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(task2PlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task2PlanItemInstance.getElementId()).isEqualTo("planItem4");
        
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
            
            HistoricPlanItemInstance historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .planItemInstanceCaseInstanceId(caseInstance.getId())
                    .planItemInstanceDefinitionId("humanTask1")
                    .singleResult();
            assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicPlanItemInstance.getElementId()).isEqualTo("planItem3");
            
            historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .planItemInstanceCaseInstanceId(caseInstance.getId())
                    .planItemInstanceDefinitionId("humanTask2")
                    .singleResult();
            assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicPlanItemInstance.getElementId()).isEqualTo("planItem4");

            List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .orderByTaskCreateTime()
                    .asc()
                    .list();
            assertThat(historicTasks).hasSize(2);
            
            HistoricTaskInstance historicTask = historicTasks.get(0);
            assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicTask.getTaskDefinitionKey()).isEqualTo("humanTask1");
            
            historicTask = historicTasks.get(1);
            assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicTask.getTaskDefinitionKey()).isEqualTo("humanTask2");
        }
    }
    
    @Test
    void withIfSentryEventDeferredAndChangingPlanItemIdWithDefinitionId() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/twosentry-ifpart-eventdeferred.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCase")
                .variable("var1", "test")
                .start();
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/twosentry-ifpart-eventdeferred-other-planitem-id.cmmn.xml");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                    .isNotEqualTo(destinationDefinition.getId());
        }

        // Act
        assertThatThrownBy(() -> {
            cmmnMigrationService.createCaseInstanceMigrationBuilder()
                    .migrateToCaseDefinition(destinationDefinition.getId())
                    .migrate(caseInstance.getId());
        }).isInstanceOf(FlowableException.class)
                .hasMessageStartingWith("Plan item could not be found for PlanItemInstance with id: ")
                .hasMessageContainingAll("name: Task 1", "definitionId: humanTask1", "state: active", "elementId: planItem1",
                        "caseInstanceId: " + caseInstance.getId(),
                        "caseDefinitionId: " + destinationDefinition.getId());

        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addChangePlanItemIdWithDefinitionIdMapping(new ChangePlanItemIdWithDefinitionIdMapping("humanTask1", "humanTask1"))
                .addChangePlanItemIdWithDefinitionIdMapping(new ChangePlanItemIdWithDefinitionIdMapping("humanTask2", "humanTask2"))
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
        
        PlanItemInstance task1PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("humanTask1").singleResult();
        assertThat(task1PlanItemInstance).isNotNull();
        assertThat(task1PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(task1PlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task1PlanItemInstance.getElementId()).isEqualTo("planItem3");
        
        PlanItemInstance task2PlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("humanTask2").singleResult();
        assertThat(task2PlanItemInstance).isNotNull();
        assertThat(task2PlanItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(task2PlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task2PlanItemInstance.getElementId()).isEqualTo("planItem4");
        
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
            
            HistoricPlanItemInstance historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .planItemInstanceCaseInstanceId(caseInstance.getId())
                    .planItemInstanceDefinitionId("humanTask1")
                    .singleResult();
            assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicPlanItemInstance.getElementId()).isEqualTo("planItem3");
            
            historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .planItemInstanceCaseInstanceId(caseInstance.getId())
                    .planItemInstanceDefinitionId("humanTask2")
                    .singleResult();
            assertThat(historicPlanItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicPlanItemInstance.getElementId()).isEqualTo("planItem4");

            List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .orderByTaskCreateTime()
                    .asc()
                    .list();
            assertThat(historicTasks).hasSize(2);
            
            HistoricTaskInstance historicTask = historicTasks.get(0);
            assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicTask.getTaskDefinitionKey()).isEqualTo("humanTask1");
            
            historicTask = historicTasks.get(1);
            assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            assertThat(historicTask.getTaskDefinitionKey()).isEqualTo("humanTask2");
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
    void withActivateStage() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/case-with-stage.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("migrationCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/case-with-stage.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("cmmnStage1"))
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
        assertThat(planItemInstances).hasSize(3);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Stage 1", "Stage 1", "Human task 1");
        assertThat(planItemInstances)
                .filteredOn("name", "Human task 1")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        assertThat(planItemInstances)
                .filteredOn("name", "Stage 1")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.WAITING_FOR_REPETITION, PlanItemInstanceState.ACTIVE);
        
        PlanItemInstance stagePlanItem = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("cmmnStage1").planItemInstanceState(PlanItemInstanceState.ACTIVE).singleResult();
        
        PlanItemInstance taskPlanItem = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stageTask1").singleResult();
        
        assertThat(taskPlanItem.getStageInstanceId()).isEqualTo(stagePlanItem.getId());
        
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
            assertThat(historicTasks).hasSize(1);
            for (HistoricTaskInstance historicTask : historicTasks) {
                assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            }
        }
    }
    
    @Test
    void withActivateAnotherRepetitionStage() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/case-with-active-stage.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("migrationCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/case-with-active-stage.cmmn.xml");

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances).hasSize(3);
        
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Stage 1", "Stage 1", "Human task 1");
        assertThat(planItemInstances)
                .filteredOn("name", "Human task 1")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        assertThat(planItemInstances)
                .filteredOn("name", "Stage 1")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.WAITING_FOR_REPETITION, PlanItemInstanceState.ACTIVE);
        
        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("stageTask1"))
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances).hasSize(4);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Stage 1", "Stage 1", "Human task 1", "Human task 1");
        assertThat(planItemInstances)
                .filteredOn("name", "Human task 1")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE, PlanItemInstanceState.ACTIVE);
        
        assertThat(planItemInstances)
                .filteredOn("name", "Stage 1")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.WAITING_FOR_REPETITION, PlanItemInstanceState.ACTIVE);
        
        PlanItemInstance stagePlanItem = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("cmmnStage1").planItemInstanceState(PlanItemInstanceState.ACTIVE).singleResult();
        
        List<PlanItemInstance> taskPlanItems = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("stageTask1").list();
        
        for (PlanItemInstance taskPlanItemInstance : taskPlanItems) {
            assertThat(taskPlanItemInstance.getStageInstanceId()).isEqualTo(stagePlanItem.getId());
        }
        
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
    void repetitionListenerAndChangedTask() {
        // Arrange
        CaseDefinition originalDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/repetition-with-listener-and-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("repetitionTaskCase")
                .variable("exitVar", "test")
                .start();
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(4);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(originalDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("userEventListener1", "repeatableTask", "cmmnStage1", "stageTask");
        
        UserEventListenerInstance userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId());
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(4);
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("repeatableTask").singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.WAITING_FOR_REPETITION);
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/repetition-with-listener-and-changed-task.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .removeWaitingForRepetitionPlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createRemoveWaitingForRepetitionPlanItemDefinitionMappingFor("repeatableTask"))
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("changedTask"))
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("stageTask"))
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
                .containsExactlyInAnyOrder("userEventListener1", "changedTask");
        
        userEventListenerInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListenerInstance.getId());
    }
    
    @Test
    void terminateUnavailableListener() {
        // Arrange
        CaseDefinition originalDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/unavailable-listener.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(planItemInstances).hasSize(2);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(originalDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getPlanItemDefinitionId)
                .containsExactlyInAnyOrder("humanTask1", "userEventListener1");
        
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("userEventListener1").singleResult();
        assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.UNAVAILABLE);
        
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/unavailable-listener.cmmn.xml");

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
    void testMultiTenantCaseInstanceMigrationWithCustomDefaultTenantProvider() {
        DefaultTenantProvider originalDefaultTenantValue = cmmnEngineConfiguration.getDefaultTenantProvider();
        cmmnEngineConfiguration.setFallbackToDefaultTenant(true);
        CustomTenantProvider customTenantProvider = new CustomTenantProvider();
        cmmnEngineConfiguration.setDefaultTenantProvider(customTenantProvider);
        
        try {
            // Arrange
            CmmnDeployment deployment = cmmnRepositoryService.createDeployment()
                    .name("my deploy")
                    .addClasspathResource("org/flowable/cmmn/test/migration/one-task.cmmn.xml")
                    .tenantId("tenant1-default")
                    .deploy();
            
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("testCase")
                    .tenantId("tenant1")
                    .start();
            
            deployment = cmmnRepositoryService.createDeployment()
                    .name("my deploy")
                    .addClasspathResource("org/flowable/cmmn/test/migration/two-task.cmmn.xml")
                    .tenantId("tenant1-default")
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
    void withAdditionalReptitionTask() {
        // Arrange
        CaseDefinition originalDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/repetition-task.cmmn.xml");

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionId(originalDefinition.getId())
                .start();

        // Assert
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("repeatableTask");
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("repeatableTask").list();
        assertThat(planItemInstances.size()).isEqualTo(1);
        
        Map<String, Object> localVarMap = cmmnRuntimeService.getLocalVariables(planItemInstances.get(0).getId());
        assertThat(localVarMap.size()).isEqualTo(1);
        assertThat(localVarMap.get("repetitionCounter")).isEqualTo(1);

        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/repetition-task-new-repetition-task.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("repeatableTask2"))
                .migrate(caseInstance.getId());

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("repeatableTask2").singleResult();
        assertThat(task.getName()).isEqualTo("repeatableTask2");
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("repeatableTask2").list();
        assertThat(planItemInstances.size()).isEqualTo(1);
        
        localVarMap = cmmnRuntimeService.getLocalVariables(planItemInstances.get(0).getId());
        assertThat(localVarMap.size()).isEqualTo(1);
        assertThat(localVarMap.get("repetitionCounter2")).isEqualTo(1);
        
        // Assert
        cmmnTaskService.complete(task.getId());
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("repeatableTask2").singleResult();
        assertThat(task.getName()).isEqualTo("repeatableTask2");
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("repeatableTask2").list();
        assertThat(planItemInstances.size()).isEqualTo(1);
        
        localVarMap = cmmnRuntimeService.getLocalVariables(planItemInstances.get(0).getId());
        assertThat(localVarMap.size()).isEqualTo(1);
        assertThat(localVarMap.get("repetitionCounter2")).isEqualTo(2);
    }
    
    @Test
    void withAdditionalRepetitionWithTwoTasks() {
        // Arrange
        CaseDefinition originalDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/repetition-2tasks.cmmn.xml");

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionId(originalDefinition.getId())
                .start();

        // Assert
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("initialTask");
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("initialTask").list();
        assertThat(planItemInstances.size()).isEqualTo(1);
        
        Map<String, Object> localVarMap = cmmnRuntimeService.getLocalVariables(planItemInstances.get(0).getId());
        assertThat(localVarMap.size()).isEqualTo(1);
        assertThat(localVarMap.get("initialCounter")).isEqualTo(1);
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("dependingTask").list();
        assertThat(planItemInstances.size()).isEqualTo(1);
        assertThat(planItemInstances.get(0).getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        localVarMap  = cmmnRuntimeService.getLocalVariables(planItemInstances.get(0).getId());
        assertThat(localVarMap.size()).isEqualTo(1);
        assertThat(localVarMap.get("dependingCounter")).isEqualTo(1);
        
        cmmnTaskService.complete(task.getId());
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("dependingTask").singleResult();
        assertThat(task.getName()).isEqualTo("dependingTask");
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("dependingTask").list();
        assertThat(planItemInstances.size()).isEqualTo(2);
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("dependingTask").planItemInstanceStateActive().list();
        assertThat(planItemInstances.size()).isEqualTo(1);
        
        localVarMap = cmmnRuntimeService.getLocalVariables(planItemInstances.get(0).getId());
        assertThat(localVarMap.size()).isEqualTo(1);
        assertThat(localVarMap.get("dependingCounter")).isEqualTo(1);
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("dependingTask").planItemInstanceStateWaitingForRepetition().list();
        assertThat(planItemInstances.size()).isEqualTo(1);
        
        localVarMap = cmmnRuntimeService.getLocalVariables(planItemInstances.get(0).getId());
        assertThat(localVarMap.size()).isEqualTo(1);
        assertThat(localVarMap.get("dependingCounter")).isEqualTo(2);

        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/repetition-2tasks-new-repetition-2tasks.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("extraInitialTask"))
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("extraDependingTask"))
                .migrate(caseInstance.getId());

        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("extraInitialTask").singleResult();
        assertThat(task.getName()).isEqualTo("extraInitialTask");
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("extraInitialTask").list();
        assertThat(planItemInstances.size()).isEqualTo(1);
        
        localVarMap = cmmnRuntimeService.getLocalVariables(planItemInstances.get(0).getId());
        assertThat(localVarMap.size()).isEqualTo(1);
        assertThat(localVarMap.get("extraInitialCounter")).isEqualTo(1);
        
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("extraDependingTask").count()).isEqualTo(0);
        
        // Assert
        cmmnTaskService.complete(task.getId());
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("extraDependingTask").singleResult();
        assertThat(task.getName()).isEqualTo("extraDependingTask");
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("extraDependingTask").list();
        assertThat(planItemInstances.size()).isEqualTo(2);
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("extraDependingTask").planItemInstanceStateActive().list();
        assertThat(planItemInstances.size()).isEqualTo(1);
        
        localVarMap = cmmnRuntimeService.getLocalVariables(planItemInstances.get(0).getId());
        assertThat(localVarMap.size()).isEqualTo(1);
        assertThat(localVarMap.get("extraDependingCounter")).isEqualTo(1);
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemDefinitionId("extraDependingTask").planItemInstanceStateWaitingForRepetition().list();
        assertThat(planItemInstances.size()).isEqualTo(1);
        
        localVarMap = cmmnRuntimeService.getLocalVariables(planItemInstances.get(0).getId());
        assertThat(localVarMap.size()).isEqualTo(1);
        assertThat(localVarMap.get("extraDependingCounter")).isEqualTo(2);
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
        
        List<SentryPartInstanceEntity> sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<>() {

            @Override
            public List<SentryPartInstanceEntity> execute(CommandContext commandContext) {
                return cmmnEngineConfiguration.getSentryPartInstanceEntityManager().findSentryPartInstancesByCaseInstanceId(caseInstance.getId());
            }

        });
        
        assertThat(sentryPartInstances).hasSize(1);
        assertThat(sentryPartInstances.get(0).getOnPartId()).isEqualTo("sentryOnPartcmmnEntrySentry_2");
        assertThat(sentryPartInstances.get(0).getCaseDefinitionId()).isEqualTo(caseInstance.getCaseDefinitionId());
        
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
        
        sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<>() {

            @Override
            public List<SentryPartInstanceEntity> execute(CommandContext commandContext) {
                return cmmnEngineConfiguration.getSentryPartInstanceEntityManager().findSentryPartInstancesByCaseInstanceId(caseInstance.getId());
            }

        });
        
        assertThat(sentryPartInstances).hasSize(1);
        assertThat(sentryPartInstances.get(0).getOnPartId()).isEqualTo("sentryOnPartcmmnEntrySentry_2");
        assertThat(sentryPartInstances.get(0).getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        
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
        
        List<SentryPartInstanceEntity> sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<>() {

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
        
        sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<>() {

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
        
        List<SentryPartInstanceEntity> sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<>() {

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
        
        sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<>() {

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
        
        List<SentryPartInstanceEntity> sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<>() {

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
        
        sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<>() {

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
        
        List<SentryPartInstanceEntity> sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<>() {

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
        
        sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<>() {

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
        
        List<SentryPartInstanceEntity> sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<>() {

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
        
        sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<>() {

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
        
        List<SentryPartInstanceEntity> sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<>() {

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
        
        sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<>() {

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
        
        List<SentryPartInstanceEntity> sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<>() {

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
        
        sentryPartInstances = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<>() {

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

    @Test
    void withPreUpgradeExpression() {
        // Arrange
        CaseDefinition definition1 = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition definition2 = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(definition2.getId())
                .withPreUpgradeExpression("${variableContainer.setVariable('preUpgradeExpressionExecuted', true)}")
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .includeCaseVariables()
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(definition2.getId());
        assertThat(caseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(2);
        assertThat(caseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(definition2.getDeploymentId());

        assertThat((Boolean) caseInstanceAfterMigration.getCaseVariables().getOrDefault("preUpgradeExpressionExecuted", false)).isTrue();
    }

    @Test
    void withPostUpgradeExpression() {
        // Arrange
        CaseDefinition definition1 = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition definition2 = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(definition2.getId())
                .withPostUpgradeExpression("${variableContainer.setVariable('postUpgradeExpressionExecuted', true)}")
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .includeCaseVariables()
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(definition2.getId());
        assertThat(caseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(2);
        assertThat(caseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(definition2.getDeploymentId());

        assertThat((Boolean) caseInstanceAfterMigration.getCaseVariables().getOrDefault("postUpgradeExpressionExecuted", false)).isTrue();
    }
    
    @Test
    void activatePlanItemWithCompletedStage() {
        // Arrange
        CaseDefinition sourceDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/activate-planitem-ended-stages.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("caseWithEndedStage").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/activate-planitem-ended-stages.cmmn.xml");

        Task task = cmmnTaskService.createTaskQuery().scopeId(caseInstance.getId()).taskDefinitionKey("humanTask1").singleResult();
        cmmnTaskService.complete(task.getId());
        
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(sourceDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Human task 2");
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances).hasSize(4);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(sourceDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Human task", "Human task 2", "Service task", "Expanded stage");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("serviceTask1"))
                .migrate(caseInstance.getId());
        
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Human task 2");
        assertThat(planItemInstances)
                .filteredOn("name", "Human task 2")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances).hasSize(5);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Human task", "Human task 2", "Service task", "Service task", "Expanded stage");

        // Assert
        List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId())
                .planItemInstanceDefinitionId("expandedStage1")
                .list();

        assertThat(historicPlanItemInstances.size()).isEqualTo(1);
        assertThat(historicPlanItemInstances.get(0).getState()).isEqualTo("completed");
    }
    
    @Test
    void migrateCaseInstancesWithLocalVariablesForStage() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-local-variables.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("example-stage-case").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-local-variables-extra-task.cmmn.xml");

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        
        Map<String, Object> localStageVarMap = new HashMap<>();
        localStageVarMap.put("stageNr", 1);
        
        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("extra-task"))
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("stage", null, localStageVarMap))
                .migrateCaseInstances(caseInstance.getCaseDefinitionId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances).hasSize(4);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Start Task", "Stage", "Stage Task 1", "Extra Task");
        assertThat(planItemInstances)
                .filteredOn("name", "Start Task")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        assertThat(planItemInstances)
                .filteredOn("name", "Stage")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        assertThat(planItemInstances)
                .filteredOn("name", "Stage Task 1")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        assertThat(planItemInstances)
                .filteredOn("name", "Extra Task")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(3);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("stage-task").singleResult();
        cmmnTaskService.complete(task.getId());
        
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(2);
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("task").singleResult();
        cmmnTaskService.complete(task.getId());
        
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("extra-task").singleResult();
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
            assertThat(historicTasks).hasSize(3);
            for (HistoricTaskInstance historicTask : historicTasks) {
                assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            }
        }
    }
    
    @Test
    void migrateCaseInstancesWithLocalVariablesForRepeatingStage() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-repetition-local-variables.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("example-stage-case").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-repetition-local-variables-extra-task.cmmn.xml");

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(2);
        
        Map<String, Object> localStageVarMap = new HashMap<>();
        localStageVarMap.put("stageNr", 1);
        
        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("extra-task"))
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("repeating-stage", null, localStageVarMap))
                .migrateCaseInstances(caseInstance.getCaseDefinitionId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances).hasSize(6);
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Exit Task", "Start Repeating Task", "Repeating Stage", "Repeating Stage", "Stage repeating Task 1", "Extra Task");
        assertThat(planItemInstances)
                .filteredOn("name", "Start Repeating Task")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        assertThat(planItemInstances)
                .filteredOn("name", "Repeating Stage")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE, PlanItemInstanceState.WAITING_FOR_REPETITION);
        
        assertThat(planItemInstances)
                .filteredOn("name", "Stage repeating Task 1")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        assertThat(planItemInstances)
                .filteredOn("name", "Extra Task")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(4);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("stage-repeating-task").singleResult();
        cmmnTaskService.complete(task.getId());
        
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(3);
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("exit-task").singleResult();
        cmmnTaskService.complete(task.getId());
    
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getCaseDefinitionId())
                .isEqualTo(destinationDefinition.getId());

            List<HistoricPlanItemInstance> historicPlanItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId()).list();
            assertThat(historicPlanItemInstances).hasSize(6);
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
    void migrateCaseInstancesWithLocalVariablesForAvailableStage() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/task-local-variables.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("example-stage-case").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-local-variables.cmmn.xml");

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        
        Map<String, Object> localStageVarMap = new HashMap<>();
        localStageVarMap.put("stageNr", 1);
        
        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("stage", localStageVarMap))
                .migrateCaseInstances(caseInstance.getCaseDefinitionId());

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
                .containsExactlyInAnyOrder("Start Task", "Stage");
        assertThat(planItemInstances)
                .filteredOn("name", "Start Task")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        
        assertThat(planItemInstances)
                .filteredOn("name", "Stage")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.AVAILABLE);
        
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("task").singleResult();
        cmmnTaskService.complete(task.getId());
        
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("stage-task").singleResult();
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
            assertThat(historicTasks).hasSize(2);
            for (HistoricTaskInstance historicTask : historicTasks) {
                assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            }
        }
    }

    @Test
    void migrateCaseInstancesWithConditionalActivate() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").variable("activateTask3", true).start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").variable("activateTask3", false).start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/three-task.cmmn.xml");

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance1.getId()).count()).isEqualTo(2);

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addMoveToAvailablePlanItemDefinitionMapping(
                        PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("humanTask3", "${activateTask3}"))
                .migrateCaseInstances(caseInstance1.getCaseDefinitionId());

        // Assert
        CaseInstance caseInstance1AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .singleResult();
        assertThat(caseInstance1AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances1 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances1).hasSize(3);
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1", "Task 2", "Task 3");
        assertThat(planItemInstances1)
                .filteredOn("name", "Task 3")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);

        CaseInstance caseInstance2AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .singleResult();
        assertThat(caseInstance2AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances2 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances2).hasSize(2);
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1", "Task 2");
    }

    @Test
    void migrateCaseInstancesWithConditionalTerminateAndLocalVariable() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/second-task-with-sentry.cmmn.xml");

        PlanItemInstance planItemInstanceCase1Task2 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .planItemInstanceName("Task 2")
                .singleResult();
        PlanItemInstance planItemInstanceCase2Task2 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .planItemInstanceName("Task 2")
                .singleResult();
        cmmnRuntimeService.setLocalVariable(planItemInstanceCase1Task2.getId(), "disableTask2", false);
        cmmnRuntimeService.setLocalVariable(planItemInstanceCase2Task2.getId(), "disableTask2", true);

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance1.getId()).count()).isEqualTo(2);

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addTerminatePlanItemDefinitionMapping(
                        PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("humanTask2", "${disableTask2}"))
                .migrateCaseInstances(caseInstance1.getCaseDefinitionId());

        // Assert
        CaseInstance caseInstance1AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .singleResult();
        assertThat(caseInstance1AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances1 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances1).hasSize(2);
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1", "Task 2");
        assertThat(planItemInstances1)
                .filteredOn("name", "Task 2")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);

        CaseInstance caseInstance2AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .singleResult();
        assertThat(caseInstance2AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances2 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances2).hasSize(2);
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1", "Task 2");
        assertThat(planItemInstances2)
                .filteredOn("name", "Task 2")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.TERMINATED);
    }

    @Test
    void migrateCaseInstancesWithStageActive() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-three-tasks.cmmn.xml");
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("example-stage-case").start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("example-stage-case").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-three-tasks-two-new.cmmn.xml");

        Task task = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance1.getId())
                .taskName("Task 1")
                .singleResult();
        cmmnTaskService.complete(task.getId());

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance1.getId()).count()).isEqualTo(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance2.getId()).count()).isEqualTo(2);

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("task4",
                        "${planItemInstances.definitionId(\"cmmnStage_1\").active().exists()}"))
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("task5",
                        "${planItemInstances.definitionId(\"cmmnStage_2\").active().exists()}"))
                .migrateCaseInstances(caseInstance1.getCaseDefinitionId());

        // Assert
        CaseInstance caseInstance1AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .singleResult();
        assertThat(caseInstance1AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances1 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances1).hasSize(6);
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 2", "Task 5", "Task 3", "Stage 1", "Stage 2", "Task 1");
        assertThat(planItemInstances1)
                .filteredOn("name", "Task 5")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);

        assertThat(planItemInstances1)
                .filteredOn("name", "Stage 1")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.COMPLETED);
        assertThat(planItemInstances1)
                .filteredOn("name", "Task 1")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.COMPLETED);

        CaseInstance caseInstance2AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .singleResult();
        assertThat(caseInstance2AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances2 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances2).hasSize(5);
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1", "Task 4", "Task 3", "Stage 1", "Stage 2");
        assertThat(planItemInstances2)
                .filteredOn("name", "Task 4")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        assertThat(planItemInstances2)
                .filteredOn("name", "Stage 1")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        assertThat(planItemInstances2)
                .filteredOn("name", "Stage 2")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.AVAILABLE);
    }

    @Test
    void migrateCaseInstancesWithStageActiveBasedOnStageCondition() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-three-tasks.cmmn.xml");
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("example-stage-case").variable("activateTask", false).start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("example-stage-case").variable("activateTask", false).start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-three-tasks-two-new.cmmn.xml");

        Task task = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance1.getId())
                .taskName("Task 1")
                .singleResult();
        cmmnTaskService.complete(task.getId());

        // write the local variables activateTask to true to make the information available for every stage in which we are
        List<PlanItemInstance> activeStages = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(ELEMENT_STAGE)
                .planItemInstanceStateActive()
                .list();
        for (PlanItemInstance activeStage : activeStages) {
            cmmnRuntimeService.setLocalVariable(activeStage.getId(), "activateTask", true);
        }

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance1.getId()).count()).isEqualTo(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance2.getId()).count()).isEqualTo(2);

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                // we can't assume that it's the direct parent, since it's always taking the closest active ancestor
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("task4",
                        "${activateTask}"))
                .addActivatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("task5",
                        "${activateTask}"))
                .migrateCaseInstances(caseInstance1.getCaseDefinitionId());

        // Assert
        CaseInstance caseInstance1AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .singleResult();
        assertThat(caseInstance1AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances1 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances1).hasSize(6);
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 2", "Task 5", "Task 3", "Stage 1", "Stage 2", "Task 1");
        assertThat(planItemInstances1)
                .filteredOn("name", "Task 5")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);

        assertThat(planItemInstances1)
                .filteredOn("name", "Stage 1")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.COMPLETED);
        assertThat(planItemInstances1)
                .filteredOn("name", "Task 1")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.COMPLETED);

        CaseInstance caseInstance2AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .singleResult();
        assertThat(caseInstance2AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances2 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances2).hasSize(5);
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1", "Task 4", "Task 3", "Stage 1", "Stage 2");
        assertThat(planItemInstances2)
                .filteredOn("name", "Task 4")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        assertThat(planItemInstances2)
                .filteredOn("name", "Stage 1")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        assertThat(planItemInstances2)
                .filteredOn("name", "Stage 2")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.AVAILABLE);
    }

    @Test
    void migrateCaseInstancesWithStageAvailableBasedOnStageCondition() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-three-tasks.cmmn.xml");
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("example-stage-case").variable("activateTask", false).start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("example-stage-case").variable("activateTask", false).start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-three-tasks-two-new.cmmn.xml");

        Task task = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance1.getId())
                .taskName("Task 1")
                .singleResult();
        cmmnTaskService.complete(task.getId());

        // write the local variables activateTask to true to make the information available for every stage in which we are
        List<PlanItemInstance> activeStages = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(ELEMENT_STAGE)
                .planItemInstanceStateActive()
                .list();
        for (PlanItemInstance activeStage : activeStages) {
            cmmnRuntimeService.setLocalVariable(activeStage.getId(), "activateTask", true);
        }

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance1.getId()).count()).isEqualTo(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance2.getId()).count()).isEqualTo(2);

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                // we can't assume that it's the direct parent, since it's always taking the closest active ancestor
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("task4",
                        "${activateTask}"))
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("task5",
                        "${activateTask}"))
                .migrateCaseInstances(caseInstance1.getCaseDefinitionId());

        // Assert
        CaseInstance caseInstance1AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .singleResult();
        assertThat(caseInstance1AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances1 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances1).hasSize(6);
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 2", "Task 5", "Task 3", "Stage 1", "Stage 2", "Task 1");
        assertThat(planItemInstances1)
                .filteredOn("name", "Task 5")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);

        assertThat(planItemInstances1)
                .filteredOn("name", "Stage 1")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.COMPLETED);
        assertThat(planItemInstances1)
                .filteredOn("name", "Task 1")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.COMPLETED);

        CaseInstance caseInstance2AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .singleResult();
        assertThat(caseInstance2AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances2 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances2).hasSize(5);
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1", "Task 4", "Task 3", "Stage 1", "Stage 2");
        assertThat(planItemInstances2)
                .filteredOn("name", "Task 4")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        assertThat(planItemInstances2)
                .filteredOn("name", "Stage 1")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.ACTIVE);
        assertThat(planItemInstances2)
                .filteredOn("name", "Stage 2")
                .extracting(PlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.AVAILABLE);
    }

    @Test
    void migrateCaseInstancesWithMoveStageToAvailableAndAlreadyAvailableStage() {
        // Arrange
        CaseDefinition definition1 = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/task-followed-by-stage.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition definition2 = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/task-followed-by-stage.cmmn.xml");

        assertThat(definition2.getId()).isNotEqualTo(definition1.getId());

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(definition2.getId())
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("cmmnStage_2"))
                .migrate(caseInstance.getId());

        // Assert
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances)
                .hasSize(2)
                .extracting(PlanItemInstance::getName, PlanItemInstance::getState)
                .containsExactlyInAnyOrder(
                        Tuple.tuple("Task 1", PlanItemInstanceState.ACTIVE),
                        Tuple.tuple("Stage", PlanItemInstanceState.AVAILABLE)
                );
    }

    @Test
    void migrateCaseInstancesWithRepetitionAndStageVariable() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-with-user-event-listener-and-task.cmmn.xml");
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repetitionCase").start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repetitionCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-with-user-event-listener-and-task-with-repetition.cmmn.xml");

        PlanItemInstance caseInstance1Stage = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceName("Stage")
                .caseInstanceId(caseInstance1.getId())
                .singleResult();
        cmmnRuntimeService.setLocalVariable(caseInstance1Stage.getId(), "addRepetition", true);

        PlanItemInstance userEventListenerPlanItemInstance1 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .planItemDefinitionId("userEventListener")
                .singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(userEventListenerPlanItemInstance1.getId());

        PlanItemInstance caseInstance2Stage = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceName("Stage")
                .caseInstanceId(caseInstance2.getId())
                .singleResult();
        cmmnRuntimeService.setLocalVariable(caseInstance2Stage.getId(), "addRepetition", false);

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addWaitingForRepetitionPlanItemDefinitionMapping(
                        PlanItemDefinitionMappingBuilder.createWaitingForRepetitionPlanItemDefinitionMappingFor("humanTask", "${addRepetition}")
                )
                .migrateCaseInstances(caseInstance1.getCaseDefinitionId());

        // Assert
        CaseInstance caseInstance1AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .singleResult();
        assertThat(caseInstance1AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances1 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances1).hasSize(4);
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Stage", "User Event Listener", "Human Task", "Human Task");
        assertThat(planItemInstances1)
                .filteredOn("name", "Human Task")
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(PlanItemInstanceState.ACTIVE, PlanItemInstanceState.WAITING_FOR_REPETITION);

        CaseInstance caseInstance2AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .singleResult();
        assertThat(caseInstance2AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances2 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances2).hasSize(3);
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Stage", "User Event Listener", "Human Task");
        assertThat(planItemInstances2)
                .filteredOn("name", "Human Task")
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(PlanItemInstanceState.AVAILABLE);
    }

    @Test
    void migrateCaseInstancesWithRepetitionAndStageVariableAndInactiveHumanTask() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-with-user-event-listener-and-task.cmmn.xml");
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repetitionCase").start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repetitionCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-with-user-event-listener-and-task-with-repetition.cmmn.xml");

        PlanItemInstance caseInstance1Stage = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceName("Stage")
                .caseInstanceId(caseInstance1.getId())
                .singleResult();
        cmmnRuntimeService.setLocalVariable(caseInstance1Stage.getId(), "addRepetition", true);

        PlanItemInstance caseInstance2Stage = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceName("Stage")
                .caseInstanceId(caseInstance2.getId())
                .singleResult();
        cmmnRuntimeService.setLocalVariable(caseInstance2Stage.getId(), "addRepetition", false);

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addWaitingForRepetitionPlanItemDefinitionMapping(
                        PlanItemDefinitionMappingBuilder.createWaitingForRepetitionPlanItemDefinitionMappingFor("humanTask", "${addRepetition}")
                )
                .migrateCaseInstances(caseInstance1.getCaseDefinitionId());

        // Assert
        CaseInstance caseInstance1AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .singleResult();
        assertThat(caseInstance1AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances1 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances1).hasSize(4);
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Stage", "User Event Listener", "Human Task", "Human Task");
        assertThat(planItemInstances1)
                .filteredOn("name", "Human Task")
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.WAITING_FOR_REPETITION);

        CaseInstance caseInstance2AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .singleResult();
        assertThat(caseInstance2AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances2 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances2).hasSize(3);
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Stage", "User Event Listener", "Human Task");
        assertThat(planItemInstances2)
                .filteredOn("name", "Human Task")
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(PlanItemInstanceState.AVAILABLE);
    }

    @Test
    void migrateCaseInstancesWithRepetitionAndPlanItemVariable() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-with-user-event-listener-and-task.cmmn.xml");
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repetitionCase").start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repetitionCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-with-user-event-listener-and-task-with-repetition.cmmn.xml");

        PlanItemInstance humanTaskCase1 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceName("Human Task")
                .caseInstanceId(caseInstance1.getId())
                .singleResult();
        cmmnRuntimeService.setLocalVariable(humanTaskCase1.getId(), "addRepetition", true);

        PlanItemInstance userEventListenerPlanItemInstance1 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .planItemDefinitionId("userEventListener")
                .singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(userEventListenerPlanItemInstance1.getId());

        PlanItemInstance caseInstance2Stage = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceName("Stage")
                .caseInstanceId(caseInstance2.getId())
                .singleResult();
        cmmnRuntimeService.setLocalVariable(caseInstance2Stage.getId(), "addRepetition", false);

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addWaitingForRepetitionPlanItemDefinitionMapping(
                        PlanItemDefinitionMappingBuilder.createWaitingForRepetitionPlanItemDefinitionMappingFor("humanTask", "${addRepetition}")
                )
                .migrateCaseInstances(caseInstance1.getCaseDefinitionId());

        // Assert
        CaseInstance caseInstance1AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .singleResult();
        assertThat(caseInstance1AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances1 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances1).hasSize(4);
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Stage", "User Event Listener", "Human Task", "Human Task");
        assertThat(planItemInstances1)
                .filteredOn("name", "Human Task")
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(PlanItemInstanceState.ACTIVE, PlanItemInstanceState.WAITING_FOR_REPETITION);

        CaseInstance caseInstance2AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .singleResult();
        assertThat(caseInstance2AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances2 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances2).hasSize(3);
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Stage", "User Event Listener", "Human Task");
        assertThat(planItemInstances2)
                .filteredOn("name", "Human Task")
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(PlanItemInstanceState.AVAILABLE);
    }

    @Test
    void migrateCaseInstancesWithTerminateBasedOnCondition() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task-repetition.cmmn.xml");
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");

        Task task = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance1.getId())
                .taskName("Task 2")
                .singleResult();
        cmmnTaskService.complete(task.getId());

        List<PlanItemInstance> planItemInstances1Before = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances1Before)
                .filteredOn("name", "Task 2")
                .hasSize(2)
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(PlanItemInstanceState.ACTIVE, PlanItemInstanceState.COMPLETED);

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance1.getId()).count()).isEqualTo(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance2.getId()).count()).isEqualTo(2);

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("humanTask2",
                        "${planItemInstances.definitionId(\"humanTask2\").completed().exists()}"))
                .migrateCaseInstances(caseInstance1.getCaseDefinitionId());

        // Assert
        CaseInstance caseInstance1AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .singleResult();
        assertThat(caseInstance1AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances1 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances1).hasSize(3);
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1", "Task 2", "Task 2");
        assertThat(planItemInstances1)
                .filteredOn("name", "Task 2")
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(PlanItemInstanceState.TERMINATED, PlanItemInstanceState.COMPLETED);

        CaseInstance caseInstance2AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .singleResult();
        assertThat(caseInstance2AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances2 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances2).hasSize(2);
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1", "Task 2");
        assertThat(planItemInstances2)
                .filteredOn("name", "Task 2")
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(PlanItemInstanceState.ACTIVE);
    }

    @Test
    void migrateCaseInstancesWithRepetitionRemoveWaitingForRepetitionAndPlanItemVariable() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-with-user-event-listener-and-task-with-repetition.cmmn.xml");
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repetitionCase").start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("repetitionCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-with-user-event-listener-and-task.cmmn.xml");

        PlanItemInstance userEventListenerPlanItemInstance1 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .planItemDefinitionId("userEventListener")
                .singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(userEventListenerPlanItemInstance1.getId());

        PlanItemInstance humanTaskCase1 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceName("Human Task")
                .caseInstanceId(caseInstance1.getId())
                .planItemInstanceStateWaitingForRepetition()
                .singleResult();
        cmmnRuntimeService.setLocalVariable(humanTaskCase1.getId(), "removeRepetition", true);

        PlanItemInstance userEventListenerPlanItemInstance2 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .planItemDefinitionId("userEventListener")
                .singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(userEventListenerPlanItemInstance2.getId());

        PlanItemInstance humanTaskCase2 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemInstanceName("Human Task")
                .caseInstanceId(caseInstance2.getId())
                .planItemInstanceStateWaitingForRepetition()
                .singleResult();
        cmmnRuntimeService.setLocalVariable(humanTaskCase2.getId(), "removeRepetition", false);

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .removeWaitingForRepetitionPlanItemDefinitionMapping(
                        PlanItemDefinitionMappingBuilder.createRemoveWaitingForRepetitionPlanItemDefinitionMappingFor("humanTask", "${removeRepetition}")
                )
                .migrateCaseInstances(caseInstance1.getCaseDefinitionId());

        // Assert
        CaseInstance caseInstance1AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .singleResult();
        assertThat(caseInstance1AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances1 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance1.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getName)
                // User event listener is twice since we triggered it once already
                .containsExactlyInAnyOrder("Stage", "User Event Listener", "User Event Listener", "Human Task");
        assertThat(planItemInstances1)
                .filteredOn("name", "Human Task")
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(PlanItemInstanceState.ACTIVE);

        CaseInstance caseInstance2AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .singleResult();
        assertThat(caseInstance2AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances2 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getName)
                .containsExactlyInAnyOrder("Stage", "User Event Listener", "User Event Listener", "Human Task", "Human Task");
        assertThat(planItemInstances2)
                .filteredOn("name", "Human Task")
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(PlanItemInstanceState.ACTIVE, PlanItemInstanceState.WAITING_FOR_REPETITION);
    }


    @Test
    void migrateCaseInstancesWithRemoveWaitingForRepetitionBasedOnCondition() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/user-event-listener-with-repetition.cmmn.xml");
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("stageCase").start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("stageCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1",
                "org/flowable/cmmn/test/migration/user-event-listener-without-repetition.cmmn.xml");

        String caseInstanceId1 = caseInstance1.getId();
        PlanItemInstance userEventListener = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstanceId1)
                .planItemDefinitionId("userEventListener")
                .singleResult();
        cmmnRuntimeService.createPlanItemInstanceTransitionBuilder(userEventListener.getId())
                .trigger();

        List<PlanItemInstance> planItemInstances1Before = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstanceId1)
                .includeEnded()
                .list();
        assertThat(planItemInstances1Before)
                .filteredOn("name", "Task 1")
                .hasSize(2)
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(PlanItemInstanceState.WAITING_FOR_REPETITION, PlanItemInstanceState.ACTIVE);

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceId1).count()).isEqualTo(2);
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance2.getId()).count()).isEqualTo(1);

        // Act
        String condition = "${planItemInstances.definitionId(\"humanTask1\").waitingForRepetition().exists()}";
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("userEventListener",
                        condition)) // end the event listener first
                .removeWaitingForRepetitionPlanItemDefinitionMapping(
                        PlanItemDefinitionMappingBuilder.createRemoveWaitingForRepetitionPlanItemDefinitionMappingFor("humanTask1", condition))
                .migrateCaseInstances(caseInstance1.getCaseDefinitionId());

        // Assert
        CaseInstance caseInstance1AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstanceId1)
                .singleResult();
        assertThat(caseInstance1AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances1 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstanceId1)
                .includeEnded()
                .list();
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getName)
                .filteredOn(Objects::nonNull)
                .containsExactlyInAnyOrder("Task 1", "Task 2"); // waiting for repetition isn't visible anymore, thus Task 1 is only once in here
        assertThat(planItemInstances1)
                .filteredOn("name", "Task 1")
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(PlanItemInstanceState.ACTIVE);
        assertThat(planItemInstances1)
                .filteredOn("planItemDefinitionId", "userEventListener")
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(PlanItemInstanceState.TERMINATED, PlanItemInstanceState.COMPLETED);

        CaseInstance caseInstance2AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .singleResult();
        assertThat(caseInstance2AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances2 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getName)
                .filteredOn(Objects::nonNull)
                .containsExactlyInAnyOrder("Task 1", "Task 2");
        assertThat(planItemInstances2)
                .filteredOn("name", "Task 1")
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(PlanItemInstanceState.AVAILABLE);
        assertThat(planItemInstances2)
                .filteredOn("planItemDefinitionId", "userEventListener")
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(PlanItemInstanceState.AVAILABLE);
    }

    @Test
    void migrateCaseInstancesWithMarkAsActiveBasedOnCondition() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-without-event-listener-inside.cmmn.xml");
        CaseInstance caseInstance1 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/stage-with-event-listener-inside.cmmn.xml");

        String caseInstanceId1 = caseInstance1.getId();

        Task task = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstanceId1)
                .taskName("Task 1")
                .singleResult();
        cmmnTaskService.complete(task.getId());
        // now the stage is active

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceId1).count()).isEqualTo(1); // Task 2
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance2.getId()).count()).isEqualTo(1); // Task 1

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addMoveToAvailablePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor(
                        "eventListener",
                        "${planItemInstances.definitionId(\"cmmnStage_6\").active().exists()}"
                ))
                .migrateCaseInstances(caseInstance1.getCaseDefinitionId());

        // Assert
        CaseInstance caseInstance1AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstanceId1)
                .singleResult();
        assertThat(caseInstance1AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances1 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstanceId1)
                .includeEnded()
                .list();
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances1)
                .extracting(PlanItemInstance::getName)
                .filteredOn(Objects::nonNull)
                .contains("Event Listener");
        assertThat(planItemInstances1)
                .filteredOn("name", "Event Listener")
                .extracting(PlanItemInstance::getState)
                .containsExactlyInAnyOrder(PlanItemInstanceState.AVAILABLE);

        CaseInstance caseInstance2AfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .singleResult();
        assertThat(caseInstance2AfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances2 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance2.getId())
                .includeEnded()
                .list();
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances2)
                .extracting(PlanItemInstance::getName)
                .filteredOn(Objects::nonNull)
                .doesNotContain("Event Listener");
    }


    @Test
    void withCaseTaskWhichWillBeTerminated() {
        // Arrange
        CaseDefinition subcaseDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        deployCaseDefinition("test2", "org/flowable/cmmn/test/migration/case-with-subcase.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testParentCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test2", "org/flowable/cmmn/test/migration/case-without-subcase.cmmn.xml");

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addTerminatePlanItemDefinitionMapping(PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("cmmnTask_1"))
                .migrate(caseInstance.getId());

        // Assert
        HistoricPlanItemInstance cmmnTask1 = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId())
                .planItemInstanceDefinitionId("cmmnTask_1")
                .singleResult();
        assertThat(cmmnTask1.getState()).isEqualTo(PlanItemInstanceState.TERMINATED);
        long subcaseInstances = cmmnRuntimeService.createCaseInstanceQuery()
                .caseDefinitionId(subcaseDefinition.getId())
                .count();
        assertThat(subcaseInstances).isEqualTo(0);
    }

    @Test
    void testCaseInstanceMigrationDocument() {
        Map<String, Object> localVariables = new HashMap<>();
        localVariables.put("myStr", "abc");
        localVariables.put("myInt", 2);

        ActivatePlanItemDefinitionMapping activatePlanItemDefinitionMapping = PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("activateId", "${activateCondition}");
        activatePlanItemDefinitionMapping.setWithLocalVariables(localVariables);

        MoveToAvailablePlanItemDefinitionMapping moveToAvailablePlanItemDefinitionMapping = PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("availableId", "${availableCondition}");
        moveToAvailablePlanItemDefinitionMapping.setWithLocalVariables(localVariables);

        String documentAsJson = this.cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .addTerminatePlanItemDefinitionMapping(
                        PlanItemDefinitionMappingBuilder.createTerminatePlanItemDefinitionMappingFor("terminateId", "${terminateCondition}"))
                .addMoveToAvailablePlanItemDefinitionMapping(moveToAvailablePlanItemDefinitionMapping)
                .addWaitingForRepetitionPlanItemDefinitionMapping(
                        PlanItemDefinitionMappingBuilder.createWaitingForRepetitionPlanItemDefinitionMappingFor("repetitionId", "${repetitionCondition}"))
                .removeWaitingForRepetitionPlanItemDefinitionMapping(
                        PlanItemDefinitionMappingBuilder.createRemoveWaitingForRepetitionPlanItemDefinitionMappingFor("removeRepetitionId",
                                "${removeRepetitionCondition}"))
                .addActivatePlanItemDefinitionMapping(activatePlanItemDefinitionMapping)
                .addChangePlanItemIdMapping(new ChangePlanItemIdMapping("oldPlanItemId", "newPlanItemId"))
                .addChangePlanItemIdWithDefinitionIdMapping(new ChangePlanItemIdWithDefinitionIdMapping("oldPlanItemDefinitionId", "newPlanItemDefinitionId"))
                .withPreUpgradeExpression("${preExpression}")
                .withPostUpgradeExpression("${postExpression}")
                .getCaseInstanceMigrationDocument()
                .asJsonString();
        CaseInstanceMigrationDocument caseInstanceMigrationDocumentFromString = CaseInstanceMigrationDocumentConverter.convertFromJson(documentAsJson);
        String documentAsJsonConverted = this.cmmnMigrationService.createCaseInstanceMigrationBuilderFromCaseInstanceMigrationDocument(
                        caseInstanceMigrationDocumentFromString)
                .getCaseInstanceMigrationDocument()
                .asJsonString();
        assertThatJson(documentAsJsonConverted).isEqualTo(documentAsJson);
    }

    @Test
    void testMigrationWithMappingsWithLocalVariables() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/three-tasks-with-sentry.cmmn.xml");

        Map<String, Object> taskTwoLocalVariables = new HashMap<>();
        taskTwoLocalVariables.put("myStr", "abc");
        taskTwoLocalVariables.put("myInt", 2);
        Map<String, Object> taskThreeLocalVariables = new HashMap<>();
        taskThreeLocalVariables.put("myStr", "def");
        taskThreeLocalVariables.put("myInt", 3);

        ActivatePlanItemDefinitionMapping activatePlanItemDefinitionMapping = PlanItemDefinitionMappingBuilder.createActivatePlanItemDefinitionMappingFor("humanTask2");
        activatePlanItemDefinitionMapping.setWithLocalVariables(taskTwoLocalVariables);

        MoveToAvailablePlanItemDefinitionMapping moveToAvailablePlanItemDefinitionMapping = PlanItemDefinitionMappingBuilder.createMoveToAvailablePlanItemDefinitionMappingFor("humanTask3");
        moveToAvailablePlanItemDefinitionMapping.setWithLocalVariables(taskThreeLocalVariables);

        List<PlanItemInstance> planItemInstances2 = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();

        // Act
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .addActivatePlanItemDefinitionMapping(activatePlanItemDefinitionMapping)
                .addMoveToAvailablePlanItemDefinitionMapping(moveToAvailablePlanItemDefinitionMapping)
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(caseInstanceAfterMigration.getCaseDefinitionKey()).isEqualTo("testCase");
        assertThat(caseInstanceAfterMigration.getCaseDefinitionName()).isEqualTo("threeTasks");
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
                .containsOnly(PlanItemInstanceState.ACTIVE, PlanItemInstanceState.AVAILABLE);

        PlanItemInstance planItemInstanceTaskTwo = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("humanTask2").singleResult();
        Map<String, Object> localVarMapTaskTwo = cmmnRuntimeService.getLocalVariables(planItemInstanceTaskTwo.getId());
        assertThat(localVarMapTaskTwo.get("myStr")).isEqualTo("abc");
        assertThat(localVarMapTaskTwo.get("myInt")).isEqualTo(2);

        PlanItemInstance planItemInstanceTaskThree = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionId("humanTask3").singleResult();
        Map<String, Object> localVarMapTaskThree = cmmnRuntimeService.getLocalVariables(planItemInstanceTaskThree.getId());
        assertThat(localVarMapTaskThree.get("myStr")).isEqualTo("def");
        assertThat(localVarMapTaskThree.get("myInt")).isEqualTo(3);

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        for (Task task : tasks) {
            assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
            cmmnTaskService.complete(task.getId());
        }

        Task thirdTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(thirdTask.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }

    protected class CustomTenantProvider implements DefaultTenantProvider {

        @Override
        public String getDefaultTenant(String tenantId, String scope, String scopeKey) {
            return tenantId + "-default";
        }
        
    }
}
