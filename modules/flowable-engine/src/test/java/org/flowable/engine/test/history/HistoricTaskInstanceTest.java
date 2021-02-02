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

package org.flowable.engine.test.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Tom Baeyens
 * @author Frederik Heremans
 */
public class HistoricTaskInstanceTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testHistoricTaskInstance() throws Exception {
        Map<String, Object> varMap = new HashMap<>();
        varMap.put("formKeyVar", "expressionFormKey");
        String processInstanceId = runtimeService.startProcessInstanceByKey("HistoricTaskInstanceTest", varMap).getId();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

        // Set priority to non-default value
        org.flowable.task.api.Task runtimeTask = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        runtimeTask.setPriority(1234);

        // Set due-date
        Date dueDate = sdf.parse("01/02/2003 04:05:06");
        runtimeTask.setDueDate(dueDate);
        taskService.saveTask(runtimeTask);
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        String taskId = runtimeTask.getId();
        String taskDefinitionKey = runtimeTask.getTaskDefinitionKey();

        assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(1);
        HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertThat(historicTaskInstance.getId()).isEqualTo(taskId);
        assertThat(historicTaskInstance.getPriority()).isEqualTo(1234);
        assertThat(historicTaskInstance.getName()).isEqualTo("Clean up");
        assertThat(historicTaskInstance.getDescription()).isEqualTo("Schedule an engineering meeting for next week with the new hire.");
        assertThat(historicTaskInstance.getDueDate()).isEqualTo(dueDate);
        assertThat(historicTaskInstance.getAssignee()).isEqualTo("kermit");
        assertThat(historicTaskInstance.getTaskDefinitionKey()).isEqualTo(taskDefinitionKey);
        assertThat(historicTaskInstance.getFormKey()).isEqualTo("expressionFormKey");
        assertThat(historicTaskInstance.getEndTime()).isNull();
        assertThat(historicTaskInstance.getDurationInMillis()).isNull();
        assertThat(historicTaskInstance.getWorkTimeInMillis()).isNull();

        runtimeService.setVariable(processInstanceId, "deadline", "yesterday");

        taskService.claim(taskId, "kermit");
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(1);
        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertThat(historicTaskInstance.getClaimTime()).isNotNull();
        assertThat(historicTaskInstance.getWorkTimeInMillis()).isNull();
        assertThat(historicTaskInstance.getFormKey()).isEqualTo("expressionFormKey");

        taskService.complete(taskId);
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(1);

        historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
        assertThat(historicTaskInstance.getId()).isEqualTo(taskId);
        assertThat(historicTaskInstance.getPriority()).isEqualTo(1234);
        assertThat(historicTaskInstance.getName()).isEqualTo("Clean up");
        assertThat(historicTaskInstance.getDescription()).isEqualTo("Schedule an engineering meeting for next week with the new hire.");
        assertThat(historicTaskInstance.getDueDate()).isEqualTo(dueDate);
        assertThat(historicTaskInstance.getAssignee()).isEqualTo("kermit");
        assertThat(historicTaskInstance.getDeleteReason()).isNull();
        assertThat(historicTaskInstance.getTaskDefinitionKey()).isEqualTo(taskDefinitionKey);
        assertThat(historicTaskInstance.getFormKey()).isEqualTo("expressionFormKey");
        assertThat(historicTaskInstance.getEndTime()).isNotNull();
        assertThat(historicTaskInstance.getDurationInMillis()).isNotNull();
        assertThat(historicTaskInstance.getClaimTime()).isNotNull();
        assertThat(historicTaskInstance.getWorkTimeInMillis()).isNotNull();

