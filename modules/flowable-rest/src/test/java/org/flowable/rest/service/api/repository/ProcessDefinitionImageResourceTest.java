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

package org.flowable.rest.service.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.Test;

/**
 * @author Bassam Al-Sarori
 */
public class ProcessDefinitionImageResourceTest extends BaseSpringRestTestCase {

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml", "org/flowable/rest/service/api/repository/oneTaskProcess.png" })
    public void testGetProcessDefinitionImage() throws Exception {
        ProcessDefinition oneTaskProcess = repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").singleResult();
        CloseableHttpResponse response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_IMAGE, oneTaskProcess.getId())),
                HttpStatus.SC_OK);
        assertThat(response.getEntity().getContent()).isNotNull();
        assertThat(response.getEntity().getContentType().getValue()).isEqualTo("image/png");
        closeResponse(response);
    }

    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/repository/oneTaskProcess.bpmn20.xml" })
    public void testGetProcessDefinitionImageWithoutImage() throws Exception {
        ProcessDefinition oneTaskProcess = repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").singleResult();

        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_IMAGE, oneTaskProcess.getId())),
                HttpStatus.SC_BAD_REQUEST);

        // response content type application/json;charset=UTF-8
        assertThat(response.getEntity().getContentType().getValue().split(";")[0]).isEqualTo("application/json");
        closeResponse(response);
    }

    /**
     * Test getting an unexisting process definition.
     */
    @Test
    public void testGetUnexistingProcessDefinition() {
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_PROCESS_DEFINITION_IMAGE, "unexistingpi")), HttpStatus.SC_NOT_FOUND));
    }
}
