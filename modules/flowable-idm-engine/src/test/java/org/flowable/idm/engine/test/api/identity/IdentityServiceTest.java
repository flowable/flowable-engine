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

package org.flowable.idm.engine.test.api.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.Picture;
import org.flowable.idm.api.Token;
import org.flowable.idm.api.User;
import org.flowable.idm.engine.impl.authentication.ApacheDigester;
import org.flowable.idm.engine.test.PluggableFlowableIdmTestCase;
import org.junit.jupiter.api.Test;

/**
 * @author Frederik Heremans
 */
public class IdentityServiceTest extends PluggableFlowableIdmTestCase {

    @Test
    public void testUserInfo() {
        User user = idmIdentityService.newUser("testuser");
        idmIdentityService.saveUser(user);

        idmIdentityService.setUserInfo("testuser", "myinfo", "myvalue");
        assertThat(idmIdentityService.getUserInfo("testuser", "myinfo")).isEqualTo("myvalue");

        idmIdentityService.setUserInfo("testuser", "myinfo", "myvalue2");
        assertThat(idmIdentityService.getUserInfo("testuser", "myinfo")).isEqualTo("myvalue2");

        idmIdentityService.deleteUserInfo("testuser", "myinfo");
        assertThat(idmIdentityService.getUserInfo("testuser", "myinfo")).isNull();

        idmIdentityService.deleteUser(user.getId());
    }

    @Test
    public void testCreateExistingUser() {
        User user = idmIdentityService.newUser("testuser");
        idmIdentityService.saveUser(user);

        // Expected exception while saving new user with the same name as an existing one.
        assertThatThrownBy(() -> {
            User secondUser = idmIdentityService.newUser("testuser");
            idmIdentityService.saveUser(secondUser);
        })
                .isInstanceOf(RuntimeException.class);

        idmIdentityService.deleteUser(user.getId());
    }

    @Test
    public void testUpdateUser() {
        // First, create a new user
        User user = idmIdentityService.newUser("johndoe");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("johndoe@alfresco.com");
        idmIdentityService.saveUser(user);

        // Fetch and update the user
        user = idmIdentityService.createUserQuery().userId("johndoe").singleResult();
        user.setEmail("updated@alfresco.com");
        user.setFirstName("Jane");
        user.setLastName("Donnel");
        idmIdentityService.saveUser(user);

        user = idmIdentityService.createUserQuery().userId("johndoe").singleResult();
        assertThat(user.getFirstName()).isEqualTo("Jane");
        assertThat(user.getLastName()).isEqualTo("Donnel");
        assertThat(user.getEmail()).isEqualTo("updated@alfresco.com");

        idmIdentityService.deleteUser(user.getId());
    }

    @Test
    public void testUpdateUserDeltaOnly() {
        // First, create a new user
        User user = idmIdentityService.newUser("testuser");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setDisplayName("John Doe");
        user.setEmail("testuser@flowable.com");
        user.setPassword("test");
        idmIdentityService.saveUser(user);
        String initialPassword = user.getPassword();

        // Fetch and update the user
        user = idmIdentityService.createUserQuery().userId("testuser").singleResult();
        assertThat(user)
                .returns("John", User::getFirstName)
                .returns("Doe", User::getLastName)
                .returns("John Doe", User::getDisplayName)
                .returns("testuser@flowable.com", User::getEmail)
                .returns(initialPassword, User::getPassword);

        user.setFirstName("Jane");
        idmIdentityService.saveUser(user);

        user = idmIdentityService.createUserQuery().userId("testuser").singleResult();
        assertThat(user)
                .returns("Jane", User::getFirstName)
                .returns("Doe", User::getLastName)
                .returns("John Doe", User::getDisplayName)
                .returns("testuser@flowable.com", User::getEmail)
                .returns(initialPassword, User::getPassword);

        user.setLastName("Doelle");
        idmIdentityService.saveUser(user);

        user = idmIdentityService.createUserQuery().userId("testuser").singleResult();
        assertThat(user)
                .returns("Jane", User::getFirstName)
                .returns("Doelle", User::getLastName)
                .returns("John Doe", User::getDisplayName)
                .returns("testuser@flowable.com", User::getEmail)
                .returns(initialPassword, User::getPassword);

        user.setDisplayName("Jane Doelle");
        idmIdentityService.saveUser(user);

        user = idmIdentityService.createUserQuery().userId("testuser").singleResult();
        assertThat(user)
                .returns("Jane", User::getFirstName)
                .returns("Doelle", User::getLastName)
                .returns("Jane Doelle", User::getDisplayName)
                .returns("testuser@flowable.com", User::getEmail)
                .returns(initialPassword, User::getPassword);

        user.setEmail("janedoelle@flowable.com");
        idmIdentityService.saveUser(user);

        user = idmIdentityService.createUserQuery().userId("testuser").singleResult();
        assertThat(user)
                .returns("Jane", User::getFirstName)
                .returns("Doelle", User::getLastName)
                .returns("Jane Doelle", User::getDisplayName)
                .returns("janedoelle@flowable.com", User::getEmail)
                .returns(initialPassword, User::getPassword);

        user.setPassword("test-pass");
        idmIdentityService.saveUser(user);

        user = idmIdentityService.createUserQuery().userId("testuser").singleResult();
        assertThat(user)
                .returns("Jane", User::getFirstName)
                .returns("Doelle", User::getLastName)
                .returns("Jane Doelle", User::getDisplayName)
                .returns("janedoelle@flowable.com", User::getEmail)
                .returns(initialPassword, User::getPassword);

        idmIdentityService.deleteUser(user.getId());
    }

