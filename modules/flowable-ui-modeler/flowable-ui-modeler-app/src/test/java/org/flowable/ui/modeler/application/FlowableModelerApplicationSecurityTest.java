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
package org.flowable.ui.modeler.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.flowable.ui.common.model.RemoteToken;
import org.flowable.ui.common.model.RemoteUser;
import org.flowable.ui.common.security.CookieConstants;
import org.flowable.ui.common.security.DefaultPrivileges;
import org.flowable.ui.common.service.idm.RemoteIdmService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
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
public class FlowableModelerApplicationSecurityTest {

    private static final Set<String> ACTUATOR_LINKS = new HashSet<>(
        Arrays.asList(
            "self",
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
            "mappings",
            "caches",
            "caches-cache",
            "health-component",
            "health-component-instance"
        )
    );

    @LocalServerPort
    private int serverPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private RemoteIdmService remoteIdmService;

    private Map<String, RemoteToken> tokens = new HashMap<>();
    private Map<String, RemoteUser> users = new HashMap<>();

    @Before
    public void setUp() {
        when(remoteIdmService.getToken(anyString()))
            .thenAnswer((Answer<RemoteToken>) invocation -> tokens.get(invocation.<String>getArgument(0)));

        when(remoteIdmService.getUser(anyString()))
            .thenAnswer((Answer<RemoteUser>) invocation -> users.get(invocation.<String>getArgument(0)));

        when(remoteIdmService.authenticateUser(anyString(), eq("test")))
            .thenAnswer((Answer<RemoteUser>) invocation -> users.get(invocation.<String>getArgument(0)));

        RemoteUser testUser = new RemoteUser();
        testUser.setId("test-user");
        testUser.setPrivileges(Collections.emptyList());
        users.put("test-user", testUser);

        RemoteUser testAdmin = new RemoteUser();
        testAdmin.setId("test-admin");
        testAdmin.setPrivileges(Collections.singletonList(DefaultPrivileges.ACCESS_ADMIN));
        users.put("test-admin", testAdmin);

        RemoteUser testModeler = new RemoteUser();
        testModeler.setId("test-modeler");
        testModeler.setPrivileges(Collections.singletonList(DefaultPrivileges.ACCESS_MODELER));
        users.put("test-modeler", testModeler);

        RemoteUser testRest = new RemoteUser();
        testRest.setId("test-rest");
        testRest.setPrivileges(Collections.singletonList(DefaultPrivileges.ACCESS_REST_API));
        users.put("test-rest", testRest);

        RemoteToken tokenUser = new RemoteToken();
        tokenUser.setId("user");
        tokenUser.setUserId("test-user");
        tokenUser.setValue("test-user-value");
        tokens.put("user", tokenUser);

        RemoteToken tokenAdmin = new RemoteToken();
        tokenAdmin.setId("admin");
        tokenAdmin.setUserId("test-admin");
        tokenAdmin.setValue("test-admin-value");
        tokens.put("admin", tokenAdmin);

        RemoteToken tokenModeler = new RemoteToken();
        tokenModeler.setId("modeler");
        tokenModeler.setUserId("test-modeler");
        tokenModeler.setValue("test-modeler-value");
        tokens.put("modeler", tokenModeler);

        RemoteToken tokenRest = new RemoteToken();
        tokenRest.setId("rest");
        tokenRest.setUserId("test-rest");
        tokenRest.setValue("test-rest-value");
        tokens.put("rest", tokenRest);
    }

    @Test
    public void nonAuthenticatedUserShouldBeRedirectedToIdm() {
        String stencilsUrl = "http://localhost:" + serverPort + "/flowable-modeler/app/rest/stencil-sets/editor";
        ResponseEntity<Object> result = restTemplate.getForEntity(stencilsUrl, Object.class);

        assertThat(result.getStatusCode())
            .as("GET editor stencil-sets")
            .isEqualTo(HttpStatus.FOUND);

        assertThat(result.getHeaders().getFirst(HttpHeaders.LOCATION))
            .as("redirect location")
            .isEqualTo("http://localhost:8080/flowable-idm/#/login?redirectOnAuthSuccess=true&redirectUrl=" + stencilsUrl);
    }

