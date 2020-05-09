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
package org.flowable.spring.test.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.flowable.task.api.Task;
import org.flowable.task.service.delegate.DelegateTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Joram Barrez
 */
@ContextConfiguration("classpath:org/flowable/spring/test/transaction/SpringIdmTransactionsTest-context.xml")
public class SpringIdmTransactionsTest extends SpringFlowableTestCase {

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @AfterEach
    protected void tearDown() throws Exception {

        List<Group> allGroups = identityService.createGroupQuery().list();
        for (Group group : allGroups) {

            List<User> members = identityService.createUserQuery().memberOfGroup(group.getId()).list();
            for (User member : members) {
                identityService.deleteMembership(member.getId(), group.getId());
            }

            identityService.deleteGroup(group.getId());
        }

        List<User> allUsers = identityService.createUserQuery().list();
        for (User user : allUsers) {
            identityService.deleteUser(user.getId());
        }

    }

    @Test
    @Deployment
    public void testCommitOnNoException() {

        // No users should exist prior to this test
        assertThat(identityService.createUserQuery().list()).isEmpty();

        runtimeService.startProcessInstanceByKey("testProcess");
        Task task = taskService.createTaskQuery().singleResult();

        taskService.complete(task.getId());
        assertThat(identityService.createUserQuery().list()).hasSize(1);

    }

    @Test
    @Deployment
    public void testTransactionRolledBackOnException() {

        // No users should exist prior to this test
        assertThat(identityService.createUserQuery().list()).isEmpty();

        runtimeService.startProcessInstanceByKey("testProcess");
        Task task = taskService.createTaskQuery().singleResult();

        // Completing the task throws an exception
        assertThatThrownBy(() -> taskService.complete(task.getId()))
                .isInstanceOf(Exception.class);

        // Should have rolled back to task
        assertThat(taskService.createTaskQuery().singleResult()).isNotNull();
        assertThat(historyService.createHistoricProcessInstanceQuery().finished().count()).isZero();

        // The logic in the tasklistener (creating a new user) should rolled back too:
        // no new user should have been created
        assertThat(identityService.createUserQuery().list()).isEmpty();
    }

    @Test
    @Deployment
    public void testMultipleIdmCallsInDelegate() {
        runtimeService.startProcessInstanceByKey("multipleServiceInvocations");

        // The service task should have created a user which is part of the admin group
        User user = identityService.createUserQuery().singleResult();
        assertThat(user.getId()).isEqualTo("Kermit");
        Group group = identityService.createGroupQuery().groupMember(user.getId()).singleResult();
        assertThat(group).isNotNull();
        assertThat(group.getId()).isEqualTo("admin");

        identityService.deleteMembership("Kermit", "admin");
    }

    // From https://github.com/flowable/flowable-engine/issues/26
    @Test
    public void testCreateMemberships() {
        Group group = identityService.newGroup("group");
        User tom = identityService.newUser("tom");
        User mat = identityService.newUser("mat");

        // save group
        identityService.saveGroup(group);
        // save users
        identityService.saveUser(tom);
        identityService.saveUser(mat);
        // create memberships
        identityService.createMembership(tom.getId(), group.getId());
        identityService.createMembership(mat.getId(), group.getId());

        // verify that the group has been created
        assertThat(identityService.createGroupQuery().groupId(group.getId()).singleResult()).isNotNull();
        // verify that the users have been created
        assertThat(identityService.createUserQuery().userId(tom.getId()).singleResult()).isNotNull();
        assertThat(identityService.createUserQuery().userId(mat.getId()).singleResult()).isNotNull();
        // verify that the users are members of the group
        assertThat(identityService.createGroupQuery().groupMember(tom.getId()).singleResult()).isNotNull();
        assertThat(identityService.createGroupQuery().groupMember(mat.getId()).singleResult()).isNotNull();
    }

    @Test
    public void testCreateMembershipsWithinTransaction() {
        final Group group = identityService.newGroup("group");
        final User tom = identityService.newUser("tom");
        final User mat = identityService.newUser("mat");

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.execute(new TransactionCallbackWithoutResult() {

            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                // save group
                identityService.saveGroup(group);
                // save users
                identityService.saveUser(tom);
                identityService.saveUser(mat);
                // create memberships
                identityService.createMembership(tom.getId(), group.getId());
                identityService.createMembership(mat.getId(), group.getId());
            }
        });

        // verify that the group has been created
        assertThat(identityService.createGroupQuery().groupId(group.getId()).singleResult()).isNotNull();
        // verify that the users have been created
        assertThat(identityService.createUserQuery().userId(tom.getId()).singleResult()).isNotNull();
        assertThat(identityService.createUserQuery().userId(mat.getId()).singleResult()).isNotNull();
        // verify that the users are members of the group
        assertThat(identityService.createGroupQuery().groupMember(tom.getId()).singleResult()).isNotNull();
        assertThat(identityService.createGroupQuery().groupMember(mat.getId()).singleResult()).isNotNull();
    }

    @Test
    @Deployment
    public void testCreateMembershipsInTaskListener() {

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

        // start processing instance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());

        // verify that the process instance has been started
        assertThat(processInstance).isNotNull();
        // verify that the group has been created
        assertThat(identityService.createGroupQuery().groupId("group").singleResult()).isNotNull();
        // verify that the users have been created
        assertThat(identityService.createUserQuery().userId("tom").singleResult()).isNotNull();
        assertThat(identityService.createUserQuery().userId("mat").singleResult()).isNotNull();
        // verify that the users are members of the group
        assertThat(identityService.createGroupQuery().groupMember("tom").singleResult()).isNotNull();
        assertThat(identityService.createGroupQuery().groupMember("mat").singleResult()).isNotNull();
    }

    /*
     * DELEGATE CLASSES
     */

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

    public static class CreateUserAndMembershipTestDelegate implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {

            ManagementService managementService = Context.getProcessEngineConfiguration().getManagementService();
            managementService.executeCommand(new Command<Void>() {

                @Override
                public Void execute(CommandContext commandContext) {
                    return null;
                }
            });

            IdentityService identityService = Context.getProcessEngineConfiguration().getIdentityService();

            String username = "Kermit";
            User user = identityService.newUser(username);
            user.setPassword("123");
            user.setFirstName("Manually");
            user.setLastName("created");
            identityService.saveUser(user);

            // Add admin group
            Group group = identityService.newGroup("admin");
            identityService.saveGroup(group);

            identityService.createMembership(username, "admin");
        }

    }

    public static class TestCreateMembershipTaskListener implements TaskListener {

        @Autowired
        private IdentityService identityService;

        @Override
        public void notify(DelegateTask delegateTask) {

            // save group
            Group group = identityService.newGroup("group");
            identityService.saveGroup(group);
            // save users
            User tom = identityService.newUser("tom");
            identityService.saveUser(tom);

            User mat = identityService.newUser("mat");
            identityService.saveUser(mat);
            // create memberships
            identityService.createMembership(tom.getId(), group.getId());
            identityService.createMembership(mat.getId(), group.getId());
        }
    }

}
