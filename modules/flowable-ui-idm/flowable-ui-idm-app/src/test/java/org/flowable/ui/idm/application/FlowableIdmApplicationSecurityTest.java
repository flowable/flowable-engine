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
package org.flowable.ui.idm.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.Token;
import org.flowable.idm.api.User;
import org.flowable.ui.common.security.CookieConstants;
import org.flowable.ui.common.security.DefaultPrivileges;
import org.junit.After;
import org.junit.Before;
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
@Import(FlowableIdmApplicationSecurityTest.TestBootstrapConfiguration.class)
public class FlowableIdmApplicationSecurityTest {

    private static final Set<String> ACTUATOR_LINKS = new HashSet<>(
        Arrays.asList(
            "self",
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
            "mappings",
            "caches",
            "caches-cache",
            "health-path"
        )
    );

    @LocalServerPort
    private int serverPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private IdmIdentityService idmIdentityService;

    @Before
    public void validateUsers() {
        User user = idmIdentityService.createUserQuery().userId("test-user").singleResult();
        assertThat(user).as("test-user").isNotNull();
        List<Privilege> userPrivileges = idmIdentityService.createPrivilegeQuery().userId("test-user").list();

        assertThat(userPrivileges)
            .extracting(Privilege::getName)
            .as("test-user privileges")
            .isEmpty();

        User admin = idmIdentityService.createUserQuery().userId("test-admin").singleResult();
        assertThat(admin).as("test-admin").isNotNull();
        List<Privilege> adminPrivileges = idmIdentityService.createPrivilegeQuery().userId("test-admin").list();

        assertThat(adminPrivileges)
            .extracting(Privilege::getName)
            .as("test-admin privileges")
            .contains(DefaultPrivileges.ACCESS_ADMIN)
            .doesNotContain(DefaultPrivileges.ACCESS_REST_API, DefaultPrivileges.ACCESS_IDM);

        User idm = idmIdentityService.createUserQuery().userId("test-idm").singleResult();
        assertThat(idm).as("test-idm").isNotNull();
        List<Privilege> idmPrivileges = idmIdentityService.createPrivilegeQuery().userId("test-idm").list();

        assertThat(idmPrivileges)
            .extracting(Privilege::getName)
            .as("test-idm privileges")
            .contains(DefaultPrivileges.ACCESS_IDM)
            .doesNotContain(DefaultPrivileges.ACCESS_REST_API, DefaultPrivileges.ACCESS_ADMIN);

        User rest = idmIdentityService.createUserQuery().userId("test-rest").singleResult();
        assertThat(rest).as("test-rest").isNotNull();
        List<Privilege> restPrivileges = idmIdentityService.createPrivilegeQuery().userId("test-rest").list();

        assertThat(restPrivileges)
            .extracting(Privilege::getName)
            .as("test-rest privileges")
            .contains(DefaultPrivileges.ACCESS_REST_API)
            .doesNotContain(DefaultPrivileges.ACCESS_IDM, DefaultPrivileges.ACCESS_ADMIN);
    }

    @Before
    public void createTokens() {
        Token tokenUser = idmIdentityService.newToken("user");
        tokenUser.setUserId("test-user");
        tokenUser.setTokenValue("test-user-value");
        tokenUser.setTokenDate(new Date());
        idmIdentityService.saveToken(tokenUser);

        Token tokenAdmin = idmIdentityService.newToken("admin");
        tokenAdmin.setUserId("test-admin");
        tokenAdmin.setTokenValue("test-admin-value");
        tokenAdmin.setTokenDate(new Date());
        idmIdentityService.saveToken(tokenAdmin);

        Token tokenIdm = idmIdentityService.newToken("idm");
        tokenIdm.setUserId("test-idm");
        tokenIdm.setTokenValue("test-idm-value");
        tokenIdm.setTokenDate(new Date());
        idmIdentityService.saveToken(tokenIdm);

        Token tokenRest = idmIdentityService.newToken("rest");
        tokenRest.setUserId("test-rest");
        tokenRest.setTokenValue("test-rest-value");
        tokenRest.setTokenDate(new Date());
        idmIdentityService.saveToken(tokenRest);
    }

    @After
    public void removeTokens() {
        idmIdentityService.createTokenQuery()
            .tokenIds(Arrays.asList("user", "admin", "idm", "rest"))
            .list()
            .forEach(token -> idmIdentityService.deleteToken(token.getId()));

    }

