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

import org.flowable.cmmn.api.migration.CaseInstanceMigrationValidationResult;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

public class CaseInstanceDirectPlanItemMigrationTest extends AbstractCaseMigrationTest {

    @Test
    void directMigrateOneTaskCase() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-start.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-rename-task.cmmn.xml");

        CaseInstanceMigrationValidationResult validationResult = cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .validateMigration(caseInstance.getId());
        
        assertThat(validationResult.isMigrationValid()).isTrue();

        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .enableAutomaticPlanItemInstanceCreation()
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
        assertThat(planItemInstances.get(0).getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(planItemInstances.get(0).getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(1);
        Task task = tasks.get(0);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Task 1 updated");
        cmmnTaskService.complete(task.getId());
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }
    
    @Test
    void directMigrateRenameTaskIdCase() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-start.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-rename-task-id.cmmn.xml");

        CaseInstanceMigrationValidationResult validationResult = cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .validateMigration(caseInstance.getId());
        
        assertThat(validationResult.isMigrationValid()).isTrue();

        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .enableAutomaticPlanItemInstanceCreation()
                .migrate(caseInstance.getId());

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(1);
        Task task = tasks.get(0);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Task 1 updated");
        cmmnTaskService.complete(task.getId());
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }
    
    @Test
    void directMigrateAddTaskCase() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-start.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-add-task.cmmn.xml");

        CaseInstanceMigrationValidationResult validationResult = cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .validateMigration(caseInstance.getId());
        
        assertThat(validationResult.isMigrationValid()).isTrue();

        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .enableAutomaticPlanItemInstanceCreation()
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
        for (PlanItemInstance planItemInstance : planItemInstances) {
            assertThat(planItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            if ("humanTask1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
                
            } else if ("humanTask2".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
            }
        }
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(1);
        Task task = tasks.get(0);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Task 1");
        cmmnTaskService.complete(task.getId());
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(1);
        task = tasks.get(0);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Task 2");
        cmmnTaskService.complete(task.getId());
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }
    
    @Test
    void directMigrateAddRepetitionTaskCase() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-start.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-add-repetition-task.cmmn.xml");

        CaseInstanceMigrationValidationResult validationResult = cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .validateMigration(caseInstance.getId());
        
        assertThat(validationResult.isMigrationValid()).isTrue();

        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .enableAutomaticPlanItemInstanceCreation()
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
        for (PlanItemInstance planItemInstance : planItemInstances) {
            assertThat(planItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            if ("humanTask1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
                
            } else if ("humanTask2".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            }
        }
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).hasSize(2);
        Task task = tasks.get(0);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Task 1");
        cmmnTaskService.complete(task.getId());
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(1);
        task = tasks.get(0);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Task 2");
        cmmnTaskService.complete(task.getId());
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(1);
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(1);
        
