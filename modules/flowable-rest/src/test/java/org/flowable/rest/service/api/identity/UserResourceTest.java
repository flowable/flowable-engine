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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
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
public class UserResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a single user.
     */
    @Test
    public void testGetUser() throws Exception {
        User savedUser = null;
        try {
            User newUser = identityService.newUser("testuser");
            newUser.setFirstName("Fred");
            newUser.setLastName("McDonald");
            newUser.setDisplayName("Fred McDonald");
            newUser.setEmail("no-reply@flowable.org");
            identityService.saveUser(newUser);
            savedUser = newUser;

            CloseableHttpResponse response = executeRequest(
                    new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_USER, newUser.getId())), HttpStatus.SC_OK);

            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "id: 'testuser',"
                            + "firstName: 'Fred',"
                            + "lastName: 'McDonald',"
                            + "displayName: 'Fred McDonald',"
                            + "email: 'no-reply@flowable.org',"
                            + "url: '" + SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_USER, newUser.getId()) + "'"
                            + "}");

        } finally {

            // Delete user after test passes or fails
            if (savedUser != null) {
                identityService.deleteUser(savedUser.getId());
            }
        }
    }

    /**
     * Test getting an unexisting user.
     */
    @Test
    public void testGetUnexistingUser() throws Exception {
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_USER, "unexisting")), HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test deleting a single user.
     */
    @Test
    public void testDeleteUser() throws Exception {
        User savedUser = null;
        try {
            User newUser = identityService.newUser("testuser");
            newUser.setFirstName("Fred");
            newUser.setLastName("McDonald");
            newUser.setEmail("no-reply@flowable.org");
            identityService.saveUser(newUser);
            savedUser = newUser;

            closeResponse(executeRequest(new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_USER, newUser.getId())), HttpStatus.SC_NO_CONTENT));

            // Check if user is deleted
            assertThat(identityService.createUserQuery().userId(newUser.getId()).count()).isZero();
            savedUser = null;

        } finally {

            // Delete user after test fails
            if (savedUser != null) {
                identityService.deleteUser(savedUser.getId());
            }
        }
    }

    /**
     * Test deleting an unexisting user.
     */
    @Test
    public void testDeleteUnexistingUser() throws Exception {
        closeResponse(executeRequest(new HttpDelete(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_USER, "unexisting")), HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test updating a single user.
     */
    @Test
    public void testUpdateUser() throws Exception {
        User savedUser = null;
        try {
            User newUser = identityService.newUser("testuser");
            newUser.setFirstName("Fred");
            newUser.setLastName("McDonald");
            newUser.setEmail("no-reply@flowable.org");
            identityService.saveUser(newUser);
            savedUser = newUser;

            ObjectNode taskUpdateRequest = objectMapper.createObjectNode();
            taskUpdateRequest.put("firstName", "Tijs");
            taskUpdateRequest.put("lastName", "Barrez");
            taskUpdateRequest.put("displayName", "Tijs Barrez");
            taskUpdateRequest.put("email", "no-reply@flowable.org");
            taskUpdateRequest.put("password", "updatedpassword");

            HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_USER, newUser.getId()));
            httpPut.setEntity(new StringEntity(taskUpdateRequest.toString()));
            CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "id: 'testuser',"
                            + "firstName: 'Tijs',"
                            + "lastName: 'Barrez',"
                            + "displayName: 'Tijs Barrez',"
                            + "email: 'no-reply@flowable.org',"
                            + "url: '" + SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_USER, newUser.getId()) + "'"
                            + "}");

            // Check user is updated in Flowable
            newUser = identityService.createUserQuery().userId(newUser.getId()).singleResult();
            assertThat(newUser.getLastName()).isEqualTo("Barrez");
            assertThat(newUser.getFirstName()).isEqualTo("Tijs");
            assertThat(newUser.getDisplayName()).isEqualTo("Tijs Barrez");
            assertThat(newUser.getEmail()).isEqualTo("no-reply@flowable.org");
            assertThat(newUser.getPassword()).isEqualTo("updatedpassword");

        } finally {

            // Delete user after test fails
            if (savedUser != null) {
                identityService.deleteUser(savedUser.getId());
            }
        }
    }

    /**
     * Test updating a single user passing in no fields in the json, user should remain unchanged.
     */
    @Test
    public void testUpdateUserNoFields() throws Exception {
        User savedUser = null;
        try {
            User newUser = identityService.newUser("testuser");
            newUser.setFirstName("Fred");
            newUser.setLastName("McDonald");
            newUser.setDisplayName("Fred McDonald");
            newUser.setEmail("no-reply@flowable.org");
            identityService.saveUser(newUser);
            savedUser = newUser;

            ObjectNode taskUpdateRequest = objectMapper.createObjectNode();

            HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_USER, newUser.getId()));
            httpPut.setEntity(new StringEntity(taskUpdateRequest.toString()));
            CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "id: 'testuser',"
                            + "firstName: 'Fred',"
                            + "lastName: 'McDonald',"
                            + "displayName: 'Fred McDonald',"
                            + "email: 'no-reply@flowable.org',"
                            + "url: '" + SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_USER, newUser.getId()) + "'"
                            + "}");

            // Check user is updated in Flowable
            newUser = identityService.createUserQuery().userId(newUser.getId()).singleResult();
            assertThat(newUser.getLastName()).isEqualTo("McDonald");
            assertThat(newUser.getFirstName()).isEqualTo("Fred");
            assertThat(newUser.getDisplayName()).isEqualTo("Fred McDonald");
            assertThat(newUser.getEmail()).isEqualTo("no-reply@flowable.org");
            assertThat(newUser.getPassword()).isNull();

        } finally {

            // Delete user after test fails
            if (savedUser != null) {
                identityService.deleteUser(savedUser.getId());
            }
        }
    }

    /**
     * Test updating a single user passing in no fields in the json, user should remain unchanged.
     */
    @Test
    public void testUpdateUserNullFields() throws Exception {
        User savedUser = null;
        try {
            User newUser = identityService.newUser("testuser");
            newUser.setFirstName("Fred");
            newUser.setLastName("McDonald");
            newUser.setDisplayName("Fred McDonald");
            newUser.setEmail("no-reply@flowable.org");
            identityService.saveUser(newUser);
            savedUser = newUser;

            ObjectNode taskUpdateRequest = objectMapper.createObjectNode();
            taskUpdateRequest.putNull("firstName");
            taskUpdateRequest.putNull("lastName");
            taskUpdateRequest.putNull("displayName");
            taskUpdateRequest.putNull("email");
            taskUpdateRequest.putNull("password");

            HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_USER, newUser.getId()));
            httpPut.setEntity(new StringEntity(taskUpdateRequest.toString()));
            CloseableHttpResponse response = executeRequest(httpPut, HttpStatus.SC_OK);
            JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
            closeResponse(response);
            assertThat(responseNode).isNotNull();
            assertThatJson(responseNode)
                    .when(Option.IGNORING_EXTRA_FIELDS)
                    .isEqualTo("{"
                            + "id: 'testuser',"
                            + "firstName: null,"
                            + "lastName: null,"
                            + "displayName: null,"
                            + "email: null,"
                            + "url: '" + SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_USER, newUser.getId()) + "'"
                            + "}");

            // Check user is updated in Flowable
            newUser = identityService.createUserQuery().userId(newUser.getId()).singleResult();
            assertThat(newUser.getLastName()).isNull();
            assertThat(newUser.getFirstName()).isNull();
            assertThat(newUser.getDisplayName()).isNull();
            assertThat(newUser.getEmail()).isNull();

        } finally {

            // Delete user after test fails
            if (savedUser != null) {
                identityService.deleteUser(savedUser.getId());
            }
        }
    }

    /**
     * Test updating an unexisting user.
     */
    @Test
    public void testUpdateUnexistingUser() throws Exception {
        HttpPut httpPut = new HttpPut(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_USER, "unexisting"));
        httpPut.setEntity(new StringEntity(objectMapper.createObjectNode().toString()));
        closeResponse(executeRequest(httpPut, HttpStatus.SC_NOT_FOUND));
    }
}
