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
package org.flowable.cmmn.engine.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.cmmn.engine.test.impl.CmmnTestRunner;
import org.junit.After;
import org.junit.runner.RunWith;

/**
 * @author Joram Barrez
 */
@RunWith(CmmnTestRunner.class)
public abstract class AbstractFlowableCmmnTestCase {

    public static CmmnEngine cmmnEngine;

    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    protected CmmnManagementService cmmnManagementService;
    protected CmmnRepositoryService cmmnRepositoryService;
    protected CmmnRuntimeService cmmnRuntimeService;
    protected CmmnTaskService cmmnTaskService;
    protected CmmnHistoryService cmmnHistoryService;

    protected String deploymentId;

    @After
    public void cleanup() {
        if (deploymentId != null) {
           cmmnRepositoryService.deleteDeployment(deploymentId, true);
        }
    }

    protected void deployOneHumanTaskCaseModel() {
        deploymentId = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/one-human-task-model.cmmn")
                .deploy()
                .getId();
    }

    protected CaseInstance deployAndStartOneHumanTaskCaseModel() {
        deployOneHumanTaskCaseModel();
        return cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
    }

    protected void deployOneTaskCaseModel() {
        deploymentId = cmmnRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/one-task-model.cmmn")
                .deploy()
                .getId();
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
        long count = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count();
        assertEquals(createCaseInstanceEndedErrorMessage(caseInstance, count), 0, count);
        assertEquals("Runtime case instance found", 0, cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).finished().count());
    }

    protected String createCaseInstanceEndedErrorMessage(CaseInstance caseInstance, long count) {
        String errorMessage = "Plan item instances found for case instance: ";
        if (count != 0) {
            List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).list();
            String names = planItemInstances.stream()
                .map(planItemInstance -> planItemInstance.getName() + "(" + planItemInstance.getPlanItemDefinitionType() + ")")
                .collect(Collectors.joining(", "));
            errorMessage += names;
        }
        return errorMessage;
    }

    protected void assertCaseInstanceEnded(CaseInstance caseInstance, int nrOfExpectedMilestones) {
        assertCaseInstanceEnded(caseInstance);
        assertEquals(0, cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count());
        assertEquals(nrOfExpectedMilestones, cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count());
    }
    
    protected void assertCaseInstanceNotEnded(CaseInstance caseInstance) {
        assertTrue("Found no plan items for case instance", cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count() > 0);
        assertTrue("No runtime case instance found", cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count() > 0);
        assertNull("Historical case instance is already marked as ended", cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult().getEndTime());
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
            fail("No plan item instances found with name " + name);
        }

        assertEquals("Incorrect number of states found: " + planItemInstanceStates, states.length, planItemInstanceStates.size());
        List<String> originalStates = new ArrayList<>(planItemInstanceStates);
        for (String state : states) {
            assertTrue("State '" + state + "' not found in plan item instances states '" + originalStates + "'", planItemInstanceStates.remove(state));
        }
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

        assertEquals(itemVariableValues.size(), tasks.size());
        for (int ii = 0; ii < tasks.size(); ii++) {
            PlanItemInstance task = tasks.get(ii);

            Object itemValue = cmmnRuntimeService.getLocalVariable(task.getId(), "item");
            Object itemIndexValue = cmmnRuntimeService.getLocalVariable(task.getId(), "itemIndex");
            assertEquals(itemVariableValues.get(ii), itemValue);
            assertEquals(itemIndexVariableValues.get(ii), itemIndexValue);
        }
    }

    protected void completePlanItems(String caseInstanceId, String planItemName, int expectedCount, int numberToComplete) {
        // now let's complete all Tasks B -> nothing must happen additionally
        List<PlanItemInstance> tasks = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstanceId)
            .planItemInstanceName(planItemName)
            .planItemInstanceStateActive()
            .orderByCreateTime().asc()
            .list();

        assertEquals(expectedCount, tasks.size());
        assertTrue(numberToComplete <= expectedCount);
        int completedCount = 0;
        while (completedCount < numberToComplete) {
            cmmnRuntimeService.triggerPlanItemInstance(tasks.get(completedCount++).getId());
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
    
    protected void waitForAsyncHistoryExecutorToProcessAllJobs() {
        CmmnJobTestHelper.waitForAsyncHistoryExecutorToProcessAllJobs(cmmnEngineConfiguration, 20000L, 200L, true);
    }

}
