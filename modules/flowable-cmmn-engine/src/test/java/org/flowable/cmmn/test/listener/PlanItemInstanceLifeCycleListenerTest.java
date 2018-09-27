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
package org.flowable.cmmn.test.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.List;

import org.flowable.cmmn.api.listener.PlanItemInstanceLifeCycleListener;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.task.api.Task;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class PlanItemInstanceLifeCycleListenerTest extends FlowableCmmnTestCase {

    private List<PlanItemInstanceLifeCycleListener> originalLifeCycleListeners;
    private String deploymentId;

    private AbstractTestLifeCycleListener testLifeCycleListener;

    @Before
    public void addListeners() {
        this.originalLifeCycleListeners = cmmnEngineConfiguration.getPlanItemInstanceLifeCycleListeners();

        this.deploymentId = cmmnRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/listener/PlanItemInstanceLifeCycleListenerTest.cmmn")
            .deploy()
            .getId();
    }

    @After
    public void removeListeners() {
        cmmnEngineConfiguration.setPlanItemInstanceLifeCycleListeners(originalLifeCycleListeners);
        cmmnRepositoryService.deleteDeployment(deploymentId, true);
    }

    @Test
    public void testReceiveAllLifeCycleEvents() {
        setTestLifeCycleListener(new TestReceiveAllLifeCycleListener());

        // Start case instance
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testLifeCycleListener").start();

        List<TestLifeCycleEvent> events = testLifeCycleListener.getEvents();
        assertEquals(7, events.size());

        assertEvent(events.get(0), "Stage one", null, PlanItemInstanceState.AVAILABLE);
        assertEvent(events.get(1), "Stage two", null, PlanItemInstanceState.AVAILABLE);

        assertEvent(events.get(2), "Stage one", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);

        assertEvent(events.get(3), "A", null, PlanItemInstanceState.AVAILABLE);
        assertEvent(events.get(4), "B", null, PlanItemInstanceState.AVAILABLE);

        assertEvent(events.get(5), "A", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        assertEvent(events.get(6), "B", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ENABLED);

        testLifeCycleListener.clear();

        // Disable B
        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.disablePlanItemInstance(planItemInstanceB.getId());

        events = testLifeCycleListener.getEvents();
        assertEquals(1, events.size());
        assertEvent(events.get(0), "B", PlanItemInstanceState.ENABLED, PlanItemInstanceState.DISABLED);

        testLifeCycleListener.clear();

        // Enable B
        cmmnRuntimeService.enablePlanItemInstance(planItemInstanceB.getId());

        events = testLifeCycleListener.getEvents();
        assertEquals(1, events.size());
        assertEvent(events.get(0), "B", PlanItemInstanceState.DISABLED, PlanItemInstanceState.ENABLED);

        testLifeCycleListener.clear();

        // Start B
        planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.startPlanItemInstance(planItemInstanceB.getId());

        events = testLifeCycleListener.getEvents();
        assertEquals(1, events.size());
        assertEvent(events.get(0), "B", PlanItemInstanceState.ENABLED, PlanItemInstanceState.ACTIVE);

        testLifeCycleListener.clear();

        // Complete A and B
        for (Task task : cmmnTaskService.createTaskQuery().orderByTaskName().asc().list()) {
            cmmnTaskService.complete(task.getId());
        }

        events = testLifeCycleListener.getEvents();
        assertEquals(10, events.size());

        assertEvent(events.get(0), "A", PlanItemInstanceState.ACTIVE, PlanItemInstanceState.COMPLETED);
        assertEvent(events.get(1), "B", PlanItemInstanceState.ACTIVE, PlanItemInstanceState.COMPLETED);
        assertEvent(events.get(2), "Stage one", PlanItemInstanceState.ACTIVE, PlanItemInstanceState.COMPLETED);

        assertEvent(events.get(3), "Stage two", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);

        assertEvent(events.get(4), "timer", null, PlanItemInstanceState.AVAILABLE);
        assertEvent(events.get(5), "M1", null, PlanItemInstanceState.AVAILABLE);
        assertEvent(events.get(6), "C", null, PlanItemInstanceState.AVAILABLE);

        assertEvent(events.get(8), "C", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        assertEvent(events.get(7), "M1", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        assertEvent(events.get(9), "M1", PlanItemInstanceState.ACTIVE, PlanItemInstanceState.COMPLETED);
    }

    @Test
    public void testFilterOnType() {
        setTestLifeCycleListener(new TestFilterTypesLifeCycleListener());

        // Start case instance
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testLifeCycleListener").start();

        List<TestLifeCycleEvent> events = testLifeCycleListener.getEvents();
        assertEquals(4, events.size());

        assertEvent(events.get(0), "A", null, PlanItemInstanceState.AVAILABLE);
        assertEvent(events.get(1), "B", null, PlanItemInstanceState.AVAILABLE);

        assertEvent(events.get(2), "A", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        assertEvent(events.get(3), "B", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ENABLED);

        testLifeCycleListener.clear();

        // Disable B
        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.disablePlanItemInstance(planItemInstanceB.getId());

        events = testLifeCycleListener.getEvents();
        assertEquals(1, events.size());
        assertEvent(events.get(0), "B", PlanItemInstanceState.ENABLED, PlanItemInstanceState.DISABLED);

        testLifeCycleListener.clear();

        // Enable B
        cmmnRuntimeService.enablePlanItemInstance(planItemInstanceB.getId());

        events = testLifeCycleListener.getEvents();
        assertEquals(1, events.size());
        assertEvent(events.get(0), "B", PlanItemInstanceState.DISABLED, PlanItemInstanceState.ENABLED);

        testLifeCycleListener.clear();

        // Start B
        planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.startPlanItemInstance(planItemInstanceB.getId());

        events = testLifeCycleListener.getEvents();
        assertEquals(1, events.size());
        assertEvent(events.get(0), "B", PlanItemInstanceState.ENABLED, PlanItemInstanceState.ACTIVE);

        testLifeCycleListener.clear();

        // Complete A and B
        for (Task task : cmmnTaskService.createTaskQuery().orderByTaskName().asc().list()) {
            cmmnTaskService.complete(task.getId());
        }

        events = testLifeCycleListener.getEvents();
        assertEquals(4, events.size());

        assertEvent(events.get(0), "A", PlanItemInstanceState.ACTIVE, PlanItemInstanceState.COMPLETED);
        assertEvent(events.get(1), "B", PlanItemInstanceState.ACTIVE, PlanItemInstanceState.COMPLETED);
        assertEvent(events.get(2), "C", null, PlanItemInstanceState.AVAILABLE);
        assertEvent(events.get(3), "C", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
    }

    @Test
    public void testFilterBySourceState() {
        setTestLifeCycleListener(new TestFilterSourceStateLifeCycleListener(PlanItemInstanceState.AVAILABLE));

        // Start case instance
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testLifeCycleListener").start();

        List<TestLifeCycleEvent> events = testLifeCycleListener.getEvents();
        assertEquals(3, events.size());

        assertEvent(events.get(0), "Stage one", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        assertEvent(events.get(1), "A", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        assertEvent(events.get(2), "B", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ENABLED);

        testLifeCycleListener.clear();

        // Disable B
        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.disablePlanItemInstance(planItemInstanceB.getId());

        events = testLifeCycleListener.getEvents();
        assertEquals(0, events.size());

        // Enable B
        cmmnRuntimeService.enablePlanItemInstance(planItemInstanceB.getId());

        events = testLifeCycleListener.getEvents();
        assertEquals(0, events.size());

        // Start B
        planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.startPlanItemInstance(planItemInstanceB.getId());

        events = testLifeCycleListener.getEvents();
        assertEquals(0, events.size());

        testLifeCycleListener.clear();

        // Complete A and B
        for (Task task : cmmnTaskService.createTaskQuery().orderByTaskName().asc().list()) {
            cmmnTaskService.complete(task.getId());
        }

        events = testLifeCycleListener.getEvents();
        assertEquals(3, events.size());

        assertEvent(events.get(0), "Stage two", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        assertEvent(events.get(1), "M1", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        assertEvent(events.get(2), "C", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
    }

    @Test
    public void testFilterByTargetState() {
        setTestLifeCycleListener(new TestFilterTargetStateLifeCycleListener(PlanItemInstanceState.ACTIVE));

        // Start case instance
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testLifeCycleListener").start();

        List<TestLifeCycleEvent> events = testLifeCycleListener.getEvents();
        assertEquals(2, events.size());

        assertEvent(events.get(0), "Stage one", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        assertEvent(events.get(1), "A", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);

        testLifeCycleListener.clear();

        // Disable B
        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.disablePlanItemInstance(planItemInstanceB.getId());

        events = testLifeCycleListener.getEvents();
        assertEquals(0, events.size());

        // Enable B
        cmmnRuntimeService.enablePlanItemInstance(planItemInstanceB.getId());

        events = testLifeCycleListener.getEvents();
        assertEquals(0, events.size());

        // Start B
        planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.startPlanItemInstance(planItemInstanceB.getId());

        events = testLifeCycleListener.getEvents();
        assertEquals(1, events.size());
        assertEvent(events.get(0), "B", PlanItemInstanceState.ENABLED, PlanItemInstanceState.ACTIVE);

        testLifeCycleListener.clear();

        // Complete A and B
        for (Task task : cmmnTaskService.createTaskQuery().orderByTaskName().asc().list()) {
            cmmnTaskService.complete(task.getId());
        }

        events = testLifeCycleListener.getEvents();
        assertEquals(3, events.size());

        assertEvent(events.get(0), "Stage two", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        assertEvent(events.get(1), "M1", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        assertEvent(events.get(2), "C", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
    }

    private void setTestLifeCycleListener(AbstractTestLifeCycleListener testLifeCycleListener) {
        cmmnEngineConfiguration.setPlanItemInstanceLifeCycleListeners(Collections.singletonList(testLifeCycleListener));
        this.testLifeCycleListener = testLifeCycleListener;
    }

    private void assertEvent(TestLifeCycleEvent event, String name, String oldState, String newState) {
        assertEquals(name, event.getPlanItemInstance().getName());
        if (oldState == null) {
            assertNull(event.getOldState());
        } else {
            assertEquals(oldState, event.getOldState());
        }
        assertEquals(newState, event.getNewState());
    }

}