        historyService.deleteHistoricTaskInstance(taskId);
        managementService.executeCommand(commandContext -> {
            processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().deleteHistoricTaskLogEntriesForTaskId(taskId);
            return null;
        });

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertThat(historyService.createHistoricTaskInstanceQuery().count()).isZero();
    }

    @Test
    public void testDeleteHistoricTaskInstance() throws Exception {
        // deleting unexisting historic task instance should be silently ignored
        historyService.deleteHistoricTaskInstance("unexistingId");
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
    }

    @Test
    @Deployment
    public void testHistoricTaskInstanceQuery() throws Exception {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.MILLISECOND, 0);
        processEngineConfiguration.getClock().setCurrentTime(start.getTime());

        // First instance is finished
        ProcessInstance finishedInstance = runtimeService.startProcessInstanceByKey("HistoricTaskQueryTest", "myBusinessKey");
        processEngineConfiguration.getClock().reset();

        // Set priority to non-default value
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(finishedInstance.getId()).singleResult();
        task.setPriority(1234);
        task.setOwner("fozzie");
        Date dueDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("01/02/2003 04:05:06");
        task.setDueDate(dueDate);

        taskService.saveTask(task);
        taskService.addUserIdentityLink(task.getId(), "gonzo", "someType");

        // Complete the task
        String taskId = task.getId();
        taskService.complete(taskId);
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        // org.flowable.task.service.Task id
        assertThat(historyService.createHistoricTaskInstanceQuery().taskId(taskId).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskId("unexistingtaskid").count()).isZero();

        // Name
        assertThat(historyService.createHistoricTaskInstanceQuery().taskName("Clean up").count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskName("unexistingname").count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().taskNameLike("Clean u%").count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskNameLike("%lean up").count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskNameLike("%lean u%").count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskNameLike("%unexistingname%").count()).isZero();

        // Description
        assertThat(historyService.createHistoricTaskInstanceQuery().taskDescription("Historic task description").count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskDescription("unexistingdescription").count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().taskDescriptionLike("%task description").count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskDescriptionLike("Historic task %").count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskDescriptionLike("%task%").count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskDescriptionLike("%unexistingdescripton%").count()).isZero();

        // Execution id
        assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(finishedInstance.getId()).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().executionId("unexistingexecution").count()).isZero();

        // Process instance id
        assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(finishedInstance.getId()).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId("unexistingid").count()).isZero();

        // Process instance business key
        assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceBusinessKey("myBusinessKey").count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceBusinessKey("unexistingKey").count()).isZero();

        // Process definition id
        assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionId(finishedInstance.getProcessDefinitionId()).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionId("unexistingdefinitionid").count()).isZero();

        // Process definition name
        assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionName("Historic task query test process").count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionName("unexistingdefinitionname").count()).isZero();

        // Process definition key
        assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionKey("HistoricTaskQueryTest").count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionKey("unexistingdefinitionkey").count()).isZero();

        // Process definition key in
        List<String> includeIds = new ArrayList<>();
        assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionKeyIn(includeIds).count()).isEqualTo(1);
        includeIds.add("unexistingProcessDefinition");
        assertThat(historyService.createHistoricTaskInstanceQuery().processDefinitionKeyIn(includeIds).count()).isZero();
        includeIds.add("HistoricTaskQueryTest");
        assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKeyIn(includeIds).count()).isEqualTo(1);

        // Form key
        HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery().processInstanceId(finishedInstance.getId()).singleResult();
        assertThat(historicTask.getFormKey()).isEqualTo("testFormKey");

        // Assignee
        assertThat(historyService.createHistoricTaskInstanceQuery().taskAssignee("kermit").count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskAssignee("johndoe").count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigneeLike("%ermit").count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigneeLike("kermi%").count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigneeLike("%ermi%").count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigneeLike("%johndoe%").count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().taskAssigned().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskUnassigned().count()).isZero();

        // Delete reason
        assertThat(historyService.createHistoricTaskInstanceQuery().taskDeleteReason("deleted").count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().taskWithoutDeleteReason().count()).isEqualTo(1);

        // org.flowable.task.service.Task definition ID
        assertThat(historyService.createHistoricTaskInstanceQuery().taskDefinitionKey("task").count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskDefinitionKey("unexistingkey").count()).isZero();

        // org.flowable.task.service.Task priority
        assertThat(historyService.createHistoricTaskInstanceQuery().taskPriority(1234).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskPriority(5678).count()).isZero();

        assertThat(historyService.createHistoricTaskInstanceQuery().taskMinPriority(1234).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskMinPriority(1000).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskMinPriority(1300).count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().taskMaxPriority(1234).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskMaxPriority(1300).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskMaxPriority(1000).count()).isZero();

        // Due date
        Calendar anHourAgo = Calendar.getInstance();
        anHourAgo.setTime(dueDate);
        anHourAgo.add(Calendar.HOUR, -1);

        Calendar anHourLater = Calendar.getInstance();
        anHourLater.setTime(dueDate);
        anHourLater.add(Calendar.HOUR, 1);

        assertThat(historyService.createHistoricTaskInstanceQuery().taskDueDate(dueDate).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskDueDate(anHourAgo.getTime()).count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().taskDueDate(anHourLater.getTime()).count()).isZero();

        // Due date before
        assertThat(historyService.createHistoricTaskInstanceQuery().taskDueBefore(anHourLater.getTime()).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskDueBefore(anHourAgo.getTime()).count()).isZero();

        // Due date after
        assertThat(historyService.createHistoricTaskInstanceQuery().taskDueAfter(anHourAgo.getTime()).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskDueAfter(anHourLater.getTime()).count()).isZero();

        anHourAgo = new GregorianCalendar();
        anHourAgo.setTime(start.getTime());
        anHourAgo.add(Calendar.HOUR, -1);

        anHourLater = Calendar.getInstance();
        anHourLater.setTime(start.getTime());
        anHourLater.add(Calendar.HOUR, 1);

        // Start date
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            assertThat(historyService.createHistoricTaskInstanceQuery().taskCreatedOn(start.getTime()).count()).isEqualTo(1);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskCreatedOn(anHourAgo.getTime()).count()).isZero();
        }
        
        assertThat(historyService.createHistoricTaskInstanceQuery().taskCreatedAfter(anHourAgo.getTime()).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskCreatedAfter(anHourLater.getTime()).count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().taskCreatedBefore(anHourAgo.getTime()).count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().taskCreatedBefore(anHourLater.getTime()).count()).isEqualTo(1);

        // Completed date
        assertThat(historyService.createHistoricTaskInstanceQuery().taskCompletedAfter(anHourAgo.getTime()).count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskCompletedAfter(anHourLater.getTime()).count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().taskCompletedBefore(anHourAgo.getTime()).count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().taskCompletedBefore(anHourLater.getTime()).count()).isEqualTo(1);

        // Filter based on identity-links Assignee is involved
        assertThat(historyService.createHistoricTaskInstanceQuery().taskInvolvedUser("kermit").count()).isEqualTo(1);

        // Owner is involved
        assertThat(historyService.createHistoricTaskInstanceQuery().taskInvolvedUser("fozzie").count()).isEqualTo(1);

        // Manually involved person
        assertThat(historyService.createHistoricTaskInstanceQuery().taskInvolvedUser("gonzo").count()).isEqualTo(1);

        // Finished and Unfinished - Add anther other instance that has a running task (unfinished)
        runtimeService.startProcessInstanceByKey("HistoricTaskQueryTest");
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertThat(historyService.createHistoricTaskInstanceQuery().finished().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().unfinished().count()).isEqualTo(1);
    }

    @Test
    @Deployment
    public void testHistoricTaskInstanceQueryByTaskDefinitionKeys() throws Exception {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.MILLISECOND, 0);
        processEngineConfiguration.getClock().setCurrentTime(start.getTime());

        // First instance is finished
        ProcessInstance finishedInstance = runtimeService.startProcessInstanceByKey("taskDefinitionKeysProcess", "myBusinessKey");
        processEngineConfiguration.getClock().reset();

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(finishedInstance.getId()).list();
        for (Task task : tasks) {
            taskService.complete(task.getId());
        }

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertThat(historyService.createHistoricTaskInstanceQuery().taskDefinitionKeys(Arrays.asList("taskKey1", "taskKey123", "invalid")).list())
            .extracting(TaskInfo::getTaskDefinitionKey, TaskInfo::getName)
            .containsExactlyInAnyOrder(
                tuple("taskKey1", "Task A"),
                tuple("taskKey123", "Task B")
            );
        assertThat(historyService.createHistoricTaskInstanceQuery().taskDefinitionKeys(Arrays.asList("taskKey1", "taskKey123", "invalid")).count())
            .isEqualTo(2);

        assertThat(historyService.createHistoricTaskInstanceQuery().taskDefinitionKeys(Arrays.asList("invalid1", "invalid2")).list()).isEmpty();
        assertThat(historyService.createHistoricTaskInstanceQuery().taskDefinitionKeys(Arrays.asList("invalid1", "invalid2")).count()).isZero();

        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskId("invalid").taskDefinitionKeys(Arrays.asList("taskKey1", "taskKey123", "invalid"))
            .endOr().list())
            .extracting(TaskInfo::getTaskDefinitionKey, TaskInfo::getName)
            .containsExactlyInAnyOrder(
                tuple("taskKey1", "Task A"),
                tuple("taskKey123", "Task B")
            );
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskId("invalid").taskDefinitionKeys(Arrays.asList("taskKey1", "taskKey123", "invalid"))
            .endOr().count()).isEqualTo(2);
    }

    @Test
    @Deployment
    public void testHistoricIdentityLinksForTaskOwner() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTaskProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        String taskId = task.getId();
        taskService.setOwner(taskId, "kermit");
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        // task is still active
        List<HistoricIdentityLink> historicIdentityLinksForTask = historyService.getHistoricIdentityLinksForTask(task.getId());
        assertThat(historicIdentityLinksForTask).hasSize(1);
        assertThat(historicIdentityLinksForTask.get(0).getCreateTime()).isNotNull();

        assertThat(historicIdentityLinksForTask.get(0).getUserId()).isEqualTo("kermit");
        assertThat(historicIdentityLinksForTask.get(0).getType()).isEqualTo(IdentityLinkType.OWNER);

        // change owner
        taskService.setOwner(taskId, "gonzo");
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        historicIdentityLinksForTask = historyService.getHistoricIdentityLinksForTask(task.getId());
        assertThat(historicIdentityLinksForTask).hasSize(2);

        taskService.setOwner(taskId, null);
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        historicIdentityLinksForTask = historyService.getHistoricIdentityLinksForTask(task.getId());
        assertThat(historicIdentityLinksForTask).hasSize(3);

        taskService.complete(taskId);
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        historicIdentityLinksForTask = historyService.getHistoricIdentityLinksForTask(task.getId());
        assertThat(historicIdentityLinksForTask).hasSize(3);

        for (HistoricIdentityLink link : historicIdentityLinksForTask) {
            assertThat(link.getCreateTime()).isNotNull();
        }

        org.flowable.task.api.Task secondTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(secondTask).isNotNull();

        secondTask.setOwner("fozzie");
        taskService.saveTask(secondTask);
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        historicIdentityLinksForTask = historyService.getHistoricIdentityLinksForTask(secondTask.getId());
        assertThat(historicIdentityLinksForTask).hasSize(1);
        assertThat(historicIdentityLinksForTask.get(0).getCreateTime()).isNotNull();
    }

    @Test
    @Deployment
    public void testHistoricIdentityLinksOnTaskClaim() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTaskProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        // over a time period the task can be claimed by multiple users we must keep track of who claimed it
        String taskId = task.getId();
        taskService.claim(taskId, "kermit");
        taskService.unclaim(taskId);

        taskService.claim(taskId, "fozzie");
        taskService.unclaim(taskId);

        taskService.claim(taskId, "gonzo");
        taskService.unclaim(taskId);

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        
        // task is still active
        List<HistoricIdentityLink> historicIdentityLinksForTask = historyService.getHistoricIdentityLinksForTask(task.getId());
        assertThat(historicIdentityLinksForTask).hasSize(6);

        int nullCount = 0;
        int kermitCount = 0;
        int fozzieCount = 0;
        int gonzoCount = 0;

        // The order of history identity links is not guaranteed.
        for (HistoricIdentityLink link : historicIdentityLinksForTask) {
            assertThat(link.getType()).as("Expected ASSIGNEE lnk type").isEqualTo(IdentityLinkType.ASSIGNEE);
            if (link.getUserId() == null) {
                nullCount++;
            } else if ("kermit".equals(link.getUserId())) {
                kermitCount++;
                assertThat(link.getCreateTime()).isNotNull();
            } else if ("fozzie".equals(link.getUserId())) {
                fozzieCount++;
                assertThat(link.getCreateTime()).isNotNull();
            } else if ("gonzo".equals(link.getUserId())) {
                assertThat(link.getCreateTime()).isNotNull();
                gonzoCount++;
            }
        }

        assertThat(nullCount).isEqualTo(3);
        assertThat(kermitCount).isEqualTo(1);
        assertThat(fozzieCount).isEqualTo(1);
        assertThat(gonzoCount).isEqualTo(1);

        List<HistoricIdentityLink> historicIdentityLinksForProcess = historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(historicIdentityLinksForProcess).hasSize(3);

        // historic links should be present after the task is completed
        taskService.complete(taskId);
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        
        historicIdentityLinksForTask = historyService.getHistoricIdentityLinksForTask(task.getId());
        nullCount = 0;
        kermitCount = 0;
        fozzieCount = 0;
        gonzoCount = 0;

        // The order of history identity links is not guaranteed.
        for (HistoricIdentityLink link : historicIdentityLinksForTask) {
            assertThat(link.getType()).as("Expected ASSIGNEE lnk type").isEqualTo(IdentityLinkType.ASSIGNEE);
            if (link.getUserId() == null) {
                nullCount++;
            } else if ("kermit".equals(link.getUserId())) {
                kermitCount++;
            } else if ("fozzie".equals(link.getUserId())) {
                fozzieCount++;
            } else if ("gonzo".equals(link.getUserId())) {
                gonzoCount++;
            }
        }

        assertThat(nullCount).isEqualTo(3);
        assertThat(kermitCount).isEqualTo(1);
        assertThat(fozzieCount).isEqualTo(1);
        assertThat(gonzoCount).isEqualTo(1);

        historicIdentityLinksForProcess = historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(historicIdentityLinksForProcess).hasSize(3);

        org.flowable.task.api.Task secondTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(secondTask).isNotNull();

        String secondTaskId = secondTask.getId();
        taskService.setAssignee(secondTaskId, "newKid");
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        // 4 users now participated to the process
        historicIdentityLinksForProcess = historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(historicIdentityLinksForProcess).hasSize(4);

        // 4 users participated after the last task (and the process) is completed
        taskService.complete(secondTaskId);
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        
        historicIdentityLinksForProcess = historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId());
        assertThat(historicIdentityLinksForProcess).hasSize(4);

        historicIdentityLinksForTask = historyService.getHistoricIdentityLinksForTask(secondTaskId);
        assertThat(historicIdentityLinksForTask).hasSize(1);
        assertThat(historicIdentityLinksForTask.get(0).getUserId()).isEqualTo("newKid");
    }

    @Test
    @Deployment
    public void testHistoricTaskInstanceOrQuery() throws Exception {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.MILLISECOND, 0);
        processEngineConfiguration.getClock().setCurrentTime(start.getTime());

        // First instance is finished
        ProcessInstance finishedInstance = runtimeService.startProcessInstanceByKey("HistoricTaskQueryTest", "myBusinessKey");
        processEngineConfiguration.getClock().reset();

        // Set priority to non-default value
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(finishedInstance.getId()).singleResult();
        task.setPriority(1234);
        task.setOwner("fozzie");
        Date dueDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("01/02/2003 04:05:06");
        task.setDueDate(dueDate);

        taskService.saveTask(task);
        taskService.addUserIdentityLink(task.getId(), "gonzo", "someType");

        // Complete the task
        String taskId = task.getId();
        taskService.complete(taskId);
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        // org.flowable.task.service.Task id
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskId(taskId).endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskId(taskId).or().taskId(taskId).endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskId("unexistingtaskid").count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskId("unexistingtaskid").endOr().count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskId(taskId).taskName("Clean up").endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskId("unexistingtaskid").taskName("Clean up").endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskId("unexistingtaskid").taskName("unexistingname").endOr().count()).isZero();

        // Name
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskName("Clean up").endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskName("unexistingname").endOr().count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskNameLike("Clean u%").endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskNameLike("%unexistingname%").endOr().count()).isZero();
        final List<String> taskNameList = new ArrayList<>(1);
        taskNameList.add("Clean up");
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskNameIn(taskNameList).endOr().count()).isEqualTo(1);
        taskNameList.clear();
        taskNameList.add("unexistingname");
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskNameIn(taskNameList).endOr().count()).isZero();
        taskNameList.clear();
        taskNameList.add("clean up");
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskNameInIgnoreCase(taskNameList).endOr().count()).isEqualTo(1);
        taskNameList.clear();
        taskNameList.add("unexistingname");
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskNameInIgnoreCase(taskNameList).endOr().count()).isZero();

        taskNameList.clear();
        taskNameList.add("clean up");
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskName("Clean up").endOr().or().taskNameInIgnoreCase(taskNameList).endOr().count()).isEqualTo(1);
        taskNameList.clear();
        taskNameList.add("unexistingname");
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskName("Clean up").endOr().or().taskNameInIgnoreCase(taskNameList).endOr().count()).isZero();

        // Description
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskDescription("Historic task description").endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskDescription("unexistingdescription").endOr().count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskDescriptionLike("%task description").endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskDescriptionLike("%task description").taskDescription("unexistingdescription").endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskDescriptionLike("%unexistingdescripton%").count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskDescriptionLike("%unexistingdescripton%").taskDescription("unexistingdescription").endOr().count()).isZero();

        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskDescription("Historic task description").endOr().or().taskDescriptionLike("%task description").endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskDescription("Historic task description").endOr().or().taskDescriptionLike("%task description2").endOr().count()).isZero();

        // Execution id
        assertThat(historyService.createHistoricTaskInstanceQuery().or().processInstanceId(finishedInstance.getId()).endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().executionId("unexistingexecution").endOr().count()).isZero();

        // Process instance id
        assertThat(historyService.createHistoricTaskInstanceQuery().or().processInstanceId(finishedInstance.getId()).endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().processInstanceId("unexistingid").endOr().count()).isZero();

        // Process instance business key
        assertThat(historyService.createHistoricTaskInstanceQuery().or().processInstanceBusinessKey("myBusinessKey").endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().processInstanceBusinessKey("unexistingKey").endOr().count()).isZero();

        // Process definition id
        assertThat(historyService.createHistoricTaskInstanceQuery().or().processDefinitionId(finishedInstance.getProcessDefinitionId()).endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().processDefinitionId("unexistingdefinitionid").endOr().count()).isZero();

        // Process definition name
        assertThat(historyService.createHistoricTaskInstanceQuery().or().processDefinitionName("Historic task query test process").endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().processDefinitionName("unexistingdefinitionname").endOr().count()).isZero();

        // Process definition key
        assertThat(historyService.createHistoricTaskInstanceQuery().or().processDefinitionKey("HistoricTaskQueryTest").endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().processDefinitionKey("unexistingdefinitionkey").endOr().count()).isZero();

        // Process definition key and ad hoc task
        org.flowable.task.api.Task adhocTask = taskService.newTask();
        taskService.saveTask(adhocTask);
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskId(adhocTask.getId()).processDefinitionKey("unexistingdefinitionkey").endOr().count()).isEqualTo(1);
        taskService.deleteTask(adhocTask.getId(), true);
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        // Process definition key in
        List<String> includeIds = new ArrayList<>();
        assertThat(historyService.createHistoricTaskInstanceQuery().or().processDefinitionKey("unexistingdefinitionkey").processDefinitionKeyIn(includeIds).endOr().count()).isZero();
        includeIds.add("unexistingProcessDefinition");
        assertThat(historyService.createHistoricTaskInstanceQuery().or().processDefinitionKey("unexistingdefinitionkey").processDefinitionKeyIn(includeIds).endOr().count()).isZero();
        includeIds.add("unexistingProcessDefinition");
        assertThat(historyService.createHistoricTaskInstanceQuery().or().processDefinitionKey("HistoricTaskQueryTest").processDefinitionKeyIn(includeIds).endOr().count()).isEqualTo(1);
        includeIds.add("HistoricTaskQueryTest");
        assertThat(historyService.createHistoricProcessInstanceQuery().or().processDefinitionKey("unexistingdefinitionkey").processDefinitionKeyIn(includeIds).endOr().count()).isEqualTo(1);

        // Assignee
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskAssignee("kermit").endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskAssignee("johndoe").endOr().count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskAssigneeLike("%ermit").endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskAssigneeLike("%johndoe%").endOr().count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskAssigned().endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskUnassigned().endOr().count()).isZero();

        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskAssignee("kermit").endOr().or().taskAssigneeLike("%ermit").endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskAssignee("kermit").endOr().or().taskAssigneeLike("%johndoe%").endOr().count()).isZero();

        // Delete reason
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskDeleteReason("deleted").endOr().count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskWithoutDeleteReason().endOr().count()).isEqualTo(1);

        // org.flowable.task.service.Task definition ID
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskDefinitionKey("task").endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskDefinitionKey("unexistingkey").endOr().count()).isZero();

        // org.flowable.task.service.Task priority
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskPriority(1234).endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskPriority(5678).endOr().count()).isZero();

        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskMinPriority(1234).endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskMinPriority(1000).endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskMinPriority(1300).endOr().count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskMaxPriority(1234).endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskMaxPriority(1300).endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskMaxPriority(1000).endOr().count()).isZero();

        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskPriority(1234).endOr().or().taskMinPriority(1234).endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskPriority(1234).endOr().or().taskMinPriority(1300).endOr().count()).isZero();

        // Due date
        Calendar anHourAgo = Calendar.getInstance();
        anHourAgo.setTime(dueDate);
        anHourAgo.add(Calendar.HOUR, -1);

        Calendar anHourLater = Calendar.getInstance();
        anHourLater.setTime(dueDate);
        anHourLater.add(Calendar.HOUR, 1);

        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskDueDate(dueDate).endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskDueDate(anHourAgo.getTime()).endOr().count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskDueDate(anHourLater.getTime()).endOr().count()).isZero();

        // Due date before
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskDueBefore(anHourLater.getTime()).endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskDueBefore(anHourAgo.getTime()).endOr().count()).isZero();

        // Due date after
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskDueAfter(anHourAgo.getTime()).endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskDueAfter(anHourLater.getTime()).endOr().count()).isZero();

        anHourAgo = new GregorianCalendar();
        anHourAgo.setTime(start.getTime());
        anHourAgo.add(Calendar.HOUR, -1);

        anHourLater = Calendar.getInstance();
        anHourLater.setTime(start.getTime());
        anHourLater.add(Calendar.HOUR, 1);

        // Start date
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            assertThat(historyService.createHistoricTaskInstanceQuery().or().taskCreatedOn(start.getTime()).endOr().count()).isEqualTo(1);
            assertThat(historyService.createHistoricTaskInstanceQuery().or().taskCreatedOn(anHourAgo.getTime()).endOr().count()).isZero();
        }
        
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskCreatedAfter(anHourAgo.getTime()).endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskCreatedAfter(anHourLater.getTime()).endOr().count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskCreatedBefore(anHourAgo.getTime()).endOr().count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskCreatedBefore(anHourLater.getTime()).endOr().count()).isEqualTo(1);

        // Completed date
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskCompletedAfter(anHourAgo.getTime()).endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskCompletedAfter(anHourLater.getTime()).endOr().count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskCompletedBefore(anHourAgo.getTime()).endOr().count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskCompletedBefore(anHourLater.getTime()).endOr().count()).isEqualTo(1);

        // Filter based on identity-links Assignee is involved
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskInvolvedUser("kermit").endOr().count()).isEqualTo(1);

        // Owner is involved
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskInvolvedUser("fozzie").endOr().count()).isEqualTo(1);

        // Manually involved person
        assertThat(historyService.createHistoricTaskInstanceQuery().or().taskInvolvedUser("gonzo").endOr().count()).isEqualTo(1);

        // Finished and Unfinished - Add anther other instance that has a running task (unfinished)
        runtimeService.startProcessInstanceByKey("HistoricTaskQueryTest");
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertThat(historyService.createHistoricTaskInstanceQuery().or().finished().endOr().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().or().unfinished().endOr().count()).isEqualTo(1);
    }

    @Test
    @Deployment
    public void testHistoricTaskInstanceQueryProcessFinished() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TwoTaskHistoricTaskQueryTest");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        // Running task on running process should be available
        assertThat(historyService.createHistoricTaskInstanceQuery().processUnfinished().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().processFinished().count()).isZero();

        // Finished and running task on running process should be available
        taskService.complete(task.getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        
        assertThat(historyService.createHistoricTaskInstanceQuery().processUnfinished().count()).isEqualTo(2);
        assertThat(historyService.createHistoricTaskInstanceQuery().processFinished().count()).isZero();

        // 2 finished tasks are found for finished process after completing last task of process
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        
        assertThat(historyService.createHistoricTaskInstanceQuery().processUnfinished().count()).isZero();
        assertThat(historyService.createHistoricTaskInstanceQuery().processFinished().count()).isEqualTo(2);
    }

    @Test
    @Deployment
    public void testHistoricTaskInstanceQuerySorting() {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("HistoricTaskQueryTest");

        String taskId = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult().getId();
        taskService.complete(taskId);
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertThat(historyService.createHistoricTaskInstanceQuery().orderByDeleteReason().asc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByExecutionId().asc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByHistoricActivityInstanceId().asc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskCreateTime().asc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByProcessDefinitionId().asc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByProcessInstanceId().asc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskDescription().asc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskName().asc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskDefinitionKey().asc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskPriority().asc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskAssignee().asc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskId().asc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByCategory().asc().count()).isEqualTo(1);

        assertThat(historyService.createHistoricTaskInstanceQuery().orderByDeleteReason().desc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByExecutionId().desc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByHistoricActivityInstanceId().desc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskCreateTime().desc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByProcessDefinitionId().desc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByProcessInstanceId().desc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskDescription().desc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskName().desc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskDefinitionKey().desc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskPriority().desc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskAssignee().desc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByTaskId().desc().count()).isEqualTo(1);
        assertThat(historyService.createHistoricTaskInstanceQuery().orderByCategory().desc().count()).isEqualTo(1);
    }

    @Test
    @Deployment
    public void testHistoricIdentityLinksOnTask() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("historicIdentityLinks");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        // Set additional identity-link not coming from process
        taskService.addUserIdentityLink(task.getId(), "gonzo", "customUseridentityLink");
        assertThat(taskService.getIdentityLinksForTask(task.getId())).hasSize(4);
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        // Check historic identity-links when task is still active
        List<HistoricIdentityLink> historicIdentityLinks = historyService.getHistoricIdentityLinksForTask(task.getId());
        assertThat(historicIdentityLinks).hasSize(4);

        // Validate all links
        boolean foundCandidateUser = false;
        boolean foundCandidateGroup = false;
        boolean foundAssignee = false;
        boolean foundCustom = false;
        for (HistoricIdentityLink link : historicIdentityLinks) {
            assertThat(link.getTaskId()).isEqualTo(task.getId());
            if (link.getGroupId() != null) {
                assertThat(link.getGroupId()).isEqualTo("sales");
                foundCandidateGroup = true;
            } else {
                if ("candidate".equals(link.getType())) {
                    assertThat(link.getUserId()).isEqualTo("fozzie");
                    foundCandidateUser = true;
                } else if ("assignee".equals(link.getType())) {
                    assertThat(link.getUserId()).isEqualTo("kermit");
                    foundAssignee = true;
                } else if ("customUseridentityLink".equals(link.getType())) {
                    assertThat(link.getUserId()).isEqualTo("gonzo");
                    foundCustom = true;
                }
            }
        }

        assertThat(foundAssignee).isTrue();
        assertThat(foundCandidateGroup).isTrue();
        assertThat(foundCandidateUser).isTrue();
        assertThat(foundCustom).isTrue();

        // Now complete the task and check if links are still there
        taskService.complete(task.getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        
        assertThat(historyService.getHistoricIdentityLinksForTask(task.getId())).hasSize(4);

        // After deleting historic task, exception should be thrown when trying to get links
        historyService.deleteHistoricTaskInstance(task.getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

        assertThatThrownBy(() -> historyService.getHistoricIdentityLinksForTask(task.getId()).size())
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class);

        managementService.executeCommand(commandContext -> {
            processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().deleteHistoricTaskLogEntriesForTaskId(task.getId());
            return null;
        });
    }

    @Test
    public void testInvalidSorting() {
        assertThatThrownBy(() -> historyService.createHistoricTaskInstanceQuery().asc())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> historyService.createHistoricTaskInstanceQuery().desc())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> historyService.createHistoricTaskInstanceQuery().orderByProcessInstanceId().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    /**
     * Test to validate fix for ACT-1939: HistoryService loads invalid task local variables for completed task
     */
    @Test
    @Deployment
    public void testVariableUpdateOrderHistoricTaskInstance() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("historicTask");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        // Update task and process-variable 10 times
        for (int i = 0; i < 10; i++) {
            taskService.setVariableLocal(task.getId(), "taskVar", i);
            runtimeService.setVariable(task.getExecutionId(), "procVar", i);
        }

        taskService.complete(task.getId());
        
        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        
        // Check if all variables have the value for the latest revision
        HistoricTaskInstance taskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).includeProcessVariables().singleResult();

        Object varValue = taskInstance.getProcessVariables().get("procVar");
        assertThat(varValue).isEqualTo(9);

        taskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).includeTaskLocalVariables().singleResult();

        varValue = taskInstance.getTaskLocalVariables().get("taskVar");
        assertThat(varValue).isEqualTo(9);
    }

}
