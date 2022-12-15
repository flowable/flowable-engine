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
package org.flowable.eventregistry.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRegistryEvent;
import org.flowable.eventregistry.api.EventRegistryNonMatchingEventConsumer;
import org.flowable.eventregistry.api.EventRegistryProcessingInfo;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.model.ChannelModel;
import org.flowable.eventsubscription.api.EventSubscription;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@BpmnJmsEventTest
@TestPropertySource(properties = {
        "application.test.jms-queue=test-bpmn-queue",
        "application.test.another-jms-queue=another-bpmn-queue"
})
public class ProcessWithEventRegistryTest {
    
    @Autowired
    protected ProcessEngine processEngine;
    
    @Autowired
    protected RepositoryService repositoryService;
    
    @Autowired
    protected RuntimeService runtimeService;
    
    @Autowired
    protected ManagementService managementService;
    
    @Autowired
    protected TaskService taskService;

    @Autowired
    protected EventRegistryEngineConfiguration eventEngineConfiguration;
    
    @Autowired
    protected JmsTemplate jmsTemplate;
    
    @Test
    @Deployment(resources = { "org/flowable/eventregistry/integrationtest/testMultipleStartEvents.bpmn20.xml",
            "org/flowable/eventregistry/integrationtest/one.event",
            "org/flowable/eventregistry/integrationtest/another.event",
            "org/flowable/eventregistry/integrationtest/one.channel",
            "org/flowable/eventregistry/integrationtest/another.channel"})
    public void testMultipleStartEvents() {
        try {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("multipleStartEvents").singleResult();
            List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().processDefinitionId(processDefinition.getId()).list();
            
            EventSubscription eventSubscriptionOne = null;
            EventSubscription eventSubscriptionAnother = null;
            for (EventSubscription eventSubscription : eventSubscriptions) {
                if ("start1".equals(eventSubscription.getActivityId())) {
                    eventSubscriptionOne = eventSubscription;
                } else if ("start2".equals(eventSubscription.getActivityId())) {
                    eventSubscriptionAnother = eventSubscription;
                }
            }
            assertThat(eventSubscriptionAnother).isNotNull();
            assertThat(eventSubscriptionAnother.getActivityId()).isEqualTo("start2");
            assertThat(eventSubscriptionAnother.getEventType()).isEqualTo("another");
            
            assertThat(eventSubscriptionOne).isNotNull();
            assertThat(eventSubscriptionOne.getActivityId()).isEqualTo("start1");
            assertThat(eventSubscriptionOne.getEventType()).isEqualTo("one");
            
            jmsTemplate.convertAndSend("another-bpmn-queue", "{"
                + "    \"payload1\": \"kermit\","
                + "    \"payload2\": 123"
                + "}", messageProcessor -> {
                    
                messageProcessor.setStringProperty("headerProperty1", "123a");
                return messageProcessor;
            });

            await("receive events")
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    assertThat(runtimeService.createExecutionQuery().processDefinitionKey("multipleStartEvents").activityId("receive2").count()).isEqualTo(1);
                });
            
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("multipleStartEvents").singleResult();
            Object varValue1 = runtimeService.getVariable(processInstance.getId(), "anotherValue1");
            assertThat(varValue1).isEqualTo("kermit");
            Object varValue2 = runtimeService.getVariable(processInstance.getId(), "anotherValue2");
            assertThat(varValue2).isEqualTo(123);
            
            Execution execution = runtimeService.createExecutionQuery().processDefinitionKey("multipleStartEvents").activityId("receive2").singleResult();
            runtimeService.trigger(execution.getId());
            assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(0);
            
            jmsTemplate.convertAndSend("test-bpmn-queue", "{"
                + "    \"payload1\": \"fozzie\","
                + "    \"payload2\": 456"
                + "}", messageProcessor -> {
                    
                messageProcessor.setStringProperty("headerProperty1", "456b");
                return messageProcessor;
            });

