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

package org.flowable.rest.service.api.identity;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.engine.test.Deployment;
import org.flowable.idm.api.Group;
import org.flowable.rest.service.BaseSpringRestTest;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.Assert;
import org.junit.Test;

import capital.scalable.restdocs.payload.JacksonResponseFieldSnippet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
public class GroupCollectionResourceMvcTest extends BaseSpringRestTest {

    /**
     * Test getting all groups.
     */
    @Test
    public void testGetGroups() throws Exception {
        List<Group> savedGroups = new ArrayList<>();
        try {
            Group group1 = identityService.newGroup("testgroup1");
            group1.setName("Test group");
            group1.setType("Test type");
            identityService.saveGroup(group1);
            savedGroups.add(group1);

            Group group2 = identityService.newGroup("testgroup2");
            group2.setName("Another group");
            group2.setType("Another type");
            identityService.saveGroup(group2);
            savedGroups.add(group2);

            Group group3 = identityService.createGroupQuery().groupId("admin").singleResult();
            Assert.assertNotNull(group3);

            // Test filter-less
            String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_GROUP_COLLECTION);
            assertResultsPresentInDataResponse(url, group1.getId(), group2.getId(), group3.getId());

            // Test based on name
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_GROUP_COLLECTION) + "?name=Test group";
            assertResultsPresentInDataResponse(url, group1.getId());

            // Test based on name like
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_GROUP_COLLECTION) + "?nameLike=% group";
            assertResultsPresentInDataResponse(url, group2.getId(), group1.getId());

            // Test based on type
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_GROUP_COLLECTION) + "?type=Another type";
            assertResultsPresentInDataResponse(url, group2.getId());

            // Test based on group member
            url = RestUrls.createRelativeResourceUrl(RestUrls.URL_GROUP_COLLECTION) + "?member=kermit";
            assertResultsPresentInDataResponse(url, group3.getId());

        } finally {

            // Delete groups after test passes or fails
            if (!savedGroups.isEmpty()) {
                for (Group group : savedGroups) {
                    identityService.deleteGroup(group.getId());
                }
            }
        }
    }
}
