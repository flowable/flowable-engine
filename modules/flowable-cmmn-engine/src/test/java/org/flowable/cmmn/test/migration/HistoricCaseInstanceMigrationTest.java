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

import java.util.List;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;

public class HistoricCaseInstanceMigrationTest extends AbstractCaseMigrationTest {

    @Test
    void withSimpleOneTaskCase() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        // Act
        cmmnMigrationService.createHistoricCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .migrate(caseInstance.getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            // Assert
            HistoricCaseInstance historicCaseInstanceAfterMigration = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .singleResult();
            
            assertHistoricCaseInstanceValues(historicCaseInstanceAfterMigration, caseInstance, destinationDefinition);
        }
    }
    
    @Test
    void withRunningOneTaskCase() {
        // Arrange
        deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");

        // Act
        assertThatThrownBy(() -> {
            cmmnMigrationService.createHistoricCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .migrate(caseInstance.getId());
        }).isInstanceOf(FlowableException.class).hasMessageContaining("Historic case instance has not ended");
    }
    
    @Test
    void migrateMultipleOneTaskCasesWithCaseDefinitionId() {
        // Arrange
        CaseDefinition originalCaseDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseInstance caseInstance3 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance2.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        // Act
        cmmnMigrationService.createHistoricCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .migrateHistoricCaseInstances(originalCaseDefinition.getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            // Assert
            HistoricCaseInstance historicCaseInstanceAfterMigration = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .singleResult();
            
            assertHistoricCaseInstanceValues(historicCaseInstanceAfterMigration, caseInstance, destinationDefinition);
            
            historicCaseInstanceAfterMigration = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .caseInstanceId(caseInstance2.getId())
                    .singleResult();
            
            assertHistoricCaseInstanceValues(historicCaseInstanceAfterMigration, caseInstance2, destinationDefinition);
            
            historicCaseInstanceAfterMigration = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .caseInstanceId(caseInstance3.getId())
                    .singleResult();
            
            assertThat(historicCaseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(originalCaseDefinition.getId());
        }
    }
    
    @Test
    void migrateMultipleOneTaskCasesWithCaseDefinitionKey() {
        // Arrange
        CaseDefinition originalCaseDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/one-task.cmmn.xml");
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseInstance caseInstance3 = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCase").start();
        CaseDefinition destinationDefinition = deployCaseDefinition("test1", "org/flowable/cmmn/test/migration/two-task.cmmn.xml");
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance2.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        // Act
        cmmnMigrationService.createHistoricCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(destinationDefinition.getId())
                .migrateHistoricCaseInstances(originalCaseDefinition.getKey(), 1, "");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            // Assert
            HistoricCaseInstance historicCaseInstanceAfterMigration = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .singleResult();
            
            assertHistoricCaseInstanceValues(historicCaseInstanceAfterMigration, caseInstance, destinationDefinition);
            
            historicCaseInstanceAfterMigration = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .caseInstanceId(caseInstance2.getId())
                    .singleResult();
            
            assertHistoricCaseInstanceValues(historicCaseInstanceAfterMigration, caseInstance2, destinationDefinition);
            
            historicCaseInstanceAfterMigration = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .caseInstanceId(caseInstance3.getId())
                    .singleResult();
            
            assertThat(historicCaseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(originalCaseDefinition.getId());
        }
    }
    
    protected void assertHistoricCaseInstanceValues(HistoricCaseInstance historicCaseInstanceAfterMigration, CaseInstance caseInstance, CaseDefinition destinationDefinition) {
        assertThat(historicCaseInstanceAfterMigration.getCaseDefinitionId()).isEqualTo(destinationDefinition.getId());
        assertThat(historicCaseInstanceAfterMigration.getCaseDefinitionKey()).isEqualTo("testCase");
        assertThat(historicCaseInstanceAfterMigration.getCaseDefinitionName()).isEqualTo("Two Task Test Case");
        assertThat(historicCaseInstanceAfterMigration.getCaseDefinitionVersion()).isEqualTo(2);
        assertThat(historicCaseInstanceAfterMigration.getCaseDefinitionDeploymentId()).isEqualTo(destinationDefinition.getDeploymentId());
        List<HistoricPlanItemInstance> planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId())
                .list();
        assertThat(planItemInstances).hasSize(1);
        assertThat(planItemInstances)
                .extracting(HistoricPlanItemInstance::getCaseDefinitionId)
                .containsOnly(destinationDefinition.getId());
        assertThat(planItemInstances)
                .extracting(HistoricPlanItemInstance::getName)
                .containsExactlyInAnyOrder("Task 1");
        assertThat(planItemInstances)
                .extracting(HistoricPlanItemInstance::getState)
                .containsOnly(PlanItemInstanceState.COMPLETED);
    
        List<HistoricTaskInstance> historicTasks = cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(historicTasks).hasSize(1);
        for (HistoricTaskInstance historicTask : historicTasks) {
            assertThat(historicTask.getScopeDefinitionId()).isEqualTo(destinationDefinition.getId());
        }
    }

}
