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
package org.flowable.engine.test.api.identity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.flowable.engine.IdentityService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.task.service.delegate.DelegateTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class IdmTransactionsTest extends PluggableFlowableTestCase {

    @AfterEach
    protected void tearDown() throws Exception {

        List<User> allUsers = identityService.createUserQuery().list();
        for (User user : allUsers) {
            identityService.deleteUser(user.getId());
        }

        List<Group> allGroups = identityService.createGroupQuery().list();
        for (Group group : allGroups) {
            identityService.deleteGroup(group.getId());
        }
    }

    @Test
    @Deployment
    public void testCommitOnNoException() {

        // No users should exist prior to this test
        assertEquals(0, identityService.createUserQuery().list().size());

        runtimeService.startProcessInstanceByKey("testProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();

        taskService.complete(task.getId());
        assertEquals(1, identityService.createUserQuery().list().size());

    }

    @Test
    @Deployment
    public void testTransactionRolledBackOnException() {

        // No users should exist prior to this test
        assertEquals(0, identityService.createUserQuery().list().size());

        runtimeService.startProcessInstanceByKey("testProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();

        // Completing the task throws an exception
        assertThatThrownBy(() -> taskService.complete(task.getId()));

        // Should have rolled back to task
        assertNotNull(taskService.createTaskQuery().singleResult());
        assertEquals(0L, historyService.createHistoricProcessInstanceQuery().finished().count());

        // The logic in the tasklistener (creating a new user) should rolled back too:
        // no new user should have been created
        assertEquals(0, identityService.createUserQuery().list().size());

    }

    @Test
    @Deployment
    public void testMultipleIdmCallsInDelegate() {
        runtimeService.startProcessInstanceByKey("multipleServiceInvocations");

        // The service task should have created a user which is part of the admin group
        User user = identityService.createUserQuery().singleResult();
        assertEquals("Kermit", user.getId());
        Group group = identityService.createGroupQuery().groupMember(user.getId()).singleResult();
        assertNotNull(group);
        assertEquals("admin", group.getId());

        identityService.deleteMembership("Kermit", "admin");
    }

    public static class NoopDelegate implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
        }

    }

    public static class TestExceptionThrowingDelegate implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            throw new RuntimeException("Fail!");
        }

    }

    public static class TestCreateUserTaskListener implements TaskListener {

        @Override
        public void notify(DelegateTask delegateTask) {
            IdentityService identityService = Context.getProcessEngineConfiguration().getIdentityService();
            User user = identityService.newUser("Kermit");
            user.setFirstName("Mr");
            user.setLastName("Kermit");
            identityService.saveUser(user);
        }

    }

}