            await("receive events")
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    assertThat(runtimeService.createExecutionQuery().processDefinitionKey("multipleStartEvents").activityId("receive1").count()).isEqualTo(1);
                });
            
            processInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("multipleStartEvents").singleResult();
            varValue1 = runtimeService.getVariable(processInstance.getId(), "value1");
            assertThat(varValue1).isEqualTo("fozzie");
            varValue2 = runtimeService.getVariable(processInstance.getId(), "value2");
            assertThat(varValue2).isEqualTo(456);
            
            execution = runtimeService.createExecutionQuery().processDefinitionKey("multipleStartEvents").activityId("receive1").singleResult();
            runtimeService.trigger(execution.getId());
            assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(0);

        } finally {
            List<EventDeployment> eventDeployments = getEventRepositoryService().createDeploymentQuery().list();
            for (EventDeployment eventDeployment : eventDeployments) {
                getEventRepositoryService().deleteDeployment(eventDeployment.getId());
            }
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/eventregistry/integrationtest/testReceiveEventTaskWithCorrelationAndPayload.bpmn20.xml",
            "org/flowable/eventregistry/integrationtest/one-header-correlation.event",
            "org/flowable/eventregistry/integrationtest/one.channel"})
    public void testHeaderCorrelationEvent() {
        try {
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("process")
                    .variable("customerIdVar", "123a")
                    .start();
            
            jmsTemplate.convertAndSend("test-bpmn-queue", "{"
                + "    \"payload1\": \"kermit\","
                + "    \"payload2\": 123"
                + "}", messageProcessor -> {
                    
                messageProcessor.setStringProperty("headerProperty1", "123a");
                messageProcessor.setStringProperty("headerProperty2", "header1");
                return messageProcessor;
            });

            await("receive events")
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
                });
            
            assertThat(runtimeService.getVariable(processInstance.getId(), "headerValue1")).isEqualTo("123a");
            assertThat(runtimeService.getVariable(processInstance.getId(), "headerValue2")).isEqualTo("header1");
            assertThat(runtimeService.getVariable(processInstance.getId(), "value1")).isEqualTo("kermit");
            assertThat(runtimeService.getVariable(processInstance.getId(), "value2")).isEqualTo(123);
            
            taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());
            assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            
            ProcessInstance anotherProcessInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("process")
                    .variable("customerIdVar", "456b")
                    .start();
            
            jmsTemplate.convertAndSend("test-bpmn-queue", "{"
                + "    \"payload1\": \"fozzie\","
                + "    \"payload2\": 456"
                + "}", messageProcessor -> {
                    
                messageProcessor.setStringProperty("headerProperty1", "123b");
                messageProcessor.setStringProperty("headerProperty2", "header2");
                return messageProcessor;
            });
            
            TestNonMatchingEventConsumer nonMatchingEventConsumer = new TestNonMatchingEventConsumer();
            getEventRegistryEngineConfiguration().setNonMatchingEventConsumer(nonMatchingEventConsumer);
            
            await("receive events")
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    assertThat(nonMatchingEventConsumer.getNonMatchingEvent()).isNotNull();
                });
            
            assertThat(taskService.createTaskQuery().processInstanceId(anotherProcessInstance.getId()).count()).isZero();
            
            nonMatchingEventConsumer.setNonMatchingEvent(null);
            
            jmsTemplate.convertAndSend("test-bpmn-queue", "{"
                + "    \"payload1\": \"fozzie\","
                + "    \"payload2\": 456"
                + "}", messageProcessor -> {
                    
                messageProcessor.setStringProperty("headerProperty1", "456b");
                messageProcessor.setStringProperty("headerProperty2", "header2");
                return messageProcessor;
            });
            
            await("receive events")
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    assertThat(taskService.createTaskQuery().processInstanceId(anotherProcessInstance.getId()).count()).isEqualTo(1);
                });
            
            assertThat(nonMatchingEventConsumer.getNonMatchingEvent()).isNull();
            
            assertThat(runtimeService.getVariable(anotherProcessInstance.getId(), "headerValue1")).isEqualTo("456b");
            assertThat(runtimeService.getVariable(anotherProcessInstance.getId(), "headerValue2")).isEqualTo("header2");
            assertThat(runtimeService.getVariable(anotherProcessInstance.getId(), "value1")).isEqualTo("fozzie");
            assertThat(runtimeService.getVariable(anotherProcessInstance.getId(), "value2")).isEqualTo(456);

        } finally {
            List<EventDeployment> eventDeployments = getEventRepositoryService().createDeploymentQuery().list();
            for (EventDeployment eventDeployment : eventDeployments) {
                getEventRepositoryService().deleteDeployment(eventDeployment.getId());
            }
        }
    }
    
    @Test
    @Deployment(resources = { "org/flowable/eventregistry/integrationtest/testReceiveEventTaskWithCorrelationAndFullPayload.bpmn20.xml",
            "org/flowable/eventregistry/integrationtest/one-fullpayload.event",
            "org/flowable/eventregistry/integrationtest/one.channel"})
    public void testFullPayloadEvent() {
        try {
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("process")
                    .variable("customerIdVar", "123a")
                    .start();
            
            jmsTemplate.convertAndSend("test-bpmn-queue", "{"
                + "    \"payload1\": \"kermit\","
                + "    \"payload2\": 123"
                + "}", messageProcessor -> {
                    
                messageProcessor.setStringProperty("headerProperty1", "123a");
                return messageProcessor;
            });

            await("receive events")
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
                });
            
            ObjectNode payloadNode = (ObjectNode) runtimeService.getVariable(processInstance.getId(), "fullPayloadValue");
            assertThat(payloadNode.get("payload1").asText()).isEqualTo("kermit");
            assertThat(payloadNode.get("payload2").asInt()).isEqualTo(123);

        } finally {
            List<EventDeployment> eventDeployments = getEventRepositoryService().createDeploymentQuery().list();
            for (EventDeployment eventDeployment : eventDeployments) {
                getEventRepositoryService().deleteDeployment(eventDeployment.getId());
            }
        }
    }
    
    @Test
    @Deployment(resources = { "org/flowable/eventregistry/integrationtest/testReceiveEventTaskWithCorrelationAndFullPayload.bpmn20.xml",
            "org/flowable/eventregistry/integrationtest/one-fullpayload.event",
            "org/flowable/eventregistry/integrationtest/one.channel"})
    public void testFullPayloadEventWithDuplicateDeployment() {
        try {
            repositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/integrationtest/testReceiveEventTaskWithCorrelationAndFullPayload.bpmn20.xml")
                .addClasspathResource("org/flowable/eventregistry/integrationtest/one-fullpayload.event")
                .addClasspathResource("org/flowable/eventregistry/integrationtest/one.channel")
                .deploy();
            
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("process")
                    .variable("customerIdVar", "123a")
                    .start();
            
            jmsTemplate.convertAndSend("test-bpmn-queue", "{"
                + "    \"payload1\": \"kermit\","
                + "    \"payload2\": 123"
                + "}", messageProcessor -> {
                    
                messageProcessor.setStringProperty("headerProperty1", "123a");
                return messageProcessor;
            });

            await("receive events")
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
                });
            
            ObjectNode payloadNode = (ObjectNode) runtimeService.getVariable(processInstance.getId(), "fullPayloadValue");
            assertThat(payloadNode.get("payload1").asText()).isEqualTo("kermit");
            assertThat(payloadNode.get("payload2").asInt()).isEqualTo(123);

        } finally {
            List<EventDeployment> eventDeployments = getEventRepositoryService().createDeploymentQuery().list();
            for (EventDeployment eventDeployment : eventDeployments) {
                getEventRepositoryService().deleteDeployment(eventDeployment.getId());
            }
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/eventregistry/integrationtest/testReceiveEventTaskWithCorrelationAndFullPayload.bpmn20.xml",
            "org/flowable/eventregistry/integrationtest/one-fullpayload.event",
            "org/flowable/eventregistry/integrationtest/one.channel"})
    public void testFullPayloadEventWithDuplicateDeploymentWithChangeDetectionRun() throws InterruptedException {
        try {
            repositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/integrationtest/testReceiveEventTaskWithCorrelationAndFullPayload.bpmn20.xml")
                .addClasspathResource("org/flowable/eventregistry/integrationtest/one-fullpayload.event")
                .addClasspathResource("org/flowable/eventregistry/integrationtest/one.channel")
                .deploy();

            EventRepositoryService eventRepositoryService = eventEngineConfiguration.getEventRepositoryService();
            List<ChannelDefinition> channels = eventRepositoryService.createChannelDefinitionQuery()
                    .list();

            channels.sort(Comparator.comparing(ChannelDefinition::getVersion).reversed());

            eventRepositoryService.getChannelModelById(channels.get(0).getId());
            eventRepositoryService.getChannelModelById(channels.get(1).getId());


            eventEngineConfiguration.getEventRegistryChangeDetectionManager().detectChanges();

            Thread.sleep(1000);

            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("process")
                    .variable("customerIdVar", "123a")
                    .start();


            jmsTemplate.convertAndSend("test-bpmn-queue", "{"
                + "    \"payload1\": \"kermit\","
                + "    \"payload2\": 123"
                + "}", messageProcessor -> {

                messageProcessor.setStringProperty("headerProperty1", "123a");
                return messageProcessor;
            });

            await("receive events")
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
                });

            ObjectNode payloadNode = (ObjectNode) runtimeService.getVariable(processInstance.getId(), "fullPayloadValue");
            assertThat(payloadNode.get("payload1").asText()).isEqualTo("kermit");
            assertThat(payloadNode.get("payload2").asInt()).isEqualTo(123);

        } finally {
            List<EventDeployment> eventDeployments = getEventRepositoryService().createDeploymentQuery().list();
            for (EventDeployment eventDeployment : eventDeployments) {
                getEventRepositoryService().deleteDeployment(eventDeployment.getId());
            }
        }
    }


    @Test
    @Deployment(resources = { "org/flowable/eventregistry/integrationtest/testReceiveEventTaskWithCorrelationAndPayload.bpmn20.xml",
            "org/flowable/eventregistry/integrationtest/one-header-correlation.event",
            "org/flowable/eventregistry/integrationtest/one.channel"})
    public void testChannelDeployedMultipleTimes() {
        try {
            ProcessInstance instance1 = runtimeService.createProcessInstanceBuilder().processDefinitionKey("process")
                    .variable("customerIdVar", "123a")
                    .start();

            jmsTemplate.convertAndSend("test-bpmn-queue", "{"
                    + "    \"payload1\": \"kermit\","
                    + "    \"payload2\": 123"
                    + "}", messageProcessor -> {

                messageProcessor.setStringProperty("headerProperty1", "123a");
                return messageProcessor;
            });

            await("receive events")
                    .atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(200))
                    .untilAsserted(() -> {
                        assertThat(taskService.createTaskQuery().processInstanceId(instance1.getId()).count()).isEqualTo(1);
                    });

            assertThat(runtimeService.getVariables(instance1.getId()))
                    .contains(
                            entry("value1", "kermit"),
                            entry("value2", 123)
                    );

            taskService.complete(taskService.createTaskQuery().processInstanceId(instance1.getId()).singleResult().getId());
            assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(instance1.getId()).count()).isZero();

            repositoryService.createDeployment()
                .addClasspathResource("org/flowable/eventregistry/integrationtest/one-v2.channel")
                .deploy();

            ProcessInstance instance2 = runtimeService.createProcessInstanceBuilder().processDefinitionKey("process")
                    .variable("customerIdVar", "123b")
                    .start();

            jmsTemplate.convertAndSend("test-bpmn-queue-v2", "{"
                    + "    \"payload1\": \"fozzie\","
                    + "    \"payload2\": 124"
                    + "}", messageProcessor -> {

                messageProcessor.setStringProperty("headerProperty1", "123b");
                return messageProcessor;
            });

            await("receive events")
                    .atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(200))
                    .untilAsserted(() -> {
                        assertThat(taskService.createTaskQuery().processInstanceId(instance2.getId()).count()).isEqualTo(1);
                    });

            assertThat(runtimeService.getVariables(instance2.getId()))
                    .contains(
                            entry("value1", "fozzie"),
                            entry("value2", 124)
                    );

            taskService.complete(taskService.createTaskQuery().processInstanceId(instance2.getId()).singleResult().getId());
            assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(instance2.getId()).count()).isZero();

            ChannelDefinition channel1 = getEventRepositoryService().createChannelDefinitionQuery().channelVersion(1).singleResult();
            ChannelModel channelModel = getEventRepositoryService().getChannelModelById(channel1.getId());
            assertThat(channelModel).isNotNull();

            ProcessInstance instance3 = runtimeService.createProcessInstanceBuilder().processDefinitionKey("process")
                    .variable("customerIdVar", "123c")
                    .start();

            jmsTemplate.convertAndSend("test-bpmn-queue-v2", "{"
                    + "    \"payload1\": \"piggie\","
                    + "    \"payload2\": 125"
                    + "}", messageProcessor -> {

                messageProcessor.setStringProperty("headerProperty1", "123c");
                return messageProcessor;
            });

            await("receive events")
                    .atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(200))
                    .untilAsserted(() -> {
                        assertThat(taskService.createTaskQuery().processInstanceId(instance3.getId()).count()).isEqualTo(1);
                    });

            assertThat(runtimeService.getVariables(instance3.getId()))
                    .contains(
                            entry("value1", "piggie"),
                            entry("value2", 125)
                    );

            taskService.complete(taskService.createTaskQuery().processInstanceId(instance3.getId()).singleResult().getId());
            assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(instance3.getId()).count()).isZero();

        } finally {
            List<EventDeployment> eventDeployments = getEventRepositoryService().createDeploymentQuery().list();
            for (EventDeployment eventDeployment : eventDeployments) {
                getEventRepositoryService().deleteDeployment(eventDeployment.getId());
            }
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/eventregistry/integrationtest/testStartProcessWithSystemEvent.bpmn20.xml",
            "org/flowable/eventregistry/integrationtest/testSendSystemEvent.bpmn20.xml",
            "org/flowable/eventregistry/integrationtest/issueEvent.event"})
    public void testMultiInstanceSendSystemEvent() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode issueArray = objectMapper.createArrayNode();
            addIssue(issueArray, "1", "bug");
            addIssue(issueArray, "2", "task");
            addIssue(issueArray, "3", "question");
            addIssue(issueArray, "4", "question");
            addIssue(issueArray, "5", "bug");
            addIssue(issueArray, "6", "task");
            addIssue(issueArray, "7", "question");
            addIssue(issueArray, "8", "question");
            addIssue(issueArray, "9", "bug");
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("sendEventProcess")
                    .variable("issues", issueArray)
                    .start();
            
            JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngine.getProcessEngineConfiguration(), managementService, 10000, 500);
            
            List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processDefinitionKey("startProcessWithSystemEvent").list();
            assertThat(processInstances).hasSize(9);
            
        } finally {
            List<EventDeployment> eventDeployments = getEventRepositoryService().createDeploymentQuery().list();
            for (EventDeployment eventDeployment : eventDeployments) {
                getEventRepositoryService().deleteDeployment(eventDeployment.getId());
            }
        }
    }
    
    protected EventRepositoryService getEventRepositoryService() {
        return getEventRegistryEngineConfiguration().getEventRepositoryService();
    }
    
    protected EventRegistryEngineConfiguration getEventRegistryEngineConfiguration() {
        return (EventRegistryEngineConfiguration) 
                processEngine.getProcessEngineConfiguration().getEngineConfigurations().get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
    }
    
    protected void addIssue(ArrayNode issueArray, String issueNo, String issueType) {
        ObjectNode issueNode = issueArray.addObject();
        issueNode.put("issueNo", issueNo);
        issueNode.put("issueType", issueType);
    }
    
    protected class TestNonMatchingEventConsumer implements EventRegistryNonMatchingEventConsumer {

        protected EventRegistryEvent nonMatchingEvent;
        
        @Override
        public void handleNonMatchingEvent(EventRegistryEvent event, EventRegistryProcessingInfo eventRegistryProcessingInfo) {
            nonMatchingEvent = event;
        }
     
        public EventRegistryEvent getNonMatchingEvent() {
            return nonMatchingEvent;
        }

        public void setNonMatchingEvent(EventRegistryEvent nonMatchingEvent) {
            this.nonMatchingEvent = nonMatchingEvent;
        }
    }
}