    @Test
    public void nonAdminUserShouldBeRedirectedToIdm() {
        String stencilsUrl = "http://localhost:" + serverPort + "/flowable-modeler/app/rest/stencil-sets/editor";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("user", "test-user-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<Object> result = restTemplate.exchange(stencilsUrl, HttpMethod.GET, request, Object.class);

        assertThat(result.getStatusCode())
            .as("GET editor stencil-sets")
            .isEqualTo(HttpStatus.FOUND);

        assertThat(result.getHeaders().getFirst(HttpHeaders.LOCATION))
            .as("redirect location")
            .isEqualTo("http://localhost:8080/flowable-idm/#/login?redirectOnAuthSuccess=true&redirectUrl=" + stencilsUrl);
    }

    @Test
    public void adminUserShouldBeRedirectedToIdm() {
        String stencilsUrl = "http://localhost:" + serverPort + "/flowable-modeler/app/rest/stencil-sets/editor";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("admin", "test-admin-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<Object> result = restTemplate.exchange(stencilsUrl, HttpMethod.GET, request, Object.class);

        assertThat(result.getStatusCode())
            .as("GET editor stencil-sets")
            .isEqualTo(HttpStatus.FOUND);

        assertThat(result.getHeaders().getFirst(HttpHeaders.LOCATION))
            .as("redirect location")
            .isEqualTo("http://localhost:8080/flowable-idm/#/login?redirectOnAuthSuccess=true&redirectUrl=" + stencilsUrl);
    }

    @Test
    public void restUserShouldBeRedirectedToIdm() {
        String stencilsUrl = "http://localhost:" + serverPort + "/flowable-modeler/app/rest/stencil-sets/editor";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("rest", "test-rest-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<Object> result = restTemplate.exchange(stencilsUrl, HttpMethod.GET, request, Object.class);

        assertThat(result.getStatusCode())
            .as("GET editor stencil-sets")
            .isEqualTo(HttpStatus.FOUND);

        assertThat(result.getHeaders().getFirst(HttpHeaders.LOCATION))
            .as("redirect location")
            .isEqualTo("http://localhost:8080/flowable-idm/#/login?redirectOnAuthSuccess=true&redirectUrl=" + stencilsUrl);
    }

    @Test
    public void modelerUserShouldBeAbleToAccessStencilSets() {
        String stencilsUrl = "http://localhost:" + serverPort + "/flowable-modeler/app/rest/stencil-sets/editor";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("modeler", "test-modeler-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<Object> result = restTemplate.exchange(stencilsUrl, HttpMethod.GET, request, Object.class);

        assertThat(result.getStatusCode())
            .as("GET editor stencil-sets")
            .isEqualTo(HttpStatus.OK);
    }

    @Test
    public void nonAuthenticatedUserShouldNotBeAbleToAccessActuator() {
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-modeler/actuator";
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
        RemoteUser user = users.get("test-user");
        assertThat(user).as("test-user").isNotNull();
        assertThat(user.getPrivileges())
            .as("test-user privileges")
            .doesNotContain(DefaultPrivileges.ACCESS_ADMIN);

        RemoteToken token = tokens.get("user");

        assertThat(token)
            .as("test-user token")
            .isNotNull();
        assertThat(token.getUserId())
            .as("test-user token user id")
            .isEqualTo("test-user");
        assertThat(token.getId())
            .as("test-user token id")
            .isEqualTo("user");
        assertThat(token.getValue())
            .as("test-user token value")
            .isEqualTo("test-user-value");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization("test-user", "test"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-modeler/actuator";
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
        RemoteUser user = users.get("test-user");
        assertThat(user).as("test-user").isNotNull();
        assertThat(user.getPrivileges())
            .as("test-user privileges")
            .doesNotContain(DefaultPrivileges.ACCESS_ADMIN);

        RemoteToken token = tokens.get("user");

        assertThat(token)
            .as("test-user token")
            .isNotNull();
        assertThat(token.getUserId())
            .as("test-user token user id")
            .isEqualTo("test-user");
        assertThat(token.getId())
            .as("test-user token id")
            .isEqualTo("user");
        assertThat(token.getValue())
            .as("test-user token value")
            .isEqualTo("test-user-value");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("user", "test-user-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-modeler/actuator";
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
    public void modelerUserShouldNotBeAbleToAccessActuator() {
        RemoteUser user = users.get("test-modeler");
        assertThat(user).as("test-modeler").isNotNull();
        assertThat(user.getPrivileges())
            .as("test-modeler privileges")
            .contains(DefaultPrivileges.ACCESS_MODELER)
            .doesNotContain(DefaultPrivileges.ACCESS_ADMIN);

        RemoteToken token = tokens.get("modeler");

        assertThat(token)
            .as("test-modeler token")
            .isNotNull();
        assertThat(token.getUserId())
            .as("test-modeler token user id")
            .isEqualTo("test-modeler");
        assertThat(token.getId())
            .as("test-modeler token id")
            .isEqualTo("modeler");
        assertThat(token.getValue())
            .as("test-modeler token value")
            .isEqualTo("test-modeler-value");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization("test-modeler", "test"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-modeler/actuator";
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
    public void modelerUserWithCookieShouldNotBeAbleToAccessActuator() {
        RemoteUser user = users.get("test-modeler");
        assertThat(user).as("test-modeler").isNotNull();
        assertThat(user.getPrivileges())
            .as("test-modeler privileges")
            .contains(DefaultPrivileges.ACCESS_MODELER)
            .doesNotContain(DefaultPrivileges.ACCESS_ADMIN);

        RemoteToken token = tokens.get("modeler");

        assertThat(token)
            .as("test-modeler token")
            .isNotNull();
        assertThat(token.getUserId())
            .as("test-modeler token user id")
            .isEqualTo("test-modeler");
        assertThat(token.getId())
            .as("test-modeler token id")
            .isEqualTo("modeler");
        assertThat(token.getValue())
            .as("test-modeler token value")
            .isEqualTo("test-modeler-value");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("modeler", "test-modeler-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-modeler/actuator";
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
        RemoteUser user = users.get("test-rest");
        assertThat(user).as("test-rest").isNotNull();
        assertThat(user.getPrivileges())
            .as("test-rest privileges")
            .contains(DefaultPrivileges.ACCESS_REST_API)
            .doesNotContain(DefaultPrivileges.ACCESS_ADMIN, DefaultPrivileges.ACCESS_MODELER);

        RemoteToken token = tokens.get("rest");

        assertThat(token)
            .as("test-rest token")
            .isNotNull();
        assertThat(token.getUserId())
            .as("test-rest token user id")
            .isEqualTo("test-rest");
        assertThat(token.getId())
            .as("test-rest token id")
            .isEqualTo("rest");
        assertThat(token.getValue())
            .as("test-rest token value")
            .isEqualTo("test-rest-value");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization("test-rest", "test"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-modeler/actuator";
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
        RemoteUser user = users.get("test-rest");
        assertThat(user).as("test-rest").isNotNull();
        assertThat(user.getPrivileges())
            .as("test-rest privileges")
            .contains(DefaultPrivileges.ACCESS_REST_API)
            .doesNotContain(DefaultPrivileges.ACCESS_ADMIN, DefaultPrivileges.ACCESS_MODELER);

        RemoteToken token = tokens.get("rest");

        assertThat(token)
            .as("test-rest token")
            .isNotNull();
        assertThat(token.getUserId())
            .as("test-rest token user id")
            .isEqualTo("test-rest");
        assertThat(token.getId())
            .as("test-rest token id")
            .isEqualTo("rest");
        assertThat(token.getValue())
            .as("test-rest token value")
            .isEqualTo("test-rest-value");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("rest", "test-rest-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-modeler/actuator";
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
        RemoteUser user = users.get("test-admin");
        assertThat(user).as("test-admin").isNotNull();
        assertThat(user.getPrivileges())
            .as("test-admin privileges")
            .contains(DefaultPrivileges.ACCESS_ADMIN);

        RemoteToken token = tokens.get("admin");

        assertThat(token)
            .as("test-admin token")
            .isNotNull();
        assertThat(token.getUserId())
            .as("test-admin token user id")
            .isEqualTo("test-admin");
        assertThat(token.getId())
            .as("test-admin token id")
            .isEqualTo("admin");
        assertThat(token.getValue())
            .as("test-admin token value")
            .isEqualTo("test-admin-value");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization("test-admin", "test"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-modeler/actuator";
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
        RemoteUser user = users.get("test-admin");
        assertThat(user).as("test-admin").isNotNull();
        assertThat(user.getPrivileges())
            .as("test-admin privileges")
            .contains(DefaultPrivileges.ACCESS_ADMIN);

        RemoteToken token = tokens.get("admin");

        assertThat(token)
            .as("test-admin token")
            .isNotNull();
        assertThat(token.getUserId())
            .as("test-admin token user id")
            .isEqualTo("test-admin");
        assertThat(token.getId())
            .as("test-admin token id")
            .isEqualTo("admin");
        assertThat(token.getValue())
            .as("test-admin token value")
            .isEqualTo("test-admin-value");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("admin", "test-admin-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-modeler/actuator";
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
        String apiModelsUrl = "http://localhost:" + serverPort + "/flowable-modeler/api/editor/models";

        ResponseEntity<Object> result = restTemplate.getForEntity(apiModelsUrl, Object.class);

        assertThat(result.getStatusCode())
            .as("GET API Editor models")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void nonAuthorizedShouldNotBeAbleToAccessApi() {
        RemoteUser user = users.get("test-user");
        assertThat(user).as("test-user").isNotNull();
        assertThat(user.getPrivileges())
            .as("test-user privileges")
            .doesNotContain(DefaultPrivileges.ACCESS_ADMIN, DefaultPrivileges.ACCESS_REST_API);

        RemoteToken token = tokens.get("user");

        assertThat(token)
            .as("test-user token")
            .isNotNull();
        assertThat(token.getUserId())
            .as("test-user token user id")
            .isEqualTo("test-user");
        assertThat(token.getId())
            .as("test-user token id")
            .isEqualTo("user");
        assertThat(token.getValue())
            .as("test-user token value")
            .isEqualTo("test-user-value");

        String apiModelsUrl = "http://localhost:" + serverPort + "/flowable-modeler/api/editor/models";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization("test-user", "test"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<Object> result = restTemplate.exchange(apiModelsUrl, HttpMethod.GET, request, Object.class);

        assertThat(result.getStatusCode())
            .as("GET API Editor models")
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void nonAuthorizedUserWithCookieShouldNotBeAbleToAccessApi() {
        RemoteUser user = users.get("test-user");
        assertThat(user).as("test-user").isNotNull();
        assertThat(user.getPrivileges())
            .as("test-user privileges")
            .doesNotContain(DefaultPrivileges.ACCESS_ADMIN, DefaultPrivileges.ACCESS_REST_API);

        RemoteToken token = tokens.get("user");

        assertThat(token)
            .as("test-user token")
            .isNotNull();
        assertThat(token.getUserId())
            .as("test-user token user id")
            .isEqualTo("test-user");
        assertThat(token.getId())
            .as("test-user token id")
            .isEqualTo("user");
        assertThat(token.getValue())
            .as("test-user token value")
            .isEqualTo("test-user-value");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("user", "test-user-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String apiModelsUrl = "http://localhost:" + serverPort + "/flowable-modeler/api/editor/models";
        ResponseEntity<Object> result = restTemplate.exchange(apiModelsUrl, HttpMethod.GET, request, Object.class);

        assertThat(result.getStatusCode())
            .as("GET API Editor models")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void modelerUserShouldNotBeAbleToAccessApi() {
        RemoteUser user = users.get("test-modeler");
        assertThat(user).as("test-modeler").isNotNull();
        assertThat(user.getPrivileges())
            .as("test-modeler privileges")
            .contains(DefaultPrivileges.ACCESS_MODELER)
            .doesNotContain(DefaultPrivileges.ACCESS_REST_API);

        RemoteToken token = tokens.get("modeler");

        assertThat(token)
            .as("test-modeler token")
            .isNotNull();
        assertThat(token.getUserId())
            .as("test-modeler token modeler id")
            .isEqualTo("test-modeler");
        assertThat(token.getId())
            .as("test-modeler token id")
            .isEqualTo("modeler");
        assertThat(token.getValue())
            .as("test-modeler token value")
            .isEqualTo("test-modeler-value");

        String apiModelsUrl = "http://localhost:" + serverPort + "/flowable-modeler/api/editor/models";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization("test-modeler", "test"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<Object> result = restTemplate.exchange(apiModelsUrl, HttpMethod.GET, request, Object.class);

        assertThat(result.getStatusCode())
            .as("GET API Editor models")
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void modelerUserWithCookieShouldNotBeAbleToAccessApi() {
        RemoteUser user = users.get("test-modeler");
        assertThat(user).as("test-modeler").isNotNull();
        assertThat(user.getPrivileges())
            .as("test-modeler privileges")
            .contains(DefaultPrivileges.ACCESS_MODELER)
            .doesNotContain(DefaultPrivileges.ACCESS_REST_API);

        RemoteToken token = tokens.get("modeler");

        assertThat(token)
            .as("test-modeler token")
            .isNotNull();
        assertThat(token.getUserId())
            .as("test-modeler token modeler id")
            .isEqualTo("test-modeler");
        assertThat(token.getId())
            .as("test-modeler token id")
            .isEqualTo("modeler");
        assertThat(token.getValue())
            .as("test-modeler token value")
            .isEqualTo("test-modeler-value");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("modeler", "test-modeler-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String apiModelsUrl = "http://localhost:" + serverPort + "/flowable-modeler/api/editor/models";
        ResponseEntity<Object> result = restTemplate.exchange(apiModelsUrl, HttpMethod.GET, request, Object.class);

        assertThat(result.getStatusCode())
            .as("GET API Editor models")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void adminUserShouldNotBeAbleToAccessApi() {
        RemoteUser user = users.get("test-admin");
        assertThat(user).as("test-admin").isNotNull();
        assertThat(user.getPrivileges())
            .as("test-admin privileges")
            .contains(DefaultPrivileges.ACCESS_ADMIN)
            .doesNotContain(DefaultPrivileges.ACCESS_REST_API);

        RemoteToken token = tokens.get("admin");

        assertThat(token)
            .as("test-admin token")
            .isNotNull();
        assertThat(token.getUserId())
            .as("test-admin token admin id")
            .isEqualTo("test-admin");
        assertThat(token.getId())
            .as("test-admin token id")
            .isEqualTo("admin");
        assertThat(token.getValue())
            .as("test-admin token value")
            .isEqualTo("test-admin-value");

        String apiModelsUrl = "http://localhost:" + serverPort + "/flowable-modeler/api/editor/models";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization("test-admin", "test"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<Object> result = restTemplate.exchange(apiModelsUrl, HttpMethod.GET, request, Object.class);

        assertThat(result.getStatusCode())
            .as("GET API Editor models")
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void adminUserWithCookieShouldNotBeAbleToAccessApi() {
        RemoteUser user = users.get("test-admin");
        assertThat(user).as("test-admin").isNotNull();
        assertThat(user.getPrivileges())
            .as("test-admin privileges")
            .contains(DefaultPrivileges.ACCESS_ADMIN)
            .doesNotContain(DefaultPrivileges.ACCESS_REST_API);

        RemoteToken token = tokens.get("admin");

        assertThat(token)
            .as("test-admin token")
            .isNotNull();
        assertThat(token.getUserId())
            .as("test-admin token admin id")
            .isEqualTo("test-admin");
        assertThat(token.getId())
            .as("test-admin token id")
            .isEqualTo("admin");
        assertThat(token.getValue())
            .as("test-admin token value")
            .isEqualTo("test-admin-value");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("admin", "test-admin-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String apiModelsUrl = "http://localhost:" + serverPort + "/flowable-modeler/api/editor/models";
        ResponseEntity<Object> result = restTemplate.exchange(apiModelsUrl, HttpMethod.GET, request, Object.class);

        assertThat(result.getStatusCode())
            .as("GET API Editor models")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void restUserShouldNotBeAbleToAccessApi() {
        RemoteUser user = users.get("test-rest");
        assertThat(user).as("test-rest").isNotNull();
        assertThat(user.getPrivileges())
            .as("test-rest privileges")
            .contains(DefaultPrivileges.ACCESS_REST_API);

        RemoteToken token = tokens.get("rest");

        assertThat(token)
            .as("test-rest token")
            .isNotNull();
        assertThat(token.getUserId())
            .as("test-rest token rest id")
            .isEqualTo("test-rest");
        assertThat(token.getId())
            .as("test-rest token id")
            .isEqualTo("rest");
        assertThat(token.getValue())
            .as("test-rest token value")
            .isEqualTo("test-rest-value");

        String apiModelsUrl = "http://localhost:" + serverPort + "/flowable-modeler/api/editor/models";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization("test-rest", "test"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<Object> result = restTemplate.exchange(apiModelsUrl, HttpMethod.GET, request, Object.class);

        assertThat(result.getStatusCode())
            .as("GET API Editor models")
            .isEqualTo(HttpStatus.OK);
    }

    @Test
    public void restUserWithCookieShouldNotBeAbleToAccessApi() {
        RemoteUser user = users.get("test-rest");
        assertThat(user).as("test-rest").isNotNull();
        assertThat(user.getPrivileges())
            .as("test-rest privileges")
            .contains(DefaultPrivileges.ACCESS_REST_API);

        RemoteToken token = tokens.get("rest");

        assertThat(token)
            .as("test-rest token")
            .isNotNull();
        assertThat(token.getUserId())
            .as("test-rest token rest id")
            .isEqualTo("test-rest");
        assertThat(token.getId())
            .as("test-rest token id")
            .isEqualTo("rest");
        assertThat(token.getValue())
            .as("test-rest token value")
            .isEqualTo("test-rest-value");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("rest", "test-rest-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String apiModelsUrl = "http://localhost:" + serverPort + "/flowable-modeler/api/editor/models";
        ResponseEntity<Object> result = restTemplate.exchange(apiModelsUrl, HttpMethod.GET, request, Object.class);

        assertThat(result.getStatusCode())
            .as("GET API Editor models")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private Map<String, TestLink> getEndpointLinks() {
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-modeler/actuator";
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
}
