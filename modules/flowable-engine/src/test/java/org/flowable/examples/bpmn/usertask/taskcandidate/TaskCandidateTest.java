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
package org.flowable.examples.bpmn.usertask.taskcandidate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez, Saeid Mirzaei
 */
public class TaskCandidateTest extends PluggableFlowableTestCase {

    private static final String KERMIT = "kermit";

    private static final String GONZO = "gonzo";

    @BeforeEach
    public void setUp() throws Exception {

        Group accountants = identityService.newGroup("accountancy");
        identityService.saveGroup(accountants);
        Group managers = identityService.newGroup("management");
        identityService.saveGroup(managers);
        Group sales = identityService.newGroup("sales");
        identityService.saveGroup(sales);

        User kermit = identityService.newUser(KERMIT);
        identityService.saveUser(kermit);
        identityService.createMembership(KERMIT, "accountancy");

        User gonzo = identityService.newUser(GONZO);
        identityService.saveUser(gonzo);
        identityService.createMembership(GONZO, "management");
        identityService.createMembership(GONZO, "accountancy");
        identityService.createMembership(GONZO, "sales");
    }

    @AfterEach
    public void tearDown() throws Exception {
        identityService.deleteUser(KERMIT);
        identityService.deleteUser(GONZO);
        identityService.deleteGroup("sales");
        identityService.deleteGroup("accountancy");
        identityService.deleteGroup("management");

    }

    @Test
    @Deployment
    public void testSingleCandidateGroup() {

        // Deploy and start process
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("singleCandidateGroup");

        // org.flowable.task.service.Task should not yet be assigned to kermit
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskAssignee(KERMIT).list();
        assertThat(tasks).isEmpty();

        // The task should be visible in the candidate task list
        tasks = taskService.createTaskQuery().taskCandidateUser(KERMIT).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Pay out expenses");
        String taskId = tasks.get(0).getId();

        // Claim the task
        taskService.claim(taskId, KERMIT);

        // The task must now be gone from the candidate task list
        tasks = taskService.createTaskQuery().taskCandidateUser(KERMIT).list();
        assertThat(tasks).isEmpty();

        // The task will be visible on the personal task list
        tasks = taskService.createTaskQuery().taskAssignee(KERMIT).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Pay out expenses");
        taskId = tasks.get(0).getId();

        // Completing the task ends the process
        taskService.complete(taskId);

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    public void testMultipleCandidateGroups() {

        // Deploy and start process
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("multipleCandidatesGroup");

        // org.flowable.task.service.Task should not yet be assigned to anyone
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskAssignee(KERMIT).list();

        assertThat(tasks).isEmpty();
        tasks = taskService.createTaskQuery().taskAssignee(GONZO).list();

        assertThat(tasks).isEmpty();

        // The task should be visible in the candidate task list of Gonzo and Kermit
        // and anyone in the management/accountancy group
        assertThat(taskService.createTaskQuery().taskCandidateUser(KERMIT).list()).hasSize(1);
        assertThat(taskService.createTaskQuery().taskCandidateUser(GONZO).list()).hasSize(1);
        assertThat(taskService.createTaskQuery().taskCandidateGroup("management").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskCandidateGroup("accountancy").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskCandidateGroup("sales").count()).isZero();

        // Gonzo claims the task
        tasks = taskService.createTaskQuery().taskCandidateUser(GONZO).list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("Approve expenses");
        taskService.claim(tasks.get(0).getId(), GONZO);

        // The task must now be gone from the candidate task lists
        assertThat(taskService.createTaskQuery().taskCandidateUser(KERMIT).list()).isEmpty();
        assertThat(taskService.createTaskQuery().taskCandidateUser(GONZO).list()).isEmpty();
        assertThat(taskService.createTaskQuery().taskCandidateGroup("management").count()).isZero();

        // The task will be visible on the personal task list of Gonzo
        assertThat(taskService.createTaskQuery().taskAssignee(GONZO).count()).isEqualTo(1);

        // But not on the personal task list of (for example) Kermit
        assertThat(taskService.createTaskQuery().taskAssignee(KERMIT).count()).isZero();

        // Completing the task ends the process
        taskService.complete(tasks.get(0).getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    public void testMultipleCandidateUsers() {
        runtimeService.startProcessInstanceByKey("multipleCandidateUsersExample", Collections.singletonMap("Variable", (Object) "var"));

        assertThat(taskService.createTaskQuery().taskCandidateUser(GONZO).list()).hasSize(1);
        assertThat(taskService.createTaskQuery().taskCandidateUser(KERMIT).list()).hasSize(1);

        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().taskInvolvedUser(KERMIT).list();
        assertThat(tasks).hasSize(1);

        org.flowable.task.api.Task task = tasks.get(0);
        taskService.setVariableLocal(task.getId(), "taskVar", 123);
        tasks = taskService.createTaskQuery().taskInvolvedUser(KERMIT).includeProcessVariables().includeTaskLocalVariables().list();
        task = tasks.get(0);

        assertThat(task.getProcessVariables()).hasSize(1);
        assertThat(task.getTaskLocalVariables()).hasSize(1);
        taskService.addUserIdentityLink(task.getId(), GONZO, "test");

        tasks = taskService.createTaskQuery().taskInvolvedUser(GONZO).includeProcessVariables().includeTaskLocalVariables().list();
        assertThat(tasks).hasSize(1);
        assertThat(task.getProcessVariables()).hasSize(1);
        assertThat(task.getTaskLocalVariables()).hasSize(1);
    }

    @Test
    @Deployment
    public void testMixedCandidateUserAndGroup() {
        runtimeService.startProcessInstanceByKey("mixedCandidateUserAndGroupExample");

        assertThat(taskService.createTaskQuery().taskCandidateUser(GONZO).list()).hasSize(1);
        assertThat(taskService.createTaskQuery().taskCandidateUser(KERMIT).list()).hasSize(1);
    }

    // test if candidate group works with expression, when there is a function
    // with one parameter
    @Test
    @Deployment
    public void testCandidateExpressionOneParam() {
        Map<String, Object> params = new HashMap<>();
        params.put("testBean", new TestBean());

        runtimeService.startProcessInstanceByKey("candidateWithExpression", params);
        assertThat(taskService.createTaskQuery().taskCandidateUser(KERMIT).list()).hasSize(1);

    }

    // test if candidate group works with expression, when there is a function
    // with two parameters
    @Test
    @Deployment
    public void testCandidateExpressionTwoParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("testBean", new TestBean());

        runtimeService.startProcessInstanceByKey("candidateWithExpression", params);
        assertThat(taskService.createTaskQuery().taskCandidateUser(KERMIT).count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskCandidateGroup("sales").count()).isEqualTo(1);
    }

}
