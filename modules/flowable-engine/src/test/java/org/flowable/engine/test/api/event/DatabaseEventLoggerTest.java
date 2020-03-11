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
package org.flowable.engine.test.api.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.event.EventLogEntry;
import org.flowable.engine.impl.event.logger.EventLogger;
import org.flowable.engine.impl.event.logger.handler.Fields;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Joram Barrez
 */
public class DatabaseEventLoggerTest extends PluggableFlowableTestCase {

    protected EventLogger databaseEventLogger;

    protected ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    protected void setUp() throws Exception {

        // Database event logger setup
        databaseEventLogger = new EventLogger(processEngineConfiguration.getClock(), processEngineConfiguration.getObjectMapper());
        runtimeService.addEventListener(databaseEventLogger);
    }

    @AfterEach
    protected void tearDown() throws Exception {

        // Database event logger teardown
        runtimeService.removeEventListener(databaseEventLogger);

    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/event/DatabaseEventLoggerProcess.bpmn20.xml" })
    public void testDatabaseEvents() throws IOException {

        String testTenant = "testTenant";

        String deploymentId = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/api/event/DatabaseEventLoggerProcess.bpmn20.xml")
                .tenantId(testTenant)
                .deploy().getId();

        // Run process to gather data
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKeyAndTenantId("DatabaseEventLoggerProcess",
                CollectionUtil.singletonMap("testVar", "helloWorld"), testTenant);

        // Verify event log entries
        List<EventLogEntry> eventLogEntries = managementService.getEventLogEntries(null, null);

        String processDefinitionId = processInstance.getProcessDefinitionId();
        Iterator<EventLogEntry> iterator = eventLogEntries.iterator();
        while (iterator.hasNext()) {
            EventLogEntry entry = iterator.next();
            if (entry.getProcessDefinitionId() != null && !entry.getProcessDefinitionId().equals(processDefinitionId)) {
                iterator.remove();
            }
        }

        assertThat(eventLogEntries).hasSize(15);

        long lastLogNr = -1;
        for (int i = 0; i < eventLogEntries.size(); i++) {

            EventLogEntry entry = eventLogEntries.get(i);

            if (i == 0) {

                assertThat(entry.getType()).isNotNull();
                assertThat(entry.getType()).isEqualTo(FlowableEngineEventType.VARIABLE_CREATED.name());
                assertThat(entry.getProcessDefinitionId()).isNotNull();
                assertThat(entry.getProcessInstanceId()).isNotNull();
                assertThat(entry.getTimeStamp()).isNotNull();
                assertThat(entry.getTaskId()).isNull();

                Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>() {

                });
                assertThat(data)
                        .containsKeys(
                                Fields.PROCESS_DEFINITION_ID,
                                Fields.PROCESS_INSTANCE_ID,
                                Fields.VALUE_STRING,
                                Fields.TENANT_ID
                        );
                assertThat(data.get(Fields.TENANT_ID)).isEqualTo(testTenant);
            }

            // process instance start
            if (i == 1) {

                assertThat(entry.getType()).isNotNull();
                assertThat(entry.getType()).isEqualTo("PROCESSINSTANCE_START");
                assertThat(entry.getProcessDefinitionId()).isNotNull();
                assertThat(entry.getProcessInstanceId()).isNotNull();
                assertThat(entry.getTimeStamp()).isNotNull();
                assertThat(entry.getExecutionId()).isNull();
                assertThat(entry.getTaskId()).isNull();

                Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>() {

                });
                assertThat(data)
                        .containsKeys(
                                Fields.ID,
                                Fields.PROCESS_DEFINITION_ID,
                                Fields.TENANT_ID
                        );
                assertThat(data.get(Fields.TENANT_ID)).isEqualTo(testTenant);

                Map<String, Object> variableMap = (Map<String, Object>) data.get(Fields.VARIABLES);
                assertThat(variableMap).hasSize(1);
                assertThat(variableMap.get("testVar")).isEqualTo("helloWorld");

                assertThat(data).doesNotContainKey(Fields.NAME);
                assertThat(data).doesNotContainKey(Fields.BUSINESS_KEY);
            }

            // Activity started
            if (i == 2 || i == 5 || i == 8 || i == 12) {
                assertThat(entry.getType()).isNotNull();
                assertThat(entry.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_STARTED.name());
                assertThat(entry.getProcessDefinitionId()).isNotNull();
                assertThat(entry.getProcessInstanceId()).isNotNull();
                assertThat(entry.getTimeStamp()).isNotNull();
                assertThat(entry.getExecutionId()).isNotNull();
                assertThat(entry.getTaskId()).isNull();

                Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>() {

                });
                assertThat(data)
                        .containsKeys(
                                Fields.ACTIVITY_ID,
                                Fields.PROCESS_DEFINITION_ID,
                                Fields.PROCESS_INSTANCE_ID,
                                Fields.EXECUTION_ID,
                                Fields.ACTIVITY_TYPE,
                                Fields.TENANT_ID
                        );
                assertThat(data.get(Fields.TENANT_ID)).isEqualTo(testTenant);
            }

