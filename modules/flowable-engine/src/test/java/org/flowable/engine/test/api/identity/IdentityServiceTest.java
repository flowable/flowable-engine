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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.Picture;
import org.flowable.idm.api.User;
import org.junit.jupiter.api.Test;

/**
 * @author Frederik Heremans
 */
public class IdentityServiceTest extends PluggableFlowableTestCase {

    @Test
    public void testUserInfo() {
        User user = identityService.newUser("testuser");
        identityService.saveUser(user);

        identityService.setUserInfo("testuser", "myinfo", "myvalue");
        assertThat(identityService.getUserInfo("testuser", "myinfo")).isEqualTo("myvalue");

        identityService.setUserInfo("testuser", "myinfo", "myvalue2");
        assertThat(identityService.getUserInfo("testuser", "myinfo")).isEqualTo("myvalue2");

        identityService.deleteUserInfo("testuser", "myinfo");
        assertThat(identityService.getUserInfo("testuser", "myinfo")).isNull();

        identityService.deleteUser(user.getId());
    }

    @Test
    public void testCreateExistingUser() {
        User user = identityService.newUser("testuser");
        identityService.saveUser(user);

        assertThatThrownBy(() -> {
            User secondUser = identityService.newUser("testuser");
            identityService.saveUser(secondUser);
        })
                .isInstanceOf(RuntimeException.class);

        identityService.deleteUser(user.getId());
    }

    @Test
    public void testUpdateUser() {
        // First, create a new user
        User user = identityService.newUser("johndoe");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("johndoe@alfresco.com");
        user.setTenantId("originalTenantId");
        identityService.saveUser(user);

        // Fetch and update the user
        user = identityService.createUserQuery().userId("johndoe").singleResult();
        user.setEmail("updated@alfresco.com");
        user.setFirstName("Jane");
        user.setLastName("Donnel");
        user.setTenantId("flowable");
        identityService.saveUser(user);

        user = identityService.createUserQuery().userId("johndoe").singleResult();
        assertThat(user.getFirstName()).isEqualTo("Jane");
        assertThat(user.getLastName()).isEqualTo("Donnel");
        assertThat(user.getEmail()).isEqualTo("updated@alfresco.com");
        assertThat(user.getTenantId()).isEqualTo("flowable");

        identityService.deleteUser(user.getId());
    }

    @Test
    public void testCreateUserWithoutTenantId() {
        // First, create a new user
        User user = identityService.newUser("johndoe");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("johndoe@alfresco.com");
        identityService.saveUser(user);

        // Fetch and update the user
        user = identityService.createUserQuery().userId("johndoe").singleResult();
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.getEmail()).isEqualTo("johndoe@alfresco.com");
        assertThat(user.getTenantId()).isNull();

