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

import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.idm.api.User;
import org.flowable.idm.api.UserQuery;
import org.flowable.idm.engine.impl.persistence.entity.UserEntity;
import org.flowable.idm.engine.test.PluggableFlowableIdmTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class UserQueryTest extends PluggableFlowableIdmTestCase {

    @BeforeEach
    protected void setUp() throws Exception {
        createUser("kermit", "Kermit", "Thefrog", "Kermit Thefrog", "kermit@muppetshow.com");
        createUser("fozzie", "Fozzie", "Bear", "Fozzie Bear", "fozzie@muppetshow.com");
        createUser("gonzo", "Gonzo", "The great", "Gonzo The great", "gonzo@muppetshow.com");

        idmIdentityService.saveGroup(idmIdentityService.newGroup("muppets"));
        idmIdentityService.saveGroup(idmIdentityService.newGroup("frogs"));

        idmIdentityService.createMembership("kermit", "muppets");
        idmIdentityService.createMembership("kermit", "frogs");
        idmIdentityService.createMembership("fozzie", "muppets");
        idmIdentityService.createMembership("gonzo", "muppets");
    }

    private User createUser(String id, String firstName, String lastName, String displayName, String email) {
        User user = idmIdentityService.newUser(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDisplayName(displayName);
        user.setEmail(email);
        idmIdentityService.saveUser(user);
        return user;
    }

    @AfterEach
    protected void tearDown() throws Exception {
        idmIdentityService.deleteUser("kermit");
        idmIdentityService.deleteUser("fozzie");
        idmIdentityService.deleteUser("gonzo");

        idmIdentityService.deleteGroup("muppets");
        idmIdentityService.deleteGroup("frogs");
    }

    @Test
    public void testQueryByNoCriteria() {
        UserQuery query = idmIdentityService.createUserQuery();
        verifyQueryResults(query, 3);
    }

    @Test
    public void testQueryById() {
        UserQuery query = idmIdentityService.createUserQuery().userId("kermit");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidId() {
        UserQuery query = idmIdentityService.createUserQuery().userId("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createUserQuery().userId(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> idmIdentityService.createUserQuery().userIds(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByIdIgnoreCase() {
        UserQuery query = idmIdentityService.createUserQuery().userIdIgnoreCase("KErmit");
        verifyQueryResults(query, 1);

        assertThatThrownBy(() -> idmIdentityService.createUserQuery().userIdIgnoreCase(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByFirstName() {
        UserQuery query = idmIdentityService.createUserQuery().userFirstName("Gonzo");
        verifyQueryResults(query, 1);

        User result = query.singleResult();
        assertThat(result.getId()).isEqualTo("gonzo");
    }

    @Test
    public void testQueryByInvalidFirstName() {
        UserQuery query = idmIdentityService.createUserQuery().userFirstName("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createUserQuery().userFirstName(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByFirstNameLike() {
        UserQuery query = idmIdentityService.createUserQuery().userFirstNameLike("%o%");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createUserQuery().userFirstNameLike("Ker%");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidFirstNameLike() {
        UserQuery query = idmIdentityService.createUserQuery().userFirstNameLike("%mispiggy%");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createUserQuery().userFirstNameLike(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByFirstNameLikeIgnoreCase() {
        UserQuery query = idmIdentityService.createUserQuery().userFirstNameLikeIgnoreCase("%O%");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createUserQuery().userFirstNameLikeIgnoreCase("KEr%");
        verifyQueryResults(query, 1);

        assertThatThrownBy(() -> idmIdentityService.createUserQuery().userFirstNameLikeIgnoreCase(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByLastName() {
        UserQuery query = idmIdentityService.createUserQuery().userLastName("Bear");
        verifyQueryResults(query, 1);

        User result = query.singleResult();
        assertThat(result.getId()).isEqualTo("fozzie");
    }

    @Test
    public void testQueryByInvalidLastName() {
        UserQuery query = idmIdentityService.createUserQuery().userLastName("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createUserQuery().userLastName(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByLastNameLike() {
        UserQuery query = idmIdentityService.createUserQuery().userLastNameLike("%rog%");
        verifyQueryResults(query, 1);

        query = idmIdentityService.createUserQuery().userLastNameLike("%ea%");
        verifyQueryResults(query, 2);
    }

    @Test
    public void testQueryByLastNameLikeIgnoreCase() {
        UserQuery query = idmIdentityService.createUserQuery().userLastNameLikeIgnoreCase("%ROg%");
        verifyQueryResults(query, 1);

        query = idmIdentityService.createUserQuery().userLastNameLikeIgnoreCase("%Ea%");
        verifyQueryResults(query, 2);

        assertThatThrownBy(() -> idmIdentityService.createUserQuery().userLastNameLikeIgnoreCase(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByFullNameLike() {
        UserQuery query = idmIdentityService.createUserQuery().userFullNameLike("%erm%");
        verifyQueryResults(query, 1);

        query = idmIdentityService.createUserQuery().userFullNameLike("%ea%");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createUserQuery().userFullNameLike("%e%");
        verifyQueryResults(query, 3);

        assertThatThrownBy(() -> idmIdentityService.createUserQuery().userFullNameLike(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByFullNameLikeIgnoreCase() {
        UserQuery query = idmIdentityService.createUserQuery().userFullNameLikeIgnoreCase("%ERm%");
        verifyQueryResults(query, 1);

        query = idmIdentityService.createUserQuery().userFullNameLikeIgnoreCase("%Ea%");
        verifyQueryResults(query, 2);

        query = idmIdentityService.createUserQuery().userFullNameLikeIgnoreCase("%E%");
        verifyQueryResults(query, 3);

        assertThatThrownBy(() -> idmIdentityService.createUserQuery().userFullNameLikeIgnoreCase(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByFirstAndLastNameCombinedLike() {
        UserQuery query = idmIdentityService.createUserQuery().userFullNameLike("%ermit The%");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidLastNameLike() {
        UserQuery query = idmIdentityService.createUserQuery().userLastNameLike("%invalid%");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createUserQuery().userLastNameLike(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByDisplayName() {
        UserQuery query = idmIdentityService.createUserQuery().userDisplayName("Fozzie Bear");
        verifyQueryResults(query, 1);

        User result = query.singleResult();
        assertThat(result.getId()).isEqualTo("fozzie");
    }

    @Test
    public void testQueryByInvalidDisplayName() {
        UserQuery query = idmIdentityService.createUserQuery().userDisplayName("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createUserQuery().userDisplayName(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByDisplayNameLike() {
        UserQuery query = idmIdentityService.createUserQuery().userDisplayNameLike("%rog%");
        verifyQueryResults(query, 1);

        query = idmIdentityService.createUserQuery().userDisplayNameLike("%ea%");
        verifyQueryResults(query, 2);

        assertThatThrownBy(() -> idmIdentityService.createUserQuery().userDisplayNameLike(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByDisplayNameLikeIgnoreCase() {
        UserQuery query = idmIdentityService.createUserQuery().userDisplayNameLikeIgnoreCase("%ROg%");
        verifyQueryResults(query, 1);

        query = idmIdentityService.createUserQuery().userDisplayNameLikeIgnoreCase("%Ea%");
        verifyQueryResults(query, 2);

        assertThatThrownBy(() -> idmIdentityService.createUserQuery().userDisplayNameLikeIgnoreCase(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByEmail() {
        UserQuery query = idmIdentityService.createUserQuery().userEmail("kermit@muppetshow.com");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidEmail() {
        UserQuery query = idmIdentityService.createUserQuery().userEmail("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createUserQuery().userEmail(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByEmailLike() {
        UserQuery query = idmIdentityService.createUserQuery().userEmailLike("%muppetshow.com");
        verifyQueryResults(query, 3);

        query = idmIdentityService.createUserQuery().userEmailLike("%kermit%");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidEmailLike() {
        UserQuery query = idmIdentityService.createUserQuery().userEmailLike("%invalid%");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createUserQuery().userEmailLike(null).singleResult())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQuerySorting() {
        // asc
        assertThat(idmIdentityService.createUserQuery().orderByUserId().asc().count()).isEqualTo(3);
        assertThat(idmIdentityService.createUserQuery().orderByUserEmail().asc().count()).isEqualTo(3);
        assertThat(idmIdentityService.createUserQuery().orderByUserFirstName().asc().count()).isEqualTo(3);
        assertThat(idmIdentityService.createUserQuery().orderByUserLastName().asc().count()).isEqualTo(3);

        // desc
        assertThat(idmIdentityService.createUserQuery().orderByUserId().desc().count()).isEqualTo(3);
        assertThat(idmIdentityService.createUserQuery().orderByUserEmail().desc().count()).isEqualTo(3);
        assertThat(idmIdentityService.createUserQuery().orderByUserFirstName().desc().count()).isEqualTo(3);
        assertThat(idmIdentityService.createUserQuery().orderByUserLastName().desc().count()).isEqualTo(3);

        // Combined with criteria
        UserQuery query = idmIdentityService.createUserQuery().userLastNameLike("%ea%").orderByUserFirstName().asc();
        List<User> users = query.list();
        assertThat(users)
                .extracting(User::getFirstName)
                .containsExactly("Fozzie", "Gonzo");
    }

    @Test
    public void testQueryInvalidSortingUsage() {
        assertThatThrownBy(() -> idmIdentityService.createUserQuery().orderByUserId().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> idmIdentityService.createUserQuery().orderByUserId().orderByUserEmail().list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByMemberOf() {
        UserQuery query = idmIdentityService.createUserQuery().memberOfGroup("muppets");
        verifyQueryResults(query, 3);

        query = idmIdentityService.createUserQuery().memberOfGroup("frogs");
        verifyQueryResults(query, 1);

        User result = query.singleResult();
        assertThat(result.getId()).isEqualTo("kermit");
    }

    @Test
    public void testQueryByInvalidMemberOf() {
        UserQuery query = idmIdentityService.createUserQuery().memberOfGroup("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> idmIdentityService.createUserQuery().memberOfGroup(null).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class);
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
        assertThat(idmManagementService.getTableName(User.class)).isEqualTo("ACT_ID_USER");
        assertThat(idmManagementService.getTableName(UserEntity.class)).isEqualTo("ACT_ID_USER");
        String tableName = idmManagementService.getTableName(User.class);
        String baseQuerySql = "SELECT * FROM " + tableName;

        assertThat(idmIdentityService.createNativeUserQuery().sql(baseQuerySql).list()).hasSize(3);

        assertThat(idmIdentityService.createNativeUserQuery().sql(baseQuerySql + " where ID_ = #{id}").parameter("id", "kermit").list()).hasSize(1);

        // paging
        assertThat(idmIdentityService.createNativeUserQuery().sql(baseQuerySql).listPage(0, 2)).hasSize(2);
        assertThat(idmIdentityService.createNativeUserQuery().sql(baseQuerySql).listPage(1, 3)).hasSize(2);
        assertThat(idmIdentityService.createNativeUserQuery().sql(baseQuerySql + " where ID_ = #{id}").parameter("id", "kermit").listPage(0, 1)).hasSize(1);
    }

}
