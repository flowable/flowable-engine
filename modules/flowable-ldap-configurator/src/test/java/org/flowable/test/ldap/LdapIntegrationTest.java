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
package org.flowable.test.ldap;

import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.test.Deployment;
import org.flowable.idm.api.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration("classpath:flowable-context.xml")
public class LdapIntegrationTest extends LDAPTestCase {

    @Test
    public void testAuthenticationThroughLdap() {
        assertTrue(identityService.checkPassword("kermit", "pass"));
        assertTrue(identityService.checkPassword("bunsen", "pass"));
        assertFalse(identityService.checkPassword("kermit", "blah"));
    }

    @Test
    public void testAuthenticationThroughLdapEmptyPassword() {
        try {
            identityService.checkPassword("kermit", null);
            fail();
        } catch (FlowableException e) {
        }
        try {
            identityService.checkPassword("kermit", "");
            fail();
        } catch (FlowableException e) {
        }
    }

    @Test
    @Deployment
    public void testCandidateGroupFetchedThroughLdap() {
        runtimeService.startProcessInstanceByKey("testCandidateGroup");
        assertEquals(1, taskService.createTaskQuery().count());
        assertEquals(1, taskService.createTaskQuery().taskCandidateGroup("sales").count());

        // Pepe is a member of the candidate group and should be able to find the task
        assertEquals(1, taskService.createTaskQuery().taskCandidateUser("pepe").count());

        // Dr. Bunsen is also a member of the candidate group and should be able to find the task
        assertEquals(1, taskService.createTaskQuery().taskCandidateUser("bunsen").count());

        // Kermit is a candidate user and should be able to find the task
        assertEquals(1, taskService.createTaskQuery().taskCandidateUser("kermit").count());
    }

    @Test
    public void testUserQueryById() {
        List<User> users = identityService.createUserQuery().userId("kermit").list();
        assertEquals(1, users.size());

        User user = users.get(0);
        assertEquals("kermit", user.getId());
        assertEquals("Kermit", user.getFirstName());
        assertEquals("The Frog", user.getLastName());

        user = identityService.createUserQuery().userId("fozzie").singleResult();
        assertEquals("fozzie", user.getId());
        assertEquals("Fozzie", user.getFirstName());
        assertEquals("Bear", user.getLastName());
    }

    @Test
    public void testUserQueryByFullNameLike() {
        List<User> users = identityService.createUserQuery().userFullNameLike("ermi").list();
        assertEquals(1, users.size());
        assertEquals(1, identityService.createUserQuery().userFullNameLike("ermi").count());

        User user = users.get(0);
        assertEquals("kermit", user.getId());
        assertEquals("Kermit", user.getFirstName());
        assertEquals("The Frog", user.getLastName());

        users = identityService.createUserQuery().userFullNameLike("rog").list();
        assertEquals(1, users.size());
        assertEquals(1, identityService.createUserQuery().userFullNameLike("rog").count());

        user = users.get(0);
        assertEquals("kermit", user.getId());
        assertEquals("Kermit", user.getFirstName());
        assertEquals("The Frog", user.getLastName());

        users = identityService.createUserQuery().userFullNameLike("e").list();
        assertEquals(5, users.size());
        assertEquals(5, identityService.createUserQuery().userFullNameLike("e").count());

        users = identityService.createUserQuery().userFullNameLike("The").list();
        assertEquals(3, users.size());
        assertEquals(3, identityService.createUserQuery().userFullNameLike("The").count());
    }

}
