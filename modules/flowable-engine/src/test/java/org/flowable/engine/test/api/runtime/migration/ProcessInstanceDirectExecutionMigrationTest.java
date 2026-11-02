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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.migration.ProcessInstanceMigrationBuilder;
import org.flowable.engine.migration.ProcessInstanceMigrationValidationResult;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class ProcessInstanceDirectExecutionMigrationTest extends AbstractProcessInstanceMigrationTest {

    @AfterEach
    protected void tearDown() {
        deleteDeployments();
    }
    
    @Test
    public void testSimpleMigrationWithActivityAutoMapping() {
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-start.bpmn20.xml");
        
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("one-task");
        
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-rename-task.bpmn20.xml");
        
        ProcessInstanceMigrationValidationResult validationResult = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .validateMigration(processInstance.getId());
        assertThat(validationResult.isMigrationValid()).isTrue();
        
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId());
        
        processInstanceMigrationBuilder.migrate(processInstance.getId());
        
        List<Execution> executions = runtimeService.createExecutionQuery().list();
        assertThat(executions).hasSize(2); //includes root execution
        executions.stream()
                .map(e -> (ExecutionEntity) e)
                .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId()));
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        
        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    public void testChangeTaskIdMigrationWithActivityAutoMapping() {
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-start.bpmn20.xml");
        
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("one-task");
        
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-rename-task-id.bpmn20.xml");
        
        ProcessInstanceMigrationValidationResult validationResult = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .validateMigration(processInstance.getId());
        assertThat(validationResult.isMigrationValid()).isFalse();
        
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId());
        
        assertThatThrownBy(() -> processInstanceMigrationBuilder.migrate(processInstance.getId()))
            .isInstanceOf(FlowableException.class)
            .hasMessageContaining("Migration Activity mapping missing for activity definition Id:'userTask1Id'");
        
    }
    
    @Test
    public void testChangeTaskTypeMigrationWithActivityAutoMapping() {
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-start.bpmn20.xml");
        
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("one-task");
        
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-change-task-type.bpmn20.xml");
        
        ProcessInstanceMigrationValidationResult validationResult = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .validateMigration(processInstance.getId());
        assertThat(validationResult.isMigrationValid()).isTrue();
        
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId());
        
        processInstanceMigrationBuilder.migrate(processInstance.getId());
        
        List<Execution> executions = runtimeService.createExecutionQuery().list();
        assertThat(executions).hasSize(2); //includes root execution
        executions.stream()
                .map(e -> (ExecutionEntity) e)
                .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId()));
        
        assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()).list()).hasSize(0);
        
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult();
        assertThat(execution.getActivityId()).isEqualTo("userTask1Id");
        
        runtimeService.trigger(execution.getId());
        
        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    public void testAddTaskBeforeMigrationWithActivityAutoMapping() {
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-start.bpmn20.xml");
        
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("one-task");
        
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-add-task-before.bpmn20.xml");
        
        ProcessInstanceMigrationValidationResult validationResult = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .validateMigration(processInstance.getId());
        assertThat(validationResult.isMigrationValid()).isTrue();
        
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId());
        
        processInstanceMigrationBuilder.migrate(processInstance.getId());
        
        List<Execution> executions = runtimeService.createExecutionQuery().list();
        assertThat(executions).hasSize(2); //includes root execution
        executions.stream()
                .map(e -> (ExecutionEntity) e)
                .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId()));
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        
        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    public void testAddTaskAfterMigrationWithActivityAutoMapping() {
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-start.bpmn20.xml");
        
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("one-task");
        
        ProcessDefinition version2ProcessDef = deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/one-task-add-task-after.bpmn20.xml");
        
        ProcessInstanceMigrationValidationResult validationResult = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId())
                .validateMigration(processInstance.getId());
        assertThat(validationResult.isMigrationValid()).isTrue();
        
        ProcessInstanceMigrationBuilder processInstanceMigrationBuilder = processMigrationService.createProcessInstanceMigrationBuilder()
                .migrateToProcessDefinition(version2ProcessDef.getId());
        
        processInstanceMigrationBuilder.migrate(processInstance.getId());
        
        List<Execution> executions = runtimeService.createExecutionQuery().list();
        assertThat(executions).hasSize(2); //includes root execution
        executions.stream()
                .map(e -> (ExecutionEntity) e)
                .forEach(e -> assertThat(e.getProcessDefinitionId()).isEqualTo(version2ProcessDef.getId()));
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        
        assertProcessEnded(processInstance.getId());
    }

}
