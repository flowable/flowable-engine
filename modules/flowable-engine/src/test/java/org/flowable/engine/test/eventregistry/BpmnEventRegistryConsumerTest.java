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
package org.flowable.engine.test.eventregistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.flowable.common.engine.api.constant.ReferenceTypes;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class BpmnEventRegistryConsumerTest extends FlowableEventRegistryBpmnTestCase {

    protected TestInboundEventChannelAdapter inboundEventChannelAdapter;

    @BeforeEach
    public void setUp() throws Exception {
        inboundEventChannelAdapter = setupTestChannel();

        getEventRepositoryService().createEventModelBuilder()
            .key("myEvent")
            .resourceName("myEvent.event")
            .correlationParameter("customerId", EventPayloadTypes.STRING)
            .correlationParameter("orderId", EventPayloadTypes.STRING)
            .payload("payload1", EventPayloadTypes.STRING)
            .payload("payload2", EventPayloadTypes.INTEGER)
            .deploy();
    }
    
    protected TestInboundEventChannelAdapter setupTestChannel() {
        TestInboundEventChannelAdapter inboundEventChannelAdapter = new TestInboundEventChannelAdapter();
        getEventRegistryEngineConfiguration().getExpressionManager().getBeans()
            .put("inboundEventChannelAdapter", inboundEventChannelAdapter);

        getEventRepositoryService().createInboundChannelModelBuilder()
            .key("test-channel")
            .resourceName("testChannel.channel")
            .channelAdapter("${inboundEventChannelAdapter}")
            .jsonDeserializer()
            .detectEventKeyUsingJsonField("type")
            .jsonFieldsMapDirectlyToPayload()
            .deploy();

        return inboundEventChannelAdapter;
    }

    @AfterEach
    public void tearDown() throws Exception {
        EventRepositoryService eventRepositoryService = getEventRepositoryService();
        List<EventDeployment> deployments = eventRepositoryService.createDeploymentQuery().list();
        for (EventDeployment eventDeployment : deployments) {
            eventRepositoryService.deleteDeployment(eventDeployment.getId());
        }
    }

    @Test
    @Deployment
    public void testBoundaryEventListenerNoCorrelation() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("task");
        
        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().activityId("eventBoundary").singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(eventSubscription.getEventType()).isEqualTo("myEvent");

        inboundEventChannelAdapter.triggerTestEvent();
        Task afterTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(afterTask.getTaskDefinitionKey()).isEqualTo("taskAfterBoundary");
        
        eventSubscription = runtimeService.createEventSubscriptionQuery().activityId("eventBoundary").singleResult();
        assertThat(eventSubscription).isNull();
    }
    
    @Test
    @Deployment
    public void testBoundaryEventListenerNoCorrelationNoTrigger() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("task");

        taskService.complete(task.getId());
        
        Task afterTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(afterTask.getTaskDefinitionKey()).isEqualTo("taskAfterTask");
        
        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().activityId("eventBoundary").singleResult();
        assertThat(eventSubscription).isNull();
    }

    @Test
    @Deployment
    public void testBoundaryEventListenerWithCorrelation() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("customerIdVar", "kermit");
        ProcessInstance kermitProcessInstance = runtimeService.startProcessInstanceByKey("process", variableMap);
        
        variableMap.clear();
        variableMap.put("customerIdVar", "gonzo");
        ProcessInstance gonzoProcessInstance = runtimeService.startProcessInstanceByKey("process", variableMap);
 
        assertThat(taskService.createTaskQuery().processInstanceId(kermitProcessInstance.getId()).singleResult().getTaskDefinitionKey()).isEqualTo("task");
        assertThat(taskService.createTaskQuery().processInstanceId(gonzoProcessInstance.getId()).singleResult().getTaskDefinitionKey()).isEqualTo("task");

        inboundEventChannelAdapter.triggerTestEvent("kermit");
        assertThat(taskService.createTaskQuery().processInstanceId(kermitProcessInstance.getId()).singleResult().getTaskDefinitionKey()).isEqualTo("taskAfterBoundary");
        assertThat(taskService.createTaskQuery().processInstanceId(gonzoProcessInstance.getId()).singleResult().getTaskDefinitionKey()).isEqualTo("task");

        inboundEventChannelAdapter.triggerTestEvent("fozzie");
        assertThat(taskService.createTaskQuery().processInstanceId(kermitProcessInstance.getId()).singleResult().getTaskDefinitionKey()).isEqualTo("taskAfterBoundary");
        assertThat(taskService.createTaskQuery().processInstanceId(gonzoProcessInstance.getId()).singleResult().getTaskDefinitionKey()).isEqualTo("task");

        inboundEventChannelAdapter.triggerTestEvent("gonzo");
        assertThat(taskService.createTaskQuery().processInstanceId(kermitProcessInstance.getId()).singleResult().getTaskDefinitionKey()).isEqualTo("taskAfterBoundary");
        assertThat(taskService.createTaskQuery().processInstanceId(gonzoProcessInstance.getId()).singleResult().getTaskDefinitionKey()).isEqualTo("taskAfterBoundary");
    }
    
    @Test
    @Deployment
    public void testBoundaryEventListenerWithPayload() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("task");
        
        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().activityId("eventBoundary").singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(eventSubscription.getEventType()).isEqualTo("myEvent");

        inboundEventChannelAdapter.triggerTestEvent("payloadStartCustomer");
        Task afterTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(afterTask.getTaskDefinitionKey()).isEqualTo("taskAfterBoundary");
        
        assertThat(runtimeService.getVariables(processInstance.getId()))
            .containsOnly(
                entry("customerIdVar", "payloadStartCustomer"),
                entry("payload1", "Hello World")
            );
        
        eventSubscription = runtimeService.createEventSubscriptionQuery().activityId("eventBoundary").singleResult();
        assertThat(eventSubscription).isNull();
    }

    @Test
    @Deployment
    public void testReceiveEventTaskNoCorrelation() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().activityId("task").singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(eventSubscription.getEventType()).isEqualTo("myEvent");

        inboundEventChannelAdapter.triggerTestEvent();
        Task afterTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(afterTask.getTaskDefinitionKey()).isEqualTo("taskAfterTask");

        assertThat(runtimeService.createEventSubscriptionQuery().activityId("task").singleResult()).isNull();
    }

    @Test
    @Deployment
    public void testReceiveEventTaskWithCorrelationAndPayload() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("customerIdVar", "kermit");
        ProcessInstance kermitProcessInstance = runtimeService.startProcessInstanceByKey("process", variableMap);

        variableMap.clear();
        variableMap.put("customerIdVar", "gonzo");
        ProcessInstance gonzoProcessInstance = runtimeService.startProcessInstanceByKey("process", variableMap);

        inboundEventChannelAdapter.triggerTestEvent("kermit");
        assertThat(taskService.createTaskQuery().processInstanceId(kermitProcessInstance.getId()).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processInstanceId(gonzoProcessInstance.getId()).count()).isZero();

        inboundEventChannelAdapter.triggerTestEvent("kermit");
        Task afterTask = taskService.createTaskQuery().processInstanceId(kermitProcessInstance.getId()).singleResult();
        assertThat(afterTask.getTaskDefinitionKey()).isEqualTo("taskAfterTask");

        assertThat(runtimeService.getVariables(kermitProcessInstance.getId()))
            .containsOnly(
                entry("customerIdVar", "kermit"),
                entry("payload1", "Hello World")
            );

        inboundEventChannelAdapter.triggerTestEvent("fozzie");
        assertThat(taskService.createTaskQuery().processInstanceId(kermitProcessInstance.getId()).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processInstanceId(gonzoProcessInstance.getId()).count()).isZero();

        inboundEventChannelAdapter.triggerTestEvent("gonzo");
        assertThat(taskService.createTaskQuery().processInstanceId(kermitProcessInstance.getId()).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processInstanceId(gonzoProcessInstance.getId()).count()).isEqualTo(1);

        inboundEventChannelAdapter.triggerTestEvent("kermit");
        assertThat(taskService.createTaskQuery().processInstanceId(kermitProcessInstance.getId()).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processInstanceId(gonzoProcessInstance.getId()).count()).isEqualTo(1);

    }

    @Test
    @Deployment
    public void testIntermediateCatchEventNoCorrelation() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().activityId("catchEvent").singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(eventSubscription.getEventType()).isEqualTo("myEvent");

        inboundEventChannelAdapter.triggerTestEvent();
        Task afterTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(afterTask.getTaskDefinitionKey()).isEqualTo("taskAfterTask");

        assertThat(runtimeService.createEventSubscriptionQuery().activityId("task").singleResult()).isNull();
    }

    @Test
    @Deployment
    public void testIntermediateCatchEventWithCorrelationAndPayload() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("customerIdVar", "kermit");
        ProcessInstance kermitProcessInstance = runtimeService.startProcessInstanceByKey("process", variableMap);

        variableMap.clear();
        variableMap.put("customerIdVar", "gonzo");
        ProcessInstance gonzoProcessInstance = runtimeService.startProcessInstanceByKey("process", variableMap);

        inboundEventChannelAdapter.triggerTestEvent("kermit");
        assertThat(taskService.createTaskQuery().processInstanceId(kermitProcessInstance.getId()).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processInstanceId(gonzoProcessInstance.getId()).count()).isZero();

        inboundEventChannelAdapter.triggerTestEvent("kermit");
        Task afterTask = taskService.createTaskQuery().processInstanceId(kermitProcessInstance.getId()).singleResult();
        assertThat(afterTask.getTaskDefinitionKey()).isEqualTo("taskAfterTask");

        assertThat(runtimeService.getVariables(kermitProcessInstance.getId()))
            .containsOnly(
                entry("customerIdVar", "kermit"),
                entry("payload1", "Hello World")
            );

        inboundEventChannelAdapter.triggerTestEvent("fozzie");
        assertThat(taskService.createTaskQuery().processInstanceId(kermitProcessInstance.getId()).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processInstanceId(gonzoProcessInstance.getId()).count()).isZero();

        inboundEventChannelAdapter.triggerTestEvent("gonzo");
        assertThat(taskService.createTaskQuery().processInstanceId(kermitProcessInstance.getId()).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processInstanceId(gonzoProcessInstance.getId()).count()).isEqualTo(1);

        inboundEventChannelAdapter.triggerTestEvent("kermit");
        assertThat(taskService.createTaskQuery().processInstanceId(kermitProcessInstance.getId()).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().processInstanceId(gonzoProcessInstance.getId()).count()).isEqualTo(1);

    }


    @Test
    @Deployment
    public void testProcessStartNoCorrelationParameter() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("process").singleResult();
        assertThat(processDefinition).isNotNull();

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery()
            .processDefinitionId(processDefinition.getId())
            .scopeType(ScopeTypes.BPMN)
            .singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getEventType()).isEqualTo("myEvent");

        assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();

        for (int i = 1; i <= 5; i++) {
            inboundEventChannelAdapter.triggerTestEvent();
            assertThat(runtimeService.createProcessInstanceQuery().list()).hasSize(i);
        }
    }
    
    @Test
    @Deployment
    public void testProcessStartSimpleCorrelationParameter() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("process").singleResult();
        assertThat(processDefinition).isNotNull();

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery()
            .processDefinitionId(processDefinition.getId())
            .scopeType(ScopeTypes.BPMN)
            .singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getEventType()).isEqualTo("myEvent");

        assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();
        
        inboundEventChannelAdapter.triggerTestEvent("anotherCustomer");
        assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();

        for (int i = 1; i <= 5; i++) {
            inboundEventChannelAdapter.triggerTestEvent("testCustomer");
            assertThat(runtimeService.createProcessInstanceQuery().list()).hasSize(i);
        }
    }
    
    @Test
    @Deployment
    public void testProcessStartWithPayload() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("process").singleResult();
        assertThat(processDefinition).isNotNull();

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery()
            .processDefinitionId(processDefinition.getId())
            .scopeType(ScopeTypes.BPMN)
            .singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getEventType()).isEqualTo("myEvent");

        assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();
        
        inboundEventChannelAdapter.triggerTestEvent("payloadStartCustomer");
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("process").singleResult();
        assertThat(runtimeService.getVariables(processInstance.getId()))
            .containsOnly(
                entry("customerIdVar", "payloadStartCustomer"),
                entry("payload1", "Hello World")
            );
    }
    
    @Test
    @Deployment
    public void testProcessStartWithEventSubProcess() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("process").singleResult();
        assertThat(processDefinition).isNotNull();

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery()
            .processDefinitionId(processDefinition.getId())
            .scopeType(ScopeTypes.BPMN)
            .singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getEventType()).isEqualTo("myEvent");

        assertThat(runtimeService.createProcessInstanceQuery().list()).isEmpty();

        inboundEventChannelAdapter.triggerTestEvent("myVar1");
        
        assertThat(runtimeService.createProcessInstanceQuery().list()).hasSize(1);
        String processInstanceId = runtimeService.createProcessInstanceQuery().singleResult().getId();
        
        assertThat(taskService.createTaskQuery().processInstanceId(processInstanceId).taskDefinitionKey("task").count()).isEqualTo(1);
        
        List<EventSubscription> eventSubProcessEventSubscriptions = runtimeService.createEventSubscriptionQuery()
            .processInstanceId(processInstanceId)
            .scopeType(ScopeTypes.BPMN)
            .list();
        
        assertThat(eventSubProcessEventSubscriptions.size()).isEqualTo(2);
        
        for (EventSubscription subProcessEventSubscription : eventSubProcessEventSubscriptions) {
            assertThat(subProcessEventSubscription.getEventType()).isEqualTo("myEvent");
        }
        
        inboundEventChannelAdapter.triggerTestEvent("myVar2");
        
        assertThat(taskService.createTaskQuery().processInstanceId(processInstanceId).taskDefinitionKey("subProcessTask1").count()).isZero();
        
        inboundEventChannelAdapter.triggerTestEvent("myVar1");
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).taskDefinitionKey("subProcessTask1").singleResult();
        assertThat(task).isNotNull();
        
        assertThat(taskService.createTaskQuery().processInstanceId(processInstanceId).taskDefinitionKey("task").count()).isEqualTo(1);
        
        inboundEventChannelAdapter.triggerTestEvent("myVar1Interrupting");
        assertThat(taskService.createTaskQuery().processInstanceId(processInstanceId).taskDefinitionKey("task").count()).isZero();
        assertThat(taskService.createTaskQuery().processInstanceId(processInstanceId).taskDefinitionKey("subProcessTask1").count()).isZero();
        
        task = taskService.createTaskQuery().processInstanceId(processInstanceId).taskDefinitionKey("subProcessTask1Interrupting").singleResult();
        assertThat(task).isNotNull();
        
        taskService.complete(task.getId());
        
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).count()).isZero();
    }

    @Test
    @Deployment
    public void testStartOnlyOneInstance() {
        for (int i = 1; i <= 9; i++) {
            inboundEventChannelAdapter.triggerTestEvent("testCustomer");
            assertThat(runtimeService.createProcessInstanceQuery().list()).hasSize(1);
        }
        assertThat(runtimeService.createProcessInstanceQuery().singleResult().getReferenceId()).isNotNull();
        assertThat(runtimeService.createProcessInstanceQuery().singleResult().getReferenceType()).isEqualTo(ReferenceTypes.EVENT_PROCESS);

        for (int i = 1; i <= 3; i++) {
            inboundEventChannelAdapter.triggerTestEvent("anotherTestCustomer");
            assertThat(runtimeService.createProcessInstanceQuery().list()).hasSize(2);
        }
    }
    
    @Test
    @Deployment(resources = "org/flowable/engine/test/eventregistry/BpmnEventRegistryConsumerTest.testStartOnlyOneInstance.bpmn20.xml")
    public void testStartOneInstanceWithMultipleProcessDefinitionVersions() {
        inboundEventChannelAdapter.triggerTestEvent("testCustomer");
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("correlation").count()).isEqualTo(1);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("correlation").count()).isEqualTo(1);
        }
        
        org.flowable.engine.repository.Deployment deployment = null;
        try {
            deployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/eventregistry/BpmnEventRegistryConsumerTest.testStartOnlyOneInstance.bpmn20.xml")
                .deploy();
            
            assertThat(repositoryService.createProcessDefinitionQuery().processDefinitionKey("correlation").latestVersion().singleResult().getVersion()).isEqualTo(2);
            
            inboundEventChannelAdapter.triggerTestEvent("testCustomer");
            assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("correlation").count()).isEqualTo(1);

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("correlation").count()).isEqualTo(1);
            }
    
            inboundEventChannelAdapter.triggerTestEvent("anotherTestCustomer");
            assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("correlation").count()).isEqualTo(2);
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("correlation").count()).isEqualTo(2);
            }
            inboundEventChannelAdapter.triggerTestEvent("anotherTestCustomer");
            assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("correlation").count()).isEqualTo(2);

            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("correlation").count()).isEqualTo(2);
            }
            
        } finally {
            repositoryService.deleteDeployment(deployment.getId(), true);
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                // nothing to do, isHistoryLevelAtLeast will execute the jobs
            }
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/eventregistry/BpmnEventRegistryConsumerTest.testStartOnlyOneInstance.bpmn20.xml")
    public void testStartOneInstanceWithSingleDefinition() {

        inboundEventChannelAdapter.triggerTestEvent("testCustomer");
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("correlation").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().singleResult()).isNotNull();

        inboundEventChannelAdapter.triggerTestEvent("testCustomer");
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("correlation").count()).isEqualTo(1);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/eventregistry/BpmnEventRegistryConsumerTest.testStartOnlyOneInstance.bpmn20.xml")
    public void testStartOneInstanceWithAsyncStartInConfiguration() {

        boolean originalEventRegistryStartProcessInstanceAsync = processEngineConfiguration.isEventRegistryStartProcessInstanceAsync();
        try {
            processEngineConfiguration.setEventRegistryStartProcessInstanceAsync(true);
            inboundEventChannelAdapter.triggerTestEvent("testCustomer");
            assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("correlation").count()).isEqualTo(1);
            assertThat(taskService.createTaskQuery().singleResult()).isNull();
            waitForJobExecutorToProcessAllJobs(10000, 200);
            assertThat(taskService.createTaskQuery().singleResult()).isNotNull();

            inboundEventChannelAdapter.triggerTestEvent("testCustomer");
            assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("correlation").count()).isEqualTo(1);

        } finally {
            processEngineConfiguration.setEventRegistryStartProcessInstanceAsync(originalEventRegistryStartProcessInstanceAsync);
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/eventregistry/BpmnEventRegistryConsumerTest.testStartOnlyOneInstanceAsync.bpmn20.xml")
    public void testStartOneInstanceWithAsyncStartInProcessModel() {
        inboundEventChannelAdapter.triggerTestEvent("testCustomer");
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("correlation").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().singleResult()).isNull();
        waitForJobExecutorToProcessAllJobs(10000, 200);
        assertThat(taskService.createTaskQuery().singleResult()).isNotNull();

        inboundEventChannelAdapter.triggerTestEvent("testCustomer");
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("correlation").count()).isEqualTo(1);
    }

    @Test
    public void testRedeployDefinitionWithRuntimeEventSubscriptions() {
        org.flowable.engine.repository.Deployment deployment1 = repositoryService.createDeployment()
            .addClasspathResource("org/flowable/engine/test/eventregistry/BpmnEventRegistryConsumerTest.testRedeploy.bpmn20.xml")
            .deploy();
        deploymentIdsForAutoCleanup.add(deployment1.getId());
        ProcessDefinition processDefinition1 = repositoryService.createProcessDefinitionQuery().deploymentId(deployment1.getId()).singleResult();

        // After deploying, there should be one eventsubscription: to start the instance
        assertThat( runtimeService.createEventSubscriptionQuery().list())
            .extracting(EventSubscription::getEventType, EventSubscription::getProcessDefinitionId, EventSubscription::getProcessInstanceId)
            .containsOnly(tuple("myEvent", processDefinition1.getId(), null));

        // After the instance is started, there should be one additional eventsubscription
        inboundEventChannelAdapter.triggerTestEvent();
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance.getProcessDefinitionId()).isEqualTo(processDefinition1.getId());

        assertThat( runtimeService.createEventSubscriptionQuery().list())
            .extracting(EventSubscription::getEventType, EventSubscription::getProcessDefinitionId, EventSubscription::getProcessInstanceId)
            .containsOnly(
                tuple("myEvent", processDefinition1.getId(), null),
                tuple("myEvent", processDefinition1.getId(), processInstance.getId())
            );

        // Redeploying the same definition:
        // Event subscription to start should reflect new definition id
        // Existing subscription for boundary event should remain
        org.flowable.engine.repository.Deployment deployment2 = repositoryService.createDeployment()
            .addClasspathResource("org/flowable/engine/test/eventregistry/BpmnEventRegistryConsumerTest.testRedeploy.bpmn20.xml")
            .deploy();
        deploymentIdsForAutoCleanup.add(deployment2.getId());
        ProcessDefinition processDefinition2 = repositoryService.createProcessDefinitionQuery().deploymentId(deployment2.getId()).singleResult();

        assertThat( runtimeService.createEventSubscriptionQuery().list())
            .extracting(EventSubscription::getEventType, EventSubscription::getProcessDefinitionId, EventSubscription::getProcessInstanceId)
            .containsOnly(
                tuple("myEvent", processDefinition2.getId(), null), // note the new definition id
                tuple("myEvent", processDefinition1.getId(), processInstance.getId()) // note the original id
            );

        // Triggering the instance event subscription should continue the case instance like before
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list())
            .extracting(Task::getName)
            .containsOnly("My task");

        inboundEventChannelAdapter.triggerTestEvent();

        // Ended thanks to boundary event
        assertProcessEnded(processInstance.getId());
        processInstance = runtimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getProcessDefinitionId()).isEqualTo(processDefinition2.getId());

        assertThat(runtimeService.createEventSubscriptionQuery().list())
            .extracting(EventSubscription::getEventType, EventSubscription::getProcessDefinitionId, EventSubscription::getProcessInstanceId)
            .containsOnly(
                    tuple("myEvent", processDefinition2.getId(), null),
                    tuple("myEvent", processDefinition2.getId(), processInstance.getId()) // triggering the test event started a new process
            );
    }

    @Test
    public void testEventRegistrySubscriptionsRecreatedOnDeploymentDelete() {
        org.flowable.engine.repository.Deployment deployment1 = repositoryService.createDeployment()
            .addClasspathResource("org/flowable/engine/test/eventregistry/BpmnEventRegistryConsumerTest.testRedeploy.bpmn20.xml")
            .deploy();
        deploymentIdsForAutoCleanup.add(deployment1.getId());
        ProcessDefinition processDefinition1 = repositoryService.createProcessDefinitionQuery().deploymentId(deployment1.getId()).singleResult();

        // After deploying, there should be one eventsubscription: to start the instance
        assertThat( runtimeService.createEventSubscriptionQuery().list())
            .extracting(EventSubscription::getEventType, EventSubscription::getProcessDefinitionId, EventSubscription::getProcessInstanceId)
            .containsOnly(tuple("myEvent", processDefinition1.getId(), null));

        // After the instance is started, there should be one additional eventsubscription
        inboundEventChannelAdapter.triggerTestEvent();
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance.getProcessDefinitionId()).isEqualTo(processDefinition1.getId());

        assertThat( runtimeService.createEventSubscriptionQuery().list())
            .extracting(EventSubscription::getEventType, EventSubscription::getProcessDefinitionId, EventSubscription::getProcessInstanceId)
            .containsOnly(
                tuple("myEvent", processDefinition1.getId(), null),
                tuple("myEvent", processDefinition1.getId(), processInstance.getId())
            );

        // Redeploying the same definition:
        // Event subscription to start should reflect new definition id
        // Existing subscription for boundary event should remain
        org.flowable.engine.repository.Deployment deployment2 = repositoryService.createDeployment()
            .addClasspathResource("org/flowable/engine/test/eventregistry/BpmnEventRegistryConsumerTest.testRedeploy.bpmn20.xml")
            .deploy();
        deploymentIdsForAutoCleanup.add(deployment2.getId());
        ProcessDefinition processDefinition2 = repositoryService.createProcessDefinitionQuery().deploymentId(deployment2.getId()).singleResult();

        assertThat( runtimeService.createEventSubscriptionQuery().list())
            .extracting(EventSubscription::getEventType, EventSubscription::getProcessDefinitionId, EventSubscription::getProcessInstanceId)
            .containsOnly(
                tuple("myEvent", processDefinition2.getId(), null), // note the new definition id
                tuple("myEvent", processDefinition1.getId(), processInstance.getId()) // note the original id
            );

        // Triggering the instance event subscription should continue the case instance like before
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list())
            .extracting(Task::getName)
            .containsOnly("My task");

        inboundEventChannelAdapter.triggerTestEvent();

        // Ended thanks to boundary event
        assertProcessEnded(processInstance.getId());
        processInstance = runtimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getProcessDefinitionId()).isEqualTo(processDefinition2.getId());

        assertThat(runtimeService.createEventSubscriptionQuery().list())
            .extracting(EventSubscription::getEventType, EventSubscription::getProcessDefinitionId, EventSubscription::getProcessInstanceId)
            .containsOnly(
                    tuple("myEvent", processDefinition2.getId(), null),
                    tuple("myEvent", processDefinition2.getId(), processInstance.getId()) // triggering the test event started a new process
            );

        deploymentIdsForAutoCleanup.remove(deployment2.getId());

        // Removing the second definition should recreate the one from the first one
        // There won't be a process instance since we will do cascaded delete of the deployment
        repositoryService.deleteDeployment(deployment2.getId(), true);
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // nothing to do, isHistoryLevelAtLeast will execute the jobs
        }

        assertThat( runtimeService.createEventSubscriptionQuery().list())
                .extracting(EventSubscription::getEventType, EventSubscription::getProcessDefinitionId, EventSubscription::getProcessInstanceId)
                .containsOnly(
                        tuple("myEvent", processDefinition1.getId(), null)
                );
    }

    @Test
    public void testEventRegistrySubscriptionsShouldNotBeRecreatedOnNonLatestDeploymentDelete() {
        org.flowable.engine.repository.Deployment deployment1 = repositoryService.createDeployment()
            .addClasspathResource("org/flowable/engine/test/eventregistry/BpmnEventRegistryConsumerTest.testRedeploy.bpmn20.xml")
            .deploy();
        deploymentIdsForAutoCleanup.add(deployment1.getId());
        ProcessDefinition processDefinition1 = repositoryService.createProcessDefinitionQuery().deploymentId(deployment1.getId()).singleResult();

        // After deploying, there should be one eventsubscription: to start the instance
        assertThat( runtimeService.createEventSubscriptionQuery().list())
            .extracting(EventSubscription::getEventType, EventSubscription::getProcessDefinitionId, EventSubscription::getProcessInstanceId)
            .containsOnly(tuple("myEvent", processDefinition1.getId(), null));

        // After the instance is started, there should be one additional eventsubscription
        inboundEventChannelAdapter.triggerTestEvent();
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance.getProcessDefinitionId()).isEqualTo(processDefinition1.getId());

        assertThat( runtimeService.createEventSubscriptionQuery().list())
            .extracting(EventSubscription::getEventType, EventSubscription::getProcessDefinitionId, EventSubscription::getProcessInstanceId)
            .containsOnly(
                tuple("myEvent", processDefinition1.getId(), null),
                tuple("myEvent", processDefinition1.getId(), processInstance.getId())
            );

        // Redeploying the same definition:
        // Event subscription to start should reflect new definition id
        // Existing subscription for boundary event should remain
        org.flowable.engine.repository.Deployment deployment2 = repositoryService.createDeployment()
            .addClasspathResource("org/flowable/engine/test/eventregistry/BpmnEventRegistryConsumerTest.testRedeploy.bpmn20.xml")
            .deploy();
        deploymentIdsForAutoCleanup.add(deployment2.getId());
        ProcessDefinition processDefinition2 = repositoryService.createProcessDefinitionQuery().deploymentId(deployment2.getId()).singleResult();

        assertThat( runtimeService.createEventSubscriptionQuery().list())
            .extracting(EventSubscription::getEventType, EventSubscription::getProcessDefinitionId, EventSubscription::getProcessInstanceId)
            .containsOnly(
                tuple("myEvent", processDefinition2.getId(), null), // note the new definition id
                tuple("myEvent", processDefinition1.getId(), processInstance.getId()) // note the original id
            );

        // Triggering the instance event subscription should continue the case instance like before
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list())
            .extracting(Task::getName)
            .containsOnly("My task");

        inboundEventChannelAdapter.triggerTestEvent();

        // Ended thanks to boundary event
        assertProcessEnded(processInstance.getId());
        processInstance = runtimeService.createProcessInstanceQuery().singleResult();
        assertThat(processInstance).isNotNull();
        assertThat(processInstance.getProcessDefinitionId()).isEqualTo(processDefinition2.getId());

        assertThat(runtimeService.createEventSubscriptionQuery().list())
            .extracting(EventSubscription::getEventType, EventSubscription::getProcessDefinitionId, EventSubscription::getProcessInstanceId)
            .containsOnly(
                    tuple("myEvent", processDefinition2.getId(), null),
                    tuple("myEvent", processDefinition2.getId(), processInstance.getId()) // triggering the test event started a new process
            );

        deploymentIdsForAutoCleanup.remove(deployment1.getId());

        // Removing the second definition should recreate the one from the first one
        // There won't be a process instance since we will do cascaded delete of the deployment
        repositoryService.deleteDeployment(deployment1.getId(), true);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // nothing to do, isHistoryLevelAtLeast will execute the jobs
        }

        assertThat( runtimeService.createEventSubscriptionQuery().list())
                .extracting(EventSubscription::getEventType, EventSubscription::getProcessDefinitionId, EventSubscription::getProcessInstanceId)
                .containsOnly(
                        tuple("myEvent", processDefinition2.getId(), null),
                        tuple("myEvent", processDefinition2.getId(), processInstance.getId())
                );
    }

    private static class TestInboundEventChannelAdapter implements InboundEventChannelAdapter {

        public InboundChannelModel inboundChannelModel;
        public EventRegistry eventRegistry;

        @Override
        public void setInboundChannelModel(InboundChannelModel inboundChannelModel) {
            this.inboundChannelModel = inboundChannelModel;
        }

        @Override
        public void setEventRegistry(EventRegistry eventRegistry) {
            this.eventRegistry = eventRegistry;
        }

        public void triggerTestEvent() {
            triggerTestEvent(null);
        }

        public void triggerTestEvent(String customerId) {
            triggerTestEvent(customerId, null);
        }

        public void triggerOrderTestEvent(String orderId) {
            triggerTestEvent(null, orderId);
        }

        public void triggerTestEvent(String customerId, String orderId) {
            ObjectMapper objectMapper = new ObjectMapper();

            ObjectNode json = objectMapper.createObjectNode();
            json.put("type", "myEvent");
            if (customerId != null) {
                json.put("customerId", customerId);
            }

            if (orderId != null) {
                json.put("orderId", orderId);
            }
            json.put("payload1", "Hello World");
            json.put("payload2", new Random().nextInt());
            try {
                eventRegistry.eventReceived(inboundChannelModel, objectMapper.writeValueAsString(json));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

    }



}
