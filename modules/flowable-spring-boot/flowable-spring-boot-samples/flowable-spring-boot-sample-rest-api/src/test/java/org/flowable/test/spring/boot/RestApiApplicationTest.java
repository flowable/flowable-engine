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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.flowable.cmmn.rest.service.api.repository.CaseDefinitionResponse;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.content.rest.service.api.content.ContentItemResponse;
import org.flowable.dmn.rest.service.api.repository.DmnDeploymentResponse;
import org.flowable.rest.service.api.identity.GroupResponse;
import org.flowable.rest.service.api.repository.FormDefinitionResponse;
import org.flowable.rest.service.api.repository.ProcessDefinitionResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import flowable.Application;

/**
 * @author Filip Hrisafov
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebClient(registerRestTemplate = true)
public class RestApiApplicationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int serverPort;

    @Test
    public void testRestApiIntegration() {
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/process-api/repository/process-definitions";

        ResponseEntity<DataResponse<ProcessDefinitionResponse>> response = restTemplate
            .exchange(processDefinitionsUrl, HttpMethod.GET, null, new ParameterizedTypeReference<DataResponse<ProcessDefinitionResponse>>() {

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
    public void testCmmnRestApiIntegration() {
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/cmmn-api/cmmn-repository/case-definitions";

        ResponseEntity<DataResponse<CaseDefinitionResponse>> response = restTemplate
            .exchange(processDefinitionsUrl, HttpMethod.GET, null, new ParameterizedTypeReference<DataResponse<CaseDefinitionResponse>>() {
            });

        assertThat(response.getStatusCode())
            .as("Status code")
            .isEqualTo(HttpStatus.OK);
        DataResponse<CaseDefinitionResponse> caseDefinitions = response.getBody();
        assertThat(caseDefinitions).isNotNull();
        assertThat(caseDefinitions.getTotal()).isEqualTo(1);
        CaseDefinitionResponse defResponse = caseDefinitions.getData().get(0);
        assertThat(defResponse.getKey()).isEqualTo("case1");
        assertThat(defResponse.getUrl()).startsWith("http://localhost:" + serverPort + "/cmmn-api/cmmn-repository/case-definitions/");
    }

    @Test
    public void testCmmnRestApiIntegrationNotFound() {
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/cmmn-api/cmmn-repository/case-definitions/does-not-exist";

        ResponseEntity<String> response = restTemplate.getForEntity(processDefinitionsUrl, String.class);

        BasicJsonTester jsonTester = new BasicJsonTester(getClass());

        assertThat(jsonTester.from(response.getBody())).isEqualToJson("{"
            + "\"message\": \"Not found\","
            + "\"exception\": \"no deployed case definition found with id 'does-not-exist'\""
            + "}");
        assertThat(response.getStatusCode())
            .as("Status code")
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void testContentRestApiIntegration() {
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/content-api/content-service/content-items";

        ResponseEntity<DataResponse<ContentItemResponse>> response = restTemplate
            .exchange(processDefinitionsUrl, HttpMethod.GET, null, new ParameterizedTypeReference<DataResponse<ContentItemResponse>>() {
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
    public void testDmnRestApiIntegration() {
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/dmn-api/dmn-repository/deployments";

        ResponseEntity<DataResponse<DmnDeploymentResponse>> response = restTemplate
            .exchange(processDefinitionsUrl, HttpMethod.GET, null, new ParameterizedTypeReference<DataResponse<DmnDeploymentResponse>>() {
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
    public void testFormRestApiIntegration() {
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/form-api/form-repository/form-definitions";

        ResponseEntity<DataResponse<FormDefinitionResponse>> response = restTemplate
            .exchange(processDefinitionsUrl, HttpMethod.GET, null, new ParameterizedTypeReference<DataResponse<FormDefinitionResponse>>() {
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
    public void testIdmRestApiIntegration() {
        String processDefinitionsUrl = "http://localhost:" + serverPort + "/idm-api/groups";

        ResponseEntity<DataResponse<GroupResponse>> response = restTemplate
            .exchange(processDefinitionsUrl, HttpMethod.GET, null, new ParameterizedTypeReference<DataResponse<GroupResponse>>() {
            });

        assertThat(response.getStatusCode())
            .as("Status code")
            .isEqualTo(HttpStatus.OK);
        DataResponse<GroupResponse> groups = response.getBody();
        assertThat(groups).isNotNull();
        assertThat(groups.getData())
            .extracting(GroupResponse::getId, GroupResponse::getType, GroupResponse::getName, GroupResponse::getUrl)
            .containsExactlyInAnyOrder(
                tuple("user", "security-role", "users", null)
            );
        assertThat(groups.getTotal()).isEqualTo(1);
    }
}
