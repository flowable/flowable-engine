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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
        assertEquals("myvalue", idmIdentityService.getUserInfo("testuser", "myinfo"));

        idmIdentityService.setUserInfo("testuser", "myinfo", "myvalue2");
        assertEquals("myvalue2", idmIdentityService.getUserInfo("testuser", "myinfo"));

        idmIdentityService.deleteUserInfo("testuser", "myinfo");
        assertNull(idmIdentityService.getUserInfo("testuser", "myinfo"));

        idmIdentityService.deleteUser(user.getId());
    }

    @Test
    public void testCreateExistingUser() {
        User user = idmIdentityService.newUser("testuser");
        idmIdentityService.saveUser(user);
        try {
            User secondUser = idmIdentityService.newUser("testuser");
            idmIdentityService.saveUser(secondUser);
            fail("Exception should have been thrown");
        } catch (RuntimeException re) {
            // Expected exception while saving new user with the same name as an
            // existing one.
        }

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
        assertEquals("Jane", user.getFirstName());
        assertEquals("Donnel", user.getLastName());
        assertEquals("updated@alfresco.com", user.getEmail());

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
        assertArrayEquals("niceface".getBytes(), picture.getBytes(), "byte arrays differ");
        assertEquals("image/string", picture.getMimeType());

        // interface definition states that setting picture to null should delete it
        idmIdentityService.setUserPicture(userId, null);
        assertNull(idmIdentityService.getUserPicture(userId), "it should be possible to nullify user picture");
        user = idmIdentityService.createUserQuery().userId("johndoe").singleResult();
        assertNull(idmIdentityService.getUserPicture(userId), "it should be possible to delete user picture");

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
        assertEquals("Updated", group.getName());

        idmIdentityService.deleteGroup(group.getId());
    }

    @Test
    public void findUserByUnexistingId() {
        User user = idmIdentityService.createUserQuery().userId("unexistinguser").singleResult();
        assertNull(user);
    }

    @Test
    public void findGroupByUnexistingId() {
        Group group = idmIdentityService.createGroupQuery().groupId("unexistinggroup").singleResult();
        assertNull(group);
    }

    @Test
    public void testCreateMembershipUnexistingGroup() {
        User johndoe = idmIdentityService.newUser("johndoe");
        idmIdentityService.saveUser(johndoe);

        try {
            idmIdentityService.createMembership(johndoe.getId(), "unexistinggroup");
            fail("Expected exception");
        } catch (RuntimeException re) {
            // Exception expected
        }

        idmIdentityService.deleteUser(johndoe.getId());
    }

    @Test
    public void testCreateMembershipUnexistingUser() {
        Group sales = idmIdentityService.newGroup("sales");
        idmIdentityService.saveGroup(sales);

        try {
            idmIdentityService.createMembership("unexistinguser", sales.getId());
            fail("Expected exception");
        } catch (RuntimeException re) {
            // Exception expected
        }

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

        try {
            idmIdentityService.createMembership(johndoe.getId(), sales.getId());
        } catch (RuntimeException re) {
            // Expected exception, membership already exists
        }

        idmIdentityService.deleteGroup(sales.getId());
        idmIdentityService.deleteUser(johndoe.getId());
    }

    @Test
    public void testSaveGroupNullArgument() {
        try {
            idmIdentityService.saveGroup(null);
            fail("FlowableException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("group is null", ae.getMessage());
        }
    }

    @Test
    public void testSaveUserNullArgument() {
        try {
            idmIdentityService.saveUser(null);
            fail("FlowableException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("user is null", ae.getMessage());
        }
    }

    @Test
    public void testFindGroupByIdNullArgument() {
        try {
            idmIdentityService.createGroupQuery().groupId(null).singleResult();
            fail("FlowableException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("id is null", ae.getMessage());
        }
    }

    @Test
    public void testCreateMembershipNullArguments() {
        try {
            idmIdentityService.createMembership(null, "group");
            fail("FlowableException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("userId is null", ae.getMessage());
        }

        try {
            idmIdentityService.createMembership("userId", null);
            fail("FlowableException expected");
        } catch (FlowableException ae) {
            assertTextPresent("groupId is null", ae.getMessage());
        }
    }

    @Test
    public void testFindGroupsByUserIdNullArguments() {
        try {
            idmIdentityService.createGroupQuery().groupMember(null).singleResult();
            fail("FlowableException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("userId is null", ae.getMessage());
        }
    }

    @Test
    public void testFindUsersByGroupUnexistingGroup() {
        List<User> users = idmIdentityService.createUserQuery().memberOfGroup("unexistinggroup").list();
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    public void testDeleteGroupNullArguments() {
        try {
            idmIdentityService.deleteGroup(null);
            fail("FlowableException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("groupId is null", ae.getMessage());
        }
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
        assertEquals(1, groups.size());
        assertEquals("sales", groups.get(0).getId());

        // Delete the membership and check members of sales group
        idmIdentityService.deleteMembership(johndoe.getId(), sales.getId());
        groups = idmIdentityService.createGroupQuery().groupMember(johndoe.getId()).list();
        assertTrue(groups.isEmpty());

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
        try {
            idmIdentityService.deleteMembership(null, "group");
            fail("FlowableException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("userId is null", ae.getMessage());
        }

        try {
            idmIdentityService.deleteMembership("user", null);
            fail("FlowableException expected");
        } catch (FlowableException ae) {
            assertTextPresent("groupId is null", ae.getMessage());
        }
    }

    @Test
    public void testDeleteUserNullArguments() {
        try {
            idmIdentityService.deleteUser(null);
            fail("FlowableException expected");
        } catch (FlowableIllegalArgumentException ae) {
            assertTextPresent("userId is null", ae.getMessage());
        }
    }

    @Test
    public void testDeleteUserUnexistingUserId() {
        // No exception should be thrown. Deleting an unexisting user should
        // be ignored silently
        idmIdentityService.deleteUser("unexistinguser");
    }

    @Test
    public void testCheckPasswordNullSafe() {
        assertFalse(idmIdentityService.checkPassword("userId", null));
        assertFalse(idmIdentityService.checkPassword(null, "passwd"));
        assertFalse(idmIdentityService.checkPassword(null, null));
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
        assertNotEquals("xxx", johndoe.getPassword());
        assertEquals("John Doe", johndoe.getFirstName());
        assertTrue(idmIdentityService.checkPassword("johndoe", "xxx"));

        user = idmIdentityService.createUserQuery().userId("johndoe").list().get(0);
        user.setPassword("yyy");
        idmIdentityService.saveUser(user);
        assertTrue(idmIdentityService.checkPassword("johndoe", "xxx"));

        user = idmIdentityService.createUserQuery().userId("johndoe").list().get(0);
        user.setPassword("yyy");
        idmIdentityService.updateUserPassword(user);
        assertTrue(idmIdentityService.checkPassword("johndoe", "yyy"));

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

        try {

            user2.setFirstName("name two");
            idmIdentityService.saveUser(user2);

            fail("Expected an exception");
        } catch (FlowableOptimisticLockingException e) {
            // Expected an exception
        }

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

        try {

            group2.setName("name two");
            idmIdentityService.saveGroup(group2);

            fail("Expected an exception");
        } catch (FlowableOptimisticLockingException e) {
            // Expected an exception
        }

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
        assertEquals("myToken", token1.getId());
        assertEquals("myValue", token1.getTokenValue());
        assertEquals("127.0.0.1", token1.getIpAddress());
        assertNull(token1.getUserAgent());

        token1.setUserAgent("myAgent");
        idmIdentityService.saveToken(token1);

        token1 = idmIdentityService.createTokenQuery().singleResult();
        assertEquals("myAgent", token1.getUserAgent());

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

        try {

            token2.setUserAgent("name two");
            idmIdentityService.saveToken(token2);

            fail("Expected an exception");
        } catch (FlowableOptimisticLockingException e) {
            // Expected an exception
        }

        idmIdentityService.deleteToken(token.getId());
    }

}
