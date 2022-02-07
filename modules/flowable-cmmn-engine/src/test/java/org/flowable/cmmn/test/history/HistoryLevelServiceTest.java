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
package org.flowable.cmmn.test.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.task.api.Task;
import org.junit.Test;

public class HistoryLevelServiceTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/history/testStartSimplePassthroughCase.cmmn")
    public void testDefaultHistoryLevelSTP() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("var1", "test")
                .variable("var2", 10)
                .start();

        CmmnHistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(cmmnEngineConfiguration, cmmnManagementService, 10000, 200);
        
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(4);
        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().count()).isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().count()).isEqualTo(2);
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/history/testStartSimplePassthroughCaseInstanceLevel.cmmn")
    public void testInstanceLevelSTP() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("var1", "test")
                .variable("var2", 10)
                .start();

        CmmnHistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(cmmnEngineConfiguration, cmmnManagementService, 10000, 200);
        
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().count()).isZero();
        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().count()).isZero();
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/history/testStartSimplePassthroughCaseCustomLevelPlanItems.cmmn")
    public void testCustomLevelPlanItemsSTP() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("var1", "test")
                .variable("var2", 10)
                .start();

        CmmnHistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(cmmnEngineConfiguration, cmmnManagementService, 10000, 200);
        
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(2);
        
        List<HistoricPlanItemInstance> planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().list();
        assertThat(planItemInstances)
            .extracting(HistoricPlanItemInstance::getPlanItemDefinitionId,
                    HistoricPlanItemInstance::getPlanItemDefinitionType)
            .containsOnly(
                    tuple("taskA", "task"),
                    tuple("mileStoneTwo", "milestone")
            );
        
        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().singleResult().getElementId()).isEqualTo("planItemMileStoneTwo");
        
        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().count()).isZero();
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/history/testTwoTaskCase.cmmn")
    public void testDefaultHistoryLevelTwoTasks() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("var1", "test")
                .variable("var2", 10)
                .start();
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        CmmnHistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(cmmnEngineConfiguration, cmmnManagementService, 10000, 200);
        
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(3);
        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().count()).isEqualTo(2);
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/history/testTwoTaskCaseInstanceLevel.cmmn")
    public void testInstanceLevelTwoTasks() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("var1", "test")
                .variable("var2", 10)
                .start();
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        CmmnHistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(cmmnEngineConfiguration, cmmnManagementService, 10000, 200);
        
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isZero();
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().count()).isZero();
        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().count()).isZero();
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/history/testTwoTaskCaseInstanceLevelPlanItems.cmmn")
    public void testInstanceLevelTwoTasksWithCustomPlanItems() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("var1", "test")
                .variable("var2", 10)
                .start();
        
        CmmnHistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(cmmnEngineConfiguration, cmmnManagementService, 10000, 200);
        
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isZero();
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(2);
        
        List<HistoricPlanItemInstance> planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().list();
        assertThat(planItemInstances)
            .extracting(HistoricPlanItemInstance::getPlanItemDefinitionId,
                    HistoricPlanItemInstance::getPlanItemDefinitionType,
                    HistoricPlanItemInstance::getState)
            .containsOnly(
                    tuple("taskB", "humantask", PlanItemInstanceState.AVAILABLE),
                    tuple("mileStoneOne", "milestone", PlanItemInstanceState.AVAILABLE)
            );
        
        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().count()).isZero();
        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().count()).isZero();
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        
        CmmnHistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(cmmnEngineConfiguration, cmmnManagementService, 10000, 200);
        
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isZero();
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(2);
        
        planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().list();
        assertThat(planItemInstances)
            .extracting(HistoricPlanItemInstance::getPlanItemDefinitionId,
                    HistoricPlanItemInstance::getPlanItemDefinitionType,
                    HistoricPlanItemInstance::getState)
            .containsOnly(
                    tuple("taskB", "humantask", PlanItemInstanceState.ACTIVE),
                    tuple("mileStoneOne", "milestone", PlanItemInstanceState.AVAILABLE)
            );
        
        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().count()).isZero();
        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().count()).isZero();
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        CmmnHistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(cmmnEngineConfiguration, cmmnManagementService, 10000, 200);
        
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isZero();
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(2);
        
        planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().list();
        assertThat(planItemInstances)
            .extracting(HistoricPlanItemInstance::getPlanItemDefinitionId,
                    HistoricPlanItemInstance::getPlanItemDefinitionType,
                    HistoricPlanItemInstance::getState)
            .containsOnly(
                    tuple("taskB", "humantask", PlanItemInstanceState.COMPLETED),
                    tuple("mileStoneOne", "milestone", PlanItemInstanceState.COMPLETED)
            );
        
        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().count()).isZero();
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/history/testTwoTaskCaseTaskLevel.cmmn")
    public void testTaskLevelTwoTasks() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("var1", "test")
                .variable("var2", 10)
                .start();
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        CmmnHistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(cmmnEngineConfiguration, cmmnManagementService, 10000, 200);
        
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isZero();
        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().count()).isZero();
        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().count()).isZero();
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/history/testTwoTaskCaseTaskLevelPlanItems.cmmn")
    public void testTaskLevelTwoTasksWithCustomPlanItems() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .variable("var1", "test")
                .variable("var2", 10)
                .start();
        
        CmmnHistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(cmmnEngineConfiguration, cmmnManagementService, 10000, 200);
        
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(2);
        
        List<HistoricPlanItemInstance> planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().list();
        assertThat(planItemInstances)
            .extracting(HistoricPlanItemInstance::getPlanItemDefinitionId,
                    HistoricPlanItemInstance::getPlanItemDefinitionType,
                    HistoricPlanItemInstance::getState)
            .containsOnly(
                    tuple("taskB", "humantask", PlanItemInstanceState.AVAILABLE),
                    tuple("mileStoneOne", "milestone", PlanItemInstanceState.AVAILABLE)
            );
        
        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().count()).isZero();
        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().count()).isZero();
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        
        CmmnHistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(cmmnEngineConfiguration, cmmnManagementService, 10000, 200);
        
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(2);
        
        planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().list();
        assertThat(planItemInstances)
            .extracting(HistoricPlanItemInstance::getPlanItemDefinitionId,
                    HistoricPlanItemInstance::getPlanItemDefinitionType,
                    HistoricPlanItemInstance::getState)
            .containsOnly(
                    tuple("taskB", "humantask", PlanItemInstanceState.ACTIVE),
                    tuple("mileStoneOne", "milestone", PlanItemInstanceState.AVAILABLE)
            );
        
        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().count()).isZero();
        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().count()).isZero();
        
        task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        CmmnHistoryTestHelper.waitForJobExecutorToProcessAllHistoryJobs(cmmnEngineConfiguration, cmmnManagementService, 10000, 200);
        
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().count()).isEqualTo(2);
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().count()).isEqualTo(2);
        
        planItemInstances = cmmnHistoryService.createHistoricPlanItemInstanceQuery().list();
        assertThat(planItemInstances)
            .extracting(HistoricPlanItemInstance::getPlanItemDefinitionId,
                    HistoricPlanItemInstance::getPlanItemDefinitionType,
                    HistoricPlanItemInstance::getState)
            .containsOnly(
                    tuple("taskB", "humantask", PlanItemInstanceState.COMPLETED),
                    tuple("mileStoneOne", "milestone", PlanItemInstanceState.COMPLETED)
            );
        
        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().count()).isZero();
    }
}
