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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.GroupQuery;
import org.flowable.idm.engine.impl.persistence.entity.GroupEntity;
import org.flowable.idm.engine.test.PluggableFlowableIdmTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class GroupQueryTest extends PluggableFlowableIdmTestCase {

    @BeforeEach
    protected void setUp() throws Exception {

        createGroup("muppets", "Muppet show characters", "user");
        createGroup("frogs", "Famous frogs", "user");
        createGroup("mammals", "Famous mammals from eighties", "user");
        createGroup("admin", "Administrators", "security");

        idmIdentityService.saveUser(idmIdentityService.newUser("kermit"));
        idmIdentityService.saveUser(idmIdentityService.newUser("fozzie"));
        idmIdentityService.saveUser(idmIdentityService.newUser("mispiggy"));

        idmIdentityService.createMembership("kermit", "muppets");
        idmIdentityService.createMembership("fozzie", "muppets");
        idmIdentityService.createMembership("mispiggy", "muppets");

        idmIdentityService.createMembership("kermit", "frogs");

        idmIdentityService.createMembership("fozzie", "mammals");
        idmIdentityService.createMembership("mispiggy", "mammals");

        idmIdentityService.createMembership("kermit", "admin");

    }

    @AfterEach
    protected void tearDown() throws Exception {
        idmIdentityService.deleteUser("kermit");
        idmIdentityService.deleteUser("fozzie");
        idmIdentityService.deleteUser("mispiggy");

        idmIdentityService.deleteGroup("muppets");
        idmIdentityService.deleteGroup("mammals");
        idmIdentityService.deleteGroup("frogs");
        idmIdentityService.deleteGroup("admin");
    }

    @Test
    public void testQueryById() {
        GroupQuery query = idmIdentityService.createGroupQuery().groupId("muppets");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidId() {
        GroupQuery query = idmIdentityService.createGroupQuery().groupId("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createGroupQuery().groupId(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> idmIdentityService.createGroupQuery().groupIds(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByName() {
        GroupQuery query = idmIdentityService.createGroupQuery().groupName("Muppet show characters");
        verifyQueryResults(query, 1);

        query = idmIdentityService.createGroupQuery().groupName("Famous frogs");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidName() {
        GroupQuery query = idmIdentityService.createGroupQuery().groupName("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createGroupQuery().groupName(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByNameLike() {
        GroupQuery query = idmIdentityService.createGroupQuery().groupNameLike("%Famous%");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createGroupQuery().groupNameLike("Famous%");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createGroupQuery().groupNameLike("%show%");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidNameLike() {
        GroupQuery query = idmIdentityService.createGroupQuery().groupNameLike("%invalid%");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createGroupQuery().groupNameLike(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByNameLikeIgnoreCase() {
        GroupQuery query = idmIdentityService.createGroupQuery().groupNameLikeIgnoreCase("%FAMOus%");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createGroupQuery().groupNameLikeIgnoreCase("FAMOus%");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createGroupQuery().groupNameLikeIgnoreCase("%SHoW%");
        verifyQueryResults(query, 1);

        assertThatThrownBy(() -> idmIdentityService.createGroupQuery().groupNameLikeIgnoreCase(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByType() {
        GroupQuery query = idmIdentityService.createGroupQuery().groupType("user");
        verifyQueryResults(query, 3);

        query = idmIdentityService.createGroupQuery().groupType("admin");
        verifyQueryResults(query, 0);
    }

    @Test
    public void testQueryByInvalidType() {
        GroupQuery query = idmIdentityService.createGroupQuery().groupType("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createGroupQuery().groupType(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByMember() {
        GroupQuery query = idmIdentityService.createGroupQuery().groupMember("fozzie");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createGroupQuery().groupMember("kermit");
        verifyQueryResults(query, 3);

        query = query.orderByGroupId().asc();
        List<Group> groups = query.list();
        assertThat(groups)
                .extracting(Group::getId)
                .containsExactly("admin", "frogs", "muppets");

        query = query.groupType("user");
        groups = query.list();
        assertThat(groups)
                .extracting(Group::getId)
                .containsExactly("frogs", "muppets");
    }

    @Test
    public void testQueryByInvalidMember() {
        GroupQuery query = idmIdentityService.createGroupQuery().groupMember("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createGroupQuery().groupMember(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByGroupMembers() {
        List<Group> groups = idmIdentityService.createGroupQuery().groupMembers(Arrays.asList("kermit", "fozzie")).orderByGroupId().asc().list();
        assertThat(groups)
                .extracting(Group::getId)
                .containsExactly("admin", "frogs", "mammals", "muppets");

        groups = idmIdentityService.createGroupQuery().groupMembers(Arrays.asList("fozzie")).orderByGroupId().asc().list();
        assertThat(groups)
                .extracting(Group::getId)
                .containsExactly("mammals", "muppets");

        groups = idmIdentityService.createGroupQuery().groupMembers(Arrays.asList("kermit", "fozzie")).orderByGroupId().asc().listPage(1, 2);
        assertThat(groups)
                .extracting(Group::getId)
                .containsExactly("frogs", "mammals");
    }

    @Test
    public void testQueryByInvalidGroupMember() {
        GroupQuery query = idmIdentityService.createGroupQuery().groupMembers(Arrays.asList("invalid"));
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createGroupQuery().groupMembers(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQuerySorting() {
        // asc
        assertThat(idmIdentityService.createGroupQuery().orderByGroupId().asc().count()).isEqualTo(4);
        assertThat(idmIdentityService.createGroupQuery().orderByGroupName().asc().count()).isEqualTo(4);
        assertThat(idmIdentityService.createGroupQuery().orderByGroupType().asc().count()).isEqualTo(4);

        // desc
        assertThat(idmIdentityService.createGroupQuery().orderByGroupId().desc().count()).isEqualTo(4);
        assertThat(idmIdentityService.createGroupQuery().orderByGroupName().desc().count()).isEqualTo(4);
        assertThat(idmIdentityService.createGroupQuery().orderByGroupType().desc().count()).isEqualTo(4);

        // Multiple sortings
        GroupQuery query = idmIdentityService.createGroupQuery().orderByGroupType().asc().orderByGroupName().desc();
        List<Group> groups = query.list();
        assertThat(groups)
                .extracting(Group::getType)
                .containsExactly("security", "user", "user", "user");

        assertThat(groups)
                .extracting(Group::getId)
                .containsExactly("admin", "muppets", "mammals", "frogs");
    }

    @Test
    public void testQueryInvalidSortingUsage() {
        assertThatThrownBy(() -> idmIdentityService.createGroupQuery().orderByGroupName().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    private void verifyQueryResults(GroupQuery query, int countExpected) {
        assertThat(query.list()).hasSize(countExpected);
        assertThat(query.count()).isEqualTo(countExpected);

        if (countExpected == 1) {
            assertThat(query.singleResult()).isNotNull();
        } else if (countExpected > 1) {
            verifySingleResultFails(query);
        } else if (countExpected == 0) {
            assertThat(query.singleResult()).isNull();
        }
    }

    private void verifySingleResultFails(GroupQuery query) {
        assertThatThrownBy(() -> query.singleResult())
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    public void testNativeQuery() {
        assertThat(idmManagementService.getTableName(Group.class)).isEqualTo("ACT_ID_GROUP");
        assertThat(idmManagementService.getTableName(GroupEntity.class)).isEqualTo("ACT_ID_GROUP");
        String tableName = idmManagementService.getTableName(Group.class);
        String baseQuerySql = "SELECT * FROM " + tableName;

        assertThat(idmIdentityService.createNativeGroupQuery().sql(baseQuerySql).list()).hasSize(4);

        assertThat(idmIdentityService.createNativeGroupQuery().sql(baseQuerySql + " where ID_ = #{id}").parameter("id", "admin").list()).hasSize(1);

        assertThat(idmIdentityService
                .createNativeGroupQuery()
                .sql(
                        "SELECT aig.* from " + tableName + " aig" + " inner join ACT_ID_MEMBERSHIP aim on aig.ID_ = aim.GROUP_ID_ "
                                + " inner join ACT_ID_USER aiu on aiu.ID_ = aim.USER_ID_ where aiu.ID_ = #{id}")
                .parameter("id", "kermit").list()).hasSize(3);

        // paging
        assertThat(idmIdentityService.createNativeGroupQuery().sql(baseQuerySql).listPage(0, 2)).hasSize(2);
        assertThat(idmIdentityService.createNativeGroupQuery().sql(baseQuerySql).listPage(1, 3)).hasSize(3);
        assertThat(idmIdentityService.createNativeGroupQuery().sql(baseQuerySql + " where ID_ = #{id}").parameter("id", "admin").listPage(0, 1)).hasSize(1);
    }

}
