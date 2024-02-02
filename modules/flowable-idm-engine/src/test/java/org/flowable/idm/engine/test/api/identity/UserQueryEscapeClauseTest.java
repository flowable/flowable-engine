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

import org.flowable.idm.api.User;
import org.flowable.idm.api.UserQuery;
import org.flowable.idm.engine.test.ResourceFlowableIdmTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserQueryEscapeClauseTest extends ResourceFlowableIdmTestCase {

    public UserQueryEscapeClauseTest() {
        super("escapeclause/flowable.idm.cfg.xml");
    }

    @BeforeEach
    protected void setUp() throws Exception {
        createUser("kermit", "Kermit%", "Thefrog%", "kermit%@muppetshow.com");
        createUser("fozzie", "Fozzie_", "Bear_", "fozzie_@muppetshow.com");
    }

    private User createUser(String id, String firstName, String lastName, String email) {
        User user = idmIdentityService.newUser(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        idmIdentityService.saveUser(user);
        return user;
    }

    @AfterEach
    protected void tearDown() throws Exception {
        idmIdentityService.deleteUser("kermit");
        idmIdentityService.deleteUser("fozzie");
    }

    @Test
    public void testQueryByFirstNameLike() {
        UserQuery query = idmIdentityService.createUserQuery().userFirstNameLike("%|%%");
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.singleResult().getId()).isEqualTo("kermit");

        query = idmIdentityService.createUserQuery().userFirstNameLike("%|_%");
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.singleResult().getId()).isEqualTo("fozzie");
    }

    @Test
    public void testQueryByLastNameLike() {
        UserQuery query = idmIdentityService.createUserQuery().userLastNameLike("%|%%");
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.singleResult().getId()).isEqualTo("kermit");

        query = idmIdentityService.createUserQuery().userLastNameLike("%|_%");
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.singleResult().getId()).isEqualTo("fozzie");
    }

    @Test
    public void testQueryByFullNameLike() {
        UserQuery query = idmIdentityService.createUserQuery().userFullNameLike("%og|%%");
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.singleResult().getId()).isEqualTo("kermit");

        query = idmIdentityService.createUserQuery().userFullNameLike("%it|%%");
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.singleResult().getId()).isEqualTo("kermit");

        query = idmIdentityService.createUserQuery().userFullNameLike("%ar|_%");
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.singleResult().getId()).isEqualTo("fozzie");

        query = idmIdentityService.createUserQuery().userFullNameLike("%ie|_%");
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.singleResult().getId()).isEqualTo("fozzie");
    }

    @Test
    public void testQueryByEmailLike() {
        UserQuery query = idmIdentityService.createUserQuery().userEmailLike("%|%%");
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.singleResult().getId()).isEqualTo("kermit");

        query = idmIdentityService.createUserQuery().userEmailLike("%|_%");
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.singleResult().getId()).isEqualTo("fozzie");
    }
}
