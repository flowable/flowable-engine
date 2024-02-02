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
package org.flowable.test.ldap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.test.Deployment;
import org.flowable.idm.api.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration("classpath:flowable-context.xml")
public class LdapIntegrationTest extends LDAPTestCase {

    @Test
    public void testAuthenticationThroughLdap() {
        assertThat(identityService.checkPassword("kermit", "pass")).isTrue();
        assertThat(identityService.checkPassword("bunsen", "pass")).isTrue();
        assertThat(identityService.checkPassword("kermit", "blah")).isFalse();
    }

    @Test
    public void testAuthenticationThroughLdapEmptyPassword() {
        assertThatThrownBy(() -> identityService.checkPassword("kermit", null))
                .isInstanceOf(FlowableException.class);
        assertThatThrownBy(() -> identityService.checkPassword("kermit", ""))
                .isInstanceOf(FlowableException.class);
    }

    @Test
    @Deployment
    public void testCandidateGroupFetchedThroughLdap() {
        runtimeService.startProcessInstanceByKey("testCandidateGroup");
        assertThat(taskService.createTaskQuery().count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskCandidateGroup("sales").count()).isEqualTo(1);

        // Pepe is a member of the candidate group and should be able to find the task
        assertThat(taskService.createTaskQuery().taskCandidateUser("pepe").count()).isEqualTo(1);

        // Dr. Bunsen is also a member of the candidate group and should be able to find the task
        assertThat(taskService.createTaskQuery().taskCandidateUser("bunsen").count()).isEqualTo(1);

        // Kermit is a candidate user and should be able to find the task
        assertThat(taskService.createTaskQuery().taskCandidateUser("kermit").count()).isEqualTo(1);
    }

    @Test
    public void testUserQueryById() {
        List<User> users = identityService.createUserQuery().userId("kermit").list();
        assertThat(users)
                .extracting(User::getId, User::getFirstName, User::getLastName)
                .containsExactly(tuple("kermit", "Kermit", "The Frog"));

        User user = identityService.createUserQuery().userId("fozzie").singleResult();
        assertThat(user.getId()).isEqualTo("fozzie");
        assertThat(user.getFirstName()).isEqualTo("Fozzie");
        assertThat(user.getLastName()).isEqualTo("Bear");

        user = identityService.createUserQuery().userId("non-existing").singleResult();
        assertThat(user).isNull();
    }

    @Test
    public void testUserQueryByIdIgnoreCase() {
        List<User> users = identityService.createUserQuery().userIdIgnoreCase("KERMIT").list();
        assertThat(users)
                .extracting(User::getId, User::getFirstName, User::getLastName)
                .containsExactly(tuple("kermit", "Kermit", "The Frog"));

        User user = identityService.createUserQuery().userId("fozzie").singleResult();
        assertThat(user.getId()).isEqualTo("fozzie");
        assertThat(user.getFirstName()).isEqualTo("Fozzie");
        assertThat(user.getLastName()).isEqualTo("Bear");

        user = identityService.createUserQuery().userId("non-existing").singleResult();
        assertThat(user).isNull();
    }

    @Test
    public void testUserQueryByFullNameLike() {
        List<User> users = identityService.createUserQuery().userFullNameLike("ermi").list();
        assertThat(identityService.createUserQuery().userFullNameLike("ermi").count()).isEqualTo(1);

        assertThat(users)
                .extracting(User::getId, User::getFirstName, User::getLastName)
                .containsExactly(tuple("kermit", "Kermit", "The Frog"));

        users = identityService.createUserQuery().userFullNameLike("rog").list();
        assertThat(identityService.createUserQuery().userFullNameLike("rog").count()).isEqualTo(1);

        assertThat(users)
                .extracting(User::getId, User::getFirstName, User::getLastName)
                .containsExactly(tuple("kermit", "Kermit", "The Frog"));

        users = identityService.createUserQuery().userFullNameLike("e").list();
        assertThat(users).hasSize(5);
        assertThat(identityService.createUserQuery().userFullNameLike("e").count()).isEqualTo(5);

        users = identityService.createUserQuery().userFullNameLike("The").list();
        assertThat(users).hasSize(3);
        assertThat(identityService.createUserQuery().userFullNameLike("The").count()).isEqualTo(3);
    }

}
