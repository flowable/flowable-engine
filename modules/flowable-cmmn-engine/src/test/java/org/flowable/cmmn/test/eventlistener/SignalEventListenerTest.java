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
package org.flowable.cmmn.test.eventlistener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.api.runtime.SignalEventListenerInstance;
import org.flowable.cmmn.engine.impl.CmmnManagementServiceImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.service.impl.persistence.entity.SignalEventSubscriptionEntity;
import org.flowable.task.api.Task;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * @author Tijs Rademakers
 */
public class SignalEventListenerTest extends FlowableCmmnTestCase {

    @Rule
    public TestName name = new TestName();

    @Test
    @CmmnDeployment
    public void testSimpleEnableTask() {
        //Simple use of the SignalEventListener as EntryCriteria of a Task
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertThat(caseInstance).isNotNull();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(1);

        //3 PlanItems reachable
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().count()).isEqualTo(3);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().list())
                .extracting(PlanItemInstance::getPlanItemDefinitionType, PlanItemInstance::getPlanItemDefinitionId, PlanItemInstance::getState)
                .containsExactlyInAnyOrder(
                        tuple(PlanItemDefinitionType.SIGNAL_EVENT_LISTENER, "eventListener", PlanItemInstanceState.AVAILABLE),
                        tuple(PlanItemDefinitionType.HUMAN_TASK, "taskA", PlanItemInstanceState.ACTIVE),
                        tuple(PlanItemDefinitionType.HUMAN_TASK, "taskB", PlanItemInstanceState.AVAILABLE)
                );

        //1 Signal Event Listener
        PlanItemInstance listenerInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.SIGNAL_EVENT_LISTENER).singleResult();
        assertThat(listenerInstance).isNotNull();
        assertThat(listenerInstance.getPlanItemDefinitionId()).isEqualTo("eventListener");
        assertThat(listenerInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        // Verify same result is returned from query
        SignalEventListenerInstance eventListenerInstance = cmmnRuntimeService.createSignalEventListenerInstanceQuery().caseInstanceId(caseInstance.getId())
                .singleResult();
        assertThat(eventListenerInstance).isNotNull();
        assertThat(listenerInstance.getId()).isEqualTo(eventListenerInstance.getId());
        assertThat(listenerInstance.getCaseDefinitionId()).isEqualTo(eventListenerInstance.getCaseDefinitionId());
        assertThat(listenerInstance.getCaseInstanceId()).isEqualTo(eventListenerInstance.getCaseInstanceId());
        assertThat(listenerInstance.getElementId()).isEqualTo(eventListenerInstance.getElementId());
        assertThat(listenerInstance.getName()).isEqualTo(eventListenerInstance.getName());
        assertThat(listenerInstance.getPlanItemDefinitionId()).isEqualTo(eventListenerInstance.getPlanItemDefinitionId());
        assertThat(listenerInstance.getStageInstanceId()).isEqualTo(eventListenerInstance.getStageInstanceId());
        assertThat(listenerInstance.getState()).isEqualTo(eventListenerInstance.getState());

        assertThat(cmmnRuntimeService.createSignalEventListenerInstanceQuery().count()).isEqualTo(1);
        assertThat(cmmnRuntimeService.createSignalEventListenerInstanceQuery().list()).hasSize(1);

        assertThat(cmmnRuntimeService.createSignalEventListenerInstanceQuery().caseDefinitionId(listenerInstance.getCaseDefinitionId()).singleResult())
                .isNotNull();

        //2 HumanTasks ... one active and other waiting (available)
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).count()).isEqualTo(2);
        PlanItemInstance active = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceStateActive().singleResult();
        assertThat(active).isNotNull();
        assertThat(active.getPlanItemDefinitionId()).isEqualTo("taskA");
        PlanItemInstance available = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
                .planItemInstanceStateAvailable().singleResult();
        assertThat(available).isNotNull();
        assertThat(available.getPlanItemDefinitionId()).isEqualTo("taskB");

        EventSubscription eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(eventListenerInstance.getCaseInstanceId())
                .singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getSubScopeId()).isEqualTo(eventListenerInstance.getId());
        assertThat(eventSubscription.getScopeDefinitionId()).isEqualTo(eventListenerInstance.getCaseDefinitionId());
        assertThat(eventSubscription.getScopeType()).isEqualTo(ScopeTypes.CMMN);
        assertThat(eventSubscription.getEventName()).isEqualTo("testSignal");

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().list())
                    .extracting(HistoricPlanItemInstance::getPlanItemDefinitionType, HistoricPlanItemInstance::getPlanItemDefinitionId, HistoricPlanItemInstance::getState)
                    .containsExactlyInAnyOrder(
                            tuple(PlanItemDefinitionType.SIGNAL_EVENT_LISTENER, "eventListener", PlanItemInstanceState.AVAILABLE),
                            tuple(PlanItemDefinitionType.HUMAN_TASK, "taskA", PlanItemInstanceState.ACTIVE),
                            tuple(PlanItemDefinitionType.HUMAN_TASK, "taskB", PlanItemInstanceState.AVAILABLE)
                    );
        }

        // Trigger the signal
        cmmnRuntimeService.triggerPlanItemInstance(listenerInstance.getId());

        // SignalEventListener should be completed
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.SIGNAL_EVENT_LISTENER).count()).isZero();

        // Only 2 PlanItems left
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().list()).hasSize(2);
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().list())
                .extracting(PlanItemInstance::getPlanItemDefinitionType, PlanItemInstance::getPlanItemDefinitionId, PlanItemInstance::getState)
                .containsExactlyInAnyOrder(
                        tuple(PlanItemDefinitionType.HUMAN_TASK, "taskA", PlanItemInstanceState.ACTIVE),
                        tuple(PlanItemDefinitionType.HUMAN_TASK, "taskB", PlanItemInstanceState.ACTIVE)
                );

        // Both Human task should be "active" now, as the sentry kicks on the SignalEventListener transition
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK).planItemInstanceStateActive()
                .count()).isEqualTo(2);

        eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().scopeId(eventListenerInstance.getCaseInstanceId()).singleResult();
        assertThat(eventSubscription).isNull();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().list())
                    .extracting(HistoricPlanItemInstance::getPlanItemDefinitionType, HistoricPlanItemInstance::getPlanItemDefinitionId, HistoricPlanItemInstance::getState)
                    .containsExactlyInAnyOrder(
                            tuple(PlanItemDefinitionType.SIGNAL_EVENT_LISTENER, "eventListener", PlanItemInstanceState.COMPLETED),
                            tuple(PlanItemDefinitionType.HUMAN_TASK, "taskA", PlanItemInstanceState.ACTIVE),
                            tuple(PlanItemDefinitionType.HUMAN_TASK, "taskB", PlanItemInstanceState.ACTIVE)
                    );
        }

        // Finish the case instance
        assertCaseInstanceNotEnded(caseInstance);
        cmmnTaskService.createTaskQuery().list().forEach(t -> cmmnTaskService.complete(t.getId()));
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testTerminateStage() {
        //Test case where the SignalEventListener is used to complete (ExitCriteria) of a Stage
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(name.getMethodName()).start();
        assertThat(caseInstance).isNotNull();
        assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(1);

        //3 PlanItems reachable
        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().list()).hasSize(3);

        //1 Stage
        PlanItemInstance stage = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType(PlanItemDefinitionType.STAGE).singleResult();
        assertThat(stage).isNotNull();

        //1 User Event Listener
        PlanItemInstance listenerInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.SIGNAL_EVENT_LISTENER).singleResult();
        assertThat(listenerInstance).isNotNull();
        assertThat(listenerInstance.getPlanItemDefinitionId()).isEqualTo("eventListener");
        assertThat(listenerInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);

        //1 Task on Active state
        PlanItemInstance task = cmmnRuntimeService.createPlanItemInstanceQuery().planItemDefinitionType("task").singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getState()).isEqualTo(PlanItemInstanceState.ACTIVE);
        assertThat(task.getStageInstanceId()).isEqualTo(stage.getId());

        //Trigger the listener
        assertCaseInstanceNotEnded(caseInstance);
        cmmnRuntimeService.triggerPlanItemInstance(listenerInstance.getId());
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testActiveState() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testSimpleEnableTask").start();
        SignalEventListenerInstance signalEventListenerInstance = cmmnRuntimeService.createSignalEventListenerInstanceQuery()
            .caseInstanceId(caseInstance.getId()).singleResult();

        // In older releases, the signal event listener lifecycle wasn't consistent with the other event listeners.
        // More specifically: it could be active (which an event listener never can be), which now is available (consistent with all event listeners)

        assertThat(signalEventListenerInstance.getState()).isEqualTo(PlanItemInstanceState.AVAILABLE);
        assertThat(cmmnRuntimeService.createEventSubscriptionQuery().list()).isNotEmpty();

        // Changing the state programmatically to mimic the old instances
        ((CmmnManagementServiceImpl) cmmnManagementService).executeCommand(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {

                PlanItemInstanceEntity planItemInstanceEntity = cmmnEngineConfiguration.getPlanItemInstanceEntityManager()
                    .findById(signalEventListenerInstance.getId());
                planItemInstanceEntity.setState(PlanItemInstanceState.ACTIVE);

                return null;
            }

        });

        // Note that the SignalEventListenerInstanceQuery since its creation has a stateAvailable method, but no stateActive,
        // so usage of this API can't have been with stateAvailable before the fix.
        assertThat(cmmnRuntimeService.createSignalEventListenerInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult()).isNotNull();

        // Terminating the case triggers the state change
        cmmnRuntimeService.terminateCaseInstance(caseInstance.getId());

        assertThat(cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isEqualTo(0L);
        assertThat(cmmnRuntimeService.createEventSubscriptionQuery().list()).isEmpty();

    }

    @Test
    public void testRedeployDefinitionWithRuntimeEventSubscriptions() {
        org.flowable.cmmn.api.repository.CmmnDeployment deployment = cmmnRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/eventlistener/SignalEventListenerTest.testRedeploy.cmmn")
            .deploy();
        addDeploymentForAutoCleanup(deployment);
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deployment.getId()).singleResult();

        // After the case instance is started, there should be one eventsubscription for the signal event listener
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testRedeploy").start();
        assertThat(cmmnRuntimeService.createEventSubscriptionQuery().scopeType(ScopeTypes.CMMN).list())
            .extracting(EventSubscription::getEventType, EventSubscription::getScopeDefinitionId,  EventSubscription::getScopeType, EventSubscription::getScopeId)
            .containsOnly(
                tuple(SignalEventSubscriptionEntity.EVENT_TYPE, caseDefinition.getId(), ScopeTypes.CMMN, caseInstance.getId())
            );

        // Redeploying the same definition:
        // Event subscription to start should reflect new definition id
        // Existing subscription for event listener should remain
        org.flowable.cmmn.api.repository.CmmnDeployment redeployment = cmmnRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/eventlistener/SignalEventListenerTest.testRedeploy.cmmn")
            .deploy();
        addDeploymentForAutoCleanup(redeployment);
        CaseDefinition caseDefinitionAfterRedeploy = cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(redeployment.getId()).singleResult();

        assertThat(cmmnRuntimeService.createEventSubscriptionQuery().scopeType(ScopeTypes.CMMN).list())
            .extracting(EventSubscription::getEventType, EventSubscription::getScopeDefinitionId,  EventSubscription::getScopeType, EventSubscription::getScopeId)
            .containsOnly(
                tuple(SignalEventSubscriptionEntity.EVENT_TYPE, caseDefinition.getId(), ScopeTypes.CMMN, caseInstance.getId())
            );

        // Triggering the instance event subscription should continue the case instance like before
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list())
            .extracting(Task::getName)
            .containsOnly("My task 1");

        SignalEventListenerInstance signalEventListenerInstance = cmmnRuntimeService.createSignalEventListenerInstanceQuery()
            .caseInstanceId(caseInstance.getId()).singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(signalEventListenerInstance.getId());

        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list())
            .extracting(Task::getName, Task::getScopeId)
            .containsOnly(
                tuple("My task 1", caseInstance.getId()),
                tuple("My task 2", caseInstance.getId()));
    }

    @Test
    @CmmnDeployment
    public void testWithRepetitionMultiple() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testRepetition")
                .variable("keepRepeating", true)
                .start();

        SignalEventListenerInstance signalEventListenerInstance = cmmnRuntimeService.createSignalEventListenerInstanceQuery()
                .caseInstanceId(caseInstance.getId()).singleResult();
        String signalPlanItemInstanceId = signalEventListenerInstance.getId();
        
        EventSubscription eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().subScopeId(signalEventListenerInstance.getId()).singleResult();
        String eventSubScriptionId = eventSubscription.getId();
        assertThat(eventSubscription).isNotNull();
        
        cmmnRuntimeService.triggerPlanItemInstance(signalEventListenerInstance.getId());
        
        cmmnRuntimeService.setVariable(caseInstance.getId(), "keepRepeating", false);

        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskDefinitionKey("taskA").active().singleResult().getId());
        
        signalEventListenerInstance = cmmnRuntimeService.createSignalEventListenerInstanceQuery()
                .caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(signalEventListenerInstance.getId()).isEqualTo(signalPlanItemInstanceId);
        
        eventSubscription = cmmnRuntimeService.createEventSubscriptionQuery().subScopeId(signalEventListenerInstance.getId()).singleResult();
        assertThat(eventSubscription.getId()).isEqualTo(eventSubScriptionId);
        
        cmmnRuntimeService.triggerPlanItemInstance(signalEventListenerInstance.getId());
        
        assertThat(cmmnRuntimeService.createEventSubscriptionQuery().scopeId(caseInstance.getId()).count()).isZero();
        
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testWithRepetitionParallel() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testRepetition")
                .variable("keepRepeating", true)
                .start();

        cmmnEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                SignalEventListenerInstance signalEventListenerInstance = cmmnRuntimeService.createSignalEventListenerInstanceQuery()
                        .caseInstanceId(caseInstance.getId()).singleResult();
                cmmnRuntimeService.triggerPlanItemInstance(signalEventListenerInstance.getId());
                
                cmmnRuntimeService.setVariable(caseInstance.getId(), "keepRepeating", false);
                
                signalEventListenerInstance = cmmnRuntimeService.createSignalEventListenerInstanceQuery()
                        .caseInstanceId(caseInstance.getId()).singleResult();
                cmmnRuntimeService.triggerPlanItemInstance(signalEventListenerInstance.getId());
                
                return null;
            }
            
        });
        
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskDefinitionKey("taskA").active().singleResult().getId());
        
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testWithRepetitionParallelMultipleTasks() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testRepetition")
                .variable("keepRepeating", true)
                .variable("keepRepeatingTask", true)
                .start();

        cmmnEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                SignalEventListenerInstance signalEventListenerInstance = cmmnRuntimeService.createSignalEventListenerInstanceQuery()
                        .caseInstanceId(caseInstance.getId()).singleResult();
                cmmnRuntimeService.triggerPlanItemInstance(signalEventListenerInstance.getId());
                
                cmmnRuntimeService.setVariable(caseInstance.getId(), "keepRepeating", false);
                
                signalEventListenerInstance = cmmnRuntimeService.createSignalEventListenerInstanceQuery()
                        .caseInstanceId(caseInstance.getId()).singleResult();
                cmmnRuntimeService.triggerPlanItemInstance(signalEventListenerInstance.getId());
                
                cmmnRuntimeService.setVariable(caseInstance.getId(), "keepRepeatingTask", false);
                
                return null;
            }
            
        });
        
        assertThat(cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("taskA").count()).isEqualTo(2);
        for (Task task : cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskDefinitionKey("taskA").list()) {
            cmmnTaskService.complete(task.getId());
        }
        
        assertCaseInstanceEnded(caseInstance);
    }
}

