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

import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.idm.api.User;
import org.flowable.idm.api.UserQuery;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

/**
 * @author Joram Barrez
 */
public class UserQueryTest extends PluggableFlowableTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        createUser("kermit", "Kermit", "Thefrog", "kermit@muppetshow.com", "flowable");
        createUser("fozzie", "Fozzie", "Bear", "fozzie@muppetshow.com", "flowable");
        createUser("gonzo", "Gonzo", "The great", "gonzo@muppetshow.com", "flowable");
        createUser("homer", "Homer", "Simpson", "homer@simpson.tv", "simpsons");

        identityService.saveGroup(identityService.newGroup("muppets"));
        identityService.saveGroup(identityService.newGroup("frogs"));

        identityService.createMembership("kermit", "muppets");
        identityService.createMembership("kermit", "frogs");
        identityService.createMembership("fozzie", "muppets");
        identityService.createMembership("gonzo", "muppets");
    }

    private User createUser(String id, String firstName, String lastName, String email, String tenantId) {
        User user = identityService.newUser(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setTenantId(tenantId);
        identityService.saveUser(user);
        return user;
    }

    @Override
    protected void tearDown() throws Exception {
        identityService.deleteUser("kermit");
        identityService.deleteUser("fozzie");
        identityService.deleteUser("gonzo");
        identityService.deleteUser("homer");

        identityService.deleteGroup("muppets");
        identityService.deleteGroup("frogs");

        super.tearDown();
    }

    public void testQueryByNoCriteria() {
        UserQuery query = identityService.createUserQuery();
        verifyQueryResults(query, 4);
    }

    public void testQueryById() {
        UserQuery query = identityService.createUserQuery().userId("kermit");
        verifyQueryResults(query, 1);
    }

    public void testQueryByInvalidId() {
        UserQuery query = identityService.createUserQuery().userId("invalid");
        verifyQueryResults(query, 0);

        try {
            identityService.createUserQuery().userId(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByFirstName() {
        UserQuery query = identityService.createUserQuery().userFirstName("Gonzo");
        verifyQueryResults(query, 1);

        User result = query.singleResult();
        assertEquals("gonzo", result.getId());
    }

    public void testQueryByInvalidFirstName() {
        UserQuery query = identityService.createUserQuery().userFirstName("invalid");
        verifyQueryResults(query, 0);

        try {
            identityService.createUserQuery().userFirstName(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByFirstNameLike() {
        UserQuery query = identityService.createUserQuery().userFirstNameLike("%o%");
        verifyQueryResults(query, 3);

        query = identityService.createUserQuery().userFirstNameLike("Ker%");
        verifyQueryResults(query, 1);
    }

    public void testQueryByInvalidFirstNameLike() {
        UserQuery query = identityService.createUserQuery().userFirstNameLike("%mispiggy%");
        verifyQueryResults(query, 0);

        try {
            identityService.createUserQuery().userFirstNameLike(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByLastName() {
        UserQuery query = identityService.createUserQuery().userLastName("Bear");
        verifyQueryResults(query, 1);

        User result = query.singleResult();
        assertEquals("fozzie", result.getId());
    }

    public void testQueryByInvalidLastName() {
        UserQuery query = identityService.createUserQuery().userLastName("invalid");
        verifyQueryResults(query, 0);

        try {
            identityService.createUserQuery().userLastName(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByLastNameLike() {
        UserQuery query = identityService.createUserQuery().userLastNameLike("%rog%");
        verifyQueryResults(query, 1);

        query = identityService.createUserQuery().userLastNameLike("%ea%");
        verifyQueryResults(query, 2);
    }

    public void testQueryByFullNameLike() {
        UserQuery query = identityService.createUserQuery().userFullNameLike("%erm%");
        verifyQueryResults(query, 1);

        query = identityService.createUserQuery().userFullNameLike("%ea%");
        verifyQueryResults(query, 2);

        query = identityService.createUserQuery().userFullNameLike("%e%");
        verifyQueryResults(query, 4);
    }

    public void testQueryByInvalidLastNameLike() {
        UserQuery query = identityService.createUserQuery().userLastNameLike("%invalid%");
        verifyQueryResults(query, 0);

        try {
            identityService.createUserQuery().userLastNameLike(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByEmail() {
        UserQuery query = identityService.createUserQuery().userEmail("kermit@muppetshow.com");
        verifyQueryResults(query, 1);
    }

    public void testQueryByInvalidEmail() {
        UserQuery query = identityService.createUserQuery().userEmail("invalid");
        verifyQueryResults(query, 0);

        try {
            identityService.createUserQuery().userEmail(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByEmailLike() {
        UserQuery query = identityService.createUserQuery().userEmailLike("%muppetshow.com");
        verifyQueryResults(query, 3);

        query = identityService.createUserQuery().userEmailLike("%kermit%");
        verifyQueryResults(query, 1);
    }

    public void testQueryByInvalidEmailLike() {
        UserQuery query = identityService.createUserQuery().userEmailLike("%invalid%");
        verifyQueryResults(query, 0);

        try {
            identityService.createUserQuery().userEmailLike(null).singleResult();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQuerySorting() {
        // asc
        assertEquals(4, identityService.createUserQuery().orderByUserId().asc().count());
        assertEquals(4, identityService.createUserQuery().orderByUserEmail().asc().count());
        assertEquals(4, identityService.createUserQuery().orderByUserFirstName().asc().count());
        assertEquals(4, identityService.createUserQuery().orderByUserLastName().asc().count());

        // desc
        assertEquals(4, identityService.createUserQuery().orderByUserId().desc().count());
        assertEquals(4, identityService.createUserQuery().orderByUserEmail().desc().count());
        assertEquals(4, identityService.createUserQuery().orderByUserFirstName().desc().count());
        assertEquals(4, identityService.createUserQuery().orderByUserLastName().desc().count());

        // Combined with criteria
        UserQuery query = identityService.createUserQuery().userLastNameLike("%ea%").orderByUserFirstName().asc();
        List<User> users = query.list();
        assertEquals(2, users.size());
        assertEquals("Fozzie", users.get(0).getFirstName());
        assertEquals("Gonzo", users.get(1).getFirstName());
    }

    public void testQueryInvalidSortingUsage() {
        try {
            identityService.createUserQuery().orderByUserId().list();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }

        try {
            identityService.createUserQuery().orderByUserId().orderByUserEmail().list();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByMemberOf() {
        UserQuery query = identityService.createUserQuery().memberOfGroup("muppets");
        verifyQueryResults(query, 3);

        query = identityService.createUserQuery().memberOfGroup("frogs");
        verifyQueryResults(query, 1);

        User result = query.singleResult();
        assertEquals("kermit", result.getId());
    }

    public void testQueryByInvalidMemberOf() {
        UserQuery query = identityService.createUserQuery().memberOfGroup("invalid");
        verifyQueryResults(query, 0);

        try {
            identityService.createUserQuery().memberOfGroup(null).list();
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    public void testQueryByTenantId() {
        UserQuery query = identityService.createUserQuery().tenantId("flowable");
        verifyQueryResults(query, 3);

        query = identityService.createUserQuery().tenantId("simpsons");
        verifyQueryResults(query, 1);
    }

    public void testQueryByInvalidTenantId() {
        UserQuery query = identityService.createUserQuery().tenantId("invalidTenantId");
        verifyQueryResults(query, 0);
    }

    public void testQueryByNullTenantId() {
        try {
            identityService.createUserQuery().tenantId(null);
            fail("FlowableIllegalArgumentException expected");
        } catch (FlowableIllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("TenantId is null"));
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
        String baseQuerySql = "SELECT * FROM ACT_ID_USER";

        assertEquals(4, identityService.createNativeUserQuery().sql(baseQuerySql).list().size());

        assertEquals(1, identityService.createNativeUserQuery().sql(baseQuerySql + " where ID_ = #{id}").parameter("id", "kermit").list().size());

        // paging
        assertEquals(2, identityService.createNativeUserQuery().sql(baseQuerySql).listPage(0, 2).size());
        assertEquals(3, identityService.createNativeUserQuery().sql(baseQuerySql).listPage(1, 4).size());
        assertEquals(1, identityService.createNativeUserQuery().sql(baseQuerySql + " where ID_ = #{id}").parameter("id", "kermit").listPage(0, 1).size());
    }

}