    @Test
    public void testUserPicture() {
        // First, create a new user
        User user = idmIdentityService.newUser("johndoe");
        idmIdentityService.saveUser(user);
        String userId = user.getId();

        Picture picture = new Picture("niceface".getBytes(), "image/string");
        idmIdentityService.setUserPicture(userId, picture);

        picture = idmIdentityService.getUserPicture(userId);

        // Fetch and update the user
        user = idmIdentityService.createUserQuery().userId("johndoe").singleResult();
        assertThat(picture.getBytes()).as("byte arrays differ").isEqualTo("niceface".getBytes());
        assertThat(picture.getMimeType()).isEqualTo("image/string");

        // interface definition states that setting picture to null should delete it
        idmIdentityService.setUserPicture(userId, null);
        assertThat(idmIdentityService.getUserPicture(userId)).as("it should be possible to nullify user picture").isNull();
        user = idmIdentityService.createUserQuery().userId("johndoe").singleResult();
        assertThat(idmIdentityService.getUserPicture(userId)).as("it should be possible to delete user picture").isNull();

        idmIdentityService.deleteUser(user.getId());
    }

    @Test
    public void testUpdateGroup() {
        Group group = idmIdentityService.newGroup("sales");
        group.setName("Sales");
        idmIdentityService.saveGroup(group);

        group = idmIdentityService.createGroupQuery().groupId("sales").singleResult();
        group.setName("Updated");
        idmIdentityService.saveGroup(group);

        group = idmIdentityService.createGroupQuery().groupId("sales").singleResult();
        assertThat(group.getName()).isEqualTo("Updated");

        idmIdentityService.deleteGroup(group.getId());
    }

    @Test
    public void findUserByUnexistingId() {
        User user = idmIdentityService.createUserQuery().userId("unexistinguser").singleResult();
        assertThat(user).isNull();
    }

    @Test
    public void findGroupByUnexistingId() {
        Group group = idmIdentityService.createGroupQuery().groupId("unexistinggroup").singleResult();
        assertThat(group).isNull();
    }

    @Test
    public void testCreateMembershipUnexistingGroup() {
        User johndoe = idmIdentityService.newUser("johndoe");
        idmIdentityService.saveUser(johndoe);

        assertThatThrownBy(() -> idmIdentityService.createMembership(johndoe.getId(), "unexistinggroup"))
                .isInstanceOf(RuntimeException.class);

        idmIdentityService.deleteUser(johndoe.getId());
    }

    @Test
    public void testCreateMembershipUnexistingUser() {
        Group sales = idmIdentityService.newGroup("sales");
        idmIdentityService.saveGroup(sales);

        assertThatThrownBy(() -> idmIdentityService.createMembership("unexistinguser", sales.getId()))
                .isInstanceOf(RuntimeException.class);

        idmIdentityService.deleteGroup(sales.getId());
    }

    @Test
    public void testCreateMembershipAlreadyExisting() {
        Group sales = idmIdentityService.newGroup("sales");
        idmIdentityService.saveGroup(sales);
        User johndoe = idmIdentityService.newUser("johndoe");
        idmIdentityService.saveUser(johndoe);

        // Create the membership
        idmIdentityService.createMembership(johndoe.getId(), sales.getId());

        assertThatThrownBy(() -> idmIdentityService.createMembership(johndoe.getId(), sales.getId()))
                .isInstanceOf(RuntimeException.class);

        idmIdentityService.deleteGroup(sales.getId());
        idmIdentityService.deleteUser(johndoe.getId());
    }

