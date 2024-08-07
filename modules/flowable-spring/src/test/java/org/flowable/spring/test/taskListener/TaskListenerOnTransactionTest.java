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
package org.flowable.spring.test.taskListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Yvo Swillens
 */
@ContextConfiguration("classpath:org/flowable/spring/test/executionListener/TransactionDependentListenerTest-context.xml")
@DirtiesContext
public class TaskListenerOnTransactionTest extends SpringFlowableTestCase {

    @Test
    @Deployment
    public void testOnCompleteCommitted() {
        CurrentTaskTransactionDependentTaskListener.clear();

        Map<String, Object> variables = new HashMap<>();
        variables.put("serviceTask1", false);
        variables.put("serviceTask2", false);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("taskListenersOnCompleteCommitted", variables);

        // task 1 has committed listener
        Task task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        // task 2 has rolled-back listener
        task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        List<CurrentTaskTransactionDependentTaskListener.CurrentTask> currentTasks = CurrentTaskTransactionDependentTaskListener.getCurrentTasks();

        assertThat(currentTasks)
                .extracting(
                        CurrentTaskTransactionDependentTaskListener.CurrentTask::getTaskId,
                        CurrentTaskTransactionDependentTaskListener.CurrentTask::getTaskName,
                        CurrentTaskTransactionDependentTaskListener.CurrentTask::getProcessInstanceId)
                .containsExactly(
                        tuple("usertask1", "User Task 1", processInstance.getId())
                );
    }

    @Test
    @Deployment
    public void testOnCompleteRolledBack() {
        CurrentTaskTransactionDependentTaskListener.clear();

        Map<String, Object> variables = new HashMap<>();
        variables.put("serviceTask1", false);
        variables.put("serviceTask2", false);
        variables.put("serviceTask3", true);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("taskListenersOnCompleteCommitted", variables);

        // task 1 has before-commit listener
        Task task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        // task 2 has rolled-back listener
        task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        // task 3 has rolled-back listener
        task = taskService.createTaskQuery().singleResult();

        try {
            taskService.complete(task.getId());
        } catch (Exception ex) {

        }

        List<CurrentTaskTransactionDependentTaskListener.CurrentTask> currentTasks = CurrentTaskTransactionDependentTaskListener.getCurrentTasks();

        assertThat(currentTasks)
                .extracting(
                        CurrentTaskTransactionDependentTaskListener.CurrentTask::getTaskId,
                        CurrentTaskTransactionDependentTaskListener.CurrentTask::getTaskName,
                        CurrentTaskTransactionDependentTaskListener.CurrentTask::getProcessInstanceId)
                .containsExactly(
                        tuple("usertask1", "User Task 1", processInstance.getId()),
                        tuple("usertask3", "User Task 3", processInstance.getId())
                );
    }

    @Test
    @Deployment
    public void testOnCompleteExecutionVariables() {

        CurrentTaskTransactionDependentTaskListener.clear();

        runtimeService.startProcessInstanceByKey("taskListenersOnCompleteExecutionVariables");

        // task 1 has committed listener
        Task task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        // task 2 has committed listener
        task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        List<CurrentTaskTransactionDependentTaskListener.CurrentTask> currentTasks = CurrentTaskTransactionDependentTaskListener.getCurrentTasks();
        assertThat(currentTasks)
                .extracting(
                        CurrentTaskTransactionDependentTaskListener.CurrentTask::getTaskId,
                        CurrentTaskTransactionDependentTaskListener.CurrentTask::getTaskName)
                .containsExactly(
                        tuple("usertask1", "User Task 1"),
                        tuple("usertask2", "User Task 2")
                );

        assertThat(currentTasks.get(0).getExecutionVariables())
                .containsOnly(entry("injectedExecutionVariable", "test1"));

        assertThat(currentTasks.get(1).getExecutionVariables())
                .containsOnly(entry("injectedExecutionVariable", "test2"));
    }

    @Test
    @Deployment
    public void testOnCompleteTransactionalOperation() {
        CurrentTaskTransactionDependentTaskListener.clear();

        ProcessInstance firstProcessInstance = runtimeService.startProcessInstanceByKey("transactionDependentTaskListenerProcess");
        assertProcessEnded(firstProcessInstance.getId());

        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();
            assertThat(historicProcessInstances)
                    .extracting(HistoricProcessInstance::getProcessDefinitionKey)
                    .containsExactly("transactionDependentTaskListenerProcess");
        }

        ProcessInstance secondProcessInstance = runtimeService.startProcessInstanceByKey("secondTransactionDependentTaskListenerProcess");

        Task task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(secondProcessInstance.getId());

        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            // first historic process instance was deleted by task listener
            List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();
            assertThat(historicProcessInstances)
                    .extracting(HistoricProcessInstance::getProcessDefinitionKey)
                    .containsExactly("secondTransactionDependentTaskListenerProcess");
        }

        List<MyTransactionalOperationTransactionDependentTaskListener.CurrentTask> currentTasks = MyTransactionalOperationTransactionDependentTaskListener
                .getCurrentTasks();
        assertThat(currentTasks)
                .extracting(CurrentTaskTransactionDependentTaskListener.CurrentTask::getTaskId,
                        CurrentTaskTransactionDependentTaskListener.CurrentTask::getTaskName)
                .containsExactly(tuple("usertask1", "User Task 1"));
    }

    @Test
    @Deployment
    public void testOnCompleteCustomPropertiesResolver() {
        CurrentTaskTransactionDependentTaskListener.clear();

        runtimeService.startProcessInstanceByKey("transactionDependentTaskListenerProcess");

        Task task = taskService.createTaskQuery().singleResult();
        taskService.complete(task.getId());

        List<CurrentTaskTransactionDependentTaskListener.CurrentTask> currentTasks = CurrentTaskTransactionDependentTaskListener.getCurrentTasks();
        assertThat(currentTasks)
                .extracting(CurrentTaskTransactionDependentTaskListener.CurrentTask::getTaskId,
                        CurrentTaskTransactionDependentTaskListener.CurrentTask::getTaskName)
                .containsExactly(tuple("usertask1", "User Task 1"));

        assertThat(currentTasks.get(0).getCustomPropertiesMap())
                .containsOnly(entry("customProp1", "usertask1"));
    }

}
