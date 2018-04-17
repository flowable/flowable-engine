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
package org.flowable.engine.test.bpmn.dynamic;

import java.util.Arrays;
import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.GraphicInfo;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.dynamic.DynamicEmbeddedSubProcessBuilder;
import org.flowable.engine.impl.dynamic.DynamicUserTaskBuilder;
import org.flowable.engine.impl.persistence.CountingExecutionEntity;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;

public class DynamicBpmnInjectionTest extends PluggableFlowableTestCase {

    public void testInjectUserTaskInProcessInstance() {
        deployOneTaskTestProcess();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        DynamicUserTaskBuilder taskBuilder = new DynamicUserTaskBuilder();
        taskBuilder.id("custom_task")
            .name("My injected task")
            .assignee("kermit");
        dynamicBpmnService.injectUserTaskInProcessInstance(processInstance.getId(), taskBuilder);

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());
        
        deploymentIdsForAutoCleanup.add(repositoryService.getProcessDefinition(tasks.get(0).getProcessDefinitionId()).getDeploymentId()); // For auto-cleanup
        
        for (Task t : tasks) {
            taskService.complete(t.getId());
        }

        assertProcessEnded(processInstance.getId());
    }

    public void testInjectParallelTask() {
        deployOneTaskTestProcess();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().singleResult();

        DynamicUserTaskBuilder taskBuilder = new DynamicUserTaskBuilder();
        taskBuilder.id("custom_task")
            .name("My injected task")
            .assignee("kermit");
        dynamicBpmnService.injectParallelUserTask(task.getId(), taskBuilder);
        
        Task injectedTask = taskService.createTaskQuery().taskName("My injected task").singleResult();
        assertNotNull(injectedTask);
        
        deploymentIdsForAutoCleanup.add(repositoryService.getProcessDefinition(injectedTask.getProcessDefinitionId()).getDeploymentId()); // For auto-cleanup
        
        if (processEngineConfiguration.getPerformanceSettings().isEnableExecutionRelationshipCounts()) {
            Execution execution = runtimeService.createExecutionQuery().executionId(injectedTask.getExecutionId()).singleResult();
            assertEquals(1, ((CountingExecutionEntity) execution).getTaskCount());
        }

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());
        for (Task t : tasks) {
            taskService.complete(t.getId());
        }
        assertProcessEnded(processInstance.getId());
    }
    
    @org.flowable.engine.test.Deployment
    public void testOneTaskDi() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().singleResult();

        DynamicUserTaskBuilder taskBuilder = new DynamicUserTaskBuilder();
        taskBuilder.id("custom_task")
            .name("My injected task")
            .assignee("kermit");
        dynamicBpmnService.injectParallelUserTask(task.getId(), taskBuilder);
        
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").list();
        assertEquals(2, processDefinitions.size());
        
        ProcessDefinition rootDefinition = null;
        ProcessDefinition derivedFromDefinition = null;
        for (ProcessDefinition definitionItem : processDefinitions) {
            if (definitionItem.getDerivedFrom() != null && definitionItem.getDerivedFromRoot() != null) {
                derivedFromDefinition = definitionItem;
            } else {
                rootDefinition = definitionItem;
            }
        }
        
        assertNotNull(derivedFromDefinition);
        deploymentIdsForAutoCleanup.add(derivedFromDefinition.getDeploymentId()); // For auto-cleanup
        
        BpmnModel bpmnModel = repositoryService.getBpmnModel(derivedFromDefinition.getId());
        FlowElement taskElement = bpmnModel.getFlowElement("theTask");
        SubProcess subProcessElement = (SubProcess) taskElement.getParentContainer();
        assertNotNull(subProcessElement);
        GraphicInfo subProcessGraphicInfo = bpmnModel.getGraphicInfo(subProcessElement.getId());
        assertNotNull(subProcessGraphicInfo);
        assertFalse(subProcessGraphicInfo.getExpanded());
        
        BpmnModel rootBpmnModel = repositoryService.getBpmnModel(rootDefinition.getId());
        GraphicInfo taskGraphicInfo = rootBpmnModel.getGraphicInfo("theTask");
        
        assertEquals(taskGraphicInfo.getX(), subProcessGraphicInfo.getX());
        assertEquals(taskGraphicInfo.getY(), subProcessGraphicInfo.getY());
        assertEquals(taskGraphicInfo.getWidth(), subProcessGraphicInfo.getWidth());
        assertEquals(taskGraphicInfo.getHeight(), subProcessGraphicInfo.getHeight());
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
            assertEquals(derivedFromDefinition.getId(), historicProcessInstance.getProcessDefinitionId());
            assertEquals(Integer.valueOf(derivedFromDefinition.getVersion()), historicProcessInstance.getProcessDefinitionVersion());
            
            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).list();
            assertEquals(2, historicTasks.size());
            for (HistoricTaskInstance historicTaskInstance : historicTasks) {
                assertEquals(derivedFromDefinition.getId(), historicTaskInstance.getProcessDefinitionId());
            }
            
            List<HistoricActivityInstance> historicActivities = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).list();
            assertEquals(4, historicActivities.size());
            for (HistoricActivityInstance historicActivityInstance : historicActivities) {
                assertEquals(derivedFromDefinition.getId(), historicActivityInstance.getProcessDefinitionId());
            }
        }
        
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());
        for (Task t : tasks) {
            taskService.complete(t.getId());
        }
        assertProcessEnded(processInstance.getId());  
    }

    public void testInjectParallelTaskNoJoin() {
        deployOneTaskTestProcess();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().singleResult();

        DynamicUserTaskBuilder taskBuilder = new DynamicUserTaskBuilder();
        taskBuilder.id("custom_task")
            .name("My injected task")
            .assignee("kermit");
        dynamicBpmnService.injectParallelUserTask(task.getId(), taskBuilder);
        
        Task injectedTask = taskService.createTaskQuery().taskName("My injected task").singleResult();
        assertNotNull(injectedTask);
        
        deploymentIdsForAutoCleanup.add(repositoryService.getProcessDefinition(injectedTask.getProcessDefinitionId()).getDeploymentId()); // For auto-cleanup
        
        if (processEngineConfiguration.getPerformanceSettings().isEnableExecutionRelationshipCounts()) {
            Execution execution = runtimeService.createExecutionQuery().executionId(injectedTask.getExecutionId()).singleResult();
            assertEquals(1, ((CountingExecutionEntity) execution).getTaskCount());
        }

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals(taskBuilder.getName(), tasks.get(0).getName());
        taskService.complete(tasks.get(0).getId());

        task = taskService.createTaskQuery().singleResult();
        assertEquals("The Task", task.getName());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    public void testInjectParallelSubProcessSimple() {
        deployOneTaskTestProcess();
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/bpmn/dynamic/dynamic_onetask.bpmn20.xml")
                .deploy();
        
        deploymentIdsForAutoCleanup.add(deployment.getId());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().singleResult();

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("oneTaskV2")
                .singleResult();
        
        DynamicEmbeddedSubProcessBuilder subProcessBuilder = new DynamicEmbeddedSubProcessBuilder()
                .id("customSubprocess")
                .processDefinitionId(processDefinition.getId());
        dynamicBpmnService.injectParallelEmbeddedSubProcess(task.getId(), subProcessBuilder);
        
        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        deploymentIdsForAutoCleanup.add(repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId()).getDeploymentId()); // For auto-cleanup
        
        if (processEngineConfiguration.getPerformanceSettings().isEnableExecutionRelationshipCounts()) {
            Execution execution = runtimeService.createExecutionQuery().activityId("usertaskV2").singleResult();
            assertEquals(1, ((CountingExecutionEntity) execution).getTaskCount());
        }
        
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());
        for (Task t : tasks) {
            taskService.complete(t.getId());
        }
        
        assertProcessEnded(processInstance.getId());
    }

    public void testInjectParallelSubProcessComplex() {
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/bpmn/dynamic/dynamic_test_process01.bpmn")
                .addClasspathResource("org/flowable/engine/test/bpmn/dynamic/dynamic_test_process02.bpmn")
                .deploy();
        
        deploymentIdsForAutoCleanup.add(deployment.getId());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess01");
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertEquals(4, tasks.size());
        assertEquals("task A", tasks.get(0).getName());
        assertEquals("task B", tasks.get(1).getName());
        assertEquals("task C", tasks.get(2).getName());
        assertEquals("task D", tasks.get(3).getName());

        Task taskB = tasks.get(1);
        ProcessDefinition subProcessDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("testProcess02").singleResult();
        dynamicBpmnService.injectParallelEmbeddedSubProcess(taskB.getId(), new DynamicEmbeddedSubProcessBuilder()
                .id("injectedSubProcess")
                .processDefinitionId(subProcessDefinition.getId()));
        
        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        deploymentIdsForAutoCleanup.add(repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId()).getDeploymentId()); // For auto-cleanup

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertEquals(9, tasks.size());
        List<String> expectedTaskNames = Arrays.asList("five", "four", "one", "task A", "task B", "task C", "task D", "three", "two");
        for (int i=0; i<expectedTaskNames.size(); i++) {
            assertEquals(expectedTaskNames.get(i), tasks.get(i).getName());
        }
        
        // first complete the tasks from the original process definition and check that it won't continue to the next task (After B and After sub process).
        taskService.complete(taskService.createTaskQuery().taskName("task A").singleResult().getId());
        taskService.complete(taskService.createTaskQuery().taskName("task B").singleResult().getId());
        taskService.complete(taskService.createTaskQuery().taskName("task C").singleResult().getId());
        taskService.complete(taskService.createTaskQuery().taskName("task D").singleResult().getId());
        
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        expectedTaskNames = Arrays.asList("five", "four", "one", "three", "two");
        for (int i=0; i<expectedTaskNames.size(); i++) {
            assertEquals(expectedTaskNames.get(i), tasks.get(i).getName());
        }

        for (Task task : tasks) {
            taskService.complete(task.getId());
        }
       
        // now task After B should be available
        Task afterBTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("after B", afterBTask.getName());
        taskService.complete(afterBTask.getId());

        Task afterSubProcessTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("after sub process", afterSubProcessTask.getName());
        taskService.complete(afterSubProcessTask.getId());
        assertProcessEnded(processInstance.getId());
    }
    
    public void testInjectParallelTask2Times() {
        deployOneTaskTestProcess();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().singleResult();

        DynamicUserTaskBuilder taskBuilder = new DynamicUserTaskBuilder();
        taskBuilder.id("custom_task")
            .name("My injected task")
            .assignee("kermit");
        dynamicBpmnService.injectParallelUserTask(task.getId(), taskBuilder);
        
        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processInstance.getProcessDefinitionId()).singleResult();
        deploymentIdsForAutoCleanup.add(processDefinition.getDeploymentId()); // For auto-cleanup
        assertNotNull(processDefinition.getDerivedFrom());
        assertNotNull(processDefinition.getDerivedFromRoot());
        assertEquals(1, processDefinition.getDerivedVersion());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());
        for (Task t : tasks) {
            taskService.complete(t.getId());
        }
        assertProcessEnded(processInstance.getId());
        
        processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        task = taskService.createTaskQuery().singleResult();
        
        taskBuilder = new DynamicUserTaskBuilder()
                .id("custom_task")
                .name("My injected task")
                .assignee("kermit");
        dynamicBpmnService.injectParallelUserTask(task.getId(), taskBuilder);
        
        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processInstance.getProcessDefinitionId()).singleResult();
        deploymentIdsForAutoCleanup.add(processDefinition.getDeploymentId()); // For auto-cleanup
        assertNotNull(processDefinition.getDerivedFrom());
        assertNotNull(processDefinition.getDerivedFromRoot());
        assertEquals(2, processDefinition.getDerivedVersion());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());
        for (Task t : tasks) {
            taskService.complete(t.getId());
        }
        assertProcessEnded(processInstance.getId());
    }

    public void testInjectParallelSubProcessComplexNoJoin() {
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/bpmn/dynamic/dynamic_test_process01.bpmn")
                .addClasspathResource("org/flowable/engine/test/bpmn/dynamic/dynamic_test_process02.bpmn")
                .deploy();
        
        deploymentIdsForAutoCleanup.add(deployment.getId()); // For auto-cleanup

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess01");
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertEquals(4, tasks.size());

        Task taskB = tasks.get(1);
        ProcessDefinition subProcessDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("testProcess02").singleResult();
        dynamicBpmnService.injectEmbeddedSubProcessInProcessInstance(processInstance.getId(), new DynamicEmbeddedSubProcessBuilder()
                .id("injectedSubProcess")
                .processDefinitionId(subProcessDefinition.getId()));
        
        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        deploymentIdsForAutoCleanup.add(repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId()).getDeploymentId()); // For auto-cleanup

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertEquals(9, tasks.size());
        List<String> expectedTaskNames = Arrays.asList("five", "four", "one", "task A", "task B", "task C", "task D", "three", "two");
        for (int i = 0; i < expectedTaskNames.size(); i++) {
            assertEquals(expectedTaskNames.get(i), tasks.get(i).getName());
        }

        taskService.complete(taskB.getId());
        Task afterBTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("afterB").singleResult();
        assertNotNull(afterBTask.getId());
        taskService.complete(afterBTask.getId());
        
        // first complete the tasks from the original process definition and check that it continues to the next task (After sub process).
        taskService.complete(taskService.createTaskQuery().taskName("task A").singleResult().getId());
        taskService.complete(taskService.createTaskQuery().taskName("task C").singleResult().getId());
        taskService.complete(taskService.createTaskQuery().taskName("task D").singleResult().getId());
        
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        expectedTaskNames = Arrays.asList("after sub process", "five", "four", "one", "three", "two");
        for (int i = 0; i < expectedTaskNames.size(); i++) {
            assertEquals(expectedTaskNames.get(i), tasks.get(i).getName());
        }
        
        Task afterSubProcessTask = tasks.get(0);
        assertEquals("after sub process", afterSubProcessTask.getName());
        taskService.complete(afterSubProcessTask.getId());
        
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        expectedTaskNames = Arrays.asList("five", "four", "one", "three", "two");
        for (int i = 0; i < expectedTaskNames.size(); i++) {
            assertEquals(expectedTaskNames.get(i), tasks.get(i).getName());
        }

        for (Task task : tasks) {
            taskService.complete(task.getId());
        }

        assertProcessEnded(processInstance.getId());
    }

}