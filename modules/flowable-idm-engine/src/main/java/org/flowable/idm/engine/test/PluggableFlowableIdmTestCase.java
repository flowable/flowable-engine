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

package org.flowable.idm.engine.test;

import java.util.List;

import org.flowable.idm.api.Group;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.User;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Base class for the test cases.
 * 
 * The main reason not to use our own test support classes is that we need to run our test suite with various configurations, e.g. with and without spring, standalone or on a server etc. Those
 * requirements create some complications so we think it's best to use a separate base class. That way it is much easier for us to maintain our own codebase and at the same time provide stability on
 * the test support classes that we offer as part of our api (in org.flowable.engine.test).
 * 
 * @author Tom Baeyens
 * @author Joram Barrez
 */
@Tag("pluggable")
@ExtendWith(PluggableFlowableIdmExtension.class)
public abstract class PluggableFlowableIdmTestCase extends AbstractFlowableIdmTestCase {

    protected Group createGroup(String id, String name, String type) {
        Group group = idmIdentityService.newGroup(id);
        group.setName(name);
        group.setType(type);
        idmIdentityService.saveGroup(group);
        return group;
    }

    protected void clearAllUsersAndGroups() {

        // Privileges
        List<Privilege> privileges = idmIdentityService.createPrivilegeQuery().list();
        for (Privilege privilege : privileges) {
            idmIdentityService.deletePrivilege(privilege.getId());
        }

        // Groups
        List<Group> groups = idmIdentityService.createGroupQuery().list();
        for (Group group : groups) {
            List<User> members = idmIdentityService.createUserQuery().memberOfGroup(group.getId()).list();
            for (User member : members) {
                idmIdentityService.deleteMembership(member.getId(), group.getId());
            }
            idmIdentityService.deleteGroup(group.getId());
        }

        // Users
        List<User> users = idmIdentityService.createUserQuery().list();
        for (User user : users) {
            idmIdentityService.deleteUser(user.getId());
        }

    }

}
