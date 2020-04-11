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

import java.util.Arrays;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.idm.api.User;
import org.flowable.idm.api.UserQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class UserQueryTest extends PluggableFlowableTestCase {

    @BeforeEach
    protected void setUp() throws Exception {

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

    @AfterEach
    protected void tearDown() throws Exception {
        identityService.deleteUser("kermit");
        identityService.deleteUser("fozzie");
        identityService.deleteUser("gonzo");
        identityService.deleteUser("homer");

        identityService.deleteGroup("muppets");
        identityService.deleteGroup("frogs");

    }

    @Test
    public void testQueryByNoCriteria() {
        UserQuery query = identityService.createUserQuery();
        verifyQueryResults(query, 4);
    }

    @Test
    public void testQueryById() {
        UserQuery query = identityService.createUserQuery().userId("kermit");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidId() {
        UserQuery query = identityService.createUserQuery().userId("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> identityService.createUserQuery().userId(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByFirstName() {
        UserQuery query = identityService.createUserQuery().userFirstName("Gonzo");
        verifyQueryResults(query, 1);

        User result = query.singleResult();
        assertThat(result.getId()).isEqualTo("gonzo");
    }

    @Test
    public void testQueryByInvalidFirstName() {
        UserQuery query = identityService.createUserQuery().userFirstName("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> identityService.createUserQuery().userFirstName(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByFirstNameLike() {
        UserQuery query = identityService.createUserQuery().userFirstNameLike("%o%");
        verifyQueryResults(query, 3);

        query = identityService.createUserQuery().userFirstNameLike("Ker%");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidFirstNameLike() {
        UserQuery query = identityService.createUserQuery().userFirstNameLike("%mispiggy%");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> identityService.createUserQuery().userFirstNameLike(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByLastName() {
        UserQuery query = identityService.createUserQuery().userLastName("Bear");
        verifyQueryResults(query, 1);

        User result = query.singleResult();
        assertThat(result.getId()).isEqualTo("fozzie");
    }

    @Test
    public void testQueryByInvalidLastName() {
        UserQuery query = identityService.createUserQuery().userLastName("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> identityService.createUserQuery().userLastName(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByLastNameLike() {
        UserQuery query = identityService.createUserQuery().userLastNameLike("%rog%");
        verifyQueryResults(query, 1);

        query = identityService.createUserQuery().userLastNameLike("%ea%");
        verifyQueryResults(query, 2);
    }

    @Test
    public void testQueryByFullNameLike() {
        UserQuery query = identityService.createUserQuery().userFullNameLike("%erm%");
        verifyQueryResults(query, 1);

        query = identityService.createUserQuery().userFullNameLike("%ea%");
        verifyQueryResults(query, 2);

        query = identityService.createUserQuery().userFullNameLike("%e%");
        verifyQueryResults(query, 4);
    }

    @Test
    public void testQueryByInvalidLastNameLike() {
        UserQuery query = identityService.createUserQuery().userLastNameLike("%invalid%");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> identityService.createUserQuery().userLastNameLike(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByEmail() {
        UserQuery query = identityService.createUserQuery().userEmail("kermit@muppetshow.com");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidEmail() {
        UserQuery query = identityService.createUserQuery().userEmail("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> identityService.createUserQuery().userEmail(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByEmailLike() {
        UserQuery query = identityService.createUserQuery().userEmailLike("%muppetshow.com");
        verifyQueryResults(query, 3);

        query = identityService.createUserQuery().userEmailLike("%kermit%");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidEmailLike() {
        UserQuery query = identityService.createUserQuery().userEmailLike("%invalid%");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> identityService.createUserQuery().userEmailLike(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQuerySorting() {
        // asc
        assertThat(identityService.createUserQuery().orderByUserId().asc().count()).isEqualTo(4);
        assertThat(identityService.createUserQuery().orderByUserEmail().asc().count()).isEqualTo(4);
        assertThat(identityService.createUserQuery().orderByUserFirstName().asc().count()).isEqualTo(4);
        assertThat(identityService.createUserQuery().orderByUserLastName().asc().count()).isEqualTo(4);

        // desc
        assertThat(identityService.createUserQuery().orderByUserId().desc().count()).isEqualTo(4);
        assertThat(identityService.createUserQuery().orderByUserEmail().desc().count()).isEqualTo(4);
        assertThat(identityService.createUserQuery().orderByUserFirstName().desc().count()).isEqualTo(4);
        assertThat(identityService.createUserQuery().orderByUserLastName().desc().count()).isEqualTo(4);

        // Combined with criteria
        UserQuery query = identityService.createUserQuery().userLastNameLike("%ea%").orderByUserFirstName().asc();
        List<User> users = query.list();
        assertThat(users)
                .extracting(User::getFirstName)
                .containsExactly("Fozzie", "Gonzo");
    }

    @Test
    public void testQueryInvalidSortingUsage() {
        assertThatThrownBy(() -> identityService.createUserQuery().orderByUserId().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> identityService.createUserQuery().orderByUserId().orderByUserEmail().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByMemberOf() {
        UserQuery query = identityService.createUserQuery().memberOfGroup("muppets");
        verifyQueryResults(query, 3);

        query = identityService.createUserQuery().memberOfGroup("frogs");
        verifyQueryResults(query, 1);

        User result = query.singleResult();
        assertThat(result.getId()).isEqualTo("kermit");
    }

    @Test
    public void testQueryByInvalidMemberOf() {
        UserQuery query = identityService.createUserQuery().memberOfGroup("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> identityService.createUserQuery().memberOfGroup(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByMemberOfGroups() {
        List<User> users = identityService.createUserQuery().memberOfGroups(Arrays.asList("muppets", "frogs")).orderByUserId().asc().list();
        assertThat(users)
                .extracting(User::getId)
                .containsExactly("fozzie", "gonzo", "kermit");

        users = identityService.createUserQuery().memberOfGroups(Arrays.asList("frogs")).list();
        assertThat(users)
                .extracting(User::getId)
                .containsExactly("kermit");
    }

    @Test
    public void testQueryByInvalidMemberOfGroups() {
        UserQuery query = identityService.createUserQuery().memberOfGroups(Arrays.asList("invalid"));
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> identityService.createUserQuery().memberOfGroups(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByTenantId() {
        UserQuery query = identityService.createUserQuery().tenantId("flowable");
        verifyQueryResults(query, 3);

        query = identityService.createUserQuery().tenantId("simpsons");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidTenantId() {
        UserQuery query = identityService.createUserQuery().tenantId("invalidTenantId");
        verifyQueryResults(query, 0);
    }

    @Test
    public void testQueryByNullTenantId() {
        assertThatThrownBy(() -> identityService.createUserQuery().tenantId(null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("TenantId is null");
    }

    private void verifyQueryResults(UserQuery query, int countExpected) {
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

    private void verifySingleResultFails(UserQuery query) {
        assertThatThrownBy(() -> query.singleResult())
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    public void testNativeQuery() {
        String baseQuerySql = "SELECT * FROM " + IdentityTestUtil.getTableName("ACT_ID_USER", processEngineConfiguration);

        assertThat(identityService.createNativeUserQuery().sql(baseQuerySql).list()).hasSize(4);

        assertThat(identityService.createNativeUserQuery().sql(baseQuerySql + " where ID_ = #{id}").parameter("id", "kermit").list()).hasSize(1);

        // paging
        assertThat(identityService.createNativeUserQuery().sql(baseQuerySql).listPage(0, 2)).hasSize(2);
        assertThat(identityService.createNativeUserQuery().sql(baseQuerySql).listPage(1, 4)).hasSize(3);
        assertThat(identityService.createNativeUserQuery().sql(baseQuerySql + " where ID_ = #{id}").parameter("id", "kermit").listPage(0, 1)).hasSize(1);
    }

}