    @Test
    public void nonAuthenticatedUserShouldBeUnauthotized() {
        String authenticateUrl = "http://localhost:" + serverPort + "/flowable-idm/app/rest/admin/groups";
        ResponseEntity<Object> result = restTemplate.getForEntity(authenticateUrl, Object.class);

        assertThat(result.getStatusCode())
            .as("GET App Groups")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void nonIdmUserShouldBeUnauthotized() {
        String authenticateUrl = "http://localhost:" + serverPort + "/flowable-idm/app/rest/admin/groups";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("user", "test-user-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<Object> result = restTemplate.exchange(authenticateUrl, HttpMethod.GET, request, Object.class);

        assertThat(result.getStatusCode())
            .as("GET App Groups")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void adminUserShouldBeUnauthorized() {
        String authenticateUrl = "http://localhost:" + serverPort + "/flowable-idm/app/rest/admin/groups";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("admin", "test-admin-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<Object> result = restTemplate.exchange(authenticateUrl, HttpMethod.GET, request, Object.class);

        assertThat(result.getStatusCode())
            .as("GET App Groups")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void restUserShouldBeUnauthorized() {
        String authenticateUrl = "http://localhost:" + serverPort + "/flowable-idm/app/rest/admin/groups";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("rest", "test-rest-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<Object> result = restTemplate.exchange(authenticateUrl, HttpMethod.GET, request, Object.class);

        assertThat(result.getStatusCode())
            .as("GET App Groups")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void idmUserShouldBeAbleToAccessInternalRestApp() {
        String authenticateUrl = "http://localhost:" + serverPort + "/flowable-idm/app/rest/admin/groups";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("idm", "test-idm-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<Object> result = restTemplate.exchange(authenticateUrl, HttpMethod.GET, request, Object.class);

        assertThat(result.getStatusCode())
            .as("GET App Groups")
            .isEqualTo(HttpStatus.OK);
    }

    @Test
    public void nonAuthenticatedUserShouldNotBeAbleToAccessActuator() {
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-idm/actuator";
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
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization("test-user", "test"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-idm/actuator";
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
    public void nonAuthorizedUserWithCookieShouldNotBeAbleToAccessActuator() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("user", "test-user-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-idm/actuator";
        ResponseEntity<Object> entity = restTemplate.exchange(actuatorUrl, HttpMethod.GET, request, Object.class);

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

            ResponseEntity<Object> endpointResponse = restTemplate.exchange(link.getHref(), HttpMethod.GET, request, Object.class);

            assertThat(endpointResponse.getStatusCode())
                .as("Endpoint '" + endpoint + "' response status")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    public void idmUserShouldNotBeAbleToAccessActuator() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization("test-idm", "test"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-idm/actuator";
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
    public void idmUserWithCookieShouldNotBeAbleToAccessActuator() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("idm", "test-idm-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-idm/actuator";
        ResponseEntity<Object> entity = restTemplate.exchange(actuatorUrl, HttpMethod.GET, request, Object.class);

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

            ResponseEntity<Object> endpointResponse = restTemplate.exchange(link.getHref(), HttpMethod.GET, request, Object.class);

            assertThat(endpointResponse.getStatusCode())
                .as("Endpoint '" + endpoint + "' response status")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    public void restUserShouldNotBeAbleToAccessActuator() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization("test-rest", "test"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-idm/actuator";
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
    public void restUserWithCookieShouldNotBeAbleToAccessActuator() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("rest", "test-rest-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-idm/actuator";
        ResponseEntity<Object> entity = restTemplate.exchange(actuatorUrl, HttpMethod.GET, request, Object.class);

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

            ResponseEntity<Object> endpointResponse = restTemplate.exchange(link.getHref(), HttpMethod.GET, request, Object.class);

            assertThat(endpointResponse.getStatusCode())
                .as("Endpoint '" + endpoint + "' response status")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    public void authorizedUserShouldBeAbleToAccessActuator() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization("test-admin", "test"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-idm/actuator";
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

    @Test
    public void authorizedUserWithCookieShouldNotBeAbleToAccessActuator() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("admin", "test-admin-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-idm/actuator";
        ResponseEntity<Object> entity = restTemplate.exchange(actuatorUrl, HttpMethod.GET, request, Object.class);

        assertThat(entity.getStatusCode())
            .as("GET Actuator response status")
            .isEqualTo(HttpStatus.UNAUTHORIZED);

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
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    public void nonAuthenticatedShouldNotBeAbleToAccessApi() {
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-idm/api/idm/users";

        ResponseEntity<Object> result = restTemplate.getForEntity(processDefinitionsUrl, Object.class);

        assertThat(result.getStatusCode())
            .as("GET IDM Api Users")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void nonAuthorizedShouldNotBeAbleToAccessApi() {
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-idm/api/idm/users";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization("test-user", "test"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<Object> result = restTemplate.exchange(processDefinitionsUrl, HttpMethod.GET, request, Object.class);

        assertThat(result.getStatusCode())
            .as("GET IDM Api Users")
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void nonAuthorizedUserWithCookieShouldNotBeAbleToAccessApi() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("user", "test-user-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-idm/api/idm/users";
        ResponseEntity<Object> result = restTemplate.exchange(processDefinitionsUrl, HttpMethod.GET, request, Object.class);

        assertThat(result.getStatusCode())
            .as("GET IDM Api Users")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void idmUserShouldNotBeAbleToAccessApi() {
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-idm/api/idm/users";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization("test-idm", "test"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<Object> result = restTemplate.exchange(processDefinitionsUrl, HttpMethod.GET, request, Object.class);

        assertThat(result.getStatusCode())
            .as("GET IDM Api Users")
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void idmUserWithCookieShouldNotBeAbleToAccessApi() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("idm", "test-idm-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-idm/api/idm/users";
        ResponseEntity<Object> result = restTemplate.exchange(processDefinitionsUrl, HttpMethod.GET, request, Object.class);

        assertThat(result.getStatusCode())
            .as("GET IDM Api Users")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void adminUserShouldNotBeAbleToAccessApi() {
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-idm/api/idm/users";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization("test-admin", "test"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<Object> result = restTemplate.exchange(processDefinitionsUrl, HttpMethod.GET, request, Object.class);

        assertThat(result.getStatusCode())
            .as("GET IDM Api Users")
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void adminUserWithCookieShouldNotBeAbleToAccessApi() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("admin", "test-admin-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-idm/api/idm/users";
        ResponseEntity<Object> result = restTemplate.exchange(processDefinitionsUrl, HttpMethod.GET, request, Object.class);

        assertThat(result.getStatusCode())
            .as("GET IDM Api Users")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void restUserShouldNotBeAbleToAccessApi() {
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-idm/api/idm/users?filter={filter}";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization("test-rest", "test"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<Object> result = restTemplate.exchange(processDefinitionsUrl, HttpMethod.GET, request, Object.class, "test");

        assertThat(result.getStatusCode())
            .as("GET IDM Api Users")
            .isEqualTo(HttpStatus.OK);
    }

    @Test
    public void restUserWithCookieShouldNotBeAbleToAccessApi() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("rest", "test-rest-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-idm/api/idm/users";
        ResponseEntity<Object> result = restTemplate.exchange(processDefinitionsUrl, HttpMethod.GET, request, Object.class);

        assertThat(result.getStatusCode())
            .as("GET IDM Api Users")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private Map<String, TestLink> getEndpointLinks() {
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-idm/actuator";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization("test-admin", "test"));
        HttpEntity<?> request = new HttpEntity<>(headers);
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

    protected static String authorization(String username, String password) {
        String auth = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }

    protected static String rememberMeCookie(String tokenId, String tokenValue) {
        String auth = tokenId + ":" + tokenValue;
        return CookieConstants.COOKIE_NAME + "=" + Base64.getEncoder().encodeToString(auth.getBytes());
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

    @TestConfiguration
    static class TestBootstrapConfiguration {

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

            User idm = idmIdentityService.newUser("test-idm");
            idm.setPassword("test");
            idmIdentityService.saveUser(idm);

            User rest = idmIdentityService.newUser("test-rest");
            rest.setPassword("test");
            idmIdentityService.saveUser(rest);

            User admin = idmIdentityService.newUser("test-admin");
            admin.setPassword("test");
            idmIdentityService.saveUser(admin);

            Privilege adminAccess = idmIdentityService.createPrivilegeQuery().privilegeName(DefaultPrivileges.ACCESS_ADMIN).singleResult();
            if (adminAccess == null) {
                adminAccess = idmIdentityService.createPrivilege(DefaultPrivileges.ACCESS_ADMIN);
            }

            idmIdentityService.addUserPrivilegeMapping(adminAccess.getId(), "test-admin");

            Privilege idmAccess = idmIdentityService.createPrivilegeQuery().privilegeName(DefaultPrivileges.ACCESS_IDM).singleResult();
            if (idmAccess == null) {
                idmAccess = idmIdentityService.createPrivilege(DefaultPrivileges.ACCESS_IDM);
            }

            idmIdentityService.addUserPrivilegeMapping(idmAccess.getId(), "test-idm");

            Privilege restAccess = idmIdentityService.createPrivilegeQuery().privilegeName(DefaultPrivileges.ACCESS_REST_API).singleResult();
            if (restAccess == null) {
                restAccess = idmIdentityService.createPrivilege(DefaultPrivileges.ACCESS_REST_API);
            }

            idmIdentityService.addUserPrivilegeMapping(restAccess.getId(), "test-rest");
        }
    }
}
