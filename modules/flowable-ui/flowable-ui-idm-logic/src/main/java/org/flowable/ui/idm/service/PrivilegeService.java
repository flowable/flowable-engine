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

/**
 * Service for retrieving and changing privilege information.
 * 
 * @author Joram Barrez
 */
public interface PrivilegeService {

    Privilege findPrivilege(String id);

    List<Privilege> findPrivileges();

    List<User> findUsersWithPrivilege(String privilegeId);

    void addUserPrivilege(String privilegeId, String userId);

    void deleteUserPrivilege(String privilegeId, String userId);

    List<Group> findGroupsWithPrivilege(String privilegeId);

    void addGroupPrivilege(String privilegeId, String groupId);

    void deleteGroupPrivilege(String privilegeId, String groupId);

}