        cmmnTaskService.complete(tasks.get(0).getId());
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(1);
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(1);
    }
    
    @Test
    void directMigrateAddConditionalTaskWithFalseEvaluationCase() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-start.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCase")
                .variable("var1", "John Doe")
                .start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-add-conditional-task.cmmn.xml");

        CaseInstanceMigrationValidationResult validationResult = cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .validateMigration(caseInstance.getId());
        
        assertThat(validationResult.isMigrationValid()).isTrue();

        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .enableAutomaticPlanItemInstanceCreation()
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
        for (PlanItemInstance planItemInstance : planItemInstances) {
            assertThat(planItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            if ("humanTask1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
                
            } else if ("humanTask2".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
            }
        }
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).hasSize(1);
        
        cmmnRuntimeService.setVariable(caseInstance.getId(), "var1", "test");
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).hasSize(2);
        
        Task task = tasks.get(0);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Task 1");
        cmmnTaskService.complete(task.getId());
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(1);
        task = tasks.get(0);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Task 2");
        cmmnTaskService.complete(task.getId());
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(0);
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }
    
    @Test
    void directMigrateAddConditionalTaskWithTrueEvaluationCase() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-start.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCase")
                .variable("var1", "test")
                .start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-add-conditional-task.cmmn.xml");

        CaseInstanceMigrationValidationResult validationResult = cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .validateMigration(caseInstance.getId());
        
        assertThat(validationResult.isMigrationValid()).isTrue();

        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .enableAutomaticPlanItemInstanceCreation()
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
        for (PlanItemInstance planItemInstance : planItemInstances) {
            assertThat(planItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            if ("humanTask1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
                
            } else if ("humanTask2".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            }
        }
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).hasSize(2);
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).hasSize(2);
        
        Task task = tasks.get(0);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Task 1");
        cmmnTaskService.complete(task.getId());
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(1);
        task = tasks.get(0);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Task 2");
        cmmnTaskService.complete(task.getId());
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(0);
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }

    @Test
    void directMigrateAddTaskCaseWithAutomaticPlanItemInstanceCreation() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-start.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-add-task.cmmn.xml");

        CaseInstanceMigrationValidationResult validationResult = cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .validateMigration(caseInstance.getId());
        
        assertThat(validationResult.isMigrationValid()).isTrue();

        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .enableAutomaticPlanItemInstanceCreation()
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
        for (PlanItemInstance planItemInstance : planItemInstances) {
            assertThat(planItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            if ("humanTask1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
                
            } else if ("humanTask2".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
            }
        }
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(1);
        Task task = tasks.get(0);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Task 1");
        cmmnTaskService.complete(task.getId());
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(1);
        task = tasks.get(0);
        assertThat(task.getName()).isEqualTo("Task 2");
        cmmnTaskService.complete(task.getId());
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }
    
    @Test
    void directMigrateAddStageListenerTaskCaseWithAutomaticPlanItemInstanceCreation() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-start.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task-add-stage-listener-task.cmmn.xml");

        CaseInstanceMigrationValidationResult validationResult = cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .validateMigration(caseInstance.getId());
        
        assertThat(validationResult.isMigrationValid()).isTrue();

        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .enableAutomaticPlanItemInstanceCreation()
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(6);
        for (PlanItemInstance planItemInstance : planItemInstances) {
            assertThat(planItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            if ("humanTask1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
                
            } else if ("humanTask2".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            
            } else if ("userEventListener1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
            
            } else if ("expandedStage1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            
            } else if ("stage1HumanTask1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            
            } else if ("stage1UserEventListener1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
            }
        }
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).hasSize(3);
        
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(2);
        
        Task task = tasks.get(0);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Stage task 1");
        cmmnTaskService.complete(task.getId());
        task = tasks.get(1);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Task 1");
        cmmnTaskService.complete(task.getId());
        task = tasks.get(2);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Task 2");
        cmmnTaskService.complete(task.getId());
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(0);
        
        UserEventListenerInstance userEventListener = cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnRuntimeService.completeUserEventListenerInstance(userEventListener.getId());
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }
    
    @Test
    void directMigrateAddListenerTaskToStageCaseWithAutomaticPlanItemInstanceCreation() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/task-and-stage-start.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/task-and-stage-add-task-listener.cmmn.xml");

        CaseInstanceMigrationValidationResult validationResult = cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .validateMigration(caseInstance.getId());
        
        assertThat(validationResult.isMigrationValid()).isTrue();

        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .enableAutomaticPlanItemInstanceCreation()
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(6);
        for (PlanItemInstance planItemInstance : planItemInstances) {
            assertThat(planItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            if ("humanTask1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
                
            } else if ("humanTask2".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            
            } else if ("expandedStage1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            
            } else if ("stage1HumanTask1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
                
            } else if ("stage1HumanTask2".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            
            } else if ("stage1UserEventListener1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
            }
        }
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).hasSize(4);
        
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(1);
        
        Task task = tasks.get(0);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Stage task 1");
        cmmnTaskService.complete(task.getId());
        task = tasks.get(1);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Stage task 2");
        cmmnTaskService.complete(task.getId());
        task = tasks.get(2);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Task 1");
        cmmnTaskService.complete(task.getId());
        task = tasks.get(3);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Task 2");
        cmmnTaskService.complete(task.getId());
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(0);
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }
    
    @Test
    void directMigrateAddTaskWithSentryToStageCaseWithAutomaticPlanItemInstanceCreation() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/task-and-stage-start.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/task-and-stage-add-task-with-sentry.cmmn.xml");

        CaseInstanceMigrationValidationResult validationResult = cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .validateMigration(caseInstance.getId());
        
        assertThat(validationResult.isMigrationValid()).isTrue();

        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .enableAutomaticPlanItemInstanceCreation()
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(4);
        for (PlanItemInstance planItemInstance : planItemInstances) {
            assertThat(planItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            if ("humanTask1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            
            } else if ("expandedStage1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            
            } else if ("stage1HumanTask1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
                
            } else if ("stage1HumanTask2".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
            } 
        }
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).hasSize(2);
        
        Task task = tasks.get(0);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Stage task 1");
        cmmnTaskService.complete(task.getId());
        task = tasks.get(1);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Task 1");
        cmmnTaskService.complete(task.getId());
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(1);
        
        task = tasks.get(0);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Stage task 2");
        cmmnTaskService.complete(task.getId());
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(0);
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }
    
    @Test
    void directMigrateAddTaskAndListenerToStageWithSentryCaseWithAutomaticPlanItemInstanceCreation() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/task-and-stage-with-sentry-start.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/task-and-stage-with-sentry-add-task-listener.cmmn.xml");

        CaseInstanceMigrationValidationResult validationResult = cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .validateMigration(caseInstance.getId());
        
        assertThat(validationResult.isMigrationValid()).isTrue();

        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .enableAutomaticPlanItemInstanceCreation()
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
        for (PlanItemInstance planItemInstance : planItemInstances) {
            assertThat(planItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            if ("humanTask1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            
            } else if ("expandedStage1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
            } 
        }
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(1);
        
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(0);
        
        Task task = tasks.get(0);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Task 1");
        cmmnTaskService.complete(task.getId());
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(4);
        
        for (PlanItemInstance planItemInstance : planItemInstances) {
            assertThat(planItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            if ("expandedStage1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            
            } else if ("stage1HumanTask1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            
            } else if ("stage1HumanTask2".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            
            } else if ("userEventListener2".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
            } 
        }
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(1);
        
        task = tasks.get(0);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Stage task 1");
        cmmnTaskService.complete(task.getId());
        
        task = tasks.get(1);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Stage task 2");
        cmmnTaskService.complete(task.getId());
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(0);
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }
    
    @Test
    void directMigrateAddAsyncTaskAndListenerToStageCaseWithAutomaticPlanItemInstanceCreation() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/task-and-stage-start.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/task-and-stage-add-async-task-listener.cmmn.xml");

        CaseInstanceMigrationValidationResult validationResult = cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .validateMigration(caseInstance.getId());
        
        assertThat(validationResult.isMigrationValid()).isTrue();

        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .enableAutomaticPlanItemInstanceCreation()
                .migrate(caseInstance.getId());

        // Assert
        CaseInstance caseInstanceAfterMigration = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(caseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(5);
        for (PlanItemInstance planItemInstance : planItemInstances) {
            assertThat(planItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            if ("humanTask1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            
            } else if ("expandedStage1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            
            } else if ("stage1HumanTask1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
                
            } else if ("stage1Task2".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ASYNC_ACTIVE);
            
            } else if ("stage1UserEventListener1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
            } 
        }
        
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).hasSize(2);
        
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(1);
        
        Task task = tasks.get(0);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Stage task 1");
        cmmnTaskService.complete(task.getId());
        
        task = tasks.get(1);
        assertThat(task.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(task.getName()).isEqualTo("Task 1");
        cmmnTaskService.complete(task.getId());
        
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).hasSize(0);
        
        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(3);
        for (PlanItemInstance planItemInstance : planItemInstances) {
            assertThat(planItemInstance.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
            if ("expandedStage1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
            
            } else if ("stage1Task2".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.ASYNC_ACTIVE);
            
            } else if ("stage1UserEventListener1".equals(planItemInstance.getPlanItemDefinitionId())) {
                assertThat(planItemInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
            } 
        }
        
        assertThat(CmmnJobTestHelper.areJobsAvailable(cmmnManagementService)).isTrue();
        CmmnJobTestHelper.waitForJobExecutorToProcessAllAsyncJobs(cmmnEngineConfiguration,5000, 200, true);
        assertThat(CmmnJobTestHelper.areJobsAvailable(cmmnManagementService)).isFalse();
        
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }
}
