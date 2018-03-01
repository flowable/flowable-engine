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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Test for all REST-operations related to a identity links on a Process instance resource.
 * 
 * @author Frederik Heremans
 */
public class CaseInstanceIdentityLinkResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting all identity links.
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/oneHumanTaskCase.cmmn" })
    public void testGetIdentityLinks() throws Exception {

        // Test candidate user/groups links + manual added identityLink
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        runtimeService.addUserIdentityLink(caseInstance.getId(), "john", "customType");
        runtimeService.addUserIdentityLink(caseInstance.getId(), "paul", "candidate");

        // Execute the request
        CloseableHttpResponse response = executeRequest(
                new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_IDENTITYLINKS_COLLECTION, caseInstance.getId())), HttpStatus.SC_OK);

        JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);
        assertNotNull(responseNode);
        assertTrue(responseNode.isArray());
        assertEquals(2, responseNode.size());

        boolean johnFound = false;
        boolean paulFound = false;

        for (int i = 0; i < responseNode.size(); i++) {
            ObjectNode link = (ObjectNode) responseNode.get(i);
            assertNotNull(link);
            if (!link.get("user").isNull()) {
                if (link.get("user").textValue().equals("john")) {
                    assertEquals("customType", link.get("type").textValue());
                    assertTrue(link.get("group").isNull());
                    assertTrue(link.get("url").textValue().endsWith(RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_IDENTITYLINK, caseInstance.getId(), "john", "customType")));
                    johnFound = true;
                } else {
                    assertEquals("paul", link.get("user").textValue());
                    assertEquals("candidate", link.get("type").textValue());
                    assertTrue(link.get("group").isNull());
                    assertTrue(link.get("url").textValue().endsWith(RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_INSTANCE_IDENTITYLINK, caseInstance.getId(), "paul", "candidate")));
                    paulFound = true;
                }
            }
        }
        assertTrue(johnFound);
        assertTrue(paulFound);
    }
}
