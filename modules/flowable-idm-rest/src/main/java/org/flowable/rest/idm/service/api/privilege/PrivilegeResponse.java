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
package org.flowable.rest.idm.service.api.privilege;

import java.util.ArrayList;
import java.util.List;

import org.flowable.rest.idm.service.api.group.GroupResponse;
import org.flowable.rest.idm.service.api.user.UserResponse;

/**
 * @author Joram Barrez
 */
public class PrivilegeResponse {

    protected String id;
    protected String name;
    protected List<UserResponse> users;
    protected List<GroupResponse> groups;

    public PrivilegeResponse() {

    }

    public PrivilegeResponse(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<UserResponse> getUsers() {
        return users;
    }

    public void setUsers(List<UserResponse> users) {
        this.users = users;
    }

    public void addUser(UserResponse userRepresentation) {
        if (users == null) {
            users = new ArrayList<>();
        }
        users.add(userRepresentation);
    }

    public List<GroupResponse> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupResponse> groups) {
        this.groups = groups;
    }

    public void addGroup(GroupResponse groupRepresentation) {
        if (groups == null) {
            groups = new ArrayList<>();
        }
        groups.add(groupRepresentation);
    }

}
