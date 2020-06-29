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
package org.flowable.engine.test.bpmn.usertask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.flowable.bpmn.exceptions.XMLException;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.test.TestHelper;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Testcase for the non-spec extensions to the task candidate use case.
 * 
 * @author Joram Barrez
 */
public class TaskAssignmentExtensionsTest extends PluggableFlowableTestCase {

    @BeforeEach
    public void setUp() throws Exception {
        identityService.saveUser(identityService.newUser("kermit"));
        identityService.saveUser(identityService.newUser("gonzo"));
        identityService.saveUser(identityService.newUser("fozzie"));

        identityService.saveGroup(identityService.newGroup("management"));
        identityService.saveGroup(identityService.newGroup("accountancy"));

        identityService.createMembership("kermit", "management");
        identityService.createMembership("kermit", "accountancy");
        identityService.createMembership("fozzie", "management");
    }

    @AfterEach
    public void tearDown() throws Exception {
        identityService.deleteGroup("accountancy");
        identityService.deleteGroup("management");
        identityService.deleteUser("fozzie");
        identityService.deleteUser("gonzo");
        identityService.deleteUser("kermit");
    }

    @Test
    @Deployment
    public void testAssigneeExtension() {
        runtimeService.startProcessInstanceByKey("assigneeExtension");
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskAssignee("kermit").list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("my task");
    }

    @Test
    public void testDuplicateAssigneeDeclaration() {
        assertThatThrownBy(() -> {
            String resource = TestHelper.getBpmnProcessDefinitionResource(getClass(), "testDuplicateAssigneeDeclaration");
            repositoryService.createDeployment().addClasspathResource(resource).deploy();
        })
                .as("Invalid BPMN 2.0 process should not parse, but it gets parsed successfully")
                .isInstanceOf(XMLException.class);
    }

    @Test
    @Deployment
    public void testOwnerExtension() {
        runtimeService.startProcessInstanceByKey("ownerExtension");
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskOwner("gonzo").list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("my task");
    }

    @Test
    @Deployment
    public void testCandidateUsersExtension() {
        runtimeService.startProcessInstanceByKey("candidateUsersExtension");
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskCandidateUser("kermit").list();
        assertThat(tasks).hasSize(1);
        tasks = taskService.createTaskQuery().taskCandidateUser("gonzo").list();
        assertThat(tasks).hasSize(1);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/usertask/TaskAssignmentExtensionsTest.testCandidateUsersExpressionExtension.bpmn20.xml")
    public void testCandidateUsersCollectionVariable() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("candidateUsersExtension")
                .transientVariable("candidateUsersVar", Arrays.asList("kermit", "gonzo"))
                .start();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskCandidateUser("kermit").list();
        assertThat(tasks).hasSize(1);
        tasks = taskService.createTaskQuery().taskCandidateUser("gonzo").list();
        assertThat(tasks).hasSize(1);
        tasks = taskService.createTaskQuery().taskCandidateUser("rizzo").list();
        assertThat(tasks).isEmpty();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/usertask/TaskAssignmentExtensionsTest.testCandidateUsersExpressionExtension.bpmn20.xml")
    public void testCandidateUsersArrayNodeVariable() {
        ArrayNode users = processEngineConfiguration.getObjectMapper().createArrayNode();
        users.add("kermit");
        users.add("gonzo");
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("candidateUsersExtension")
                .transientVariable("candidateUsersVar", users)
                .start();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskCandidateUser("kermit").list();
        assertThat(tasks).hasSize(1);
        tasks = taskService.createTaskQuery().taskCandidateUser("gonzo").list();
        assertThat(tasks).hasSize(1);
        tasks = taskService.createTaskQuery().taskCandidateUser("rizzo").list();
        assertThat(tasks).isEmpty();
    }

    @Test
    @Deployment
    public void testCandidateGroupsExtension() {
        runtimeService.startProcessInstanceByKey("candidateGroupsExtension");

        // Bugfix check: potentially the query could return 2 tasks since
        // kermit is a member of the two candidate groups
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskCandidateUser("kermit").list();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getName()).isEqualTo("make profit");
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("make profit");

        tasks = taskService.createTaskQuery().taskCandidateUser("fozzie").list();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getName()).isEqualTo("make profit");
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("make profit");

        // Test the task query find-by-candidate-group operation
        TaskQuery query = taskService.createTaskQuery();
        assertThat(query.taskCandidateGroup("management").count()).isEqualTo(1);
        assertThat(query.taskCandidateGroup("accountancy").count()).isEqualTo(1);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/usertask/TaskAssignmentExtensionsTest.testCandidateGroupsExpressionExtension.bpmn20.xml")
    public void testCandidateGroupsCollectionVariable() {
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("candidateGroupsExtension")
                .transientVariable("candidateGroupsVar", Arrays.asList("management", "accountancy"))
                .start();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskCandidateUser("kermit").list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("make profit");

        tasks = taskService.createTaskQuery().taskCandidateUser("fozzie").list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("make profit");

        // Test the task query find-by-candidate-group operation
        TaskQuery query = taskService.createTaskQuery();
        assertThat(query.taskCandidateGroup("management").count()).isEqualTo(1);
        assertThat(query.taskCandidateGroup("accountancy").count()).isEqualTo(1);
        assertThat(query.taskCandidateGroup("sales").count()).isZero();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/usertask/TaskAssignmentExtensionsTest.testCandidateGroupsExpressionExtension.bpmn20.xml")
    public void testCandidateGroupsArrayNodeVariable() {
        ArrayNode users = processEngineConfiguration.getObjectMapper().createArrayNode();
        users.add("management");
        users.add("accountancy");
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("candidateGroupsExtension")
                .transientVariable("candidateGroupsVar", users)
                .start();
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskCandidateUser("kermit").list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("make profit");

        tasks = taskService.createTaskQuery().taskCandidateUser("fozzie").list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("make profit");

        // Test the task query find-by-candidate-group operation
        TaskQuery query = taskService.createTaskQuery();
        assertThat(query.taskCandidateGroup("management").count()).isEqualTo(1);
        assertThat(query.taskCandidateGroup("accountancy").count()).isEqualTo(1);
        assertThat(query.taskCandidateGroup("sales").count()).isZero();
    }

    // Test where the candidate user extension is used together
    // with the spec way of defining candidate users
    @Test
    @Deployment
    public void testMixedCandidateUserDefinition() {
        runtimeService.startProcessInstanceByKey("mixedCandidateUser");

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskCandidateUser("kermit").list();
        assertThat(tasks).hasSize(1);

        tasks = taskService.createTaskQuery().taskCandidateUser("fozzie").list();
        assertThat(tasks).hasSize(1);

        tasks = taskService.createTaskQuery().taskCandidateUser("gonzo").list();
        assertThat(tasks).hasSize(1);

        tasks = taskService.createTaskQuery().taskCandidateUser("mispiggy").list();
        assertThat(tasks).isEmpty();
    }

}
