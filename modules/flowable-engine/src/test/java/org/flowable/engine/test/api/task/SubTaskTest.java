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

package org.flowable.engine.test.api.task;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.impl.persistence.CountingTaskEntity;

/**
 * @author Tom Baeyens
 * @author Tijs Rademakers
 */
public class SubTaskTest extends PluggableFlowableTestCase {

    public void testSubTask() {
        Task gonzoTask = taskService.newTask();
        gonzoTask.setName("gonzoTask");
        taskService.saveTask(gonzoTask);

        Task subTaskOne = taskService.newTask();
        subTaskOne.setName("subtask one");
        String gonzoTaskId = gonzoTask.getId();
        subTaskOne.setParentTaskId(gonzoTaskId);
        taskService.saveTask(subTaskOne);

        Task subTaskTwo = taskService.newTask();
        subTaskTwo.setName("subtask two");
        subTaskTwo.setParentTaskId(gonzoTaskId);
        taskService.saveTask(subTaskTwo);
        
        String subTaskId = subTaskOne.getId();
        assertTrue(taskService.getSubTasks(subTaskId).isEmpty());
        assertTrue(historyService.createHistoricTaskInstanceQuery().taskParentTaskId(subTaskId).list().isEmpty());

        List<Task> subTasks = taskService.getSubTasks(gonzoTaskId);
        Set<String> subTaskNames = new HashSet<>();
        for (Task subTask : subTasks) {
            subTaskNames.add(subTask.getName());
        }

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            Set<String> expectedSubTaskNames = new HashSet<>();
            expectedSubTaskNames.add("subtask one");
            expectedSubTaskNames.add("subtask two");

            assertEquals(expectedSubTaskNames, subTaskNames);

            List<HistoricTaskInstance> historicSubTasks = historyService.createHistoricTaskInstanceQuery().taskParentTaskId(gonzoTaskId).list();

            subTaskNames = new HashSet<>();
            for (HistoricTaskInstance historicSubTask : historicSubTasks) {
                subTaskNames.add(historicSubTask.getName());
            }

            assertEquals(expectedSubTaskNames, subTaskNames);
        }

        taskService.deleteTask(gonzoTaskId, true);
    }
    
    public void testMakeSubTaskStandaloneTask() {
        Task parentTask = taskService.newTask();
        parentTask.setName("parent");
        taskService.saveTask(parentTask);

        Task subTaskOne = taskService.newTask();
        subTaskOne.setName("subtask one");
        subTaskOne.setParentTaskId(parentTask.getId());
        taskService.saveTask(subTaskOne);

        Task subTaskTwo = taskService.newTask();
        subTaskTwo.setName("subtask two");
        subTaskTwo.setParentTaskId(parentTask.getId());
        taskService.saveTask(subTaskTwo);

        assertEquals(2, taskService.getSubTasks(parentTask.getId()).size());
        
        if (processEngineConfiguration.getPerformanceSettings().isEnableTaskRelationshipCounts()) {
            CountingTaskEntity countingTaskEntity = (CountingTaskEntity) taskService.createTaskQuery().taskId(parentTask.getId()).singleResult();
            assertEquals(2, countingTaskEntity.getSubTaskCount());
        }
        
        subTaskTwo = taskService.createTaskQuery().taskId(subTaskTwo.getId()).singleResult();
        subTaskTwo.setParentTaskId(null);
        taskService.saveTask(subTaskTwo);
        
        if (processEngineConfiguration.getPerformanceSettings().isEnableTaskRelationshipCounts()) {
            CountingTaskEntity countingTaskEntity = (CountingTaskEntity) taskService.createTaskQuery().taskId(parentTask.getId()).singleResult();
            assertEquals(1, countingTaskEntity.getSubTaskCount());
        }
        
        assertEquals(1, taskService.getSubTasks(parentTask.getId()).size());
        taskService.deleteTask(parentTask.getId(), true);
        taskService.deleteTask(subTaskTwo.getId(), true);
    }

    public void testSubTaskDeleteOnProcessInstanceDelete() {
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
                .deploy();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.setAssignee(task.getId(), "test");

        Task subTask1 = taskService.newTask();
        subTask1.setName("Sub task 1");
        subTask1.setParentTaskId(task.getId());
        subTask1.setAssignee("test");
        taskService.saveTask(subTask1);

        Task subTask2 = taskService.newTask();
        subTask2.setName("Sub task 2");
        subTask2.setParentTaskId(task.getId());
        subTask2.setAssignee("test");
        taskService.saveTask(subTask2);

        List<Task> tasks = taskService.createTaskQuery().taskAssignee("test").list();
        assertEquals(3, tasks.size());

        runtimeService.deleteProcessInstance(processInstance.getId(), "none");

        tasks = taskService.createTaskQuery().taskAssignee("test").list();
        assertEquals(0, tasks.size());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery().taskAssignee("test").list();
            assertEquals(3, historicTasks.size());

            historyService.deleteHistoricProcessInstance(processInstance.getId());
            
            waitForHistoryJobExecutorToProcessAllJobs(5000, 100);

            historicTasks = historyService.createHistoricTaskInstanceQuery().taskAssignee("test").list();
            assertEquals(0, historicTasks.size());
        }

        repositoryService.deleteDeployment(deployment.getId(), true);
    }

}
