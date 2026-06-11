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

import org.flowable.cmmn.api.CmmnMigrationService;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.engine.ProcessMigrationService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CaseInstanceMigrationHistoryLevelTest extends AbstractProcessEngineIntegrationTest {

	@Before
    public void enableDefinitionHistoryLevel() {
        processEngineConfiguration.setEnableProcessDefinitionHistoryLevel(true);
        cmmnEngineConfiguration.setEnableCaseDefinitionHistoryLevel(true);
    }

	@After
    public void disableDefinitionHistoryLevel() {
        processEngineConfiguration.setEnableProcessDefinitionHistoryLevel(false);
        cmmnEngineConfiguration.setEnableCaseDefinitionHistoryLevel(false);
    }

    @Test
    public void testMigrateCaseWithActiveProcessTaskFromHistoryNoneToDefault() {
        // Deploy BPMN process
        Deployment bpmnDeployment = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml")
                .deploy();

        // Deploy case v1 with history level NONE
        CmmnDeployment cmmnDeploymentV1 = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CaseInstanceMigrationHistoryLevelTest.processTaskHistoryNone.cmmn")
                .deploy();
        CaseDefinition caseDefinitionV1 = cmmnRepositoryService.createCaseDefinitionQuery()
                .deploymentId(cmmnDeploymentV1.getId())
                .singleResult();

        // Start case instance - process task becomes active
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionId(caseDefinitionV1.getId())
                .start();

        // Verify process instance is active
        ProcessInstance processInstance = processEngineRuntimeService.createProcessInstanceQuery()
                .singleResult();
        assertThat(processInstance).isNotNull();

        Task processTask = processEngineTaskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();
        assertThat(processTask).isNotNull();

        PlanItemInstance processTaskPlanItem = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceStateActive()
                .singleResult();
        assertThat(processTaskPlanItem).isNotNull();
        assertThat(processTaskPlanItem.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);

        // Deploy case v2 with default history level (engine level, which is typically AUDIT)
        CmmnDeployment cmmnDeploymentV2 = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CaseInstanceMigrationHistoryLevelTest.processTaskHistoryDefault.cmmn")
                .deploy();
        CaseDefinition caseDefinitionV2 = cmmnRepositoryService.createCaseDefinitionQuery()
                .deploymentId(cmmnDeploymentV2.getId())
                .singleResult();

        // Migrate case instance from v1 (history=none) to v2 (history=default)
        CmmnMigrationService cmmnMigrationService = cmmnEngineConfiguration.getCmmnMigrationService();
        cmmnMigrationService.createCaseInstanceMigrationBuilder()
                .migrateToCaseDefinition(caseDefinitionV2.getId())
                .migrate(caseInstance.getId());

        // Verify migration was successful
        CaseInstance migratedCaseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(migratedCaseInstance).isNotNull();
        assertThat(migratedCaseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionV2.getId());

        // Process instance should still be active
        ProcessInstance migratedProcessInstance = processEngineRuntimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();
        assertThat(migratedProcessInstance).isNotNull();

        // Complete the process task
        Task task = processEngineTaskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();
        assertThat(task).isNotNull();
        processEngineTaskService.complete(task.getId());

        // Case should be completed
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        // Verify historic instances exist after completion
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(1);
        assertThat(processEngineHistoryService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
    }

    @Test
    public void testMigrateProcessInstanceFromHistoryNoneToDefault() {
        // Deploy process v1 with history level NONE
        Deployment deploymentV1 = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CaseInstanceMigrationHistoryLevelTest.oneTaskProcessHistoryNone.bpmn20.xml")
                .deploy();
        ProcessDefinition processDefinitionV1 = processEngineRepositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentV1.getId())
                .singleResult();

        // Start process instance with history level NONE
        ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceById(processDefinitionV1.getId());

        Task task = processEngineTaskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();
        assertThat(task).isNotNull();

        // Deploy process v2 with default history level
        Deployment deploymentV2 = processEngineRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/CaseInstanceMigrationHistoryLevelTest.oneTaskProcessHistoryDefault.bpmn20.xml")
                .deploy();
        ProcessDefinition processDefinitionV2 = processEngineRepositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentV2.getId())
                .singleResult();

        // Migrate process instance from v1 (history=none) to v2 (history=default)
        ProcessMigrationService processMigrationService = processEngine.getProcessMigrationService();
        processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(processDefinitionV2.getId())
                .migrate(processInstance.getId());

        // Verify migration was successful
        ProcessInstance migratedProcessInstance = processEngineRuntimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();
        assertThat(migratedProcessInstance).isNotNull();
        assertThat(migratedProcessInstance.getProcessDefinitionId()).isEqualTo(processDefinitionV2.getId());

        // Complete the task
        task = processEngineTaskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();
        assertThat(task).isNotNull();
        processEngineTaskService.complete(task.getId());

        // Process should be completed
        assertThat(processEngineRuntimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();

        // Verify historic process instance exists after completion
        assertThat(processEngineHistoryService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
    }
}
