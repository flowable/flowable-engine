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
package org.flowable.examples.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.junit.jupiter.api.Test;

/**
 * @author Tom Baeyens
 */
public class IdentityTest extends PluggableFlowableTestCase {

    @Test
    public void testAuthentication() {
        User user = identityService.newUser("johndoe");
        user.setPassword("xxx");
        identityService.saveUser(user);

        assertThat(identityService.checkPassword("johndoe", "xxx")).isTrue();
        assertThat(identityService.checkPassword("johndoe", "invalid pwd")).isFalse();

        identityService.deleteUser("johndoe");
    }

    @Test
    public void testFindGroupsByUserAndType() {
        Group sales = identityService.newGroup("sales");
        sales.setType("hierarchy");
        identityService.saveGroup(sales);

        Group development = identityService.newGroup("development");
        development.setType("hierarchy");
        identityService.saveGroup(development);

        Group admin = identityService.newGroup("admin");
        admin.setType("security-role");
        identityService.saveGroup(admin);

        Group user = identityService.newGroup("user");
        user.setType("security-role");
        identityService.saveGroup(user);

        User johndoe = identityService.newUser("johndoe");
        identityService.saveUser(johndoe);

        User joesmoe = identityService.newUser("joesmoe");
        identityService.saveUser(joesmoe);

        User jackblack = identityService.newUser("jackblack");
        identityService.saveUser(jackblack);

        identityService.createMembership("johndoe", "sales");
        identityService.createMembership("johndoe", "user");
        identityService.createMembership("johndoe", "admin");

        identityService.createMembership("joesmoe", "user");

        List<Group> groups = identityService.createGroupQuery().groupMember("johndoe").groupType("security-role").list();
        assertThat(groups)
                .extracting(Group::getId)
                .containsExactlyInAnyOrder("user", "admin");

        groups = identityService.createGroupQuery().groupMember("joesmoe").groupType("security-role").list();
        assertThat(groups)
                .extracting(Group::getId)
                .containsExactly("user");

        groups = identityService.createGroupQuery().groupMember("jackblack").groupType("security-role").list();
        assertThat(groups).isEmpty();

        identityService.deleteGroup("sales");
        identityService.deleteGroup("development");
        identityService.deleteGroup("admin");
        identityService.deleteGroup("user");
        identityService.deleteUser("johndoe");
        identityService.deleteUser("joesmoe");
        identityService.deleteUser("jackblack");
    }

    @Test
    public void testUser() {
        User user = identityService.newUser("johndoe");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("johndoe@alfresco.com");
        identityService.saveUser(user);

        user = identityService.createUserQuery().userId("johndoe").singleResult();
        assertThat(user.getId()).isEqualTo("johndoe");
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.getEmail()).isEqualTo("johndoe@alfresco.com");

        identityService.deleteUser("johndoe");
    }

    @Test
    public void testGroup() {
        Group group = identityService.newGroup("sales");
        group.setName("Sales division");
        identityService.saveGroup(group);

        group = identityService.createGroupQuery().groupId("sales").singleResult();
        assertThat(group.getId()).isEqualTo("sales");
        assertThat(group.getName()).isEqualTo("Sales division");

        identityService.deleteGroup("sales");
    }

    @Test
    public void testMembership() {
        Group sales = identityService.newGroup("sales");
        identityService.saveGroup(sales);

        Group development = identityService.newGroup("development");
        identityService.saveGroup(development);

        User johndoe = identityService.newUser("johndoe");
        identityService.saveUser(johndoe);

        User joesmoe = identityService.newUser("joesmoe");
        identityService.saveUser(joesmoe);

        User jackblack = identityService.newUser("jackblack");
        identityService.saveUser(jackblack);

        identityService.createMembership("johndoe", "sales");
        identityService.createMembership("joesmoe", "sales");

        identityService.createMembership("joesmoe", "development");
        identityService.createMembership("jackblack", "development");

        List<Group> groups = identityService.createGroupQuery().groupMember("johndoe").list();
        assertThat(groups)
                .extracting(Group::getId)
                .containsExactly("sales");

        groups = identityService.createGroupQuery().groupMember("joesmoe").list();
        assertThat(groups)
                .extracting(Group::getId)
                .containsExactlyInAnyOrder("sales", "development");

        groups = identityService.createGroupQuery().groupMember("jackblack").list();
        assertThat(groups)
                .extracting(Group::getId)
                .containsExactly("development");

        List<User> users = identityService.createUserQuery().memberOfGroup("sales").list();
        assertThat(users)
                .extracting(User::getId)
                .containsExactlyInAnyOrder("johndoe", "joesmoe");

        users = identityService.createUserQuery().memberOfGroup("development").list();
        assertThat(users)
                .extracting(User::getId)
                .containsExactlyInAnyOrder("joesmoe", "jackblack");

        identityService.deleteGroup("sales");
        identityService.deleteGroup("development");

        identityService.deleteUser("jackblack");
        identityService.deleteUser("joesmoe");
        identityService.deleteUser("johndoe");
    }
}
