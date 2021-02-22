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

package org.flowable.engine.test.api.task;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;
import static org.flowable.engine.impl.test.HistoryTestHelper.isHistoryLevelAtLeast;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.ibatis.exceptions.PersistenceException;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.api.FlowableTaskAlreadyClaimedException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricDetail;
import org.flowable.engine.history.HistoricVariableUpdate;
import org.flowable.engine.impl.TaskServiceImpl;
import org.flowable.engine.impl.persistence.entity.CommentEntity;
import org.flowable.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Attachment;
import org.flowable.engine.task.Comment;
import org.flowable.engine.task.Event;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityImpl;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskCompletionBuilder;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.TaskPostProcessor;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Frederik Heremans
 * @author Joram Barrez
 * @author Falko Menge
 */
public class TaskServiceTest extends PluggableFlowableTestCase {

    private Task task = null;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @AfterEach
    public void tearDown() throws Exception {
        if (task != null) {
            taskService.deleteTask(task.getId(), true);
        }
    }

    @Test
    public void testCreateTaskWithBuilder() {
        // We need to make sure the time ends on .000, .003 or .007 due to SQL Server rounding to that
        Date todayDate = Date.from(Instant.now().plus(1, ChronoUnit.SECONDS).plusMillis(133));
        task = taskService.createTaskBuilder().
                name("testName").
                description("testDescription").
                priority(35).
                owner("testOwner").
                assignee("testAssignee").
                dueDate(todayDate).
                category("testCategory").
                parentTaskId("testParentTaskId").
                tenantId("testTenantId").
                formKey("testFormKey").
                taskDefinitionId("testDefinitionId").
                taskDefinitionKey("testDefinitionKey").
                scopeType(ScopeTypes.TASK).
                scopeId("scopeIdValue").
                create();
        Task updatedTask = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertThat(updatedTask).isNotNull();
        assertThat(updatedTask.getName()).isEqualTo("testName");
        assertThat(updatedTask.getDescription()).isEqualTo("testDescription");
        assertThat(updatedTask.getPriority()).isEqualTo(35);
        assertThat(updatedTask.getOwner()).isEqualTo("testOwner");
        assertThat(updatedTask.getAssignee()).isEqualTo("testAssignee");
        assertThat(simpleDateFormat.format(updatedTask.getDueDate())).isEqualTo(simpleDateFormat.format(todayDate));
        assertThat(updatedTask.getCategory()).isEqualTo("testCategory");
        assertThat(updatedTask.getParentTaskId()).isEqualTo("testParentTaskId");
        assertThat(updatedTask.getTenantId()).isEqualTo("testTenantId");
        assertThat(updatedTask.getFormKey()).isEqualTo("testFormKey");
        assertThat(updatedTask.getTaskDefinitionId()).isEqualTo("testDefinitionId");
        assertThat(updatedTask.getTaskDefinitionKey()).isEqualTo("testDefinitionKey");
        assertThat(updatedTask.getScopeId()).isEqualTo("scopeIdValue");
        assertThat(updatedTask.getScopeType()).isEqualTo(ScopeTypes.TASK);
    }

    @Test
    public void testBuilderCreateTaskWithParent() {
        Task parentTask = taskService.newTask();
        taskService.saveTask(parentTask);
        try {
            task = taskService.createTaskBuilder().
                    name("testName").
                    parentTaskId(parentTask.getId()).
                    identityLinks(getDefaultIdentityLinks()).
                    create();
            Task updatedParentTask = taskService.createTaskQuery().taskId(parentTask.getId()).singleResult();
            assertThat(((CountingTaskEntity) updatedParentTask).getSubTaskCount()).isEqualTo(1);
            Task updatedTask = taskService.createTaskQuery().taskId(task.getId()).includeIdentityLinks().singleResult();
            assertThat(((CountingTaskEntity) updatedTask).getIdentityLinkCount()).isEqualTo(2);
        } finally {
            this.taskService.deleteTask(parentTask.getId(), true);
        }
    }