            // Leaving start
            if (i == 3) {

                assertThat(entry.getType()).isNotNull();
                assertThat(entry.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED.name());
                assertThat(entry.getProcessDefinitionId()).isNotNull();
                assertThat(entry.getProcessInstanceId()).isNotNull();
                assertThat(entry.getTimeStamp()).isNotNull();
                assertThat(entry.getExecutionId()).isNotNull();
                assertThat(entry.getTaskId()).isNull();

                Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>() {

                });
                assertThat(data)
                        .containsKeys(
                                Fields.ACTIVITY_ID,
                                Fields.PROCESS_DEFINITION_ID,
                                Fields.PROCESS_INSTANCE_ID,
                                Fields.EXECUTION_ID,
                                Fields.ACTIVITY_TYPE,
                                Fields.TENANT_ID
                        );
                assertThat(data.get(Fields.ACTIVITY_ID)).isEqualTo("startEvent1");
                assertThat(data.get(Fields.TENANT_ID)).isEqualTo(testTenant);
            }

            // Sequence flow taken
            if (i == 4 || i == 7 || i == 11) {
                assertThat(entry.getType()).isNotNull();
                assertThat(entry.getType()).isEqualTo(FlowableEngineEventType.SEQUENCEFLOW_TAKEN.name());
                assertThat(entry.getProcessDefinitionId()).isNotNull();
                assertThat(entry.getProcessInstanceId()).isNotNull();
                assertThat(entry.getTimeStamp()).isNotNull();
                assertThat(entry.getExecutionId()).isNotNull();
                assertThat(entry.getTaskId()).isNull();

                Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>() {

                });
                assertThat(data)
                        .containsKeys(
                                Fields.ID,
                                Fields.SOURCE_ACTIVITY_ID,
                                Fields.SOURCE_ACTIVITY_TYPE,
                                Fields.SOURCE_ACTIVITY_TYPE,
                                Fields.TENANT_ID
                        );
                assertThat(data.get(Fields.TENANT_ID)).isEqualTo(testTenant);
            }

            // Leaving parallel gateway
            if (i == 6) {

                assertThat(entry.getType()).isNotNull();
                assertThat(entry.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED.name());
                assertThat(entry.getProcessDefinitionId()).isNotNull();
                assertThat(entry.getProcessInstanceId()).isNotNull();
                assertThat(entry.getTimeStamp()).isNotNull();
                assertThat(entry.getExecutionId()).isNotNull();
                assertThat(entry.getTaskId()).isNull();

                Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>() {

                });
                assertThat(data)
                        .containsKeys(
                                Fields.ACTIVITY_ID,
                                Fields.PROCESS_DEFINITION_ID,
                                Fields.PROCESS_INSTANCE_ID,
                                Fields.EXECUTION_ID,
                                Fields.ACTIVITY_TYPE,
                                Fields.TENANT_ID
                        );
            }

            // Tasks
            if (i == 10 || i == 14) {

                assertThat(entry.getType()).isNotNull();
                assertThat(entry.getType()).isEqualTo(FlowableEngineEventType.TASK_CREATED.name());
                assertThat(entry.getTimeStamp()).isNotNull();
                assertThat(entry.getProcessDefinitionId()).isNotNull();
                assertThat(entry.getProcessInstanceId()).isNotNull();
                assertThat(entry.getExecutionId()).isNotNull();
                assertThat(entry.getTaskId()).isNotNull();

                Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>() {

                });
                assertThat(data)
                        .containsKeys(
                                Fields.ID,
                                Fields.NAME,
                                Fields.ASSIGNEE,
                                Fields.CREATE_TIME,
                                Fields.PRIORITY,
                                Fields.PROCESS_DEFINITION_ID,
                                Fields.EXECUTION_ID,
                                Fields.TENANT_ID
                        );
                assertThat(data)
                        .doesNotContainKeys(
                                Fields.DESCRIPTION,
                                Fields.CATEGORY,
                                Fields.OWNER,
                                Fields.DUE_DATE,
                                Fields.FORM_KEY,
                                Fields.USER_ID
                        );

                assertThat(data.get(Fields.TENANT_ID)).isEqualTo(testTenant);

            }

            if (i == 9 || i == 13) {

                assertThat(entry.getType()).isNotNull();
                assertThat(entry.getType()).isEqualTo(FlowableEngineEventType.TASK_ASSIGNED.name());
                assertThat(entry.getTimeStamp()).isNotNull();
                assertThat(entry.getProcessDefinitionId()).isNotNull();
                assertThat(entry.getProcessInstanceId()).isNotNull();
                assertThat(entry.getExecutionId()).isNotNull();
                assertThat(entry.getTaskId()).isNotNull();

                Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>() {

                });
                assertThat(data)
                        .containsKeys(
                                Fields.ID,
                                Fields.NAME,
                                Fields.ASSIGNEE,
                                Fields.CREATE_TIME,
                                Fields.PRIORITY,
                                Fields.PROCESS_DEFINITION_ID,
                                Fields.EXECUTION_ID,
                                Fields.TENANT_ID
                        );
                assertThat(data)
                        .doesNotContainKeys(
                                Fields.DESCRIPTION,
                                Fields.CATEGORY,
                                Fields.OWNER,
                                Fields.DUE_DATE,
                                Fields.FORM_KEY,
                                Fields.USER_ID
                        );

                assertThat(data.get(Fields.TENANT_ID)).isEqualTo(testTenant);
            }

            lastLogNr = entry.getLogNumber();
        }

        // Completing two tasks
        for (org.flowable.task.api.Task task : taskService.createTaskQuery().list()) {
            Authentication.setAuthenticatedUserId(task.getAssignee());
            Map<String, Object> varMap = new HashMap<>();
            varMap.put("test", "test");
            taskService.complete(task.getId(), varMap);
            Authentication.setAuthenticatedUserId(null);
        }

        // Verify events
        eventLogEntries = managementService.getEventLogEntries(lastLogNr, 100L);
        assertThat(eventLogEntries).hasSize(17);

        for (int i = 0; i < eventLogEntries.size(); i++) {

            EventLogEntry entry = eventLogEntries.get(i);

            // org.flowable.task.service.Task completion
            if (i == 1 || i == 6) {
                assertThat(entry.getType()).isNotNull();
                assertThat(entry.getType()).isEqualTo(FlowableEngineEventType.TASK_COMPLETED.name());
                assertThat(entry.getProcessDefinitionId()).isNotNull();
                assertThat(entry.getProcessInstanceId()).isNotNull();
                assertThat(entry.getExecutionId()).isNotNull();
                assertThat(entry.getTaskId()).isNotNull();

                Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>() {

                });
                assertThat(data)
                        .containsKeys(
                                Fields.ID,
                                Fields.NAME,
                                Fields.ASSIGNEE,
                                Fields.CREATE_TIME,
                                Fields.PRIORITY,
                                Fields.PROCESS_DEFINITION_ID,
                                Fields.EXECUTION_ID,
                                Fields.TENANT_ID,
                                Fields.USER_ID
                        );

                Map<String, Object> variableMap = (Map<String, Object>) data.get(Fields.VARIABLES);
                assertThat(variableMap).hasSize(1);
                assertThat(variableMap.get("test")).isEqualTo("test");

                assertThat(data)
                        .doesNotContainKeys(
                                Fields.DESCRIPTION,
                                Fields.CATEGORY,
                                Fields.OWNER,
                                Fields.DUE_DATE,
                                Fields.FORM_KEY
                        );

                assertThat(data.get(Fields.TENANT_ID)).isEqualTo(testTenant);

            }

            // Activity Completed
            if (i == 2 || i == 7 || i == 10 || i == 13) {
                assertThat(entry.getType()).isNotNull();
                assertThat(entry.getType()).isEqualTo(FlowableEngineEventType.ACTIVITY_COMPLETED.name());
                assertThat(entry.getProcessDefinitionId()).isNotNull();
                assertThat(entry.getProcessInstanceId()).isNotNull();
                assertThat(entry.getTimeStamp()).isNotNull();
                assertThat(entry.getExecutionId()).isNotNull();
                assertThat(entry.getTaskId()).isNull();

                Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>() {

                });
                assertThat(data)
                        .containsKeys(
                                Fields.ACTIVITY_ID,
                                Fields.PROCESS_DEFINITION_ID,
                                Fields.PROCESS_INSTANCE_ID,
                                Fields.EXECUTION_ID,
                                Fields.ACTIVITY_TYPE,
                                Fields.TENANT_ID,
                                Fields.BEHAVIOR_CLASS
                        );

                assertThat(data.get(Fields.TENANT_ID)).isEqualTo(testTenant);

                if (i == 2) {
                    assertThat(data.get(Fields.ACTIVITY_TYPE)).isEqualTo("userTask");
                } else if (i == 7) {
                    assertThat(data.get(Fields.ACTIVITY_TYPE)).isEqualTo("userTask");
                } else if (i == 10) {
                    assertThat(data.get(Fields.ACTIVITY_TYPE)).isEqualTo("parallelGateway");
                } else if (i == 13) {
                    assertThat(data.get(Fields.ACTIVITY_TYPE)).isEqualTo("endEvent");
                }

            }

            // Sequence flow taken
            if (i == 3 || i == 8 || i == 11) {
                assertThat(entry.getType()).isNotNull();
                assertThat(FlowableEngineEventType.SEQUENCEFLOW_TAKEN.name()).isEqualTo(entry.getType());
                assertThat(entry.getProcessDefinitionId()).isNotNull();
                assertThat(entry.getProcessInstanceId()).isNotNull();
                assertThat(entry.getTimeStamp()).isNotNull();
                assertThat(entry.getExecutionId()).isNotNull();
                assertThat(entry.getTaskId()).isNull();

                Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>() {

                });
                assertThat(data)
                        .containsKeys(
                                Fields.ID,
                                Fields.SOURCE_ACTIVITY_ID,
                                Fields.SOURCE_ACTIVITY_TYPE,
                                Fields.SOURCE_ACTIVITY_BEHAVIOR_CLASS,
                                Fields.TARGET_ACTIVITY_ID,
                                Fields.TARGET_ACTIVITY_TYPE,
                                Fields.TARGET_ACTIVITY_BEHAVIOR_CLASS
                        );

                assertThat(data.get(Fields.TENANT_ID)).isEqualTo(testTenant);
            }

            if (i == 14 || i == 15) {
                assertThat(entry.getType()).isNotNull();
                assertThat(entry.getType()).isEqualTo("VARIABLE_DELETED");
                assertThat(entry.getProcessDefinitionId()).isNotNull();
                assertThat(entry.getProcessInstanceId()).isNotNull();
                assertThat(entry.getTimeStamp()).isNotNull();
                assertThat(entry.getExecutionId()).isNotNull();
                assertThat(entry.getTaskId()).isNull();
            }

            if (i == 16) {
                assertThat(entry.getType()).isNotNull();
                assertThat(entry.getType()).isEqualTo("PROCESSINSTANCE_END");
                assertThat(entry.getProcessDefinitionId()).isNotNull();
                assertThat(entry.getProcessInstanceId()).isNotNull();
                assertThat(entry.getTimeStamp()).isNotNull();
                assertThat(entry.getExecutionId()).isNull();
                assertThat(entry.getTaskId()).isNull();

                Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>() {

                });
                assertThat(data)
                        .containsKeys(
                                Fields.ID,
                                Fields.PROCESS_DEFINITION_ID,
                                Fields.TENANT_ID
                        );
                assertThat(data)
                        .doesNotContainKeys(
                                Fields.NAME,
                                Fields.BUSINESS_KEY
                        );

                assertThat(data.get(Fields.TENANT_ID)).isEqualTo(testTenant);
            }
        }

        // Cleanup
        for (EventLogEntry eventLogEntry : managementService.getEventLogEntries(null, null)) {
            managementService.deleteEventLogEntry(eventLogEntry.getLogNumber());
        }

        repositoryService.deleteDeployment(deploymentId, true);

    }

    @Test
    public void testDatabaseEventsNoTenant() throws IOException {

        String deploymentId = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/api/event/DatabaseEventLoggerProcess.bpmn20.xml").deploy().getId();

        // Run process to gather data
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("DatabaseEventLoggerProcess", CollectionUtil.singletonMap("testVar", "helloWorld"));

        // Verify event log entries
        List<EventLogEntry> eventLogEntries = managementService.getEventLogEntries(null, null);

        String processDefinitionId = processInstance.getProcessDefinitionId();
        Iterator<EventLogEntry> iterator = eventLogEntries.iterator();
        while (iterator.hasNext()) {
            EventLogEntry entry = iterator.next();
            if (entry.getProcessDefinitionId() != null && !entry.getProcessDefinitionId().equals(processDefinitionId)) {
                iterator.remove();
            }
        }

        assertThat(eventLogEntries).hasSize(15);

        for (int i = 0; i < eventLogEntries.size(); i++) {

            EventLogEntry entry = eventLogEntries.get(i);

            if (i == 0) {
                assertThat(FlowableEngineEventType.VARIABLE_CREATED.name()).isEqualTo(entry.getType());
                Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>() {

                });
                assertThat(data.get(Fields.TENANT_ID)).isNull();
            }

            // process instance start
            if (i == 1) {
                assertThat(entry.getType()).isEqualTo("PROCESSINSTANCE_START");
                Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>() {

                });
                assertThat(data.get(Fields.TENANT_ID)).isNull();
            }

            // Activity started
            if (i == 2 || i == 5 || i == 8 || i == 12) {
                assertThat(FlowableEngineEventType.ACTIVITY_STARTED.name()).isEqualTo(entry.getType());
                Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>() {

                });
                assertThat(data.get(Fields.TENANT_ID)).isNull();
            }

            // Leaving start
            if (i == 3) {
                assertThat(FlowableEngineEventType.ACTIVITY_COMPLETED.name()).isEqualTo(entry.getType());
                Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>() {

                });
                assertThat(data.get(Fields.TENANT_ID)).isNull();
            }

            // Sequence flow taken
            if (i == 4 || i == 7 || i == 11) {
                assertThat(FlowableEngineEventType.SEQUENCEFLOW_TAKEN.name()).isEqualTo(entry.getType());
                Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>() {

                });
                assertThat(data.get(Fields.TENANT_ID)).isNull();
            }

            // Leaving parallel gateway
            if (i == 6) {
                assertThat(FlowableEngineEventType.ACTIVITY_COMPLETED.name()).isEqualTo(entry.getType());
                Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>() {

                });
                assertThat(data.get(Fields.TENANT_ID)).isNull();
            }

            // Tasks
            if (i == 10 || i == 14) {
                assertThat(FlowableEngineEventType.TASK_CREATED.name()).isEqualTo(entry.getType());
                Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>() {

                });
                assertThat(data.get(Fields.TENANT_ID)).isNull();
            }

            if (i == 9 || i == 13) {
                assertThat(FlowableEngineEventType.TASK_ASSIGNED.name()).isEqualTo(entry.getType());
                Map<String, Object> data = objectMapper.readValue(entry.getData(), new TypeReference<HashMap<String, Object>>() {

                });
                assertThat(data.get(Fields.TENANT_ID)).isNull();
            }

        }

        repositoryService.deleteDeployment(deploymentId, true);

        // Cleanup
        for (EventLogEntry eventLogEntry : managementService.getEventLogEntries(null, null)) {
            managementService.deleteEventLogEntry(eventLogEntry.getLogNumber());
        }

    }

    @Test
    public void testStandaloneTaskEvents() throws JsonParseException, JsonMappingException, IOException {

        org.flowable.task.api.Task task = taskService.newTask();
        task.setAssignee("kermit");
        task.setTenantId("myTenant");
        taskService.saveTask(task);

        List<EventLogEntry> events = managementService.getEventLogEntries(null, null);
        assertThat(events).hasSize(2);
        assertThat(events.get(0).getType()).isEqualTo("TASK_ASSIGNED");
        assertThat(events.get(1).getType()).isEqualTo("TASK_CREATED");

        for (EventLogEntry eventLogEntry : events) {
            Map<String, Object> data = objectMapper.readValue(eventLogEntry.getData(), new TypeReference<HashMap<String, Object>>() {

            });
            assertThat(data.get(Fields.TENANT_ID)).isEqualTo("myTenant");
        }

        // Cleanup
        taskService.deleteTask(task.getId(), true);
        for (EventLogEntry eventLogEntry : managementService.getEventLogEntries(null, null)) {
            managementService.deleteEventLogEntry(eventLogEntry.getLogNumber());
        }

    }

}
