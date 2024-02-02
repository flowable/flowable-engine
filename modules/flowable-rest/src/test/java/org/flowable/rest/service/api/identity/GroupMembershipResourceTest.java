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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * @author Frederik Heremans
 */
public class GroupMembershipResourceTest extends BaseSpringRestTestCase {

    @Test
    public void testCreateMembership() throws Exception {
        try {
            Group testGroup = identityService.newGroup("testgroup");
            testGroup.setName("Test group");
            testGroup.setType("Test type");
            identityService.saveGroup(testGroup);

            User testUser = identityService.newUser("testuser");
            identityService.saveUser(testUser);

            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("userId", "testuser");

            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_GROUP_MEMBERSHIP_COLLECTION, "testgroup"));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            CloseableHttpResponse response = executeRequest(httpPost, HttpStatus.SC_CREATED);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + " userId: 'testuser',"
                            + " groupId: 'testgroup',"
                            + " url: '" + SERVER_URL_PREFIX + RestUrls
                            .createRelativeResourceUrl(RestUrls.URL_GROUP_MEMBERSHIP, testGroup.getId(), testUser.getId()) + "'"
                            + "}");

            Group createdGroup = identityService.createGroupQuery().groupId("testgroup").singleResult();
            assertThat(createdGroup).isNotNull();
            assertThat(createdGroup.getName()).isEqualTo("Test group");
            assertThat(createdGroup.getType()).isEqualTo("Test type");

            assertThat(identityService.createUserQuery().memberOfGroup("testgroup").singleResult()).isNotNull();
            assertThat(identityService.createUserQuery().memberOfGroup("testgroup").singleResult().getId()).isEqualTo("testuser");
        } finally {
            try {
                identityService.deleteGroup("testgroup");
            } catch (Throwable ignore) {
                // Ignore, since the group may not have been created in the test
                // or already deleted
            }

            try {
                identityService.deleteUser("testuser");
            } catch (Throwable ignore) {
                // Ignore, since the group may not have been created in the test
                // or already deleted
            }
        }
    }

    @Test
    public void testCreateMembershipAlreadyExisting() throws Exception {
        try {
            Group testGroup = identityService.newGroup("testgroup");
            testGroup.setName("Test group");
            testGroup.setType("Test type");
            identityService.saveGroup(testGroup);

            User testUser = identityService.newUser("testuser");
            identityService.saveUser(testUser);

            identityService.createMembership("testuser", "testgroup");

            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("userId", "testuser");

            HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_GROUP_MEMBERSHIP_COLLECTION, "testgroup"));
            httpPost.setEntity(new StringEntity(requestNode.toString()));
            closeResponse(executeRequest(httpPost, HttpStatus.SC_CONFLICT));

        } finally {
            try {
                identityService.deleteGroup("testgroup");
            } catch (Throwable ignore) {
                // Ignore, since the group may not have been created in the test
                // or already deleted
            }

            try {
                identityService.deleteUser("testuser");
            } catch (Throwable ignore) {
                // Ignore, since the group may not have been created in the test
                // or already deleted
            }
        }
    }

    @Test
    public void testDeleteMembership() throws Exception {
        try {
            Group testGroup = identityService.newGroup("testgroup");
            testGroup.setName("Test group");
            testGroup.setType("Test type");
            identityService.saveGroup(testGroup);

            User testUser = identityService.newUser("testuser");
            identityService.saveUser(testUser);

            identityService.createMembership("testuser", "testgroup");

            HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_GROUP_MEMBERSHIP, "testgroup", "testuser"));
            CloseableHttpResponse response = executeRequest(httpDelete, HttpStatus.SC_NO_CONTENT);
            closeResponse(response);

            // Check if membership is actually deleted
            assertThat(identityService.createUserQuery().memberOfGroup("testgroup").singleResult()).isNull();
        } finally {
            try {
                identityService.deleteGroup("testgroup");
            } catch (Throwable ignore) {
                // Ignore, since the group may not have been created in the test
                // or already deleted
            }

            try {
                identityService.deleteUser("testuser");
            } catch (Throwable ignore) {
                // Ignore, since the group may not have been created in the test
                // or already deleted
            }
        }
    }

    /**
     * Test delete membership that is no member in the group.
     */
    @Test
    public void testDeleteMembershipNoMember() throws Exception {
        try {
            Group testGroup = identityService.newGroup("testgroup");
            testGroup.setName("Test group");
            testGroup.setType("Test type");
            identityService.saveGroup(testGroup);

            User testUser = identityService.newUser("testuser");
            identityService.saveUser(testUser);

            HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_GROUP_MEMBERSHIP, "testgroup", "testuser"));
            closeResponse(executeRequest(httpDelete, HttpStatus.SC_NOT_FOUND));

        } finally {
            try {
                identityService.deleteGroup("testgroup");
            } catch (Throwable ignore) {
                // Ignore, since the group may not have been created in the test
                // or already deleted
            }

            try {
                identityService.deleteUser("testuser");
            } catch (Throwable ignore) {
                // Ignore, since the group may not have been created in the test
                // or already deleted
            }
        }
    }

    /**
     * Test deleting member from an unexisting group.
     */
    @Test
    public void testDeleteMemberfromUnexistingGroup() throws Exception {
        HttpDelete httpDelete = new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_GROUP_MEMBERSHIP, "unexisting", "kermit"));
        closeResponse(executeRequest(httpDelete, HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test adding member to an unexisting group.
     */
    @Test
    public void testAddMemberToUnexistingGroup() throws Exception {
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_GROUP_MEMBERSHIP_COLLECTION, "unexisting"));
        httpPost.setEntity(new StringEntity(objectMapper.createObjectNode().toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test adding member to a group, without specifying userId
     */
    @Test
    public void testAddMemberNoUserId() throws Exception {
        HttpPost httpPost = new HttpPost(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_GROUP_MEMBERSHIP_COLLECTION, "admin"));
        httpPost.setEntity(new StringEntity(objectMapper.createObjectNode().toString()));
        closeResponse(executeRequest(httpPost, HttpStatus.SC_BAD_REQUEST));
    }
}
