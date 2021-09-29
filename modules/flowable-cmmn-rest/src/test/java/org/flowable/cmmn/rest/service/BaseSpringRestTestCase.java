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
package org.flowable.cmmn.rest.service;

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
import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.test.impl.CmmnTestHelper;
import org.flowable.cmmn.rest.conf.ApplicationConfiguration;
import org.flowable.cmmn.rest.service.api.RestUrlBuilder;
import org.flowable.cmmn.rest.util.TestServerUtil;
import org.flowable.cmmn.rest.util.TestServerUtil.TestServer;
import org.flowable.common.engine.impl.db.SchemaManager;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.test.EnsureCleanDbUtils;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.User;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import junit.framework.TestCase;

public abstract class BaseSpringRestTestCase extends TestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSpringRestTestCase.class);
    
    protected static final String EMPTY_LINE = "\n";
    protected static final List<String> TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK = Arrays.asList(
        "ACT_GE_PROPERTY",
        "ACT_ID_PROPERTY",
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

    protected static CmmnEngine cmmnEngine;

    protected String deploymentId;
    protected Throwable exception;

    protected static CmmnEngineConfiguration cmmnEngineConfiguration;
    protected static CmmnRepositoryService repositoryService;
    protected static CmmnRuntimeService runtimeService;
    protected static CmmnTaskService taskService;
    protected static CmmnHistoryService historyService;
    protected static CmmnManagementService managementService;
    protected static IdmIdentityService identityService;
    protected static FormRepositoryService formRepositoryService;
    protected static org.flowable.form.api.FormService formEngineFormService;

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
        cmmnEngine = appContext.getBean("cmmnEngine", CmmnEngine.class);
        cmmnEngineConfiguration = appContext.getBean(CmmnEngineConfiguration.class);
        repositoryService = appContext.getBean(CmmnRepositoryService.class);
        runtimeService = appContext.getBean(CmmnRuntimeService.class);
        taskService = appContext.getBean(CmmnTaskService.class);
        historyService = appContext.getBean(CmmnHistoryService.class);
        managementService = appContext.getBean(CmmnManagementService.class);
        identityService = appContext.getBean(IdmIdentityService.class);
        formRepositoryService = appContext.getBean(FormRepositoryService.class);
        formEngineFormService = appContext.getBean(org.flowable.form.api.FormService.class);

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
        createUsers();

        try {

            deploymentId = CmmnTestHelper.annotationDeploymentSetUp(cmmnEngine, getClass(), getName());

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
            Authentication.setAuthenticatedUserId(null);
            CmmnTestHelper.annotationDeploymentTearDown(cmmnEngine, deploymentId, getClass(), getName());
            dropUsers();
            assertAndEnsureCleanDb();
            cmmnEngineConfiguration.getClock().reset();
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
        identityService.deleteUser("kermit");
        identityService.deleteGroup("admin");
    }

    /**
     * Each test is assumed to clean up all DB content it entered. After a test method executed, this method scans all tables to see if the DB is completely clean. It throws AssertionFailed in case
     * the DB is not clean. If the DB is not clean, it is cleaned by performing a create a drop.
     */
    protected void assertAndEnsureCleanDb() throws Throwable {
        EnsureCleanDbUtils.assertAndEnsureCleanDb(
                getName(),
                LOGGER,
                cmmnEngineConfiguration,
                TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK,
                exception == null,
                commandContext -> {
                    SchemaManager schemaManager = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getSchemaManager();
                    schemaManager.schemaDrop();
                    schemaManager.schemaCreate();
                    return null;
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

    public void assertCaseEnded(final String caseInstanceId) {
        CaseInstance caseInstance = cmmnEngine.getCmmnRuntimeService().createCaseInstanceQuery().caseInstanceId(caseInstanceId).singleResult();

        if (caseInstance != null) {
            throw new AssertionError("Expected finished case instance '" + caseInstanceId + "' but it was still in the db");
        }
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
        assertThat(dataNode).hasSize(numberOfResultsExpected);

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
            assertThat(dataNode).hasSize(numberOfResultsExpected);

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

    protected String getISODateStringWithTZ(Date date) {
        if (date == null) {
            return null;
        }
        return ISODateTimeFormat.dateTime().print(new DateTime(date));
    }

    protected String buildUrl(String[] fragments, Object... arguments) {
        return URL_BUILDER.buildUrl(fragments, arguments);
    }
}
