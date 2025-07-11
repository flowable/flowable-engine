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
import static org.assertj.core.api.Assertions.fail;

import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnMigrationService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.DynamicCmmnService;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.history.CmmnHistoryManager;
import org.flowable.cmmn.engine.impl.history.DefaultCmmnHistoryManager;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.cmmn.engine.test.impl.CmmnTestHelper;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.test.EnsureCleanDb;
import org.flowable.common.engine.impl.test.LoggingExtension;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Joram Barrez
 */
@ExtendWith(LoggingExtension.class)
@EnsureCleanDb(excludeTables = {
        "ACT_GE_PROPERTY",
        "ACT_ID_PROPERTY"
})
public abstract class AbstractFlowableCmmnTestCase {

    public CmmnEngine cmmnEngine;

    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    protected CmmnManagementService cmmnManagementService;
    protected CmmnRepositoryService cmmnRepositoryService;
    protected CmmnRuntimeService cmmnRuntimeService;
    protected DynamicCmmnService dynamicCmmnService;
    protected CmmnTaskService cmmnTaskService;
    protected CmmnHistoryService cmmnHistoryService;
    protected CmmnMigrationService cmmnMigrationService;

    protected Set<String> autoCleanupDeploymentIds = new HashSet<>();

    protected String addDeploymentForAutoCleanup(CmmnDeployment cmmnDeployment) {
        String deploymentId = cmmnDeployment.getId();
        addDeploymentForAutoCleanup(deploymentId);
        return deploymentId;
    }

    protected void addDeploymentForAutoCleanup(String deploymentId) {
        this.autoCleanupDeploymentIds.add(deploymentId);
    }

    @BeforeEach
    void init(CmmnEngine cmmnEngine) {
        this.cmmnEngine = cmmnEngine;
        this.cmmnEngineConfiguration = cmmnEngine.getCmmnEngineConfiguration();
        this.cmmnManagementService = cmmnEngine.getCmmnManagementService();
        this.cmmnRepositoryService = cmmnEngine.getCmmnRepositoryService();
        this.cmmnRuntimeService = cmmnEngine.getCmmnRuntimeService();
        this.dynamicCmmnService = cmmnEngine.getDynamicCmmnService();
        this.cmmnTaskService = cmmnEngine.getCmmnTaskService();
        this.cmmnHistoryService = cmmnEngine.getCmmnHistoryService();
        this.cmmnMigrationService = cmmnEngine.getCmmnMigrationService();
    }

    @AfterEach
    void baseCleanup() {
        if (autoCleanupDeploymentIds != null && !autoCleanupDeploymentIds.isEmpty()) {
            for (String deploymentId : autoCleanupDeploymentIds) {
                CmmnTestHelper.deleteDeployment(cmmnEngineConfiguration, deploymentId);
            }
        }
        autoCleanupDeploymentIds = new HashSet<>();
    }

    protected void deployOneHumanTaskCaseModel() {
        addDeploymentForAutoCleanup(cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/one-human-task-model.cmmn")
                .deploy()
        );
    }

    protected CaseInstance deployAndStartOneHumanTaskCaseModel() {
        deployOneHumanTaskCaseModel();
        return cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
    }

    protected void deployOneTaskCaseModel() {
        addDeploymentForAutoCleanup(cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/one-task-model.cmmn")
                .deploy());
    }

    protected CaseDefinition deployCaseDefinition(String name, String path) {
        CmmnDeployment deployment = cmmnRepositoryService.createDeployment()
            .name(name)
            .addClasspathResource(path)
            .deploy();

        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery()
            .deploymentId(deployment.getId()).singleResult();

        return caseDefinition;
    }

    protected void deleteDeployment(String deploymentId) {
        boolean isAsyncHistoryEnabled = cmmnEngineConfiguration.isAsyncHistoryEnabled();
        CmmnHistoryManager asyncHistoryManager = null;
        if (isAsyncHistoryEnabled) {
            cmmnEngineConfiguration.setAsyncHistoryEnabled(false);
            asyncHistoryManager = cmmnEngineConfiguration.getCmmnHistoryManager();
            cmmnEngineConfiguration.setCmmnHistoryManager(new DefaultCmmnHistoryManager(cmmnEngineConfiguration));
        }

        cmmnRepositoryService.deleteDeployment(deploymentId, true);

        if (isAsyncHistoryEnabled) {
            cmmnEngineConfiguration.setAsyncHistoryEnabled(true);
            cmmnEngineConfiguration.setCmmnHistoryManager(asyncHistoryManager);
        }
    }
    
