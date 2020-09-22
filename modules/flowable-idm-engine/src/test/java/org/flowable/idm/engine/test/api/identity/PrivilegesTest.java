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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.PrivilegeMapping;
import org.flowable.idm.api.User;
import org.flowable.idm.engine.impl.persistence.entity.PrivilegeEntity;
import org.flowable.idm.engine.test.PluggableFlowableIdmTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class PrivilegesTest extends PluggableFlowableIdmTestCase {

    private static final String adminPrivilegename = "access admin application";
    private static final String modelerPrivilegeName = "access modeler application";

    @BeforeEach
    protected void setUp() throws Exception {
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

        Privilege adminPrivilege = idmIdentityService.createPrivilege(adminPrivilegename);
        idmIdentityService.addGroupPrivilegeMapping(adminPrivilege.getId(), "admins");
        idmIdentityService.addUserPrivilegeMapping(adminPrivilege.getId(), "mispiggy");

        Privilege modelerPrivilege = idmIdentityService.createPrivilege(modelerPrivilegeName);
        idmIdentityService.addGroupPrivilegeMapping(modelerPrivilege.getId(), "admins");
        idmIdentityService.addGroupPrivilegeMapping(modelerPrivilege.getId(), "engineering");
        idmIdentityService.addUserPrivilegeMapping(modelerPrivilege.getId(), "kermit");

        String startProcessesPrivilegename = "start processes";
        Privilege startProcessesPrivilege = idmIdentityService.createPrivilege(startProcessesPrivilegename);
        idmIdentityService.addGroupPrivilegeMapping(startProcessesPrivilege.getId(), "sales");
    }

    @AfterEach
    protected void tearDown() throws Exception {
        clearAllUsersAndGroups();
    }

    @Test
    public void deleteUserPrivilegeMapping() {
        String privilegeId = idmIdentityService.createPrivilegeQuery().privilegeName(adminPrivilegename).singleResult().getId();
        assertThat(idmIdentityService.getUsersWithPrivilege(privilegeId)).hasSize(1);
        idmIdentityService.deleteUserPrivilegeMapping(privilegeId, "mispiggy");
        assertThat(idmIdentityService.getUsersWithPrivilege(privilegeId)).isEmpty();
    }

    @Test
    public void deleteGroupPrivilegeMapping() {
        String privilegeId = idmIdentityService.createPrivilegeQuery().privilegeName(adminPrivilegename).singleResult().getId();
        assertThat(idmIdentityService.getGroupsWithPrivilege(privilegeId)).hasSize(1);
        idmIdentityService.deleteGroupPrivilegeMapping(privilegeId, "admins");
        assertThat(idmIdentityService.getGroupsWithPrivilege(privilegeId)).isEmpty();
    }

    @Test
    public void testCreateDuplicatePrivilege() {
        assertThatThrownBy(() -> idmIdentityService.createPrivilege(adminPrivilegename))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testGetUsers() {
        String privilegeId = idmIdentityService.createPrivilegeQuery().privilegeName(adminPrivilegename).singleResult().getId();
        List<User> users = idmIdentityService.getUsersWithPrivilege(privilegeId);
        assertThat(users)
                .extracting(User::getId)
                .containsExactly("mispiggy");

        assertThat(idmIdentityService.getUsersWithPrivilege("does not exist")).isEmpty();

        assertThatThrownBy(() -> idmIdentityService.getUsersWithPrivilege(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testGetGroups() {
        String privilegeId = idmIdentityService.createPrivilegeQuery().privilegeName(modelerPrivilegeName).singleResult().getId();
        List<Group> groups = idmIdentityService.getGroupsWithPrivilege(privilegeId);
        assertThat(groups)
                .extracting(Group::getId)
                .containsExactly("admins", "engineering");

        assertThat(idmIdentityService.getGroupsWithPrivilege("does not exist")).isEmpty();

        assertThatThrownBy(() -> idmIdentityService.getGroupsWithPrivilege(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryAll() {
        List<Privilege> privileges = idmIdentityService.createPrivilegeQuery().list();
        assertThat(privileges).hasSize(3);
        assertThat(idmIdentityService.createPrivilegeQuery().count()).isEqualTo(3L);
    }

    @Test
    public void testQueryByName() {
        List<Privilege> privileges = idmIdentityService.createPrivilegeQuery().privilegeName(adminPrivilegename).list();
        assertThat(privileges).hasSize(1);

        assertThat(idmIdentityService.getUsersWithPrivilege(privileges.get(0).getId())).hasSize(1);
        assertThat(idmIdentityService.getGroupsWithPrivilege(privileges.get(0).getId())).hasSize(1);
    }

    @Test
    public void testQueryByInvalidName() {
        assertThat(idmIdentityService.createPrivilegeQuery().privilegeName("does not exist").list()).isEmpty();
    }

    @Test
    public void testQueryByUserId() {
        List<Privilege> privileges = idmIdentityService.createPrivilegeQuery().userId("kermit").list();
        assertThat(privileges).hasSize(1);

        Privilege privilege = privileges.get(0);
        assertThat(privilege.getName()).isEqualTo(modelerPrivilegeName);
    }

    @Test
    public void testQueryByInvalidUserId() {
        assertThat(idmIdentityService.createPrivilegeQuery().userId("does not exist").list()).isEmpty();
    }

    @Test
    public void testQueryByGroupId() {
        List<Privilege> privileges = idmIdentityService.createPrivilegeQuery().groupId("admins").list();
        assertThat(privileges).hasSize(2);
    }

    @Test
    public void testQueryByInvalidGroupId() {
        assertThat(idmIdentityService.createPrivilegeQuery().groupId("does not exist").list()).isEmpty();
    }

    @Test
    public void testQueryByGroupIds() {
        List<Privilege> privileges = idmIdentityService.createPrivilegeQuery().groupIds(Arrays.asList("admins")).list();
        assertThat(privileges).hasSize(2);

        privileges = idmIdentityService.createPrivilegeQuery().groupIds(Arrays.asList("admins", "engineering")).list();
        assertThat(privileges).hasSize(2);

        privileges = idmIdentityService.createPrivilegeQuery().groupIds(Arrays.asList("engineering")).list();
        assertThat(privileges).hasSize(1);

        privileges = idmIdentityService.createPrivilegeQuery().groupIds(Arrays.asList("admins", "engineering")).listPage(0, 1);
        assertThat(privileges).hasSize(1);

        privileges = idmIdentityService.createPrivilegeQuery().groupIds(Arrays.asList("admins", "engineering")).listPage(1, 1);
        assertThat(privileges).hasSize(1);
    }

    @Test
    public void testQueryByInvalidGroupIds() {
        assertThat(idmIdentityService.createPrivilegeQuery().groupIds(Arrays.asList("does not exist")).list()).isEmpty();
    }

    @Test
    public void testNativeQuery() {
        assertThat(idmManagementService.getTableName(Privilege.class)).isEqualTo("ACT_ID_PRIV");
        assertThat(idmManagementService.getTableName(PrivilegeEntity.class)).isEqualTo("ACT_ID_PRIV");

        String tableName = idmManagementService.getTableName(PrivilegeEntity.class);
        String baseQuerySql = "SELECT * FROM " + tableName + " where NAME_ = #{name}";

        assertThat(idmIdentityService.createNativeUserQuery().sql(baseQuerySql).parameter("name", adminPrivilegename).list()).hasSize(1);
    }

    @Test
    public void testGetPrivilegeMappings() {
        Privilege modelerPrivilege = idmIdentityService.createPrivilegeQuery().privilegeName(modelerPrivilegeName).singleResult();
        List<PrivilegeMapping> privilegeMappings = idmIdentityService.getPrivilegeMappingsByPrivilegeId(modelerPrivilege.getId());
        assertThat(privilegeMappings).hasSize(3);
        assertThat(privilegeMappings)
                .extracting(PrivilegeMapping::getUserId)
                .contains("kermit");
        assertThat(privilegeMappings)
                .extracting(PrivilegeMapping::getGroupId)
                .contains("admins", "engineering");
    }

    @Test
    public void testPrivilegeUniqueName() {
        Privilege privilege = idmIdentityService.createPrivilege("test");

        assertThatThrownBy(() -> idmIdentityService.createPrivilege("test"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        idmIdentityService.deletePrivilege(privilege.getId());
    }

}
