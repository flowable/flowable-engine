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
package org.flowable.dmn.rest.service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
import org.flowable.common.engine.impl.test.EnsureCleanDb;
import org.flowable.common.engine.impl.test.LoggingExtension;
import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.api.DmnHistoryService;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.test.DmnDeploymentId;
import org.flowable.dmn.rest.conf.ApplicationConfiguration;
import org.flowable.dmn.rest.util.TestServer;
import org.flowable.dmn.spring.impl.test.InternalFlowableDmnSpringExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@SpringJUnitWebConfig(ApplicationConfiguration.class)
@ExtendWith(InternalFlowableDmnSpringExtension.class)
@ExtendWith(LoggingExtension.class)
@EnsureCleanDb(excludeTables = {
        "ACT_GE_PROPERTY"
})
public abstract class BaseSpringDmnRestTestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSpringDmnRestTestCase.class);

    protected String SERVER_URL_PREFIX;
    protected DmnRestUrlBuilder URL_BUILDER;

    protected ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected DmnEngine dmnEngine;

    protected String deploymentId;
    protected Throwable exception;

    @Autowired
    protected DmnEngineConfiguration dmnEngineConfiguration;
    @Autowired
    protected DmnRepositoryService dmnRepositoryService;
    @Autowired
    protected DmnDecisionService dmnRuleService;
    @Autowired
    protected DmnHistoryService dmnHistoryService;

    @Autowired
    protected CloseableHttpClient client;
    protected LinkedList<CloseableHttpResponse> httpResponses = new LinkedList<>();

    @Autowired
    protected TestServer server;

    @BeforeEach
    void init(@DmnDeploymentId String deploymentId) {
        this.deploymentId = deploymentId;

        SERVER_URL_PREFIX = server.getServerUrlPrefix();
        URL_BUILDER = DmnRestUrlBuilder.usingBaseUrl(SERVER_URL_PREFIX);
    }

    @AfterEach
    void cleanup() {
        closeHttpConnections();
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
        CloseableHttpResponse response = null;
        try {
            if (addJsonContentType && request.getFirstHeader(HttpHeaders.CONTENT_TYPE) == null) {
                // Revert to default content-type
                request.addHeader(new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"));
            }
            response = client.execute(request);
            assertThat(response.getStatusLine()).isNotNull();

            int responseStatusCode = response.getStatusLine().getStatusCode();
            if (expectedStatusCode != responseStatusCode) {
                LOGGER.info("Wrong status code : {}, but should be {}", responseStatusCode, expectedStatusCode);
                LOGGER.info("Response body: {}", IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
            }

            assertThat(responseStatusCode).isEqualTo(expectedStatusCode);
            httpResponses.add(response);
            return response;

        } catch (IOException e) {
            fail(e.getMessage());
        }
        return null;
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
    protected void assertResultsPresentInDataResponse(String url, String... expectedResourceIds) throws JsonProcessingException, IOException {
        int numberOfResultsExpected = expectedResourceIds.length;

        // Do the actual call
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        // Check status and size
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        assertThat(dataNode).hasSize(numberOfResultsExpected);

        // Check presence of ID's
        List<String> toBeFound = new ArrayList<>(Arrays.asList(expectedResourceIds));
        Iterator<JsonNode> it = dataNode.iterator();
        while (it.hasNext()) {
            String id = it.next().get("id").textValue();
            toBeFound.remove(id);
        }
        assertThat(toBeFound)
                .as("Not all expected ids have been found in result, missing: " + StringUtils.join(toBeFound, ", "))
                .isEmpty();
    }

    protected void assertResultsPresentInPostDataResponseWithStatusCheck(String url, ObjectNode body, int expectedStatusCode, String... expectedResourceIds) throws JsonProcessingException, IOException {
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
            assertThat(dataNode).hasSize(numberOfResultsExpected);

            // Check presence of ID's
            if (expectedResourceIds != null) {
                List<String> toBeFound = new ArrayList<>(Arrays.asList(expectedResourceIds));
                Iterator<JsonNode> it = dataNode.iterator();
                while (it.hasNext()) {
                    String id = it.next().get("id").textValue();
                    toBeFound.remove(id);
                }
                assertThat(toBeFound)
                        .as("Not all expected ids have been found in result, missing: " + StringUtils.join(toBeFound, ", "))
                        .isEmpty();
            }
        }

        closeResponse(response);
    }
}
