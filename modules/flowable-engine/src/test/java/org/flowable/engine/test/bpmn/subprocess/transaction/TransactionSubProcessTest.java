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

package org.flowable.engine.test.bpmn.subprocess.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.api.EventSubscriptionQuery;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class TransactionSubProcessTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/subprocess/transaction/TransactionSubProcessTest.testSimpleCase.bpmn20.xml" })
    public void testSimpleCaseTxSuccessful() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transactionProcess");

        // after the process is started, we have compensate event subscriptions:
        assertThat(createEventSubscriptionQuery().eventType("compensate").activityId("bookHotel").count()).isEqualTo(1);
        assertThat(createEventSubscriptionQuery().eventType("compensate").activityId("bookFlight").count()).isEqualTo(1);

        // the task is present:
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        // making the tx succeed:
        taskService.setVariable(task.getId(), "confirmed", true);
        taskService.complete(task.getId());

        // now the process instance execution is sitting in the 'afterSuccess' task
        // -> has left the transaction using the "normal" sequence flow
        List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstance.getId());
        assertThat(activeActivityIds).contains("afterSuccess");

        // there is a compensate event subscription for the transaction under the process instance
        EventSubscription eventSubscriptionEntity = createEventSubscriptionQuery().eventType("compensate").activityId("tx").executionId(processInstance.getId()).singleResult();

        // there is an event-scope execution associated with the event-subscription:
        assertThat(eventSubscriptionEntity.getConfiguration()).isNotNull();
        Execution eventScopeExecution = runtimeService.createExecutionQuery().executionId(eventSubscriptionEntity.getConfiguration()).singleResult();
        assertThat(eventScopeExecution).isNotNull();

        // we still have compensate event subscriptions for the compensation handlers, only now they are part of the event scope
        assertThat(createEventSubscriptionQuery().eventType("compensate").activityId("bookHotel").executionId(eventScopeExecution.getId()).count()).isEqualTo(1);
        assertThat(createEventSubscriptionQuery().eventType("compensate").activityId("bookFlight").executionId(eventScopeExecution.getId()).count()).isEqualTo(1);
        assertThat(createEventSubscriptionQuery().eventType("compensate").activityId("chargeCard").executionId(eventScopeExecution.getId()).count()).isEqualTo(1);

        // assert that the compensation handlers have not been invoked:
        assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookHotel")).isNull();
        assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookFlight")).isNull();
        assertThat(runtimeService.getVariable(processInstance.getId(), "undoChargeCard")).isNull();

        // end the process instance
        Execution receiveExecution = runtimeService.createExecutionQuery().activityId("afterSuccess").singleResult();
        runtimeService.trigger(receiveExecution.getId());
        assertProcessEnded(processInstance.getId());
        assertThat(runtimeService.createExecutionQuery().count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/subprocess/transaction/TransactionSubProcessTest.testSimpleCase.bpmn20.xml" })
    public void testSimpleCaseTxCancelled() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transactionProcess");

        // after the process is started, we have compensate event subscriptions:
        assertThat(createEventSubscriptionQuery().eventType("compensate").activityId("bookHotel").count()).isEqualTo(1);
        assertThat(createEventSubscriptionQuery().eventType("compensate").activityId("bookFlight").count()).isEqualTo(1);

        // the task is present:
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("askCustomer");

        // making the tx fail:
        taskService.setVariable(task.getId(), "confirmed", false);
        taskService.complete(task.getId());

        // now the process instance execution is sitting in the 'afterCancellation' task
        // -> has left the transaction using the cancel boundary event
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("afterCancellation").singleResult();
        assertThat(execution).isNotNull();

        // we have no more compensate event subscriptions
        assertThat(createEventSubscriptionQuery().eventType("compensate").count()).isZero();

        // assert that the compensation handlers have been invoked:
        assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookHotel")).isEqualTo(1);
        assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookFlight")).isEqualTo(1);
        assertThat(runtimeService.getVariable(processInstance.getId(), "undoChargeCard")).isEqualTo(1);

        // if we have history, we check that the invocation of the compensation
        // handlers is recorded in history.
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("undoBookFlight").count()).isEqualTo(1);
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("undoBookHotel").count()).isEqualTo(1);
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("undoChargeCard").count()).isEqualTo(1);
        }

        // end the process instance
        Execution receiveExecution = runtimeService.createExecutionQuery().activityId("afterCancellation").singleResult();
        runtimeService.trigger(receiveExecution.getId());
        assertProcessEnded(processInstance.getId());
        assertThat(runtimeService.createExecutionQuery().count()).isZero();
    }

    @Test
    @Deployment
    public void testCancelEndConcurrent() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transactionProcess");

        // after the process is started, we have compensate event subscriptions:
        assertThat(createEventSubscriptionQuery().eventType("compensate").activityId("bookHotel").count()).isEqualTo(1);
        assertThat(createEventSubscriptionQuery().eventType("compensate").activityId("bookFlight").count()).isEqualTo(1);

        // the task is present:
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("askCustomer");

        // making the tx fail:
        taskService.setVariable(task.getId(), "confirmed", false);
        taskService.complete(task.getId());

        // now the process instance execution is sitting in the 'afterCancellation' task
        // -> has left the transaction using the cancel boundary event
        List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstance.getId());
        assertThat(activeActivityIds).contains("afterCancellation");

        // we have no more compensate event subscriptions
        assertThat(createEventSubscriptionQuery().eventType("compensate").count()).isZero();

        // assert that the compensation handlers have been invoked:
        assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookHotel")).isEqualTo(1);
        assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookFlight")).isEqualTo(1);

        // if we have history, we check that the invocation of the compensation handlers is recorded in history.
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("undoBookHotel").count()).isEqualTo(1);
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("undoBookFlight").count()).isEqualTo(1);
        }

        // end the process instance
        Execution receiveExecution = runtimeService.createExecutionQuery().activityId("afterCancellation").singleResult();
        runtimeService.trigger(receiveExecution.getId());
        assertProcessEnded(processInstance.getId());
        assertThat(runtimeService.createExecutionQuery().count()).isZero();
    }

    @Test
    @Deployment
    public void testNestedCancelInner() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transactionProcess");

        // after the process is started, we have compensate event subscriptions:
        assertThat(createEventSubscriptionQuery().eventType("compensate").activityId("bookFlight").count()).isZero();
        assertThat(createEventSubscriptionQuery().eventType("compensate").activityId("innerTxbookHotel").count()).isEqualTo(5);
        assertThat(createEventSubscriptionQuery().eventType("compensate").activityId("innerTxbookFlight").count()).isEqualTo(1);

        // the tasks are present:
        org.flowable.task.api.Task taskInner = taskService.createTaskQuery().taskDefinitionKey("innerTxaskCustomer").singleResult();
        org.flowable.task.api.Task taskOuter = taskService.createTaskQuery().taskDefinitionKey("bookFlight").singleResult();
        assertThat(taskInner).isNotNull();
        assertThat(taskOuter).isNotNull();

        // making the tx fail:
        taskService.setVariable(taskInner.getId(), "confirmed", false);
        taskService.complete(taskInner.getId());

        // now the process instance execution is sitting in the
        // 'afterInnerCancellation' task
        // -> has left the transaction using the cancel boundary event
        List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstance.getId());
        assertThat(activeActivityIds).contains("afterInnerCancellation");

        // we have no more compensate event subscriptions for the inner tx
        assertThat(createEventSubscriptionQuery().eventType("compensate").activityId("innerTxbookHotel").count()).isZero();
        assertThat(createEventSubscriptionQuery().eventType("compensate").activityId("innerTxbookFlight").count()).isZero();

        // we do not have a subscription or the outer tx yet
        assertThat(createEventSubscriptionQuery().eventType("compensate").activityId("bookFlight").count()).isZero();

        // assert that the compensation handlers have been invoked:
        assertThat(runtimeService.getVariable(processInstance.getId(), "innerTxundoBookHotel")).isEqualTo(5);
        assertThat(runtimeService.getVariable(processInstance.getId(), "innerTxundoBookFlight")).isEqualTo(1);

        // if we have history, we check that the invocation of the compensation handlers is recorded in history.
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("innerTxundoBookHotel").count()).isEqualTo(5);
            assertThat(historyService.createHistoricActivityInstanceQuery().activityId("innerTxundoBookFlight").count()).isEqualTo(1);
        }

        // complete the task in the outer tx
        taskService.complete(taskOuter.getId());

        // end the process instance (signal the execution still sitting in afterInnerCancellation)
        runtimeService.trigger(runtimeService.createExecutionQuery().activityId("afterInnerCancellation").singleResult().getId());

        assertProcessEnded(processInstance.getId());
        assertThat(runtimeService.createExecutionQuery().count()).isZero();
    }

    @Test
    @Deployment
    public void testNestedCancelOuter() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transactionProcess");

        // after the process is started, we have compensate event subscriptions:
        assertThat(createEventSubscriptionQuery().eventType("compensate").activityId("bookFlight").count()).isZero();
        assertThat(createEventSubscriptionQuery().eventType("compensate").activityId("innerTxbookHotel").count()).isEqualTo(5);
        assertThat(createEventSubscriptionQuery().eventType("compensate").activityId("innerTxbookFlight").count()).isEqualTo(1);

        // the tasks are present:
        org.flowable.task.api.Task taskInner = taskService.createTaskQuery().taskDefinitionKey("innerTxaskCustomer").singleResult();
        org.flowable.task.api.Task taskOuter = taskService.createTaskQuery().taskDefinitionKey("bookFlight").singleResult();
        assertThat(taskInner).isNotNull();
        assertThat(taskOuter).isNotNull();

        // making the outer tx fail (invokes cancel end event)
        taskService.complete(taskOuter.getId());

        // now the process instance is sitting in 'afterOuterCancellation'
        List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstance.getId());
        assertThat(activeActivityIds).contains("afterOuterCancellation");

        // we have no more compensate event subscriptions
        assertThat(createEventSubscriptionQuery().eventType("compensate").activityId("innerTxbookHotel").count()).isZero();
        assertThat(createEventSubscriptionQuery().eventType("compensate").activityId("innerTxbookFlight").count()).isZero();
        assertThat(createEventSubscriptionQuery().eventType("compensate").activityId("bookFlight").count()).isZero();

        // the compensation handlers of the inner tx have not been invoked
        assertThat(runtimeService.getVariable(processInstance.getId(), "innerTxundoBookHotel")).isNull();
        assertThat(runtimeService.getVariable(processInstance.getId(), "innerTxundoBookFlight")).isNull();

        // the compensation handler in the outer tx has been invoked
        assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookFlight")).isEqualTo(1);

        // end the process instance (signal the execution still sitting in afterOuterCancellation)
        runtimeService.trigger(runtimeService.createExecutionQuery().activityId("afterOuterCancellation").singleResult().getId());

        assertProcessEnded(processInstance.getId());
        assertThat(runtimeService.createExecutionQuery().count()).isZero();

    }

    /*
     * The cancel end event cancels all instances, compensation is performed for all instances
     * 
     * see spec page 470: "If the cancelActivity attribute is set, the Activity the Event is attached to is then cancelled (in case of a multi-instance, all its instances are cancelled);"
     */
    @Test
    @Deployment
    public void testMultiInstanceTx() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transactionProcess");

        // there are now 5 instances of the transaction:

        List<EventSubscription> eventSubscriptionEntities = createEventSubscriptionQuery().eventType("compensate").list();

        // there are 10 compensation event subscriptions
        assertThat(eventSubscriptionEntities).hasSize(10);

        org.flowable.task.api.Task task = taskService.createTaskQuery().listPage(0, 1).get(0);

        // canceling one instance triggers compensation for all other instances:
        taskService.setVariable(task.getId(), "confirmed", false);
        taskService.complete(task.getId());

        assertThat(createEventSubscriptionQuery().count()).isZero();

        assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookHotel")).isEqualTo(5);
        assertThat(runtimeService.getVariable(processInstance.getId(), "undoBookFlight")).isEqualTo(5);

        runtimeService.trigger(runtimeService.createExecutionQuery().activityId("afterCancellation").singleResult().getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/subprocess/transaction/TransactionSubProcessTest.testMultiInstanceTx.bpmn20.xml" })
    public void testMultiInstanceTxSuccessful() {

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transactionProcess");

        // there are now 5 instances of the transaction:

        List<EventSubscription> EventSubscriptionEntitys = createEventSubscriptionQuery().eventType("compensate").list();

        // there are 10 compensation event subscriptions
        assertThat(EventSubscriptionEntitys).hasSize(10);

        // first complete the inner user-tasks
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        for (org.flowable.task.api.Task task : tasks) {
            taskService.setVariable(task.getId(), "confirmed", true);
            taskService.complete(task.getId());
        }

        // now complete the inner receive tasks
        List<Execution> executions = runtimeService.createExecutionQuery().activityId("receive").list();
        for (Execution execution : executions) {
            runtimeService.trigger(execution.getId());
        }

        runtimeService.trigger(runtimeService.createExecutionQuery().activityId("afterSuccess").singleResult().getId());

        assertThat(createEventSubscriptionQuery().count()).isZero();
        assertProcessEnded(processInstance.getId());

    }

    @Test
    public void testMultipleCancelBoundaryFails() {
        assertThatThrownBy(() -> repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/subprocess/transaction/TransactionSubProcessTest.testMultipleCancelBoundaryFails.bpmn20.xml").deploy())
                .isInstanceOf(Exception.class)
                .hasMessageContaining("multiple boundary events with cancelEventDefinition not supported on same transaction");
    }

    @Test
    public void testCancelBoundaryNoTransactionFails() {
        assertThatThrownBy(() -> repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/subprocess/transaction/TransactionSubProcessTest.testCancelBoundaryNoTransactionFails.bpmn20.xml").deploy())
                .isInstanceOf(Exception.class)
                .hasMessageContaining("boundary event with cancelEventDefinition only supported on transaction subprocesses");
    }

    @Test
    public void testCancelEndNoTransactionFails() {
        assertThatThrownBy(() -> repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/subprocess/transaction/TransactionSubProcessTest.testCancelEndNoTransactionFails.bpmn20.xml").deploy())
                .isInstanceOf(Exception.class)
                .hasMessageContaining("end event with cancelEventDefinition only supported inside transaction subprocess");
    }

    private EventSubscriptionQuery createEventSubscriptionQuery() {
        return runtimeService.createEventSubscriptionQuery();
    }

    @Test
    @Deployment
    public void testParseWithDI() {

        // this test simply makes sure we can parse a transaction subprocess with DI information
        // the actual transaction behavior is tested by other test cases

        // failing case

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TransactionSubProcessTest");

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        taskService.setVariable(task.getId(), "confirmed", false);

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());

        // success case

        processInstance = runtimeService.startProcessInstanceByKey("TransactionSubProcessTest");

        task = taskService.createTaskQuery().singleResult();
        taskService.setVariable(task.getId(), "confirmed", true);

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }
}
