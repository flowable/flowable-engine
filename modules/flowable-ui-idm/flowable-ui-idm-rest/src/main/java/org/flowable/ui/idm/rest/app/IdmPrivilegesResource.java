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
package org.flowable.ui.idm.rest.app;

import org.flowable.idm.api.Group;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.User;
import org.flowable.ui.common.model.GroupRepresentation;
import org.flowable.ui.common.model.UserRepresentation;
import org.flowable.ui.common.service.exception.NotFoundException;
import org.flowable.ui.idm.model.AddGroupPrivilegeRepresentation;
import org.flowable.ui.idm.model.AddUserPrivilegeRepresentation;
import org.flowable.ui.idm.model.PrivilegeRepresentation;
import org.flowable.ui.idm.service.PrivilegeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.ArrayList;
import java.util.List;
/**
 * @author Joram Barrez
 */
@RestController
@RequestMapping("/app")
public class IdmPrivilegesResource {

    @Autowired
    protected PrivilegeService privilegeService;

    @GetMapping(value = "/rest/admin/privileges")
    public List<PrivilegeRepresentation> getPrivileges() {
        List<Privilege> privileges = privilegeService.findPrivileges();
        List<PrivilegeRepresentation> representations = new ArrayList<>(privileges.size());
        for (Privilege privilege : privileges) {
            representations.add(new PrivilegeRepresentation(privilege.getId(), privilege.getName()));
        }
        return representations;
    }

    @GetMapping(value = "/rest/admin/privileges/{privilegeId}")
    public PrivilegeRepresentation getPrivilege(@PathVariable String privilegeId) {

        Privilege privilege = privilegeService.findPrivilege(privilegeId);

        if (privilege != null) {
            PrivilegeRepresentation privilegeRepresentation = new PrivilegeRepresentation();
            privilegeRepresentation.setId(privilege.getId());
            privilegeRepresentation.setName(privilege.getName());

            List<User> users = privilegeService.findUsersWithPrivilege(privilegeId);
            for (User user : users) {
                privilegeRepresentation.addUser(new UserRepresentation(user));
            }

            List<Group> groups = privilegeService.findGroupsWithPrivilege(privilegeId);
            for (Group group : groups) {
                privilegeRepresentation.addGroup(new GroupRepresentation(group));
            }

            return privilegeRepresentation;
        } else {
            throw new NotFoundException();
        }
    }

    @GetMapping(value = "/rest/admin/privileges/{privilegeId}/users")
    public List<UserRepresentation> getUsers(@PathVariable String privilegeId) {
        return getPrivilege(privilegeId).getUsers();
    }

    @PostMapping(value = "/rest/admin/privileges/{privilegeId}/users")
    public void addUserPrivilege(@PathVariable String privilegeId,
                                 @RequestBody AddUserPrivilegeRepresentation representation) {
        privilegeService.addUserPrivilege(privilegeId, representation.getUserId());
    }

    @DeleteMapping(value = "/rest/admin/privileges/{privilegeId}/users/{userId}")
    public void deleteUserPrivilege(@PathVariable String privilegeId, @PathVariable String userId) {
        privilegeService.deleteUserPrivilege(privilegeId, userId);
    }

    @GetMapping(value = "/rest/admin/privileges/{privilegeId}/groups")
    public List<GroupRepresentation> getGroups(@PathVariable String privilegeId) {
        return getPrivilege(privilegeId).getGroups();
    }

    @PostMapping(value = "/rest/admin/privileges/{privilegeId}/groups")
    public void addGroupPrivilege(@PathVariable String privilegeId,
                                  @RequestBody AddGroupPrivilegeRepresentation representation) {
        privilegeService.addGroupPrivilege(privilegeId, representation.getGroupId());
    }

    @DeleteMapping(value = "/rest/admin/privileges/{privilegeId}/groups/{groupId}")
    public void deleteGroupPrivilege(@PathVariable String privilegeId, @PathVariable String groupId) {
        privilegeService.deleteGroupPrivilege(privilegeId, groupId);
    }

}