    @Test
    public void testSaveGroupNullArgument() {
        assertThatThrownBy(() -> idmIdentityService.saveGroup(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("group is null");
    }

    @Test
    public void testSaveUserNullArgument() {
        assertThatThrownBy(() -> idmIdentityService.saveUser(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("user is null");
    }

    @Test
    public void testFindGroupByIdNullArgument() {
        assertThatThrownBy(() -> idmIdentityService.createGroupQuery().groupId(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("id is null");
    }

    @Test
    public void testCreateMembershipNullArguments() {
        assertThatThrownBy(() -> idmIdentityService.createMembership(null, "group"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("userId is null");

        assertThatThrownBy(() -> idmIdentityService.createMembership("userId", null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("groupId is null");
    }

    @Test
    public void testFindGroupsByUserIdNullArguments() {
        assertThatThrownBy(() -> idmIdentityService.createGroupQuery().groupMember(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("userId is null");
    }

    @Test
    public void testFindUsersByGroupUnexistingGroup() {
        List<User> users = idmIdentityService.createUserQuery().memberOfGroup("unexistinggroup").list();
        assertThat(users).isEmpty();
    }

    @Test
    public void testDeleteGroupNullArguments() {
        assertThatThrownBy(() -> idmIdentityService.deleteGroup(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("groupId is null");
    }

    @Test
    public void testDeleteMembership() {
        Group sales = idmIdentityService.newGroup("sales");
        idmIdentityService.saveGroup(sales);

        User johndoe = idmIdentityService.newUser("johndoe");
        idmIdentityService.saveUser(johndoe);
        // Add membership
        idmIdentityService.createMembership(johndoe.getId(), sales.getId());

        List<Group> groups = idmIdentityService.createGroupQuery().groupMember(johndoe.getId()).list();
        assertThat(groups)
                .extracting(Group::getId)
                .containsExactly("sales");

        // Delete the membership and check members of sales group
        idmIdentityService.deleteMembership(johndoe.getId(), sales.getId());
        groups = idmIdentityService.createGroupQuery().groupMember(johndoe.getId()).list();
        assertThat(groups).isEmpty();

        idmIdentityService.deleteGroup("sales");
        idmIdentityService.deleteUser("johndoe");
    }

    @Test
    public void testDeleteMembershipWhenUserIsNoMember() {
        Group sales = idmIdentityService.newGroup("sales");
        idmIdentityService.saveGroup(sales);

        User johndoe = idmIdentityService.newUser("johndoe");
        idmIdentityService.saveUser(johndoe);

        // Delete the membership when the user is no member
        idmIdentityService.deleteMembership(johndoe.getId(), sales.getId());

        idmIdentityService.deleteGroup("sales");
        idmIdentityService.deleteUser("johndoe");
    }

    @Test
    public void testDeleteMembershipUnexistingGroup() {
        User johndoe = idmIdentityService.newUser("johndoe");
        idmIdentityService.saveUser(johndoe);
        // No exception should be thrown when group doesn't exist
        idmIdentityService.deleteMembership(johndoe.getId(), "unexistinggroup");
        idmIdentityService.deleteUser(johndoe.getId());
    }

    @Test
    public void testDeleteMembershipUnexistingUser() {
        Group sales = idmIdentityService.newGroup("sales");
        idmIdentityService.saveGroup(sales);
        // No exception should be thrown when user doesn't exist
        idmIdentityService.deleteMembership("unexistinguser", sales.getId());
        idmIdentityService.deleteGroup(sales.getId());
    }

    @Test
    public void testDeleteMemberschipNullArguments() {
        assertThatThrownBy(() -> idmIdentityService.deleteMembership(null, "group"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("userId is null");

        assertThatThrownBy(() -> idmIdentityService.deleteMembership("user", null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("groupId is null");
    }

    @Test
    public void testDeleteUserNullArguments() {
        assertThatThrownBy(() -> idmIdentityService.deleteUser(null))
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("userId is null");
    }

    @Test
    public void testDeleteUserUnexistingUserId() {
        // No exception should be thrown. Deleting an unexisting user should
        // be ignored silently
        idmIdentityService.deleteUser("unexistinguser");
    }

    @Test
    public void testCheckPasswordNullSafe() {
        assertThat(idmIdentityService.checkPassword("userId", null)).isFalse();
        assertThat(idmIdentityService.checkPassword(null, "passwd")).isFalse();
        assertThat(idmIdentityService.checkPassword(null, null)).isFalse();
    }

    @Test
    public void testChangePassword() {

        idmEngineConfiguration.setPasswordEncoder(new ApacheDigester(ApacheDigester.Digester.MD5));

        User user = idmIdentityService.newUser("johndoe");
        user.setPassword("xxx");
        idmIdentityService.saveUser(user);

        user = idmIdentityService.createUserQuery().userId("johndoe").list().get(0);
        user.setFirstName("John Doe");
        idmIdentityService.saveUser(user);
        User johndoe = idmIdentityService.createUserQuery().userId("johndoe").list().get(0);
        assertThat(johndoe.getPassword()).isNotEqualTo("xxx");
        assertThat(johndoe.getFirstName()).isEqualTo("John Doe");
        assertThat(idmIdentityService.checkPassword("johndoe", "xxx")).isTrue();

        user = idmIdentityService.createUserQuery().userId("johndoe").list().get(0);
        user.setPassword("yyy");
        idmIdentityService.saveUser(user);
        assertThat(idmIdentityService.checkPassword("johndoe", "xxx")).isTrue();

        user = idmIdentityService.createUserQuery().userId("johndoe").list().get(0);
        user.setPassword("yyy");
        idmIdentityService.updateUserPassword(user);
        assertThat(idmIdentityService.checkPassword("johndoe", "yyy")).isTrue();

        idmIdentityService.deleteUser("johndoe");
    }

    @Test
    public void testUserOptimisticLockingException() {
        User user = idmIdentityService.newUser("kermit");
        idmIdentityService.saveUser(user);

        User user1 = idmIdentityService.createUserQuery().singleResult();
        User user2 = idmIdentityService.createUserQuery().singleResult();

        user1.setFirstName("name one");
        idmIdentityService.saveUser(user1);

        assertThatThrownBy(() -> {
            user2.setFirstName("name two");
            idmIdentityService.saveUser(user2);
        })
                .isExactlyInstanceOf(FlowableOptimisticLockingException.class);

        idmIdentityService.deleteUser(user.getId());
    }

    @Test
    public void testGroupOptimisticLockingException() {
        Group group = idmIdentityService.newGroup("group");
        idmIdentityService.saveGroup(group);

        Group group1 = idmIdentityService.createGroupQuery().singleResult();
        Group group2 = idmIdentityService.createGroupQuery().singleResult();

        group1.setName("name one");
        idmIdentityService.saveGroup(group1);

        assertThatThrownBy(() -> {
            group2.setName("name two");
            idmIdentityService.saveGroup(group2);
        })
                .isExactlyInstanceOf(FlowableOptimisticLockingException.class);

        idmIdentityService.deleteGroup(group.getId());
    }

    @Test
    public void testNewToken() {
        Token token = idmIdentityService.newToken("myToken");
        token.setIpAddress("127.0.0.1");
        token.setTokenValue("myValue");
        token.setTokenDate(new Date());

        idmIdentityService.saveToken(token);

        Token token1 = idmIdentityService.createTokenQuery().singleResult();
        assertThat(token1.getId()).isEqualTo("myToken");
        assertThat(token1.getTokenValue()).isEqualTo("myValue");
        assertThat(token1.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(token1.getUserAgent()).isNull();

        token1.setUserAgent("myAgent");
        idmIdentityService.saveToken(token1);

        token1 = idmIdentityService.createTokenQuery().singleResult();
        assertThat(token1.getUserAgent()).isEqualTo("myAgent");

        idmIdentityService.deleteToken(token1.getId());
    }

    @Test
    public void testTokenOptimisticLockingException() {
        Token token = idmIdentityService.newToken("myToken");
        idmIdentityService.saveToken(token);

        Token token1 = idmIdentityService.createTokenQuery().singleResult();
        Token token2 = idmIdentityService.createTokenQuery().singleResult();

        token1.setUserAgent("name one");
        idmIdentityService.saveToken(token1);

        assertThatThrownBy(() -> {
            token2.setUserAgent("name two");
            idmIdentityService.saveToken(token2);
        })
                .isExactlyInstanceOf(FlowableOptimisticLockingException.class);

        idmIdentityService.deleteToken(token.getId());
    }

}
