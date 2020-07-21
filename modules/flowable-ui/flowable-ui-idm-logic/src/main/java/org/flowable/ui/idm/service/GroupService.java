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
import org.flowable.idm.api.User;

/**
 * @author Joram Barre
 */
public interface GroupService {

    List<Group> getGroups(String filter);

    List<Group> getGroupsForUser(String userId);

    Group getGroup(String groupId);

    List<User> getGroupUsers(String groupId, String filter, Integer page, Integer pageSize);

    long countTotalGroupUsers(String groupId, String filter, Integer page, Integer pageSize);

    Group createNewGroup(String id, String name, String type);

    Group updateGroupName(String groupId, String name);

    void deleteGroup(String groupId);

    void addGroupMember(String groupId, String userId);

    void deleteGroupMember(String groupId, String userId);

}
