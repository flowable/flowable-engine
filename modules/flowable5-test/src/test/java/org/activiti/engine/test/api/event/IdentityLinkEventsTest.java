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
package org.activiti.engine.test.api.event;

import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.IdentityLink;

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
    @Deployment(resources = { "org/activiti/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testProcessDefinitionIdentityLinkEvents() throws Exception {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("oneTaskProcess").singleResult();

        assertNotNull(processDefinition);

        // Add candidate user and group
        repositoryService.addCandidateStarterUser(processDefinition.getId(), "kermit");
        repositoryService.addCandidateStarterGroup(processDefinition.getId(), "sales");
        assertEquals(4, listener.getEventsReceived().size());

        FlowableEngineEntityEvent event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, event.getType());
        assertTrue(event.getEntity() instanceof IdentityLink);
        assertEquals(processDefinition.getId(), event.getProcessDefinitionId());
        assertNull(event.getProcessInstanceId());
        assertNull(event.getExecutionId());

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ENTITY_INITIALIZED, event.getType());

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(2);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, event.getType());
        assertTrue(event.getEntity() instanceof IdentityLink);
        assertEquals(processDefinition.getId(), event.getProcessDefinitionId());
        assertNull(event.getProcessInstanceId());
        assertNull(event.getExecutionId());

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(3);
        assertEquals(FlowableEngineEventType.ENTITY_INITIALIZED, event.getType());
        listener.clearEventsReceived();

        // Delete identity links
        repositoryService.deleteCandidateStarterUser(processDefinition.getId(), "kermit");
        repositoryService.deleteCandidateStarterGroup(processDefinition.getId(), "sales");
        assertEquals(2, listener.getEventsReceived().size());
        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ENTITY_DELETED, event.getType());
        assertTrue(event.getEntity() instanceof IdentityLink);
        assertEquals(processDefinition.getId(), event.getProcessDefinitionId());
        assertNull(event.getProcessInstanceId());
        assertNull(event.getExecutionId());
        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ENTITY_DELETED, event.getType());
        assertTrue(event.getEntity() instanceof IdentityLink);
        assertEquals(processDefinition.getId(), event.getProcessDefinitionId());
        assertNull(event.getProcessInstanceId());
        assertNull(event.getExecutionId());
    }

    /**
     * Check identity links on process instances.
     */
    @Deployment(resources = { "org/activiti/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testProcessInstanceIdentityLinkEvents() throws Exception {
        Authentication.setAuthenticatedUserId(null);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        // Add identity link
        runtimeService.addUserIdentityLink(processInstance.getId(), "kermit", "test");
        assertEquals(2, listener.getEventsReceived().size());

        FlowableEngineEntityEvent event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, event.getType());
        assertTrue(event.getEntity() instanceof IdentityLink);
        assertEquals(processInstance.getId(), event.getProcessInstanceId());
        assertEquals(processInstance.getId(), event.getExecutionId());
        assertEquals(processInstance.getProcessDefinitionId(), event.getProcessDefinitionId());
        IdentityLink link = (IdentityLink) event.getEntity();
        assertEquals("kermit", link.getUserId());
        assertEquals("test", link.getType());

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ENTITY_INITIALIZED, event.getType());

        listener.clearEventsReceived();

        // Deleting process should delete identity link
        runtimeService.deleteProcessInstance(processInstance.getId(), "test");
        assertEquals(1, listener.getEventsReceived().size());

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ENTITY_DELETED, event.getType());
        assertTrue(event.getEntity() instanceof IdentityLink);
        link = (IdentityLink) event.getEntity();
        assertEquals("kermit", link.getUserId());
        assertEquals("test", link.getType());
    }

    /**
     * Check identity links on process instances.
     */
    @Deployment(resources = { "org/activiti/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testTaskIdentityLinks() throws Exception {
        Authentication.setAuthenticatedUserId(null);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);

        // Add identity link
        taskService.addCandidateUser(task.getId(), "kermit");
        taskService.addCandidateGroup(task.getId(), "sales");

        // Three events are received, since the user link on the task also creates an involvement in the process
        assertEquals(6, listener.getEventsReceived().size());

        FlowableEngineEntityEvent event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, event.getType());
        assertTrue(event.getEntity() instanceof IdentityLink);
        IdentityLink link = (IdentityLink) event.getEntity();
        assertEquals("kermit", link.getUserId());
        assertEquals("candidate", link.getType());
        assertEquals(task.getId(), link.getTaskId());
        assertEquals(task.getExecutionId(), event.getExecutionId());
        assertEquals(task.getProcessDefinitionId(), event.getProcessDefinitionId());
        assertEquals(task.getProcessInstanceId(), event.getProcessInstanceId());

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ENTITY_INITIALIZED, event.getType());
        assertEquals("kermit", link.getUserId());
        assertEquals("candidate", link.getType());

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(4);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, event.getType());
        assertTrue(event.getEntity() instanceof IdentityLink);
        link = (IdentityLink) event.getEntity();
        assertEquals("sales", link.getGroupId());
        assertEquals("candidate", link.getType());
        assertEquals(task.getId(), link.getTaskId());
        assertEquals(task.getExecutionId(), event.getExecutionId());
        assertEquals(task.getProcessDefinitionId(), event.getProcessDefinitionId());
        assertEquals(task.getProcessInstanceId(), event.getProcessInstanceId());
        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(5);
        assertEquals(FlowableEngineEventType.ENTITY_INITIALIZED, event.getType());
        assertEquals("sales", link.getGroupId());
        assertEquals("candidate", link.getType());

        listener.clearEventsReceived();

        // Deleting process should delete identity link
        runtimeService.deleteProcessInstance(processInstance.getId(), "test");
        assertEquals(3, listener.getEventsReceived().size());

        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ENTITY_DELETED, event.getType());
        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ENTITY_DELETED, event.getType());
        event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(2);
        assertEquals(FlowableEngineEventType.ENTITY_DELETED, event.getType());
    }

    /**
     * Check deletion of links on process instances.
     */
    @Deployment(resources = { "org/activiti/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testProcessInstanceIdentityDeleteCandidateGroupEvents() throws Exception {
        Authentication.setAuthenticatedUserId(null);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(task);

        // Add identity link
        taskService.addCandidateUser(task.getId(), "kermit");
        taskService.addCandidateGroup(task.getId(), "sales");

        // Three events are received, since the user link on the task also creates an involvement in the process. See previous test
        assertEquals(6, listener.getEventsReceived().size());

        listener.clearEventsReceived();
        taskService.deleteCandidateUser(task.getId(), "kermit");
        assertEquals(1, listener.getEventsReceived().size());

        listener.clearEventsReceived();
        taskService.deleteCandidateGroup(task.getId(), "sales");
        assertEquals(1, listener.getEventsReceived().size());
    }

    @Override
    protected void initializeServices() {
        super.initializeServices();

        listener = new TestFlowableEntityEventListener(IdentityLink.class);
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        if (listener != null) {
            listener.clearEventsReceived();
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }
}
