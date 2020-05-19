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
package org.flowable.app.rest.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
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
import org.flowable.app.api.AppManagementService;
import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.engine.AppEngine;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.engine.impl.util.CommandContextUtil;
import org.flowable.app.engine.test.impl.AppTestHelper;
import org.flowable.app.rest.conf.ApplicationConfiguration;
import org.flowable.app.rest.util.TestServerUtil;
import org.flowable.app.rest.util.TestServerUtil.TestServer;
import org.flowable.common.engine.impl.db.SchemaManager;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.test.EnsureCleanDbUtils;
import org.flowable.common.rest.util.RestUrlBuilder;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import junit.framework.TestCase;

public class BaseSpringRestTestCase extends TestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSpringRestTestCase.class);
    
    protected static final String EMPTY_LINE = "\n";
    protected static final List<String> TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK = Arrays.asList(
                    "ACT_GE_PROPERTY",
                    "ACT_ID_PROPERTY",
                    "ACT_APP_DATABASECHANGELOG",
                    "ACT_APP_DATABASECHANGELOGLOCK",
                    "ACT_CMMN_DATABASECHANGELOG",
                    "ACT_CMMN_DATABASECHANGELOGLOCK",
                    "ACT_FO_DATABASECHANGELOG",
                    "ACT_FO_DATABASECHANGELOGLOCK",
                    "FLW_EV_DATABASECHANGELOG",
                    "FLW_EV_DATABASECHANGELOGLOCK"
    );

    protected static String SERVER_URL_PREFIX;
    protected static RestUrlBuilder URL_BUILDER;

    protected static Server server;
    protected static ApplicationContext appContext;
    protected ObjectMapper objectMapper = new ObjectMapper();

    protected static AppEngine appEngine;

    protected String deploymentId;
    protected Throwable exception;

    protected static AppEngineConfiguration appEngineConfiguration;
    protected static AppRepositoryService repositoryService;
    protected static AppManagementService managementService;

    protected static CloseableHttpClient client;
    protected static LinkedList<CloseableHttpResponse> httpResponses = new LinkedList<>();

    protected DateFormat longDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    static {

        TestServer testServer = TestServerUtil.createAndStartServer(ApplicationConfiguration.class);
        server = testServer.getServer();
        appContext = testServer.getApplicationContext();
        SERVER_URL_PREFIX = testServer.getServerUrlPrefix();
        URL_BUILDER = RestUrlBuilder.usingBaseUrl(SERVER_URL_PREFIX);

        // Lookup services
        appEngine = appContext.getBean("appEngine", AppEngine.class);
        appEngineConfiguration = appContext.getBean(AppEngineConfiguration.class);
        repositoryService = appContext.getBean(AppRepositoryService.class);
        managementService = appContext.getBean(AppManagementService.class);
        
        // Create http client for all tests
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("kermit", "kermit");
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
        try {

            deploymentId = AppTestHelper.annotationDeploymentSetUp(appEngine, getClass(), getName());

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
            AppTestHelper.annotationDeploymentTearDown(appEngine, deploymentId, getClass(), getName());
            assertAndEnsureCleanDb();
            appEngineConfiguration.getClock().reset();
            closeHttpConnections();
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

    /**
     * Each test is assumed to clean up all DB content it entered. After a test method executed, this method scans all tables to see if the DB is completely clean. It throws AssertionFailed in case
     * the DB is not clean. If the DB is not clean, it is cleaned by performing a create a drop.
     */
    protected void assertAndEnsureCleanDb() throws Throwable {
        EnsureCleanDbUtils.assertAndEnsureCleanDb(
                getName(),
                LOGGER,
                appEngineConfiguration,
                TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK,
                exception == null,
                new Command<Void>() {
                    @Override
                    public Void execute(CommandContext commandContext) {
                        SchemaManager schemaManager = CommandContextUtil.getAppEngineConfiguration(commandContext).getSchemaManager();
                        schemaManager.schemaDrop();
                        schemaManager.schemaCreate();
                        return null;
                    }
                }
        );
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
        assertThat(toBeFound).as("Not all expected ids have been found in result, missing: " + StringUtils.join(toBeFound, ", ")).isEmpty();
    }

    protected void assertEmptyResultsPresentInDataResponse(String url) throws JsonProcessingException, IOException {
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
    protected void assertResultsPresentInPostDataResponse(String url, ObjectNode body, String... expectedResourceIds) throws JsonProcessingException, IOException {
        assertResultsPresentInPostDataResponseWithStatusCheck(url, body, HttpStatus.SC_OK, expectedResourceIds);
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

    /**
     * Extract a date from the given string. Assertion fails when invalid date has been provided.
     */
    protected Date getDateFromISOString(String isoString) {
        DateTimeFormatter dateFormat = ISODateTimeFormat.dateTime();
        try {
            return dateFormat.parseDateTime(isoString).toDate();
        } catch (IllegalArgumentException iae) {
            fail("Illegal date provided: " + isoString);
            return null;
        }
    }

    protected String getISODateString(Date time) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        longDateFormat.setTimeZone(tz);
        return longDateFormat.format(time);
    }

    protected String buildUrl(String[] fragments, Object... arguments) {
        return URL_BUILDER.buildUrl(fragments, arguments);
    }
}
