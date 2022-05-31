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
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;

import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRegistryEvent;
import org.flowable.eventregistry.api.EventRegistryNonMatchingEventConsumer;
import org.flowable.eventregistry.api.EventRegistryProcessingInfo;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@BpmnJmsEventTest
@TestPropertySource(properties = {
        "application.test.jms-queue=test-queue"
})
public class ProcessWithEventRegistryTest {
    
    @Autowired
    protected ProcessEngine processEngine;
    
    @Autowired
    protected RuntimeService runtimeService;
    
    @Autowired
    protected ManagementService managementService;
    
    @Autowired
    protected TaskService taskService;
    
    @Autowired
    protected JmsTemplate jmsTemplate;

    @Test
    @Deployment(resources = { "org/flowable/eventregistry/integrationtest/testReceiveEventTaskWithCorrelationAndPayload.bpmn20.xml",
            "org/flowable/eventregistry/integrationtest/one-header-correlation.event",
            "org/flowable/eventregistry/integrationtest/one.channel"})
    public void testHeaderCorrelationEvent() {
        try {
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("process")
                    .variable("customerIdVar", "123a")
                    .start();
            
            jmsTemplate.convertAndSend("test-queue", "{"
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
            
            jmsTemplate.convertAndSend("test-queue", "{"
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
            
            jmsTemplate.convertAndSend("test-queue", "{"
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
            
            jmsTemplate.convertAndSend("test-queue", "{"
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
