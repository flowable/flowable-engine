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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.spring.impl.test.FlowableSpringExtension;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = ProcessWithDynamicChannelProcessorTestConfiguration.class)
@ExtendWith(FlowableSpringExtension.class)
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "application.test.jms-queue=test-queue"
})
public class ProcessWithDynamicChannelProcessorTest {

    @Autowired
    protected ProcessEngine processEngine;

    @Autowired
    protected RuntimeService runtimeService;

    @Autowired
    protected TaskService taskService;

    @SpyBean
    protected JmsTemplate jmsTemplate;

    @Test
    @Deployment(resources = { "org/flowable/eventregistry/integrationtest/testSendAndReceiveEventTaskWithCorrelationAndPayload.bpmn20.xml",
            "org/flowable/eventregistry/integrationtest/one-header-correlation.event",
            "org/flowable/eventregistry/integrationtest/one.channel",
            "org/flowable/eventregistry/integrationtest/two.channel",
            "org/flowable/eventregistry/integrationtest/two-outbound.channel"
    })
    public void testHeaderCorrelationEvent() {
        try {
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
                    .untilAsserted(() ->
                            assertTrue(runtimeService.createProcessInstanceQuery()
                                    .processDefinitionKey("process")
                                    .variableValueEquals("customerIdVar", "123a")
                                    .count() > 0)
                    );
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("process")
                    .variableValueEquals("customerIdVar", "123a")
                    .singleResult();

            assertThat(runtimeService.getVariable(processInstance.getId(), "headerValue1")).isNull();
            assertThat(runtimeService.getVariable(processInstance.getId(), "headerValue2")).isEqualTo("header1");
            assertThat(runtimeService.getVariable(processInstance.getId(), "value1")).isEqualTo("kermit");
            assertThat(runtimeService.getVariable(processInstance.getId(), "value2")).isEqualTo(123);

            Task activeTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            //FIXME: activeTask == null
            taskService.complete(activeTask.getId());

            assertThat(runtimeService.getVariable(processInstance.getId(), "value1")).isEqualTo("your-fake-reply");
            assertThat(runtimeService.getVariable(processInstance.getId(), "value2")).isEqualTo(123);

            assertThat(runtimeService.getVariable(processInstance.getId(), "headerValue1")).isEqualTo("123a");

            Task finalTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            taskService.complete(finalTask.getId());
            assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();

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

}
