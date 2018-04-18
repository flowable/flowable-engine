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

import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.GroupQuery;
import org.flowable.idm.engine.impl.persistence.entity.GroupEntity;
import org.flowable.idm.engine.test.PluggableFlowableIdmTestCase;

/**
 * @author Joram Barrez
 */
public class GroupQueryTest extends PluggableFlowableIdmTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

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

    @Override
    protected void tearDown() throws Exception {
        idmIdentityService.deleteUser("kermit");
        idmIdentityService.deleteUser("fozzie");
        idmIdentityService.deleteUser("mispiggy");

        idmIdentityService.deleteGroup("muppets");
        idmIdentityService.deleteGroup("mammals");
        idmIdentityService.deleteGroup("frogs");
        idmIdentityService.deleteGroup("admin");

        super.tearDown();
    }

    public void testQueryById() {
        GroupQuery query = idmIdentityService.createGroupQuery().groupId("muppets");
        verifyQueryResults(query, 1);
    }

    public void testQueryByInvalidId() {
        GroupQuery query = idmIdentityService.createGroupQuery().groupId("invalid");
        verifyQueryResults(query, 0);

        try {
            idmIdentityService.createGroupQuery().groupId(null).list();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByName() {
        GroupQuery query = idmIdentityService.createGroupQuery().groupName("Muppet show characters");
        verifyQueryResults(query, 1);

        query = idmIdentityService.createGroupQuery().groupName("Famous frogs");
        verifyQueryResults(query, 1);
    }

    public void testQueryByInvalidName() {
        GroupQuery query = idmIdentityService.createGroupQuery().groupName("invalid");
        verifyQueryResults(query, 0);

        try {
            idmIdentityService.createGroupQuery().groupName(null).list();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByNameLike() {
        GroupQuery query = idmIdentityService.createGroupQuery().groupNameLike("%Famous%");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createGroupQuery().groupNameLike("Famous%");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createGroupQuery().groupNameLike("%show%");
        verifyQueryResults(query, 1);
    }

    public void testQueryByInvalidNameLike() {
        GroupQuery query = idmIdentityService.createGroupQuery().groupNameLike("%invalid%");
        verifyQueryResults(query, 0);

        try {
            idmIdentityService.createGroupQuery().groupNameLike(null).list();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByNameLikeIgnoreCase() {
        GroupQuery query = idmIdentityService.createGroupQuery().groupNameLikeIgnoreCase("%FAMOus%");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createGroupQuery().groupNameLikeIgnoreCase("FAMOus%");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createGroupQuery().groupNameLikeIgnoreCase("%SHoW%");
        verifyQueryResults(query, 1);
    }

    public void testQueryByType() {
        GroupQuery query = idmIdentityService.createGroupQuery().groupType("user");
        verifyQueryResults(query, 3);

        query = idmIdentityService.createGroupQuery().groupType("admin");
        verifyQueryResults(query, 0);
    }

    public void testQueryByInvalidType() {
        GroupQuery query = idmIdentityService.createGroupQuery().groupType("invalid");
        verifyQueryResults(query, 0);

        try {
            idmIdentityService.createGroupQuery().groupType(null).list();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByMember() {
        GroupQuery query = idmIdentityService.createGroupQuery().groupMember("fozzie");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createGroupQuery().groupMember("kermit");
        verifyQueryResults(query, 3);

        query = query.orderByGroupId().asc();
        List<Group> groups = query.list();
        assertEquals(3, groups.size());
        assertEquals("admin", groups.get(0).getId());
        assertEquals("frogs", groups.get(1).getId());
        assertEquals("muppets", groups.get(2).getId());

        query = query.groupType("user");
        groups = query.list();
        assertEquals(2, groups.size());
        assertEquals("frogs", groups.get(0).getId());
        assertEquals("muppets", groups.get(1).getId());
    }

    public void testQueryByInvalidMember() {
        GroupQuery query = idmIdentityService.createGroupQuery().groupMember("invalid");
        verifyQueryResults(query, 0);

        try {
            idmIdentityService.createGroupQuery().groupMember(null).list();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQuerySorting() {
        // asc
        assertEquals(4, idmIdentityService.createGroupQuery().orderByGroupId().asc().count());
        assertEquals(4, idmIdentityService.createGroupQuery().orderByGroupName().asc().count());
        assertEquals(4, idmIdentityService.createGroupQuery().orderByGroupType().asc().count());

        // desc
        assertEquals(4, idmIdentityService.createGroupQuery().orderByGroupId().desc().count());
        assertEquals(4, idmIdentityService.createGroupQuery().orderByGroupName().desc().count());
        assertEquals(4, idmIdentityService.createGroupQuery().orderByGroupType().desc().count());

        // Multiple sortings
        GroupQuery query = idmIdentityService.createGroupQuery().orderByGroupType().asc().orderByGroupName().desc();
        List<Group> groups = query.list();
        assertEquals(4, query.count());

        assertEquals("security", groups.get(0).getType());
        assertEquals("user", groups.get(1).getType());
        assertEquals("user", groups.get(2).getType());
        assertEquals("user", groups.get(3).getType());

        assertEquals("admin", groups.get(0).getId());
        assertEquals("muppets", groups.get(1).getId());
        assertEquals("mammals", groups.get(2).getId());
        assertEquals("frogs", groups.get(3).getId());
    }

    public void testQueryInvalidSortingUsage() {
        try {
            idmIdentityService.createGroupQuery().orderByGroupId().list();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }

        try {
            idmIdentityService.createGroupQuery().orderByGroupId().orderByGroupName().list();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    private void verifyQueryResults(GroupQuery query, int countExpected) {
        assertEquals(countExpected, query.list().size());
        assertEquals(countExpected, query.count());

        if (countExpected == 1) {
            assertNotNull(query.singleResult());
        } else if (countExpected > 1) {
            verifySingleResultFails(query);
        } else if (countExpected == 0) {
            assertNull(query.singleResult());
        }
    }

    private void verifySingleResultFails(GroupQuery query) {
        try {
            query.singleResult();
            fail();
        } catch (FlowableException e) {
        }
    }

    public void testNativeQuery() {
        assertEquals("ACT_ID_GROUP", idmManagementService.getTableName(Group.class));
        assertEquals("ACT_ID_GROUP", idmManagementService.getTableName(GroupEntity.class));
        String tableName = idmManagementService.getTableName(Group.class);
        String baseQuerySql = "SELECT * FROM " + tableName;

        assertEquals(4, idmIdentityService.createNativeGroupQuery().sql(baseQuerySql).list().size());

        assertEquals(1, idmIdentityService.createNativeGroupQuery().sql(baseQuerySql + " where ID_ = #{id}").parameter("id", "admin").list().size());

        assertEquals(
                3,
                idmIdentityService
                        .createNativeGroupQuery()
                        .sql(
                                "SELECT aig.* from " + tableName + " aig" + " inner join ACT_ID_MEMBERSHIP aim on aig.ID_ = aim.GROUP_ID_ "
                                        + " inner join ACT_ID_USER aiu on aiu.ID_ = aim.USER_ID_ where aiu.ID_ = #{id}")
                        .parameter("id", "kermit").list().size());

        // paging
        assertEquals(2, idmIdentityService.createNativeGroupQuery().sql(baseQuerySql).listPage(0, 2).size());
        assertEquals(3, idmIdentityService.createNativeGroupQuery().sql(baseQuerySql).listPage(1, 3).size());
        assertEquals(1, idmIdentityService.createNativeGroupQuery().sql(baseQuerySql + " where ID_ = #{id}").parameter("id", "admin").listPage(0, 1).size());
    }

}
