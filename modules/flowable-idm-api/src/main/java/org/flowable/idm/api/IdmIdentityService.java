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
package org.flowable.idm.api;

import java.util.List;

import org.flowable.engine.common.api.FlowableIllegalArgumentException;

/**
 * Service to manage {@link User}s and {@link Group}s.
 * 
 * @author Tom Baeyens
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public interface IdmIdentityService {

    /**
     * Creates a new user. The user is transient and must be saved using {@link #saveUser(User)}.
     * 
     * @param userId
     *            id for the new user, cannot be null.
     */
    User newUser(String userId);

    /**
     * Saves the user. If the user already existed, the user is updated except user password.
     * Use {@link #updateUserPassword(User)} to update existing user password.
     * 
     * @param user
     *            user to save, cannot be null.
     * @throws RuntimeException
     *             when a user with the same name already exists.
     * @see #updateUserPassword(User)
     */
    void saveUser(User user);

    /**
     * Update user password. Use {@link #saveUser(User)} for new user.
     *
     * @param user user password to change, cannot be null.
     * @see #saveUser(User)
     */
    void updateUserPassword(User user);

    /**
     * Creates a {@link UserQuery} that allows to programmatically query the users.
     */
    UserQuery createUserQuery();

    /**
     * Returns a new {@link org.flowable.engine.common.api.query.NativeQuery} for tasks.
     */
    NativeUserQuery createNativeUserQuery();

    /**
     * @param userId
     *            id of user to delete, cannot be null. When an id is passed for an unexisting user, this operation is ignored.
     */
    void deleteUser(String userId);

    /**
     * Creates a new group. The group is transient and must be saved using {@link #saveGroup(Group)}.
     * 
     * @param groupId
     *            id for the new group, cannot be null.
     */
    Group newGroup(String groupId);

    /**
     * Creates a {@link GroupQuery} that allows to programmatically query the groups.
     */
    GroupQuery createGroupQuery();

    /**
     * Returns a new {@link org.flowable.engine.common.api.query.NativeQuery} for tasks.
     */
    NativeGroupQuery createNativeGroupQuery();

    /**
     * Saves the group. If the group already existed, the group is updated.
     * 
     * @param group
     *            group to save. Cannot be null.
     * @throws RuntimeException
     *             when a group with the same name already exists.
     */
    void saveGroup(Group group);

    /**
     * Deletes the group. When no group exists with the given id, this operation is ignored.
     * 
     * @param groupId
     *            id of the group that should be deleted, cannot be null.
     */
    void deleteGroup(String groupId);

    /**
     * @param userId
     *            the userId, cannot be null.
     * @param groupId
     *            the groupId, cannot be null.
     * @throws RuntimeException
     *             when the given user or group doesn't exist or when the user is already member of the group.
     */
    void createMembership(String userId, String groupId);

    /**
     * Delete the membership of the user in the group. When the group or user don't exist or when the user is not a member of the group, this operation is ignored.
     * 
     * @param userId
     *            the user's id, cannot be null.
     * @param groupId
     *            the group's id, cannot be null.
     */
    void deleteMembership(String userId, String groupId);

    /**
     * Checks if the password is valid for the given user. Arguments userId and password are nullsafe.
     */
    boolean checkPassword(String userId, String password);
    
    /**
     * Passes the authenticated user id for this particular thread. All service method (from any service) invocations done by the same thread will have access to this authenticatedUserId.
     */
    void setAuthenticatedUserId(String authenticatedUserId);

    /**
     * Sets the picture for a given user.
     * 
     * @param userId
     * @param picture
     *            can be null to delete the picture.
     * @throws org.flowable.engine.common.api.FlowableObjectNotFoundException
     *             if the user doesn't exist.
     */
    void setUserPicture(String userId, Picture picture);

    /**
     * Retrieves the picture for a given user.
     * 
     * @param userId
     * @return null if the user doesn't have a picture.
     *
     * @throws org.flowable.engine.common.api.FlowableObjectNotFoundException
     *             if the user doesn't exist.
     */
    Picture getUserPicture(String userId);

    /**
     * Creates a new token. The token is transient and must be saved using {@link #saveToken(Token)}.
     * 
     * @param id
     *            id for the new token, cannot be null.
     */
    Token newToken(String id);

    /**
     * Saves the token. If the token already existed, the token is updated.
     * 
     * @param token
     *            token to save, cannot be null.
     */
    void saveToken(Token token);

    /**
     * @param tokenId
     *            id of token to delete, cannot be null. When an id is passed for an unexisting token, this operation is ignored.
     */
    void deleteToken(String tokenId);

    /**
     * Creates a {@link TokenQuery} that allows to programmatically query the tokens.
     */
    TokenQuery createTokenQuery();

    /**
     * Returns a new {@link org.flowable.engine.common.api.query.NativeQuery} for tokens.
     */
    NativeTokenQuery createNativeTokenQuery();

    /** Generic extensibility key-value pairs associated with a user */
    void setUserInfo(String userId, String key, String value);

    /** Generic extensibility key-value pairs associated with a user */
    String getUserInfo(String userId, String key);

    /** Generic extensibility keys associated with a user */
    List<String> getUserInfoKeys(String userId);

    /**
     * Delete an entry of the generic extensibility key-value pairs associated with a user
     */
    void deleteUserInfo(String userId, String key);

    /**
     * Creates a new {@link Privilege} with the provided name.
     * 
     * @throws FlowableIllegalArgumentException
     *             if privilegeName is null.
     */
    Privilege createPrivilege(String privilegeName);

    /**
     * Assigns a privilege to a user.
     */
    void addUserPrivilegeMapping(String privilegeId, String userId);

    /**
     * Removes a privilege for a user.
     */
    void deleteUserPrivilegeMapping(String privilegeId, String userId);

    /**
     * Assigns a privilege to a group.
     */
    void addGroupPrivilegeMapping(String privilegeId, String groupId);

    /**
     * Delete a privilege for a group.
     */
    void deleteGroupPrivilegeMapping(String privilegeId, String groupId);
    
    /**
     * Get all privilege mappings for a specific privilege
     */
    List<PrivilegeMapping> getPrivilegeMappingsByPrivilegeId(String privilegeId);

    /**
     * Deletes the privilege with the given id. Note that this also removes all user/group mappings for this privilege.
     */
    void deletePrivilege(String privilegeId);

    /**
     * Returns all {@link User} instances that have a particular privilege.
     */
    List<User> getUsersWithPrivilege(String privilegeId);

    /**
     * Returns all {@link Group} instances that have a particular privilege.
     */
    List<Group> getGroupsWithPrivilege(String privilegeId);

    /**
     * Creates a {@link PrivilegeQuery} that allows to programmatically query privileges.
     */
    PrivilegeQuery createPrivilegeQuery();

}
