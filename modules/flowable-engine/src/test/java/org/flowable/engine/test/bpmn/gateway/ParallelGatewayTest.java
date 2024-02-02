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

package org.flowable.engine.test.bpmn.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.service.impl.EventSubscriptionQueryImpl;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class ParallelGatewayTest extends PluggableFlowableTestCase {

    /**
     * Case where there is a parallel gateway that splits into 3 paths of execution, that are immediately joined, without any wait states in between. In the end, no executions should be in the
     * database.
     */
    @Test
    @Deployment
    public void testSplitMergeNoWaitstates() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("forkJoinNoWaitStates");
        assertThat(processInstance.isEnded()).isTrue();
    }

    @Test
    @Deployment
    public void testUnstructuredConcurrencyTwoForks() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("unstructuredConcurrencyTwoForks");
        assertThat(processInstance.isEnded()).isTrue();
    }

    @Test
    @Deployment
    public void testUnstructuredConcurrencyTwoJoins() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("unstructuredConcurrencyTwoJoins");
        assertThat(processInstance.isEnded()).isTrue();
    }

    @Test
    @Deployment
    public void testForkFollowedByOnlyEndEvents() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("forkFollowedByEndEvents");
        assertThat(processInstance.isEnded()).isTrue();
    }

    @Test
    @Deployment
    public void testNestedForksFollowedByEndEvents() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedForksFollowedByEndEvents");
        assertThat(processInstance.isEnded()).isTrue();
    }

    // ACT-482
    @Test
    @Deployment
    public void testNestedForkJoin() {
        runtimeService.startProcessInstanceByKey("nestedForkJoin");

        // After process starts, only task 0 should be active
        TaskQuery query = taskService.createTaskQuery().orderByTaskName().asc();
        List<org.flowable.task.api.Task> tasks = query.list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task 0");

        // Completing task 0 will create org.flowable.task.service.Task A and B
        taskService.complete(tasks.get(0).getId());
        tasks = query.list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task A", "Task B");

        // Completing task A should not trigger any new tasks
        taskService.complete(tasks.get(0).getId());
        tasks = query.list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task B");

        // Completing task B creates tasks B1 and B2
        taskService.complete(tasks.get(0).getId());
        tasks = query.list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task B1", "Task B2");

        // Completing B1 and B2 will activate both joins, and process reaches
        // task C
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());
        tasks = query.list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Task C");
    }

    /**
     * https://activiti.atlassian.net/browse/ACT-1222
     */
    @Test
    @Deployment
    public void testRecyclingExecutionWithCallActivity() {
        runtimeService.startProcessInstanceByKey("parent-process");

        // After process start we have two tasks, one from the parent and one
        // from the sub process
        TaskQuery query = taskService.createTaskQuery().orderByTaskName().asc();
        List<org.flowable.task.api.Task> tasks = query.list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Another task", "Some Task");

        // we complete the task from the parent process, the root execution is
        // recycled, the task in the sub process is still there
        taskService.complete(tasks.get(1).getId());
        tasks = query.list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Another task");

        // we end the task in the sub process and the sub process instance end
        // is propagated to the parent process
        taskService.complete(tasks.get(0).getId());
        assertThat(taskService.createTaskQuery().count()).isZero();

        // There is a QA config without history, so we cannot work with this:
        // assertEquals(1,
        // historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).finished().count());
    }

    // Test to verify ACT-1755
    @Test
    @Deployment
    public void testHistoryTables() {
        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.ACTIVITY)) {
            ProcessInstance pi = runtimeService.startProcessInstanceByKey("testHistoryRecords");
            List<HistoricActivityInstance> history = historyService.createHistoricActivityInstanceQuery().processInstanceId(pi.getId()).list();
            for (HistoricActivityInstance h : history) {
                assertActivityInstancesAreSame(h, runtimeService.createActivityInstanceQuery().activityInstanceId(h.getId()).singleResult());
                if ("parallelgateway2".equals(h.getActivityId())) {
                    assertThat(h.getEndTime()).isNotNull();
                }
            }
        }
    }

    @Test
    @Deployment
    public void testAsyncBehavior() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("async");
        waitForJobExecutorToProcessAllJobs(20000L, 250L);
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }

    /*
     * @Test
     * @Deployment public void testAsyncBehavior() { for (int i = 0; i < 100; i++) { ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("async"); } assertEquals(200,
     * managementService.createJobQuery().count()); waitForJobExecutorToProcessAllJobs(120000, 5000); assertThat(managementService.createJobQuery().count()).isZero(); assertEquals(0,
     * runtimeService.createProcessInstanceQuery().count()); }
     */

    @Test
    @Deployment
    public void testHistoricActivityInstanceEndTimes() {
        runtimeService.startProcessInstanceByKey("nestedForkJoin");
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery().list();
            assertThat(historicActivityInstances).hasSize(41);
            for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                assertThat(historicActivityInstance.getStartTime()).isNotNull();
                assertThat(historicActivityInstance.getEndTime()).isNotNull();
                if (historicActivityInstance.getActivityId().startsWith("flow")) {
                    assertThat(historicActivityInstance.getEndTime()).isEqualTo(historicActivityInstance.getStartTime());
                }
            }
        }
    }

    @Test
    @Deployment
    public void testNonTerminatingEndEventShouldNotRemoveSubscriptions() {
        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery()
                .processDefinitionKey("ec7ca606-a4f2-11e7-abc4-cec278b6b50a")
                .singleResult();

        final ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionId(processDefinition.getId())
                .start();

        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .list();

        assertThat(tasks).hasSize(1);

        processEngine.getManagementService().executeCommand(new Command<String>() {
            @Override
            public String execute(CommandContext commandContext) {
                EventSubscriptionQueryImpl q = new EventSubscriptionQueryImpl(commandContext, processEngineConfiguration.getEventSubscriptionServiceConfiguration());
                q.processInstanceId(processInstance.getProcessInstanceId());

                List<EventSubscription> subs = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService()
                        .findEventSubscriptionsByQueryCriteria(q);
                assertThat(subs)
                        .extracting(EventSubscription::getEventName)
                        .containsExactly("testmessage");

                return subs.get(0).getExecutionId();
            }
        });
        taskService.complete(tasks.get(0).getId());

        tasks = taskService.createTaskQuery()
                .processInstanceId(processInstance.getProcessInstanceId())
                .taskName("task 2")
                .list();
        assertThat(tasks).hasSize(1);

        taskService.complete(tasks.get(0).getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getProcessInstanceId()).list();
        assertThat(tasks).hasSize(1);

        processEngine.getManagementService().executeCommand(new Command<String>() {
            @Override
            public String execute(CommandContext commandContext) {
                EventSubscriptionQueryImpl q = new EventSubscriptionQueryImpl(commandContext, processEngineConfiguration.getEventSubscriptionServiceConfiguration());
                q.processInstanceId(processInstance.getProcessInstanceId());

                List<EventSubscription> subs = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService()
                        .findEventSubscriptionsByQueryCriteria(q);
                assertThat(subs)
                        .extracting(EventSubscription::getEventName)
                        .containsExactly("testmessage");

                return subs.get(0).getExecutionId();
            }
        });

    }

}
