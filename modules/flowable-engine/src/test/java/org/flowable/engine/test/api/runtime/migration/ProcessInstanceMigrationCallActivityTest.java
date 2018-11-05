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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.assertj.core.api.Condition;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.migration.ProcessInstanceMigrationValidationResult;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.migration.ActivityMigrationMapping;
import org.flowable.engine.migration.ProcessInstanceMigrationBuilder;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ExecutionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationCallActivityTest extends PluggableFlowableTestCase {

    @AfterEach
    protected void tearDown() {
        deleteDeployments();
    }

    @Test
    public void testMigrateActivityIntoCallActivitySubProcess() {
        ProcessDefinition procDefSimpleOneTask = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-simple-process.bpmn20.xml");
        ProcessDefinition procDefWithCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/two-tasks-with-call-activity.bpmn20.xml");
        ProcessDefinition procDefCallActivity = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml");

        //Start the processInstance
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(procDefSimpleOneTask.getId());

        //Confirm the state to migrate
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("userTask1Id");
        assertThat(executions).extracting("processDefinitionId").containsOnly(procDefSimpleOneTask.getId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("userTask1Id");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefSimpleOneTask.getId());

        //Prepare and action the migration - to a new ProcessDefinition containing a call activity subProcess
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = runtimeService.createProcessInstanceMigrationBuilder()
            .migrateToProcessDefinition(procDefWithCallActivity.getId())
            .addActivityMigrationMapping(ActivityMigrationMapping.createMappingFor("userTask1Id", "theTask").inCallActivity("callActivity"));
        ProcessInstanceMigrationValidationResult processInstanceMigrationValidationResult = processInstanceMigrationBuilder.validateMigration(processInstance.getId());
        assertThat(processInstanceMigrationValidationResult.getValidationMessages()).isEmpty();

        processInstanceMigrationBuilder.migrate(processInstance.getId());

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertNotNull(subProcessInstance);

        List<Execution> processExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(processExecutions).extracting(Execution::getActivityId).containsExactly("callActivity");
        assertThat(processExecutions).extracting("processDefinitionId").containsOnly(procDefWithCallActivity.getId());

        List<Execution> subProcessExecutions = runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().list();
        assertThat(subProcessExecutions).extracting(Execution::getActivityId).containsExactly("theTask");
        assertThat(subProcessExecutions).extracting("processDefinitionId").containsOnly(procDefCallActivity.getId());

        List<Task> processTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(processTasks).isEmpty();

        List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).list();
        assertThat(subProcessTasks).extracting(Task::getTaskDefinitionKey).containsExactly("theTask");
        assertThat(subProcessTasks).extracting(Task::getProcessDefinitionId).containsOnly(procDefCallActivity.getId());

        completeTask(subProcessTasks.get(0));

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("secondTask");
        assertThat(task).extracting(Task::getProcessDefinitionId).isEqualTo(procDefWithCallActivity.getId());

        completeTask(task);

        assertProcessEnded(processInstance.getId());
    }

}
