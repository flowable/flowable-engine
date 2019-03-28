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

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.junit.jupiter.api.Test;

public class NonCascadeDeleteTest extends PluggableFlowableTestCase {

    private static final String PROCESS_DEFINITION_KEY = "oneTaskProcess";

    private String deploymentId;

    private String processInstanceId;

    @Test
    public void testHistoricProcessInstanceQuery() {
        deploymentId = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
                .deploy().getId();

        processInstanceId = runtimeService.startProcessInstanceByKey(PROCESS_DEFINITION_KEY).getId();
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        taskService.complete(task.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            assertEquals(PROCESS_DEFINITION_KEY, processInstance.getProcessDefinitionKey());

            // Delete deployment and historic process instance remains.
            repositoryService.deleteDeployment(deploymentId, false);

            HistoricProcessInstance processInstanceAfterDelete = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            assertNull(processInstanceAfterDelete.getProcessDefinitionKey());
            assertNull(processInstanceAfterDelete.getProcessDefinitionName());
            assertNull(processInstanceAfterDelete.getProcessDefinitionVersion());

            assertTrue(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceId).count() > 0);
            assertTrue(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceId).count() > 0);

            // clean
            historyService.deleteHistoricProcessInstance(processInstanceId);
            managementService.executeCommand( commandContext -> {
                CommandContextUtil.getHistoricTaskService(commandContext).deleteHistoricTaskLogEntriesForProcessDefinition(processInstance.getProcessDefinitionId());
                return null;
            });

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);
        }
    }
}
