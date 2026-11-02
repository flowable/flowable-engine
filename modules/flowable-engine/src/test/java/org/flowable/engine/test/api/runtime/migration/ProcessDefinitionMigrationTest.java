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

package org.flowable.engine.test.api.runtime.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.migration.ActivityMigrationMapping;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class ProcessDefinitionMigrationTest extends AbstractProcessInstanceMigrationTest {

    @AfterEach
    protected void tearDown() {
        deleteDeployments();
    }

    @Test
    public void testDefinitionMigrationWithTaskMapping() {
        // Deploy first version of the process
        ProcessDefinition sourceDefinition = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-simple-process.bpmn20.xml");

        // Start and instance of the recent first version of the process for migration
        ProcessInstance processInstance1ToMigrate = runtimeService.startProcessInstanceByKey("MP");
        taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance1ToMigrate.getId()).singleResult().getId());

        Task beforeMigrationTask1 = taskService.createTaskQuery().processInstanceId(processInstance1ToMigrate.getId()).singleResult();
        
        ProcessInstance processInstance2ToMigrate = runtimeService.startProcessInstanceByKey("MP");
        
        Task beforeMigrationTask2 = taskService.createTaskQuery().processInstanceId(processInstance2ToMigrate.getId()).singleResult();

        //Deploy second version of the process
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/three-tasks-simple-process.bpmn20.xml");

        //Migrate process
        processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "userTask2Id"))
                .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask2Id", "intermediateTask"))
                .migrateProcessInstances(sourceDefinition.getId());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance1ToMigrate.getId()).list();
        assertThat(executions).hasSize(2); //includes root execution
        for (Execution execution : executions) {
            assertThat(((ExecutionEntity) execution).getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());
        }

        Task task = taskService.createTaskQuery().processInstanceId(processInstance1ToMigrate.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey, Task::getId)
                .containsExactly(version2ProcessDef.getId(), "intermediateTask", beforeMigrationTask1.getId());
        
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance2ToMigrate.getId()).list();
        assertThat(executions).hasSize(2); //includes root execution
        for (Execution execution : executions) {
            assertThat(((ExecutionEntity) execution).getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());
        }

        task = taskService.createTaskQuery().processInstanceId(processInstance2ToMigrate.getId()).singleResult();
        assertThat(task)
                .extracting(Task::getProcessDefinitionId, Task::getTaskDefinitionKey, Task::getId)
                .containsExactly(version2ProcessDef.getId(), "userTask2Id", beforeMigrationTask2.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance1ToMigrate.getId()).singleResult();
            assertThat(historicProcessInstance.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());

            List<HistoricTaskInstance> historicTaskInstances = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance1ToMigrate.getId()).list();
            assertThat(historicTaskInstances).hasSize(2);
            for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
                assertThat(historicTaskInstance.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());
            }

            List<HistoricActivityInstance> historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance1ToMigrate.getId()).list();
            assertThat(historicActivityInstances).hasSize(5);
            for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                assertThat(historicActivityInstance.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());
            }
            
            historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstance2ToMigrate.getId()).singleResult();
            assertThat(historicProcessInstance.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());

            historicTaskInstances = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance2ToMigrate.getId()).list();
            assertThat(historicTaskInstances).hasSize(1);
            for (HistoricTaskInstance historicTaskInstance : historicTaskInstances) {
                assertThat(historicTaskInstance.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());
            }

            historicActivityInstances = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance2ToMigrate.getId()).list();
            assertThat(historicActivityInstances).hasSize(3);
            for (HistoricActivityInstance historicActivityInstance : historicActivityInstances) {
                assertThat(historicActivityInstance.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId());
            }
        }
        
        task = taskService.createTaskQuery().processInstanceId(processInstance1ToMigrate.getId()).singleResult();

        // complete intermediate task
        taskService.complete(task.getId());

        // complete final task
        task = taskService.createTaskQuery().processInstanceId(processInstance1ToMigrate.getId()).singleResult();
        taskService.complete(task.getId());

        assertProcessEnded(processInstance1ToMigrate.getId());
        
        task = taskService.createTaskQuery().processInstanceId(processInstance2ToMigrate.getId()).singleResult();

        // complete final task
        taskService.complete(task.getId());

        assertProcessEnded(processInstance2ToMigrate.getId());
    }
}
