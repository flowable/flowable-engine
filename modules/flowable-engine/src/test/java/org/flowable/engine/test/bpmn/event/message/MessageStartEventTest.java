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

package org.flowable.engine.test.bpmn.event.message;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.Calendar;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.api.EventSubscriptionQuery;
import org.flowable.eventsubscription.service.impl.persistence.entity.MessageEventSubscriptionEntity;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class MessageStartEventTest extends PluggableFlowableTestCase {

    @Test
    public void testDeploymentCreatesSubscriptions() {
        String deploymentId1 = repositoryService.createDeployment()
            .addClasspathResource("org/flowable/engine/test/bpmn/event/message/MessageStartEventTest.testSingleMessageStartEvent.bpmn20.xml")
            .deploy().getId();
        ProcessDefinition processDefinition1 = repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId1).singleResult();

        Assertions.assertThat(runtimeService.createEventSubscriptionQuery().list())
            .extracting(EventSubscription::getEventType, EventSubscription::getProcessDefinitionId, EventSubscription::getProcessInstanceId)
            .containsOnly(tuple(MessageEventSubscriptionEntity.EVENT_TYPE, processDefinition1.getId(), null));

        String deploymentId2 = repositoryService.createDeployment()
            .addClasspathResource("org/flowable/engine/test/bpmn/event/message/MessageStartEventTest.testSingleMessageStartEvent.bpmn20.xml")
            .deploy().getId();
        ProcessDefinition processDefinition2 = repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId2).singleResult();

        Assertions.assertThat(runtimeService.createEventSubscriptionQuery().list())
            .extracting(EventSubscription::getEventType, EventSubscription::getProcessDefinitionId, EventSubscription::getProcessInstanceId)
            .containsOnly(tuple(MessageEventSubscriptionEntity.EVENT_TYPE, processDefinition2.getId(), null)); // Note the changed definition id

        repositoryService.deleteDeployment(deploymentId1);
        repositoryService.deleteDeployment(deploymentId2);
    }

    @Test
    public void testSameMessageNameFails() {
        String deploymentId = repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/message/MessageStartEventTest.testSingleMessageStartEvent.bpmn20.xml")
                .deploy().getId();
        assertThatThrownBy(() -> repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/message/otherProcessWithNewInvoiceMessage.bpmn20.xml").deploy())
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("there already is a message event subscription for the message with name");

        // clean db:
        repositoryService.deleteDeployment(deploymentId);

    }

    @Test
    public void testSameMessageNameInSameProcessFails() {
        assertThatThrownBy(() -> repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/message/testSameMessageNameInSameProcessFails.bpmn20.xml").deploy())
                .as("exception expected: Cannot have more than one message event subscription with name 'newInvoiceMessage' for scope")
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    public void testUpdateProcessVersionCancelsSubscriptions() {
        String deploymentId = repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/message/MessageStartEventTest.testSingleMessageStartEvent.bpmn20.xml")
                .deploy().getId();

        List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery().list();
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();

        assertThat(eventSubscriptions).hasSize(1);
        assertThat(processDefinitions).hasSize(1);

        String newDeploymentId = repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/event/message/MessageStartEventTest.testSingleMessageStartEvent.bpmn20.xml")
                .deploy().getId();

        List<EventSubscription> newEventSubscriptions = runtimeService.createEventSubscriptionQuery().list();
        List<ProcessDefinition> newProcessDefinitions = repositoryService.createProcessDefinitionQuery().list();

        assertThat(newEventSubscriptions).hasSize(1);
        assertThat(newProcessDefinitions).hasSize(2);
        for (ProcessDefinition processDefinition : newProcessDefinitions) {
            if (processDefinition.getVersion() == 1) {
                for (EventSubscription subscription : newEventSubscriptions) {
                    assertThat(subscription.getConfiguration()).isNotEqualTo(processDefinition.getId());
                }
            } else {
                for (EventSubscription subscription : newEventSubscriptions) {
                    assertThat(processDefinition.getId()).isEqualTo(subscription.getConfiguration());
                }
            }
        }
        assertThat(eventSubscriptions).isNotEqualTo(newEventSubscriptions);

        repositoryService.deleteDeployment(deploymentId);
        repositoryService.deleteDeployment(newDeploymentId);
    }

    @Test
    @Deployment
    public void testSingleMessageStartEvent() {

        // using startProcessInstanceByMessage triggers the message start event

        ProcessInstance processInstance = runtimeService.startProcessInstanceByMessage("newInvoiceMessage");

        assertThat(processInstance.isEnded()).isFalse();

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());

        // using startProcessInstanceByKey also triggers the message event, if there is a single start event

        processInstance = runtimeService.startProcessInstanceByKey("singleMessageStartEvent");

        assertThat(processInstance.isEnded()).isFalse();

        task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
        
        // start process instance again after clearing the process definition cache (force deployment)
        
        processEngineConfiguration.getProcessDefinitionCache().clear();
        
        processInstance = runtimeService.startProcessInstanceByMessage("newInvoiceMessage");

        assertThat(processInstance.isEnded()).isFalse();

        task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    @Deployment
    public void testBusinessKeySet() {
        ProcessInstance pi1 = runtimeService.startProcessInstanceByKey("start-by-message-1", "business-key-123");
        assertThat(runtimeService.createProcessInstanceQuery()
                .processInstanceId(pi1.getProcessInstanceId())
                .singleResult()
                .getBusinessKey())
                .isEqualTo("business-key-123");

        ProcessInstance pi2 = runtimeService.startProcessInstanceByMessage("start-by-message", "business-key-456");
        // This step fails as the businessKey is null
        assertThat(runtimeService.createProcessInstanceQuery()
                .processInstanceId(pi2.getProcessInstanceId())
                .singleResult()
                .getBusinessKey())
                .isEqualTo("business-key-456");
    }

    @Test
    @Deployment
    public void testMessageStartEventAndNoneStartEvent() {

        // using startProcessInstanceByKey triggers the none start event

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

        assertThat(processInstance.isEnded()).isFalse();

        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("taskAfterNoneStart").singleResult();
        assertThat(task).isNotNull();

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());

        // using startProcessInstanceByMessage triggers the message start event

        processInstance = runtimeService.startProcessInstanceByMessage("newInvoiceMessage");

        assertThat(processInstance.isEnded()).isFalse();

        task = taskService.createTaskQuery().taskDefinitionKey("taskAfterMessageStart").singleResult();
        assertThat(task).isNotNull();

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());

    }

    @Test
    @Deployment
    public void testMultipleMessageStartEvents() {

        // sending newInvoiceMessage

        ProcessInstance processInstance = runtimeService.startProcessInstanceByMessage("newInvoiceMessage");

        assertThat(processInstance.isEnded()).isFalse();

        org.flowable.task.api.Task task = taskService.createTaskQuery().taskDefinitionKey("taskAfterMessageStart").singleResult();
        assertThat(task).isNotNull();

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());

        // sending newInvoiceMessage2

        processInstance = runtimeService.startProcessInstanceByMessage("newInvoiceMessage2");

        assertThat(processInstance.isEnded()).isFalse();

        task = taskService.createTaskQuery().taskDefinitionKey("taskAfterMessageStart2").singleResult();
        assertThat(task).isNotNull();

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());

        // starting the process using startProcessInstanceByKey is possible, the
        // first message start event will be the default:
        processInstance = runtimeService.startProcessInstanceByKey("testProcess");
        assertThat(processInstance.isEnded()).isFalse();
        task = taskService.createTaskQuery().taskDefinitionKey("taskAfterMessageStart").singleResult();
        assertThat(task).isNotNull();
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    public void testQueryMessageStartEvents() {
        assertEventSubscriptionQuery(runtimeService.createEventSubscriptionQuery(), 2);

        assertEventSubscriptionQuery(runtimeService.createEventSubscriptionQuery().eventType("message").orderByExecutionId().asc(), 2);

        assertEventSubscriptionQuery(runtimeService.createEventSubscriptionQuery().eventType("signal").orderById().desc(), 0);

        assertEventSubscriptionQuery(runtimeService.createEventSubscriptionQuery().eventName("newInvoiceMessage").orderByProcessDefinitionId().asc(), 1);

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("testProcess").singleResult();

        assertEventSubscriptionQuery(runtimeService.createEventSubscriptionQuery().processDefinitionId(processDefinition.getId()).orderByProcessInstanceId().desc(), 2);

        assertEventSubscriptionQuery(runtimeService.createEventSubscriptionQuery().processDefinitionId("nonexisting"), 0);
        
        assertEventSubscriptionQuery(runtimeService.createEventSubscriptionQuery().withoutProcessDefinitionId(), 0);
        
        assertEventSubscriptionQuery(runtimeService.createEventSubscriptionQuery().withoutScopeDefinitionId(), 2);

        assertEventSubscriptionQuery(runtimeService.createEventSubscriptionQuery().activityId("messageStart").orderByTenantId().asc(), 1);

        assertEventSubscriptionQuery(runtimeService.createEventSubscriptionQuery().activityId("nonexisting"), 0);

        Calendar compareCal = Calendar.getInstance();
        compareCal.set(2010, 1, 1, 0, 0, 0);
        assertEventSubscriptionQuery(runtimeService.createEventSubscriptionQuery().createdAfter(compareCal.getTime()), 2);

        compareCal = Calendar.getInstance();
        assertEventSubscriptionQuery(runtimeService.createEventSubscriptionQuery().createdBefore(compareCal.getTime()), 2);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/event/message/MessageStartEventTest.testSingleMessageStartEvent.bpmn20.xml")
    public void testMessageSubscriptionsRecreatedOnDeploymentDelete() {
        ProcessDefinition processDefinition1 = repositoryService.createProcessDefinitionQuery().singleResult();

        Assertions.assertThat(runtimeService.createEventSubscriptionQuery().list())
                .extracting(EventSubscription::getEventType, EventSubscription::getProcessDefinitionId, EventSubscription::getProcessInstanceId)
                .containsOnly(tuple(MessageEventSubscriptionEntity.EVENT_TYPE, processDefinition1.getId(), null));

        String deploymentId2 = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/bpmn/event/message/MessageStartEventTest.testSingleMessageStartEvent.bpmn20.xml")
                .deploy().getId();
        ProcessDefinition processDefinition2 = repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId2).singleResult();

        Assertions.assertThat(runtimeService.createEventSubscriptionQuery().list())
                .extracting(EventSubscription::getEventType, EventSubscription::getProcessDefinitionId, EventSubscription::getProcessInstanceId)
                .containsOnly(tuple(MessageEventSubscriptionEntity.EVENT_TYPE, processDefinition2.getId(), null)); // Note the changed definition id

        repositoryService.deleteDeployment(deploymentId2, true);

        Assertions.assertThat(runtimeService.createEventSubscriptionQuery().list())
                .extracting(EventSubscription::getEventType, EventSubscription::getProcessDefinitionId, EventSubscription::getProcessInstanceId)
                .containsOnly(tuple(MessageEventSubscriptionEntity.EVENT_TYPE, processDefinition1.getId(), null)); // Note the changed definition id to v1
    }


    protected void assertEventSubscriptionQuery(EventSubscriptionQuery query, int count) {
        assertThat(query.count()).isEqualTo(count);
        assertThat(query.list()).hasSize(count);
    }

}
