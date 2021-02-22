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
package org.flowable.engine.test.api.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.IdentityLink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test case for all {@link FlowableEvent}s related to process definitions.
 *
 * @author Frederik Heremans
 */
public class IdentityLinkEventsTest extends PluggableFlowableTestCase {

    private TestFlowableEntityEventListener listener;

    /**
     * Check identity links on process definitions.
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testProcessDefinitionIdentityLinkEvents() throws Exception {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").singleResult();

        assertThat(processDefinition).isNotNull();

        // Add candidate user and group
        repositoryService.addCandidateStarterUser(processDefinition.getId(), "kermit");
        repositoryService.addCandidateStarterGroup(processDefinition.getId(), "sales");
        assertThat(listener.getEventsReceived()).hasSize(4);

        FlowableEntityEvent event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        assertThat(event.getEntity()).isInstanceOf(IdentityLink.class);
        assertThat(((IdentityLink) event.getEntity()).getProcessDefinitionId()).isEqualTo(processDefinition.getId());

        event = (FlowableEntityEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);

        event = (FlowableEntityEvent) listener.getEventsReceived().get(2);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        assertThat(event.getEntity()).isInstanceOf(IdentityLink.class);
        assertThat(((IdentityLink) event.getEntity()).getProcessDefinitionId()).isEqualTo(processDefinition.getId());

        event = (FlowableEntityEvent) listener.getEventsReceived().get(3);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
        listener.clearEventsReceived();

        // Delete identity links
        repositoryService.deleteCandidateStarterUser(processDefinition.getId(), "kermit");
        repositoryService.deleteCandidateStarterGroup(processDefinition.getId(), "sales");
        assertThat(listener.getEventsReceived()).hasSize(2);
        event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);
        assertThat(event.getEntity()).isInstanceOf(IdentityLink.class);
        assertThat(((IdentityLink) event.getEntity()).getProcessDefinitionId()).isEqualTo(processDefinition.getId());

        event = (FlowableEntityEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);
        assertThat(event.getEntity()).isInstanceOf(IdentityLink.class);
        assertThat(((IdentityLink) event.getEntity()).getProcessDefinitionId()).isEqualTo(processDefinition.getId());
    }

    /**
     * Check identity links on process instances.
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testProcessInstanceIdentityLinkEvents() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        // Add identity link
        runtimeService.addUserIdentityLink(processInstance.getId(), "kermit", "test");
        assertThat(listener.getEventsReceived()).hasSize(2);

        FlowableEntityEvent event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        assertThat(event.getEntity()).isInstanceOf(IdentityLink.class);
        IdentityLink link = (IdentityLink) event.getEntity();
        assertThat(link.getProcessInstanceId()).isEqualTo(processInstance.getId());
        assertThat(link.getUserId()).isEqualTo("kermit");
        assertThat(link.getType()).isEqualTo("test");

        event = (FlowableEntityEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);

        listener.clearEventsReceived();

        // Deleting process should delete identity link
        runtimeService.deleteProcessInstance(processInstance.getId(), "test");
        assertThat(listener.getEventsReceived()).hasSize(1);

        event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);
        assertThat(event.getEntity()).isInstanceOf(IdentityLink.class);
        link = (IdentityLink) event.getEntity();
        assertThat(link.getUserId()).isEqualTo("kermit");
        assertThat(link.getType()).isEqualTo("test");

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
    }

    /**
     * Check identity links on process instances.
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testTaskIdentityLinks() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        // Add identity link
        taskService.addCandidateUser(task.getId(), "kermit");
        taskService.addCandidateGroup(task.getId(), "sales");

        // Three events are received, since the user link on the task also
        // creates an involvement in the process
        assertThat(listener.getEventsReceived()).hasSize(6);

        FlowableEntityEvent event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        assertThat(event.getEntity()).isInstanceOf(IdentityLink.class);
        IdentityLink link = (IdentityLink) event.getEntity();
        assertThat(link.getUserId()).isEqualTo("kermit");
        assertThat(link.getType()).isEqualTo("candidate");
        assertThat(link.getTaskId()).isEqualTo(task.getId());

        event = (FlowableEntityEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
        assertThat(link.getUserId()).isEqualTo("kermit");
        assertThat(link.getType()).isEqualTo("candidate");

        event = (FlowableEntityEvent) listener.getEventsReceived().get(4);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        assertThat(event.getEntity()).isInstanceOf(IdentityLink.class);
        link = (IdentityLink) event.getEntity();
        assertThat(link.getGroupId()).isEqualTo("sales");
        assertThat(link.getType()).isEqualTo("candidate");
        assertThat(link.getTaskId()).isEqualTo(task.getId());

        event = (FlowableEntityEvent) listener.getEventsReceived().get(5);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
        assertThat(link.getGroupId()).isEqualTo("sales");
        assertThat(link.getType()).isEqualTo("candidate");

        listener.clearEventsReceived();

        // Deleting process should delete identity link
        runtimeService.deleteProcessInstance(processInstance.getId(), "test");
        assertThat(listener.getEventsReceived()).hasSize(3);

        event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);
        event = (FlowableEntityEvent) listener.getEventsReceived().get(1);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);
        event = (FlowableEntityEvent) listener.getEventsReceived().get(2);
        assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
    }

    /**
     * Check deletion of links on process instances.
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testProcessInstanceIdentityDeleteCandidateGroupEvents() throws Exception {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        // Add identity link
        taskService.addCandidateUser(task.getId(), "kermit");
        taskService.addCandidateGroup(task.getId(), "sales");

        // Three events are received, since the user link on the task also creates an involvement in the process. See previous test
        assertThat(listener.getEventsReceived()).hasSize(6);

        listener.clearEventsReceived();
        taskService.deleteCandidateUser(task.getId(), "kermit");
        assertThat(listener.getEventsReceived()).hasSize(1);

        listener.clearEventsReceived();
        taskService.deleteCandidateGroup(task.getId(), "sales");
        assertThat(listener.getEventsReceived()).hasSize(1);
    }

    @BeforeEach
    protected void setUp() {

        listener = new TestFlowableEntityEventListener(IdentityLink.class);
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @AfterEach
    protected void tearDown() throws Exception {

        if (listener != null) {
            listener.clearEventsReceived();
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }
}
