package org.flowable.cmmn.test.runtime;

import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.AVAILABLE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.COMPLETED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ENABLED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.UNAVAILABLE;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

/**
 * Adds testing around plan item completion evaluation.
 *
 * @author Micha Kiener
 */
public class PlanItemCompletionTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testManualActivatedTaskWithRepetition() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("requiredTaskWithRepetitionAndManualActivation").start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(3, planItemInstances.size());
        String[] expectedNames = new String[] { "Stage A", "Task A", "User Listener A" };
        String[] expectedStates = new String[] { ACTIVE, ENABLED, UNAVAILABLE };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
            assertEquals(expectedStates[i], planItemInstances.get(i).getState());
        }

        // activate task and complete it the first time will make the user listener available
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(1).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(1).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .orderByEndTime().asc()
            .includeEnded()
            .list();

        assertEquals(4, planItemInstances.size());
        expectedNames = new String[] { "Stage A", "Task A", "Task A", "User Listener A" };
        expectedStates = new String[] { ACTIVE, ENABLED, COMPLETED, AVAILABLE };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
            assertEquals(expectedStates[i], planItemInstances.get(i).getState());
        }

        // trigger user listener to complete stage
        cmmnRuntimeService.completeUserEventListenerInstance(planItemInstances.get(3).getId());

        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }

    @Test
    @CmmnDeployment
    public void testManualActivatedTaskWithRepetitionIgnoredAfterFirstCompletion() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("requiredTaskWithRepetitionAndManualActivation").start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(3, planItemInstances.size());
        String[] expectedNames = new String[] { "Stage A", "Task A", "User Listener A" };
        String[] expectedStates = new String[] { ACTIVE, ENABLED, UNAVAILABLE };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
            assertEquals(expectedStates[i], planItemInstances.get(i).getState());
        }

        // activate task and complete it the first time will complete the case as it will be ignored after first completion
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(1).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(1).getId());

        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }
}
