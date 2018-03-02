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

package org.flowable.cmmn.rest.service.api.runtime;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.RestUrls;

/**
 * @author Tijs Rademakers
 */
public class CaseInstanceDiagramResourceTest extends BaseSpringRestTestCase {

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/repeatingStage.cmmn" })
    public void testGetCaseDiagram() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("testRepeatingStage").start();

        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_DIAGRAM, caseInstance.getId())),
                HttpStatus.SC_OK);
        assertNotNull(response.getEntity().getContent());
        assertEquals("image/png", response.getEntity().getContentType().getValue());
        closeResponse(response);
    }

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/oneHumanTaskCase.cmmn" })
    public void testGetCaseDiagramWithoutDiagram() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_DIAGRAM, caseInstance.getId())), HttpStatus.SC_BAD_REQUEST));
    }

    /**
     * Test getting an unexisting case instance.
     */
    public void testGetUnexistingCaseInstance() {
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_DIAGRAM, "unexistingpi")), HttpStatus.SC_NOT_FOUND));
    }
}
