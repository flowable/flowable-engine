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

import java.util.Base64;

import org.flowable.common.rest.api.DataResponse;
import org.flowable.rest.service.api.repository.ProcessDefinitionResponse;
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

/**
 * @author Filip Hrisafov
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebClient(registerRestTemplate = true)
public class SampleLdapApplicationRestTest extends AbstractSampleLdapTest {

    @LocalServerPort
    private int serverPort;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    public void invalidPasswordShouldBeRejected() {

        String processDefinitionsUrl = "http://localhost:" + serverPort + "/process-api/repository/process-definitions";
        HttpEntity<?> request = new HttpEntity<>(createHeaders("kermit", "password"));
        ResponseEntity<DataResponse<ProcessDefinitionResponse>> response = testRestTemplate
            .exchange(processDefinitionsUrl, HttpMethod.GET, request, new ParameterizedTypeReference<DataResponse<ProcessDefinitionResponse>>() {

            });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void correctPasswordShouldBeAccepted() {

        String processDefinitionsUrl = "http://localhost:" + serverPort + "/process-api/repository/process-definitions";
        HttpEntity<?> request = new HttpEntity<>(createHeaders("kermit", "pass"));
        ResponseEntity<DataResponse<ProcessDefinitionResponse>> response = testRestTemplate
            .exchange(processDefinitionsUrl, HttpMethod.GET, request, new ParameterizedTypeReference<DataResponse<ProcessDefinitionResponse>>() {

            });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        request = new HttpEntity<>(createHeaders("pepe", "pass"));
        response = testRestTemplate
            .exchange(processDefinitionsUrl, HttpMethod.GET, request, new ParameterizedTypeReference<DataResponse<ProcessDefinitionResponse>>() {

            });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        String jobsUrl = "http://localhost:" + serverPort + "/process-api/management/jobs";
        request = new HttpEntity<>(createHeaders("kermit", "pass"));
        response = testRestTemplate
            .exchange(jobsUrl, HttpMethod.GET, request, new ParameterizedTypeReference<DataResponse<ProcessDefinitionResponse>>() {

            });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        request = new HttpEntity<>(createHeaders("fozzie", "pass"));
        response = testRestTemplate
            .exchange(jobsUrl, HttpMethod.GET, request, new ParameterizedTypeReference<DataResponse<ProcessDefinitionResponse>>() {

            });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    }

    @Test
    public void lackingPermissionsShouldBeForbidden() {

        String jobsUrl = "http://localhost:" + serverPort + "/process-api/management/jobs";
        HttpEntity<?> request = new HttpEntity<>(createHeaders("pepe", "pass"));
        ResponseEntity<DataResponse<ProcessDefinitionResponse>> response = testRestTemplate
            .exchange(jobsUrl, HttpMethod.GET, request, new ParameterizedTypeReference<DataResponse<ProcessDefinitionResponse>>() {

            });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    }

    protected static HttpHeaders createHeaders(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, base64Authentication(username, password));
        return headers;
    }

    protected static String base64Authentication(String username, String password) {
        String auth = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }
}