    @Test
    public void testCreateTaskWithOwnerAssigneeAndIdentityLinks() {
        task = taskService.createTaskBuilder().
                name("testName").
                owner("testOwner").
                assignee("testAssignee").
                identityLinks(getDefaultIdentityLinks()).
                create();
        Task updatedTask = taskService.createTaskQuery().taskId(task.getId()).includeIdentityLinks().singleResult();
        assertThat(updatedTask).isNotNull();
        assertThat(updatedTask.getName()).isEqualTo("testName");
        assertThat(updatedTask.getAssignee()).isEqualTo("testAssignee");
        assertThat(updatedTask.getOwner()).isEqualTo("testOwner");
        assertThat(updatedTask.getIdentityLinks()).hasSize(2);
        assertThat(updatedTask.getPriority()).isEqualTo(Task.DEFAULT_PRIORITY);

        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).includeIdentityLinks()
                    .singleResult();
            assertThat(historicTaskInstance).isNotNull();
            assertThat(historicTaskInstance.getName()).isEqualTo("testName");
            assertThat(historicTaskInstance.getPriority()).isEqualTo(Task.DEFAULT_PRIORITY);
            assertThat(historicTaskInstance.getIdentityLinks()).hasSize(2);
        }

        taskService.deleteUserIdentityLink(updatedTask.getId(), "testUserBuilder", IdentityLinkType.CANDIDATE);
        taskService.deleteGroupIdentityLink(updatedTask.getId(), "testGroupBuilder", IdentityLinkType.CANDIDATE);
    }

    @Test
    public void testCreateTaskWithBuilderAndPostprocessor() {
        TaskServiceConfiguration taskServiceConfiguration = (TaskServiceConfiguration) this.processEngineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG);
        TaskPostProcessor previousTaskPostProcessor = taskServiceConfiguration.getTaskPostProcessor();
        try {
            taskServiceConfiguration.setTaskPostProcessor(
                    taskEntity -> {
                        taskEntity.addUserIdentityLink("testUser", IdentityLinkType.CANDIDATE);
                        taskEntity.addGroupIdentityLink("testGroup", IdentityLinkType.CANDIDATE);
                        return taskEntity;
                    }
            );
            task = taskService.createTaskBuilder().
                    name("testName").
                    create();
            Task updatedTask = taskService.createTaskQuery().taskId(task.getId()).includeIdentityLinks().singleResult();
            assertThat(updatedTask).isNotNull();
            assertThat(updatedTask.getName()).isEqualTo("testName");
            assertThat(updatedTask.getIdentityLinks()).hasSize(2);

            if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).includeIdentityLinks()
                        .singleResult();
                assertThat(historicTaskInstance).isNotNull();
                assertThat(historicTaskInstance.getName()).isEqualTo("testName");
                assertThat(historicTaskInstance.getIdentityLinks()).hasSize(2);
            }

            taskService.deleteUserIdentityLink(updatedTask.getId(), "testUser", IdentityLinkType.CANDIDATE);
            taskService.deleteGroupIdentityLink(updatedTask.getId(), "testGroup", IdentityLinkType.CANDIDATE);
        } finally {
            taskServiceConfiguration.setTaskPostProcessor(previousTaskPostProcessor);
        }
    }

    @Test
    public void testCreateTaskWithOwnerAssigneeAndIdentityLinksAndPostProcessor() {
        TaskServiceConfiguration taskServiceConfiguration = (TaskServiceConfiguration) this.processEngineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG);
        TaskPostProcessor previousTaskPostProcessor = taskServiceConfiguration.getTaskPostProcessor();
        try {
            taskServiceConfiguration.setTaskPostProcessor(
                    taskEntity -> {
                        taskEntity.setName("testNameFromPostProcessor");
                        taskEntity.addUserIdentityLink("testUser", IdentityLinkType.CANDIDATE);
                        taskEntity.addGroupIdentityLink("testGroup", IdentityLinkType.CANDIDATE);
                        return taskEntity;
                    }
            );

            task = taskService.createTaskBuilder().
                    name("testName").
                    owner("testOwner").
                    assignee("testAssignee").
                    identityLinks(getDefaultIdentityLinks()).
                    create();
            Task updatedTask = taskService.createTaskQuery().taskId(task.getId()).includeIdentityLinks().singleResult();
            assertThat(updatedTask).isNotNull();
            assertThat(updatedTask.getName()).isEqualTo("testNameFromPostProcessor");
            assertThat(updatedTask.getAssignee()).isEqualTo("testAssignee");
            assertThat(updatedTask.getOwner()).isEqualTo("testOwner");
            assertThat(updatedTask.getIdentityLinks()).hasSize(4);

            if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).includeIdentityLinks()
                        .singleResult();
                assertThat(historicTaskInstance).isNotNull();
                assertThat(historicTaskInstance.getName()).isEqualTo("testNameFromPostProcessor");
                assertThat(historicTaskInstance.getIdentityLinks()).hasSize(4);
            }

            taskService.deleteUserIdentityLink(updatedTask.getId(), "testUserBuilder", IdentityLinkType.CANDIDATE);
            taskService.deleteGroupIdentityLink(updatedTask.getId(), "testGroupBuilder", IdentityLinkType.CANDIDATE);
            taskService.deleteUserIdentityLink(updatedTask.getId(), "testUser", IdentityLinkType.CANDIDATE);
            taskService.deleteGroupIdentityLink(updatedTask.getId(), "testGroup", IdentityLinkType.CANDIDATE);
        } finally {
            taskServiceConfiguration.setTaskPostProcessor(previousTaskPostProcessor);
        }
    }

    @Test
    public void testSaveTaskUpdate() throws Exception {

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
        org.flowable.task.api.Task task = taskService.newTask();
        task.setDescription("description");
        task.setName("taskname");
        task.setPriority(0);
        task.setAssignee("taskassignee");
        task.setOwner("taskowner");
        Date dueDate = sdf.parse("01/02/2003 04:05:06");
        task.setDueDate(dueDate);
        taskService.saveTask(task);

        // Fetch the task again and update
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertThat(task.getDescription()).isEqualTo("description");
        assertThat(task.getName()).isEqualTo("taskname");
        assertThat(task.getAssignee()).isEqualTo("taskassignee");
        assertThat(task.getOwner()).isEqualTo("taskowner");
        assertThat(task.getDueDate()).isEqualTo(dueDate);
        assertThat(task.getPriority()).isZero();
        assertThat(task.getScopeId()).isNull();
        assertThat(task.getScopeType()).isNull();

        task.setName("updatedtaskname");
        task.setDescription("updateddescription");
        task.setPriority(1);
        task.setAssignee("updatedassignee");
        task.setOwner("updatedowner");
        dueDate = sdf.parse("01/02/2003 04:05:06");
        task.setDueDate(dueDate);
        taskService.saveTask(task);

        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("updatedtaskname");
        assertThat(task.getDescription()).isEqualTo("updateddescription");
        assertThat(task.getAssignee()).isEqualTo("updatedassignee");
        assertThat(task.getOwner()).isEqualTo("updatedowner");
        assertThat(task.getDueDate()).isEqualTo(dueDate);
        assertThat(task.getPriority()).isEqualTo(1);

        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult();
            assertThat(historicTaskInstance.getName()).isEqualTo("updatedtaskname");
            assertThat(historicTaskInstance.getDescription()).isEqualTo("updateddescription");
            assertThat(historicTaskInstance.getAssignee()).isEqualTo("updatedassignee");
            assertThat(historicTaskInstance.getOwner()).isEqualTo("updatedowner");
            assertThat(historicTaskInstance.getDueDate()).isEqualTo(dueDate);
            assertThat(historicTaskInstance.getPriority()).isEqualTo(1);
        }

        // Finally, delete task
        taskService.deleteTask(task.getId(), true);
    }

    @Test
    public void testTaskOwner() {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setOwner("johndoe");
        taskService.saveTask(task);

        // Fetch the task again and update
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertThat(task.getOwner()).isEqualTo("johndoe");

        task.setOwner("joesmoe");
        taskService.saveTask(task);

        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertThat(task.getOwner()).isEqualTo("joesmoe");

        // Finally, delete task
        taskService.deleteTask(task.getId(), true);
    }

    @Test
    public void testTaskComments() {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            org.flowable.task.api.Task task = taskService.newTask();
            task.setOwner("johndoe");
            taskService.saveTask(task);
            String taskId = task.getId();

            identityService.setAuthenticatedUserId("johndoe");
            // Fetch the task again and update
            taskService
                    .addComment(
                            taskId,
                            null,
                            "look at this \n       isn't this great? slkdjf sldkfjs ldkfjs ldkfjs ldkfj sldkfj sldkfj sldkjg laksfg sdfgsd;flgkj ksajdhf skjdfh ksjdhf skjdhf kalskjgh lskh dfialurhg kajsh dfuieqpgkja rzvkfnjviuqerhogiuvysbegkjz lkhf ais liasduh flaisduh ajiasudh vaisudhv nsfd");
            Comment comment = taskService.getTaskComments(taskId).get(0);
            assertThat(comment.getUserId()).isEqualTo("johndoe");
            assertThat(comment.getTaskId()).isEqualTo(taskId);
            assertThat(comment.getProcessInstanceId()).isNull();
            assertThat(((Event) comment).getMessage()).isEqualTo(
                    "look at this isn't this great? slkdjf sldkfjs ldkfjs ldkfjs ldkfj sldkfj sldkfj sldkjg laksfg sdfgsd;flgkj ksajdhf skjdfh ksjdhf skjdhf kalskjgh lskh dfialurhg ...");
            assertThat(comment.getFullMessage()).isEqualTo(
                    "look at this \n       isn't this great? slkdjf sldkfjs ldkfjs ldkfjs ldkfj sldkfj sldkfj sldkjg laksfg sdfgsd;flgkj ksajdhf skjdfh ksjdhf skjdhf kalskjgh lskh dfialurhg kajsh dfuieqpgkja rzvkfnjviuqerhogiuvysbegkjz lkhf ais liasduh flaisduh ajiasudh vaisudhv nsfd");
            assertThat(comment.getTime()).isNotNull();

            // Finally, delete task
            taskService.deleteTask(taskId, true);
        }
    }

    @Test
    public void testCustomTaskComments() {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            org.flowable.task.api.Task task = taskService.newTask();
            task.setOwner("johndoe");
            taskService.saveTask(task);
            String taskId = task.getId();

            identityService.setAuthenticatedUserId("johndoe");
            String customType1 = "Type1";
            String customType2 = "Type2";

            Comment comment = taskService.addComment(taskId, null, "This is a regular comment");
            Comment customComment1 = taskService.addComment(taskId, null, customType1, "This is a custom comment of type Type1");
            taskService.addComment(taskId, null, customType1, "This is another Type1 comment");
            Comment customComment3 = taskService.addComment(taskId, null, customType2, "This is another custom comment. Type2 this time!");

            assertThat(comment.getType()).isEqualTo(CommentEntity.TYPE_COMMENT);
            assertThat(customComment1.getType()).isEqualTo(customType1);
            assertThat(customComment3.getType()).isEqualTo(customType2);

            assertThat(taskService.getComment(comment.getId())).isNotNull();
            assertThat(taskService.getComment(customComment1.getId())).isNotNull();

            List<Comment> regularComments = taskService.getTaskComments(taskId);
            assertThat(regularComments)
                    .extracting(Comment::getFullMessage)
                    .containsExactly("This is a regular comment");

            List<Event> allComments = taskService.getTaskEvents(taskId);
            assertThat(allComments).hasSize(4);

            List<Comment> type2Comments = taskService.getCommentsByType(customType2);
            assertThat(type2Comments)
                    .extracting(Comment::getFullMessage)
                    .containsExactly("This is another custom comment. Type2 this time!");

            List<Comment> taskTypeComments = taskService.getTaskComments(taskId, customType1);
            assertThat(taskTypeComments).hasSize(2);

            // Clean up
            taskService.deleteTask(taskId, true);
        }
    }
    
    @Test
    public void testUpdateTaskComments() {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            org.flowable.task.api.Task task = taskService.newTask();
            task.setOwner("johndoe");
            taskService.saveTask(task);
            String taskId = task.getId();

            identityService.setAuthenticatedUserId("johndoe");

            Comment comment = taskService.addComment(taskId, null, "This is a regular comment");

            assertThat(comment.getType()).isEqualTo(CommentEntity.TYPE_COMMENT);
            assertThat(taskService.getComment(comment.getId())).isNotNull();

            List<Comment> regularComments = taskService.getTaskComments(taskId);
            assertThat(regularComments)
                    .extracting(Comment::getFullMessage)
                    .containsExactly("This is a regular comment");

            comment.setFullMessage("Updated comment");
            taskService.saveComment(comment);

            regularComments = taskService.getTaskComments(taskId);
            assertThat(regularComments)
                    .extracting(Comment::getFullMessage)
                    .containsExactly("Updated comment");

            // Clean up
            taskService.deleteTask(taskId, true);
        }
    }

    @Test
    public void testTaskAttachments() {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            org.flowable.task.api.Task task = taskService.newTask();
            task.setOwner("johndoe");
            taskService.saveTask(task);
            String taskId = task.getId();
            identityService.setAuthenticatedUserId("johndoe");
            // Fetch the task again and update
            taskService.createAttachment("web page", taskId, null, "weatherforcast", "temperatures and more", "http://weather.com");
            Attachment attachment = taskService.getTaskAttachments(taskId).get(0);
            assertThat(attachment.getName()).isEqualTo("weatherforcast");
            assertThat(attachment.getDescription()).isEqualTo("temperatures and more");
            assertThat(attachment.getType()).isEqualTo("web page");
            assertThat(attachment.getTaskId()).isEqualTo(taskId);
            assertThat(attachment.getProcessInstanceId()).isNull();
            assertThat(attachment.getUrl()).isEqualTo("http://weather.com");
            assertThat(taskService.getAttachmentContent(attachment.getId())).isNull();

            // Finally, clean up
            taskService.deleteTask(taskId);

            assertThat(taskService.getTaskComments(taskId)).isEmpty();

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskId(taskId).list()).hasSize(1);

            taskService.deleteTask(taskId, true);
        }
    }

    @Test
    public void testSaveTaskAttachment() {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            org.flowable.task.api.Task task = taskService.newTask();
            task.setOwner("johndoe");
            taskService.saveTask(task);
            String taskId = task.getId();
            identityService.setAuthenticatedUserId("johndoe");

            // Fetch attachment and update its name
            taskService.createAttachment("web page", taskId, null, "weatherforcast", "temperatures and more", "http://weather.com");
            Attachment attachment = taskService.getTaskAttachments(taskId).get(0);
            attachment.setName("UpdatedName");
            taskService.saveAttachment(attachment);

            // Refetch and verify
            attachment = taskService.getTaskAttachments(taskId).get(0);
            assertThat(attachment.getName()).isEqualTo("UpdatedName");

            // Finally, clean up
            taskService.deleteTask(taskId);

            assertThat(taskService.getTaskComments(taskId)).isEmpty();

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            assertThat(historyService.createHistoricTaskInstanceQuery().taskId(taskId).list()).hasSize(1);

            taskService.deleteTask(taskId, true);
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testTaskAttachmentWithProcessInstanceId() {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

            String processInstanceId = processInstance.getId();
            taskService.createAttachment("web page", null, processInstanceId, "weatherforcast", "temperatures and more", "http://weather.com");
            Attachment attachment = taskService.getProcessInstanceAttachments(processInstanceId).get(0);
            assertThat(attachment.getName()).isEqualTo("weatherforcast");
            assertThat(attachment.getDescription()).isEqualTo("temperatures and more");
            assertThat(attachment.getType()).isEqualTo("web page");
            assertThat(attachment.getProcessInstanceId()).isEqualTo(processInstanceId);
            assertThat(attachment.getTaskId()).isNull();
            assertThat(attachment.getUrl()).isEqualTo("http://weather.com");
            assertThat(taskService.getAttachmentContent(attachment.getId())).isNull();

            // Finally, clean up
            taskService.deleteAttachment(attachment.getId());

            // TODO: Bad API design. Need to fix attachment/comment properly
            ((TaskServiceImpl) taskService).deleteComments(null, processInstanceId);
        }
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void saveUserTaskTest() {
        Map<String, Object> startMap = new HashMap<>();
        startMap.put("titleId", "testTitleId");
        startMap.put("otherVariable", "testOtherVariable");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", startMap);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId())
            .includeIdentityLinks()
            .singleResult();
        task.setDueDate(new Date());
        task.setCategory("main");
        taskService.saveTask(task);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testMultipleProcessesStarted() {

        // Start a few process instances
        for (int i = 0; i < 20; i++) {
            processEngine.getRuntimeService().startProcessInstanceByKey("oneTaskProcess");
        }

        // See if there are tasks for kermit
        List<org.flowable.task.api.Task> tasks = processEngine.getTaskService().createTaskQuery().list();
        assertThat(tasks).hasSize(20);
    }

    @Test
    public void testTaskDelegation() {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setOwner("johndoe");
        taskService.saveTask(task);
        taskService.delegateTask(task.getId(), "joesmoe");
        String taskId = task.getId();

        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertThat(task.getOwner()).isEqualTo("johndoe");
        assertThat(task.getAssignee()).isEqualTo("joesmoe");
        assertThat(task.getDelegationState()).isEqualTo(DelegationState.PENDING);

        // try to complete (should fail)
        assertThatThrownBy(() -> taskService.complete(taskId))
                .isExactlyInstanceOf(FlowableException.class);

        taskService.resolveTask(taskId);
        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertThat(task.getOwner()).isEqualTo("johndoe");
        assertThat(task.getAssignee()).isEqualTo("johndoe");
        assertThat(task.getDelegationState()).isEqualTo(DelegationState.RESOLVED);

        task.setAssignee(null);
        task.setDelegationState(null);
        taskService.saveTask(task);
        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertThat(task.getOwner()).isEqualTo("johndoe");
        assertThat(task.getAssignee()).isNull();
        assertThat(task.getDelegationState()).isNull();

        task.setAssignee("jackblack");
        task.setDelegationState(DelegationState.RESOLVED);
        taskService.saveTask(task);
        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertThat(task.getOwner()).isEqualTo("johndoe");
        assertThat(task.getAssignee()).isEqualTo("jackblack");
        assertThat(task.getDelegationState()).isEqualTo(DelegationState.RESOLVED);

        // Finally, delete task
        taskService.deleteTask(taskId, true);
    }

    @Test
    public void testTaskDelegationThroughServiceCall() {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setOwner("johndoe");
        taskService.saveTask(task);
        String taskId = task.getId();

        // Fetch the task again and update
        task = taskService.createTaskQuery().taskId(taskId).singleResult();

        taskService.delegateTask(task.getId(), "joesmoe");

        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertThat(task.getOwner()).isEqualTo("johndoe");
        assertThat(task.getAssignee()).isEqualTo("joesmoe");
        assertThat(task.getDelegationState()).isEqualTo(DelegationState.PENDING);

        taskService.resolveTask(taskId);

        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertThat(task.getOwner()).isEqualTo("johndoe");
        assertThat(task.getAssignee()).isEqualTo("johndoe");
        assertThat(task.getDelegationState()).isEqualTo(DelegationState.RESOLVED);

        // Finally, delete task
        taskService.deleteTask(taskId, true);
    }

    @Test
    public void testTaskAssignee() {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setAssignee("johndoe");
        taskService.saveTask(task);

        // Fetch the task again and update
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertThat(task.getAssignee()).isEqualTo("johndoe");

        task.setAssignee("joesmoe");
        taskService.saveTask(task);

        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertThat(task.getAssignee()).isEqualTo("joesmoe");

        // Finally, delete task
        taskService.deleteTask(task.getId(), true);
    }

    @Test
    public void testSaveTaskNullTask() {
        assertThatThrownBy(() -> taskService.saveTask(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("task is null");
    }

    @Test
    public void testDeleteTaskNullTaskId() {
        assertThatThrownBy(() -> taskService.deleteTask(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testDeleteTaskUnexistingTaskId() {
        // Deleting unexisting task should be silently ignored
        taskService.deleteTask("unexistingtaskid");
    }

    @Test
    public void testDeleteTasksNullTaskIds() {
        assertThatThrownBy(() -> taskService.deleteTasks(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testDeleteTasksTaskIdsUnexistingTaskId() {

        org.flowable.task.api.Task existingTask = taskService.newTask();
        taskService.saveTask(existingTask);

        // The unexisting taskId's should be silently ignored. Existing task should have been deleted.
        taskService.deleteTasks(Arrays.asList("unexistingtaskid1", existingTask.getId()), true);

        existingTask = taskService.createTaskQuery().taskId(existingTask.getId()).singleResult();
        assertThat(existingTask).isNull();
    }

    @Test
    public void testDeleteTaskIdentityLink() {
        org.flowable.task.api.Task task = null;
        try {
            task = taskService.newTask();
            task.setName("test");
            taskService.saveTask(task);

            taskService.addCandidateGroup(task.getId(), "sales");
            taskService.addCandidateUser(task.getId(), "kermit");

            assertThat(taskService.createTaskQuery().taskCandidateGroup("sales").singleResult()).isNotNull();
            assertThat(taskService.createTaskQuery().taskCandidateUser("kermit").singleResult()).isNotNull();

            // Delete identity link for group
            taskService.deleteGroupIdentityLink(task.getId(), "sales", "candidate");

            // Link should be removed
            assertThat(taskService.createTaskQuery().taskCandidateGroup("sales").singleResult()).isNull();

            // User link should remain unaffected
            assertThat(taskService.createTaskQuery().taskCandidateUser("kermit").singleResult()).isNotNull();

        } finally {
            // Adhoc task not part of deployment, cleanup
            if (task != null && task.getId() != null) {
                taskService.deleteTask(task.getId(), true);
            }
        }
    }

    @Test
    public void testClaimNullArguments() {
        assertThatThrownBy(() -> taskService.claim(null, "userid"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("taskId is null");
    }

    @Test
    public void testClaimUnexistingTaskId() {
        User user = identityService.newUser("user");
        identityService.saveUser(user);

        assertThatThrownBy(() -> taskService.claim("unexistingtaskid", user.getId()))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("Cannot find task with id unexistingtaskid");

        identityService.deleteUser(user.getId());
    }

    @Test
    public void testClaimAlreadyClaimedTaskByOtherUser() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);
        User user = identityService.newUser("user");
        identityService.saveUser(user);
        User secondUser = identityService.newUser("seconduser");
        identityService.saveUser(secondUser);

        // Claim task the first time
        taskService.claim(task.getId(), user.getId());

        assertThatThrownBy(() -> taskService.claim(task.getId(), secondUser.getId()))
                .isInstanceOf(FlowableTaskAlreadyClaimedException.class)
                .hasMessage("Task '" + task.getId() + "' is already claimed by someone else.");

        taskService.deleteTask(task.getId(), true);
        identityService.deleteUser(user.getId());
        identityService.deleteUser(secondUser.getId());
    }

    @Test
    public void testClaimAlreadyClaimedTaskBySameUser() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);
        User user = identityService.newUser("user");
        identityService.saveUser(user);

        // Claim task the first time
        taskService.claim(task.getId(), user.getId());
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();

        // Claim the task again with the same user. No exception should be thrown
        taskService.claim(task.getId(), user.getId());

        taskService.deleteTask(task.getId(), true);
        identityService.deleteUser(user.getId());
    }

    @Test
    public void testUnClaimTask() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);
        User user = identityService.newUser("user");
        identityService.saveUser(user);

        // Claim task the first time
        taskService.claim(task.getId(), user.getId());
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertThat(task.getAssignee()).isEqualTo(user.getId());

        // Unclaim the task
        taskService.unclaim(task.getId());

        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertThat(task.getAssignee()).isNull();

        taskService.deleteTask(task.getId(), true);
        identityService.deleteUser(user.getId());
    }

    @Test
    public void testCompleteTaskNullTaskId() {
        assertThatThrownBy(() -> taskService.complete(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("taskId is null");
    }

    @Test
    public void testCompleteTaskUnexistingTaskId() {
        assertThatThrownBy(() -> taskService.complete("unexistingtask"))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("Cannot find task with id unexistingtask");
    }

    @Test
    public void testCompleteTaskWithParametersNullTaskId() {
        assertThatThrownBy(() -> taskService.complete(null, new HashMap<>()))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("taskId is null");
    }

    @Test
    public void testCompleteTaskWithParametersUnexistingTaskId() {
        assertThatThrownBy(() -> taskService.complete("unexistingtask", new HashMap<>()))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("Cannot find task with id unexistingtask");
    }

    @Test
    public void testCompleteTaskWithParametersNullParameters() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);

        String taskId = task.getId();
        taskService.complete(taskId, (Map<String, Object>) null);

        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            historyService.deleteHistoricTaskInstance(taskId);
        }

        // Fetch the task again
        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertThat(task).isNull();

        managementService.executeCommand(commandContext -> {
            processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().deleteHistoricTaskLogEntriesForTaskId(taskId);
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCompleteTaskWithParametersEmptyParameters() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);

        String taskId = task.getId();
        taskService.complete(taskId, Collections.EMPTY_MAP);

        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            historyService.deleteHistoricTaskInstance(taskId);
        }

        // Fetch the task again
        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertThat(task).isNull();

        managementService.executeCommand(commandContext -> {
            processEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskService().deleteHistoricTaskLogEntriesForTaskId(taskId);
            return null;
        });
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testCompleteWithParametersTask() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");

        // Fetch first task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("First task");

        // Complete first task
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("myParam", "myValue");
        taskService.complete(task.getId(), taskParams);

        // Fetch second task
        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Second task");

        // Verify task parameters set on execution
        Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());
        assertThat(variables)
                .containsOnly(entry("myParam", "myValue"));
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testCompleteWithParametersTask2() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");

        // Fetch first task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("First task");

        // Complete first task
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("myParam", "myValue");
        taskService.complete(task.getId(), taskParams, false); // Only difference with previous test

        // Fetch second task
        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Second task");

        // Verify task parameters set on execution
        Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());
        assertThat(variables)
                .containsOnly(entry("myParam", "myValue"));
    }

    @Test
    @Deployment(resources = {"org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml"})
    public void testCompleteWithExecutionBaseNewPropertyExpressionTask() {
        runtimeService.startProcessInstanceByKey("twoTasksProcess");

        // Fetch first task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("First task");

        // Complete first task
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("${execution.myParam}", "myValue");
        assertThatThrownBy(() -> taskService.complete(task.getId(), taskParams))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("Error while evaluating expression: ${execution.myParam}");
    }

    @Test
    @Deployment(resources = {"org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml"})
    public void testCompleteWithExecutionIdParametersTask() {
        runtimeService.startProcessInstanceByKey("twoTasksProcess");

        // Fetch first task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("First task");

        // Complete first task
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("${execution.id}", "myValue");
        assertThatThrownBy(() -> taskService.complete(task.getId(), taskParams))
                .isExactlyInstanceOf(PersistenceException.class);
    }

    @Test
    @Deployment(resources = {"org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml"})
    public void testCompleteWithExecutionNameParametersTask() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");

        // Fetch first task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("First task");

        // Complete first task
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("${execution.name}", "myUpdatedName");
        taskService.complete(task.getId(), taskParams);

        // Verify task parameters set on execution
        Execution subExecution = runtimeService.createExecutionQuery().parentId(processInstance.getId()).singleResult();
        assertThat(subExecution.getName()).isEqualTo("myUpdatedName");
    }

    @Test
    @Deployment(resources = {"org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml"})
    public void testCompleteWithExistingVariableParametersTask_withoutBase() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess",
                Collections.singletonMap("newVariable", "oldValue")
            );

        // Fetch first task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("First task");

        // Complete first task
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("${newVariable}", "newVariableValue");
        taskService.complete(task.getId(), taskParams);

        // Verify task parameters set on execution
        Execution subExecution = runtimeService.createExecutionQuery().parentId(processInstance.getId()).singleResult();
        Map<String, VariableInstance> variableInstances = runtimeService.getVariableInstances(subExecution.getId());
        assertThat(variableInstances.get("newVariable").getTextValue()).isEqualTo("newVariableValue");

        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration )) {
            HistoricVariableInstance historicVariableInstance = this.historyService.createHistoricVariableInstanceQuery()
                .id(variableInstances.get("newVariable").getId())
                .singleResult();
            assertThat(historicVariableInstance.getValue()).isEqualTo("newVariableValue");
        }
    }

    @Test
    @Deployment(resources = {"org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml"})
    public void testCompleteWithExistingNewVariableParametersTask_withoutBase() {
        runtimeService.startProcessInstanceByKey("twoTasksProcess");

        // Fetch first task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("First task");

        // Complete first task
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("${newVariable}", "newVariableValue");

        assertThatThrownBy(() -> taskService.complete(task.getId(), taskParams))
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    @Deployment(resources = {"org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml"})
    public void testCompleteWithExistingNewVariableParametersTask() {
        runtimeService.startProcessInstanceByKey("twoTasksProcess");

        // Fetch first task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("First task");

        // Complete first task
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("${execution.newVariable}", "newVariableValue");

        assertThatThrownBy(() -> taskService.complete(task.getId(), taskParams))
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    @Deployment(resources = {"org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml"})
    public void testCompleteWithExistingVariableParametersTask() {
        runtimeService.startProcessInstanceByKey("twoTasksProcess",
                Collections.singletonMap("newVariable", "oldValue")
        );

        // Fetch first task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("First task");

        // Complete first task
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("${execution.newVariable}", "newVariableValue");

        assertThatThrownBy(() -> taskService.complete(task.getId(), taskParams))
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    @Deployment
    public void testCompleteWithTaskLocalParameters() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testTaskLocalVars");

        // Fetch first task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();

        // Complete first task
        TaskCompletionBuilder taskCompletionBuilder = taskService.createTaskCompletionBuilder();
        taskCompletionBuilder
                .taskId(task.getId())
                .variableLocal("a", 1)
                .variableLocal("b", 1)
                .complete();

        // Verify vars are not stored process instance wide
        assertThat(runtimeService.getVariable(processInstance.getId(), "a")).isNull();
        assertThat(runtimeService.getVariable(processInstance.getId(), "b")).isNull();

        // verify script listener has done its job
        assertThat(runtimeService.getVariable(processInstance.getId(), "sum")).isEqualTo(2);

        // Fetch second task
        taskService.createTaskQuery().singleResult();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskWithFormKeyProcess.bpmn20.xml" })
    public void taskFormModelExceptions() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskWithFormProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        assertThatThrownBy(() -> taskService.getTaskFormModel(task.getId(), true))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Form engine is not initialized");
        assertThatThrownBy(() -> taskService.getTaskFormModel(task.getId()))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Form engine is not initialized");

        assertThatThrownBy(() -> taskService.completeTaskWithForm(task.getId(), "formDefinitionId", "outcome", Collections.EMPTY_MAP))
                .isInstanceOf(FlowableIllegalArgumentException.class);
        assertThatThrownBy(() -> taskService.completeTaskWithForm(task.getId(), "formDefinitionId", "outcome", Collections.EMPTY_MAP, Collections.EMPTY_MAP))
                .isInstanceOf(FlowableIllegalArgumentException.class);
        assertThatThrownBy(() -> taskService.completeTaskWithForm(task.getId(), "formDefinitionId", "outcome", Collections.EMPTY_MAP, false))
                .isInstanceOf(FlowableIllegalArgumentException.class);

        taskService.complete(task.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskWithFormKeyProcess.bpmn20.xml" })
    public void testCompleteTaskWithFormKey() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskWithFormProcess");

        // Fetch task
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("my task");
        assertThat(task.getFormKey()).isEqualTo("myFormKey");
        assertThat(task.getAssignee()).isEqualTo("myAssignee");
        assertThat(task.getOwner()).isEqualTo("myOwner");
        assertThat(task.getCategory()).isEqualTo("myCategory");
        assertThat(task.getPriority()).isEqualTo(60);
        assertThat(task.getDueDate()).isNotNull();

        // Complete task
        taskService.complete(task.getId());

        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult();
            assertThat(historicTask.getName()).isEqualTo("my task");
            assertThat(historicTask.getFormKey()).isEqualTo("myFormKey");
            assertThat(historicTask.getAssignee()).isEqualTo("myAssignee");
            assertThat(historicTask.getOwner()).isEqualTo("myOwner");
            assertThat(historicTask.getCategory()).isEqualTo("myCategory");
            assertThat(historicTask.getPriority()).isEqualTo(60);
            assertThat(historicTask.getDueDate()).isNotNull();
        }
    }

    @Test
    public void testSetAssignee() {
        User user = identityService.newUser("user");
        identityService.saveUser(user);

        org.flowable.task.api.Task task = taskService.newTask();
        assertThat(task.getAssignee()).isNull();
        taskService.saveTask(task);

        // Set assignee
        taskService.setAssignee(task.getId(), user.getId());

        // Fetch task again
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertThat(task.getAssignee()).isEqualTo(user.getId());

        // Set assignee to null
        taskService.setAssignee(task.getId(), null);

        identityService.deleteUser(user.getId());
        taskService.deleteTask(task.getId(), true);
    }

    @Test
    public void testSetAssigneeNullTaskId() {
        assertThatThrownBy(() -> taskService.setAssignee(null, "userId"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("taskId is null");
    }

    @Test
    public void testSetAssigneeUnexistingTask() {
        User user = identityService.newUser("user");
        identityService.saveUser(user);

        assertThatThrownBy(() -> taskService.setAssignee("unexistingTaskId", user.getId()))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("Cannot find task with id unexistingTaskId");

        identityService.deleteUser(user.getId());
    }

    @Test
    public void testAddCandidateUserDuplicate() {
        // Check behavior when adding the same user twice as candidate
        User user = identityService.newUser("user");
        identityService.saveUser(user);

        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);

        taskService.addCandidateUser(task.getId(), user.getId());

        // Add as candidate the second time
        taskService.addCandidateUser(task.getId(), user.getId());

        identityService.deleteUser(user.getId());
        taskService.deleteTask(task.getId(), true);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testCountingTaskForAddRemoveIdentityLink() {
        processEngineConfiguration.setEnableTaskRelationshipCounts(true);
        TaskServiceConfiguration taskServiceConfiguration = (TaskServiceConfiguration) processEngineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG);
        taskServiceConfiguration.setEnableTaskRelationshipCounts(true);

        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();
        assertThat(((CountingTaskEntity) currentTask).getIdentityLinkCount()).isZero();

        taskService.addUserIdentityLink(currentTask.getId(), "user01", IdentityLinkType.PARTICIPANT);
        currentTask = taskService.createTaskQuery().singleResult();
        assertThat(((CountingTaskEntity) currentTask).getIdentityLinkCount()).isEqualTo(1);

        taskService.addUserIdentityLink(currentTask.getId(), "user02", IdentityLinkType.PARTICIPANT);
        currentTask = taskService.createTaskQuery().singleResult();
        assertThat(((CountingTaskEntity) currentTask).getIdentityLinkCount()).isEqualTo(2);

        taskService.deleteUserIdentityLink(currentTask.getId(), "user01", IdentityLinkType.PARTICIPANT);
        currentTask = taskService.createTaskQuery().singleResult();
        assertThat(((CountingTaskEntity) currentTask).getIdentityLinkCount()).isEqualTo(1);

        taskService.addGroupIdentityLink(currentTask.getId(), "group01", IdentityLinkType.PARTICIPANT);
        currentTask = taskService.createTaskQuery().singleResult();
        assertThat(((CountingTaskEntity) currentTask).getIdentityLinkCount()).isEqualTo(2);

        taskService.addGroupIdentityLink(currentTask.getId(), "group02", IdentityLinkType.PARTICIPANT);
        currentTask = taskService.createTaskQuery().singleResult();
        assertThat(((CountingTaskEntity) currentTask).getIdentityLinkCount()).isEqualTo(3);

        taskService.addGroupIdentityLink(currentTask.getId(), "group02", IdentityLinkType.PARTICIPANT);
        currentTask = taskService.createTaskQuery().singleResult();
        assertThat(((CountingTaskEntity) currentTask).getIdentityLinkCount()).isEqualTo(4);

        // start removing identity links
        taskService.deleteGroupIdentityLink(currentTask.getId(), "group02", IdentityLinkType.PARTICIPANT);
        currentTask = taskService.createTaskQuery().singleResult();
        // Two identityLinks with id "group02" are found. Both are deleted.
        assertThat(((CountingTaskEntity) currentTask).getIdentityLinkCount()).isEqualTo(2);

        // remove "user02" once
        taskService.deleteUserIdentityLink(currentTask.getId(), "user02", IdentityLinkType.PARTICIPANT);
        currentTask = taskService.createTaskQuery().singleResult();
        assertThat(((CountingTaskEntity) currentTask).getIdentityLinkCount()).isEqualTo(1);

        // remove "user02" twice
        taskService.deleteUserIdentityLink(currentTask.getId(), "user02", IdentityLinkType.PARTICIPANT);
        currentTask = taskService.createTaskQuery().singleResult();
        assertThat(((CountingTaskEntity) currentTask).getIdentityLinkCount()).isEqualTo(1);

        taskService.deleteGroupIdentityLink(currentTask.getId(), "group01", IdentityLinkType.PARTICIPANT);
        currentTask = taskService.createTaskQuery().singleResult();
        assertThat(((CountingTaskEntity) currentTask).getIdentityLinkCount()).isZero();

        // make sure the "identityLinkCount" value does not go negative
        taskService.deleteGroupIdentityLink(currentTask.getId(), "group01", IdentityLinkType.PARTICIPANT);
        currentTask = taskService.createTaskQuery().singleResult();
        assertThat(((CountingTaskEntity) currentTask).getIdentityLinkCount()).isZero();

        processEngineConfiguration.setEnableTaskRelationshipCounts(false);
        taskServiceConfiguration.setEnableTaskRelationshipCounts(false);
    }

    @Test
    public void testAddCandidateUserNullTaskId() {
        assertThatThrownBy(() -> taskService.addCandidateUser(null, "userId"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("taskId is null");
    }

    @Test
    public void testAddCandidateUserNullUserId() {
        assertThatThrownBy(() -> taskService.addCandidateUser("taskId", null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("identityId is null");
    }

    @Test
    public void testAddCandidateUserUnexistingTask() {
        User user = identityService.newUser("user");
        identityService.saveUser(user);

        assertThatThrownBy(() -> taskService.addCandidateUser("unexistingTaskId", user.getId()))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("Cannot find task with id unexistingTaskId");

        identityService.deleteUser(user.getId());
    }

    @Test
    public void testAddCandidateGroupNullTaskId() {
        assertThatThrownBy(() -> taskService.addCandidateGroup(null, "groupId"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("taskId is null");
    }

    @Test
    public void testAddCandidateGroupNullGroupId() {
        assertThatThrownBy(() -> taskService.addCandidateGroup("taskId", null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("identityId is null");
    }

    @Test
    public void testAddCandidateGroupUnexistingTask() {
        Group group = identityService.newGroup("group");
        identityService.saveGroup(group);
        assertThatThrownBy(() -> taskService.addCandidateGroup("unexistingTaskId", group.getId()))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("Cannot find task with id unexistingTaskId");
        identityService.deleteGroup(group.getId());
    }

    @Test
    public void testAddGroupIdentityLinkNullTaskId() {
        assertThatThrownBy(() -> taskService.addGroupIdentityLink(null, "groupId", IdentityLinkType.CANDIDATE))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("taskId is null");
    }

    @Test
    public void testAddGroupIdentityLinkNullUserId() {
        assertThatThrownBy(() -> taskService.addGroupIdentityLink("taskId", null, IdentityLinkType.CANDIDATE))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("identityId is null");
    }

    @Test
    public void testAddGroupIdentityLinkUnexistingTask() {
        User user = identityService.newUser("user");
        identityService.saveUser(user);

        assertThatThrownBy(() -> taskService.addGroupIdentityLink("unexistingTaskId", user.getId(), IdentityLinkType.CANDIDATE))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("Cannot find task with id unexistingTaskId");

        identityService.deleteUser(user.getId());
    }

    @Test
    public void testAddUserIdentityLinkNullTaskId() {
        assertThatThrownBy(() -> taskService.addUserIdentityLink(null, "userId", IdentityLinkType.CANDIDATE))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("taskId is null");
    }

    @Test
    public void testAddUserIdentityLinkNullUserId() {
        assertThatThrownBy(() -> taskService.addUserIdentityLink("taskId", null, IdentityLinkType.CANDIDATE))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("identityId is null");
    }

    @Test
    public void testAddUserIdentityLinkUnexistingTask() {
        User user = identityService.newUser("user");
        identityService.saveUser(user);

        assertThatThrownBy(() -> taskService.addUserIdentityLink("unexistingTaskId", user.getId(), IdentityLinkType.CANDIDATE))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("Cannot find task with id unexistingTaskId");

        identityService.deleteUser(user.getId());
    }

    @Test
    public void testGetIdentityLinksWithCandidateUser() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);
        String taskId = task.getId();

        identityService.saveUser(identityService.newUser("kermit"));

        taskService.addCandidateUser(taskId, "kermit");
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        assertThat(identityLinks)
                .extracting(IdentityLink::getUserId, IdentityLink::getGroupId, IdentityLink::getType)
                .containsExactly(tuple("kermit", null, IdentityLinkType.CANDIDATE));

        // cleanup
        taskService.deleteTask(taskId, true);
        identityService.deleteUser("kermit");
    }

    @Test
    public void testGetIdentityLinksWithCandidateGroup() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);
        String taskId = task.getId();

        identityService.saveGroup(identityService.newGroup("muppets"));

        taskService.addCandidateGroup(taskId, "muppets");
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        assertThat(identityLinks)
                .extracting(IdentityLink::getUserId, IdentityLink::getGroupId, IdentityLink::getType)
                .containsExactly(tuple(null, "muppets", IdentityLinkType.CANDIDATE));

        // cleanup
        taskService.deleteTask(taskId, true);
        identityService.deleteGroup("muppets");
    }

    @Test
    public void testGetIdentityLinksWithAssignee() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);
        String taskId = task.getId();

        identityService.saveUser(identityService.newUser("kermit"));

        taskService.claim(taskId, "kermit");
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        assertThat(identityLinks)
                .extracting(IdentityLink::getUserId, IdentityLink::getGroupId, IdentityLink::getType)
                .containsExactly(tuple("kermit", null, IdentityLinkType.ASSIGNEE));

        // cleanup
        taskService.deleteTask(taskId, true);
        identityService.deleteUser("kermit");
    }

    @Test
    public void testGetIdentityLinksWithNonExistingAssignee() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);
        String taskId = task.getId();

        taskService.claim(taskId, "nonExistingAssignee");
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        assertThat(identityLinks)
                .extracting(IdentityLink::getUserId, IdentityLink::getGroupId, IdentityLink::getType)
                .containsExactly(tuple("nonExistingAssignee", null, IdentityLinkType.ASSIGNEE));

        // cleanup
        taskService.deleteTask(taskId, true);
    }

    @Test
    public void testGetIdentityLinksWithOwner() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);
        String taskId = task.getId();

        identityService.saveUser(identityService.newUser("kermit"));
        identityService.saveUser(identityService.newUser("fozzie"));

        taskService.claim(taskId, "kermit");
        taskService.delegateTask(taskId, "fozzie");

        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        assertThat(identityLinks)
                .extracting(IdentityLink::getUserId, IdentityLink::getGroupId, IdentityLink::getType)
                .containsExactlyInAnyOrder(
                        tuple("fozzie", null, IdentityLinkType.ASSIGNEE),
                        tuple("kermit", null, IdentityLinkType.OWNER)
                );

        // cleanup
        taskService.deleteTask(taskId, true);
        identityService.deleteUser("kermit");
        identityService.deleteUser("fozzie");
    }

    @Test
    public void testGetIdentityLinksWithNonExistingOwner() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);
        String taskId = task.getId();

        taskService.claim(taskId, "nonExistingOwner");
        taskService.delegateTask(taskId, "nonExistingAssignee");
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);

        assertThat(identityLinks)
                .extracting(IdentityLink::getUserId, IdentityLink::getGroupId, IdentityLink::getType)
                .containsExactly(
                        tuple("nonExistingAssignee", null, IdentityLinkType.ASSIGNEE),
                        tuple("nonExistingOwner", null, IdentityLinkType.OWNER)
                );

        // cleanup
        taskService.deleteTask(taskId, true);
    }

    @Test
    public void testSetPriority() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);

        taskService.setPriority(task.getId(), 12345);

        // Fetch task again to check if the priority is set
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertThat(task.getPriority()).isEqualTo(12345);

        taskService.deleteTask(task.getId(), true);
    }

    @Test
    public void testSetPriorityUnexistingTaskId() {
        assertThatThrownBy(() -> taskService.setPriority("unexistingtask", 12345))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("Cannot find task with id unexistingtask");
    }

    @Test
    public void testSetPriorityNullTaskId() {
        assertThatThrownBy(() -> taskService.setPriority(null, 12345))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("taskId is null");
    }

    @Test
    public void testSetDueDate() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);

        // Set the due date to a non-null value
        // We need to make sure the time ends on .000, .003 or .007 due to SQL Server rounding to that
        Date now = Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS).plusMillis(843));
        taskService.setDueDate(task.getId(), now);

        // Fetch task to check if the due date was persisted
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertThat(task.getDueDate()).isNotNull();

        // Set the due date to null
        taskService.setDueDate(task.getId(), null);

        // Re-fetch the task to make sure the due date was set to null
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertThat(task.getDueDate()).isNull();

        // Call saveTask to update due date
        task = taskService.createTaskQuery().taskId(task.getId()).includeIdentityLinks().singleResult();
        // We need to make sure the time ends on .000, .003 or .007 due to SQL Server rounding to that
        now = Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS).plusMillis(987));
        task.setDueDate(now);
        taskService.saveTask(task);
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertThat(task.getDueDate()).isEqualTo(now);

        taskService.deleteTask(task.getId(), true);
    }

    @Test
    public void testSetDueDateUnexistingTaskId() {
        assertThatThrownBy(() -> taskService.setDueDate("unexistingtask", new Date()))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("Cannot find task with id unexistingtask");
    }

    @Test
    public void testSetDueDateNullTaskId() {
        assertThatThrownBy(() -> taskService.setDueDate(null, new Date()))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("taskId is null");
    }

    /**
     * @see <a href="https://activiti.atlassian.net/browse/ACT-1059">https://activiti.atlassian.net/browse/ACT-1059</a>
     */
    @Test
    public void testSetDelegationState() {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setOwner("wuzh");
        taskService.saveTask(task);
        taskService.delegateTask(task.getId(), "other");
        String taskId = task.getId();

        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertThat(task.getOwner()).isEqualTo("wuzh");
        assertThat(task.getAssignee()).isEqualTo("other");
        assertThat(task.getDelegationState()).isEqualTo(DelegationState.PENDING);

        task.setDelegationState(DelegationState.RESOLVED);
        taskService.saveTask(task);

        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertThat(task.getOwner()).isEqualTo("wuzh");
        assertThat(task.getAssignee()).isEqualTo("other");
        assertThat(task.getDelegationState()).isEqualTo(DelegationState.RESOLVED);

        taskService.deleteTask(taskId, true);
    }

    private void checkHistoricVariableUpdateEntity(String variableName, String processInstanceId) {
        if (isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            boolean deletedVariableUpdateFound = false;

            List<HistoricDetail> resultSet = historyService.createHistoricDetailQuery().processInstanceId(processInstanceId).list();
            for (HistoricDetail currentHistoricDetail : resultSet) {
                assertThat(currentHistoricDetail).isInstanceOf(HistoricDetailVariableInstanceUpdateEntity.class);
                HistoricDetailVariableInstanceUpdateEntity historicVariableUpdate = (HistoricDetailVariableInstanceUpdateEntity) currentHistoricDetail;

                if (historicVariableUpdate.getName().equals(variableName)) {
                    if (historicVariableUpdate.getValue() == null) {
                        if (deletedVariableUpdateFound) {
                            fail("Mismatch: A HistoricVariableUpdateEntity with a null value already found");
                        } else {
                            deletedVariableUpdateFound = true;
                        }
                    }
                }
            }

            assertThat(deletedVariableUpdateFound).isTrue();
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testRemoveVariable() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        taskService.setVariable(currentTask.getId(), "variable1", "value1");
        assertThat(taskService.getVariable(currentTask.getId(), "variable1")).isEqualTo("value1");
        assertThat(taskService.getVariableLocal(currentTask.getId(), "variable1")).isNull();

        taskService.removeVariable(currentTask.getId(), "variable1");

        assertThat(taskService.getVariable(currentTask.getId(), "variable1")).isNull();

        checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
    }

    @Test
    public void testRemoveVariableNullTaskId() {
        assertThatThrownBy(() -> taskService.removeVariable(null, "variable"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("taskId is null");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testRemoveVariables() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        Map<String, Object> varsToDelete = new HashMap<>();
        varsToDelete.put("variable1", "value1");
        varsToDelete.put("variable2", "value2");
        taskService.setVariables(currentTask.getId(), varsToDelete);
        taskService.setVariable(currentTask.getId(), "variable3", "value3");

        assertThat(taskService.getVariable(currentTask.getId(), "variable1")).isEqualTo("value1");
        assertThat(taskService.getVariable(currentTask.getId(), "variable2")).isEqualTo("value2");
        assertThat(taskService.getVariable(currentTask.getId(), "variable3")).isEqualTo("value3");
        assertThat(taskService.getVariableLocal(currentTask.getId(), "variable1")).isNull();
        assertThat(taskService.getVariableLocal(currentTask.getId(), "variable2")).isNull();
        assertThat(taskService.getVariableLocal(currentTask.getId(), "variable3")).isNull();

        taskService.removeVariables(currentTask.getId(), varsToDelete.keySet());

        assertThat(taskService.getVariable(currentTask.getId(), "variable1")).isNull();
        assertThat(taskService.getVariable(currentTask.getId(), "variable2")).isNull();
        assertThat(taskService.getVariable(currentTask.getId(), "variable3")).isEqualTo("value3");

        assertThat(taskService.getVariableLocal(currentTask.getId(), "variable1")).isNull();
        assertThat(taskService.getVariableLocal(currentTask.getId(), "variable2")).isNull();
        assertThat(taskService.getVariableLocal(currentTask.getId(), "variable3")).isNull();

        checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
        checkHistoricVariableUpdateEntity("variable2", processInstance.getId());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRemoveVariablesNullTaskId() {
        assertThatThrownBy(() -> taskService.removeVariables(null, new HashSet<>()))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("taskId is null");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testRemoveVariableLocal() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        taskService.setVariableLocal(currentTask.getId(), "variable1", "value1");
        assertThat(taskService.getVariable(currentTask.getId(), "variable1")).isEqualTo("value1");
        assertThat(taskService.getVariableLocal(currentTask.getId(), "variable1")).isEqualTo("value1");

        taskService.removeVariableLocal(currentTask.getId(), "variable1");

        assertThat(taskService.getVariable(currentTask.getId(), "variable1")).isNull();
        assertThat(taskService.getVariableLocal(currentTask.getId(), "variable1")).isNull();

        checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testCountingTaskForAddRemoveVariable() {
        processEngineConfiguration.setEnableTaskRelationshipCounts(true);
        TaskServiceConfiguration taskServiceConfiguration = (TaskServiceConfiguration) processEngineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG);
        taskServiceConfiguration.setEnableTaskRelationshipCounts(true);
        
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        assertThat(((CountingTaskEntity) currentTask).getVariableCount()).isZero();

        taskService.setVariableLocal(currentTask.getId(), "variable1", "value1");
        currentTask = taskService.createTaskQuery().singleResult();

        assertThat(((CountingTaskEntity) currentTask).getVariableCount()).isEqualTo(1);

        // process variables should have no effect
        taskService.setVariable(currentTask.getId(), "processVariable1", "procValue1");
        currentTask = taskService.createTaskQuery().singleResult();

        assertThat(((CountingTaskEntity) currentTask).getVariableCount()).isEqualTo(1);

        Map<String, Object> localVars = new HashMap<>();
        localVars.put("localVar1", "localValue1");
        localVars.put("localVar2", "localValue2");
        localVars.put("localVar3", "localValue3");

        taskService.setVariablesLocal(currentTask.getId(), localVars);
        currentTask = taskService.createTaskQuery().singleResult();

        assertThat(((CountingTaskEntity) currentTask).getVariableCount()).isEqualTo(4);

        taskService.removeVariablesLocal(currentTask.getId(), localVars.keySet());
        currentTask = taskService.createTaskQuery().singleResult();
        assertThat(((CountingTaskEntity) currentTask).getVariableCount()).isEqualTo(1);

        taskService.removeVariablesLocal(currentTask.getId(), localVars.keySet());
        currentTask = taskService.createTaskQuery().singleResult();
        assertThat(((CountingTaskEntity) currentTask).getVariableCount()).isEqualTo(1);

        taskService.removeVariableLocal(currentTask.getId(), "variable1");
        currentTask = taskService.createTaskQuery().singleResult();
        assertThat(((CountingTaskEntity) currentTask).getVariableCount()).isZero();

        // make sure it does not get negative
        taskService.removeVariableLocal(currentTask.getId(), "variable1");
        currentTask = taskService.createTaskQuery().singleResult();
        assertThat(((CountingTaskEntity) currentTask).getVariableCount()).isZero();

        processEngineConfiguration.setEnableTaskRelationshipCounts(false);
        taskServiceConfiguration.setEnableTaskRelationshipCounts(false);
    }

    @Test
    public void testRemoveVariableLocalNullTaskId() {
        assertThatThrownBy(() -> taskService.removeVariableLocal(null, "variable"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("taskId is null");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testRemoveVariablesLocal() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        Map<String, Object> varsToDelete = new HashMap<>();
        varsToDelete.put("variable1", "value1");
        varsToDelete.put("variable2", "value2");
        taskService.setVariablesLocal(currentTask.getId(), varsToDelete);
        taskService.setVariableLocal(currentTask.getId(), "variable3", "value3");

        assertThat(taskService.getVariable(currentTask.getId(), "variable1")).isEqualTo("value1");
        assertThat(taskService.getVariable(currentTask.getId(), "variable2")).isEqualTo("value2");
        assertThat(taskService.getVariable(currentTask.getId(), "variable3")).isEqualTo("value3");
        assertThat(taskService.getVariableLocal(currentTask.getId(), "variable1")).isEqualTo("value1");
        assertThat(taskService.getVariableLocal(currentTask.getId(), "variable2")).isEqualTo("value2");
        assertThat(taskService.getVariableLocal(currentTask.getId(), "variable3")).isEqualTo("value3");

        taskService.removeVariables(currentTask.getId(), varsToDelete.keySet());

        assertThat(taskService.getVariable(currentTask.getId(), "variable1")).isNull();
        assertThat(taskService.getVariable(currentTask.getId(), "variable2")).isNull();
        assertThat(taskService.getVariable(currentTask.getId(), "variable3")).isEqualTo("value3");

        assertThat(taskService.getVariableLocal(currentTask.getId(), "variable1")).isNull();
        assertThat(taskService.getVariableLocal(currentTask.getId(), "variable2")).isNull();
        assertThat(taskService.getVariableLocal(currentTask.getId(), "variable3")).isEqualTo("value3");

        checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
        checkHistoricVariableUpdateEntity("variable2", processInstance.getId());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRemoveVariablesLocalNullTaskId() {
        assertThatThrownBy(() -> taskService.removeVariablesLocal(null, new HashSet<>()))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("taskId is null");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableByHistoricActivityInstance() throws Exception {
        if (isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            assertThat(processInstance).isNotNull();
            org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();

            taskService.setVariable(task.getId(), "variable1", "value1");
            Thread.sleep(50L); // to make sure the times for ordering below are different.
            taskService.setVariable(task.getId(), "variable1", "value2");
            
            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            HistoricActivityInstance historicActivitiInstance = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityId("theTask").singleResult();
            assertThat(historicActivitiInstance).isNotNull();

            List<HistoricDetail> resultSet = historyService.createHistoricDetailQuery().variableUpdates()
                    .orderByTime()
                    .asc()
                    .list();

            assertThat(resultSet).hasSize(2);
            assertThat(((HistoricVariableUpdate) resultSet.get(0)).getVariableName()).isEqualTo("variable1");
            assertThat(((HistoricVariableUpdate) resultSet.get(0)).getValue()).isEqualTo("value1");
            assertThat(((HistoricVariableUpdate) resultSet.get(1)).getVariableName()).isEqualTo("variable1");
            assertThat(((HistoricVariableUpdate) resultSet.get(1)).getValue()).isEqualTo("value2");

            resultSet = historyService.createHistoricDetailQuery().variableUpdates()
                    .activityInstanceId(historicActivitiInstance.getId())
                    .orderByTime()
                    .asc()
                    .list();

            assertThat(resultSet).hasSize(2);
            assertThat(((HistoricVariableUpdate) resultSet.get(0)).getVariableName()).isEqualTo("variable1");
            assertThat(((HistoricVariableUpdate) resultSet.get(0)).getValue()).isEqualTo("value1");
            assertThat(((HistoricVariableUpdate) resultSet.get(1)).getVariableName()).isEqualTo("variable1");
            assertThat(((HistoricVariableUpdate) resultSet.get(1)).getValue()).isEqualTo("value2");
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableByActivityInstance() throws Exception {
        if (isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            assertThat(processInstance).isNotNull();
            org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();

            taskService.setVariable(task.getId(), "variable1", "value1");
            Thread.sleep(50L); // to make sure the times for ordering below are different.
            taskService.setVariable(task.getId(), "variable1", "value2");

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            ActivityInstance activityInstance = runtimeService.createActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityId("theTask").singleResult();
            assertThat(activityInstance).isNotNull();

            List<HistoricDetail> resultSet = historyService.createHistoricDetailQuery().variableUpdates()
                    .orderByTime()
                    .asc()
                    .list();

            assertThat(resultSet).hasSize(2);
            assertThat(((HistoricVariableUpdate) resultSet.get(0)).getVariableName()).isEqualTo("variable1");
            assertThat(((HistoricVariableUpdate) resultSet.get(0)).getValue()).isEqualTo("value1");
            assertThat(((HistoricVariableUpdate) resultSet.get(1)).getVariableName()).isEqualTo("variable1");
            assertThat(((HistoricVariableUpdate) resultSet.get(1)).getValue()).isEqualTo("value2");

            resultSet = historyService.createHistoricDetailQuery().variableUpdates()
                    .activityInstanceId(activityInstance.getId())
                    .orderByTime()
                    .asc()
                    .list();

            assertThat(resultSet).hasSize(2);
            assertThat(((HistoricVariableUpdate) resultSet.get(0)).getVariableName()).isEqualTo("variable1");
            assertThat(((HistoricVariableUpdate) resultSet.get(0)).getValue()).isEqualTo("value1");
            assertThat(((HistoricVariableUpdate) resultSet.get(1)).getVariableName()).isEqualTo("variable1");
            assertThat(((HistoricVariableUpdate) resultSet.get(1)).getValue()).isEqualTo("value2");
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testUserTaskOptimisticLocking() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task task1 = taskService.createTaskQuery().singleResult();
        org.flowable.task.api.Task task2 = taskService.createTaskQuery().singleResult();

        task1.setDescription("test description one");
        taskService.saveTask(task1);

        assertThatThrownBy(() -> {
            task2.setDescription("test description two");
            taskService.saveTask(task2);
        })
                .isExactlyInstanceOf(FlowableOptimisticLockingException.class);
    }

    @Test
    public void testDeleteTaskWithDeleteReason() {
        // ACT-900: deleteReason can be manually specified - can only be
        // validated when historyLevel > ACTIVITY
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            org.flowable.task.api.Task task = taskService.newTask();
            task.setName("test task");
            taskService.saveTask(task);

            assertThat(task.getId()).isNotNull();

            taskService.deleteTask(task.getId(), "deleted for testing purposes");

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult();

            assertThat(historicTaskInstance).isNotNull();
            assertThat(historicTaskInstance.getDeleteReason()).isEqualTo("deleted for testing purposes");

            // Delete historic task that is left behind, will not be cleaned up
            // because this is not part of a process
            taskService.deleteTask(task.getId(), true);
        }
    }

    @Test
    public void testResolveTaskNullTaskId() {
        assertThatThrownBy(() -> taskService.resolveTask(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("taskId is null");
    }

    @Test
    public void testResolveTaskUnexistingTaskId() {
        assertThatThrownBy(() -> taskService.resolveTask("blergh"))
                .isExactlyInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("Cannot find task with id blergh");
    }

    @Test
    public void testResolveTaskWithParametersNullParameters() {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setDelegationState(DelegationState.PENDING);
        taskService.saveTask(task);

        String taskId = task.getId();
        taskService.resolveTask(taskId, null);

        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            historyService.deleteHistoricTaskInstance(taskId);
        }

        // Fetch the task again
        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertThat(task.getDelegationState()).isEqualTo(DelegationState.RESOLVED);

        taskService.deleteTask(taskId, true);
    }

    @Test
    public void resolveTaskWithParametersNullParametersEmptyTransientVariables() {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setDelegationState(DelegationState.PENDING);
        taskService.saveTask(task);

        String taskId = task.getId();
        taskService.resolveTask(taskId, null, Collections.EMPTY_MAP);

        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            historyService.deleteHistoricTaskInstance(taskId);
        }

        // Fetch the task again
        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertThat(task.getDelegationState()).isEqualTo(DelegationState.RESOLVED);

        taskService.deleteTask(taskId, true);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testResolveTaskWithParametersEmptyParameters() {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setDelegationState(DelegationState.PENDING);
        taskService.saveTask(task);

        String taskId = task.getId();
        taskService.resolveTask(taskId, Collections.EMPTY_MAP);

        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            historyService.deleteHistoricTaskInstance(taskId);
        }

        // Fetch the task again
        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertThat(task.getDelegationState()).isEqualTo(DelegationState.RESOLVED);

        taskService.deleteTask(taskId, true);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testResolveWithParametersTask() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");

        // Fetch first task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("First task");

        taskService.delegateTask(task.getId(), "johndoe");

        // Resolve first task
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("myParam", "myValue");
        taskService.resolveTask(task.getId(), taskParams);

        // Verify that task is resolved
        task = taskService.createTaskQuery().taskDelegationState(DelegationState.RESOLVED).singleResult();
        assertThat(task.getName()).isEqualTo("First task");

        // Verify task parameters set on execution
        Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());
        assertThat(variables)
                .containsOnly(entry("myParam", "myValue"));
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testDeleteTaskPartOfProcess() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task).isNotNull();

        assertThatThrownBy(() -> taskService.deleteTask(task.getId()))
                .isInstanceOf(FlowableException.class)
                .hasMessage("The task cannot be deleted because is part of a running process");

        assertThatThrownBy(() -> taskService.deleteTask(task.getId(), true))
                .isInstanceOf(FlowableException.class)
                .hasMessage("The task cannot be deleted because is part of a running process");

        assertThatThrownBy(() -> taskService.deleteTask(task.getId(), "test"))
                .isInstanceOf(FlowableException.class)
                .hasMessage("The task cannot be deleted because is part of a running process");

        String idLists[] = { task.getId() };
        assertThatThrownBy(() -> taskService.deleteTasks(Arrays.asList(idLists)))
                .isInstanceOf(FlowableException.class)
                .hasMessage("The task cannot be deleted because is part of a running process");

        assertThatThrownBy(() -> taskService.deleteTasks(Arrays.asList(idLists), true))
                .isInstanceOf(FlowableException.class)
                .hasMessage("The task cannot be deleted because is part of a running process");

        assertThatThrownBy(() -> taskService.deleteTasks(Arrays.asList(idLists), "test"))
                .isInstanceOf(FlowableException.class)
                .hasMessage("The task cannot be deleted because is part of a running process");
    }

    @Test
    @Deployment
    public void testFormKeyExpression() {
        runtimeService.startProcessInstanceByKey("testFormExpression", CollectionUtil.singletonMap("var", "abc"));

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getFormKey()).isEqualTo("first-form.json");
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getFormKey()).isEqualTo("form-abc.json");

        task.setFormKey("form-changed.json");
        taskService.saveTask(task);
        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getFormKey()).isEqualTo("form-changed.json");

        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult();
            assertThat(historicTaskInstance.getFormKey()).isEqualTo("form-changed.json");
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableLocalWithCast() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        taskService.setVariableLocal(currentTask.getId(), "variable1", "value1");

        String variable = taskService.getVariableLocal(currentTask.getId(), "variable1", String.class);

        assertThat(variable).isEqualTo("value1");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableLocalNotExistingWithCast() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        String variable = taskService.getVariableLocal(currentTask.getId(), "variable1", String.class);

        assertThat(variable).isNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableLocalWithInvalidCast() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        taskService.setVariableLocal(currentTask.getId(), "variable1", "value1");

        assertThatThrownBy(() -> taskService.getVariableLocal(currentTask.getId(), "variable1", Boolean.class))
            .isExactlyInstanceOf(ClassCastException.class);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableWithCast() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        taskService.setVariable(currentTask.getId(), "variable1", "value1");

        String variable = taskService.getVariable(currentTask.getId(), "variable1", String.class);

        assertThat(variable).isEqualTo("value1");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableNotExistingWithCast() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        String variable = taskService.getVariable(currentTask.getId(), "variable1", String.class);

        assertThat(variable).isNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableWithInvalidCast() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        taskService.setVariable(currentTask.getId(), "variable1", "value1");

        assertThatThrownBy(() -> taskService.getVariable(currentTask.getId(), "variable1", Boolean.class))
            .isExactlyInstanceOf(ClassCastException.class);
    }

    @Test
    public void testClaimTime() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);
        User user = identityService.newUser("user");
        identityService.saveUser(user);

        assertThat(task.getClaimTime()).isNull();

        // Claim task
        taskService.claim(task.getId(), user.getId());
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();

        assertThat(task.getClaimTime()).isNotNull();

        // Unclaim task
        taskService.unclaim(task.getId());
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();

        assertThat(task.getClaimTime()).isNull();

        taskService.deleteTask(task.getId(), true);
        identityService.deleteUser(user.getId());
    }

    private static Set<IdentityLinkEntityImpl> getDefaultIdentityLinks() {
        IdentityLinkEntityImpl identityLinkEntityCandidateUser = new IdentityLinkEntityImpl();
        identityLinkEntityCandidateUser.setUserId("testUserBuilder");
        identityLinkEntityCandidateUser.setType(IdentityLinkType.CANDIDATE);
        IdentityLinkEntityImpl identityLinkEntityCandidateGroup = new IdentityLinkEntityImpl();
        identityLinkEntityCandidateGroup.setGroupId("testGroupBuilder");
        identityLinkEntityCandidateGroup.setType(IdentityLinkType.CANDIDATE);

        return Stream.of(
                identityLinkEntityCandidateUser,
                identityLinkEntityCandidateGroup
        ).collect(toSet());
    }

}
