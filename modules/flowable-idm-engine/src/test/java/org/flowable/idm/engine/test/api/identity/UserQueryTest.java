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
import org.flowable.idm.api.User;
import org.flowable.idm.api.UserQuery;
import org.flowable.idm.engine.impl.persistence.entity.UserEntity;
import org.flowable.idm.engine.test.PluggableFlowableIdmTestCase;

/**
 * @author Joram Barrez
 */
public class UserQueryTest extends PluggableFlowableIdmTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        createUser("kermit", "Kermit", "Thefrog", "kermit@muppetshow.com");
        createUser("fozzie", "Fozzie", "Bear", "fozzie@muppetshow.com");
        createUser("gonzo", "Gonzo", "The great", "gonzo@muppetshow.com");

        idmIdentityService.saveGroup(idmIdentityService.newGroup("muppets"));
        idmIdentityService.saveGroup(idmIdentityService.newGroup("frogs"));

        idmIdentityService.createMembership("kermit", "muppets");
        idmIdentityService.createMembership("kermit", "frogs");
        idmIdentityService.createMembership("fozzie", "muppets");
        idmIdentityService.createMembership("gonzo", "muppets");
    }

    private User createUser(String id, String firstName, String lastName, String email) {
        User user = idmIdentityService.newUser(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        idmIdentityService.saveUser(user);
        return user;
    }

    @Override
    protected void tearDown() throws Exception {
        idmIdentityService.deleteUser("kermit");
        idmIdentityService.deleteUser("fozzie");
        idmIdentityService.deleteUser("gonzo");

        idmIdentityService.deleteGroup("muppets");
        idmIdentityService.deleteGroup("frogs");

        super.tearDown();
    }

    public void testQueryByNoCriteria() {
        UserQuery query = idmIdentityService.createUserQuery();
        verifyQueryResults(query, 3);
    }

    public void testQueryById() {
        UserQuery query = idmIdentityService.createUserQuery().userId("kermit");
        verifyQueryResults(query, 1);
    }

    public void testQueryByInvalidId() {
        UserQuery query = idmIdentityService.createUserQuery().userId("invalid");
        verifyQueryResults(query, 0);

        try {
            idmIdentityService.createUserQuery().userId(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByIdIgnoreCase() {
        UserQuery query = idmIdentityService.createUserQuery().userIdIgnoreCase("KErmit");
        verifyQueryResults(query, 1);
    }

    public void testQueryByFirstName() {
        UserQuery query = idmIdentityService.createUserQuery().userFirstName("Gonzo");
        verifyQueryResults(query, 1);

        User result = query.singleResult();
        assertEquals("gonzo", result.getId());
    }

    public void testQueryByInvalidFirstName() {
        UserQuery query = idmIdentityService.createUserQuery().userFirstName("invalid");
        verifyQueryResults(query, 0);

        try {
            idmIdentityService.createUserQuery().userFirstName(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByFirstNameLike() {
        UserQuery query = idmIdentityService.createUserQuery().userFirstNameLike("%o%");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createUserQuery().userFirstNameLike("Ker%");
        verifyQueryResults(query, 1);
    }

    public void testQueryByInvalidFirstNameLike() {
        UserQuery query = idmIdentityService.createUserQuery().userFirstNameLike("%mispiggy%");
        verifyQueryResults(query, 0);

        try {
            idmIdentityService.createUserQuery().userFirstNameLike(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByFirstNameLikeIgnoreCase() {
        UserQuery query = idmIdentityService.createUserQuery().userFirstNameLikeIgnoreCase("%O%");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createUserQuery().userFirstNameLikeIgnoreCase("KEr%");
        verifyQueryResults(query, 1);
    }

    public void testQueryByLastName() {
        UserQuery query = idmIdentityService.createUserQuery().userLastName("Bear");
        verifyQueryResults(query, 1);

        User result = query.singleResult();
        assertEquals("fozzie", result.getId());
    }

    public void testQueryByInvalidLastName() {
        UserQuery query = idmIdentityService.createUserQuery().userLastName("invalid");
        verifyQueryResults(query, 0);

        try {
            idmIdentityService.createUserQuery().userLastName(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByLastNameLike() {
        UserQuery query = idmIdentityService.createUserQuery().userLastNameLike("%rog%");
        verifyQueryResults(query, 1);

        query = idmIdentityService.createUserQuery().userLastNameLike("%ea%");
        verifyQueryResults(query, 2);
    }

    public void testQueryByLastNameLikeIgnoreCase() {
        UserQuery query = idmIdentityService.createUserQuery().userLastNameLikeIgnoreCase("%ROg%");
        verifyQueryResults(query, 1);

        query = idmIdentityService.createUserQuery().userLastNameLikeIgnoreCase("%Ea%");
        verifyQueryResults(query, 2);
    }

    public void testQueryByFullNameLike() {
        UserQuery query = idmIdentityService.createUserQuery().userFullNameLike("%erm%");
        verifyQueryResults(query, 1);

        query = idmIdentityService.createUserQuery().userFullNameLike("%ea%");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createUserQuery().userFullNameLike("%e%");
        verifyQueryResults(query, 3);
    }

    public void testQueryByFullNameLikeIgnoreCase() {
        UserQuery query = idmIdentityService.createUserQuery().userFullNameLikeIgnoreCase("%ERm%");
        verifyQueryResults(query, 1);

        query = idmIdentityService.createUserQuery().userFullNameLikeIgnoreCase("%Ea%");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createUserQuery().userFullNameLikeIgnoreCase("%E%");
        verifyQueryResults(query, 3);
    }
    
    public void testQueryByFirstAndLastNameCombinedLike() {
        UserQuery query = idmIdentityService.createUserQuery().userFullNameLike("%ermit The%");
        verifyQueryResults(query, 1);
    }

    public void testQueryByInvalidLastNameLike() {
        UserQuery query = idmIdentityService.createUserQuery().userLastNameLike("%invalid%");
        verifyQueryResults(query, 0);

        try {
            idmIdentityService.createUserQuery().userLastNameLike(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByEmail() {
        UserQuery query = idmIdentityService.createUserQuery().userEmail("kermit@muppetshow.com");
        verifyQueryResults(query, 1);
    }

    public void testQueryByInvalidEmail() {
        UserQuery query = idmIdentityService.createUserQuery().userEmail("invalid");
        verifyQueryResults(query, 0);

        try {
            idmIdentityService.createUserQuery().userEmail(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByEmailLike() {
        UserQuery query = idmIdentityService.createUserQuery().userEmailLike("%muppetshow.com");
        verifyQueryResults(query, 3);

        query = idmIdentityService.createUserQuery().userEmailLike("%kermit%");
        verifyQueryResults(query, 1);
    }

    public void testQueryByInvalidEmailLike() {
        UserQuery query = idmIdentityService.createUserQuery().userEmailLike("%invalid%");
        verifyQueryResults(query, 0);

        try {
            idmIdentityService.createUserQuery().userEmailLike(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQuerySorting() {
        // asc
        assertEquals(3, idmIdentityService.createUserQuery().orderByUserId().asc().count());
        assertEquals(3, idmIdentityService.createUserQuery().orderByUserEmail().asc().count());
        assertEquals(3, idmIdentityService.createUserQuery().orderByUserFirstName().asc().count());
        assertEquals(3, idmIdentityService.createUserQuery().orderByUserLastName().asc().count());

        // desc
        assertEquals(3, idmIdentityService.createUserQuery().orderByUserId().desc().count());
        assertEquals(3, idmIdentityService.createUserQuery().orderByUserEmail().desc().count());
        assertEquals(3, idmIdentityService.createUserQuery().orderByUserFirstName().desc().count());
        assertEquals(3, idmIdentityService.createUserQuery().orderByUserLastName().desc().count());

        // Combined with criteria
        UserQuery query = idmIdentityService.createUserQuery().userLastNameLike("%ea%").orderByUserFirstName().asc();
        List<User> users = query.list();
        assertEquals(2, users.size());
        assertEquals("Fozzie", users.get(0).getFirstName());
        assertEquals("Gonzo", users.get(1).getFirstName());
    }

    public void testQueryInvalidSortingUsage() {
        try {
            idmIdentityService.createUserQuery().orderByUserId().list();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }

        try {
            idmIdentityService.createUserQuery().orderByUserId().orderByUserEmail().list();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByMemberOf() {
        UserQuery query = idmIdentityService.createUserQuery().memberOfGroup("muppets");
        verifyQueryResults(query, 3);

        query = idmIdentityService.createUserQuery().memberOfGroup("frogs");
        verifyQueryResults(query, 1);

        User result = query.singleResult();
        assertEquals("kermit", result.getId());
    }

    public void testQueryByInvalidMemberOf() {
        UserQuery query = idmIdentityService.createUserQuery().memberOfGroup("invalid");
        verifyQueryResults(query, 0);

        try {
            idmIdentityService.createUserQuery().memberOfGroup(null).list();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    private void verifyQueryResults(UserQuery query, int countExpected) {
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

    private void verifySingleResultFails(UserQuery query) {
        try {
            query.singleResult();
            fail();
        } catch (FlowableException e) {
        }
    }

    public void testNativeQuery() {
        assertEquals("ACT_ID_USER", idmManagementService.getTableName(User.class));
        assertEquals("ACT_ID_USER", idmManagementService.getTableName(UserEntity.class));
        String tableName = idmManagementService.getTableName(User.class);
        String baseQuerySql = "SELECT * FROM " + tableName;

        assertEquals(3, idmIdentityService.createNativeUserQuery().sql(baseQuerySql).list().size());

        assertEquals(1, idmIdentityService.createNativeUserQuery().sql(baseQuerySql + " where ID_ = #{id}").parameter("id", "kermit").list().size());

        // paging
        assertEquals(2, idmIdentityService.createNativeUserQuery().sql(baseQuerySql).listPage(0, 2).size());
        assertEquals(2, idmIdentityService.createNativeUserQuery().sql(baseQuerySql).listPage(1, 3).size());
        assertEquals(1, idmIdentityService.createNativeUserQuery().sql(baseQuerySql + " where ID_ = #{id}").parameter("id", "kermit").listPage(0, 1).size());
    }

}
