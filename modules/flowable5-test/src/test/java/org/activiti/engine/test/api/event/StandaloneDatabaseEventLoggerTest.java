package org.activiti.engine.test.api.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.event.EventLogEntry;
import org.flowable.engine.impl.event.logger.EventLogger;
import org.flowable.engine.impl.event.logger.handler.Fields;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Joram Barrez
 */
public class StandaloneDatabaseEventLoggerTest extends PluggableFlowableTestCase {

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

        super.tearDown();
    }

    public void testStandaloneTaskEvents() throws Exception {

        org.flowable.task.api.Task task = taskService.newTask();
        task.setAssignee("kermit");
        task.setTenantId("myTenant");
        taskService.saveTask(task);

        List<EventLogEntry> events = managementService.getEventLogEntries(null, null);
        assertEquals(2, events.size());
        assertEquals("TASK_ASSIGNED", events.get(0).getType());
        assertEquals("TASK_CREATED", events.get(1).getType());

        for (EventLogEntry eventLogEntry : events) {
            Map<String, Object> data = objectMapper.readValue(eventLogEntry.getData(), new TypeReference<HashMap<String, Object>>() {
            });
            assertEquals("myTenant", data.get(Fields.TENANT_ID));
        }

        // Cleanup
        taskService.deleteTask(task.getId(), true);
        for (EventLogEntry eventLogEntry : managementService.getEventLogEntries(null, null)) {
            managementService.deleteEventLogEntry(eventLogEntry.getLogNumber());
        }

    }

}
