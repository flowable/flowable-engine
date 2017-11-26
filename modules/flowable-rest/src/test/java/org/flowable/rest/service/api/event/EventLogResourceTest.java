package org.flowable.rest.service.api.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.flowable.engine.event.EventLogEntry;
import org.flowable.engine.impl.event.logger.EventLogger;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.rest.service.api.management.EventLogResource;
import org.flowable.task.api.Task;
import org.hamcrest.CoreMatchers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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
        databaseEventLogger = new EventLogger(processEngineConfiguration.getClock(), processEngineConfiguration.getObjectMapper(), null);
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

    public void testInsertEventLogEntry() throws IOException {
        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_EVENT_LOG);

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("type", "testType");
        requestNode.put("processInstanceId", "314");
        ObjectNode data = requestNode.putObject("data");
        data.put("testDataName", "testDataValue");


        // Do the actual call
        HttpPut put = new HttpPut(SERVER_URL_PREFIX + url);
        put.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(put, 200);

        // Check status and size
        closeResponse(response);

        List<EventLogEntry> testEventLogEntries = managementService.getEventLogEntriesByProcessInstanceId("314");
        assertThat(testEventLogEntries.size(), is(1));
        EventLogEntry eventLogEntry = testEventLogEntries.get(0);
        assertThat(eventLogEntry.getType(), is("testType"));
        Map<String, Object> dataMap = objectMapper.readValue(eventLogEntry.getData(), Map.class);
        assertTrue(dataMap.containsKey("testDataName"));
        assertThat(dataMap.get("testDataName"), CoreMatchers.<Object>is("testDataValue"));
    }

}
