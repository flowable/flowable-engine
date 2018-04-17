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

package org.activiti.engine.test.bpmn.event.message;

import java.util.List;

import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.repository.DeploymentProperties;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.EventSubscription;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;

/**
 * @author Daniel Meyer
 */
public class MessageStartEventTest extends PluggableFlowableTestCase {

    public void testDeploymentCreatesSubscriptions() {
        String deploymentId = repositoryService
                .createDeployment()
                .addClasspathResource("org/activiti/engine/test/bpmn/event/message/MessageStartEventTest.testSingleMessageStartEvent.bpmn20.xml")
                .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, Boolean.TRUE)
                .deploy()
                .getId();

        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();

        assertEquals(1, eventSubscriptions.size());

        repositoryService.deleteDeployment(deploymentId);
    }

    public void testSameMessageNameFails() {
        String deploymentId = repositoryService
                .createDeployment()
                .addClasspathResource("org/activiti/engine/test/bpmn/event/message/MessageStartEventTest.testSingleMessageStartEvent.bpmn20.xml")
                .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, Boolean.TRUE)
                .deploy()
                .getId();
        try {
            repositoryService
                    .createDeployment()
                    .addClasspathResource("org/activiti/engine/test/bpmn/event/message/otherProcessWithNewInvoiceMessage.bpmn20.xml")
                    .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, Boolean.TRUE)
                    .deploy();
            fail("exception expected");
        } catch (FlowableException e) {
            assertTrue(e.getMessage().contains("there already is a message event subscription for the message with name"));
        }

        // clean db:
        repositoryService.deleteDeployment(deploymentId);

    }

    public void testSameMessageNameInSameProcessFails() {
        try {
            repositoryService
                    .createDeployment()
                    .addClasspathResource("org/activiti/engine/test/bpmn/event/message/testSameMessageNameInSameProcessFails.bpmn20.xml")
                    .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, Boolean.TRUE)
                    .deploy();
            fail("exception expected: Cannot have more than one message event subscription with name 'newInvoiceMessage' for scope");
        } catch (FlowableException e) {
            e.printStackTrace();
        }
    }

    public void testUpdateProcessVersionCancelsSubscriptions() {
        String deploymentId = repositoryService
                .createDeployment()
                .addClasspathResource("org/activiti/engine/test/bpmn/event/message/MessageStartEventTest.testSingleMessageStartEvent.bpmn20.xml")
                .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, Boolean.TRUE)
                .deploy()
                .getId();

        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();

        assertEquals(1, eventSubscriptions.size());
        assertEquals(1, processDefinitions.size());

        String newDeploymentId = repositoryService
                .createDeployment()
                .addClasspathResource("org/activiti/engine/test/bpmn/event/message/MessageStartEventTest.testSingleMessageStartEvent.bpmn20.xml")
                .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, Boolean.TRUE)
                .deploy()
                .getId();

        List<EventSubscription> newEventSubscriptions = runtimeService.createEventSubscriptionQuery().list();
        List<ProcessDefinition> newProcessDefinitions = repositoryService.createProcessDefinitionQuery().list();

        assertEquals(1, newEventSubscriptions.size());
        assertEquals(2, newProcessDefinitions.size());
        for (ProcessDefinition processDefinition : newProcessDefinitions) {
            if (processDefinition.getVersion() == 1) {
                for (EventSubscription subscription : newEventSubscriptions) {
                    assertFalse(subscription.getConfiguration().equals(processDefinition.getId()));
                }
            } else {
                for (EventSubscription subscription : newEventSubscriptions) {
                    assertEquals(subscription.getConfiguration(), processDefinition.getId());
                }
            }
        }
        assertFalse(eventSubscriptions.equals(newEventSubscriptions));

        repositoryService.deleteDeployment(deploymentId);
        repositoryService.deleteDeployment(newDeploymentId);
    }

    @Deployment
    public void testSingleMessageStartEvent() {

        // using startProcessInstanceByMessage triggers the message start event

        ProcessInstance processInstance = runtimeService.startProcessInstanceByMessage("newInvoiceMessage");

        assertFalse(processInstance.isEnded());

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertNotNull(task);

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());

        // using startProcessInstanceByKey also triggers the message event, if there is a single start event

        processInstance = runtimeService.startProcessInstanceByKey("singleMessageStartEvent");

        assertFalse(processInstance.isEnded());

        task = taskService.createTaskQuery().singleResult();
        assertNotNull(task);

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment
    public void testMessageStartEventAndNoneStartEvent() {

        // using startProcessInstanceByKey triggers the none start event

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

        assertFalse(processInstance.isEnded());

        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("taskAfterNoneStart").singleResult();
        assertNotNull(task);

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());

        // using startProcessInstanceByMessage triggers the message start event

        processInstance = runtimeService.startProcessInstanceByMessage("newInvoiceMessage");

        assertFalse(processInstance.isEnded());

        task = taskService.createTaskQuery().taskDefinitionKey("taskAfterMessageStart").singleResult();
        assertNotNull(task);

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());

    }

    @Deployment
    public void testMultipleMessageStartEvents() {

        // sending newInvoiceMessage

        ProcessInstance processInstance = runtimeService.startProcessInstanceByMessage("newInvoiceMessage");

        assertFalse(processInstance.isEnded());

        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("taskAfterMessageStart").singleResult();
        assertNotNull(task);

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());

        // sending newInvoiceMessage2

        processInstance = runtimeService.startProcessInstanceByMessage("newInvoiceMessage2");

        assertFalse(processInstance.isEnded());

        task = taskService.createTaskQuery().taskDefinitionKey("taskAfterMessageStart2").singleResult();
        assertNotNull(task);

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());

        // starting the process using startProcessInstanceByKey is possible, the first message start event will be the default:
        processInstance = runtimeService.startProcessInstanceByKey("testProcess");
        assertFalse(processInstance.isEnded());
        task = taskService.createTaskQuery().taskDefinitionKey("taskAfterMessageStart").singleResult();
        assertNotNull(task);
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

}
