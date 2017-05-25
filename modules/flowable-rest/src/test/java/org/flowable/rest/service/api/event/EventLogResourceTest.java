package org.flowable.rest.service.api.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.engine.impl.event.logger.EventLogger;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Task;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.rest.service.api.management.EventLogResource;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * This class tests {@link EventLogResource} implementation
 *
 * @author martin.grofcik
 */
public class EventLogResourceTest extends BaseSpringRestTestCase {

    protected EventLogger databaseEventLogger;

    protected ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Database event logger setup
        databaseEventLogger = new EventLogger(processEngineConfiguration.getClock(), processEngineConfiguration.getObjectMapper());
        runtimeService.addEventListener(databaseEventLogger);
    }

    @Override
    protected void tearDown() throws Exception {

        // Database event logger teardown
        runtimeService.removeEventListener(databaseEventLogger);
        int eventLogEntriesSize = managementService.getEventLogEntries(0L, 100L).size();
        for(int i=0; i<eventLogEntriesSize; ) {
            managementService.deleteEventLogEntry(++i);
        }
        super.tearDown();
    }

    @Deployment(resources = {"org/flowable/rest/service/api/oneTaskProcess.bpmn20.xml"})
    public void testEventLogQueries() throws IOException {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_EVENT_LOG);

        // Do the actual call
        HttpGet get = new HttpGet(SERVER_URL_PREFIX + url +"/" + processInstance.getId());
        CloseableHttpResponse response = executeRequest(get, 200);

        // Check status and size
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent());
        closeResponse(response);

        assertThat(dataNode.size(), is(12));
    }

    public void testEmptyProcessInstanceId() {
        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_EVENT_LOG);

        // Do the actual call
        HttpGet get = new HttpGet(SERVER_URL_PREFIX + url + "/");
        CloseableHttpResponse response = executeRequest(get, 404);

        // Check status and size
        closeResponse(response);
    }
}
