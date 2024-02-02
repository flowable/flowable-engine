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

package org.flowable.engine.test.api.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.GroupQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class GroupQueryTest extends PluggableFlowableTestCase {

    @BeforeEach
    public void setUp() throws Exception {

        createGroup("muppets", "Muppet show characters", "user");
        createGroup("frogs", "Famous frogs", "user");
        createGroup("mammals", "Famous mammals from eighties", "user");
        createGroup("admin", "Administrators", "security");

        identityService.saveUser(identityService.newUser("kermit"));
        identityService.saveUser(identityService.newUser("fozzie"));
        identityService.saveUser(identityService.newUser("mispiggy"));

        identityService.createMembership("kermit", "muppets");
        identityService.createMembership("fozzie", "muppets");
        identityService.createMembership("mispiggy", "muppets");

        identityService.createMembership("kermit", "frogs");

        identityService.createMembership("fozzie", "mammals");
        identityService.createMembership("mispiggy", "mammals");

        identityService.createMembership("kermit", "admin");

    }

    private Group createGroup(String id, String name, String type) {
        Group group = identityService.newGroup(id);
        group.setName(name);
        group.setType(type);
        identityService.saveGroup(group);
        return group;
    }

    @AfterEach
    public void tearDown() throws Exception {
        identityService.deleteUser("kermit");
        identityService.deleteUser("fozzie");
        identityService.deleteUser("mispiggy");

        identityService.deleteGroup("muppets");
        identityService.deleteGroup("mammals");
        identityService.deleteGroup("frogs");
        identityService.deleteGroup("admin");

    }

    @Test
    public void testQueryById() {
        GroupQuery query = identityService.createGroupQuery().groupId("muppets");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidId() {
        GroupQuery query = identityService.createGroupQuery().groupId("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> identityService.createGroupQuery().groupId(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByName() {
        GroupQuery query = identityService.createGroupQuery().groupName("Muppet show characters");
        verifyQueryResults(query, 1);

        query = identityService.createGroupQuery().groupName("Famous frogs");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidName() {
        GroupQuery query = identityService.createGroupQuery().groupName("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> identityService.createGroupQuery().groupName(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByNameLike() {
        GroupQuery query = identityService.createGroupQuery().groupNameLike("%Famous%");
        verifyQueryResults(query, 2);

        query = identityService.createGroupQuery().groupNameLike("Famous%");
        verifyQueryResults(query, 2);

        query = identityService.createGroupQuery().groupNameLike("%show%");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidNameLike() {
        GroupQuery query = identityService.createGroupQuery().groupNameLike("%invalid%");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> identityService.createGroupQuery().groupNameLike(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByType() {
        GroupQuery query = identityService.createGroupQuery().groupType("user");
        verifyQueryResults(query, 3);

        query = identityService.createGroupQuery().groupType("admin");
        verifyQueryResults(query, 0);
    }

    @Test
    public void testQueryByInvalidType() {
        GroupQuery query = identityService.createGroupQuery().groupType("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> identityService.createGroupQuery().groupType(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByMember() {
        GroupQuery query = identityService.createGroupQuery().groupMember("fozzie");
        verifyQueryResults(query, 2);

        query = identityService.createGroupQuery().groupMember("kermit");
        verifyQueryResults(query, 3);

        query = query.orderByGroupId().asc();
        List<Group> groups = query.list();
        assertThat(groups).hasSize(3);
        assertThat(groups.get(0).getId()).isEqualTo("admin");
        assertThat(groups.get(1).getId()).isEqualTo("frogs");
        assertThat(groups.get(2).getId()).isEqualTo("muppets");

        query = query.groupType("user");
        groups = query.list();
        assertThat(groups)
                .extracting(Group::getId)
                .containsExactly("frogs", "muppets");
    }

    @Test
    public void testQueryByInvalidMember() {
        GroupQuery query = identityService.createGroupQuery().groupMember("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> identityService.createGroupQuery().groupMember(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQuerySorting() {
        // asc
        assertThat(identityService.createGroupQuery().orderByGroupId().asc().count()).isEqualTo(4);
        assertThat(identityService.createGroupQuery().orderByGroupName().asc().count()).isEqualTo(4);
        assertThat(identityService.createGroupQuery().orderByGroupType().asc().count()).isEqualTo(4);

        // desc
        assertThat(identityService.createGroupQuery().orderByGroupId().desc().count()).isEqualTo(4);
        assertThat(identityService.createGroupQuery().orderByGroupName().desc().count()).isEqualTo(4);
        assertThat(identityService.createGroupQuery().orderByGroupType().desc().count()).isEqualTo(4);

        // Multiple sortings
        GroupQuery query = identityService.createGroupQuery().orderByGroupType().asc().orderByGroupName().desc();
        List<Group> groups = query.list();
        assertThat(query.count()).isEqualTo(4);

        assertThat(groups.get(0).getType()).isEqualTo("security");
        assertThat(groups.get(1).getType()).isEqualTo("user");
        assertThat(groups.get(2).getType()).isEqualTo("user");
        assertThat(groups.get(3).getType()).isEqualTo("user");

        assertThat(groups.get(0).getId()).isEqualTo("admin");
        assertThat(groups.get(1).getId()).isEqualTo("muppets");
        assertThat(groups.get(2).getId()).isEqualTo("mammals");
        assertThat(groups.get(3).getId()).isEqualTo("frogs");
    }

    @Test
    public void testQueryInvalidSortingUsage() {
        assertThatThrownBy(() -> identityService.createGroupQuery().orderByGroupId().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> identityService.createGroupQuery().orderByGroupName().list())
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
        String baseQuerySql = "SELECT * FROM " + IdentityTestUtil.getTableName("ACT_ID_GROUP", processEngineConfiguration);

        assertThat(identityService.createNativeGroupQuery().sql(baseQuerySql).list()).hasSize(4);

        assertThat(identityService.createNativeGroupQuery().sql(baseQuerySql + " where ID_ = #{id}").parameter("id", "admin").list()).hasSize(1);

        assertThat(identityService.createNativeGroupQuery()
                .sql(
                        "SELECT aig.* from " + IdentityTestUtil.getTableName("ACT_ID_GROUP", processEngineConfiguration) + " aig" + " inner join "
                                + IdentityTestUtil.getTableName("ACT_ID_MEMBERSHIP", processEngineConfiguration) + " aim on aig.ID_ = aim.GROUP_ID_ "
                                + " inner join " + IdentityTestUtil.getTableName("ACT_ID_USER", processEngineConfiguration)
                                + " aiu on aiu.ID_ = aim.USER_ID_ where aiu.ID_ = #{id}")
                .parameter("id", "kermit").list()).hasSize(3);

        // paging
        assertThat(identityService.createNativeGroupQuery().sql(baseQuerySql).listPage(0, 2)).hasSize(2);
        assertThat(identityService.createNativeGroupQuery().sql(baseQuerySql).listPage(1, 3)).hasSize(3);
        assertThat(identityService.createNativeGroupQuery().sql(baseQuerySql + " where ID_ = #{id}").parameter("id", "admin").listPage(0, 1)).hasSize(1);
    }

}
