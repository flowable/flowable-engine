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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
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
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.TaskPostProcessor;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;

/**
 * @author Frederik Heremans
 * @author Joram Barrez
 * @author Falko Menge
 */
public class TaskServiceTest extends PluggableFlowableTestCase {

    private Task task = null;

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (task != null) {
            taskService.deleteTask(task.getId(), true);
        }
    }

    public void testCreateTaskWithBuilder() {
        task = taskService.createTaskBuilder().
                        name("testName").
                        description("testDescription").
                        priority(35).
                        owner("testOwner").
                        assignee("testAssignee").
                        dueDate(new Date(0)).
                        category("testCategory").
                        parentTaskId("testParentTaskId").
                        tenantId("testTenantId").
                        formKey("testFormKey").
                        taskDefinitionId("testDefintionId").
                        taskDefinitionKey("testDefinitionKey").
                        scopeType(ScopeTypes.CMMN).
                        scopeId("scopeIdValue").
                        create();
        Task updatedTask = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertThat(updatedTask, notNullValue());
        assertThat(updatedTask.getName(), is("testName"));
        assertThat(updatedTask.getDescription(), is("testDescription"));
        assertThat(updatedTask.getPriority(), is(35));
        assertThat(updatedTask.getOwner(), is("testOwner"));
        assertThat(updatedTask.getAssignee(), is("testAssignee"));
        assertThat(updatedTask.getDueDate(), is(new Date(0)));
        assertThat(updatedTask.getCategory(), is("testCategory"));
        assertThat(updatedTask.getParentTaskId(), is("testParentTaskId"));
        assertThat(updatedTask.getTenantId(), is("testTenantId"));
        assertThat(updatedTask.getFormKey(), is("testFormKey"));
        assertThat(updatedTask.getTaskDefinitionId(), is("testDefintionId"));
        assertThat(updatedTask.getTaskDefinitionKey(), is("testDefinitionKey"));
        assertThat(updatedTask.getScopeId(), is("scopeIdValue"));
        assertThat(updatedTask.getScopeType(), is(ScopeTypes.CMMN));
    }

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
            assertThat(((CountingTaskEntity) updatedParentTask).getSubTaskCount(), is(1));
            Task updatedTask = taskService.createTaskQuery().taskId(task.getId()).includeIdentityLinks().singleResult();
            assertThat(((CountingTaskEntity) updatedTask).getIdentityLinkCount(), is(2));
        } finally {
            this.taskService.deleteTask(parentTask.getId(), true);
        }
    }

    public void testCreateTaskWithOwnerAssigneeAndIdentityLinks() {
        task = taskService.createTaskBuilder().
                        name("testName").
                        owner("testOwner").
                        assignee("testAssignee").
                        identityLinks(getDefaultIdentityLinks()).
                        create();
        Task updatedTask = taskService.createTaskQuery().taskId(task.getId()).includeIdentityLinks().singleResult();
        assertThat(updatedTask, notNullValue());
        assertThat(updatedTask.getName(), is("testName"));
        assertThat(updatedTask.getAssignee(), is("testAssignee"));
        assertThat(updatedTask.getOwner(), is("testOwner"));
        assertThat(updatedTask.getIdentityLinks().size(), is(2));
        assertThat(updatedTask.getPriority(), is(Task.DEFAULT_PRIORITY));
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).includeIdentityLinks().singleResult();
            assertThat(historicTaskInstance, notNullValue());
            assertThat(historicTaskInstance.getName(), is("testName"));
            assertThat(historicTaskInstance.getPriority(), is(Task.DEFAULT_PRIORITY));
            assertThat(historicTaskInstance.getIdentityLinks().size(), is(2));
        }

        taskService.deleteUserIdentityLink(updatedTask.getId(), "testUserBuilder", IdentityLinkType.CANDIDATE);
        taskService.deleteGroupIdentityLink(updatedTask.getId(), "testGroupBuilder", IdentityLinkType.CANDIDATE);
    }

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
            assertThat(updatedTask, notNullValue());
            assertThat(updatedTask.getName(), is("testName"));
            assertThat(updatedTask.getIdentityLinks().size(), is(2));
            
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).includeIdentityLinks().singleResult();
                assertThat(historicTaskInstance, notNullValue());
                assertThat(historicTaskInstance.getName(), is("testName"));
                assertThat(historicTaskInstance.getIdentityLinks().size(), is(2));
            }

            taskService.deleteUserIdentityLink(updatedTask.getId(), "testUser", IdentityLinkType.CANDIDATE);
            taskService.deleteGroupIdentityLink(updatedTask.getId(), "testGroup", IdentityLinkType.CANDIDATE);
        } finally {
            taskServiceConfiguration.setTaskPostProcessor(previousTaskPostProcessor);
        }
    }

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
            assertThat(updatedTask, notNullValue());
            assertThat(updatedTask.getName(), is("testNameFromPostProcessor"));
            assertThat(updatedTask.getAssignee(), is("testAssignee"));
            assertThat(updatedTask.getOwner(), is("testOwner"));
            assertThat(updatedTask.getIdentityLinks().size(), is(4));
            
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).includeIdentityLinks().singleResult();
                assertThat(historicTaskInstance, notNullValue());
                assertThat(historicTaskInstance.getName(), is("testNameFromPostProcessor"));
                assertThat(historicTaskInstance.getIdentityLinks().size(), is(4));
            }

            taskService.deleteUserIdentityLink(updatedTask.getId(), "testUserBuilder", IdentityLinkType.CANDIDATE);
            taskService.deleteGroupIdentityLink(updatedTask.getId(), "testGroupBuilder", IdentityLinkType.CANDIDATE);
            taskService.deleteUserIdentityLink(updatedTask.getId(), "testUser", IdentityLinkType.CANDIDATE);
            taskService.deleteGroupIdentityLink(updatedTask.getId(), "testGroup", IdentityLinkType.CANDIDATE);
        } finally {
            taskServiceConfiguration.setTaskPostProcessor(previousTaskPostProcessor);
        }
    }

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
        assertEquals("description", task.getDescription());
        assertEquals("taskname", task.getName());
        assertEquals("taskassignee", task.getAssignee());
        assertEquals("taskowner", task.getOwner());
        assertEquals(dueDate, task.getDueDate());
        assertEquals(0, task.getPriority());
        assertNull(task.getScopeId());
        assertNull(task.getScopeType());

        task.setName("updatedtaskname");
        task.setDescription("updateddescription");
        task.setPriority(1);
        task.setAssignee("updatedassignee");
        task.setOwner("updatedowner");
        dueDate = sdf.parse("01/02/2003 04:05:06");
        task.setDueDate(dueDate);
        taskService.saveTask(task);

        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertEquals("updatedtaskname", task.getName());
        assertEquals("updateddescription", task.getDescription());
        assertEquals("updatedassignee", task.getAssignee());
        assertEquals("updatedowner", task.getOwner());
        assertEquals(dueDate, task.getDueDate());
        assertEquals(1, task.getPriority());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult();
            assertEquals("updatedtaskname", historicTaskInstance.getName());
            assertEquals("updateddescription", historicTaskInstance.getDescription());
            assertEquals("updatedassignee", historicTaskInstance.getAssignee());
            assertEquals("updatedowner", historicTaskInstance.getOwner());
            assertEquals(dueDate, historicTaskInstance.getDueDate());
            assertEquals(1, historicTaskInstance.getPriority());
        }

        // Finally, delete task
        taskService.deleteTask(task.getId(), true);
    }

    public void testTaskOwner() {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setOwner("johndoe");
        taskService.saveTask(task);

        // Fetch the task again and update
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertEquals("johndoe", task.getOwner());

        task.setOwner("joesmoe");
        taskService.saveTask(task);

        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertEquals("joesmoe", task.getOwner());

        // Finally, delete task
        taskService.deleteTask(task.getId(), true);
    }

    public void testTaskComments() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
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
            assertEquals("johndoe", comment.getUserId());
            assertEquals(taskId, comment.getTaskId());
            assertNull(comment.getProcessInstanceId());
            assertEquals("look at this isn't this great? slkdjf sldkfjs ldkfjs ldkfjs ldkfj sldkfj sldkfj sldkjg laksfg sdfgsd;flgkj ksajdhf skjdfh ksjdhf skjdhf kalskjgh lskh dfialurhg ...",
                    ((Event) comment).getMessage());
            assertEquals(
                    "look at this \n       isn't this great? slkdjf sldkfjs ldkfjs ldkfjs ldkfj sldkfj sldkfj sldkjg laksfg sdfgsd;flgkj ksajdhf skjdfh ksjdhf skjdhf kalskjgh lskh dfialurhg kajsh dfuieqpgkja rzvkfnjviuqerhogiuvysbegkjz lkhf ais liasduh flaisduh ajiasudh vaisudhv nsfd",
                    comment.getFullMessage());
            assertNotNull(comment.getTime());

            // Finally, delete task
            taskService.deleteTask(taskId, true);
        }
    }

    public void testCustomTaskComments() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            org.flowable.task.api.Task task = taskService.newTask();
            task.setOwner("johndoe");
            taskService.saveTask(task);
            String taskId = task.getId();

            identityService.setAuthenticatedUserId("johndoe");
            String customType1 = "Type1";
            String customType2 = "Type2";

            Comment comment = taskService.addComment(taskId, null, "This is a regular comment");
            Comment customComment1 = taskService.addComment(taskId, null, customType1, "This is a custom comment of type Type1");
            Comment customComment2 = taskService.addComment(taskId, null, customType1, "This is another Type1 comment");
            Comment customComment3 = taskService.addComment(taskId, null, customType2, "This is another custom comment. Type2 this time!");

            assertEquals(CommentEntity.TYPE_COMMENT, comment.getType());
            assertEquals(customType1, customComment1.getType());
            assertEquals(customType2, customComment3.getType());

            assertNotNull(taskService.getComment(comment.getId()));
            assertNotNull(taskService.getComment(customComment1.getId()));

            List<Comment> regularComments = taskService.getTaskComments(taskId);
            assertEquals(1, regularComments.size());
            assertEquals("This is a regular comment", regularComments.get(0).getFullMessage());

            List<Event> allComments = taskService.getTaskEvents(taskId);
            assertEquals(4, allComments.size());

            List<Comment> type2Comments = taskService.getCommentsByType(customType2);
            assertEquals(1, type2Comments.size());
            assertEquals("This is another custom comment. Type2 this time!", type2Comments.get(0).getFullMessage());

            List<Comment> taskTypeComments = taskService.getTaskComments(taskId, customType1);
            assertEquals(2, taskTypeComments.size());

            // Clean up
            taskService.deleteTask(taskId, true);
        }
    }
    
    public void testUpdateTaskComments() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            org.flowable.task.api.Task task = taskService.newTask();
            task.setOwner("johndoe");
            taskService.saveTask(task);
            String taskId = task.getId();

            identityService.setAuthenticatedUserId("johndoe");

            Comment comment = taskService.addComment(taskId, null, "This is a regular comment");

            assertEquals(CommentEntity.TYPE_COMMENT, comment.getType());
            assertNotNull(taskService.getComment(comment.getId()));
            
            List<Comment> regularComments = taskService.getTaskComments(taskId);
            assertEquals(1, regularComments.size());
            assertEquals("This is a regular comment", regularComments.get(0).getFullMessage());

            comment.setFullMessage("Updated comment");
            taskService.saveComment(comment);
            
            regularComments = taskService.getTaskComments(taskId);
            assertEquals(1, regularComments.size());
            assertEquals("Updated comment", regularComments.get(0).getFullMessage());

            // Clean up
            taskService.deleteTask(taskId, true);
        }
    }

    public void testTaskAttachments() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            org.flowable.task.api.Task task = taskService.newTask();
            task.setOwner("johndoe");
            taskService.saveTask(task);
            String taskId = task.getId();
            identityService.setAuthenticatedUserId("johndoe");
            // Fetch the task again and update
            taskService.createAttachment("web page", taskId, null, "weatherforcast", "temperatures and more", "http://weather.com");
            Attachment attachment = taskService.getTaskAttachments(taskId).get(0);
            assertEquals("weatherforcast", attachment.getName());
            assertEquals("temperatures and more", attachment.getDescription());
            assertEquals("web page", attachment.getType());
            assertEquals(taskId, attachment.getTaskId());
            assertNull(attachment.getProcessInstanceId());
            assertEquals("http://weather.com", attachment.getUrl());
            assertNull(taskService.getAttachmentContent(attachment.getId()));

            // Finally, clean up
            taskService.deleteTask(taskId);

            assertEquals(0, taskService.getTaskComments(taskId).size());
            
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);
            assertEquals(1, historyService.createHistoricTaskInstanceQuery().taskId(taskId).list().size());

            taskService.deleteTask(taskId, true);
        }
    }

    public void testSaveTaskAttachment() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
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
            assertEquals("UpdatedName", attachment.getName());

            // Finally, clean up
            taskService.deleteTask(taskId);

            assertEquals(0, taskService.getTaskComments(taskId).size());
            
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);
            
            assertEquals(1, historyService.createHistoricTaskInstanceQuery().taskId(taskId).list().size());

            taskService.deleteTask(taskId, true);
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testTaskAttachmentWithProcessInstanceId() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

            String processInstanceId = processInstance.getId();
            taskService.createAttachment("web page", null, processInstanceId, "weatherforcast", "temperatures and more", "http://weather.com");
            Attachment attachment = taskService.getProcessInstanceAttachments(processInstanceId).get(0);
            assertEquals("weatherforcast", attachment.getName());
            assertEquals("temperatures and more", attachment.getDescription());
            assertEquals("web page", attachment.getType());
            assertEquals(processInstanceId, attachment.getProcessInstanceId());
            assertNull(attachment.getTaskId());
            assertEquals("http://weather.com", attachment.getUrl());
            assertNull(taskService.getAttachmentContent(attachment.getId()));

            // Finally, clean up
            taskService.deleteAttachment(attachment.getId());

            // TODO: Bad API design. Need to fix attachment/comment properly
            ((TaskServiceImpl) taskService).deleteComments(null, processInstanceId);
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testMultipleProcessesStarted() {

        // Start a few process instances
        for (int i = 0; i < 20; i++) {
            processEngine.getRuntimeService().startProcessInstanceByKey("oneTaskProcess");
        }

        // See if there are tasks for kermit
        List<org.flowable.task.api.Task> tasks = processEngine.getTaskService().createTaskQuery().list();
        assertEquals(20, tasks.size());
    }

    public void testTaskDelegation() {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setOwner("johndoe");
        taskService.saveTask(task);
        taskService.delegateTask(task.getId(), "joesmoe");
        String taskId = task.getId();

        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertEquals("johndoe", task.getOwner());
        assertEquals("joesmoe", task.getAssignee());
        assertEquals(DelegationState.PENDING, task.getDelegationState());

        // try to complete (should fail)
        try {
            taskService.complete(task.getId());
            fail();
        } catch (FlowableException e) {
        }

        taskService.resolveTask(taskId);
        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertEquals("johndoe", task.getOwner());
        assertEquals("johndoe", task.getAssignee());
        assertEquals(DelegationState.RESOLVED, task.getDelegationState());

        task.setAssignee(null);
        task.setDelegationState(null);
        taskService.saveTask(task);
        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertEquals("johndoe", task.getOwner());
        assertNull(task.getAssignee());
        assertNull(task.getDelegationState());

        task.setAssignee("jackblack");
        task.setDelegationState(DelegationState.RESOLVED);
        taskService.saveTask(task);
        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertEquals("johndoe", task.getOwner());
        assertEquals("jackblack", task.getAssignee());
        assertEquals(DelegationState.RESOLVED, task.getDelegationState());

        // Finally, delete task
        taskService.deleteTask(taskId, true);
    }

    public void testTaskDelegationThroughServiceCall() {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setOwner("johndoe");
        taskService.saveTask(task);
        String taskId = task.getId();

        // Fetch the task again and update
        task = taskService.createTaskQuery().taskId(taskId).singleResult();

        taskService.delegateTask(task.getId(), "joesmoe");

        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertEquals("johndoe", task.getOwner());
        assertEquals("joesmoe", task.getAssignee());
        assertEquals(DelegationState.PENDING, task.getDelegationState());

        taskService.resolveTask(taskId);

        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertEquals("johndoe", task.getOwner());
        assertEquals("johndoe", task.getAssignee());
        assertEquals(DelegationState.RESOLVED, task.getDelegationState());

        // Finally, delete task
        taskService.deleteTask(taskId, true);
    }

    public void testTaskAssignee() {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setAssignee("johndoe");
        taskService.saveTask(task);

        // Fetch the task again and update
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertEquals("johndoe", task.getAssignee());

        task.setAssignee("joesmoe");
        taskService.saveTask(task);

        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertEquals("joesmoe", task.getAssignee());

        // Finally, delete task
        taskService.deleteTask(task.getId(), true);
    }

    public void testSaveTaskNullTask() {
        try {
            taskService.saveTask(null);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("task is null", ae.getMessage());
        }
    }

    public void testDeleteTaskNullTaskId() {
        try {
            taskService.deleteTask(null);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            // Expected exception
        }
    }

    public void testDeleteTaskUnexistingTaskId() {
        // Deleting unexisting task should be silently ignored
        taskService.deleteTask("unexistingtaskid");
    }

    public void testDeleteTasksNullTaskIds() {
        try {
            taskService.deleteTasks(null);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            // Expected exception
        }
    }

    public void testDeleteTasksTaskIdsUnexistingTaskId() {

        org.flowable.task.api.Task existingTask = taskService.newTask();
        taskService.saveTask(existingTask);

        // The unexisting taskId's should be silently ignored. Existing task
        // should
        // have been deleted.
        taskService.deleteTasks(Arrays.asList("unexistingtaskid1", existingTask.getId()), true);

        existingTask = taskService.createTaskQuery().taskId(existingTask.getId()).singleResult();
        assertNull(existingTask);
    }

    public void testDeleteTaskIdentityLink() {
        org.flowable.task.api.Task task = null;
        try {
            task = taskService.newTask();
            task.setName("test");
            taskService.saveTask(task);

            taskService.addCandidateGroup(task.getId(), "sales");
            taskService.addCandidateUser(task.getId(), "kermit");

            assertNotNull(taskService.createTaskQuery().taskCandidateGroup("sales").singleResult());
            assertNotNull(taskService.createTaskQuery().taskCandidateUser("kermit").singleResult());

            // Delete identity link for group
            taskService.deleteGroupIdentityLink(task.getId(), "sales", "candidate");

            // Link should be removed
            assertNull(taskService.createTaskQuery().taskCandidateGroup("sales").singleResult());

            // User link should remain unaffected
            assertNotNull(taskService.createTaskQuery().taskCandidateUser("kermit").singleResult());

        } finally {
            // Adhoc task not part of deployment, cleanup
            if (task != null && task.getId() != null) {
                taskService.deleteTask(task.getId());
                if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
                    historyService.deleteHistoricTaskInstance(task.getId());
                }
            }
        }
    }

    public void testClaimNullArguments() {
        try {
            taskService.claim(null, "userid");
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("taskId is null", ae.getMessage());
        }
    }

    public void testClaimUnexistingTaskId() {
        User user = identityService.newUser("user");
        identityService.saveUser(user);

        try {
            taskService.claim("unexistingtaskid", user.getId());
            fail("ActivitiException expected");
        } catch (FlowableObjectNotFoundException ae) {
            assertTextPresent("Cannot find task with id unexistingtaskid", ae.getMessage());
            assertEquals(org.flowable.task.api.Task.class, ae.getObjectClass());
        }

        identityService.deleteUser(user.getId());
    }

    public void testClaimAlreadyClaimedTaskByOtherUser() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);
        User user = identityService.newUser("user");
        identityService.saveUser(user);
        User secondUser = identityService.newUser("seconduser");
        identityService.saveUser(secondUser);

        // Claim task the first time
        taskService.claim(task.getId(), user.getId());

        try {
            taskService.claim(task.getId(), secondUser.getId());
            fail("ActivitiException expected");
        } catch (FlowableTaskAlreadyClaimedException ae) {
            assertTextPresent("Task '" + task.getId() + "' is already claimed by someone else.", ae.getMessage());
        }

        taskService.deleteTask(task.getId(), true);
        identityService.deleteUser(user.getId());
        identityService.deleteUser(secondUser.getId());
    }

    public void testClaimAlreadyClaimedTaskBySameUser() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);
        User user = identityService.newUser("user");
        identityService.saveUser(user);

        // Claim task the first time
        taskService.claim(task.getId(), user.getId());
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();

        // Claim the task again with the same user. No exception should be
        // thrown
        taskService.claim(task.getId(), user.getId());

        taskService.deleteTask(task.getId(), true);
        identityService.deleteUser(user.getId());
    }

    public void testUnClaimTask() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);
        User user = identityService.newUser("user");
        identityService.saveUser(user);

        // Claim task the first time
        taskService.claim(task.getId(), user.getId());
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertEquals(user.getId(), task.getAssignee());

        // Unclaim the task
        taskService.unclaim(task.getId());

        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertNull(task.getAssignee());

        taskService.deleteTask(task.getId(), true);
        identityService.deleteUser(user.getId());
    }

    public void testCompleteTaskNullTaskId() {
        try {
            taskService.complete(null);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("taskId is null", ae.getMessage());
        }
    }

    public void testCompleteTaskUnexistingTaskId() {
        try {
            taskService.complete("unexistingtask");
            fail("ActivitiException expected");
        } catch (FlowableObjectNotFoundException ae) {
            assertTextPresent("Cannot find task with id unexistingtask", ae.getMessage());
            assertEquals(org.flowable.task.api.Task.class, ae.getObjectClass());
        }
    }

    public void testCompleteTaskWithParametersNullTaskId() {
        try {
            taskService.complete(null);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("taskId is null", ae.getMessage());
        }
    }

    public void testCompleteTaskWithParametersUnexistingTaskId() {
        try {
            taskService.complete("unexistingtask");
            fail("ActivitiException expected");
        } catch (FlowableObjectNotFoundException ae) {
            assertTextPresent("Cannot find task with id unexistingtask", ae.getMessage());
            assertEquals(org.flowable.task.api.Task.class, ae.getObjectClass());
        }
    }

    public void testCompleteTaskWithParametersNullParameters() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);

        String taskId = task.getId();
        taskService.complete(taskId, null);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            historyService.deleteHistoricTaskInstance(taskId);
        }

        // Fetch the task again
        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertNull(task);
    }

    @SuppressWarnings("unchecked")
    public void testCompleteTaskWithParametersEmptyParameters() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);

        String taskId = task.getId();
        taskService.complete(taskId, Collections.EMPTY_MAP);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            historyService.deleteHistoricTaskInstance(taskId);
        }

        // Fetch the task again
        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertNull(task);
    }

    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testCompleteWithParametersTask() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");

        // Fetch first task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("First task", task.getName());

        // Complete first task
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("myParam", "myValue");
        taskService.complete(task.getId(), taskParams);

        // Fetch second task
        task = taskService.createTaskQuery().singleResult();
        assertEquals("Second task", task.getName());

        // Verify task parameters set on execution
        Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());
        assertEquals(1, variables.size());
        assertEquals("myValue", variables.get("myParam"));
    }

    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testCompleteWithParametersTask2() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");

        // Fetch first task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("First task", task.getName());

        // Complete first task
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("myParam", "myValue");
        taskService.complete(task.getId(), taskParams, false); // Only
                                                               // difference
                                                               // with previous
                                                               // test

        // Fetch second task
        task = taskService.createTaskQuery().singleResult();
        assertEquals("Second task", task.getName());

        // Verify task parameters set on execution
        Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());
        assertEquals(1, variables.size());
        assertEquals("myValue", variables.get("myParam"));
    }

    @Deployment(resources = {"org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml"})
    public void testCompleteWithExecutionBaseNewPropertyExpressionTask() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");

        // Fetch first task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("First task", task.getName());

        // Complete first task
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("${execution.myParam}", "myValue");
        try {
            taskService.complete(task.getId(), taskParams);
            fail();
        } catch (FlowableException e) {
            assertEquals("new properties are not resolved", e.getCause().getMessage(),
                    "Cannot write property: myParam");
        }
    }

    @Deployment(resources = {"org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml"})
    public void testCompleteWithExecutionIdParametersTask() {
        runtimeService.startProcessInstanceByKey("twoTasksProcess");

        // Fetch first task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("First task", task.getName());

        // Complete first task
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("${execution.id}", "myValue");
        try {
            taskService.complete(task.getId(), taskParams);
        } catch (PersistenceException e) {
            // expected exception.
        }
    }

    @Deployment(resources = {"org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml"})
    public void testCompleteWithExecutionNameParametersTask() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");

        // Fetch first task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("First task", task.getName());

        // Complete first task
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("${execution.name}", "myUpdatedName");
        taskService.complete(task.getId(), taskParams);

        // Verify task parameters set on execution
        Execution subExecution = runtimeService.createExecutionQuery().parentId(processInstance.getId()).singleResult();
        assertEquals("myUpdatedName", subExecution.getName());
    }

    @Deployment
    public void testCompleteWithTaskLocalParameters() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testTaskLocalVars");

        // Fetch first task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();

        // Complete first task
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("a", 1);
        taskParams.put("b", 1);
        taskService.complete(task.getId(), taskParams, true);

        // Verify vars are not stored process instance wide
        assertNull(runtimeService.getVariable(processInstance.getId(), "a"));
        assertNull(runtimeService.getVariable(processInstance.getId(), "b"));

        // verify script listener has done its job
        assertEquals(2, runtimeService.getVariable(processInstance.getId(), "sum"));

        // Fetch second task
        taskService.createTaskQuery().singleResult();

    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskWithFormKeyProcess.bpmn20.xml" })
    public void testCompleteTaskWithFormKey() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskWithFormProcess");

        // Fetch task
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("my task", task.getName());
        assertEquals("myFormKey", task.getFormKey());
        assertEquals("myAssignee", task.getAssignee());
        assertEquals("myOwner", task.getOwner());
        assertEquals("myCategory", task.getCategory());
        assertEquals(60, task.getPriority());
        assertNotNull(task.getDueDate());

        // Complete task
        taskService.complete(task.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult();
            assertEquals("my task", historicTask.getName());
            assertEquals("myFormKey", historicTask.getFormKey());
            assertEquals("myAssignee", historicTask.getAssignee());
            assertEquals("myOwner", historicTask.getOwner());
            assertEquals("myCategory", historicTask.getCategory());
            assertEquals(60, historicTask.getPriority());
            assertNotNull(historicTask.getDueDate());
        }
    }

    public void testSetAssignee() {
        User user = identityService.newUser("user");
        identityService.saveUser(user);

        org.flowable.task.api.Task task = taskService.newTask();
        assertNull(task.getAssignee());
        taskService.saveTask(task);

        // Set assignee
        taskService.setAssignee(task.getId(), user.getId());

        // Fetch task again
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertEquals(user.getId(), task.getAssignee());

        // Set assignee to null
        taskService.setAssignee(task.getId(), null);

        identityService.deleteUser(user.getId());
        taskService.deleteTask(task.getId(), true);
    }

    public void testSetAssigneeNullTaskId() {
        try {
            taskService.setAssignee(null, "userId");
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("taskId is null", ae.getMessage());
        }
    }

    public void testSetAssigneeUnexistingTask() {
        User user = identityService.newUser("user");
        identityService.saveUser(user);

        try {
            taskService.setAssignee("unexistingTaskId", user.getId());
            fail("ActivitiException expected");
        } catch (FlowableObjectNotFoundException ae) {
            assertTextPresent("Cannot find task with id unexistingTaskId", ae.getMessage());
            assertEquals(org.flowable.task.api.Task.class, ae.getObjectClass());
        }

        identityService.deleteUser(user.getId());
    }

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

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testCountingTaskForAddRemoveIdentityLink() {
        processEngineConfiguration.setEnableTaskRelationshipCounts(true);
        TaskServiceConfiguration taskServiceConfiguration = (TaskServiceConfiguration) processEngineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG);
        taskServiceConfiguration.setEnableTaskRelationshipCounts(true);

        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();
        assertEquals(0, ((CountingTaskEntity) currentTask).getIdentityLinkCount());

        taskService.addUserIdentityLink(currentTask.getId(), "user01", IdentityLinkType.PARTICIPANT);
        currentTask = taskService.createTaskQuery().singleResult();
        assertEquals(1, ((CountingTaskEntity) currentTask).getIdentityLinkCount());

        taskService.addUserIdentityLink(currentTask.getId(), "user02", IdentityLinkType.PARTICIPANT);
        currentTask = taskService.createTaskQuery().singleResult();
        assertEquals(2, ((CountingTaskEntity) currentTask).getIdentityLinkCount());

        taskService.deleteUserIdentityLink(currentTask.getId(), "user01", IdentityLinkType.PARTICIPANT);
        currentTask = taskService.createTaskQuery().singleResult();
        assertEquals(1, ((CountingTaskEntity) currentTask).getIdentityLinkCount());

        taskService.addGroupIdentityLink(currentTask.getId(), "group01", IdentityLinkType.PARTICIPANT);
        currentTask = taskService.createTaskQuery().singleResult();
        assertEquals(2, ((CountingTaskEntity) currentTask).getIdentityLinkCount());

        taskService.addGroupIdentityLink(currentTask.getId(), "group02", IdentityLinkType.PARTICIPANT);
        currentTask = taskService.createTaskQuery().singleResult();
        assertEquals(3, ((CountingTaskEntity) currentTask).getIdentityLinkCount());

        taskService.addGroupIdentityLink(currentTask.getId(), "group02", IdentityLinkType.PARTICIPANT);
        currentTask = taskService.createTaskQuery().singleResult();
        assertEquals(4, ((CountingTaskEntity) currentTask).getIdentityLinkCount());

        // start removing identity links
        taskService.deleteGroupIdentityLink(currentTask.getId(), "group02", IdentityLinkType.PARTICIPANT);
        currentTask = taskService.createTaskQuery().singleResult();
        // Two identityLinks with id "group02" are found. Both are deleted.
        assertEquals(2, ((CountingTaskEntity) currentTask).getIdentityLinkCount());

        // remove "user02" once
        taskService.deleteUserIdentityLink(currentTask.getId(), "user02", IdentityLinkType.PARTICIPANT);
        currentTask = taskService.createTaskQuery().singleResult();
        assertEquals(1, ((CountingTaskEntity) currentTask).getIdentityLinkCount());

        // remove "user02" twice
        taskService.deleteUserIdentityLink(currentTask.getId(), "user02", IdentityLinkType.PARTICIPANT);
        currentTask = taskService.createTaskQuery().singleResult();
        assertEquals(1, ((CountingTaskEntity) currentTask).getIdentityLinkCount());

        taskService.deleteGroupIdentityLink(currentTask.getId(), "group01", IdentityLinkType.PARTICIPANT);
        currentTask = taskService.createTaskQuery().singleResult();
        assertEquals(0, ((CountingTaskEntity) currentTask).getIdentityLinkCount());

        // make sure the "identityLinkCount" value does not go negative
        taskService.deleteGroupIdentityLink(currentTask.getId(), "group01", IdentityLinkType.PARTICIPANT);
        currentTask = taskService.createTaskQuery().singleResult();
        assertEquals(0, ((CountingTaskEntity) currentTask).getIdentityLinkCount());

        processEngineConfiguration.setEnableTaskRelationshipCounts(false);
        taskServiceConfiguration.setEnableTaskRelationshipCounts(false);
    }

    public void testAddCandidateUserNullTaskId() {
        try {
            taskService.addCandidateUser(null, "userId");
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("taskId is null", ae.getMessage());
        }
    }

    public void testAddCandidateUserNullUserId() {
        try {
            taskService.addCandidateUser("taskId", null);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("identityId is null", ae.getMessage());
        }
    }

    public void testAddCandidateUserUnexistingTask() {
        User user = identityService.newUser("user");
        identityService.saveUser(user);

        try {
            taskService.addCandidateUser("unexistingTaskId", user.getId());
            fail("ActivitiException expected");
        } catch (FlowableObjectNotFoundException ae) {
            assertTextPresent("Cannot find task with id unexistingTaskId", ae.getMessage());
            assertEquals(org.flowable.task.api.Task.class, ae.getObjectClass());
        }

        identityService.deleteUser(user.getId());
    }

    public void testAddCandidateGroupNullTaskId() {
        try {
            taskService.addCandidateGroup(null, "groupId");
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("taskId is null", ae.getMessage());
        }
    }

    public void testAddCandidateGroupNullGroupId() {
        try {
            taskService.addCandidateGroup("taskId", null);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("identityId is null", ae.getMessage());
        }
    }

    public void testAddCandidateGroupUnexistingTask() {
        Group group = identityService.newGroup("group");
        identityService.saveGroup(group);
        try {
            taskService.addCandidateGroup("unexistingTaskId", group.getId());
            fail("ActivitiException expected");
        } catch (FlowableObjectNotFoundException ae) {
            assertTextPresent("Cannot find task with id unexistingTaskId", ae.getMessage());
            assertEquals(org.flowable.task.api.Task.class, ae.getObjectClass());
        }
        identityService.deleteGroup(group.getId());
    }

    public void testAddGroupIdentityLinkNullTaskId() {
        try {
            taskService.addGroupIdentityLink(null, "groupId", IdentityLinkType.CANDIDATE);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("taskId is null", ae.getMessage());
        }
    }

    public void testAddGroupIdentityLinkNullUserId() {
        try {
            taskService.addGroupIdentityLink("taskId", null, IdentityLinkType.CANDIDATE);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("identityId is null", ae.getMessage());
        }
    }

    public void testAddGroupIdentityLinkUnexistingTask() {
        User user = identityService.newUser("user");
        identityService.saveUser(user);

        try {
            taskService.addGroupIdentityLink("unexistingTaskId", user.getId(), IdentityLinkType.CANDIDATE);
            fail("ActivitiException expected");
        } catch (FlowableObjectNotFoundException ae) {
            assertTextPresent("Cannot find task with id unexistingTaskId", ae.getMessage());
            assertEquals(org.flowable.task.api.Task.class, ae.getObjectClass());
        }

        identityService.deleteUser(user.getId());
    }

    public void testAddUserIdentityLinkNullTaskId() {
        try {
            taskService.addUserIdentityLink(null, "userId", IdentityLinkType.CANDIDATE);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("taskId is null", ae.getMessage());
        }
    }

    public void testAddUserIdentityLinkNullUserId() {
        try {
            taskService.addUserIdentityLink("taskId", null, IdentityLinkType.CANDIDATE);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("identityId is null", ae.getMessage());
        }
    }

    public void testAddUserIdentityLinkUnexistingTask() {
        User user = identityService.newUser("user");
        identityService.saveUser(user);

        try {
            taskService.addUserIdentityLink("unexistingTaskId", user.getId(), IdentityLinkType.CANDIDATE);
            fail("ActivitiException expected");
        } catch (FlowableObjectNotFoundException ae) {
            assertTextPresent("Cannot find task with id unexistingTaskId", ae.getMessage());
            assertEquals(org.flowable.task.api.Task.class, ae.getObjectClass());
        }

        identityService.deleteUser(user.getId());
    }

    public void testGetIdentityLinksWithCandidateUser() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);
        String taskId = task.getId();

        identityService.saveUser(identityService.newUser("kermit"));

        taskService.addCandidateUser(taskId, "kermit");
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        assertEquals(1, identityLinks.size());
        assertEquals("kermit", identityLinks.get(0).getUserId());
        assertNull(identityLinks.get(0).getGroupId());
        assertEquals(IdentityLinkType.CANDIDATE, identityLinks.get(0).getType());

        // cleanup
        taskService.deleteTask(taskId, true);
        identityService.deleteUser("kermit");
    }

    public void testGetIdentityLinksWithCandidateGroup() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);
        String taskId = task.getId();

        identityService.saveGroup(identityService.newGroup("muppets"));

        taskService.addCandidateGroup(taskId, "muppets");
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        assertEquals(1, identityLinks.size());
        assertEquals("muppets", identityLinks.get(0).getGroupId());
        assertNull(identityLinks.get(0).getUserId());
        assertEquals(IdentityLinkType.CANDIDATE, identityLinks.get(0).getType());

        // cleanup
        taskService.deleteTask(taskId, true);
        identityService.deleteGroup("muppets");
    }

    public void testGetIdentityLinksWithAssignee() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);
        String taskId = task.getId();

        identityService.saveUser(identityService.newUser("kermit"));

        taskService.claim(taskId, "kermit");
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        assertEquals(1, identityLinks.size());
        assertEquals("kermit", identityLinks.get(0).getUserId());
        assertNull(identityLinks.get(0).getGroupId());
        assertEquals(IdentityLinkType.ASSIGNEE, identityLinks.get(0).getType());

        // cleanup
        taskService.deleteTask(taskId, true);
        identityService.deleteUser("kermit");
    }

    public void testGetIdentityLinksWithNonExistingAssignee() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);
        String taskId = task.getId();

        taskService.claim(taskId, "nonExistingAssignee");
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        assertEquals(1, identityLinks.size());
        assertEquals("nonExistingAssignee", identityLinks.get(0).getUserId());
        assertNull(identityLinks.get(0).getGroupId());
        assertEquals(IdentityLinkType.ASSIGNEE, identityLinks.get(0).getType());

        // cleanup
        taskService.deleteTask(taskId, true);
    }

    public void testGetIdentityLinksWithOwner() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);
        String taskId = task.getId();

        identityService.saveUser(identityService.newUser("kermit"));
        identityService.saveUser(identityService.newUser("fozzie"));

        taskService.claim(taskId, "kermit");
        taskService.delegateTask(taskId, "fozzie");

        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        assertEquals(2, identityLinks.size());

        IdentityLink assignee = identityLinks.get(0);
        assertEquals("fozzie", assignee.getUserId());
        assertNull(assignee.getGroupId());
        assertEquals(IdentityLinkType.ASSIGNEE, assignee.getType());

        IdentityLink owner = identityLinks.get(1);
        assertEquals("kermit", owner.getUserId());
        assertNull(owner.getGroupId());
        assertEquals(IdentityLinkType.OWNER, owner.getType());

        // cleanup
        taskService.deleteTask(taskId, true);
        identityService.deleteUser("kermit");
        identityService.deleteUser("fozzie");
    }

    public void testGetIdentityLinksWithNonExistingOwner() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);
        String taskId = task.getId();

        taskService.claim(taskId, "nonExistingOwner");
        taskService.delegateTask(taskId, "nonExistingAssignee");
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        assertEquals(2, identityLinks.size());

        IdentityLink assignee = identityLinks.get(0);
        assertEquals("nonExistingAssignee", assignee.getUserId());
        assertNull(assignee.getGroupId());
        assertEquals(IdentityLinkType.ASSIGNEE, assignee.getType());

        IdentityLink owner = identityLinks.get(1);
        assertEquals("nonExistingOwner", owner.getUserId());
        assertNull(owner.getGroupId());
        assertEquals(IdentityLinkType.OWNER, owner.getType());

        // cleanup
        taskService.deleteTask(taskId, true);
    }

    public void testSetPriority() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);

        taskService.setPriority(task.getId(), 12345);

        // Fetch task again to check if the priority is set
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertEquals(12345, task.getPriority());

        taskService.deleteTask(task.getId(), true);
    }

    public void testSetPriorityUnexistingTaskId() {
        try {
            taskService.setPriority("unexistingtask", 12345);
            fail("ActivitiException expected");
        } catch (FlowableObjectNotFoundException ae) {
            assertTextPresent("Cannot find task with id unexistingtask", ae.getMessage());
            assertEquals(org.flowable.task.api.Task.class, ae.getObjectClass());
        }
    }

    public void testSetPriorityNullTaskId() {
        try {
            taskService.setPriority(null, 12345);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("taskId is null", ae.getMessage());
        }
    }

    public void testSetDueDate() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);

        // Set the due date to a non-null value
        Date now = new Date();
        taskService.setDueDate(task.getId(), now);

        // Fetch task to check if the due date was persisted
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertNotNull(task.getDueDate());

        // Set the due date to null
        taskService.setDueDate(task.getId(), null);

        // Re-fetch the task to make sure the due date was set to null
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
        assertNull(task.getDueDate());

        taskService.deleteTask(task.getId(), true);
    }

    public void testSetDueDateUnexistingTaskId() {
        try {
            taskService.setDueDate("unexistingtask", new Date());
            fail("ActivitiException expected");
        } catch (FlowableException ae) {
            assertTextPresent("Cannot find task with id unexistingtask", ae.getMessage());
        }
    }

    public void testSetDueDateNullTaskId() {
        try {
            taskService.setDueDate(null, new Date());
            fail("ActivitiException expected");
        } catch (FlowableException ae) {
            assertTextPresent("taskId is null", ae.getMessage());
        }
    }

    /**
     * @see <a href="https://activiti.atlassian.net/browse/ACT-1059">https://activiti.atlassian.net/browse/ACT-1059</a>
     */
    public void testSetDelegationState() {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setOwner("wuzh");
        taskService.saveTask(task);
        taskService.delegateTask(task.getId(), "other");
        String taskId = task.getId();

        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertEquals("wuzh", task.getOwner());
        assertEquals("other", task.getAssignee());
        assertEquals(DelegationState.PENDING, task.getDelegationState());

        task.setDelegationState(DelegationState.RESOLVED);
        taskService.saveTask(task);

        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertEquals("wuzh", task.getOwner());
        assertEquals("other", task.getAssignee());
        assertEquals(DelegationState.RESOLVED, task.getDelegationState());

        taskService.deleteTask(taskId, true);
    }

    private void checkHistoricVariableUpdateEntity(String variableName, String processInstanceId) {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            boolean deletedVariableUpdateFound = false;

            List<HistoricDetail> resultSet = historyService.createHistoricDetailQuery().processInstanceId(processInstanceId).list();
            for (HistoricDetail currentHistoricDetail : resultSet) {
                assertTrue(currentHistoricDetail instanceof HistoricDetailVariableInstanceUpdateEntity);
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

            assertTrue(deletedVariableUpdateFound);
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testRemoveVariable() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        taskService.setVariable(currentTask.getId(), "variable1", "value1");
        assertEquals("value1", taskService.getVariable(currentTask.getId(), "variable1"));
        assertNull(taskService.getVariableLocal(currentTask.getId(), "variable1"));

        taskService.removeVariable(currentTask.getId(), "variable1");

        assertNull(taskService.getVariable(currentTask.getId(), "variable1"));

        checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
    }

    public void testRemoveVariableNullTaskId() {
        try {
            taskService.removeVariable(null, "variable");
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("taskId is null", ae.getMessage());
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testRemoveVariables() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        Map<String, Object> varsToDelete = new HashMap<>();
        varsToDelete.put("variable1", "value1");
        varsToDelete.put("variable2", "value2");
        taskService.setVariables(currentTask.getId(), varsToDelete);
        taskService.setVariable(currentTask.getId(), "variable3", "value3");

        assertEquals("value1", taskService.getVariable(currentTask.getId(), "variable1"));
        assertEquals("value2", taskService.getVariable(currentTask.getId(), "variable2"));
        assertEquals("value3", taskService.getVariable(currentTask.getId(), "variable3"));
        assertNull(taskService.getVariableLocal(currentTask.getId(), "variable1"));
        assertNull(taskService.getVariableLocal(currentTask.getId(), "variable2"));
        assertNull(taskService.getVariableLocal(currentTask.getId(), "variable3"));

        taskService.removeVariables(currentTask.getId(), varsToDelete.keySet());

        assertNull(taskService.getVariable(currentTask.getId(), "variable1"));
        assertNull(taskService.getVariable(currentTask.getId(), "variable2"));
        assertEquals("value3", taskService.getVariable(currentTask.getId(), "variable3"));

        assertNull(taskService.getVariableLocal(currentTask.getId(), "variable1"));
        assertNull(taskService.getVariableLocal(currentTask.getId(), "variable2"));
        assertNull(taskService.getVariableLocal(currentTask.getId(), "variable3"));

        checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
        checkHistoricVariableUpdateEntity("variable2", processInstance.getId());
    }

    @SuppressWarnings("unchecked")
    public void testRemoveVariablesNullTaskId() {
        try {
            taskService.removeVariables(null, Collections.EMPTY_LIST);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("taskId is null", ae.getMessage());
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testRemoveVariableLocal() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        taskService.setVariableLocal(currentTask.getId(), "variable1", "value1");
        assertEquals("value1", taskService.getVariable(currentTask.getId(), "variable1"));
        assertEquals("value1", taskService.getVariableLocal(currentTask.getId(), "variable1"));

        taskService.removeVariableLocal(currentTask.getId(), "variable1");

        assertNull(taskService.getVariable(currentTask.getId(), "variable1"));
        assertNull(taskService.getVariableLocal(currentTask.getId(), "variable1"));

        checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testCountingTaskForAddRemoveVariable() {
        processEngineConfiguration.setEnableTaskRelationshipCounts(true);
        TaskServiceConfiguration taskServiceConfiguration = (TaskServiceConfiguration) processEngineConfiguration.getServiceConfigurations().get(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG);
        taskServiceConfiguration.setEnableTaskRelationshipCounts(true);
        
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        assertEquals(0, ((CountingTaskEntity) currentTask).getVariableCount());

        taskService.setVariableLocal(currentTask.getId(), "variable1", "value1");
        currentTask = taskService.createTaskQuery().singleResult();

        assertEquals(1, ((CountingTaskEntity) currentTask).getVariableCount());

        // process variables should have no effect
        taskService.setVariable(currentTask.getId(), "processVariable1", "procValue1");
        currentTask = taskService.createTaskQuery().singleResult();

        assertEquals(1, ((CountingTaskEntity) currentTask).getVariableCount());

        Map<String, Object> localVars = new HashMap<>();
        localVars.put("localVar1", "localValue1");
        localVars.put("localVar2", "localValue2");
        localVars.put("localVar3", "localValue3");

        taskService.setVariablesLocal(currentTask.getId(), localVars);
        currentTask = taskService.createTaskQuery().singleResult();

        assertEquals(4, ((CountingTaskEntity) currentTask).getVariableCount());

        taskService.removeVariablesLocal(currentTask.getId(), localVars.keySet());
        currentTask = taskService.createTaskQuery().singleResult();
        assertEquals(1, ((CountingTaskEntity) currentTask).getVariableCount());

        taskService.removeVariablesLocal(currentTask.getId(), localVars.keySet());
        currentTask = taskService.createTaskQuery().singleResult();
        assertEquals(1, ((CountingTaskEntity) currentTask).getVariableCount());

        taskService.removeVariableLocal(currentTask.getId(), "variable1");
        currentTask = taskService.createTaskQuery().singleResult();
        assertEquals(0, ((CountingTaskEntity) currentTask).getVariableCount());

        // make sure it does not get negative
        taskService.removeVariableLocal(currentTask.getId(), "variable1");
        currentTask = taskService.createTaskQuery().singleResult();
        assertEquals(0, ((CountingTaskEntity) currentTask).getVariableCount());

        processEngineConfiguration.setEnableTaskRelationshipCounts(false);
        taskServiceConfiguration.setEnableTaskRelationshipCounts(false);
    }

    public void testRemoveVariableLocalNullTaskId() {
        try {
            taskService.removeVariableLocal(null, "variable");
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("taskId is null", ae.getMessage());
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testRemoveVariablesLocal() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        Map<String, Object> varsToDelete = new HashMap<>();
        varsToDelete.put("variable1", "value1");
        varsToDelete.put("variable2", "value2");
        taskService.setVariablesLocal(currentTask.getId(), varsToDelete);
        taskService.setVariableLocal(currentTask.getId(), "variable3", "value3");

        assertEquals("value1", taskService.getVariable(currentTask.getId(), "variable1"));
        assertEquals("value2", taskService.getVariable(currentTask.getId(), "variable2"));
        assertEquals("value3", taskService.getVariable(currentTask.getId(), "variable3"));
        assertEquals("value1", taskService.getVariableLocal(currentTask.getId(), "variable1"));
        assertEquals("value2", taskService.getVariableLocal(currentTask.getId(), "variable2"));
        assertEquals("value3", taskService.getVariableLocal(currentTask.getId(), "variable3"));

        taskService.removeVariables(currentTask.getId(), varsToDelete.keySet());

        assertNull(taskService.getVariable(currentTask.getId(), "variable1"));
        assertNull(taskService.getVariable(currentTask.getId(), "variable2"));
        assertEquals("value3", taskService.getVariable(currentTask.getId(), "variable3"));

        assertNull(taskService.getVariableLocal(currentTask.getId(), "variable1"));
        assertNull(taskService.getVariableLocal(currentTask.getId(), "variable2"));
        assertEquals("value3", taskService.getVariableLocal(currentTask.getId(), "variable3"));

        checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
        checkHistoricVariableUpdateEntity("variable2", processInstance.getId());
    }

    @SuppressWarnings("unchecked")
    public void testRemoveVariablesLocalNullTaskId() {
        try {
            taskService.removeVariablesLocal(null, Collections.EMPTY_LIST);
            fail("ActivitiException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("taskId is null", ae.getMessage());
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableByHistoricActivityInstance() throws Exception {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            assertNotNull(processInstance);
            org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();

            taskService.setVariable(task.getId(), "variable1", "value1");
            Thread.sleep(50L); // to make sure the times for ordering below are different.
            taskService.setVariable(task.getId(), "variable1", "value2");
            
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);

            HistoricActivityInstance historicActivitiInstance = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .activityId("theTask").singleResult();
            assertNotNull(historicActivitiInstance);

            List<HistoricDetail> resultSet = historyService.createHistoricDetailQuery().variableUpdates()
                    .orderByTime()
                    .asc()
                    .list();

            assertEquals(2, resultSet.size());
            assertEquals("variable1", ((HistoricVariableUpdate) resultSet.get(0)).getVariableName());
            assertEquals("value1", ((HistoricVariableUpdate) resultSet.get(0)).getValue());
            assertEquals("variable1", ((HistoricVariableUpdate) resultSet.get(1)).getVariableName());
            assertEquals("value2", ((HistoricVariableUpdate) resultSet.get(1)).getValue());

            resultSet = historyService.createHistoricDetailQuery().variableUpdates()
                    .activityInstanceId(historicActivitiInstance.getId())
                    .orderByTime()
                    .asc()
                    .list();

            assertEquals(2, resultSet.size());
            assertEquals("variable1", ((HistoricVariableUpdate) resultSet.get(0)).getVariableName());
            assertEquals("value1", ((HistoricVariableUpdate) resultSet.get(0)).getValue());
            assertEquals("variable1", ((HistoricVariableUpdate) resultSet.get(1)).getVariableName());
            assertEquals("value2", ((HistoricVariableUpdate) resultSet.get(1)).getValue());
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testUserTaskOptimisticLocking() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task task1 = taskService.createTaskQuery().singleResult();
        org.flowable.task.api.Task task2 = taskService.createTaskQuery().singleResult();

        task1.setDescription("test description one");
        taskService.saveTask(task1);

        try {
            task2.setDescription("test description two");
            taskService.saveTask(task2);

            fail("Expecting exception");
        } catch (FlowableOptimisticLockingException e) {
            // Expected exception
        }
    }

    public void testDeleteTaskWithDeleteReason() {
        // ACT-900: deleteReason can be manually specified - can only be
        // validated when historyLevel > ACTIVITY
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            org.flowable.task.api.Task task = taskService.newTask();
            task.setName("test task");
            taskService.saveTask(task);

            assertNotNull(task.getId());

            taskService.deleteTask(task.getId(), "deleted for testing purposes");
            
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);

            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult();

            assertNotNull(historicTaskInstance);
            assertEquals("deleted for testing purposes", historicTaskInstance.getDeleteReason());

            // Delete historic task that is left behind, will not be cleaned up
            // because this is not part of a process
            taskService.deleteTask(task.getId(), true);

        }
    }

    public void testResolveTaskNullTaskId() {
        try {
            taskService.resolveTask(null);
            fail();
        } catch (FlowableException ae) {
            assertTextPresent("taskId is null", ae.getMessage());
        }
    }

    public void testResolveTaskUnexistingTaskId() {
        try {
            taskService.resolveTask("blergh");
            fail();
        } catch (FlowableException ae) {
            assertTextPresent("Cannot find task with id", ae.getMessage());
        }
    }

    public void testResolveTaskWithParametersNullParameters() {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setDelegationState(DelegationState.PENDING);
        taskService.saveTask(task);

        String taskId = task.getId();
        taskService.resolveTask(taskId, null);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            historyService.deleteHistoricTaskInstance(taskId);
        }

        // Fetch the task again
        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertEquals(DelegationState.RESOLVED, task.getDelegationState());

        taskService.deleteTask(taskId, true);
    }

    @SuppressWarnings("unchecked")
    public void testResolveTaskWithParametersEmptyParameters() {
        org.flowable.task.api.Task task = taskService.newTask();
        task.setDelegationState(DelegationState.PENDING);
        taskService.saveTask(task);

        String taskId = task.getId();
        taskService.resolveTask(taskId, Collections.EMPTY_MAP);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            historyService.deleteHistoricTaskInstance(taskId);
        }

        // Fetch the task again
        task = taskService.createTaskQuery().taskId(taskId).singleResult();
        assertEquals(DelegationState.RESOLVED, task.getDelegationState());

        taskService.deleteTask(taskId, true);
    }

    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testResolveWithParametersTask() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");

        // Fetch first task
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("First task", task.getName());

        taskService.delegateTask(task.getId(), "johndoe");

        // Resolve first task
        Map<String, Object> taskParams = new HashMap<>();
        taskParams.put("myParam", "myValue");
        taskService.resolveTask(task.getId(), taskParams);

        // Verify that task is resolved
        task = taskService.createTaskQuery().taskDelegationState(DelegationState.RESOLVED).singleResult();
        assertEquals("First task", task.getName());

        // Verify task parameters set on execution
        Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());
        assertEquals(1, variables.size());
        assertEquals("myValue", variables.get("myParam"));
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testDeleteTaskPartOfProcess() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertNotNull(task);

        try {
            taskService.deleteTask(task.getId());
        } catch (FlowableException ae) {
            assertEquals("The task cannot be deleted because is part of a running process", ae.getMessage());
        }

        try {
            taskService.deleteTask(task.getId(), true);
        } catch (FlowableException ae) {
            assertEquals("The task cannot be deleted because is part of a running process", ae.getMessage());
        }

        try {
            taskService.deleteTask(task.getId(), "test");
        } catch (FlowableException ae) {
            assertEquals("The task cannot be deleted because is part of a running process", ae.getMessage());
        }

        try {
            taskService.deleteTasks(Collections.singletonList(task.getId()));
        } catch (FlowableException ae) {
            assertEquals("The task cannot be deleted because is part of a running process", ae.getMessage());
        }

        try {
            taskService.deleteTasks(Collections.singletonList(task.getId()), true);
        } catch (FlowableException ae) {
            assertEquals("The task cannot be deleted because is part of a running process", ae.getMessage());
        }

        try {
            taskService.deleteTasks(Collections.singletonList(task.getId()), "test");
        } catch (FlowableException ae) {
            assertEquals("The task cannot be deleted because is part of a running process", ae.getMessage());
        }

    }

    @Deployment
    public void testFormKeyExpression() {
        runtimeService.startProcessInstanceByKey("testFormExpression", CollectionUtil.singletonMap("var", "abc"));

        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertEquals("first-form.json", task.getFormKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().singleResult();
        assertEquals("form-abc.json", task.getFormKey());

        task.setFormKey("form-changed.json");
        taskService.saveTask(task);
        task = taskService.createTaskQuery().singleResult();
        assertEquals("form-changed.json", task.getFormKey());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult();
            assertEquals("form-changed.json", historicTaskInstance.getFormKey());
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableLocalWithCast() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        taskService.setVariableLocal(currentTask.getId(), "variable1", "value1");

        String variable = taskService.getVariableLocal(currentTask.getId(), "variable1", String.class);

        assertEquals("value1", variable);
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableLocalNotExistingWithCast() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        String variable = taskService.getVariableLocal(currentTask.getId(), "variable1", String.class);

        assertNull(variable);
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableLocalWithInvalidCast() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        taskService.setVariableLocal(currentTask.getId(), "variable1", "value1");

        assertThatThrownBy(() -> taskService.getVariableLocal(currentTask.getId(), "variable1", Boolean.class))
            .isExactlyInstanceOf(ClassCastException.class);
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableWithCast() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        taskService.setVariable(currentTask.getId(), "variable1", "value1");

        String variable = taskService.getVariable(currentTask.getId(), "variable1", String.class);

        assertEquals("value1", variable);
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableNotExistingWithCast() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        String variable = taskService.getVariable(currentTask.getId(), "variable1", String.class);

        assertNull(variable);
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetVariableWithInvalidCast() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        org.flowable.task.api.Task currentTask = taskService.createTaskQuery().singleResult();

        taskService.setVariable(currentTask.getId(), "variable1", "value1");

        assertThatThrownBy(() -> taskService.getVariable(currentTask.getId(), "variable1", Boolean.class))
            .isExactlyInstanceOf(ClassCastException.class);
    }

    public void testClaimTime() {
        org.flowable.task.api.Task task = taskService.newTask();
        taskService.saveTask(task);
        User user = identityService.newUser("user");
        identityService.saveUser(user);

        assertNull(task.getClaimTime());

        // Claim task
        taskService.claim(task.getId(), user.getId());
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();

        assertNotNull(task.getClaimTime());

        // Unclaim task
        taskService.unclaim(task.getId());
        task = taskService.createTaskQuery().taskId(task.getId()).singleResult();

        assertNull(task.getClaimTime());

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
