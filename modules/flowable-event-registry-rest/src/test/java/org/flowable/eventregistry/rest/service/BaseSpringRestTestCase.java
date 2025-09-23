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
package org.flowable.eventregistry.rest.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.test.EnsureCleanDb;
import org.flowable.common.engine.impl.test.LoggingExtension;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.eventregistry.api.EventManagementService;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.rest.conf.ApplicationConfiguration;
import org.flowable.eventregistry.rest.service.api.RestUrlBuilder;
import org.flowable.eventregistry.rest.util.TestServer;
import org.flowable.eventregistry.spring.test.FlowableEventSpringExtension;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.User;
import org.flowable.spring.impl.test.FlowableSpringExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SpringJUnitWebConfig(ApplicationConfiguration.class)
@ExtendWith(FlowableEventSpringExtension.class)
@ExtendWith(FlowableSpringExtension.class)
@ExtendWith(LoggingExtension.class)
@EnsureCleanDb(excludeTables = {
        "ACT_GE_PROPERTY",
        "ACT_ID_PROPERTY"
})
public abstract class BaseSpringRestTestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSpringRestTestCase.class);

    protected String SERVER_URL_PREFIX;
    protected RestUrlBuilder URL_BUILDER;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected ProcessEngine processEngine;

    @Autowired
    protected EventRegistryEngineConfiguration eventRegistryEngineConfiguration;
    @Autowired
    protected EventRepositoryService repositoryService;
    @Autowired
    protected EventManagementService managementService;
    @Autowired
    protected EventRegistry eventRegistry;
    @Autowired
    protected RepositoryService processRepositoryService;
    @Autowired
    protected RuntimeService processRuntimeService;
    @Autowired
    protected TaskService processTaskService;
    @Autowired
    protected IdmIdentityService idmIdentityService;

    @Autowired
    protected CloseableHttpClient client;
    protected LinkedList<CloseableHttpResponse> httpResponses = new LinkedList<>();

    protected DateFormat longDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Autowired
    protected TestServer server;

    @BeforeEach
    void init() {
        createUsers();
        SERVER_URL_PREFIX = server.getServerUrlPrefix();
        URL_BUILDER = RestUrlBuilder.usingBaseUrl(SERVER_URL_PREFIX);
    }

    @AfterEach
    void cleanup() {
        Authentication.setAuthenticatedUserId(null);
        dropUsers();
        closeHttpConnections();
    }

    protected void createUsers() {
        User user = idmIdentityService.newUser("kermit");
        user.setFirstName("Kermit");
        user.setLastName("the Frog");
        user.setPassword("kermit");
        idmIdentityService.saveUser(user);

        Group group = idmIdentityService.newGroup("admin");
        group.setName("Administrators");
        idmIdentityService.saveGroup(group);

        idmIdentityService.createMembership(user.getId(), group.getId());
    }

    public JsonNode executeAndReadGetRequest(String url) {
        try (CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK)) {
            return objectMapper.readTree(response.getEntity().getContent());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to close request", e);
        }
    }

    /**
     * IMPORTANT: calling method is responsible for calling close() on returned {@link HttpResponse} to free the connection.
     */
    public CloseableHttpResponse executeRequest(HttpUriRequest request, int expectedStatusCode) {
        return internalExecuteRequest(request, expectedStatusCode, true);
    }

    /**
     * IMPORTANT: calling method is responsible for calling close() on returned {@link HttpResponse} to free the connection.
     */
    public CloseableHttpResponse executeBinaryRequest(HttpUriRequest request, int expectedStatusCode) {
        return internalExecuteRequest(request, expectedStatusCode, false);
    }

    protected CloseableHttpResponse internalExecuteRequest(HttpUriRequest request, int expectedStatusCode, boolean addJsonContentType) {
        try {
            if (addJsonContentType && request.getFirstHeader(HttpHeaders.CONTENT_TYPE) == null) {
                // Revert to default content-type
                request.addHeader(new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"));
            }
            CloseableHttpResponse response = client.execute(request);
            assertThat(response.getStatusLine()).isNotNull();

            int responseStatusCode = response.getStatusLine().getStatusCode();
            if (expectedStatusCode != responseStatusCode) {
                LOGGER.info("Wrong status code : {}, but should be {}", responseStatusCode, expectedStatusCode);
                if (response.getEntity() != null) {
                    LOGGER.info("Response body: {}", IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
                }
            }

            assertThat(responseStatusCode).isEqualTo(expectedStatusCode);
            httpResponses.add(response);
            return response;

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void closeResponse(CloseableHttpResponse response) {
        if (response != null) {
            try {
                response.close();
            } catch (IOException e) {
                throw new AssertionError("Could not close http connection", e);
            }
        }
    }

    protected void dropUsers() {
        idmIdentityService.deleteUser("kermit");
        idmIdentityService.deleteGroup("admin");
    }

    protected void closeHttpConnections() {
        for (CloseableHttpResponse response : httpResponses) {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    LOGGER.error("Could not close http connection", e);
                }
            }
        }
        httpResponses.clear();
    }

    protected String encode(String string) {
        if (string != null) {
            return URLEncoder.encode(string, StandardCharsets.UTF_8);
        }
        return null;
    }

    /**
     * Checks if the returned "data" array (child-node of root-json node returned by invoking a GET on the given url) contains entries with the given ID's.
     */
    protected void assertResultsPresentInDataResponse(String url, String... expectedResourceIds) throws IOException {
        int numberOfResultsExpected = expectedResourceIds.length;

        // Do the actual call
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        // Check status and size
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        assertThat(numberOfResultsExpected).isEqualTo(dataNode.size());

        // Check presence of ID's
        List<String> toBeFound = new ArrayList<>(Arrays.asList(expectedResourceIds));
        for (JsonNode aDataNode : dataNode) {
            String id = aDataNode.get("id").textValue();
            toBeFound.remove(id);
        }
        assertThat(toBeFound).as("Not all expected ids have been found in result, missing: " + StringUtils.join(toBeFound, ", ")).isEmpty();
    }

    /**
     * Checks if the returned "data" array (child-node of root-json node returned by invoking a GET on the given url) contains entries with the given ID's.
     */
    protected void assertResultsExactlyPresentInDataResponse(String url, String... expectedResourceIds) throws IOException {
        // Do the actual call
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        // Check status and size
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        assertThat(dataNode)
            .extracting(node -> node.get("id").textValue())
            .as("Expected result ids")
            .containsExactly(expectedResourceIds);
    }

    protected void assertEmptyResultsPresentInDataResponse(String url) throws IOException {
        // Do the actual call
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        // Check status and size
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        assertThat(dataNode).isEmpty();
    }

    /**
     * Checks if the returned "data" array (child-node of root-json node returned by invoking a POST on the given url) contains entries with the given ID's.
     */
    protected void assertResultsPresentInPostDataResponse(String url, ObjectNode body, String... expectedResourceIds) throws IOException {
        assertResultsPresentInPostDataResponseWithStatusCheck(url, body, HttpStatus.SC_OK, expectedResourceIds);
    }

    protected void assertResultsPresentInPostDataResponseWithStatusCheck(String url, ObjectNode body, int expectedStatusCode, String... expectedResourceIds) throws IOException {
        int numberOfResultsExpected = 0;
        if (expectedResourceIds != null) {
            numberOfResultsExpected = expectedResourceIds.length;
        }

        // Do the actual call
        HttpPost post = new HttpPost(SERVER_URL_PREFIX + url);
        post.setEntity(new StringEntity(body.toString()));
        CloseableHttpResponse response = executeRequest(post, expectedStatusCode);

        if (expectedStatusCode == HttpStatus.SC_OK) {
            // Check status and size
            JsonNode rootNode = objectMapper.readTree(response.getEntity().getContent());
            JsonNode dataNode = rootNode.get("data");
            assertThat(numberOfResultsExpected).isEqualTo(dataNode.size());

            // Check presence of ID's
            if (expectedResourceIds != null) {
                List<String> toBeFound = new ArrayList<>(Arrays.asList(expectedResourceIds));
                for (JsonNode aDataNode : dataNode) {
                    String id = aDataNode.get("id").textValue();
                    toBeFound.remove(id);
                }
                assertThat(toBeFound).as("Not all entries have been found in result, missing: " + StringUtils.join(toBeFound, ", ")).isEmpty();
            }
        }

        closeResponse(response);
    }

    /**
     * Checks if the rest operation returns an error as expected
     */
    protected void assertErrorResult(String url, ObjectNode body, int statusCode) throws IOException {

        // Do the actual call
        HttpPost post = new HttpPost(SERVER_URL_PREFIX + url);
        post.setEntity(new StringEntity(body.toString()));
        closeResponse(executeRequest(post, statusCode));
    }

    protected String buildUrl(String[] fragments, Object... arguments) {
        return URL_BUILDER.buildUrl(fragments, arguments);
    }
}
