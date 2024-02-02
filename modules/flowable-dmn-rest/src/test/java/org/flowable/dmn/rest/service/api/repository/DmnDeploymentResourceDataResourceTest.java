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
package org.flowable.dmn.rest.service.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHeader;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.rest.service.api.BaseSpringDmnRestTestCase;
import org.flowable.dmn.rest.service.api.DmnRestUrls;

/**
 * @author Yvo Swillens
 */
public class DmnDeploymentResourceDataResourceTest extends BaseSpringDmnRestTestCase {

    public void testGetDmnDeploymentResource() throws Exception {

        try {
            DmnDeployment deployment = dmnRepositoryService.createDeployment().name("Deployment 1").addInputStream("test.txt", new ByteArrayInputStream("Test content".getBytes())).deploy();

            HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DEPLOYMENT_RESOURCE_CONTENT, deployment.getId(), "test.txt"));
            httpGet.addHeader(new BasicHeader(HttpHeaders.ACCEPT, "text/plain"));
            CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);
            String responseAsString = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            closeResponse(response);
            assertThat(responseAsString).isEqualTo("Test content");
        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<DmnDeployment> deployments = dmnRepositoryService.createDeploymentQuery().list();
            for (DmnDeployment deployment : deployments) {
                dmnRepositoryService.deleteDeployment(deployment.getId());
            }
        }
    }

    public void testGetDmnDeploymentResourceForUnexistingDmnDeployment() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DEPLOYMENT_RESOURCE_CONTENT, "unexisting", "test.txt"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

}
