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
package org.flowable.spring.test.eventregistry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventregistry.api.EventDefinition;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Tijs Rademakers
 */
@ContextConfiguration("classpath:flowable-context.xml")
public class EventWithSpringBeanTest extends SpringFlowableTestCase {

    @Autowired
    protected TestInboundEventChannelAdapter inboundEventChannelAdapter;

    @BeforeEach
    protected void setUp() {
        setupTestChannel();
    }

    @AfterEach
    protected void tearDown() {
        EventRegistryEngineConfiguration eventEngineConfiguration = (EventRegistryEngineConfiguration) processEngineConfiguration.getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
        List<EventDeployment> deployments = eventEngineConfiguration.getEventRepositoryService().createDeploymentQuery().list();
        for (EventDeployment eventDeployment : deployments) {
            eventEngineConfiguration.getEventRepositoryService().deleteDeployment(eventDeployment.getId());
        }
    }

    protected void setupTestChannel() {
        EventRegistryEngineConfiguration eventEngineConfiguration = (EventRegistryEngineConfiguration) processEngineConfiguration.getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);

        eventEngineConfiguration.getEventRepositoryService().createInboundChannelModelBuilder()
                .key("test-channel")
                .resourceName("test.channel")
                .channelAdapter("${inboundEventChannelAdapter}")
                .jsonDeserializer()
                .detectEventKeyUsingJsonField("type")
                .jsonFieldsMapDirectlyToPayload()
                .deploy();
    }

    @Test
    @Deployment(resources = { "org/flowable/spring/test/eventregistry/taskWithEventProcess.bpmn20.xml",
            "org/flowable/spring/test/eventregistry/simpleEvent.event" })
    public void testEventOnUserTask() {

        EventRegistryEngineConfiguration eventEngineConfiguration = (EventRegistryEngineConfiguration) processEngineConfiguration.getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);

        EventDefinition eventDefinition = eventEngineConfiguration.getEventRepositoryService().createEventDefinitionQuery().singleResult();
        assertThat(eventDefinition).isNotNull();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("taskWithEventProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().activityId("eventBoundary").singleResult();
        assertThat(eventSubscription).isNotNull();
        assertThat(eventSubscription.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(eventSubscription.getEventType()).isEqualTo("myEvent");

        inboundEventChannelAdapter.triggerTestEvent();
        Task afterTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(afterTask.getTaskDefinitionKey()).isEqualTo("theTaskAfter");

        eventSubscription = runtimeService.createEventSubscriptionQuery().activityId("eventBoundary").singleResult();
        assertThat(eventSubscription).isNull();

        taskService.complete(afterTask.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/spring/test/eventregistry/taskWithEventProcess.bpmn20.xml" })
    public void testEventOnUserTaskWithoutVariablesSeparateDeployments() {

        EventRegistryEngineConfiguration eventEngineConfiguration = (EventRegistryEngineConfiguration) processEngineConfiguration.getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);

        EventRepositoryService eventRepositoryService = eventEngineConfiguration.getEventRepositoryService();
        EventDeployment eventDeployment = eventRepositoryService.createDeployment()
                .addClasspathResource("org/flowable/spring/test/eventregistry/simpleEvent.event").deploy();

        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("taskWithEventProcess");
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();

            EventSubscription eventSubscription = runtimeService.createEventSubscriptionQuery().activityId("eventBoundary").singleResult();
            assertThat(eventSubscription).isNotNull();
            assertThat(eventSubscription.getProcessInstanceId()).isEqualTo(processInstance.getId());
            assertThat(eventSubscription.getEventType()).isEqualTo("myEvent");

            inboundEventChannelAdapter.triggerTestEvent();
            Task afterTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(afterTask.getTaskDefinitionKey()).isEqualTo("theTaskAfter");

            eventSubscription = runtimeService.createEventSubscriptionQuery().activityId("eventBoundary").singleResult();
            assertThat(eventSubscription).isNull();

            taskService.complete(afterTask.getId());

            assertProcessEnded(processInstance.getId());

        } finally {
            eventRepositoryService.deleteDeployment(eventDeployment.getId());
        }
    }
}
