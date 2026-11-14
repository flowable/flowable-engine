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
package org.flowable.rest.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.User;
import org.flowable.rest.security.SecurityConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Verifies the {@code pre-auth} authentication mode: the caller's id is taken from a trusted
 * request header (as set by a reverse proxy) instead of HTTP Basic, and authorization still
 * uses the privileges loaded from the IDM engine.
 *
 * @author Arief Hidayat
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "flowable.rest.app.authentication-mode=pre-auth",
        "flowable.rest.app.pre-auth.principal-header=X-Forwarded-User"
    }
)
@AutoConfigureTestRestTemplate
@Import(FlowableRestApplicationPreAuthenticationTest.TestBootstrapConfiguration.class)
public class FlowableRestApplicationPreAuthenticationTest {

    protected static final String PRINCIPAL_HEADER = "X-Forwarded-User";

    @LocalServerPort
    private int serverPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private IdmIdentityService idmIdentityService;

    @Test
    public void requestWithoutPrincipalHeaderIsRejected() {
        ResponseEntity<String> entity = restTemplate.getForEntity(processDefinitionsUrl(), String.class);

        // In pre-auth mode there is no HTTP Basic challenge to issue, so a request that carries
        // no principal header is an anonymous request that the authorization rules deny: 403,
        // not a 401 with a WWW-Authenticate prompt. A trusted proxy is expected to always set
        // the header, so this is the "proxy misconfigured / bypassed" path.
        assertThat(entity.getStatusCode())
            .as("GET process-definitions without a principal header")
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void userWithRestApiPrivilegeCanAccessRestApiViaHeader() {
        List<Privilege> privileges = idmIdentityService.createPrivilegeQuery().userId("rest-admin").list();
        assertThat(privileges)
            .extracting(Privilege::getName)
            .as("rest-admin privileges")
            .contains(SecurityConstants.PRIVILEGE_ACCESS_REST_API);

        HttpEntity<?> request = new HttpEntity<>(headerFor("rest-admin"));
        ResponseEntity<String> entity = restTemplate.exchange(processDefinitionsUrl(), HttpMethod.GET, request, String.class);

        assertThat(entity.getStatusCode())
            .as("GET process-definitions as rest-admin")
            .isEqualTo(HttpStatus.OK);
    }

    @Test
    public void userWithoutRestApiPrivilegeIsForbidden() {
        User user = idmIdentityService.createUserQuery().userId("test-user").singleResult();
        assertThat(user).as("test-user").isNotNull();
        List<Privilege> privileges = idmIdentityService.createPrivilegeQuery().userId("test-user").list();
        assertThat(privileges)
            .extracting(Privilege::getName)
            .as("test-user privileges")
            .doesNotContain(SecurityConstants.PRIVILEGE_ACCESS_REST_API);

        HttpEntity<?> request = new HttpEntity<>(headerFor("test-user"));
        ResponseEntity<String> entity = restTemplate.exchange(processDefinitionsUrl(), HttpMethod.GET, request, String.class);

        assertThat(entity.getStatusCode())
            .as("GET process-definitions as test-user (no access-rest-api)")
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void adminUserCanAccessActuatorViaHeader() {
        HttpEntity<?> request = new HttpEntity<>(headerFor("rest-admin"));
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-rest/actuator";
        ResponseEntity<Object> entity = restTemplate.exchange(actuatorUrl, HttpMethod.GET, request, Object.class);

        assertThat(entity.getStatusCode())
            .as("GET actuator as rest-admin (has access-admin)")
            .isEqualTo(HttpStatus.OK);
    }

    @Test
    public void nonAdminUserCannotAccessActuatorViaHeader() {
        HttpEntity<?> request = new HttpEntity<>(headerFor("test-user"));
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-rest/actuator";
        ResponseEntity<String> entity = restTemplate.exchange(actuatorUrl, HttpMethod.GET, request, String.class);

        assertThat(entity.getStatusCode())
            .as("GET actuator as test-user (no access-admin)")
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private String processDefinitionsUrl() {
        return "http://localhost:" + serverPort + "/flowable-rest/service/repository/process-definitions";
    }

    protected static HttpHeaders headerFor(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(PRINCIPAL_HEADER, userId);
        return headers;
    }

    @TestConfiguration
    public static class TestBootstrapConfiguration {

        @Bean
        public CommandLineRunner initTestUsers(IdmIdentityService idmIdentityService) {
            return args -> {
                User testUser = idmIdentityService.createUserQuery().userId("test-user").singleResult();
                if (testUser == null) {
                    User user = idmIdentityService.newUser("test-user");
                    user.setPassword("test");
                    idmIdentityService.saveUser(user);
                }
            };
        }
    }

}