    protected Date setClockFixedToCurrentTime() {
        // SQL Server rounds the milliseconds, on order to be stable we set them to 0
        Date date = Date.from(Instant.now().with(ChronoField.MILLI_OF_SECOND, 0));
        cmmnEngineConfiguration.getClock().setCurrentTime(date);
        return date;
    }

    protected void setClockTo(long epochTime) {
        setClockTo(new Date(epochTime));
    }
    
    protected void setClockTo(Date date) {
        cmmnEngineConfiguration.getClock().setCurrentTime(date);
    }

    protected Date forwardClock(long milliseconds) {
        long currentMillis = cmmnEngineConfiguration.getClock().getCurrentTime().getTime();
        Date date = new Date(currentMillis + milliseconds);
        cmmnEngineConfiguration.getClock().setCurrentTime(date);
        return date;
    }

    protected void assertCaseInstanceEnded(CaseInstance caseInstance) {
        assertCaseInstanceEnded(caseInstance.getId());
    }

    protected void assertCaseInstanceEnded(String caseInstanceId) {
        long count = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstanceId).count();
        assertThat(count)
                .withFailMessage(() -> createCaseInstanceEndedErrorMessage(caseInstanceId, count))
                .isZero();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstanceId).count())
                .withFailMessage("Runtime case instance found")
                .isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstanceId).finished().count())
                    .isEqualTo(1);
        }
    }

    protected String createCaseInstanceEndedErrorMessage(CaseInstance caseInstance, long count) {
        return createCaseInstanceEndedErrorMessage(caseInstance.getId(), count);
    }

    protected String createCaseInstanceEndedErrorMessage(String caseInstanceId, long count) {
        String errorMessage = "Plan item instances found for case instance: ";
        if (count != 0) {
            List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstanceId).list();
            String names = planItemInstances.stream()
                .map(planItemInstance -> planItemInstance.getName() + "(" + planItemInstance.getPlanItemDefinitionType() + ")")
                .collect(Collectors.joining(", "));
            errorMessage += names;
        }
        return errorMessage;
    }

    protected void assertCaseInstanceEnded(CaseInstance caseInstance, int nrOfExpectedMilestones) {
        assertCaseInstanceEnded(caseInstance);
        assertThat(cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count())
                .isZero();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count())
                    .isEqualTo(nrOfExpectedMilestones);
        }
    }
    
    protected void assertCaseInstanceNotEnded(CaseInstance caseInstance) {
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count())
                .withFailMessage("Found no plan items for case instance")
                .isPositive();

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count())
                .withFailMessage("No runtime case instance found")
                .isPositive();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getEndTime())
                    .withFailMessage("Historical case instance is already marked as ended")
                    .isNull();
        }
    }

    protected void assertSingleTaskExists(List<Task> tasks, String name) {
        List<String> taskIds = tasks.stream()
            .filter(task -> Objects.equals(name, task.getName()))
            .map(Task::getId)
            .collect(Collectors.toList());

        assertThat(taskIds).hasSize(1);
    }

    protected void assertSamePlanItemState(CaseInstance c1) {
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<PlanItemInstance> runtimePlanItems = getAllPlanItemInstances(c1.getId());
            List<HistoricPlanItemInstance> historicPlanItems = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(c1.getId()).list();

            assertThat(historicPlanItems).hasSameSizeAs(runtimePlanItems);

            Map<String, HistoricPlanItemInstance> historyMap = new HashMap<>(historicPlanItems.size());
            for (HistoricPlanItemInstance historicPlanItem : historicPlanItems) {
                historyMap.put(historicPlanItem.getId(), historicPlanItem);
            }

            for (PlanItemInstance runtimePlanItem : runtimePlanItems) {
                HistoricPlanItemInstance historicPlanItemInstance = historyMap.remove(runtimePlanItem.getId());
                assertThat(historicPlanItemInstance).isNotNull();
                assertThat(historicPlanItemInstance.getState()).isEqualTo(runtimePlanItem.getState());
            }

            assertThat(historyMap).isEmpty();
        }
    }

    protected void assertPlanItemInstanceState(CaseInstance caseInstance, String name, String ... states) {
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName()
            .asc()
            .includeEnded()
            .list();
        assertPlanItemInstanceState(planItemInstances, name, states);
    }

    protected void assertPlanItemInstanceState(List<PlanItemInstance> planItemInstances, String name, String ... states) {
        List<String> planItemInstanceStates = planItemInstances.stream()
            .filter(planItemInstance -> Objects.equals(name, planItemInstance.getName()))
            .map(PlanItemInstance::getState)
            .collect(Collectors.toList());

        if (planItemInstanceStates.isEmpty()) {
            List<String> planItemInstanceNames = planItemInstances.stream().map(PlanItemInstance::getName).collect(Collectors.toList());
            fail("No plan item instances found with name " + name + ", following names were found:" + String.join(",", planItemInstanceNames));
        }

        assertThat(planItemInstanceStates).containsExactlyInAnyOrder(states);
    }

    protected void assertHistoricPlanItemInstanceState(List<HistoricPlanItemInstance> planItemInstances, String name, String ... states) {
        List<String> planItemInstanceStates = planItemInstances.stream()
            .filter(planItemInstance -> Objects.equals(name, planItemInstance.getName()))
            .map(HistoricPlanItemInstance::getState)
            .collect(Collectors.toList());

        if (planItemInstanceStates.isEmpty()) {
            fail("No historic plan item instances found with name " + name);
        }

        assertThat(planItemInstanceStates).containsExactlyInAnyOrder(states);
    }

    protected void assertNoPlanItemInstance(List<PlanItemInstance> planItemInstances, String name) {
        List<String> planItemInstanceStates = planItemInstances.stream()
            .filter(planItemInstance -> Objects.equals(name, planItemInstance.getName()))
            .map(PlanItemInstance::getState)
            .collect(Collectors.toList());

        if (!planItemInstanceStates.isEmpty()) {
            fail(planItemInstanceStates.size() + " plan item instance(s) found with name " + name + ", but should be 0");
        }
    }

    protected void assertPlanItemLocalVariables(String caseInstanceId, String planItemName, List<?> itemVariableValues, List<Integer> itemIndexVariableValues) {
        List<PlanItemInstance> tasks = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstanceId)
            .planItemInstanceName(planItemName)
            .planItemInstanceStateActive()
            .orderByCreateTime().asc()
            .list();

        assertThat(tasks).hasSameSizeAs(itemVariableValues);

        List<Object> itemValues = new ArrayList<>(tasks.size());
        List<Object> itemIndexValues = new ArrayList<>(tasks.size());

        for (PlanItemInstance task : tasks) {
            itemValues.add(cmmnRuntimeService.getLocalVariable(task.getId(), "item"));
            itemIndexValues.add(cmmnRuntimeService.getLocalVariable(task.getId(), "itemIndex"));
        }

        for (int ii = 0; ii < itemVariableValues.size(); ii++) {
            int index = searchForMatch(itemVariableValues.get(ii), itemIndexVariableValues.get(ii), itemValues, itemIndexValues);
            if (index == -1) {
              fail("Could not find local variable value '" + itemVariableValues.get(ii) + "' with index value '" + itemIndexVariableValues.get(ii) + "'.");
            }
            itemValues.remove(index);
            itemIndexValues.remove(index);
        }
    }

    protected int searchForMatch(Object itemValue, Integer index, List<Object> itemValues, List<Object> itemIndexValues) {
        for (int ii = 0; ii < itemValues.size(); ii++) {
            if (itemValues.get(ii).equals(itemValue) && itemIndexValues.get(ii).equals(index)) {
                return ii;
            }
        }
        return -1;
    }

    protected void completePlanItemsWithItemValues(String caseInstanceId, String planItemName, int expectedTotalCount, Object... itemValues) {
        List<PlanItemInstance> tasks = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstanceId)
            .planItemInstanceName(planItemName)
            .planItemInstanceStateActive()
            .orderByCreateTime().asc()
            .list();

        assertThat(tasks).hasSize(expectedTotalCount);
        assertThat(itemValues).isNotNull();
        assertThat(itemValues.length).isLessThanOrEqualTo(expectedTotalCount);

        for (Object itemValue : itemValues) {
            if (!completePlanItemWithItemValue(tasks, itemValue)) {
                fail("Could not find plan item instance with 'item' value of '" + itemValue + "'");
            }
        }
    }

    protected boolean completePlanItemWithItemValue(List<PlanItemInstance> planItemInstances, Object itemValue) {
        for (PlanItemInstance planItemInstance : planItemInstances) {
            Object itemValueVar = cmmnRuntimeService.getLocalVariable(planItemInstance.getId(), "item");
            if (itemValue.equals(itemValueVar)) {
                cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
                return true;
            }
        }
        return false;
    }

    protected void completeAllPlanItems(String caseInstanceId, String planItemName, int expectedCount) {
        List<PlanItemInstance> tasks = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstanceId)
            .planItemInstanceName(planItemName)
            .planItemInstanceStateActive()
            .orderByCreateTime().asc()
            .list();

        assertThat(tasks).hasSize(expectedCount);
        for (PlanItemInstance task : tasks) {
            cmmnRuntimeService.triggerPlanItemInstance(task.getId());
        }
    }

    protected List<PlanItemInstance> getAllPlanItemInstances(String caseInstanceId) {
        return cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstanceId)
            .orderByName().asc()
            .orderByEndTime().asc()
            .includeEnded()
            .list();
    }

    protected List<PlanItemInstance> getPlanItemInstances(String caseInstanceId) {
        return cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstanceId)
            .orderByName().asc()
            .list();
    }

    protected List<PlanItemInstance> getCompletedPlanItemInstances(String caseInstanceId) {
        return cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstanceId)
            .planItemInstanceStateCompleted()
            .includeEnded()
            .orderByName().asc()
            .list();
    }

    protected List<PlanItemInstance> getTerminatedPlanItemInstances(String caseInstanceId) {
        return cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstanceId)
            .orderByName().asc()
            .planItemInstanceStateTerminated()
            .includeEnded()
            .list();
    }

    protected String getPlanItemInstanceIdByName(List<PlanItemInstance> planItemInstances, String name) {
        return getPlanItemInstanceByName(planItemInstances, name, null).getId();
    }

    protected String getPlanItemInstanceIdByNameAndState(List<PlanItemInstance> planItemInstances, String name, String state) {
        return getPlanItemInstanceByName(planItemInstances, name, state).getId();
    }

    protected PlanItemInstance getPlanItemInstanceByName(List<PlanItemInstance> planItemInstances, String name, String state) {
        List<PlanItemInstance> matchingPlanItemInstances = planItemInstances.stream()
            .filter(planItemInstance -> Objects.equals(name, planItemInstance.getName()))
            .filter(planItemInstance -> state != null ? Objects.equals(state, planItemInstance.getState()) : true)
            .collect(Collectors.toList());

        if (matchingPlanItemInstances.isEmpty()) {
            fail("No plan item instances found with name " + name);
        }

        if (matchingPlanItemInstances.size() > 1) {
            fail("Found " + matchingPlanItemInstances.size() + " plan item instances with name " + name);
        }

        return matchingPlanItemInstances.get(0);
    }

    protected void waitForJobExecutorToProcessAllJobs() {
        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 20000L, 200L, true);
    }

    protected void waitForJobExecutorToProcessAllAsyncJobs() {
        CmmnJobTestHelper.waitForJobExecutorToProcessAllAsyncJobs(cmmnEngineConfiguration, 20000L, 200L, true);
    }
    
    protected void waitForAsyncHistoryExecutorToProcessAllJobs() {
        if (cmmnEngineConfiguration.isAsyncHistoryEnabled()) {
            CmmnJobTestHelper.waitForAsyncHistoryExecutorToProcessAllJobs(cmmnEngineConfiguration, 20000L, 200L, true);
        }
    }

    protected void waitForJobExecutorOnCondition(Callable<Boolean> predicate) {
        CmmnJobTestHelper.waitForJobExecutorOnCondition(cmmnEngineConfiguration, 20000L, 200L, predicate);
    }

}
