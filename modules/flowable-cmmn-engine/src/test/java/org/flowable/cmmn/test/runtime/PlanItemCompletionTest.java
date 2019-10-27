package org.flowable.cmmn.test.runtime;

import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ACTIVE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.AVAILABLE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.COMPLETED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.ENABLED;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.UNAVAILABLE;
import static org.flowable.cmmn.api.runtime.PlanItemInstanceState.WAITING_FOR_REPETITION;
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

    @Test
    @CmmnDeployment
    public void testNestedComplexCompletion() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testNestingPlanItems").start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(7, planItemInstances.size());
        String[] expectedNames = new String[] { "Stage A", "Stage B", "Task A", "Task B", "Task C", "Task D", "Task E" };
        String[] expectedStates = new String[] { ACTIVE, ACTIVE, ENABLED, ENABLED, ACTIVE, ENABLED, ENABLED };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
            assertEquals(expectedStates[i], planItemInstances.get(i).getState());
        }

        // start and complete Task A -> nothing yet to happen
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(2).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(2).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(6, planItemInstances.size());
        expectedNames = new String[] { "Stage A", "Stage B", "Task B", "Task C", "Task D", "Task E" };
        expectedStates = new String[] { ACTIVE, ACTIVE, ENABLED, ACTIVE, ENABLED, ENABLED };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
            assertEquals(expectedStates[i], planItemInstances.get(i).getState());
        }

        // complete Task C -> nothing yet to happen
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(5, planItemInstances.size());
        expectedNames = new String[] { "Stage A", "Stage B", "Task B", "Task D", "Task E" };
        expectedStates = new String[] { ACTIVE, ACTIVE, ENABLED, ENABLED, ENABLED };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
            assertEquals(expectedStates[i], planItemInstances.get(i).getState());
        }

        // start Task B -> nothing yet to happen
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(2).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(5, planItemInstances.size());
        expectedNames = new String[] { "Stage A", "Stage B", "Task B", "Task D", "Task E" };
        expectedStates = new String[] { ACTIVE, ACTIVE, ACTIVE, ENABLED, ENABLED };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
            assertEquals(expectedStates[i], planItemInstances.get(i).getState());
        }

        // complete Task B -> Stage B and then Stage A need to complete
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(2).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(3, planItemInstances.size());
        expectedNames = new String[] { "Task B", "Task D", "Task E" };
        expectedStates = new String[] { ENABLED, ENABLED, ENABLED };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
            assertEquals(expectedStates[i], planItemInstances.get(i).getState());
        }

        // start and complete Task E -> case must be completed, as Task E is ignored after first completion
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(2).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(2).getId());

        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/CMMN/test/runtime/PlanItemCompletionTest.testNestedComplexCompletion.cmmn" })
    public void testNestedComplexCompletionAlternatePath() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testNestingPlanItems").start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(7, planItemInstances.size());
        String[] expectedNames = new String[] { "Stage A", "Stage B", "Task A", "Task B", "Task C", "Task D", "Task E" };
        String[] expectedStates = new String[] { ACTIVE, ACTIVE, ENABLED, ENABLED, ACTIVE, ENABLED, ENABLED };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
            assertEquals(expectedStates[i], planItemInstances.get(i).getState());
        }


        // start Task D and E -> nothing yet to happen
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(5).getId());
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(6).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(7, planItemInstances.size());
        expectedNames = new String[] { "Stage A", "Stage B", "Task A", "Task B", "Task C", "Task D", "Task E" };
        expectedStates = new String[] { ACTIVE, ACTIVE, ENABLED, ENABLED, ACTIVE, ACTIVE, ACTIVE };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
            assertEquals(expectedStates[i], planItemInstances.get(i).getState());
        }

        // complete Task C and Task D -> still nothing yet to happen
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(4).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(5).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(5, planItemInstances.size());
        expectedNames = new String[] { "Stage A", "Stage B", "Task A", "Task B", "Task E" };
        expectedStates = new String[] { ACTIVE, ACTIVE, ENABLED, ENABLED, ACTIVE };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
            assertEquals(expectedStates[i], planItemInstances.get(i).getState());
        }

        // start and complete Task A -> nothing yet to happen
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(2).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(2).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(4, planItemInstances.size());
        expectedNames = new String[] { "Stage A", "Stage B", "Task B", "Task E" };
        expectedStates = new String[] { ACTIVE, ACTIVE, ENABLED, ACTIVE };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
            assertEquals(expectedStates[i], planItemInstances.get(i).getState());
        }

        // start Task B -> nothing yet to happen
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(2).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(4, planItemInstances.size());
        expectedNames = new String[] { "Stage A", "Stage B", "Task B", "Task E" };
        expectedStates = new String[] { ACTIVE, ACTIVE, ACTIVE, ACTIVE };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
            assertEquals(expectedStates[i], planItemInstances.get(i).getState());
        }

        // complete Task E -> nothing further changes
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(4, planItemInstances.size());
        expectedNames = new String[] { "Stage A", "Stage B", "Task B", "Task E" };
        expectedStates = new String[] { ACTIVE, ACTIVE, ACTIVE, ENABLED };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
            assertEquals(expectedStates[i], planItemInstances.get(i).getState());
        }

        // complete Task B -> case must be completed now
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(2).getId());

        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }

    @Test
    @CmmnDeployment
    public void testComplexCompletionWithoutAutocompletion() {
        CaseInstance caseInstance = runComplexCompletionTestScenario(false);

        // because autocompletion is off, we still stay in Stage A
        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(4, planItemInstances.size());
        String[] expectedNames = new String[] { "Stage A", "Task D", "Task E", "Task G" };
        String[] expectedStates = new String[] { ACTIVE, WAITING_FOR_REPETITION, AVAILABLE, ENABLED };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
            assertEquals(expectedStates[i], planItemInstances.get(i).getState());
        }
    }

    @Test
    @CmmnDeployment
    public void testComplexCompletionWithAutocompletion() {
        CaseInstance caseInstance = runComplexCompletionTestScenario(true);

        // because autocompletion is on, the case will be completed
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(0, cmmnRuntimeService.createCaseInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().finished().count());
    }

    protected CaseInstance runComplexCompletionTestScenario(boolean autocompleteEnabled) {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("testCompletionWithConditions")
            .variable("autocompleteEnabled", autocompleteEnabled)
            .start();

        List<PlanItemInstance> planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(9, planItemInstances.size());
        String[] expectedNames = new String[] { "Stage A", "Task A", "Task B", "Task C", "Task D", "Task E", "Task F", "Task G", "Task H" };
        String[] expectedStates = new String[] { ACTIVE, ACTIVE, AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE, AVAILABLE, ENABLED, ENABLED };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
            assertEquals(expectedStates[i], planItemInstances.get(i).getState());
        }

        // complete Task A -> will enable B and D, C stays in available as it has a condition
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(1).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(9, planItemInstances.size());
        expectedNames = new String[] { "Stage A", "Task B", "Task C", "Task D", "Task D", "Task E", "Task F", "Task G", "Task H" };
        expectedStates = new String[] { ACTIVE, ENABLED, AVAILABLE, ENABLED, WAITING_FOR_REPETITION, AVAILABLE, AVAILABLE, ENABLED, ENABLED };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
            assertEquals(expectedStates[i], planItemInstances.get(i).getState());
        }

        // activate Task C by setting the flag making its condition true
        cmmnRuntimeService.setVariable(caseInstance.getId(), "activateTaskC", true);

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(9, planItemInstances.size());
        expectedNames = new String[] { "Stage A", "Task B", "Task C", "Task D", "Task D", "Task E", "Task F", "Task G", "Task H" };
        expectedStates = new String[] { ACTIVE, ENABLED, ACTIVE, ENABLED, WAITING_FOR_REPETITION, AVAILABLE, AVAILABLE, ENABLED, ENABLED };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
            assertEquals(expectedStates[i], planItemInstances.get(i).getState());
        }

        // start Task B and D and complete C
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(1).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(2).getId());
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(3).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(8, planItemInstances.size());
        expectedNames = new String[] { "Stage A", "Task B", "Task D", "Task D", "Task E", "Task F", "Task G", "Task H" };
        expectedStates = new String[] { ACTIVE, ACTIVE, ACTIVE, WAITING_FOR_REPETITION, AVAILABLE, AVAILABLE, ENABLED, ENABLED };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
            assertEquals(expectedStates[i], planItemInstances.get(i).getState());
        }

        // complete Task B and D
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(1).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(2).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(6, planItemInstances.size());
        expectedNames = new String[] { "Stage A", "Task D", "Task E", "Task F", "Task G", "Task H" };
        expectedStates = new String[] { ACTIVE, WAITING_FOR_REPETITION, AVAILABLE, AVAILABLE, ENABLED, ENABLED };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
            assertEquals(expectedStates[i], planItemInstances.get(i).getState());
        }

        // start and complete Task H
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(5).getId());
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(5).getId());

        // enable Task F through making its condition true and then start it
        cmmnRuntimeService.setVariable(caseInstance.getId(), "enableTaskF", true);
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(3).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(5, planItemInstances.size());
        expectedNames = new String[] { "Stage A", "Task D", "Task E", "Task F", "Task G" };
        expectedStates = new String[] { ACTIVE, WAITING_FOR_REPETITION, AVAILABLE, ACTIVE, ENABLED };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
            assertEquals(expectedStates[i], planItemInstances.get(i).getState());
        }

        // complete Task F and start Task G
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());
        cmmnRuntimeService.startPlanItemInstance(planItemInstances.get(4).getId());

        planItemInstances = cmmnRuntimeService.createPlanItemInstanceQuery()
            .caseInstanceId(caseInstance.getId())
            .orderByName().asc()
            .list();

        assertEquals(4, planItemInstances.size());
        expectedNames = new String[] { "Stage A", "Task D", "Task E", "Task G" };
        expectedStates = new String[] { ACTIVE, WAITING_FOR_REPETITION, AVAILABLE, ACTIVE };
        for (int i = 0; i < planItemInstances.size(); i++) {
            assertEquals(expectedNames[i], planItemInstances.get(i).getName());
            assertEquals(expectedStates[i], planItemInstances.get(i).getState());
        }

        // complete Task G and depending on autocompletion being on or off, we stay in Stage A or the case will be completed
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstances.get(3).getId());

        return caseInstance;
    }
}
