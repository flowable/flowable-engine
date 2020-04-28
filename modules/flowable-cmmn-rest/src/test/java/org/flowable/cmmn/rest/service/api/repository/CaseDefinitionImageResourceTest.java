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

package org.flowable.cmmn.rest.service.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;

/**
 * @author Tijs Rademakers
 */
public class CaseDefinitionImageResourceTest extends BaseSpringRestTestCase {

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/repeatingStage.cmmn"})
    public void testGetCaseDefinitionImage() throws Exception {
        CaseDefinition repeatingStageCase = repositoryService.createCaseDefinitionQuery().caseDefinitionKey("testRepeatingStage").singleResult();
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(
                        CmmnRestUrls.URL_CASE_DEFINITION_IMAGE, repeatingStageCase.getId())), HttpStatus.SC_OK);
        assertThat(response.getEntity().getContent()).isNotNull();
        assertThat(response.getEntity().getContentType().getValue()).isEqualTo("image/png");
        closeResponse(response);
    }

    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn" })
    public void testGetCaseDefinitionImageWithoutImage() throws Exception {
        CaseDefinition oneTaskCase = repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneHumanTaskCase").singleResult();
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(
                        CmmnRestUrls.URL_CASE_DEFINITION_IMAGE, oneTaskCase.getId())), HttpStatus.SC_NOT_FOUND);
        closeResponse(response);
    }

    /**
     * Test getting an unexisting case definition.
     */
    public void testGetUnexistingCaseDefinition() {
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_CASE_DEFINITION_IMAGE, "unexistingpi")), HttpStatus.SC_NOT_FOUND));
    }
}