        identityService.deleteUser(user.getId());
    }

    @Test
    public void testUserPicture() {
        // First, create a new user
        User user = identityService.newUser("johndoe");
        identityService.saveUser(user);
        String userId = user.getId();

        Picture picture = new Picture("niceface".getBytes(), "image/string");
        identityService.setUserPicture(userId, picture);

        picture = identityService.getUserPicture(userId);

        // Fetch and update the user
        user = identityService.createUserQuery().userId("johndoe").singleResult();
        assertThat(Arrays.equals("niceface".getBytes(), picture.getBytes())).as("byte arrays differ").isTrue();
        assertThat(picture.getMimeType()).isEqualTo("image/string");

        // interface definition states that setting picture to null should delete it
        identityService.setUserPicture(userId, null);
        assertThat(identityService.getUserPicture(userId)).as("it should be possible to nullify user picture").isNull();
        user = identityService.createUserQuery().userId("johndoe").singleResult();
        assertThat(identityService.getUserPicture(userId)).as("it should be possible to delete user picture").isNull();

        identityService.deleteUser(user.getId());
    }

    @Test
    public void testUpdateGroup() {
        Group group = identityService.newGroup("sales");
        group.setName("Sales");
        identityService.saveGroup(group);

        group = identityService.createGroupQuery().groupId("sales").singleResult();
        group.setName("Updated");
        identityService.saveGroup(group);

        group = identityService.createGroupQuery().groupId("sales").singleResult();
        assertThat(group.getName()).isEqualTo("Updated");

        identityService.deleteGroup(group.getId());
    }

    @Test
    public void testFindUserByUnexistingId() {
        User user = identityService.createUserQuery().userId("unexistinguser").singleResult();
        assertThat(user).isNull();
    }

    @Test
    public void testFindGroupByUnexistingId() {
        Group group = identityService.createGroupQuery().groupId("unexistinggroup").singleResult();
        assertThat(group).isNull();
    }

    @Test
    public void testCreateMembershipUnexistingGroup() {
        User johndoe = identityService.newUser("johndoe");
        identityService.saveUser(johndoe);

        assertThatThrownBy(() -> identityService.createMembership(johndoe.getId(), "unexistinggroup"))
                .isInstanceOf(RuntimeException.class);

        identityService.deleteUser(johndoe.getId());
    }

    @Test
    public void testCreateMembershipUnexistingUser() {
        Group sales = identityService.newGroup("sales");
        identityService.saveGroup(sales);

        assertThatThrownBy(() -> identityService.createMembership("unexistinguser", sales.getId()))
                .isInstanceOf(RuntimeException.class);

        identityService.deleteGroup(sales.getId());
    }

    @Test
    public void testCreateMembershipAlreadyExisting() {
        Group sales = identityService.newGroup("sales");
        identityService.saveGroup(sales);
        User johndoe = identityService.newUser("johndoe");
        identityService.saveUser(johndoe);

        // Create the membership
        identityService.createMembership(johndoe.getId(), sales.getId());

        assertThatThrownBy(() -> identityService.createMembership(johndoe.getId(), sales.getId()))
                .isInstanceOf(RuntimeException.class);

        identityService.deleteGroup(sales.getId());
        identityService.deleteUser(johndoe.getId());
    }

    @Test
    public void testSaveGroupNullArgument() {
        assertThatThrownBy(() -> identityService.saveGroup(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("group is null");
    }

    @Test
    public void testSaveUserNullArgument() {
        assertThatThrownBy(() -> identityService.saveUser(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("user is null");
    }

    @Test
    public void testFindGroupByIdNullArgument() {
        assertThatThrownBy(() -> identityService.createGroupQuery().groupId(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("id is null");
    }

    @Test
    public void testCreateMembershipNullArguments() {
        assertThatThrownBy(() -> identityService.createMembership(null, "group"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("userId is null");

        assertThatThrownBy(() -> identityService.createMembership("userId", null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("groupId is null");
    }

    @Test
    public void testFindGroupsByUserIdNullArguments() {
        assertThatThrownBy(() -> identityService.createGroupQuery().groupMember(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("userId is null");
    }

    @Test
    public void testFindUsersByGroupUnexistingGroup() {
        List<User> users = identityService.createUserQuery().memberOfGroup("unexistinggroup").list();
        assertThat(users).isEmpty();
    }

    @Test
    public void testDeleteGroupNullArguments() {
        assertThatThrownBy(() -> identityService.deleteGroup(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("groupId is null");
    }

    @Test
    public void testDeleteMembership() {
        Group sales = identityService.newGroup("sales");
        identityService.saveGroup(sales);

        User johndoe = identityService.newUser("johndoe");
        identityService.saveUser(johndoe);
        // Add membership
        identityService.createMembership(johndoe.getId(), sales.getId());

        List<Group> groups = identityService.createGroupQuery().groupMember(johndoe.getId()).list();
        assertThat(groups)
                .extracting(Group::getId)
                .containsExactly("sales");

        // Delete the membership and check members of sales group
        identityService.deleteMembership(johndoe.getId(), sales.getId());
        groups = identityService.createGroupQuery().groupMember(johndoe.getId()).list();
        assertThat(groups).isEmpty();

        identityService.deleteGroup("sales");
        identityService.deleteUser("johndoe");
    }

    @Test
    public void testDeleteMembershipWhenUserIsNoMember() {
        Group sales = identityService.newGroup("sales");
        identityService.saveGroup(sales);

        User johndoe = identityService.newUser("johndoe");
        identityService.saveUser(johndoe);

        // Delete the membership when the user is no member
        identityService.deleteMembership(johndoe.getId(), sales.getId());

        identityService.deleteGroup("sales");
        identityService.deleteUser("johndoe");
    }

    @Test
    public void testDeleteMembershipUnexistingGroup() {
        User johndoe = identityService.newUser("johndoe");
        identityService.saveUser(johndoe);
        // No exception should be thrown when group doesn't exist
        identityService.deleteMembership(johndoe.getId(), "unexistinggroup");
        identityService.deleteUser(johndoe.getId());
    }

    @Test
    public void testDeleteMembershipUnexistingUser() {
        Group sales = identityService.newGroup("sales");
        identityService.saveGroup(sales);
        // No exception should be thrown when user doesn't exist
        identityService.deleteMembership("unexistinguser", sales.getId());
        identityService.deleteGroup(sales.getId());
    }

    @Test
    public void testDeleteMembershipNullArguments() {
        assertThatThrownBy(() -> identityService.deleteMembership(null, "group"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("userId is null");

        assertThatThrownBy(() -> identityService.deleteMembership("user", null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("groupId is null");
    }

    @Test
    public void testDeleteUserNullArguments() {
        assertThatThrownBy(() -> identityService.deleteUser(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("userId is null");
    }

    @Test
    public void testDeleteUserUnexistingUserId() {
        // No exception should be thrown. Deleting an unexisting user should
        // be ignored silently
        identityService.deleteUser("unexistinguser");
    }

    @Test
    public void testCheckPasswordNullSafe() {
        assertThat(identityService.checkPassword("userId", null)).isFalse();
        assertThat(identityService.checkPassword(null, "passwd")).isFalse();
        assertThat(identityService.checkPassword(null, null)).isFalse();
    }

    @Test
    public void testUserOptimisticLockingException() {
        User user = identityService.newUser("kermit");
        identityService.saveUser(user);

        User user1 = identityService.createUserQuery().singleResult();
        User user2 = identityService.createUserQuery().singleResult();

        user1.setFirstName("name one");
        identityService.saveUser(user1);

        assertThatThrownBy(() -> {
            user2.setFirstName("name two");
            identityService.saveUser(user2);
        })
                .isExactlyInstanceOf(FlowableOptimisticLockingException.class);

        identityService.deleteUser(user.getId());
    }

    @Test
    public void testGroupOptimisticLockingException() {
        Group group = identityService.newGroup("group");
        identityService.saveGroup(group);

        Group group1 = identityService.createGroupQuery().singleResult();
        Group group2 = identityService.createGroupQuery().singleResult();

        group1.setName("name one");
        identityService.saveGroup(group1);

        assertThatThrownBy(() -> {
            group2.setName("name two");
            identityService.saveGroup(group2);
        })
                .isExactlyInstanceOf(FlowableOptimisticLockingException.class);

        identityService.deleteGroup(group.getId());
    }

}
