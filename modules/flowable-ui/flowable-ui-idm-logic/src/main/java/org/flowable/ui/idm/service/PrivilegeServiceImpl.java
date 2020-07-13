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
package org.flowable.ui.idm.service;

import java.util.List;

import org.flowable.idm.api.Group;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Joram Barrez
 */
@Service
@Transactional
public class PrivilegeServiceImpl extends AbstractIdmService implements PrivilegeService {

    public PrivilegeServiceImpl() {
    }

    @Override
    public Privilege findPrivilege(String id) {
        return identityService.createPrivilegeQuery().privilegeId(id).singleResult();
    }

    public List<Privilege> findPrivileges() {
        return identityService.createPrivilegeQuery().list();
    }

    @Override
    public List<User> findUsersWithPrivilege(String privilegeId) {
        return identityService.getUsersWithPrivilege(privilegeId);
    }

    @Override
    public void addUserPrivilege(String privilegeId, String userId) {
        if (!isUserPrivilege(privilegeId, userId)) {
            identityService.addUserPrivilegeMapping(privilegeId, userId);
        }
    }

    @Override
    public void deleteUserPrivilege(String privilegeId, String userId) {
        if (isUserPrivilege(privilegeId, userId)) {
            identityService.deleteUserPrivilegeMapping(privilegeId, userId);
        }
    }

    @Override
    public List<Group> findGroupsWithPrivilege(String privilegeId) {
        return identityService.getGroupsWithPrivilege(privilegeId);
    }

    @Override
    public void addGroupPrivilege(String privilegeId, String groupId) {
        if (!isGroupPrivilege(privilegeId, groupId)) {
            identityService.addGroupPrivilegeMapping(privilegeId, groupId);
        }
    }

    @Override
    public void deleteGroupPrivilege(String privilegeId, String groupId) {
        if (isGroupPrivilege(privilegeId, groupId)) {
            identityService.deleteGroupPrivilegeMapping(privilegeId, groupId);
        }
    }

    protected boolean isUserPrivilege(String privilegeId, String userId) {
        User user = identityService.createUserQuery().userId(userId).singleResult();
        if (user == null) {
            throw new IllegalArgumentException("Invalid user id");
        }

        return identityService.createPrivilegeQuery().privilegeId(privilegeId).userId(userId).count() > 0;
    }

    protected boolean isGroupPrivilege(String privilegeId, String groupId) {
        Group group = identityService.createGroupQuery().groupId(groupId).singleResult();
        if (group == null) {
            throw new IllegalArgumentException("Invalid group id");
        }

        return identityService.createPrivilegeQuery().privilegeId(privilegeId).groupId(groupId).count() > 0;
    }

}
