/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.flowable.engine.test.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CallActivity;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.interceptor.IdentityLinkInterceptor;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Roman Saratz
 */
public class IdentityLinkInterceptorTest extends PluggableFlowableTestCase {

    private TestIdentityLinkInterceptor testInterceptor;
    private IdentityLinkInterceptor originalInterceptor;

    @BeforeEach
    void setUp() {
        testInterceptor = new TestIdentityLinkInterceptor();
        originalInterceptor = processEngineConfiguration.getIdentityLinkInterceptor();
        processEngineConfiguration.setIdentityLinkInterceptor(testInterceptor);
    }

    @AfterEach
    void tearDown() {
        processEngineConfiguration.setIdentityLinkInterceptor(originalInterceptor);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    void testCompleteTask() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        assertThat(testInterceptor.completedTasks).isEmpty();

        taskService.complete(task.getId());

        assertThat(testInterceptor.completedTasks).hasSize(1);
        TaskEntity completedTask = testInterceptor.completedTasks.get(0);
        assertThat(completedTask.getId()).isEqualTo(task.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    void testAddCandidateUser() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        assertThat(testInterceptor.addedIdentityLinks).isEmpty();

        taskService.addCandidateUser(task.getId(), "testUser");

        assertThat(testInterceptor.addedIdentityLinks).hasSize(1)
                .extracting(data -> data.taskId, data -> data.identityLink.getType(), data -> data.identityLink.getUserId())
                .containsExactly(tuple(task.getId(), IdentityLinkType.CANDIDATE, "testUser"));
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    void testAddCandidateGroup() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        assertThat(testInterceptor.addedIdentityLinks).isEmpty();

        taskService.addCandidateGroup(task.getId(), "testGroup");

        assertThat(testInterceptor.addedIdentityLinks).hasSize(1)
                .extracting(data -> data.taskId, data -> data.identityLink.getType(), data -> data.identityLink.getGroupId())
                .containsExactly(tuple(task.getId(), IdentityLinkType.CANDIDATE, "testGroup"));
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    void testSetAssignee() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        assertThat(testInterceptor.assigneeIdentityLinks).hasSize(1);

        taskService.setAssignee(task.getId(), "testAssignee");

        assertThat(testInterceptor.assigneeIdentityLinks).hasSize(2)
                .extracting(data -> data.taskId, data -> data.assignee)
                .containsExactly(
                        tuple(task.getId(), "testUser"),
                        tuple(task.getId(), "testAssignee")
                );
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    void testChangeAssigneeViaSaveTask() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        // Clear initial assignee data
        testInterceptor.assigneeIdentityLinks.clear();

        // Retrieve task, change assignee and save
        task.setAssignee("newAssignee");
        taskService.saveTask(task);

        assertThat(testInterceptor.assigneeIdentityLinks).hasSize(1)
                .extracting(data -> data.taskId, data -> data.assignee)
                .containsExactly(tuple(task.getId(), "newAssignee"));
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    void testRemoveAssigneeViaSaveTask() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        testInterceptor.assigneeIdentityLinks.clear();

        task.setAssignee(null);
        taskService.saveTask(task);

        assertThat(testInterceptor.assigneeIdentityLinks).hasSize(0);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    void testClaimTask() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        // remove initial assignee
        taskService.unclaim(task.getId());
        testInterceptor.assigneeIdentityLinks.clear();

        taskService.claim(task.getId(), "claimUser");

        assertThat(testInterceptor.assigneeIdentityLinks).hasSize(1)
                .extracting(data -> data.taskId, data -> data.assignee)
                .containsExactly(tuple(task.getId(), "claimUser"));
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    void testUnclaimTask() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        testInterceptor.assigneeIdentityLinks.clear();

        taskService.unclaim(task.getId());

        // unclaiming the task should not trigger the interceptor
        assertThat(testInterceptor.assigneeIdentityLinks).hasSize(0);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    void testSetOwner() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        assertThat(testInterceptor.ownerIdentityLinks).isEmpty();

        taskService.setOwner(task.getId(), "testOwner");

        assertThat(testInterceptor.ownerIdentityLinks).hasSize(1)
                .extracting(data -> data.taskId, data -> data.owner)
                .containsExactly(tuple(task.getId(), "testOwner"));
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    void testChangeOwnerViaSaveTask() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        assertThat(testInterceptor.ownerIdentityLinks).isEmpty();

        task.setOwner("newOwner");
        taskService.saveTask(task);

        assertThat(testInterceptor.ownerIdentityLinks).hasSize(1)
                .extracting(data -> data.taskId, data -> data.owner)
                .containsExactly(tuple(task.getId(), "newOwner"));
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    void testRemoveOwnerViaSaveTask() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        assertThat(testInterceptor.ownerIdentityLinks).isEmpty();

        task.setOwner(null);
        taskService.saveTask(task);

        assertThat(testInterceptor.ownerIdentityLinks).hasSize(0);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    void testCreateProcessInstance() {
        assertThat(testInterceptor.createdProcessInstances).isEmpty();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");

        assertThat(testInterceptor.createdProcessInstances).hasSize(1)
                .extracting(DelegateExecution::getId, DelegateExecution::isProcessInstanceType)
                .containsExactly(tuple(processInstance.getId(), true));
    }

    @Test
    void testCreateSubProcessInstance() {
        // Deploy main process with call activity
        BpmnModel mainModel = createMainProcessWithCallActivity();
        org.flowable.engine.repository.Deployment mainDeployment = repositoryService.createDeployment()
                .addBpmnModel("mainProcess.bpmn20.xml", mainModel).deploy();
        deploymentIdsForAutoCleanup.add(mainDeployment.getId());

        // Deploy subprocess
        String subprocessDefinitionId = deployOneTaskTestProcess();

        assertThat(testInterceptor.createdSubProcessInstances).isEmpty();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("mainProcess");

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().processDefinitionId(subprocessDefinitionId).singleResult();

        assertThat(testInterceptor.createdSubProcessInstances).hasSize(1)
                .extracting(
                        data -> data.subProcessExecution.getId(),
                        data -> data.superExecution.getProcessInstanceId()
                )
                .containsExactly(tuple(subProcessInstance.getId(), processInstance.getId()));
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    void testMultipleIdentityLinkOperations() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        // Add multiple candidates
        taskService.addCandidateUser(task.getId(), "user1");
        taskService.addCandidateUser(task.getId(), "user2");
        taskService.addCandidateGroup(task.getId(), "group1");

        // Set assignee and owner
        taskService.setAssignee(task.getId(), "assignee1");
        taskService.setOwner(task.getId(), "owner1");

        // Complete task
        taskService.complete(task.getId());

        // Verify identity link additions
        assertThat(testInterceptor.addedIdentityLinks).hasSize(3)
                .extracting(data -> data.identityLink.getType(), data -> data.identityLink.getUserId(), data -> data.identityLink.getGroupId())
                .containsExactly(
                        tuple(IdentityLinkType.CANDIDATE, "user1", null),
                        tuple(IdentityLinkType.CANDIDATE, "user2", null),
                        tuple(IdentityLinkType.CANDIDATE, null, "group1")
                );

        // Verify assignee operations
        assertThat(testInterceptor.assigneeIdentityLinks).hasSize(2)
                .extracting(data -> data.assignee)
                .containsExactly("testUser", "assignee1");

        // Verify other operations
        assertThat(testInterceptor.ownerIdentityLinks).hasSize(1);
        assertThat(testInterceptor.completedTasks).hasSize(1);
        assertThat(testInterceptor.createdProcessInstances).hasSize(1);
    }

    private BpmnModel createMainProcessWithCallActivity() {
        BpmnModel model = new BpmnModel();
        org.flowable.bpmn.model.Process process = new org.flowable.bpmn.model.Process();
        model.addProcess(process);
        process.setId("mainProcess");
        process.setName("Main Process");

        StartEvent startEvent = new StartEvent();
        startEvent.setId("start");
        process.addFlowElement(startEvent);

        CallActivity callActivity = new CallActivity();
        callActivity.setId("callActivity");
        callActivity.setCalledElement("oneTaskProcess");
        process.addFlowElement(callActivity);

        EndEvent endEvent = new EndEvent();
        endEvent.setId("end");
        process.addFlowElement(endEvent);

        process.addFlowElement(new SequenceFlow("start", "callActivity"));
        process.addFlowElement(new SequenceFlow("callActivity", "end"));

        return model;
    }

    static class TestIdentityLinkInterceptor implements IdentityLinkInterceptor {

        List<TaskEntity> completedTasks = new ArrayList<>();
        List<IdentityLinkData> addedIdentityLinks = new ArrayList<>();
        List<AssigneeData> assigneeIdentityLinks = new ArrayList<>();
        List<OwnerData> ownerIdentityLinks = new ArrayList<>();
        List<ExecutionEntity> createdProcessInstances = new ArrayList<>();
        List<SubProcessData> createdSubProcessInstances = new ArrayList<>();

        @Override
        public void handleCompleteTask(TaskEntity task) {
            completedTasks.add(task);
        }

        @Override
        public void handleAddIdentityLinkToTask(TaskEntity taskEntity, IdentityLinkEntity identityLinkEntity) {
            addedIdentityLinks.add(new IdentityLinkData(taskEntity.getId(), identityLinkEntity));
        }

        @Override
        public void handleAddAssigneeIdentityLinkToTask(TaskEntity taskEntity, String assignee) {
            assigneeIdentityLinks.add(new AssigneeData(taskEntity.getId(), assignee));
        }

        @Override
        public void handleAddOwnerIdentityLinkToTask(TaskEntity taskEntity, String owner) {
            ownerIdentityLinks.add(new OwnerData(taskEntity.getId(), owner));
        }

        @Override
        public void handleCreateProcessInstance(ExecutionEntity processInstanceExecution) {
            createdProcessInstances.add(processInstanceExecution);
        }

        @Override
        public void handleCreateSubProcessInstance(ExecutionEntity subProcessInstanceExecution, ExecutionEntity superExecution) {
            createdSubProcessInstances.add(new SubProcessData(subProcessInstanceExecution, superExecution));
        }
    }

    static class IdentityLinkData {

        String taskId;
        IdentityLinkEntity identityLink;

        IdentityLinkData(String taskId, IdentityLinkEntity identityLink) {
            this.taskId = taskId;
            this.identityLink = identityLink;
        }
    }

    static class AssigneeData {

        String taskId;
        String assignee;

        AssigneeData(String taskId, String assignee) {
            this.taskId = taskId;
            this.assignee = assignee;
        }
    }

    static class OwnerData {

        String taskId;
        String owner;

        OwnerData(String taskId, String owner) {
            this.taskId = taskId;
            this.owner = owner;
        }
    }

    static class SubProcessData {

        ExecutionEntity subProcessExecution;
        ExecutionEntity superExecution;

        SubProcessData(ExecutionEntity subProcessExecution, ExecutionEntity superExecution) {
            this.subProcessExecution = subProcessExecution;
            this.superExecution = superExecution;
        }
    }
}
