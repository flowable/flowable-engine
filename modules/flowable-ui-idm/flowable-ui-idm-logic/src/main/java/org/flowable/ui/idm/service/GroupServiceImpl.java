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

import org.apache.commons.lang3.StringUtils;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.GroupQuery;
import org.flowable.idm.api.User;
import org.flowable.idm.api.UserQuery;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.flowable.ui.common.service.exception.NotFoundException;
import org.flowable.ui.idm.constant.GroupTypes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Joram Barrez
 */
@Service
@Transactional
public class GroupServiceImpl extends AbstractIdmService implements GroupService {

    @Override
    public List<Group> getGroupsForUser(String userId) {
        return identityService.createGroupQuery().groupMember(userId).list();
    }

    @Override
    public List<Group> getGroups(String filter) {
        GroupQuery groupQuery = identityService.createGroupQuery();
        if (StringUtils.isNotEmpty(filter)) {
            groupQuery.groupNameLikeIgnoreCase("%" + (filter != null ? filter : "") + "%");
        }
        return groupQuery.orderByGroupName().asc().list();
    }

    public Group getGroup(String groupId) {
        return identityService.createGroupQuery().groupId(groupId).singleResult();
    }

    public List<User> getGroupUsers(String groupId, String filter, Integer page, Integer pageSize) {
        int pageValue = page != null ? page.intValue() : 0;
        int pageSizeValue = pageSize != null ? pageSize.intValue() : 50;

        UserQuery userQuery = createUsersForGroupQuery(groupId, filter);
        return userQuery.listPage(pageValue, pageSizeValue);
    }

    public long countTotalGroupUsers(String groupId, String filter, Integer page, Integer pageSize) {
        return createUsersForGroupQuery(groupId, filter).count();
    }

    protected UserQuery createUsersForGroupQuery(String groupId, String filter) {
        UserQuery userQuery = identityService.createUserQuery().memberOfGroup(groupId);
        if (StringUtils.isNotEmpty(filter)) {
            userQuery.userFullNameLikeIgnoreCase("%" + filter + "%");
        }
        return userQuery;
    }

    public Group createNewGroup(String id, String name, String type) {
        if (StringUtils.isBlank(name)) {
            throw new BadRequestException("Group name required");
        }

        Group newGroup = identityService.newGroup(id);
        newGroup.setName(name);

        if (type == null) {
            newGroup.setType(GroupTypes.TYPE_ASSIGNMENT);
        } else {
            newGroup.setType(type);
        }

        identityService.saveGroup(newGroup);
        return newGroup;
    }

    public Group updateGroupName(String groupId, String name) {
        if (StringUtils.isBlank(name)) {
            throw new BadRequestException("Group name required");
        }

        Group group = identityService.createGroupQuery().groupId(groupId).singleResult();
        if (group == null) {
            throw new NotFoundException();
        }

        group.setName(name);
        identityService.saveGroup(group);

        return group;
    }

    public void deleteGroup(String groupId) {
        Group group = identityService.createGroupQuery().groupId(groupId).singleResult();
        if (group == null) {
            throw new NotFoundException();
        }

        identityService.deleteGroup(groupId);
    }

    public void addGroupMember(String groupId, String userId) {
        verifyGroupMemberExists(groupId, userId);
        Group group = identityService.createGroupQuery().groupId(groupId).singleResult();
        if (group == null) {
            throw new NotFoundException();
        }

        User user = identityService.createUserQuery().userId(userId).singleResult();
        if (user == null) {
            throw new NotFoundException();
        }

        identityService.createMembership(userId, groupId);
    }

    public void deleteGroupMember(String groupId, String userId) {
        verifyGroupMemberExists(groupId, userId);
        Group group = identityService.createGroupQuery().groupId(groupId).singleResult();
        if (group == null) {
            throw new NotFoundException();
        }

        User user = identityService.createUserQuery().userId(userId).singleResult();
        if (user == null) {
            throw new NotFoundException();
        }

        identityService.deleteMembership(userId, groupId);
    }

    protected void verifyGroupMemberExists(String groupId, String userId) {
        // Check existence
        Group group = identityService.createGroupQuery().groupId(groupId).singleResult();
        User user = identityService.createUserQuery().userId(userId).singleResult();
        for (User groupMember : identityService.createUserQuery().memberOfGroup(groupId).list()) {
            if (groupMember.getId().equals(userId)) {
                user = groupMember;
            }
        }

        if (group == null || user == null) {
            throw new NotFoundException();
        }
    }

}
