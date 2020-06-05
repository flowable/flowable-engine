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
 */
package org.flowable.engine.test.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.history.HistoricData;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricVariableUpdate;
import org.flowable.engine.history.ProcessInstanceHistoryLog;
import org.flowable.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Daisuke Yoshimoto
 */
public class ProcessInstanceLogQueryAndByteArrayTypeVariableTest extends PluggableFlowableTestCase {

    protected String processInstanceId;

    private static final String LARGE_STRING_VALUE;

    static {
        StringBuilder sb = new StringBuilder("a");
        for (int i = 0; i < 4001; i++) {
            sb.append('a');
        }
        LARGE_STRING_VALUE = sb.toString();
    }

    @BeforeEach
    protected void setUp() throws Exception {
        // Deploy test process
        deployTwoTasksTestProcess();

        // Start process instance
        Map<String, Object> vars = new HashMap<>();
        // ByteArrayType Variable
        vars.put("var", LARGE_STRING_VALUE);
        this.processInstanceId = runtimeService.startProcessInstanceByKey("twoTasksProcess", vars).getId();

        // Finish tasks
        for (org.flowable.task.api.Task task : taskService.createTaskQuery().list()) {
            taskService.complete(task.getId());
        }
    }

    @Test
    public void testIncludeVariables() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            ProcessInstanceHistoryLog log = historyService.createProcessInstanceHistoryLogQuery(processInstanceId)
                    .includeVariables()
                    .singleResult();
            List<HistoricData> events = log.getHistoricData();
            assertThat(events).hasSize(1);

            for (HistoricData event : events) {
                assertThat(event).isInstanceOf(HistoricVariableInstance.class);
                assertThat(((HistoricVariableInstanceEntity) event).getValue()).isEqualTo(LARGE_STRING_VALUE);
            }
        }
    }

    @Test
    public void testIncludeVariableUpdates() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {

            HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(processInstanceId).variableName("var").singleResult();
            assertThat(historicVariableInstance.getValue()).isEqualTo(LARGE_STRING_VALUE);

            ProcessInstanceHistoryLog log = historyService.createProcessInstanceHistoryLogQuery(processInstanceId)
                    .includeVariableUpdates()
                    .singleResult();
            List<HistoricData> events = log.getHistoricData();
            assertThat(events).hasSize(1);

            for (HistoricData event : events) {
                assertThat(event).isInstanceOf(HistoricVariableUpdate.class);
                assertThat(((HistoricDetailVariableInstanceUpdateEntity) event).getValue()).isEqualTo(LARGE_STRING_VALUE);
            }
        }
    }
}
