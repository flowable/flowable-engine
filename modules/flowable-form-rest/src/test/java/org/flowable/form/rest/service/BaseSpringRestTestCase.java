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
package org.flowable.form.rest.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.eclipse.jetty.server.Server;
import org.flowable.form.api.FormManagementService;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.test.FormTestHelper;
import org.flowable.form.rest.FormRestUrlBuilder;
import org.flowable.form.rest.conf.ApplicationConfiguration;
import org.flowable.form.rest.util.TestServerUtil;
import org.flowable.form.rest.util.TestServerUtil.TestServer;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.TestCase;


public abstract class BaseSpringRestTestCase extends TestCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSpringRestTestCase.class);


    protected static final String EMPTY_LINE = "\n";
    protected static String SERVER_URL_PREFIX;
    protected static FormRestUrlBuilder URL_BUILDER;
    protected static Server server;
    protected static ApplicationContext appContext;
    protected static CloseableHttpClient client;
    protected String deploymentId;
    protected Throwable exception;
    protected static LinkedList<CloseableHttpResponse> httpResponses = new LinkedList<>();
    protected ObjectMapper objectMapper = new ObjectMapper();

    protected static final FormEngineConfiguration formEngineConfiguration;

    protected static final FormEngine formEngine;

    protected static final FormRepositoryService repositoryService;

    protected static final FormManagementService managementService;

    protected static final FormService formService;

    protected static IdmIdentityService identityService;
    protected static final String USER = "kermit";

    static {

        TestServer testServer = TestServerUtil.createAndStartServer(ApplicationConfiguration.class);
        server = testServer.getServer();
        appContext = testServer.getApplicationContext();
        SERVER_URL_PREFIX = testServer.getServerUrlPrefix();
        URL_BUILDER = FormRestUrlBuilder.usingBaseUrl(SERVER_URL_PREFIX);

        // Lookup services
        formEngine = appContext.getBean("formEngine", FormEngine.class);
        formEngineConfiguration = appContext.getBean(FormEngineConfiguration.class);
        repositoryService = appContext.getBean(FormRepositoryService.class);
        managementService = appContext.getBean(FormManagementService.class);

        formService = appContext.getBean(FormService.class);
        identityService = appContext.getBean(IdmIdentityService.class);

        // Create http client for all tests
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(USER, "kermit");
        provider.setCredentials(AuthScope.ANY, credentials);
        client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();

        // Clean shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {

                if (client != null) {
                    try {
                        client.close();
                    } catch (IOException e) {
                        LOGGER.error("Could not close http client", e);
                    }
                }

                if (server != null && server.isRunning()) {
                    try {
                        server.stop();
                    } catch (Exception e) {
                        LOGGER.error("Error stopping server", e);
                    }
                }
            }
        });
    }

    @Override
    protected void runTest() throws Throwable {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EMPTY_LINE);
            LOGGER.debug("#### START {}.{} ###########################################################", this.getClass().getSimpleName(), getName());
        }

        try {

            super.runTest();

        } catch (AssertionError e) {
            LOGGER.error(EMPTY_LINE);
            LOGGER.error("ASSERTION FAILED: {}", e, e);
            throw e;

        } catch (Throwable e) {
            LOGGER.error(EMPTY_LINE);
            LOGGER.error("EXCEPTION: {}", e, e);
            throw e;

        } finally {
            LOGGER.debug("#### END {}.{} #############################################################", this.getClass().getSimpleName(), getName());
        }
    }

    @Override
    public void runBare() throws Throwable {
        createUsers();
        try {

            deploymentId = FormTestHelper.annotationDeploymentSetUp(formEngine, getClass(), getName());

            super.runBare();

        } catch (AssertionError e) {
            LOGGER.error(EMPTY_LINE);
            LOGGER.error("ASSERTION FAILED: {}", e, e);
            exception = e;
            throw e;

        } catch (Throwable e) {
            LOGGER.error(EMPTY_LINE);
            LOGGER.error("EXCEPTION: {}", e, e);
            exception = e;
            throw e;

        } finally {
            FormTestHelper.annotationDeploymentTearDown(formEngine, deploymentId, getClass(), getName());
            dropUsers();
            FormTestHelper.assertAndEnsureCleanDb(formEngine);
            formEngineConfiguration.getClock().reset();
            closeHttpConnections();
        }
    }

    protected void createUsers() {
        User user = identityService.newUser("kermit");
        user.setFirstName("Kermit");
        user.setLastName("the Frog");
        user.setPassword("kermit");
        identityService.saveUser(user);

        Group group = identityService.newGroup("admin");
        group.setName("Administrators");
        identityService.saveGroup(group);

        identityService.createMembership(user.getId(), group.getId());
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
        identityService.deleteUser("kermit");
        identityService.deleteGroup("admin");
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
            try {
                return URLEncoder.encode(string, "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                throw new IllegalStateException("JVM does not support UTF-8 encoding.", uee);
            }
        }
        return null;
    }

    protected void storeFormInstance(String url, String body) throws IOException {
        HttpPost post = new HttpPost(SERVER_URL_PREFIX + url);
        post.setEntity(new StringEntity(body));
        CloseableHttpResponse response = executeRequest(post, HttpStatus.SC_OK);
        closeResponse(response);
    }

    /**
     * Checks if the returned "data" array (child-node of root-json node returned by invoking a GET on the given url) contains entries with the given ID's.
     */
    protected void assertResultsPresentInDataResponse(String url, String submittedBy, String... expectedResourceIds) throws IOException {
        // Do the actual call
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        // Check status and size
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        // assertThat(dataNode).hasSize(numberOfResultsExpected);

        // Check presence of ID's
        List<String> toBeFound = new ArrayList<>(Arrays.asList(expectedResourceIds));
        for (JsonNode aDataNode : dataNode) {
            String id = aDataNode.get("id").textValue();
            String sb = aDataNode.get("submittedBy").textValue();
            toBeFound.remove(id);
            assertThat(submittedBy).isEqualTo(sb);
        }
        assertThat(toBeFound)
                .as("Not all expected ids have been found in result, missing: " + StringUtils.join(toBeFound, ", "))
                .isEmpty();
    }

    public CloseableHttpResponse executeRequest(HttpUriRequest request, int expectedStatusCode) {
        return internalExecuteRequest(request, expectedStatusCode, true);
    }

}
