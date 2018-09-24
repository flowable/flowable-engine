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

import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.User;
import org.flowable.rest.security.SecurityConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Filip Hrisafov
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebClient(registerRestTemplate = true)
@Import(FlowableRestApplicationSecurityTest.TestBootstrapConfiguration.class)
public class FlowableRestApplicationSecurityTest {

    private static final Set<String> ACTUATOR_LINKS = new HashSet<>(
        Arrays.asList(
            "self",
            "flowable",
            "auditevents",
            "beans",
            "health",
            "conditions",
            "configprops",
            "env",
            "env-toMatch",
            "info",
            "loggers-name",
            "loggers",
            "heapdump",
            "threaddump",
            "metrics",
            "metrics-requiredMetricName",
            "scheduledtasks",
            "httptrace",
            "mappings"

        )
    );

    @LocalServerPort
    private int serverPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private IdmIdentityService idmIdentityService;

    @Test
    public void nonAuthenticatedUserShouldNotBeAbleToAccessActuator() {
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-rest/actuator";
        ResponseEntity<Object> entity = restTemplate.getForEntity(actuatorUrl, Object.class);

        assertThat(entity.getStatusCode())
            .as("GET Actuator response status")
            .isEqualTo(HttpStatus.UNAUTHORIZED);

        Map<String, TestLink> links = getEndpointLinks();

        for (Map.Entry<String, TestLink> entry : links.entrySet()) {
            String endpoint = entry.getKey();
            TestLink link = entry.getValue();
            if (link.isTemplated()) {
                //Templated links are ignored
                continue;
            }

            ResponseEntity<Object> endpointResponse = restTemplate.getForEntity(link.getHref(), Object.class);

            assertThat(endpointResponse.getStatusCode())
                .as("Endpoint '" + endpoint + "' response status")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    public void nonAuthorizedUserShouldNotBeAbleToAccessActuator() {
        User user = idmIdentityService.createUserQuery().userId("test-user").singleResult();
        assertThat(user).as("test-user").isNotNull();
        List<Privilege> privileges = idmIdentityService.createPrivilegeQuery().userId("test-user").list();

        assertThat(privileges)
            .extracting(Privilege::getName)
            .as("test-user privileges")
            .doesNotContain(SecurityConstants.ACCESS_ADMIN);

        HttpEntity<?> request = new HttpEntity<>(createHeaders("test-user", "test"));
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-rest/actuator";
        ResponseEntity<Object> entity = restTemplate.exchange(actuatorUrl, HttpMethod.GET, request, Object.class);

        assertThat(entity.getStatusCode())
            .as("GET Actuator response status")
            .isEqualTo(HttpStatus.FORBIDDEN);

        Set<String> allowedEndpoints = new HashSet<>();
        allowedEndpoints.add("info");
        allowedEndpoints.add("health");
        Map<String, TestLink> links = getEndpointLinks();
        assertThat(links.keySet())
            .as("Endpoints")
            .containsAll(allowedEndpoints);

        for (Map.Entry<String, TestLink> entry : links.entrySet()) {
            String endpoint = entry.getKey();
            TestLink link = entry.getValue();
            if (link.isTemplated()) {
                //Templated links are ignored
                continue;
            }

            ResponseEntity<Object> endpointResponse = restTemplate.exchange(link.getHref(), HttpMethod.GET, request, Object.class);

            assertThat(endpointResponse.getStatusCode())
                .as("Endpoint '" + endpoint + "' response status")
                .isEqualTo(allowedEndpoints.contains(endpoint) ? HttpStatus.OK : HttpStatus.FORBIDDEN);
        }
    }

    @Test
    public void authorizedUserShouldBeAbleToAccessActuator() {
        User user = idmIdentityService.createUserQuery().userId("rest-admin").singleResult();
        assertThat(user).as("rest-admin").isNotNull();
        List<Privilege> privileges = idmIdentityService.createPrivilegeQuery().userId("rest-admin").list();

        assertThat(privileges)
            .extracting(Privilege::getName)
            .as("rest-admin privileges")
            .contains(SecurityConstants.ACCESS_ADMIN);

        HttpEntity<?> request = new HttpEntity<>(createHeaders("rest-admin", "test"));
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-rest/actuator";
        ResponseEntity<Object> entity = restTemplate.exchange(actuatorUrl, HttpMethod.GET, request, Object.class);

        assertThat(entity.getStatusCode())
            .as("GET Actuator response status")
            .isEqualTo(HttpStatus.OK);

        Map<String, TestLink> links = getEndpointLinks();
        Set<String> ignoredEndpoints = new HashSet<>();
        ignoredEndpoints.add("heapdump");
        ignoredEndpoints.add("threaddump");

        for (Map.Entry<String, TestLink> entry : links.entrySet()) {
            String endpoint = entry.getKey();
            TestLink link = entry.getValue();
            if (link.isTemplated() || ignoredEndpoints.contains(endpoint)) {
                //Templated links are ignored
                continue;
            }

            ResponseEntity<Object> endpointResponse = restTemplate.exchange(link.getHref(), HttpMethod.GET, request, Object.class);

            assertThat(endpointResponse.getStatusCode())
                .as("Endpoint '" + endpoint + "' response status")
                .isEqualTo(HttpStatus.OK);
        }
    }

    private Map<String, TestLink> getEndpointLinks() {
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-rest/actuator";
        HttpEntity<?> request = new HttpEntity<>(createHeaders("rest-admin", "test"));
        ResponseEntity<Map<String, Map<String, TestLink>>> exchange = restTemplate
            .exchange(actuatorUrl, HttpMethod.GET, request, new ParameterizedTypeReference<Map<String, Map<String, TestLink>>>() {

            });

        Map<String, Map<String, TestLink>> response = exchange.getBody();
        assertThat(response)
            .as("Actuator response")
            .isNotNull()
            .containsKeys("_links");

        Map<String, TestLink> links = response.get("_links");
        assertThat(links.keySet())
            .as("Actuator links")
            .containsExactlyInAnyOrderElementsOf(ACTUATOR_LINKS);
        return links;
    }

    protected static HttpHeaders createHeaders(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, base64Auhentication(username, password));
        return headers;
    }

    protected static String base64Auhentication(String username, String password) {
        String auth = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }

    static class TestLink {

        private String href;
        private boolean templated;

        public String getHref() {
            return href;
        }

        public void setHref(String href) {
            this.href = href;
        }

        public boolean isTemplated() {
            return templated;
        }

        public void setTemplated(boolean templated) {
            this.templated = templated;
        }
    }

    /**
     * @author Filip Hrisafov
     */
    @TestConfiguration
    public static class TestBootstrapConfiguration {

        @Bean
        public CommandLineRunner initTestUsers(IdmIdentityService idmIdentityService) {
            return args -> {
                User testUser = idmIdentityService.createUserQuery().userId("test-user").singleResult();
                if (testUser == null) {
                    createTestUser(idmIdentityService);
                }
            };
        }

        private void createTestUser(IdmIdentityService idmIdentityService) {
            User user = idmIdentityService.newUser("test-user");
            user.setPassword("test");
            idmIdentityService.saveUser(user);
        }
    }

}
