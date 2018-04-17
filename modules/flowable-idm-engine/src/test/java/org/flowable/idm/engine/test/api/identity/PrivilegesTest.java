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

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.PrivilegeMapping;
import org.flowable.idm.api.User;
import org.flowable.idm.engine.impl.persistence.entity.PrivilegeEntity;
import org.flowable.idm.engine.test.PluggableFlowableIdmTestCase;

/**
 * @author Joram Barrez
 */
public class PrivilegesTest extends PluggableFlowableIdmTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        createGroup("admins", "Admins", "user");
        createGroup("sales", "Sales", "user");
        createGroup("engineering", "Engineering", "user");

        idmIdentityService.saveUser(idmIdentityService.newUser("kermit"));
        idmIdentityService.saveUser(idmIdentityService.newUser("fozzie"));
        idmIdentityService.saveUser(idmIdentityService.newUser("mispiggy"));

        idmIdentityService.createMembership("kermit", "admins");
        idmIdentityService.createMembership("kermit", "sales");
        idmIdentityService.createMembership("kermit", "engineering");
        idmIdentityService.createMembership("fozzie", "sales");
        idmIdentityService.createMembership("mispiggy", "engineering");

        String adminPrivilegename = "access admin application";
        Privilege adminPrivilege = idmIdentityService.createPrivilege(adminPrivilegename);
        idmIdentityService.addGroupPrivilegeMapping(adminPrivilege.getId(), "admins");
        idmIdentityService.addUserPrivilegeMapping(adminPrivilege.getId(), "mispiggy");

        String modelerPrivilegeName = "access modeler application";
        Privilege modelerPrivilege = idmIdentityService.createPrivilege(modelerPrivilegeName);
        idmIdentityService.addGroupPrivilegeMapping(modelerPrivilege.getId(), "admins");
        idmIdentityService.addGroupPrivilegeMapping(modelerPrivilege.getId(), "engineering");
        idmIdentityService.addUserPrivilegeMapping(modelerPrivilege.getId(), "kermit");

        String startProcessesPrivilegename = "start processes";
        Privilege startProcessesPrivilege = idmIdentityService.createPrivilege(startProcessesPrivilegename);
        idmIdentityService.addGroupPrivilegeMapping(startProcessesPrivilege.getId(), "sales");
    }

    @Override
    protected void tearDown() throws Exception {
        clearAllUsersAndGroups();
        super.tearDown();
    }

    public void testCreateDuplicatePrivilege() {
        try {
            idmIdentityService.createPrivilege("access admin application");
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testGetUsers() {
        String privilegeId = idmIdentityService.createPrivilegeQuery().privilegeName("access admin application").singleResult().getId();
        List<User> users = idmIdentityService.getUsersWithPrivilege(privilegeId);
        assertEquals(1, users.size());
        assertEquals("mispiggy", users.get(0).getId());

        assertEquals(0, idmIdentityService.getUsersWithPrivilege("does not exist").size());

        try {
            idmIdentityService.getUsersWithPrivilege(null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testGetGroups() {
        String privilegeId = idmIdentityService.createPrivilegeQuery().privilegeName("access modeler application").singleResult().getId();
        List<Group> groups = idmIdentityService.getGroupsWithPrivilege(privilegeId);
        assertEquals(2, groups.size());
        assertEquals("admins", groups.get(0).getId());
        assertEquals("engineering", groups.get(1).getId());

        assertEquals(0, idmIdentityService.getGroupsWithPrivilege("does not exist").size());

        try {
            idmIdentityService.getGroupsWithPrivilege(null);
            fail();
        } catch (Exception e) {
        }
    }

    public void testQueryAll() {
        List<Privilege> privileges = idmIdentityService.createPrivilegeQuery().list();
        assertEquals(3, privileges.size());
        assertEquals(3L, idmIdentityService.createPrivilegeQuery().count());
    }

    public void testQueryByName() {
        List<Privilege> privileges = idmIdentityService.createPrivilegeQuery().privilegeName("access admin application").list();
        assertEquals(1, privileges.size());

        assertEquals(1, idmIdentityService.getUsersWithPrivilege(privileges.get(0).getId()).size());
        assertEquals(1, idmIdentityService.getGroupsWithPrivilege(privileges.get(0).getId()).size());
    }

    public void testQueryByInvalidName() {
        assertEquals(0, idmIdentityService.createPrivilegeQuery().privilegeName("does not exist").list().size());
    }

    public void testQueryByUserId() {
        List<Privilege> privileges = idmIdentityService.createPrivilegeQuery().userId("kermit").list();
        assertEquals(1, privileges.size());

        Privilege privilege = privileges.get(0);
        assertEquals("access modeler application", privilege.getName());
    }

    public void testQueryByInvalidUserId() {
        assertEquals(0, idmIdentityService.createPrivilegeQuery().userId("does not exist").list().size());
    }

    public void testQueryByGroupId() {
        List<Privilege> privileges = idmIdentityService.createPrivilegeQuery().groupId("admins").list();
        assertEquals(2, privileges.size());
    }

    public void testQueryByInvalidGroupId() {
        assertEquals(0, idmIdentityService.createPrivilegeQuery().groupId("does not exist").list().size());
    }

    public void testNativeQuery() {
        assertEquals("ACT_ID_PRIV", idmManagementService.getTableName(Privilege.class));
        assertEquals("ACT_ID_PRIV", idmManagementService.getTableName(PrivilegeEntity.class));

        String tableName = idmManagementService.getTableName(PrivilegeEntity.class);
        String baseQuerySql = "SELECT * FROM " + tableName + " where NAME_ = #{name}";

        assertEquals(1, idmIdentityService.createNativeUserQuery().sql(baseQuerySql).parameter("name", "access admin application").list().size());
    }

    public void testGetPrivilegeMappings() {
        Privilege modelerPrivilege = idmIdentityService.createPrivilegeQuery().privilegeName("access modeler application").singleResult();
        List<PrivilegeMapping> privilegeMappings = idmIdentityService.getPrivilegeMappingsByPrivilegeId(modelerPrivilege.getId());
        assertEquals(3, privilegeMappings.size());
        List<String> users = new ArrayList<>();
        List<String> groups = new ArrayList<>();
        
        for (PrivilegeMapping privilegeMapping : privilegeMappings) {
            if (privilegeMapping.getUserId() != null) {
                users.add(privilegeMapping.getUserId());
            
            } else if (privilegeMapping.getGroupId() != null) {
                groups.add(privilegeMapping.getGroupId());
            }
        }
        
        assertTrue(users.contains("kermit"));
        assertTrue(groups.contains("admins"));
        assertTrue(groups.contains("engineering"));
    }
    
    public void testPrivilegeUniqueName() {
        Privilege privilege = idmIdentityService.createPrivilege("test");
        
        try {
            idmIdentityService.createPrivilege("test");
            fail();
        } catch (Exception e) { 
            e.printStackTrace();
        }
        
        idmIdentityService.deletePrivilege(privilege.getId());
    }
    
}
