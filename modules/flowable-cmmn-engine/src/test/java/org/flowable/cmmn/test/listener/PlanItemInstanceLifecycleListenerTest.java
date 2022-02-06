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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.listener.PlanItemInstanceLifecycleListener;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.UserEventListenerInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.task.api.Task;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class PlanItemInstanceLifecycleListenerTest extends FlowableCmmnTestCase {

    private Map<String, List<PlanItemInstanceLifecycleListener>> originalLifeCycleListeners;

    private AbstractTestLifecycleListener testLifeCycleListener;


    @Before
    public void addListeners() {
        this.originalLifeCycleListeners = cmmnEngineConfiguration.getPlanItemInstanceLifecycleListeners();

        addDeploymentForAutoCleanup(cmmnRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/listener/PlanItemInstanceLifeCycleListenerTest.cmmn")
            .deploy());
    }

    @After
    public void removeListeners() {
        cmmnEngineConfiguration.setPlanItemInstanceLifecycleListeners(originalLifeCycleListeners);
    }

    @Test
    public void testReceiveAllLifeCycleEvents() {
        setTestLifeCycleListener(null, new TestReceiveAllLifecycleListener());

        // Start case instance
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testLifeCycleListener").start();

        List<TestLifeCycleEvent> events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(7);

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
        assertThat(events).hasSize(1);
        assertEvent(events.get(0), "B", PlanItemInstanceState.ENABLED, PlanItemInstanceState.DISABLED);

        testLifeCycleListener.clear();

        // Enable B
        cmmnRuntimeService.enablePlanItemInstance(planItemInstanceB.getId());

        events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(1);
        assertEvent(events.get(0), "B", PlanItemInstanceState.DISABLED, PlanItemInstanceState.ENABLED);

        testLifeCycleListener.clear();

        // Start B
        planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.startPlanItemInstance(planItemInstanceB.getId());

        events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(1);
        assertEvent(events.get(0), "B", PlanItemInstanceState.ENABLED, PlanItemInstanceState.ACTIVE);

        testLifeCycleListener.clear();

        // Complete A and B
        for (Task task : cmmnTaskService.createTaskQuery().orderByTaskName().asc().list()) {
            cmmnTaskService.complete(task.getId());
        }

        events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(10);

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
        setTestLifeCycleListener(PlanItemDefinitionType.HUMAN_TASK, new TestFilterTypesLifecycleListener());

        // Start case instance
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testLifeCycleListener").start();

        List<TestLifeCycleEvent> events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(4);

        assertEvent(events.get(0), "A", null, PlanItemInstanceState.AVAILABLE);
        assertEvent(events.get(1), "B", null, PlanItemInstanceState.AVAILABLE);

        assertEvent(events.get(2), "A", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        assertEvent(events.get(3), "B", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ENABLED);

        testLifeCycleListener.clear();

        // Disable B
        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.disablePlanItemInstance(planItemInstanceB.getId());

        events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(1);
        assertEvent(events.get(0), "B", PlanItemInstanceState.ENABLED, PlanItemInstanceState.DISABLED);

        testLifeCycleListener.clear();

        // Enable B
        cmmnRuntimeService.enablePlanItemInstance(planItemInstanceB.getId());

        events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(1);
        assertEvent(events.get(0), "B", PlanItemInstanceState.DISABLED, PlanItemInstanceState.ENABLED);

        testLifeCycleListener.clear();

        // Start B
        planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.startPlanItemInstance(planItemInstanceB.getId());

        events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(1);
        assertEvent(events.get(0), "B", PlanItemInstanceState.ENABLED, PlanItemInstanceState.ACTIVE);

        testLifeCycleListener.clear();

        // Complete A and B
        for (Task task : cmmnTaskService.createTaskQuery().orderByTaskName().asc().list()) {
            cmmnTaskService.complete(task.getId());
        }

        events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(4);

        assertEvent(events.get(0), "A", PlanItemInstanceState.ACTIVE, PlanItemInstanceState.COMPLETED);
        assertEvent(events.get(1), "B", PlanItemInstanceState.ACTIVE, PlanItemInstanceState.COMPLETED);
        assertEvent(events.get(2), "C", null, PlanItemInstanceState.AVAILABLE);
        assertEvent(events.get(3), "C", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
    }

    @Test
    public void testFilterBySourceState() {
        setTestLifeCycleListener(null, new TestFilterSourceStateLifecycleListener(PlanItemInstanceState.AVAILABLE));

        // Start case instance
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testLifeCycleListener").start();

        List<TestLifeCycleEvent> events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(3);

        assertEvent(events.get(0), "Stage one", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        assertEvent(events.get(1), "A", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        assertEvent(events.get(2), "B", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ENABLED);

        testLifeCycleListener.clear();

        // Disable B
        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.disablePlanItemInstance(planItemInstanceB.getId());

        events = testLifeCycleListener.getEvents();
        assertThat(events).isEmpty();

        // Enable B
        cmmnRuntimeService.enablePlanItemInstance(planItemInstanceB.getId());

        events = testLifeCycleListener.getEvents();
        assertThat(events).isEmpty();

        // Start B
        planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.startPlanItemInstance(planItemInstanceB.getId());

        events = testLifeCycleListener.getEvents();
        assertThat(events).isEmpty();

        testLifeCycleListener.clear();

        // Complete A and B
        for (Task task : cmmnTaskService.createTaskQuery().orderByTaskName().asc().list()) {
            cmmnTaskService.complete(task.getId());
        }

        events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(3);

        assertEvent(events.get(0), "Stage two", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        assertEvent(events.get(1), "M1", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        assertEvent(events.get(2), "C", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
    }

    @Test
    public void testFilterByTargetState() {
        setTestLifeCycleListener(null, new TestFilterTargetStateLifecycleListener(PlanItemInstanceState.ACTIVE));

        // Start case instance
        cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testLifeCycleListener").start();

        List<TestLifeCycleEvent> events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(2);

        assertEvent(events.get(0), "Stage one", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        assertEvent(events.get(1), "A", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);

        testLifeCycleListener.clear();

        // Disable B
        PlanItemInstance planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.disablePlanItemInstance(planItemInstanceB.getId());

        events = testLifeCycleListener.getEvents();
        assertThat(events).isEmpty();

        // Enable B
        cmmnRuntimeService.enablePlanItemInstance(planItemInstanceB.getId());

        events = testLifeCycleListener.getEvents();
        assertThat(events).isEmpty();

        // Start B
        planItemInstanceB = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceName("B").singleResult();
        cmmnRuntimeService.startPlanItemInstance(planItemInstanceB.getId());

        events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(1);
        assertEvent(events.get(0), "B", PlanItemInstanceState.ENABLED, PlanItemInstanceState.ACTIVE);

        testLifeCycleListener.clear();

        // Complete A and B
        for (Task task : cmmnTaskService.createTaskQuery().orderByTaskName().asc().list()) {
            cmmnTaskService.complete(task.getId());
        }

        events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(3);

        assertEvent(events.get(0), "Stage two", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        assertEvent(events.get(1), "M1", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
        assertEvent(events.get(2), "C", PlanItemInstanceState.AVAILABLE, PlanItemInstanceState.ACTIVE);
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/listener/PlanItemInstanceLifecycleListenerTest.testUserEventListenerRepetition.cmmn")
    public void testEnterUserEventListenerRepetition() {
        setTestLifeCycleListener(null, new TestEnterUserEventListener());

        // Start case instance
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testRepetition").start();
        
        List<TestLifeCycleEvent> events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(1);
        
        UserEventListenerInstance userEventListenrInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(userEventListenrInstance.getId());
        
        events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(2);
        
        cmmnRuntimeService.triggerPlanItemInstance(userEventListenrInstance.getId());
        
        events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(3);
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/listener/PlanItemInstanceLifecycleListenerTest.testUserEventListenerRepetitionWithCondition.cmmn")
    public void testEnterUserEventListenerRepetitionWithCondition() {
        TestLeaveUserEventListener testLeaveUserEventListener = new TestLeaveUserEventListener();
        setTestLifeCycleListener(null, testLeaveUserEventListener);
        setTestLifeCycleListener(null, new TestEnterUserEventListener());

        // Start case instance
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testRepetition")
                .variable("continueRepeat", true)
                .start();
        
        List<TestLifeCycleEvent> events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(1);
        
        UserEventListenerInstance userEventListenrInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(userEventListenrInstance.getId());
        
        events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(2);
        
        List<TestLifeCycleEvent> leaveEvents = testLeaveUserEventListener.getEvents();
        assertThat(leaveEvents).hasSize(1);
        
        cmmnRuntimeService.setVariable(caseInstance.getId(), "continueRepeat", false);
        
        cmmnRuntimeService.triggerPlanItemInstance(userEventListenrInstance.getId());
        
        events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(3);
        
        leaveEvents = testLeaveUserEventListener.getEvents();
        assertThat(leaveEvents).hasSize(3);
        
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }
    
    @Test
    @CmmnDeployment
    public void testUserEventListenerRepetition() {
        setTestLifeCycleListener(null, new TestLeaveUserEventListener());

        // Start case instance
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testRepetition").start();
        
        assertThat(testLifeCycleListener.getEvents()).isEmpty();

        UserEventListenerInstance userEventListenrInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(userEventListenrInstance.getId());

        List<TestLifeCycleEvent> events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(1);
        
        cmmnRuntimeService.triggerPlanItemInstance(userEventListenrInstance.getId());
        events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(2);
        
        cmmnRuntimeService.triggerPlanItemInstance(userEventListenrInstance.getId());
        events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(3);
    }
    
    @Test
    @CmmnDeployment
    public void testUserEventListenerRepetitionWithCondition() {
        setTestLifeCycleListener(null, new TestLeaveUserEventListener());

        // Start case instance
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testRepetition")
                .variable("continueRepeat", true)
                .start();
        
        assertThat(testLifeCycleListener.getEvents()).isEmpty();

        UserEventListenerInstance userEventListenrInstance = cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(userEventListenrInstance.getId());

        List<TestLifeCycleEvent> events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(1);
        
        cmmnRuntimeService.setVariable(caseInstance.getId(), "continueRepeat", false);
        
        cmmnRuntimeService.triggerPlanItemInstance(userEventListenrInstance.getId());
        events = testLifeCycleListener.getEvents();
        assertThat(events).hasSize(3);
        
        assertThat(cmmnRuntimeService.createUserEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }

    private void setTestLifeCycleListener(String planItemDefinitionType, AbstractTestLifecycleListener testLifeCycleListener) {
        cmmnEngineConfiguration.addPlanItemInstanceLifeCycleListener(planItemDefinitionType, testLifeCycleListener);
        this.testLifeCycleListener = testLifeCycleListener;
    }

    private void assertEvent(TestLifeCycleEvent event, String name, String oldState, String newState) {
        assertThat(event.getPlanItemInstance().getName()).isEqualTo(name);
        if (oldState == null) {
            assertThat(event.getOldState()).isNull();
        } else {
            assertThat(event.getOldState()).isEqualTo(oldState);
        }
        assertThat(event.getNewState()).isEqualTo(newState);
    }

}
