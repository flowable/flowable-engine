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
package org.flowable.rest.service;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

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
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.db.DbSchemaManager;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.rest.util.RestUrlBuilder;
import org.flowable.engine.DynamicBpmnService;
import org.flowable.engine.FormService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.ProcessEngineImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.test.TestHelper;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.User;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.rest.conf.ApplicationConfiguration;
import org.flowable.rest.util.TestServerUtil;
import org.flowable.rest.util.TestServerUtil.TestServer;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;

import junit.framework.AssertionFailedError;

public class BaseSpringRestTestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSpringRestTestCase.class);
    
    protected static final List<String> TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK = Arrays.asList(
            "ACT_GE_PROPERTY",
            "ACT_ID_PROPERTY");
    
    @Rule 
    public TestName testName = new TestName();
    
    protected static String SERVER_URL_PREFIX = "";
    protected static RestUrlBuilder URL_BUILDER;

    protected ObjectMapper objectMapper = new ObjectMapper();

    protected String deploymentId;
    protected Throwable exception;

    protected static ProcessEngine processEngine;
    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    protected RepositoryService repositoryService;
    protected RuntimeService runtimeService;
    protected TaskService taskService;
    protected FormService formService;
    protected HistoryService historyService;
    protected IdentityService identityService;
    protected ManagementService managementService;
    protected DynamicBpmnService dynamicBpmnService;

    protected static CloseableHttpClient client;
    protected static LinkedList<CloseableHttpResponse> httpResponses = new LinkedList<>();

    protected ISO8601DateFormat dateFormat = new ISO8601DateFormat();
    
    protected static Server server;
    
    protected static Class<?> configurationClass;
    protected static AnnotationConfigWebApplicationContext appContext;
    
    @Before
    public void init() throws Exception {
        
        if (configurationClass != null && !configurationClass.equals(getConfigurationClass())) {
            if (appContext != null) {
                appContext.close();
            }
            if (server != null && server.isRunning()) {
                try {
                    server.stop();
                    server = null;
                } catch (Exception e) {
                    LOGGER.error("Error stopping server", e);
                    throw e;
                }
            }
        }
        
        if (configurationClass == null || appContext == null || !configurationClass.equals(getConfigurationClass())) {
            appContext = new AnnotationConfigWebApplicationContext();
            appContext.register(getConfigurationClass());
            appContext.refresh();
            configurationClass = getConfigurationClass();
        }
        
        processEngine = appContext.getBean("processEngine", ProcessEngine.class);
        processEngineConfiguration = appContext.getBean(ProcessEngineConfigurationImpl.class);
        repositoryService = appContext.getBean(RepositoryService.class);
        runtimeService = appContext.getBean(RuntimeService.class);
        taskService = appContext.getBean(TaskService.class);
        formService = appContext.getBean(FormService.class);
        historyService = appContext.getBean(HistoryService.class);
        identityService = appContext.getBean(IdentityService.class);
        managementService = appContext.getBean(ManagementService.class);
        dynamicBpmnService = appContext.getBean(DynamicBpmnService.class);
        
        if (server == null) {
            TestServer testServer = TestServerUtil.createAndStartServer(appContext);
            server = testServer.getServer();
            SERVER_URL_PREFIX = testServer.getServerUrlPrefix();
            URL_BUILDER = RestUrlBuilder.usingBaseUrl(SERVER_URL_PREFIX);
        }
        
        createHttpClient();
        createUsers();
        
        deploymentId = TestHelper.annotationDeploymentSetUp(processEngine, getClass(), testName.getMethodName());
    }
    
    protected Class<?> getConfigurationClass() {
        return ApplicationConfiguration.class;
    }

    protected void createHttpClient() {
        
        if (client != null) {
            return;
        }
        
        // Create Http client for all tests
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
    
    @After
    public void cleanup() throws Throwable {
        try {
            TestHelper.annotationDeploymentTearDown(processEngine, deploymentId, getClass(), testName.getMethodName());

        } catch (AssertionFailedError e) {
            LOGGER.error(System.lineSeparator());
            LOGGER.error("ASSERTION FAILED: {}", e, e);
            exception = e;
            throw e;

        } catch (Throwable e) {
            LOGGER.error(System.lineSeparator());
            LOGGER.error("EXCEPTION: {}", e, e);
            exception = e;
            throw e;

        } finally {
            TestHelper.annotationDeploymentTearDown(processEngine, deploymentId, getClass(), testName.getMethodName());
            dropUsers();
            assertAndEnsureCleanDb();
            processEngineConfiguration.getClock().reset();
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
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
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
            Assert.assertNotNull(response.getStatusLine());

            int responseStatusCode = response.getStatusLine().getStatusCode();
            if (expectedStatusCode != responseStatusCode) {
                LOGGER.info("Wrong status code : {}, but should be {}", responseStatusCode, expectedStatusCode);
                LOGGER.info("Response body: {}", IOUtils.toString(response.getEntity().getContent()));
            }

            Assert.assertEquals(expectedStatusCode, responseStatusCode);
            httpResponses.add(response);
            return response;

        } catch (IOException e) {
            fail(e.getMessage(), e);
        }
        return null;
    }

    public void closeResponse(CloseableHttpResponse response) {
        if (response != null) {
            try {
                response.close();
            } catch (IOException e) {
                fail("Could not close http connection", e);
            }
        }
    }

    protected void dropUsers() {
        IdentityService identityService = processEngine.getIdentityService();

        identityService.deleteUser("kermit");
        identityService.deleteGroup("admin");
        identityService.deleteMembership("kermit", "admin");
    }

    /**
     * Each test is assumed to clean up all DB content it entered. After a test method executed, this method scans all tables to see if the DB is completely clean. It throws AssertionFailed in case
     * the DB is not clean. If the DB is not clean, it is cleaned by performing a create a drop.
     */
    protected void assertAndEnsureCleanDb() throws Throwable {
        LOGGER.debug("verifying that db is clean after test");
        Map<String, Long> tableCounts = managementService.getTableCount();
        StringBuilder outputMessage = new StringBuilder();
        for (String tableName : tableCounts.keySet()) {
            String tableNameWithoutPrefix = tableName.replace(processEngineConfiguration.getDatabaseTablePrefix(), "");
            if (!TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK.contains(tableNameWithoutPrefix)) {
                Long count = tableCounts.get(tableName);
                if (count != 0L) {
                    outputMessage.append("  ").append(tableName).append(": ").append(count.toString()).append(" record(s) ");
                }
            }
        }
        if (outputMessage.length() > 0) {
            outputMessage.insert(0, "DB NOT CLEAN: \n");
            LOGGER.error(System.lineSeparator());
            LOGGER.error(outputMessage.toString());

            LOGGER.info("dropping and recreating db");

            CommandExecutor commandExecutor = ((ProcessEngineImpl) processEngine).getProcessEngineConfiguration().getCommandExecutor();
            commandExecutor.execute(new Command<Object>() {
                @Override
                public Object execute(CommandContext commandContext) {
                    DbSchemaManager dbSchemaManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getDbSchemaManager();
                    dbSchemaManager.dbSchemaDrop();
                    dbSchemaManager.dbSchemaCreate();
                    return null;
                }
            });

            if (exception != null) {
                throw exception;
            } else {
                fail(outputMessage.toString());
            }
        } else {
            LOGGER.debug("database was clean");
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
            try {
                return URLEncoder.encode(string, "UTF-8");
            } catch (UnsupportedEncodingException uee) {
                throw new IllegalStateException("JVM does not support UTF-8 encoding.", uee);
            }
        }
        return null;
    }

    public void assertProcessEnded(final String processInstanceId) {
        ProcessInstance processInstance = processEngine.getRuntimeService().createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();

        if (processInstance != null) {
            throw new AssertionFailedError("Expected finished process instance '" + processInstanceId + "' but it was still in the db");
        }
    }

    public void waitForJobExecutorToProcessAllJobs(long maxMillisToWait, long intervalMillis) {
        AsyncExecutor asyncExecutor = processEngineConfiguration.getAsyncExecutor();
        asyncExecutor.start();

        try {
            Timer timer = new Timer();
            InterruptTask task = new InterruptTask(Thread.currentThread());
            timer.schedule(task, maxMillisToWait);
            boolean areJobsAvailable = true;
            try {
                while (areJobsAvailable && !task.isTimeLimitExceeded()) {
                    Thread.sleep(intervalMillis);
                    areJobsAvailable = areJobsAvailable();
                }
            } catch (InterruptedException e) {
            } finally {
                timer.cancel();
            }
            if (areJobsAvailable) {
                throw new FlowableException("time limit of " + maxMillisToWait + " was exceeded");
            }

        } finally {
            asyncExecutor.shutdown();
        }
    }

    public void waitForJobExecutorOnCondition(long maxMillisToWait, long intervalMillis, Callable<Boolean> condition) {
        AsyncExecutor asyncExecutor = processEngineConfiguration.getAsyncExecutor();
        asyncExecutor.start();

        try {
            Timer timer = new Timer();
            InterruptTask task = new InterruptTask(Thread.currentThread());
            timer.schedule(task, maxMillisToWait);
            boolean conditionIsViolated = true;
            try {
                while (conditionIsViolated) {
                    Thread.sleep(intervalMillis);
                    conditionIsViolated = !condition.call();
                }
            } catch (InterruptedException e) {
            } catch (Exception e) {
                throw new FlowableException("Exception while waiting on condition: " + e.getMessage(), e);
            } finally {
                timer.cancel();
            }
            if (conditionIsViolated) {
                throw new FlowableException("time limit of " + maxMillisToWait + " was exceeded");
            }

        } finally {
            asyncExecutor.shutdown();
        }
    }

    public boolean areJobsAvailable() {
        return !managementService.createJobQuery().list().isEmpty();
    }

    private static class InterruptTask extends TimerTask {
        protected boolean timeLimitExceeded;
        protected Thread thread;

        public InterruptTask(Thread thread) {
            this.thread = thread;
        }

        public boolean isTimeLimitExceeded() {
            return timeLimitExceeded;
        }

        @Override
        public void run() {
            timeLimitExceeded = true;
            thread.interrupt();
        }
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
        assertEquals(numberOfResultsExpected, dataNode.size());

        // Check presence of ID's
        List<String> toBeFound = new ArrayList<>(Arrays.asList(expectedResourceIds));
        Iterator<JsonNode> it = dataNode.iterator();
        while (it.hasNext()) {
            String id = it.next().get("id").textValue();
            toBeFound.remove(id);
        }
        assertTrue("Not all expected ids have been found in result, missing: " + StringUtils.join(toBeFound, ", "), toBeFound.isEmpty());
    }

    protected void assertEmptyResultsPresentInDataResponse(String url) throws JsonProcessingException, IOException {
        // Do the actual call
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);

        // Check status and size
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        assertEquals(0, dataNode.size());
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
            assertEquals(numberOfResultsExpected, dataNode.size());

            // Check presence of ID's
            if (expectedResourceIds != null) {
                List<String> toBeFound = new ArrayList<>(Arrays.asList(expectedResourceIds));
                Iterator<JsonNode> it = dataNode.iterator();
                while (it.hasNext()) {
                    String id = it.next().get("id").textValue();
                    toBeFound.remove(id);
                }
                assertTrue("Not all entries have been found in result, missing: " + StringUtils.join(toBeFound, ", "), toBeFound.isEmpty());
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
        return dateFormat.format(time);
    }

    protected String buildUrl(String[] fragments, Object... arguments) {
        return URL_BUILDER.buildUrl(fragments, arguments);
    }
}
