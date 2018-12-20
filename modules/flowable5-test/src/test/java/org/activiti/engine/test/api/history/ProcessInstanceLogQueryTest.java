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
package org.activiti.engine.test.api.history;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.common.engine.api.history.HistoricData;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricVariableUpdate;
import org.flowable.engine.history.ProcessInstanceHistoryLog;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.task.Comment;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;

/**
 * @author Joram Barrez
 */
public class ProcessInstanceLogQueryTest extends PluggableFlowableTestCase {

    protected String processInstanceId;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Deploy test process
        deployTwoTasksTestProcess();

        // Start process instance
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("var1", "Hello");
        vars.put("var2", 123);
        this.processInstanceId = runtimeService.startProcessInstanceByKey("twoTasksProcess", vars).getId();

        // Add some comments
        taskService.addComment(null, processInstanceId, "Hello World");
        taskService.addComment(null, processInstanceId, "Hello World2");
        taskService.addComment(null, processInstanceId, "Hello World3");

        // Change some variables
        runtimeService.setVariable(processInstanceId, "var1", "new Value");

        // Finish tasks
        for (org.flowable.task.api.Task task : taskService.createTaskQuery().list()) {
            taskService.complete(task.getId());
        }
    }

    @Override
    protected void tearDown() throws Exception {

        for (Comment comment : taskService.getProcessInstanceComments(processInstanceId)) {
            taskService.deleteComment(comment.getId());
        }

        super.tearDown();
    }

    public void testBaseProperties() {
        ProcessInstanceHistoryLog log = historyService.createProcessInstanceHistoryLogQuery(processInstanceId).singleResult();
        assertNotNull(log.getId());
        assertNotNull(log.getProcessDefinitionId());
        assertNotNull(log.getStartActivityId());
        assertNotNull(log.getDurationInMillis());
        assertNotNull(log.getEndTime());
        assertNotNull(log.getStartTime());
    }

    public void testIncludeTasks() {
        ProcessInstanceHistoryLog log = historyService.createProcessInstanceHistoryLogQuery(processInstanceId)
                .includeTasks()
                .singleResult();
        List<HistoricData> events = log.getHistoricData();
        assertEquals(2, events.size());

        for (HistoricData event : events) {
            assertTrue(event instanceof HistoricTaskInstance);
        }
    }

    public void testIncludeComments() {
        ProcessInstanceHistoryLog log = historyService.createProcessInstanceHistoryLogQuery(processInstanceId)
                .includeComments()
                .singleResult();
        List<HistoricData> events = log.getHistoricData();
        assertEquals(3, events.size());

        for (HistoricData event : events) {
            assertTrue(event instanceof Comment);
        }
    }

    public void testIncludeTasksandComments() {
        ProcessInstanceHistoryLog log = historyService.createProcessInstanceHistoryLogQuery(processInstanceId)
                .includeTasks()
                .includeComments()
                .singleResult();
        List<HistoricData> events = log.getHistoricData();
        assertEquals(5, events.size());

        for (int i = 0; i < 5; i++) {
            HistoricData event = events.get(i);
            if (i < 2) { // tasks are created before comments
                assertTrue(event instanceof HistoricTaskInstance);
            } else {
                assertTrue(event instanceof Comment);
            }
        }
    }

    public void testIncludeActivities() {
        ProcessInstanceHistoryLog log = historyService.createProcessInstanceHistoryLogQuery(processInstanceId)
                .includeActivities()
                .singleResult();
        List<HistoricData> events = log.getHistoricData();
        assertEquals(5, events.size());

        for (HistoricData event : events) {
            assertTrue(event instanceof HistoricActivityInstance);
        }
    }

    public void testIncludeVariables() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            ProcessInstanceHistoryLog log = historyService.createProcessInstanceHistoryLogQuery(processInstanceId)
                    .includeVariables()
                    .singleResult();
            List<HistoricData> events = log.getHistoricData();
            assertEquals(2, events.size());

            for (HistoricData event : events) {
                assertTrue(event instanceof HistoricVariableInstance);
            }
        }
    }

    public void testIncludeVariableUpdates() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            ProcessInstanceHistoryLog log = historyService.createProcessInstanceHistoryLogQuery(processInstanceId)
                    .includeVariableUpdates()
                    .singleResult();
            List<HistoricData> events = log.getHistoricData();
            assertEquals(3, events.size());

            for (HistoricData event : events) {
                assertTrue(event instanceof HistoricVariableUpdate);
            }
        }
    }

    public void testEverything() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            ProcessInstanceHistoryLog log = historyService.createProcessInstanceHistoryLogQuery(processInstanceId)
                    .includeTasks()
                    .includeActivities()
                    .includeComments()
                    .includeVariables()
                    .includeVariableUpdates()
                    .singleResult();
            List<HistoricData> events = log.getHistoricData();
            assertEquals(15, events.size());
        }
    }

}
