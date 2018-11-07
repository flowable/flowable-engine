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
package org.flowable.test.spring.boot;

import flowable.Application;

import org.flowable.cmmn.rest.service.api.repository.CaseDefinitionResponse;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.content.rest.service.api.content.ContentItemResponse;
import org.flowable.dmn.rest.service.api.repository.DmnDeploymentResponse;
import org.flowable.rest.service.api.identity.GroupResponse;
import org.flowable.rest.service.api.repository.FormDefinitionResponse;
import org.flowable.rest.service.api.repository.ProcessDefinitionResponse;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Filip Hrisafov
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebClient(registerRestTemplate = true)
public class RestApiSecurityApplicationTest {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private RestTemplate restTemplate;

    @LocalServerPort
    private int serverPort;

    @After
    public void tearDown() {
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler());
    }

    @Test
    public void userDetailsService() {
        assertThat(userDetailsService.loadUserByUsername("jlong"))
            .as("jlong user")
            .isNotNull()
            .satisfies(user -> {
                assertThat(user.getAuthorities())
                    .as("jlong authorities")
                    .hasSize(1)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder("user-privilege");
            });

        assertThat(userDetailsService.loadUserByUsername("jbarrez"))
            .as("jbarrez user")
            .isNotNull()
            .satisfies(user -> {
                assertThat(user.getAuthorities())
                    .as("jbarrez authorities")
                    .hasSize(2)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder("user-privilege", "admin-privilege");
            });
    }

    @Test
    public void testRestApiIntegration() throws InterruptedException {
        String authenticationChallenge = "http://localhost:" + serverPort + "/repository/process-definitions";

        CountDownLatch latch401 = new CountDownLatch(1);
        restTemplate.setErrorHandler(new ResponseErrorHandler() {

            @Override
            public boolean hasError(ClientHttpResponse clientHttpResponse) throws IOException {
                return true;
            }

            @Override
            public void handleError(ClientHttpResponse clientHttpResponse) throws IOException {
                if (clientHttpResponse.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    latch401.countDown();
                }
            }
        });

        ResponseEntity<String> response = restTemplate.getForEntity(authenticationChallenge, String.class);
        latch401.await(500, TimeUnit.MILLISECONDS);
        assertThat(latch401.getCount())
            .as("401 Latch")
            .isEqualTo(0);
    }

    @Test
    public void testRestApiIntegrationWithAuthentication() {
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/process-api/repository/process-definitions";

        HttpEntity<?> request = new HttpEntity<>(createHeaders("jbarrez", "password"));
        ResponseEntity<DataResponse<ProcessDefinitionResponse>> response = restTemplate
            .exchange(processDefinitionsUrl, HttpMethod.GET, request, new ParameterizedTypeReference<DataResponse<ProcessDefinitionResponse>>() {

            });

        assertThat(response.getStatusCode())
            .as("Status code")
            .isEqualTo(HttpStatus.OK);
        DataResponse<ProcessDefinitionResponse> processDefinitions = response.getBody();
        assertThat(processDefinitions).isNotNull();
        assertThat(processDefinitions.getTotal()).isEqualTo(1);
        ProcessDefinitionResponse defResponse = processDefinitions.getData().get(0);
        assertThat(defResponse.getKey()).isEqualTo("dogeProcess");
        assertThat(defResponse.getUrl()).startsWith("http://localhost:" + serverPort + "/process-api/repository/process-definitions/dogeProcess:1:");

    }

    @Test
    public void testCmmnRestApiIntegrationWithAuthentication() {
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/cmmn-api/cmmn-repository/case-definitions";

        HttpEntity<?> request = new HttpEntity<>(createHeaders("filiphr", "password"));
        ResponseEntity<DataResponse<CaseDefinitionResponse>> response = restTemplate
            .exchange(processDefinitionsUrl, HttpMethod.GET, request, new ParameterizedTypeReference<DataResponse<CaseDefinitionResponse>>() {
            });

        assertThat(response.getStatusCode())
            .as("Status code")
            .isEqualTo(HttpStatus.OK);
        DataResponse<CaseDefinitionResponse> caseDefinitions = response.getBody();
        assertThat(caseDefinitions).isNotNull();
        CaseDefinitionResponse defResponse = caseDefinitions.getData().get(0);
        assertThat(defResponse.getKey()).isEqualTo("case1");
        assertThat(defResponse.getUrl()).startsWith("http://localhost:" + serverPort + "/cmmn-api/cmmn-repository/case-definitions/");
    }

    @Test
    public void testContentRestApiIntegrationWithAuthentication() {
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/content-api/content-service/content-items";

        HttpEntity<?> request = new HttpEntity<>(createHeaders("filiphr", "password"));
        ResponseEntity<DataResponse<ContentItemResponse>> response = restTemplate
            .exchange(processDefinitionsUrl, HttpMethod.GET, request, new ParameterizedTypeReference<DataResponse<ContentItemResponse>>() {
            });

        assertThat(response.getStatusCode())
            .as("Status code")
            .isEqualTo(HttpStatus.OK);
        DataResponse<ContentItemResponse> contentItems = response.getBody();
        assertThat(contentItems).isNotNull();
        assertThat(contentItems.getData())
            .isEmpty();
        assertThat(contentItems.getTotal()).isEqualTo(0);
    }
    @Test
    public void testDmnRestApiIntegrationWithAuthentication() {
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/dmn-api/dmn-repository/deployments";

        HttpEntity<?> request = new HttpEntity<>(createHeaders("filiphr", "password"));
        ResponseEntity<DataResponse<DmnDeploymentResponse>> response = restTemplate
            .exchange(processDefinitionsUrl, HttpMethod.GET, request, new ParameterizedTypeReference<DataResponse<DmnDeploymentResponse>>() {
            });

        assertThat(response.getStatusCode())
            .as("Status code")
            .isEqualTo(HttpStatus.OK);
        DataResponse<DmnDeploymentResponse> deployments = response.getBody();
        assertThat(deployments).isNotNull();
        assertThat(deployments.getData())
            .isEmpty();
        assertThat(deployments.getTotal()).isEqualTo(0);
    }
    @Test
    public void testFormRestApiIntegrationWithAuthentication() {
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/form-api/form-repository/form-definitions";

        HttpEntity<?> request = new HttpEntity<>(createHeaders("filiphr", "password"));
        ResponseEntity<DataResponse<FormDefinitionResponse>> response = restTemplate
            .exchange(processDefinitionsUrl, HttpMethod.GET, request, new ParameterizedTypeReference<DataResponse<FormDefinitionResponse>>() {
            });

        assertThat(response.getStatusCode())
            .as("Status code")
            .isEqualTo(HttpStatus.OK);
        DataResponse<FormDefinitionResponse> formDefinitions = response.getBody();
        assertThat(formDefinitions).isNotNull();
        assertThat(formDefinitions.getData())
            .isEmpty();
        assertThat(formDefinitions.getTotal()).isEqualTo(0);
    }
    @Test
    public void testIdmRestApiIntegrationWithAuthentication() {
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/idm-api/groups";

        HttpEntity<?> request = new HttpEntity<>(createHeaders("filiphr", "password"));
        ResponseEntity<DataResponse<GroupResponse>> response = restTemplate
            .exchange(processDefinitionsUrl, HttpMethod.GET, request, new ParameterizedTypeReference<DataResponse<GroupResponse>>() {
            });

        assertThat(response.getStatusCode())
            .as("Status code")
            .isEqualTo(HttpStatus.OK);
        DataResponse<GroupResponse> groups = response.getBody();
        assertThat(groups).isNotNull();
        assertThat(groups.getData())
            .extracting(GroupResponse::getId, GroupResponse::getType, GroupResponse::getName, GroupResponse::getUrl)
            .containsExactlyInAnyOrder(
                tuple("user", "security-role", "users", null),
                tuple("admin", "security-role", "admin", null)
            );
        assertThat(groups.getTotal()).isEqualTo(2);
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
}
