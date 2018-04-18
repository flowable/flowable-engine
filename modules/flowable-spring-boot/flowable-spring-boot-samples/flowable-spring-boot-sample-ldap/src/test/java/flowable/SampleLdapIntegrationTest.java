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
package flowable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.Collection;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.User;
import org.flowable.ldap.LDAPIdentityServiceImpl;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Filip Hrisafov
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class SampleLdapIntegrationTest extends AbstractSampleLdapTest {

    @Autowired
    private IdmIdentityService identityService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private RepositoryService repositoryService;

    private Collection<String> processes = new ArrayList<>();

    @After
    public void tearDown() {
        processes.forEach(instanceId -> runtimeService.deleteProcessInstance(instanceId, "Test tear down"));
    }

    @Test
    public void ldapIdentityServiceIsUsed() {
        assertThat(identityService).isInstanceOf(LDAPIdentityServiceImpl.class);
    }

    @Test
    public void authenticationThroughLdap() {
        assertThat(identityService.checkPassword("kermit", "pass")).isTrue();
        assertThat(identityService.checkPassword("bunsen", "pass")).isTrue();
        assertThat(identityService.checkPassword("kermit", "blah")).isFalse();
    }

    @Test
    public void authenticationThroughLdapEmptyPassword() {
        assertThatThrownBy(() -> identityService.checkPassword("kermit", null))
            .isInstanceOf(FlowableException.class)
            .hasMessage("Null or empty passwords are not allowed!");
        assertThatThrownBy(() -> identityService.checkPassword("kermit", ""))
            .isInstanceOf(FlowableException.class)
            .hasMessage("Null or empty passwords are not allowed!");
    }

    @Test
    public void candidateGroupFetchedThroughLdap() {
        repositoryService.createDeployment().addClasspathResource("processes/candidateGroup.bpmn20.xml").deploy();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testCandidateGroup");
        processes.add(processInstance.getId());
        assertThat(taskService.createTaskQuery().list()).hasSize(1);
        assertThat(taskService.createTaskQuery().taskCandidateGroup("sales").list()).hasSize(1);

        // Pepe is a member of the candidate group and should be able to find the task
        assertThat(taskService.createTaskQuery().taskCandidateUser("pepe").list()).hasSize(1);

        // Dr. Bunsen is also a member of the candidate group and should be able to find the task
        assertThat(taskService.createTaskQuery().taskCandidateUser("bunsen").list()).hasSize(1);

        // Kermit is a candidate user and should be able to find the task
        assertThat(taskService.createTaskQuery().taskCandidateUser("kermit").list()).hasSize(1);
    }

    @Test
    public void userQueryById() {
        assertThat(identityService.createUserQuery().userId("kermit").list())
            .extracting(User::getId, User::getFirstName, User::getLastName)
            .containsExactly(tuple("kermit", "Kermit", "The Frog"));

        assertThat(identityService.createUserQuery().userId("fozzie").singleResult())
            .extracting(User::getId, User::getFirstName, User::getLastName)
            .containsExactly("fozzie", "Fozzie", "Bear");
    }

    @Test
    public void userQueryByFullNameLike() {
        assertThat(identityService.createUserQuery().userFullNameLike("ermi").list())
            .extracting(User::getId, User::getFirstName, User::getLastName)
            .containsExactly(tuple("kermit", "Kermit", "The Frog"));
        assertThat(identityService.createUserQuery().userFullNameLike("ermi").count()).isEqualTo(1);

        assertThat(identityService.createUserQuery().userFullNameLike("rog").list())
            .extracting(User::getId, User::getFirstName, User::getLastName)
            .containsExactly(tuple("kermit", "Kermit", "The Frog"));
        assertThat(identityService.createUserQuery().userFullNameLike("rog").count()).isEqualTo(1);

        assertThat(identityService.createUserQuery().userFullNameLike("e").list())
            .extracting(User::getId, User::getFirstName, User::getLastName)
            .containsExactlyInAnyOrder(
                tuple("kermit", "Kermit", "The Frog"),
                tuple("pepe", "Pepe", "The King Prawn"),
                tuple("fozzie", "Fozzie", "Bear"),
                tuple("gonzo", "Gonzo", "The Great"),
                tuple("bunsen", "Dr\\, Bunsen", "Honeydew")
            );
        assertThat(identityService.createUserQuery().userFullNameLike("e").count()).isEqualTo(5);

        assertThat(identityService.createUserQuery().userFullNameLike("The").list())
            .extracting(User::getId, User::getFirstName, User::getLastName)
            .containsExactlyInAnyOrder(
                tuple("kermit", "Kermit", "The Frog"),
                tuple("pepe", "Pepe", "The King Prawn"),
                tuple("gonzo", "Gonzo", "The Great")
            );
        assertThat(identityService.createUserQuery().userFullNameLike("The").count()).isEqualTo(3);
    }
}
