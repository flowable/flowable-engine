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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.TaskQuery;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class DynamicMultiInstanceTest extends PluggableFlowableTestCase {

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.sequentialUserTasks.bpmn20.xml" })
    public void testAddSequentialUserTask() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            String procId = runtimeService.startProcessInstanceByKey("miSequentialUserTasks", CollectionUtil.singletonMap("nrOfLoops", 3)).getId();
    
            org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
            assertEquals("My Task", task.getName());
            assertEquals("kermit_0", task.getAssignee());
            
            runtimeService.addMultiInstanceExecution("miTasks", procId, null);
            
            taskService.complete(task.getId());
    
            task = taskService.createTaskQuery().singleResult();
            assertEquals("My Task", task.getName());
            assertEquals("kermit_1", task.getAssignee());
            taskService.complete(task.getId());
    
            task = taskService.createTaskQuery().singleResult();
            assertEquals("My Task", task.getName());
            assertEquals("kermit_2", task.getAssignee());
            taskService.complete(task.getId());
            
            // another mi execution was added
            task = taskService.createTaskQuery().singleResult();
            assertEquals("My Task", task.getName());
            assertEquals("kermit_3", task.getAssignee());
            taskService.complete(task.getId());
    
            assertNull(taskService.createTaskQuery().singleResult());
            assertProcessEnded(procId);
        }
    }
    
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.sequentialUserTasks.bpmn20.xml" })
    public void testDeleteSequentialUserTask() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            String procId = runtimeService.startProcessInstanceByKey("miSequentialUserTasks", CollectionUtil.singletonMap("nrOfLoops", 3)).getId();
    
            org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
            assertEquals("My Task", task.getName());
            assertEquals("kermit_0", task.getAssignee());
            
            runtimeService.deleteMultiInstanceExecution(task.getExecutionId(), false);
    
            task = taskService.createTaskQuery().singleResult();
            assertEquals("My Task", task.getName());
            assertEquals("kermit_0", task.getAssignee());
            taskService.complete(task.getId());
    
            task = taskService.createTaskQuery().singleResult();
            assertEquals("My Task", task.getName());
            assertEquals("kermit_1", task.getAssignee());
            taskService.complete(task.getId());
    
            assertNull(taskService.createTaskQuery().singleResult());
            assertProcessEnded(procId);
        }
    }
    
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.sequentialUserTasks.bpmn20.xml" })
    public void testDeleteSequentialUserTaskWithCompletion() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            String procId = runtimeService.startProcessInstanceByKey("miSequentialUserTasks", CollectionUtil.singletonMap("nrOfLoops", 3)).getId();
    
            org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
            assertEquals("My Task", task.getName());
            assertEquals("kermit_0", task.getAssignee());
            
            runtimeService.deleteMultiInstanceExecution(task.getExecutionId(), true);
    
            task = taskService.createTaskQuery().singleResult();
            assertEquals("My Task", task.getName());
            assertEquals("kermit_1", task.getAssignee());
            taskService.complete(task.getId());
    
            task = taskService.createTaskQuery().singleResult();
            assertEquals("My Task", task.getName());
            assertEquals("kermit_2", task.getAssignee());
            taskService.complete(task.getId());
    
            assertNull(taskService.createTaskQuery().singleResult());
            assertProcessEnded(procId);
        }
    }

    @Deployment
    public void testParallelUserTasks() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            String procId = runtimeService.startProcessInstanceByKey("miParallelUserTasks").getId();
    
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
            assertEquals(3, tasks.size());
            assertEquals("My Task 0", tasks.get(0).getName());
            assertEquals("My Task 1", tasks.get(1).getName());
            assertEquals("My Task 2", tasks.get(2).getName());
            
            runtimeService.addMultiInstanceExecution("miTasks", procId, null);
            
            tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
            assertEquals(4, tasks.size());
            assertEquals("My Task 0", tasks.get(0).getName());
            assertEquals("My Task 1", tasks.get(1).getName());
            assertEquals("My Task 2", tasks.get(2).getName());
            assertEquals("My Task 3", tasks.get(3).getName());
    
            taskService.complete(tasks.get(0).getId());
            taskService.complete(tasks.get(1).getId());
            taskService.complete(tasks.get(2).getId());
            taskService.complete(tasks.get(3).getId());
            assertProcessEnded(procId);
        }
    }
    
    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/DynamicMultiInstanceTest.testParallelUserTasks.bpmn20.xml" })
    public void testDeleteParallelUserTasks() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            String procId = runtimeService.startProcessInstanceByKey("miParallelUserTasks").getId();
    
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
            assertEquals(3, tasks.size());
            assertEquals("My Task 0", tasks.get(0).getName());
            assertEquals("My Task 1", tasks.get(1).getName());
            assertEquals("My Task 2", tasks.get(2).getName());
            
            runtimeService.deleteMultiInstanceExecution(tasks.get(1).getExecutionId(), false);
            
            tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
            assertEquals(2, tasks.size());
            assertEquals("My Task 0", tasks.get(0).getName());
            assertEquals("My Task 2", tasks.get(1).getName());
    
            taskService.complete(tasks.get(0).getId());
            taskService.complete(tasks.get(1).getId());
            assertProcessEnded(procId);
        }
    }
    
    @Deployment
    public void testParallelUserTasksBasedOnCollection() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            List<String> assigneeList = Arrays.asList("kermit", "gonzo", "mispiggy", "fozzie", "bubba");
            String procId = runtimeService.startProcessInstanceByKey("miParallelUserTasksBasedOnCollection", CollectionUtil.singletonMap("assigneeList", assigneeList)).getId();
        
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().orderByTaskAssignee().asc().list();
            assertEquals(5, tasks.size());
            assertEquals("bubba", tasks.get(0).getAssignee());
            assertEquals("fozzie", tasks.get(1).getAssignee());
            assertEquals("gonzo", tasks.get(2).getAssignee());
            assertEquals("kermit", tasks.get(3).getAssignee());
            assertEquals("mispiggy", tasks.get(4).getAssignee());
            
            runtimeService.addMultiInstanceExecution("miTasks", procId, Collections.singletonMap("assignee", (Object) "johndoe"));
            tasks = taskService.createTaskQuery().orderByTaskAssignee().asc().list();
            assertEquals(6, tasks.size());
        
            // Completing 3 tasks will not yet trigger completion condition
            taskService.complete(tasks.get(0).getId());
            taskService.complete(tasks.get(1).getId());
            taskService.complete(tasks.get(2).getId());
            
            assertEquals(3, taskService.createTaskQuery().count());
            
            org.flowable.task.api.Task newTask = taskService.createTaskQuery().processInstanceId(procId).taskAssignee("johndoe").singleResult();
            assertNotNull(newTask);
            
            // Completing task will trigger completion condition
            taskService.complete(newTask.getId());
            
            assertEquals(0, taskService.createTaskQuery().count());
            assertProcessEnded(procId);
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.testNestedParallelCallActivity.bpmn20.xml",
        "org/flowable/engine/test/bpmn/multiinstance/MultiInstanceTest.externalSubProcess.bpmn20.xml" })
    public void testAddNestedParallelCallActivity() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            String procId = runtimeService.startProcessInstanceByKey("miNestedParallelCallActivity").getId();
            
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
            assertEquals(14, tasks.size());
            
            runtimeService.addMultiInstanceExecution("miCallActivity", procId, null);
            
            tasks = taskService.createTaskQuery().list();
            assertEquals(16, tasks.size());
            
            for (int i = 0; i < 16; i++) {
                taskService.complete(tasks.get(i).getId());
            }
            
            assertProcessEnded(procId);
        }
    }
        
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
            assertEquals(14, tasks.size());
            
            assertNotNull(childExecutions);
            assertEquals(7, childExecutions.size());
            runtimeService.deleteMultiInstanceExecution(childExecutions.get(2).getId(), false);
            
            tasks = taskService.createTaskQuery().list();
            assertEquals(12, tasks.size());
            
            for (int i = 0; i < 12; i++) {
                taskService.complete(tasks.get(i).getId());
            }
            
            assertProcessEnded(procId);
        }
    }
    
    @Deployment
    public void testSequentialSubProcessCompletionCondition() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            String procId = runtimeService.startProcessInstanceByKey("miSequentialSubprocessCompletionCondition").getId();
            
            runtimeService.addMultiInstanceExecution("miSubProcess", procId, null);
    
            TaskQuery query = taskService.createTaskQuery().orderByTaskName().asc();
            for (int i = 0; i < 3; i++) {
                List<org.flowable.task.api.Task> tasks = query.list();
                assertEquals(2, tasks.size());
    
                assertEquals("task one", tasks.get(0).getName());
                assertEquals("task two", tasks.get(1).getName());
    
                taskService.complete(tasks.get(0).getId());
                taskService.complete(tasks.get(1).getId());
            }
    
            assertProcessEnded(procId);
        }
    }
    
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
            
            assertNotNull(miExecution);
            
            List<Execution> childExecutions = runtimeService.createExecutionQuery().parentId(miExecution.getId()).list();
            runtimeService.deleteMultiInstanceExecution(childExecutions.get(0).getId(), false);
    
            TaskQuery query = taskService.createTaskQuery().orderByTaskName().asc();
            for (int i = 0; i < 3; i++) {
                List<org.flowable.task.api.Task> tasks = query.list();
                assertEquals(2, tasks.size());
    
                assertEquals("task one", tasks.get(0).getName());
                assertEquals("task two", tasks.get(1).getName());
    
                taskService.complete(tasks.get(0).getId());
                taskService.complete(tasks.get(1).getId());
            }
    
            assertProcessEnded(procId);
        }
    }
    
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
            
            assertNotNull(miExecution);
            
            List<Execution> childExecutions = runtimeService.createExecutionQuery().parentId(miExecution.getId()).list();
            runtimeService.deleteMultiInstanceExecution(childExecutions.get(0).getId(), true);
    
            TaskQuery query = taskService.createTaskQuery().orderByTaskName().asc();
            for (int i = 0; i < 2; i++) {
                List<org.flowable.task.api.Task> tasks = query.list();
                assertEquals(2, tasks.size());
    
                assertEquals("task one", tasks.get(0).getName());
                assertEquals("task two", tasks.get(1).getName());
    
                taskService.complete(tasks.get(0).getId());
                taskService.complete(tasks.get(1).getId());
            }
    
            assertProcessEnded(procId);
        }
    }
    
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
                assertEquals(2, tasks.size());
    
                assertEquals("task one", tasks.get(0).getName());
                assertEquals("task two", tasks.get(1).getName());
    
                taskService.complete(tasks.get(0).getId());
                taskService.complete(tasks.get(1).getId());
            }
    
            assertProcessEnded(procId);
        }
    }
    
    @Deployment
    public void testMultipleParallelSubProcess() {
        if (!processEngineConfiguration.isAsyncHistoryEnabled()) {
            String procId = runtimeService.startProcessInstanceByKey("miMultipleParallelSubProcess").getId();
            
            List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().processInstanceId(procId).list();
            assertEquals(8, tasks.size());
            
            try {
                runtimeService.addMultiInstanceExecution("miSubProcess", procId, null);
                fail("Expected exception because multiple multi instance executions are present");
            } catch (FlowableException e) {
                // expected
            }
            
            List<Execution> miExecutions = runtimeService.createExecutionQuery().activityId("nesting1").list();
            assertEquals(4, miExecutions.size());
            
            runtimeService.addMultiInstanceExecution("miSubProcess", miExecutions.get(1).getId(), null);
            
            tasks = taskService.createTaskQuery().processInstanceId(procId).list();
            assertEquals(9, tasks.size());
            
            runtimeService.addMultiInstanceExecution("nesting1", procId, null);
            
            tasks = taskService.createTaskQuery().processInstanceId(procId).list();
            assertEquals(11, tasks.size());
            
            for (org.flowable.task.api.Task task : tasks) {
                taskService.complete(task.getId());
            }
            
            assertProcessEnded(procId);
        }
    }
}
