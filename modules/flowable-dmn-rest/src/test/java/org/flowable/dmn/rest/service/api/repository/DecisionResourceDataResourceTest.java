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

import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.flowable.dmn.rest.service.api.BaseSpringDmnRestTestCase;
import org.flowable.dmn.rest.service.api.DmnRestUrls;

/**
 * @author Yvo Swillens
 */
public class DecisionResourceDataResourceTest extends BaseSpringDmnRestTestCase {

    @DmnDeployment(resources = { "org/flowable/dmn/rest/service/api/repository/simple.dmn" })
    public void testGetDecisionResource() throws Exception {

        DmnDecision definition = dmnRepositoryService.createDecisionQuery().singleResult();

        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DECISION_RESOURCE_CONTENT, definition.getId()));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_OK);

        // Check "OK" status
        String content = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        closeResponse(response);
        assertThat(content).contains("Full Decision");
    }

    public void testGetDecisionResourceForUnexistingDecisionTable() throws Exception {
        HttpGet httpGet = new HttpGet(SERVER_URL_PREFIX + DmnRestUrls.createRelativeResourceUrl(DmnRestUrls.URL_DECISION_RESOURCE_CONTENT, "unexisting"));
        CloseableHttpResponse response = executeRequest(httpGet, HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }
}
