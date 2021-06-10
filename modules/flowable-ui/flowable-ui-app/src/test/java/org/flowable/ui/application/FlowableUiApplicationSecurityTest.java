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
package org.flowable.ui.application;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.Token;
import org.flowable.idm.api.User;
import org.flowable.ui.common.security.CookieConstants;
import org.flowable.ui.common.security.DefaultPrivileges;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import net.javacrumbs.jsonunit.core.Option;

/**
 * @author Filip Hrisafov
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebClient(registerRestTemplate = true)
public class FlowableUiApplicationSecurityTest {

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
            "flowable",
            "mappings",
            "caches",
            "caches-cache",
            "health-path",
            "configprops-prefix"
        )
    );

    @LocalServerPort
    private int serverPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private IdmIdentityService identityService;

    @BeforeEach
    public void setUp() {
        User testUser = identityService.newUser("test-user");
        testUser.setPassword("test");
        identityService.saveUser(testUser);

        User testAdmin = identityService.newUser("test-admin");
        testAdmin.setPassword("test");
        identityService.saveUser(testAdmin);
        Privilege accessAdminPrivilege = identityService.createPrivilegeQuery().privilegeName(DefaultPrivileges.ACCESS_ADMIN).singleResult();
        identityService.addUserPrivilegeMapping(accessAdminPrivilege.getId(), "test-admin");

        User testTask = identityService.newUser("test-task");
        testTask.setPassword("test");
        identityService.saveUser(testTask);
        Privilege accessTaskPrivilege = identityService.createPrivilegeQuery().privilegeName(DefaultPrivileges.ACCESS_TASK).singleResult();
        identityService.addUserPrivilegeMapping(accessTaskPrivilege.getId(), "test-task");

        User testModeler = identityService.newUser("test-modeler");
        testModeler.setPassword("test");
        identityService.saveUser(testModeler);
        Privilege accessModelerPrivilege = identityService.createPrivilegeQuery().privilegeName(DefaultPrivileges.ACCESS_MODELER).singleResult();
        identityService.addUserPrivilegeMapping(accessModelerPrivilege.getId(), "test-modeler");

        User testIdm = identityService.newUser("test-idm");
        testIdm.setPassword("test");
        identityService.saveUser(testIdm);
        Privilege accessIdmPrivilege = identityService.createPrivilegeQuery().privilegeName(DefaultPrivileges.ACCESS_IDM).singleResult();
        identityService.addUserPrivilegeMapping(accessIdmPrivilege.getId(), "test-idm");

        User testRest = identityService.newUser("test-rest");
        testRest.setPassword("test");
        identityService.saveUser(testRest);
        Privilege accessRestPrivilege = identityService.createPrivilegeQuery().privilegeName(DefaultPrivileges.ACCESS_REST_API).singleResult();
        identityService.addUserPrivilegeMapping(accessRestPrivilege.getId(), "test-rest");

        Token tokenUser = identityService.newToken("user");
        tokenUser.setUserId("test-user");
        tokenUser.setTokenValue("test-user-value");
        tokenUser.setTokenDate(new Date());
        identityService.saveToken(tokenUser);

        Token tokenAdmin = identityService.newToken("admin");
        tokenAdmin.setUserId("test-admin");
        tokenAdmin.setTokenValue("test-admin-value");
        tokenAdmin.setTokenDate(new Date());
        identityService.saveToken(tokenAdmin);

        Token tokenTask = identityService.newToken("task");
        tokenTask.setUserId("test-task");
        tokenTask.setTokenValue("test-task-value");
        tokenTask.setTokenDate(new Date());
        identityService.saveToken(tokenTask);

        Token tokenModeler = identityService.newToken("modeler");
        tokenModeler.setUserId("test-modeler");
        tokenModeler.setTokenValue("test-modeler-value");
        tokenModeler.setTokenDate(new Date());
        identityService.saveToken(tokenModeler);

        Token tokenIdm = identityService.newToken("idm");
        tokenIdm.setUserId("test-idm");
        tokenIdm.setTokenValue("test-idm-value");
        tokenIdm.setTokenDate(new Date());
        identityService.saveToken(tokenIdm);

        Token tokenRest = identityService.newToken("rest");
        tokenRest.setUserId("test-rest");
        tokenRest.setTokenValue("test-rest-value");
        tokenRest.setTokenDate(new Date());
        identityService.saveToken(tokenRest);
    }

    @AfterEach
    public void tearDown() {
        identityService.deleteUser("test-user");
        identityService.deleteToken("user");
        identityService.deleteUser("test-admin");
        identityService.deleteToken("admin");
        identityService.deleteUser("test-task");
        identityService.deleteToken("task");
        identityService.deleteUser("test-modeler");
        identityService.deleteToken("modeler");
        identityService.deleteUser("test-idm");
        identityService.deleteToken("idm");
        identityService.deleteUser("test-rest");
        identityService.deleteToken("rest");

    }

    @Test
    public void nonAuthenticatedUserShouldBeRedirectedToLogin() {
        String rootUrl = "http://localhost:" + serverPort + "/flowable-ui/";
        ResponseEntity<String> result = restTemplate.getForEntity(rootUrl, String.class);

        assertThat(result.getStatusCode())
                .as("GET app definitions")
                .isEqualTo(HttpStatus.FOUND);

        assertThat(result.getHeaders().getFirst(HttpHeaders.LOCATION))
                .as("redirect location")
                .isEqualTo("http://localhost:" + serverPort + "/flowable-ui/idm/#/login");
    }

    @Test
    public void nonAuthenticatedUserShouldBeForbiddenToAccessAppDefinitions() {
        String appDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-ui/app/rest/runtime/app-definitions";
        ResponseEntity<Object> result = restTemplate.getForEntity(appDefinitionsUrl, Object.class);

        assertThat(result.getStatusCode())
                .as("GET app definitions")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void nonTaskUserShouldBeForbiddenToAccessCaseDefinitions() {
        String caseDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-ui/app/rest/case-definitions";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("user", "test-user-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(caseDefinitionsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET case definitions")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void nonTaskUserShouldBeForbiddenToAccessStencilSets() {
        String stencilsUrl = "http://localhost:" + serverPort + "/flowable-ui/modeler-app/rest/stencil-sets/editor";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("user", "test-user-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(stencilsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET Stencils")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void nonTaskUserShouldBeForbiddenToAccessIdmGroups() {
        String idmGroupsUrl = "http://localhost:" + serverPort + "/flowable-ui/idm-app/rest/admin/groups";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("user", "test-user-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(idmGroupsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET Stencils")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void nonTaskUserShouldBeForbiddenToAccessServiceConfigs() {
        String configsUrl = "http://localhost:" + serverPort + "/flowable-ui/admin-app/rest/server-configs";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("user", "test-user-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(configsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET server-configs")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void nonTaskUserShouldHaveNoAppDefinitions() {
        String appDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-ui/app/rest/runtime/app-definitions";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("user", "test-user-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(appDefinitionsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET app definitions")
                .isEqualTo(HttpStatus.OK);

        String resultBody = result.getBody();
        assertThat(resultBody).isNotNull();
        assertThatJson(resultBody)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{ data: [ ] }");
    }

    @Test
    public void adminUserShouldBeForbiddenToAccessCaseDefinitions() {
        String caseDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-ui/app/rest/case-definitions";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("admin", "test-admin-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(caseDefinitionsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET case definitions")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void adminUserShouldBeForbiddenToAccessStencilSets() {
        String stencilsUrl = "http://localhost:" + serverPort + "/flowable-ui/modeler-app/rest/stencil-sets/editor";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("admin", "test-admin-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(stencilsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET Stencils")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void adminUserShouldBeForbiddenToAccessIdmGroups() {
        String idmGroupsUrl = "http://localhost:" + serverPort + "/flowable-ui/idm-app/rest/admin/groups";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("admin", "test-admin-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(idmGroupsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET Stencils")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void adminUserShouldBeAbleToAccessServiceConfigs() {
        String configsUrl = "http://localhost:" + serverPort + "/flowable-ui/admin-app/rest/server-configs";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("admin", "test-admin-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(configsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET server-configs")
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    public void adminUserShouldBeAllowedToGetAppDefinitions() {
        String appDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-ui/app/rest/runtime/app-definitions";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("admin", "test-admin-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(appDefinitionsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET app definitions")
                .isEqualTo(HttpStatus.OK);

        Object resultBody = result.getBody();
        assertThat(resultBody).isNotNull();
        assertThatJson(resultBody)
                .isEqualTo("{"
                        + "  size: 1,"
                        + "  total: 1,"
                        + "  start: 0,"
                        + "  data: ["
                        + "    { "
                        + "      defaultAppId: 'admin', name: null, description: null, theme: null, icon: null, appDefinitionId: null,"
                        + "      appDefinitionKey: null, tenantId: null, usersAccess: null, groupsAccess: null"
                        + "    }"
                        + "  ]"
                        + "}");
    }

    @Test
    public void taskUserShouldBeAbleToAccessCaseDefinitions() {
        String caseDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-ui/app/rest/case-definitions";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("task", "test-task-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(caseDefinitionsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET case definitions")
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    public void taskUserShouldBeForbiddenToAccessStencilSets() {
        String stencilsUrl = "http://localhost:" + serverPort + "/flowable-ui/modeler-app/rest/stencil-sets/editor";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("task", "test-task-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(stencilsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET Stencils")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void taskUserShouldBeForbiddenToAccessIdmGroups() {
        String idmGroupsUrl = "http://localhost:" + serverPort + "/flowable-ui/idm-app/rest/admin/groups";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("task", "test-task-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(idmGroupsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET Stencils")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void taskUserShouldBeForbiddenToAccessServiceConfigs() {
        String configsUrl = "http://localhost:" + serverPort + "/flowable-ui/admin-app/rest/server-configs";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("task", "test-task-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(configsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET server-configs")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void taskUserShouldBeAllowedToGetAppDefinitions() {
        String appDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-ui/app/rest/runtime/app-definitions";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("task", "test-task-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(appDefinitionsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET app definitions")
                .isEqualTo(HttpStatus.OK);

        Object resultBody = result.getBody();
        assertThat(resultBody).isNotNull();
        assertThatJson(resultBody)
                .isEqualTo("{"
                        + "  size: 1,"
                        + "  total: 1,"
                        + "  start: 0,"
                        + "  data: ["
                        + "    { "
                        + "      defaultAppId: 'tasks', name: null, description: null, theme: null, icon: null, appDefinitionId: null,"
                        + "      appDefinitionKey: null, tenantId: null, usersAccess: null, groupsAccess: null"
                        + "    }"
                        + "  ]"
                        + "}");
    }

    @Test
    public void modelerUserShouldBeForbiddenToAccessCaseDefinitions() {
        String caseDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-ui/app/rest/case-definitions";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("modeler", "test-modeler-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(caseDefinitionsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET case definitions")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }


    @Test
    public void modelerUserShouldBeAbleToAccessStencilSets() {
        String stencilsUrl = "http://localhost:" + serverPort + "/flowable-ui/modeler-app/rest/stencil-sets/editor";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("modeler", "test-modeler-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(stencilsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET editor stencil-sets")
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    public void modelerUserShouldBeForbiddenToAccessIdmGroups() {
        String idmGroupsUrl = "http://localhost:" + serverPort + "/flowable-ui/idm-app/rest/admin/groups";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("modeler", "test-modeler-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(idmGroupsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET Stencils")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void modelerUserShouldBeForbiddenToAccessServiceConfigs() {
        String configsUrl = "http://localhost:" + serverPort + "/flowable-ui/admin-app/rest/server-configs";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("modeler", "test-modeler-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(configsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET server-configs")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void modelerUserShouldBeAllowedToGetAppDefinitions() {
        String appDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-ui/app/rest/runtime/app-definitions";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("modeler", "test-modeler-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(appDefinitionsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET app definitions")
                .isEqualTo(HttpStatus.OK);

        Object resultBody = result.getBody();
        assertThat(resultBody).isNotNull();
        assertThatJson(resultBody)
                .isEqualTo("{"
                        + "  size: 1,"
                        + "  total: 1,"
                        + "  start: 0,"
                        + "  data: ["
                        + "    { "
                        + "      defaultAppId: 'modeler', name: null, description: null, theme: null, icon: null, appDefinitionId: null,"
                        + "      appDefinitionKey: null, tenantId: null, usersAccess: null, groupsAccess: null"
                        + "    }"
                        + "  ]"
                        + "}");
    }

    @Test
    public void idmUserShouldBeForbiddenToAccessCaseDefinitions() {
        String caseDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-ui/app/rest/case-definitions";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("idm", "test-idm-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(caseDefinitionsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET case definitions")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void idmUserShouldBeForbiddenToAccessStencilSets() {
        String stencilsUrl = "http://localhost:" + serverPort + "/flowable-ui/modeler-app/rest/stencil-sets/editor";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("idm", "test-idm-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(stencilsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET Stencils")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void idmUserShouldBeForbiddenToAccessServiceConfigs() {
        String configsUrl = "http://localhost:" + serverPort + "/flowable-ui/admin-app/rest/server-configs";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("idm", "test-idm-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(configsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET server-configs")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void idmUserShouldBeAllowedToGetAppDefinitions() {
        String appDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-ui/app/rest/runtime/app-definitions";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("idm", "test-idm-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(appDefinitionsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET app definitions")
                .isEqualTo(HttpStatus.OK);

        Object resultBody = result.getBody();
        assertThat(resultBody).isNotNull();
        assertThatJson(resultBody)
                .isEqualTo("{"
                        + "  size: 1,"
                        + "  total: 1,"
                        + "  start: 0,"
                        + "  data: ["
                        + "    { "
                        + "      defaultAppId: 'idm', name: null, description: null, theme: null, icon: null, appDefinitionId: null,"
                        + "      appDefinitionKey: null, tenantId: null, usersAccess: null, groupsAccess: null"
                        + "    }"
                        + "  ]"
                        + "}");
    }

    @Test
    public void restUserShouldBeForbiddenToAccessCaseDefinitions() {
        String caseDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-ui/app/rest/case-definitions";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("rest", "test-rest-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(caseDefinitionsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET case definitions")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void restUserShouldHaveNoAppDefinitions() {
        String appDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-ui/app/rest/runtime/app-definitions";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("rest", "test-rest-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(appDefinitionsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET app definitions")
                .isEqualTo(HttpStatus.OK);

        String resultBody = result.getBody();
        assertThat(resultBody).isNotNull();
        assertThatJson(resultBody)
                .when(Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{ data: [ ] }");
    }

    @Test
    public void restUserShouldBeForbiddenToAccessStencilSets() {
        String stencilsUrl = "http://localhost:" + serverPort + "/flowable-ui/modeler-app/rest/stencil-sets/editor";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("rest", "test-rest-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(stencilsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
                .as("GET Stencils")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void taskUserShouldBeAbleToAccessProcessApi() {
        String appDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-ui/app/rest/runtime/app-definitions";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("task", "test-task-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(appDefinitionsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
            .as("GET app definitions")
            .isEqualTo(HttpStatus.OK);
    }

    @Test
    public void nonAuthenticatedUserShouldNotBeAbleToAccessActuator() {
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-ui/actuator";
        ResponseEntity<String> entity = restTemplate.getForEntity(actuatorUrl, String.class);

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

            ResponseEntity<String> endpointResponse = restTemplate.getForEntity(link.getHref(), String.class);

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
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-ui/actuator";
        ResponseEntity<String> entity = restTemplate.exchange(actuatorUrl, HttpMethod.GET, request, String.class);

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

            ResponseEntity<String> endpointResponse = restTemplate.exchange(link.getHref(), HttpMethod.GET, request, String.class);

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
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-ui/actuator";
        ResponseEntity<String> entity = restTemplate.exchange(actuatorUrl, HttpMethod.GET, request, String.class);

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

            ResponseEntity<String> endpointResponse = restTemplate.exchange(link.getHref(), HttpMethod.GET, request, String.class);

            assertThat(endpointResponse.getStatusCode())
                .as("Endpoint '" + endpoint + "' response status")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    public void taskUserShouldNotBeAbleToAccessActuator() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization("test-task", "test"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-ui/actuator";
        ResponseEntity<String> entity = restTemplate.exchange(actuatorUrl, HttpMethod.GET, request, String.class);

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

            ResponseEntity<String> endpointResponse = restTemplate.exchange(link.getHref(), HttpMethod.GET, request, String.class);

            assertThat(endpointResponse.getStatusCode())
                .as("Endpoint '" + endpoint + "' response status")
                .isEqualTo(allowedEndpoints.contains(endpoint) ? HttpStatus.OK : HttpStatus.FORBIDDEN);
        }
    }

    @Test
    public void taskUserWithCookieShouldNotBeAbleToAccessActuator() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("task", "test-task-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-ui/actuator";
        ResponseEntity<String> entity = restTemplate.exchange(actuatorUrl, HttpMethod.GET, request, String.class);

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

            ResponseEntity<String> endpointResponse = restTemplate.exchange(link.getHref(), HttpMethod.GET, request, String.class);

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
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-ui/actuator";
        ResponseEntity<String> entity = restTemplate.exchange(actuatorUrl, HttpMethod.GET, request, String.class);

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

            ResponseEntity<String> endpointResponse = restTemplate.exchange(link.getHref(), HttpMethod.GET, request, String.class);

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
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-ui/actuator";
        ResponseEntity<String> entity = restTemplate.exchange(actuatorUrl, HttpMethod.GET, request, String.class);

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

            ResponseEntity<String> endpointResponse = restTemplate.exchange(link.getHref(), HttpMethod.GET, request, String.class);

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
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-ui/actuator";
        ResponseEntity<String> entity = restTemplate.exchange(actuatorUrl, HttpMethod.GET, request, String.class);

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

            ResponseEntity<String> endpointResponse = restTemplate.exchange(link.getHref(), HttpMethod.GET, request, String.class);

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
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-ui/actuator";
        ResponseEntity<String> entity = restTemplate.exchange(actuatorUrl, HttpMethod.GET, request, String.class);

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

            ResponseEntity<String> endpointResponse = restTemplate.exchange(link.getHref(), HttpMethod.GET, request, String.class);

            assertThat(endpointResponse.getStatusCode())
                .as("Endpoint '" + endpoint + "' response status")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    public void nonAuthenticatedShouldNotBeAbleToAccessApi() {
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-ui/process-api/repository/process-definitions";

        ResponseEntity<String> result = restTemplate.getForEntity(processDefinitionsUrl, String.class);

        assertThat(result.getStatusCode())
            .as("GET API Editor models")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void nonAuthorizedShouldNotBeAbleToAccessApi() {
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-ui/process-api/repository/process-definitions";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization("test-user", "test"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(processDefinitionsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
            .as("GET API Editor models")
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void nonAuthorizedUserWithCookieShouldNotBeAbleToAccessApi() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("user", "test-user-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-ui/process-api/repository/process-definitions";
        ResponseEntity<String> result = restTemplate.exchange(processDefinitionsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
            .as("GET API Editor models")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void taskUserShouldNotBeAbleToAccessApi() {
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-ui/process-api/repository/process-definitions";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization("test-task", "test"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(processDefinitionsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
            .as("GET API Editor models")
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void taskUserWithCookieShouldNotBeAbleToAccessApi() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("task", "test-task-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-ui/process-api/repository/process-definitions";
        ResponseEntity<String> result = restTemplate.exchange(processDefinitionsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
            .as("GET API Editor models")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void adminUserShouldNotBeAbleToAccessApi() {
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-ui/process-api/repository/process-definitions";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization("test-admin", "test"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(processDefinitionsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
            .as("GET API Editor models")
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void adminUserWithCookieShouldNotBeAbleToAccessApi() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("admin", "test-admin-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-ui/process-api/repository/process-definitions";
        ResponseEntity<String> result = restTemplate.exchange(processDefinitionsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
            .as("GET API Editor models")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void restUserShouldNotBeAbleToAccessApi() {
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-ui/process-api/repository/process-definitions";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization("test-rest", "test"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<String> result = restTemplate.exchange(processDefinitionsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
            .as("GET API Editor models")
            .isEqualTo(HttpStatus.OK);
    }

    @Test
    public void restUserWithCookieShouldNotBeAbleToAccessApi() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, rememberMeCookie("rest", "test-rest-value"));
        HttpEntity<?> request = new HttpEntity<>(headers);
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/flowable-ui/process-api/repository/process-definitions";
        ResponseEntity<String> result = restTemplate.exchange(processDefinitionsUrl, HttpMethod.GET, request, String.class);

        assertThat(result.getStatusCode())
            .as("GET API Editor models")
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private Map<String, TestLink> getEndpointLinks() {
        String actuatorUrl = "http://localhost:" + serverPort + "/flowable-ui/actuator";
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
