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
package org.flowable.standalone.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.api.event.StaticTestFlowableEventListener;
import org.flowable.engine.test.api.event.TestFlowableEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for event-listeners that are registered on a process-definition scope, rather than on the global engine-wide scope, declared in the BPMN XML.
 *
 * @author Frederik Heremans
 */
public class ProcessDefinitionScopedEventListenerDefinitionTest extends ResourceFlowableTestCase {

    public ProcessDefinitionScopedEventListenerDefinitionTest() {
        super("org/flowable/standalone/event/flowable-eventlistener.cfg.xml");
    }

    protected TestFlowableEventListener testListenerBean;

    /**
     * Test to verify listeners defined in the BPMN xml are added to the process definition and are active.
     */
    @Test
    @Deployment
    public void testProcessDefinitionListenerDefinition() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testEventListeners");
        assertThat(testListenerBean).isNotNull();

        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        // Check if the listener (defined as bean) received events (only creation, not other events)
        assertThat(testListenerBean.getEventsReceived()).isNotEmpty();
        for (FlowableEvent event : testListenerBean.getEventsReceived()) {
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        }

        // Second event received should be creation of Process instance (first is process definition create event)
        assertThat(testListenerBean.getEventsReceived().get(1)).isInstanceOf(FlowableEntityEvent.class);
        FlowableEntityEvent event = (FlowableEntityEvent) testListenerBean.getEventsReceived().get(1);
        assertThat(event.getEntity()).isInstanceOf(ProcessInstance.class);
        assertThat(((ProcessInstance) event.getEntity()).getId()).isEqualTo(processInstance.getId());

        // Check if listener, defined by classname, received all events
        List<FlowableEvent> events = StaticTestFlowableEventListener.getEventsReceived();
        assertThat(events).isNotEmpty();
        assertThat(events)
                .extracting(FlowableEvent::getType)
                .contains(FlowableEngineEventType.ENTITY_CREATED, FlowableEngineEventType.ENTITY_DELETED);
    }

    /**
     * Test to verify listeners defined in the BPMN xml with invalid class/delegateExpression values cause an exception when process is started.
     */
    @Test
    public void testProcessDefinitionListenerDefinitionError() throws Exception {

        // Deploy process with expression which references an unexisting bean
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/standalone/event/invalidEventListenerExpression.bpmn20.xml").deploy();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testInvalidEventExpression");
        assertThat(processInstance).isNotNull();
        repositoryService.deleteDeployment(deployment.getId(), true);

        // Deploy process with listener which references an unexisting class
        deployment = repositoryService.createDeployment().addClasspathResource("org/flowable/standalone/event/invalidEventListenerClass.bpmn20.xml").deploy();
        processInstance = runtimeService.startProcessInstanceByKey("testInvalidEventClass");
        repositoryService.deleteDeployment(deployment.getId(), true);
    }

    /**
     * Test to verify if event listeners defined in the BPMN XML which have illegal event-types cause an exception on deploy.
     */
    @Test
    public void testProcessDefinitionListenerDefinitionIllegalType() throws Exception {
        // In case deployment doesn't fail, we delete the deployment in the
        // finally block to ensure clean DB for subsequent tests
        AtomicReference<org.flowable.engine.repository.Deployment> deployment = null;
        try {
            assertThatThrownBy(() -> {
                deployment.set(repositoryService.createDeployment().addClasspathResource("org/flowable/standalone/event/invalidEventListenerType.bpmn20.xml")
                        .deploy());
            })
                    .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                    .hasMessage("Invalid event-type: invalid");
        } finally {
            if (deployment != null && deployment.get() != null) {
                repositoryService.deleteDeployment(deployment.get().getId(), true);
            }
        }
    }

    /**
     * Test to verify listeners defined in the BPMN xml are added to the process definition and are active, for all entity types
     */
    @Test
    @Deployment
    public void testProcessDefinitionListenerDefinitionEntities() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testEventListeners");
        assertThat(processInstance).isNotNull();
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        // Attachment entity
        TestFlowableEventListener theListener = (TestFlowableEventListener) processEngineConfiguration.getBeans().get("testAttachmentEventListener");
        assertThat(theListener).isNotNull();
        assertThat(theListener.getEventsReceived()).isEmpty();

        taskService.createAttachment("test", task.getId(), processInstance.getId(), "test", "test", "url");
        assertThat(theListener.getEventsReceived())
                .extracting(FlowableEvent::getType)
                .containsExactly(FlowableEngineEventType.ENTITY_CREATED, FlowableEngineEventType.ENTITY_INITIALIZED);
    }

    @BeforeEach
    protected void setUp() throws Exception {
        testListenerBean = (TestFlowableEventListener) processEngineConfiguration.getBeans().get("testEventListener");
    }
}
