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

package org.flowable.cmmn.test.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.interceptor.CmmnIdentityLinkInterceptor;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
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
public class CmmnIdentityLinkInterceptorTest extends FlowableCmmnTestCase {

    private TestCmmnIdentityLinkInterceptor testInterceptor;
    private CmmnIdentityLinkInterceptor originalInterceptor;

    @BeforeEach
    void setUp() {
        testInterceptor = new TestCmmnIdentityLinkInterceptor();
        originalInterceptor = cmmnEngineConfiguration.getIdentityLinkInterceptor();
        cmmnEngineConfiguration.setIdentityLinkInterceptor(testInterceptor);
    }

    @AfterEach
    void tearDown() {
        cmmnEngineConfiguration.setIdentityLinkInterceptor(originalInterceptor);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    void testCompleteTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        assertThat(testInterceptor.completedTasks).isEmpty();

        cmmnTaskService.complete(task.getId());

        assertThat(testInterceptor.completedTasks).hasSize(1);
        TaskEntity completedTask = testInterceptor.completedTasks.get(0);
        assertThat(completedTask.getId()).isEqualTo(task.getId());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    void testAddCandidateUser() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        assertThat(testInterceptor.addedIdentityLinks).isEmpty();

        cmmnTaskService.addUserIdentityLink(task.getId(), "testUser", IdentityLinkType.CANDIDATE);

        assertThat(testInterceptor.addedIdentityLinks).hasSize(1)
                .extracting(data -> data.taskId, data -> data.identityLink.getType(), data -> data.identityLink.getUserId())
                .containsExactly(tuple(task.getId(), IdentityLinkType.CANDIDATE, "testUser"));
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    void testAddCandidateGroup() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        assertThat(testInterceptor.addedIdentityLinks).isEmpty();

        cmmnTaskService.addGroupIdentityLink(task.getId(), "testGroup", IdentityLinkType.CANDIDATE);

        assertThat(testInterceptor.addedIdentityLinks).hasSize(1)
                .extracting(data -> data.taskId, data -> data.identityLink.getType(), data -> data.identityLink.getGroupId())
                .containsExactly(tuple(task.getId(), IdentityLinkType.CANDIDATE, "testGroup"));
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    void testSetAssignee() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        assertThat(testInterceptor.assigneeIdentityLinks).hasSize(1);

        cmmnTaskService.setAssignee(task.getId(), "testAssignee");

        assertThat(testInterceptor.assigneeIdentityLinks).hasSize(2).extracting(data -> data.taskId, data -> data.assignee)
                .containsExactly(tuple(task.getId(), "johnDoe"), tuple(task.getId(), "testAssignee"));
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    void testChangeAssigneeViaSaveTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        testInterceptor.assigneeIdentityLinks.clear();

        task.setAssignee("newAssignee");
        cmmnTaskService.saveTask(task);

        assertThat(testInterceptor.assigneeIdentityLinks).hasSize(1).extracting(data -> data.taskId, data -> data.assignee)
                .containsExactly(tuple(task.getId(), "newAssignee"));
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    void testRemoveAssigneeViaSaveTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        testInterceptor.assigneeIdentityLinks.clear();

        task.setAssignee(null);
        cmmnTaskService.saveTask(task);

        assertThat(testInterceptor.assigneeIdentityLinks).hasSize(0);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    void testClaimTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        // remove initial assignee
        cmmnTaskService.unclaim(task.getId());
        testInterceptor.assigneeIdentityLinks.clear();

        cmmnTaskService.claim(task.getId(), "claimUser");

        assertThat(testInterceptor.assigneeIdentityLinks).hasSize(1).extracting(data -> data.taskId, data -> data.assignee)
                .containsExactly(tuple(task.getId(), "claimUser"));
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    void testUnclaimTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        testInterceptor.assigneeIdentityLinks.clear();

        cmmnTaskService.unclaim(task.getId());

        assertThat(testInterceptor.assigneeIdentityLinks).hasSize(0);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    void testSetOwner() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        assertThat(testInterceptor.ownerIdentityLinks).isEmpty();

        cmmnTaskService.setOwner(task.getId(), "testOwner");

        assertThat(testInterceptor.ownerIdentityLinks).hasSize(1).extracting(data -> data.taskId, data -> data.owner)
                .containsExactly(tuple(task.getId(), "testOwner"));
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    void testChangeOwnerViaSaveTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        assertThat(testInterceptor.ownerIdentityLinks).isEmpty();

        task.setOwner("newOwner");
        cmmnTaskService.saveTask(task);

        assertThat(testInterceptor.ownerIdentityLinks).hasSize(1).extracting(data -> data.taskId, data -> data.owner)
                .containsExactly(tuple(task.getId(), "newOwner"));
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    void testRemoveOwnerViaSaveTask() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        assertThat(testInterceptor.ownerIdentityLinks).isEmpty();

        task.setOwner(null);
        cmmnTaskService.saveTask(task);

        assertThat(testInterceptor.ownerIdentityLinks).hasSize(0);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    void testCreateCaseInstance() {
        assertThat(testInterceptor.createdCaseInstances).isEmpty();

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();

        assertThat(testInterceptor.createdCaseInstances).hasSize(1).extracting(CaseInstance::getId).containsExactly(caseInstance.getId());
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    void testMultipleIdentityLinkOperations() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();

        cmmnTaskService.addUserIdentityLink(task.getId(), "user1", IdentityLinkType.CANDIDATE);
        cmmnTaskService.addUserIdentityLink(task.getId(), "user2", IdentityLinkType.CANDIDATE);
        cmmnTaskService.addGroupIdentityLink(task.getId(), "group1", IdentityLinkType.CANDIDATE);

        cmmnTaskService.setAssignee(task.getId(), "assignee1");
        cmmnTaskService.setOwner(task.getId(), "owner1");

        cmmnTaskService.complete(task.getId());

        assertThat(testInterceptor.addedIdentityLinks).hasSize(3)
                .extracting(data -> data.identityLink.getType(), data -> data.identityLink.getUserId(), data -> data.identityLink.getGroupId())
                .containsExactly(tuple(IdentityLinkType.CANDIDATE, "user1", null), tuple(IdentityLinkType.CANDIDATE, "user2", null),
                        tuple(IdentityLinkType.CANDIDATE, null, "group1"));

        assertThat(testInterceptor.assigneeIdentityLinks).hasSize(2).extracting(data -> data.assignee).containsExactly("johnDoe", "assignee1");

        assertThat(testInterceptor.ownerIdentityLinks).hasSize(1);
        assertThat(testInterceptor.completedTasks).hasSize(1);
        assertThat(testInterceptor.createdCaseInstances).hasSize(1);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/reactivation/Reactivation_Test_Case.cmmn.xml")
    void testReactivation() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("reactivationTestCase").start();

        Task taskA = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(taskA.getId());

        Task taskB = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(taskB.getId());

        assertThat(cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult()).isNull();

        assertThat(testInterceptor.reactivatedCaseInstances).hasSize(0);

        cmmnHistoryService.createCaseReactivationBuilder(caseInstance.getId()).transientVariable("tempVar", "tempVarValue").transientVariable("tempIntVar", 100)
                .reactivate();

        assertThat(testInterceptor.reactivatedCaseInstances).hasSize(1).extracting(CaseInstance::getId).containsExactly(caseInstance.getId());
    }

    static class TestCmmnIdentityLinkInterceptor implements CmmnIdentityLinkInterceptor {

        List<TaskEntity> completedTasks = new ArrayList<>();
        List<IdentityLinkData> addedIdentityLinks = new ArrayList<>();
        List<AssigneeData> assigneeIdentityLinks = new ArrayList<>();
        List<OwnerData> ownerIdentityLinks = new ArrayList<>();
        List<CaseInstance> createdCaseInstances = new ArrayList<>();
        List<CaseInstance> reactivatedCaseInstances = new ArrayList<>();

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
        public void handleCreateCaseInstance(CaseInstanceEntity caseInstance) {
            createdCaseInstances.add(caseInstance);
        }

        @Override
        public void handleReactivateCaseInstance(CaseInstanceEntity caseInstance) {
            reactivatedCaseInstances.add(caseInstance);
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

}
