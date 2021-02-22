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

package org.flowable.engine.test.bpmn.multiinstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class DynamicMultiInstanceTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.sequentialUserTasks.bpmn20.xml" })
    public void testAddSequentialUserTask() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            String procId = runtimeService.startProcessInstanceByKey("miSequentialUserTasks", CollectionUtil.singletonMap("nrOfLoops", 3)).getId();
    
            org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
            assertThat(task.getName()).isEqualTo("My Task");
            assertThat(task.getAssignee()).isEqualTo("kermit_0");
            
            runtimeService.addMultiInstanceExecution("miTasks", procId, null);
            
            taskService.complete(task.getId());
    
            task = taskService.createTaskQuery().singleResult();
            assertThat(task.getName()).isEqualTo("My Task");
            assertThat(task.getAssignee()).isEqualTo("kermit_1");
            taskService.complete(task.getId());
    
            task = taskService.createTaskQuery().singleResult();
            assertThat(task.getName()).isEqualTo("My Task");
            assertThat(task.getAssignee()).isEqualTo("kermit_2");
            taskService.complete(task.getId());
            
            // another mi execution was added
            task = taskService.createTaskQuery().singleResult();
            assertThat(task.getName()).isEqualTo("My Task");
            assertThat(task.getAssignee()).isEqualTo("kermit_3");
            taskService.complete(task.getId());
    
            assertThat(taskService.createTaskQuery().singleResult()).isNull();
            assertProcessEnded(procId);
        }
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.sequentialUserTasksWithCollection.bpmn20.xml" })
    public void testAddSequentialUserTaskWithCollection() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            Map<String, Object> variableMap = new HashMap<>();
            ArrayList<String> userList = new ArrayList<>();
            userList.add("admin");
            variableMap.put("taskUserList", userList);
            String procId = runtimeService.startProcessInstanceByKey("miSequentialUserTasks", variableMap).getId();

            org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
            assertThat(task.getName()).isEqualTo("My Task");
            assertThat(task.getAssignee()).isEqualTo("admin");

            userList.add("hr");
            runtimeService.setVariable(procId, "taskUserList", userList);
            runtimeService.addMultiInstanceExecution("miTasks", procId, CollectionUtil.singletonMap("taskUser", "hr"));

            taskService.complete(task.getId());

            task = taskService.createTaskQuery().singleResult();
            assertThat(task.getName()).isEqualTo("My Task");
            assertThat(task.getAssignee()).isEqualTo("hr");
            taskService.complete(task.getId());

            assertThat(taskService.createTaskQuery().singleResult()).isNull();
            assertProcessEnded(procId);
        }
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.sequentialUserTasks.bpmn20.xml" })
    public void testDeleteSequentialUserTask() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            String procId = runtimeService.startProcessInstanceByKey("miSequentialUserTasks", CollectionUtil.singletonMap("nrOfLoops", 3)).getId();
    
            org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
            assertThat(task.getName()).isEqualTo("My Task");
            assertThat(task.getAssignee()).isEqualTo("kermit_0");
            
            runtimeService.deleteMultiInstanceExecution(task.getExecutionId(), false);
    
            task = taskService.createTaskQuery().singleResult();
            assertThat(task.getName()).isEqualTo("My Task");
            assertThat(task.getAssignee()).isEqualTo("kermit_0");
            taskService.complete(task.getId());
    
            task = taskService.createTaskQuery().singleResult();
            assertThat(task.getName()).isEqualTo("My Task");
            assertThat(task.getAssignee()).isEqualTo("kermit_1");
            taskService.complete(task.getId());
    
            assertThat(taskService.createTaskQuery().singleResult()).isNull();
            assertProcessEnded(procId);
        }
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.sequentialUserTasks.bpmn20.xml" })
    public void testDeleteSequentialUserTaskWithCompletion() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            String procId = runtimeService.startProcessInstanceByKey("miSequentialUserTasks", CollectionUtil.singletonMap("nrOfLoops", 3)).getId();
    
            org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
            assertThat(task.getName()).isEqualTo("My Task");
            assertThat(task.getAssignee()).isEqualTo("kermit_0");
            
            runtimeService.deleteMultiInstanceExecution(task.getExecutionId(), true);
    
            task = taskService.createTaskQuery().singleResult();
            assertThat(task.getName()).isEqualTo("My Task");
            assertThat(task.getAssignee()).isEqualTo("kermit_1");
            taskService.complete(task.getId());
    
            task = taskService.createTaskQuery().singleResult();
            assertThat(task.getName()).isEqualTo("My Task");
            assertThat(task.getAssignee()).isEqualTo("kermit_2");
            taskService.complete(task.getId());
    
            assertThat(taskService.createTaskQuery().singleResult()).isNull();
            assertProcessEnded(procId);
        }
    }

    @Test
    @Deployment
    public void testParallelUserTasks() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            String procId = runtimeService.startProcessInstanceByKey("miParallelUserTasks").getId();
    
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
            assertThat(tasks)
                    .extracting(Task::getName)
                    .containsExactly("My Task 0", "My Task 1", "My Task 2");
            
            runtimeService.addMultiInstanceExecution("miTasks", procId, null);
            
            tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
            assertThat(tasks)
                    .extracting(Task::getName)
                    .containsExactly("My Task 0", "My Task 1", "My Task 2", "My Task 3");

            taskService.complete(tasks.get(0).getId());
            taskService.complete(tasks.get(1).getId());
            taskService.complete(tasks.get(2).getId());
            taskService.complete(tasks.get(3).getId());
            assertProcessEnded(procId);
        }
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/DynamicMultiInstanceTest.testParallelUserTasks.bpmn20.xml" })
    public void testDeleteParallelUserTasks() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            String procId = runtimeService.startProcessInstanceByKey("miParallelUserTasks").getId();
    
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
            assertThat(tasks)
                    .extracting(Task::getName)
                    .containsExactly("My Task 0", "My Task 1", "My Task 2");
            
            runtimeService.deleteMultiInstanceExecution(tasks.get(1).getExecutionId(), false);
            
            tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
            assertThat(tasks)
                    .extracting(Task::getName)
                    .containsExactly("My Task 0", "My Task 2");
    
            taskService.complete(tasks.get(0).getId());
            taskService.complete(tasks.get(1).getId());
            assertProcessEnded(procId);
        }
    }
    
    @Test
    @Deployment
    public void testParallelUserTasksBasedOnCollection() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            List<String> assigneeList = Arrays.asList("kermit", "gonzo", "mispiggy", "fozzie", "bubba");
            String procId = runtimeService.startProcessInstanceByKey("miParallelUserTasksBasedOnCollection", CollectionUtil.singletonMap("assigneeList", assigneeList)).getId();
        
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskAssignee().asc().list();
            assertThat(tasks)
                    .extracting(Task::getAssignee)
                    .containsExactly("bubba", "fozzie", "gonzo", "kermit", "mispiggy");
            
            runtimeService.addMultiInstanceExecution("miTasks", procId, Collections.singletonMap("assignee", (Object) "johndoe"));
            tasks = taskService.createTaskQuery().orderByTaskAssignee().asc().list();
            assertThat(tasks).hasSize(6);
        
            // Completing 3 tasks will not yet trigger completion condition
            taskService.complete(tasks.get(0).getId());
            taskService.complete(tasks.get(1).getId());
            taskService.complete(tasks.get(2).getId());
            
            assertThat(taskService.createTaskQuery().count()).isEqualTo(3);
            
            org.flowable.task.api.Task newTask = taskService.createTaskQuery().processInstanceId(procId).taskAssignee("johndoe").singleResult();
            assertThat(newTask).isNotNull();
            
            // Completing task will trigger completion condition
            taskService.complete(newTask.getId());
            
            assertThat(taskService.createTaskQuery().count()).isZero();
            assertProcessEnded(procId);
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testNestedParallelCallActivity.bpmn20.xml",
        "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testAddNestedParallelCallActivity() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            String procId = runtimeService.startProcessInstanceByKey("miNestedParallelCallActivity").getId();
            
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
            assertThat(tasks).hasSize(14);
            
            runtimeService.addMultiInstanceExecution("miCallActivity", procId, null);
            
            tasks = taskService.createTaskQuery().list();
            assertThat(tasks).hasSize(16);
            
            for (int i = 0; i < 16; i++) {
                taskService.complete(tasks.get(i).getId());
            }
            
            assertProcessEnded(procId);
        }
    }
        
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testNestedParallelCallActivity.bpmn20.xml",
        "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testDeleteNestedParallelCallActivity() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            String procId = runtimeService.startProcessInstanceByKey("miNestedParallelCallActivity").getId();
            
            List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(procId).onlyChildExecutions().list();
            List<Execution> childExecutions = null;
            for (Execution execution : executions) {
                ExecutionEntity executionEntity = (ExecutionEntity) execution;
                if (executionEntity.isMultiInstanceRoot()) {
                    childExecutions = runtimeService.createExecutionQuery().parentId(executionEntity.getId()).list();
                }
            }
            
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
            assertThat(tasks).hasSize(14);
            
            assertThat(childExecutions).hasSize(7);
            runtimeService.deleteMultiInstanceExecution(childExecutions.get(2).getId(), false);
            
            tasks = taskService.createTaskQuery().list();
            assertThat(tasks).hasSize(12);
            
            for (int i = 0; i < 12; i++) {
                taskService.complete(tasks.get(i).getId());
            }
            
            assertProcessEnded(procId);
        }
    }
    
    @Test
    @Deployment
    public void testSequentialSubProcessCompletionCondition() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            String procId = runtimeService.startProcessInstanceByKey("miSequentialSubprocessCompletionCondition").getId();
            
            runtimeService.addMultiInstanceExecution("miSubProcess", procId, null);
    
            TaskQuery query = taskService.createTaskQuery().orderByTaskName().asc();
            for (int i = 0; i < 3; i++) {
                List<org.flowable.task.api.Task> tasks = query.list();
                assertThat(tasks)
                        .extracting(Task::getName)
                        .containsExactly("task one", "task two");
    
                taskService.complete(tasks.get(0).getId());
                taskService.complete(tasks.get(1).getId());
            }
    
            assertProcessEnded(procId);
        }
    }
    
    @Test
    @Deployment(resources = {"org/flowable/engine/test/bpmn/multiinstance/DynamicMultiInstanceTest.testSequentialSubProcessCompletionCondition.bpmn20.xml"})
    public void testDeleteSequentialSubProcessCompletionCondition() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            String procId = runtimeService.startProcessInstanceByKey("miSequentialSubprocessCompletionCondition").getId();
            
            List<Execution> executions = runtimeService.createExecutionQuery().parentId(procId).list();
            ExecutionEntity miExecution = null;
            for (Execution execution : executions) {
                ExecutionEntity executionEntity = (ExecutionEntity) execution;
                if (executionEntity.isMultiInstanceRoot()) {
                    miExecution = executionEntity;
                    break;
                }
            }
            
            assertThat(miExecution).isNotNull();
            
            List<Execution> childExecutions = runtimeService.createExecutionQuery().parentId(miExecution.getId()).list();
            runtimeService.deleteMultiInstanceExecution(childExecutions.get(0).getId(), false);
    
            TaskQuery query = taskService.createTaskQuery().orderByTaskName().asc();
            for (int i = 0; i < 3; i++) {
                List<org.flowable.task.api.Task> tasks = query.list();
                assertThat(tasks)
                        .extracting(Task::getName)
                        .containsExactly("task one", "task two");
    
                taskService.complete(tasks.get(0).getId());
                taskService.complete(tasks.get(1).getId());
            }
    
            assertProcessEnded(procId);
        }
    }
    
    @Test
    @Deployment(resources = {"org/flowable/engine/test/bpmn/multiinstance/DynamicMultiInstanceTest.testSequentialSubProcessCompletionCondition.bpmn20.xml"})
    public void testDeleteSequentialSubProcessAsCompleted() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            String procId = runtimeService.startProcessInstanceByKey("miSequentialSubprocessCompletionCondition").getId();
            
            List<Execution> executions = runtimeService.createExecutionQuery().parentId(procId).list();
            ExecutionEntity miExecution = null;
            for (Execution execution : executions) {
                ExecutionEntity executionEntity = (ExecutionEntity) execution;
                if (executionEntity.isMultiInstanceRoot()) {
                    miExecution = executionEntity;
                    break;
                }
            }
            
            assertThat(miExecution).isNotNull();
            
            List<Execution> childExecutions = runtimeService.createExecutionQuery().parentId(miExecution.getId()).list();
            runtimeService.deleteMultiInstanceExecution(childExecutions.get(0).getId(), true);
    
            TaskQuery query = taskService.createTaskQuery().orderByTaskName().asc();
            for (int i = 0; i < 2; i++) {
                List<org.flowable.task.api.Task> tasks = query.list();
                assertThat(tasks)
                        .extracting(Task::getName)
                        .containsExactly("task one", "task two");
    
                taskService.complete(tasks.get(0).getId());
                taskService.complete(tasks.get(1).getId());
            }
    
            assertProcessEnded(procId);
        }
    }
    
    @Test
    @Deployment(resources = {"org/flowable/engine/test/bpmn/multiinstance/DynamicMultiInstanceTest.testSequentialSubProcessCompletionCondition.bpmn20.xml"})
    public void testChangeCompletionConditionSequentialSubProcess() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            String procId = runtimeService.startProcessInstanceByKey("miSequentialSubprocessCompletionCondition").getId();
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(procId).singleResult();
            
            runtimeService.addMultiInstanceExecution("miSubProcess", procId, null);
            
            ObjectNode infoNode = dynamicBpmnService.changeMultiInstanceCompletionCondition("miSubProcess", "${nrOfCompletedInstances == 5}");
            dynamicBpmnService.saveProcessDefinitionInfo(processInstance.getProcessDefinitionId(), infoNode);
    
            TaskQuery query = taskService.createTaskQuery().orderByTaskName().asc();
            for (int i = 0; i < 5; i++) {
                List<org.flowable.task.api.Task> tasks = query.list();
                assertThat(tasks)
                        .extracting(Task::getName)
                        .containsExactly("task one", "task two");
    
                taskService.complete(tasks.get(0).getId());
                taskService.complete(tasks.get(1).getId());
            }
    
            assertProcessEnded(procId);
        }
    }
    
    @Test
    @Deployment
    public void testMultipleParallelSubProcess() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            String procId = runtimeService.startProcessInstanceByKey("miMultipleParallelSubProcess").getId();
            
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(procId).list();
            assertThat(tasks).hasSize(8);

            assertThatThrownBy(() -> runtimeService.addMultiInstanceExecution("miSubProcess", procId, null))
                    .as("Expected exception because multiple multi instance executions are present")
                    .isInstanceOf(FlowableException.class);

            List<Execution> miExecutions = runtimeService.createExecutionQuery().activityId("nesting1").list();
            assertThat(miExecutions).hasSize(4);
            
            runtimeService.addMultiInstanceExecution("miSubProcess", miExecutions.get(1).getId(), null);
            
            tasks = taskService.createTaskQuery().processInstanceId(procId).list();
            assertThat(tasks).hasSize(9);
            
            runtimeService.addMultiInstanceExecution("nesting1", procId, null);
            
            tasks = taskService.createTaskQuery().processInstanceId(procId).list();
            assertThat(tasks).hasSize(11);
            
            for (org.flowable.task.api.Task task : tasks) {
                taskService.complete(task.getId());
            }
            
            assertProcessEnded(procId);
        }
    }
}
