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

import java.util.List;

import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.impl.jobexecutor.AsyncSendEventJobHandler;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.DeploymentId;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class SendInternalEventTaskTest extends FlowableEventRegistryBpmnTestCase {

    @AfterEach
    void tearDown(@DeploymentId String deploymentId) {
        if (deploymentId != null) {
            EventRepositoryService eventRepositoryService = getEventRepositoryService();
            List<EventDeployment> eventDeployments = eventRepositoryService.createDeploymentQuery()
                    .parentDeploymentId(deploymentId)
                    .list();

            for (EventDeployment eventDeployment : eventDeployments) {
                eventRepositoryService.deleteDeployment(eventDeployment.getId());
            }
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/eventregistry/SendInternalEventTaskTest.testSendEvent.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/SendInternalEventTaskTest.testSendEventProcessStartWithPayload.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/SendInternalEventTaskTest.testSendEventProcessStartOtherWithPayload.bpmn20.xml",
            "org/flowable/engine/test/eventregistry/SendInternalEventTaskTest.simple.event"
    })
    public void testSendEvent() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("sendEvent")
                .variable("customerId", "kermit")
                .variable("customerName", "Kermit the Frog")
                .variable("eventKey", "simpleTest")
                .variable("action", "start")
                .start();

        assertThat(runtimeService.createProcessInstanceQuery().list())
                .extracting(ProcessInstance::getProcessDefinitionKey)
                .containsExactlyInAnyOrder("sendEvent");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        taskService.complete(task.getId());

        assertThat(runtimeService.createProcessInstanceQuery().list())
                .extracting(ProcessInstance::getProcessDefinitionKey)
                .containsExactlyInAnyOrder("sendEvent");

        Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getJobHandlerType()).isEqualTo(AsyncSendEventJobHandler.TYPE);
        assertThat(job.getElementId()).isEqualTo("sendEventTask");

        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 5000, 200);

        assertThat(runtimeService.createProcessInstanceQuery().list())
                .extracting(ProcessInstance::getProcessDefinitionKey)
                .containsExactlyInAnyOrder("sendEvent", "eventProcessStart");

        ProcessInstance eventProcessStart = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey("eventProcessStart")
                .includeProcessVariables()
                .singleResult();

        assertThat(eventProcessStart).isNotNull();
        assertThat(eventProcessStart.getProcessVariables())
                .containsOnly(
                        entry("customerId", "kermit"),
                        entry("customerName", "Kermit the Frog")
                );

        processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("sendEvent")
                .variable("customerId", "kermit")
                .variable("customerName", "Kermit")
                .variable("eventKey", "simpleTest")
                .variable("action", "startOther")
                .start();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        taskService.complete(task.getId());

        job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(job).isNotNull();
        assertThat(job.getJobHandlerType()).isEqualTo(AsyncSendEventJobHandler.TYPE);
        assertThat(job.getElementId()).isEqualTo("sendEventTask");

        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 5000, 200);

        assertThat(runtimeService.createProcessInstanceQuery().list())
                .extracting(ProcessInstance::getProcessDefinitionKey)
                .containsExactlyInAnyOrder("sendEvent", "sendEvent", "eventProcessStart", "eventProcessStartOther");

        ProcessInstance eventProcessStartOther = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey("eventProcessStartOther")
                .includeProcessVariables()
                .singleResult();

        assertThat(eventProcessStartOther).isNotNull();
        assertThat(eventProcessStartOther.getProcessVariables())
                .containsOnly(
                        entry("customer", "kermit"),
                        entry("name", "Kermit")
                );

    }

    @Override
    protected EventRepositoryService getEventRepositoryService() {
        return getEventRegistryEngineConfiguration().getEventRepositoryService();
    }

    @Override
    protected EventRegistry getEventRegistry() {
        return getEventRegistryEngineConfiguration().getEventRegistry();
    }

    @Override
    protected EventRegistryEngineConfiguration getEventRegistryEngineConfiguration() {
        return (EventRegistryEngineConfiguration) processEngineConfiguration.getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
    }
}
