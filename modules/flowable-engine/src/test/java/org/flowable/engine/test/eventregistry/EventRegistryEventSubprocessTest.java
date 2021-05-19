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

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.InboundEventChannelAdapter;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.flowable.eventregistry.model.InboundChannelModel;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryImpl;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 * @author Filip Hrisafov
 */
public class EventRegistryEventSubprocessTest extends FlowableEventRegistryBpmnTestCase {
    
    protected TestInboundEventChannelAdapter inboundEventChannelAdapter;

    @BeforeEach
    protected void setUp() throws Exception {
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
    protected void tearDown() throws Exception {
        EventRepositoryService eventRepositoryService = getEventRepositoryService();
        List<EventDeployment> deployments = eventRepositoryService.createDeploymentQuery().list();
        for (EventDeployment eventDeployment : deployments) {
            eventRepositoryService.deleteDeployment(eventDeployment.getId());
        }
    }

    @Test
    @Deployment
    public void testNonInterruptingSubProcess() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("customerIdVar", "kermit");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variableMap);
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);
        
        inboundEventChannelAdapter.triggerTestEvent("notexisting");
        
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        inboundEventChannelAdapter.triggerTestEvent("kermit");
        
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(6);

        assertThat(taskService.createTaskQuery().count()).isEqualTo(2);
        assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);

        inboundEventChannelAdapter.triggerTestEvent("kermit");
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(9);

        assertThat(taskService.createTaskQuery().count()).isEqualTo(3);
        assertThat(createEventSubscriptionQuery().count()).isEqualTo(1);

        // now let's first complete the task in the main flow:
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("task").singleResult();
        taskService.complete(task.getId());

        assertThat(createEventSubscriptionQuery().count()).isZero();

        // we still have 7 executions:
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(7);

        // now let's complete the first task in the event subprocess
        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").list().get(0);
        taskService.complete(task.getId());

        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(4);

        // complete the second task in the event subprocess
        task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
        taskService.complete(task.getId());

        // done!
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/eventregistry/EventRegistryEventSubprocessTest.testNonInterruptingSubProcess.bpmn20.xml")
    public void testActivitiesForNonInterruptingSubProcess() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("customerIdVar", "kermit");
        runtimeService.startProcessInstanceByKey("process", variableMap);

        assertThat(runtimeService.createActivityInstanceQuery().list())
            .extracting(ActivityInstance::getActivityType, ActivityInstance::getActivityId)
            .containsExactlyInAnyOrder(
                tuple("startEvent", "theStart"),
                tuple("sequenceFlow", "flow1"),
                tuple("userTask", "task")
            );

        inboundEventChannelAdapter.triggerTestEvent("kermit");

        assertThat(runtimeService.createActivityInstanceQuery().list())
            .extracting(ActivityInstance::getActivityType, ActivityInstance::getActivityId)
            .containsExactlyInAnyOrder(
                tuple("startEvent", "theStart"),
                tuple("sequenceFlow", "flow1"),
                tuple("userTask", "task"),
                tuple("eventSubProcess", "eventRegistryEventSubProcess"),
                tuple("startEvent", "eventProcessStart"),
                tuple("sequenceFlow", "eventFlow1"),
                tuple("subProcess", "subProcess"),
                tuple("startEvent", "nestedStart"),
                tuple("sequenceFlow", "nestedFlow1"),
                tuple("userTask", "eventSubProcessTask")
            );

        // Complete the user task in the event sub process
        Task eventSubProcessTask = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
        assertThat(eventSubProcessTask).isNotNull();
        taskService.complete(eventSubProcessTask.getId());

        assertThat(runtimeService.createActivityInstanceQuery().list())
            .extracting(ActivityInstance::getActivityType, ActivityInstance::getActivityId)
            .containsExactlyInAnyOrder(
                tuple("startEvent", "theStart"),
                tuple("sequenceFlow", "flow1"),
                tuple("userTask", "task"),
                tuple("eventSubProcess", "eventRegistryEventSubProcess"),
                tuple("startEvent", "eventProcessStart"),
                tuple("sequenceFlow", "eventFlow1"),
                tuple("subProcess", "subProcess"),
                tuple("startEvent", "nestedStart"),
                tuple("sequenceFlow", "nestedFlow1"),
                tuple("userTask", "eventSubProcessTask"),
                tuple("sequenceFlow", "nestedFlow2"),
                tuple("endEvent", "nestedEnd"),
                tuple("sequenceFlow", "eventFlow2"),
                tuple("endEvent", "eventSubProcessEnd")
            );

        ActivityInstance eventSubProcessActivity = runtimeService.createActivityInstanceQuery()
            .activityType("eventSubProcess")
            .activityId("eventRegistryEventSubProcess")
            .singleResult();
        assertThat(eventSubProcessActivity).isNotNull();
        assertThat(eventSubProcessActivity.getEndTime()).isNotNull();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricActivityInstanceQuery().list())
                .extracting(HistoricActivityInstance::getActivityType, HistoricActivityInstance::getActivityId)
                .containsExactlyInAnyOrder(
                    tuple("startEvent", "theStart"),
                    tuple("sequenceFlow", "flow1"),
                    tuple("userTask", "task"),
                    tuple("eventSubProcess", "eventRegistryEventSubProcess"),
                    tuple("startEvent", "eventProcessStart"),
                    tuple("sequenceFlow", "eventFlow1"),
                    tuple("subProcess", "subProcess"),
                    tuple("startEvent", "nestedStart"),
                    tuple("sequenceFlow", "nestedFlow1"),
                    tuple("userTask", "eventSubProcessTask"),
                    tuple("sequenceFlow", "nestedFlow2"),
                    tuple("endEvent", "nestedEnd"),
                    tuple("sequenceFlow", "eventFlow2"),
                    tuple("endEvent", "eventSubProcessEnd")
                );
        }
    }

    @Test
    @Deployment
    public void testNonInterruptingSubProcessWithVariables() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("customerIdVar", "kermit");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variableMap);
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        assertThat(runtimeService.getVariables(processInstance.getId()))
                .containsOnly(
                        entry("customerIdVar", "kermit")
                );

        inboundEventChannelAdapter.triggerTestEvent("kermit", "order1");

        assertThat(runtimeService.getVariables(processInstance.getId()))
                .containsOnly(
                        entry("customerIdVar", "kermit"),
                        entry("orderIdVar", "order1")
                );
    }

    @Test
    @Deployment
    public void testInterruptingSubProcess() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("customerIdVar", "gonzo");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variableMap);
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        inboundEventChannelAdapter.triggerTestEvent("notexisting");
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);
        
        inboundEventChannelAdapter.triggerTestEvent("gonzo");
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(5);

        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        assertThat(createEventSubscriptionQuery().count()).isZero();

        // now let's complete the task in the event subprocess
        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").list().get(0);
        taskService.complete(task.getId());

        // done!
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/eventregistry/EventRegistryEventSubprocessTest.testInterruptingSubProcess.bpmn20.xml")
    public void testActivitiesForInterruptingSubProcess() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("customerIdVar", "kermit");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variableMap);

        assertThat(runtimeService.createActivityInstanceQuery().list())
            .extracting(ActivityInstance::getActivityType, ActivityInstance::getActivityId)
            .containsExactlyInAnyOrder(
                tuple("startEvent", "theStart"),
                tuple("sequenceFlow", "flow1"),
                tuple("userTask", "task")
            );

        inboundEventChannelAdapter.triggerTestEvent("kermit");

        assertThat(runtimeService.createActivityInstanceQuery().list())
            .extracting(ActivityInstance::getActivityType, ActivityInstance::getActivityId)
            .containsExactlyInAnyOrder(
                tuple("startEvent", "theStart"),
                tuple("sequenceFlow", "flow1"),
                tuple("userTask", "task"),
                tuple("eventSubProcess", "eventRegistryEventSubProcess"),
                tuple("startEvent", "eventProcessStart"),
                tuple("sequenceFlow", "eventFlow1"),
                tuple("subProcess", "subProcess"),
                tuple("startEvent", "nestedStart"),
                tuple("sequenceFlow", "nestedFlow1"),
                tuple("userTask", "eventSubProcessTask")
            );

        // Complete the user task in the event sub process
        Task eventSubProcessTask = taskService.createTaskQuery().taskDefinitionKey("eventSubProcessTask").singleResult();
        assertThat(eventSubProcessTask).isNotNull();
        taskService.complete(eventSubProcessTask.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricActivityInstanceQuery().list())
                .extracting(HistoricActivityInstance::getActivityType, HistoricActivityInstance::getActivityId)
                .containsExactlyInAnyOrder(
                    tuple("startEvent", "theStart"),
                    tuple("sequenceFlow", "flow1"),
                    tuple("userTask", "task"),
                    tuple("eventSubProcess", "eventRegistryEventSubProcess"),
                    tuple("startEvent", "eventProcessStart"),
                    tuple("sequenceFlow", "eventFlow1"),
                    tuple("subProcess", "subProcess"),
                    tuple("startEvent", "nestedStart"),
                    tuple("sequenceFlow", "nestedFlow1"),
                    tuple("userTask", "eventSubProcessTask"),
                    tuple("sequenceFlow", "nestedFlow2"),
                    tuple("endEvent", "nestedEnd"),
                    tuple("sequenceFlow", "eventFlow2"),
                    tuple("endEvent", "eventSubProcessEnd")
                );
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    public void testInterruptingSubProcessWithVariables() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("customerIdVar", "gonzo");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variableMap);
        assertThat(runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(3);

        assertThat(runtimeService.getVariables(processInstance.getId()))
                .containsOnly(
                        entry("customerIdVar", "gonzo")
                );

        inboundEventChannelAdapter.triggerTestEvent("gonzo", "order1");

        assertThat(runtimeService.getVariables(processInstance.getId()))
                .containsOnly(
                        entry("customerIdVar", "gonzo"),
                        entry("orderIdVar", "order1")
                );
    }


    private EventSubscriptionQueryImpl createEventSubscriptionQuery() {
        return new EventSubscriptionQueryImpl(processEngineConfiguration.getCommandExecutor(), processEngineConfiguration.getEventSubscriptionServiceConfiguration());
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

        public void triggerTestEvent(String customerId) {
            triggerTestEvent(customerId, null);
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
