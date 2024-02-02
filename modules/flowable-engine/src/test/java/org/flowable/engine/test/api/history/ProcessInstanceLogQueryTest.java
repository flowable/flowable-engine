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
package org.flowable.engine.test.api.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.history.HistoricData;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricVariableUpdate;
import org.flowable.engine.history.ProcessInstanceHistoryLog;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.task.Comment;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class ProcessInstanceLogQueryTest extends PluggableFlowableTestCase {

    protected String processInstanceId;

    @BeforeEach
    protected void setUp() throws Exception {

        // Deploy test process
        deployTwoTasksTestProcess();

        // Start process instance
        Map<String, Object> vars = new HashMap<>();
        vars.put("var1", "Hello");
        vars.put("var2", 123);
        this.processInstanceId = runtimeService.startProcessInstanceByKey("twoTasksProcess", vars).getId();

        // Add some comments
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            taskService.addComment(null, processInstanceId, "Hello World");
            taskService.addComment(null, processInstanceId, "Hello World2");
            taskService.addComment(null, processInstanceId, "Hello World3");
        }

        // Change some variables
        runtimeService.setVariable(processInstanceId, "var1", "new Value");

        // Finish tasks
        for (org.flowable.task.api.Task task : taskService.createTaskQuery().list()) {
            taskService.complete(task.getId());
        }

        waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
    }

    @AfterEach
    protected void tearDown() throws Exception {

        for (Comment comment : taskService.getProcessInstanceComments(processInstanceId)) {
            taskService.deleteComment(comment.getId());
        }

    }

    @Test
    public void testBaseProperties() {
        ProcessInstanceHistoryLog log = historyService.createProcessInstanceHistoryLogQuery(processInstanceId).singleResult();
        assertThat(log.getId()).isNotNull();
        assertThat(log.getProcessDefinitionId()).isNotNull();
        assertThat(log.getStartActivityId()).isNotNull();
        assertThat(log.getDurationInMillis()).isNotNull();
        assertThat(log.getEndTime()).isNotNull();
        assertThat(log.getStartTime()).isNotNull();
    }

    @Test
    public void testIncludeTasks() {
        ProcessInstanceHistoryLog log = historyService.createProcessInstanceHistoryLogQuery(processInstanceId).includeTasks().singleResult();
        List<HistoricData> events = log.getHistoricData();
        assertThat(events).hasSize(2);

        for (HistoricData event : events) {
            assertThat(event).isInstanceOf(HistoricTaskInstance.class);
        }
    }

    @Test
    public void testIncludeComments() {
        ProcessInstanceHistoryLog log = historyService.createProcessInstanceHistoryLogQuery(processInstanceId).includeComments().singleResult();
        List<HistoricData> events = log.getHistoricData();
        assertThat(events).hasSize(3);

        for (HistoricData event : events) {
            assertThat(event).isInstanceOf(Comment.class);
        }
    }

    @Test
    public void testIncludeTasksAndComments() {
        ProcessInstanceHistoryLog log = historyService.createProcessInstanceHistoryLogQuery(processInstanceId).includeTasks().includeComments().singleResult();
        List<HistoricData> events = log.getHistoricData();
        assertThat(events).hasSize(5);

        int taskCounter = 0;
        int commentCounter = 0;
        for (int i = 0; i < 5; i++) {
            HistoricData event = events.get(i);
            if (event instanceof HistoricTaskInstance) {
                taskCounter++;
            } else if (event instanceof Comment) {
                commentCounter++;
            }
        }

        assertThat(taskCounter).isEqualTo(2);
        assertThat(commentCounter).isEqualTo(3);
    }

    @Test
    public void testIncludeActivities() {
        ProcessInstanceHistoryLog log = historyService.createProcessInstanceHistoryLogQuery(processInstanceId).includeActivities().singleResult();
        List<HistoricData> events = log.getHistoricData();
        assertThat(events).hasSize(9);

        for (HistoricData event : events) {
            assertThat(event).isInstanceOf(HistoricActivityInstance.class);
        }
    }

    @Test
    public void testIncludeVariables() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            ProcessInstanceHistoryLog log = historyService.createProcessInstanceHistoryLogQuery(processInstanceId).includeVariables().singleResult();
            List<HistoricData> events = log.getHistoricData();
            assertThat(events).hasSize(2);

            for (HistoricData event : events) {
                assertThat(event).isInstanceOf(HistoricVariableInstance.class);
            }
        }
    }

    @Test
    public void testIncludeVariableUpdates() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            ProcessInstanceHistoryLog log = historyService.createProcessInstanceHistoryLogQuery(processInstanceId).includeVariableUpdates().singleResult();
            List<HistoricData> events = log.getHistoricData();
            assertThat(events).hasSize(3);

            for (HistoricData event : events) {
                assertThat(event).isInstanceOf(HistoricVariableUpdate.class);
            }
        }
    }

    @Test
    public void testEverything() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            ProcessInstanceHistoryLog log = historyService.createProcessInstanceHistoryLogQuery(processInstanceId).includeTasks().includeActivities()
                    .includeComments().includeVariables()
                    .includeVariableUpdates().singleResult();
            List<HistoricData> events = log.getHistoricData();
            assertThat(events).hasSize(19);
        }
    }

}
