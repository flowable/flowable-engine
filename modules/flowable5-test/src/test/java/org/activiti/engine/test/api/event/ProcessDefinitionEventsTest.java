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

import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;

/**
 * Test case for all {@link FlowableEvent}s related to process definitions.
 * 
 * @author Frederik Heremans
 */
public class ProcessDefinitionEventsTest extends PluggableFlowableTestCase {

    private TestMultipleFlowableEventListener listener;

    /**
     * Test create, update and delete events of process definitions.
     */
    @Deployment(resources = { "org/activiti/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testProcessDefinitionEvents() throws Exception {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("oneTaskProcess")
                .singleResult();

        assertNotNull(processDefinition);

        // Check create-event
        assertEquals(2, listener.getEventsReceived().size());
        assertTrue(listener.getEventsReceived().get(0) instanceof FlowableEntityEvent);

        FlowableEntityEvent event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ENTITY_CREATED, event.getType());
        assertEquals(processDefinition.getId(), ((ProcessDefinition) event.getEntity()).getId());

        event = (FlowableEntityEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ENTITY_INITIALIZED, event.getType());
        assertEquals(processDefinition.getId(), ((ProcessDefinition) event.getEntity()).getId());
        listener.clearEventsReceived();

        // Check update event when category is updated
        repositoryService.setProcessDefinitionCategory(processDefinition.getId(), "test");
        assertEquals(1, listener.getEventsReceived().size());
        assertTrue(listener.getEventsReceived().get(0) instanceof FlowableEntityEvent);

        event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ENTITY_UPDATED, event.getType());
        assertEquals(processDefinition.getId(), ((ProcessDefinition) event.getEntity()).getId());
        assertEquals("test", ((ProcessDefinition) event.getEntity()).getCategory());
        listener.clearEventsReceived();

        // Check update event when suspended/activated
        repositoryService.suspendProcessDefinitionById(processDefinition.getId());
        repositoryService.activateProcessDefinitionById(processDefinition.getId());

        assertEquals(2, listener.getEventsReceived().size());
        event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
        assertEquals(processDefinition.getId(), ((ProcessDefinition) event.getEntity()).getId());
        assertEquals(FlowableEngineEventType.ENTITY_SUSPENDED, event.getType());
        event = (FlowableEntityEvent) listener.getEventsReceived().get(1);
        assertEquals(FlowableEngineEventType.ENTITY_ACTIVATED, event.getType());
        assertEquals(processDefinition.getId(), ((ProcessDefinition) event.getEntity()).getId());
        listener.clearEventsReceived();

        // Check delete event when category is updated
        repositoryService.deleteDeployment(processDefinition.getDeploymentId(), true);
        deploymentIdFromDeploymentAnnotation = null;

        assertEquals(1, listener.getEventsReceived().size());
        assertTrue(listener.getEventsReceived().get(0) instanceof FlowableEntityEvent);

        event = (FlowableEntityEvent) listener.getEventsReceived().get(0);
        assertEquals(FlowableEngineEventType.ENTITY_DELETED, event.getType());
        assertEquals(processDefinition.getId(), ((ProcessDefinition) event.getEntity()).getId());
        listener.clearEventsReceived();
    }

    /**
     * test sequence of events for process definition with timer start event
     */
    @Deployment(resources = { "org/activiti/engine/test/bpmn/event/timer/StartTimerEventTest.testDurationStartTimerEvent.bpmn20.xml" })
    public void testTimerStartEventDeployment() {
        org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl activiti5ProcessConfig = (org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl) processEngineConfiguration.getFlowable5CompatibilityHandler().getRawProcessConfiguration();

        ProcessDefinition processDefinition = activiti5ProcessConfig.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey("startTimerEventExample").singleResult();
        FlowableEntityEvent processDefinitionCreated = ActivitiEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_CREATED, processDefinition);

        Job timer = activiti5ProcessConfig.getManagementService().createTimerJobQuery().singleResult();
        FlowableEntityEvent timerCreated = ActivitiEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_CREATED, timer);
        assertSequence(processDefinitionCreated, timerCreated);
        listener.clearEventsReceived();
    }

    private void assertSequence(FlowableEntityEvent before, FlowableEntityEvent after) {
        int beforeIndex = 0;
        int afterIndex = 0;
        for (int index = 0; index < listener.getEventsReceived().size(); index++) {
            FlowableEvent activitiEvent = listener.getEventsReceived().get(index);

            if (isEqual(before, activitiEvent)) {
                beforeIndex = index;
            }
            if (isEqual(after, activitiEvent)) {
                afterIndex = index;
            }
        }
        assertTrue(beforeIndex < afterIndex);
    }

    /**
     * equals is not implemented.
     */
    private boolean isEqual(FlowableEntityEvent event1, FlowableEvent activitiEvent) {
        if (activitiEvent instanceof FlowableEntityEvent && event1.getType().equals(activitiEvent.getType())) {
            FlowableEntityEvent activitiEntityEvent = (FlowableEntityEvent) activitiEvent;
            if (activitiEntityEvent.getEntity().getClass().equals(event1.getEntity().getClass())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void initializeServices() {
        super.initializeServices();

        listener = new TestMultipleFlowableEventListener();
        listener.setEventClasses(FlowableEntityEvent.class);
        listener.setEntityClasses(ProcessDefinition.class, org.activiti.engine.impl.persistence.entity.TimerJobEntity.class);

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
