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

import org.flowable.common.engine.api.constant.ReferenceTypes;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class BpmnEventRegistryConsumerTest extends AbstractBpmnEventRegistryConsumerTest {

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
    public void testBoundaryEventWith3CorrelationsAllCombinations() {
        // The BPMN has boundary events for each combination, the correlation matches a new task will be created
        getEventRepositoryService().createEventModelBuilder()
                .key("customer")
                .resourceName("customer.event")
                .correlationParameter("id", EventPayloadTypes.STRING)
                .correlationParameter("first", EventPayloadTypes.STRING)
                .correlationParameter("last", EventPayloadTypes.STRING)
                .deploy();

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("multiCorrelationProcess")
                .variable("customerId", "1234")
                .variable("customerFirstName", "John")
                .variable("customerLastName", "Doe")
                .start();

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list())
                .extracting(Task::getName)
                .containsExactlyInAnyOrder("User task");

        ObjectNode event = processEngineConfiguration.getObjectMapper().createObjectNode()
                .put("type", "customer")
                .put("id", "1235")
                .put("first", "Jane")
                .put("last", "Doele");
        inboundEventChannelAdapter.triggerTestEvent(event);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list())
                .extracting(Task::getName)
                .containsExactlyInAnyOrder("User task");

        event = processEngineConfiguration.getObjectMapper().createObjectNode()
                .put("type", "customer")
                .put("id", "1234")
                .put("first", "John")
                .put("last", "Doe");
        inboundEventChannelAdapter.triggerTestEvent(event);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list())
                .extracting(Task::getName)
                .containsExactlyInAnyOrder(
                        "User task",
                        "Customer ID Task",
                        "Customer ID and First Name Task",
                        "Customer ID and Last Name Task",
                        "Customer ID, First Name and Last Name Task",
                        "First Name Task",
                        "First Name and Last Name Task",
                        "Last Name Task"
                );
    }

    @Test
    @Deployment
    public void testBoundaryEventWith4CorrelationsAllCombinations() {
        // The BPMN has boundary events for each combination, the correlation matches a new task will be created
        getEventRepositoryService().createEventModelBuilder()
                .key("testEvent")
                .resourceName("test.event")
                .correlationParameter("id", EventPayloadTypes.STRING)
                .correlationParameter("orderId", EventPayloadTypes.STRING)
                .correlationParameter("firstName", EventPayloadTypes.STRING)
                .correlationParameter("lastName", EventPayloadTypes.STRING)
                .deploy();

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("multiCorrelationProcess")
                .variable("customerId", "customer-1")
                .variable("orderId", "order-1")
                .variable("firstName", "John")
                .variable("lastName", "Doe")
                .start();

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list())
                .extracting(Task::getName)
                .containsExactlyInAnyOrder("User task");

        ObjectNode event = processEngineConfiguration.getObjectMapper().createObjectNode()
                .put("type", "testEvent")
                .put("id", "customer-2")
                .put("orderId", "order-2")
                .put("firstName", "Jane")
                .put("lastName", "Doele");
        inboundEventChannelAdapter.triggerTestEvent(event);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list())
                .extracting(Task::getName)
                .containsExactlyInAnyOrder("User task");

        event = processEngineConfiguration.getObjectMapper().createObjectNode()
                .put("type", "testEvent")
                .put("id", "customer-1")
                .put("orderId", "order-1")
                .put("firstName", "John")
                .put("lastName", "Doe");
        inboundEventChannelAdapter.triggerTestEvent(event);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list())
                .extracting(Task::getName)
                .containsExactlyInAnyOrder(
                        "User task",
                        "ID",
                        "Order ID",
                        "First Name",
                        "Last Name",
                        "ID and Order ID",
                        "ID and First Name",
                        "ID and Last Name",
                        "Order ID and First Name",
                        "Order ID and Last Name",
                        "First Name and Last Name",
                        "ID, Order ID and First Name",
                        "ID, Order ID and Last Name",
                        "ID, First Name and Last Name",
                        "Order ID, First Name and Last Name",
                        "ID, Order ID, First Name and Last Name"
                );
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/eventregistry/BpmnEventRegistryConsumerTest.testBoundaryEventWith4CorrelationsAllCombinations.bpmn20.xml")
    public void testBoundaryEventWith4CorrelationsSubsetOfCombinations() {
        // The BPMN has boundary events for each combination, the correlation matches a new task will be created
        // In this test we are going to match a subset of the tasks
        getEventRepositoryService().createEventModelBuilder()
                .key("testEvent")
                .resourceName("test.event")
                .correlationParameter("id", EventPayloadTypes.STRING)
                .correlationParameter("orderId", EventPayloadTypes.STRING)
                .correlationParameter("firstName", EventPayloadTypes.STRING)
                .correlationParameter("lastName", EventPayloadTypes.STRING)
                .deploy();

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("multiCorrelationProcess")
                .variable("customerId", "customer-1")
                .variable("orderId", "order-1")
                .variable("firstName", "John")
                .variable("lastName", "Doe")
                .start();

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list())
                .extracting(Task::getName)
                .containsExactlyInAnyOrder("User task");

        ObjectNode event = processEngineConfiguration.getObjectMapper().createObjectNode()
                .put("type", "testEvent")
                .put("id", "customer-2")
                .put("orderId", "order-1")
                .put("firstName", "Jane")
                .put("lastName", "Doe");
        inboundEventChannelAdapter.triggerTestEvent(event);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list())
                .extracting(Task::getName)
                .containsExactlyInAnyOrder(
                        "User task",
                        "Order ID",
                        "Last Name",
                        "Order ID and Last Name"
                );

        event = processEngineConfiguration.getObjectMapper().createObjectNode()
                .put("type", "testEvent")
                .put("id", "customer-2")
                .put("orderId", "order-2")
                .put("firstName", "John")
                .put("lastName", "Smith");
        inboundEventChannelAdapter.triggerTestEvent(event);

        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list())
                .extracting(Task::getName)
                .containsExactlyInAnyOrder(
                        "User task",
                        "Order ID",
                        "Last Name",
                        "Order ID and Last Name",
                        // The first name is from the John Smith trigger
                        "First Name"
                );
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
    public void testReceiveEventTaskSkipExpression() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("_FLOWABLE_SKIP_EXPRESSION_ENABLED", true);
        variables.put("skipExpression", true);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variables);

        assertThat(runtimeService.createEventSubscriptionQuery().activityId("task").singleResult()).isNull();
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
        
        processInstance = runtimeService.startProcessInstanceByKey("process");
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
        
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeployment()
            .addClasspathResource("org/flowable/engine/test/eventregistry/BpmnEventRegistryConsumerTest.testStartOnlyOneInstance.bpmn20.xml")
            .deploy();
        deploymentIdsForAutoCleanup.add(deployment.getId());

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

}
