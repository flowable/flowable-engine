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

import static org.assertj.core.api.Assertions.assertThat;

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
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;

public class DynamicBpmnInjectionTest extends PluggableFlowableTestCase {

    @Test
    public void testInjectUserTaskInProcessInstance() {
        deployOneTaskTestProcess();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        
        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);
        
        DynamicUserTaskBuilder taskBuilder = new DynamicUserTaskBuilder();
        taskBuilder.id("custom_task")
            .name("My injected task")
            .assignee("kermit");
        dynamicBpmnService.injectUserTaskInProcessInstance(processInstance.getId(), taskBuilder);

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        
        deploymentIdsForAutoCleanup.add(repositoryService.getProcessDefinition(tasks.get(0).getProcessDefinitionId()).getDeploymentId()); // For auto-cleanup
        
        for (Task t : tasks) {
            taskService.complete(t.getId());
        }

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testInjectParallelTask() {
        deployOneTaskTestProcess();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        
        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);
        
        Task task = taskService.createTaskQuery().singleResult();

        DynamicUserTaskBuilder taskBuilder = new DynamicUserTaskBuilder();
        taskBuilder.id("custom_task")
            .name("My injected task")
            .assignee("kermit");
        dynamicBpmnService.injectParallelUserTask(task.getId(), taskBuilder);
        
        Task injectedTask = taskService.createTaskQuery().taskName("My injected task").singleResult();
        assertThat(injectedTask).isNotNull();
        
        deploymentIdsForAutoCleanup.add(repositoryService.getProcessDefinition(injectedTask.getProcessDefinitionId()).getDeploymentId()); // For auto-cleanup
        
        if (processEngineConfiguration.getPerformanceSettings().isEnableExecutionRelationshipCounts()) {
            Execution execution = runtimeService.createExecutionQuery().executionId(injectedTask.getExecutionId()).singleResult();
            assertThat(((CountingExecutionEntity) execution).getTaskCount()).isEqualTo(1);
        }

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        for (Task t : tasks) {
            taskService.complete(t.getId());
        }
        
        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    @org.flowable.engine.test.Deployment
    public void testOneTaskDi() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        
        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);
        
        Task task = taskService.createTaskQuery().singleResult();

        DynamicUserTaskBuilder taskBuilder = new DynamicUserTaskBuilder();
        taskBuilder.id("custom_task")
            .name("My injected task")
            .assignee("kermit");
        dynamicBpmnService.injectParallelUserTask(task.getId(), taskBuilder);
        
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().processDefinitionKey("oneTaskProcess").list();
        assertThat(processDefinitions).hasSize(2);
        
        ProcessDefinition rootDefinition = null;
        ProcessDefinition derivedFromDefinition = null;
        for (ProcessDefinition definitionItem : processDefinitions) {
            if (definitionItem.getDerivedFrom() != null && definitionItem.getDerivedFromRoot() != null) {
                derivedFromDefinition = definitionItem;
            } else {
                rootDefinition = definitionItem;
            }
        }
        
        assertThat(derivedFromDefinition).isNotNull();
        deploymentIdsForAutoCleanup.add(derivedFromDefinition.getDeploymentId()); // For auto-cleanup
        
        BpmnModel bpmnModel = repositoryService.getBpmnModel(derivedFromDefinition.getId());
        FlowElement taskElement = bpmnModel.getFlowElement("theTask");
        SubProcess subProcessElement = (SubProcess) taskElement.getParentContainer();
        assertThat(subProcessElement).isNotNull();
        GraphicInfo subProcessGraphicInfo = bpmnModel.getGraphicInfo(subProcessElement.getId());
        assertThat(subProcessGraphicInfo).isNotNull();
        assertThat(subProcessGraphicInfo.getExpanded()).isFalse();
        
        BpmnModel rootBpmnModel = repositoryService.getBpmnModel(rootDefinition.getId());
        GraphicInfo taskGraphicInfo = rootBpmnModel.getGraphicInfo("theTask");
        
        assertThat(subProcessGraphicInfo.getX()).isEqualTo(taskGraphicInfo.getX());
        assertThat(subProcessGraphicInfo.getY()).isEqualTo(taskGraphicInfo.getY());
        assertThat(subProcessGraphicInfo.getWidth()).isEqualTo(taskGraphicInfo.getWidth());
        assertThat(subProcessGraphicInfo.getHeight()).isEqualTo(taskGraphicInfo.getHeight());
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(historicProcessInstance.getProcessDefinitionId()).isEqualTo(derivedFromDefinition.getId());
            assertThat(historicProcessInstance.getProcessDefinitionVersion()).isEqualTo(Integer.valueOf(derivedFromDefinition.getVersion()));
            
            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).list();
            assertThat(historicTasks).hasSize(2);
            for (HistoricTaskInstance historicTaskInstance : historicTasks) {
                assertThat(historicTaskInstance.getProcessDefinitionId()).isEqualTo(derivedFromDefinition.getId());
            }
            
            List<HistoricActivityInstance> historicActivities = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).list();
            assertThat(historicActivities).hasSize(5);
            for (HistoricActivityInstance historicActivityInstance : historicActivities) {
                assertActivityInstancesAreSame(historicActivityInstance, runtimeService.createActivityInstanceQuery().activityInstanceId(historicActivityInstance.getId()).singleResult());
                assertThat(historicActivityInstance.getProcessDefinitionId()).isEqualTo(derivedFromDefinition.getId());
            }
        }
        
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        for (Task t : tasks) {
            taskService.complete(t.getId());
        }
        
        assertProcessEnded(processInstance.getId());  
    }

    @Test
    public void testInjectParallelTaskNoJoin() {
        deployOneTaskTestProcess();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        
        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);
        
        Task task = taskService.createTaskQuery().singleResult();

        DynamicUserTaskBuilder taskBuilder = new DynamicUserTaskBuilder();
        taskBuilder.id("custom_task")
            .name("My injected task")
            .assignee("kermit");
        dynamicBpmnService.injectParallelUserTask(task.getId(), taskBuilder);
        
        Task injectedTask = taskService.createTaskQuery().taskName("My injected task").singleResult();
        assertThat(injectedTask).isNotNull();
        
        deploymentIdsForAutoCleanup.add(repositoryService.getProcessDefinition(injectedTask.getProcessDefinitionId()).getDeploymentId()); // For auto-cleanup
        
        if (processEngineConfiguration.getPerformanceSettings().isEnableExecutionRelationshipCounts()) {
            Execution execution = runtimeService.createExecutionQuery().executionId(injectedTask.getExecutionId()).singleResult();
            assertThat(((CountingExecutionEntity) execution).getTaskCount()).isEqualTo(1);
        }

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).hasSize(2);
        assertThat(tasks.get(0).getName()).isEqualTo(taskBuilder.getName());
        taskService.complete(tasks.get(0).getId());

        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("The Task");
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testInjectParallelSubProcessSimple() {
        deployOneTaskTestProcessWithCandidateStarterGroup();
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/bpmn/dynamic/dynamic_onetask.bpmn20.xml")
                .deploy();
        
        deploymentIdsForAutoCleanup.add(deployment.getId());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        
        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);
        
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
            assertThat(((CountingExecutionEntity) execution).getTaskCount()).isEqualTo(1);
        }
        
        List<IdentityLink> identityLinks = repositoryService.getIdentityLinksForProcessDefinition(processInstance.getProcessDefinitionId());
        assertThat(identityLinks).hasSize(1);
        
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        for (Task t : tasks) {
            taskService.complete(t.getId());
        }
        
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testInjectParallelSubProcessComplex() {
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/bpmn/dynamic/dynamic_test_process01.bpmn")
                .addClasspathResource("org/flowable/engine/test/bpmn/dynamic/dynamic_test_process02.bpmn")
                .deploy();
        
        deploymentIdsForAutoCleanup.add(deployment.getId());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess01");
        
        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);
        
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("task A", "task B", "task C", "task D");

        Task taskB = tasks.get(1);
        ProcessDefinition subProcessDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("testProcess02").singleResult();
        dynamicBpmnService.injectParallelEmbeddedSubProcess(taskB.getId(), new DynamicEmbeddedSubProcessBuilder()
                .id("injectedSubProcess")
                .processDefinitionId(subProcessDefinition.getId()));
        
        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        deploymentIdsForAutoCleanup.add(repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId()).getDeploymentId()); // For auto-cleanup

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("five", "four", "one", "task A", "task B", "task C", "task D", "three", "two");

        // first complete the tasks from the original process definition and check that it won't continue to the next task (After B and After sub process).
        taskService.complete(taskService.createTaskQuery().taskName("task A").singleResult().getId());
        taskService.complete(taskService.createTaskQuery().taskName("task B").singleResult().getId());
        taskService.complete(taskService.createTaskQuery().taskName("task C").singleResult().getId());
        taskService.complete(taskService.createTaskQuery().taskName("task D").singleResult().getId());
        
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("five", "four", "one", "three", "two");

        for (Task task : tasks) {
            taskService.complete(task.getId());
        }
       
        // now task After B should be available
        Task afterBTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(afterBTask.getName()).isEqualTo("after B");
        taskService.complete(afterBTask.getId());

        Task afterSubProcessTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(afterSubProcessTask.getName()).isEqualTo("after sub process");
        taskService.complete(afterSubProcessTask.getId());
        assertProcessEnded(processInstance.getId());
    }
    
    @Test
    public void testInjectParallelTask2Times() {
        deployOneTaskTestProcess();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        
        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);
        
        Task task = taskService.createTaskQuery().singleResult();

        DynamicUserTaskBuilder taskBuilder = new DynamicUserTaskBuilder();
        taskBuilder.id("custom_task")
            .name("My injected task")
            .assignee("kermit");
        dynamicBpmnService.injectParallelUserTask(task.getId(), taskBuilder);
        
        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processInstance.getProcessDefinitionId()).singleResult();
        deploymentIdsForAutoCleanup.add(processDefinition.getDeploymentId()); // For auto-cleanup
        assertThat(processDefinition.getDerivedFrom()).isNotNull();
        assertThat(processDefinition.getDerivedFromRoot()).isNotNull();
        assertThat(processDefinition.getDerivedVersion()).isEqualTo(1);

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(2);
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
        assertThat(processDefinition.getDerivedFrom()).isNotNull();
        assertThat(processDefinition.getDerivedFromRoot()).isNotNull();
        assertThat(processDefinition.getDerivedVersion()).isEqualTo(2);

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).hasSize(2);
        for (Task t : tasks) {
            taskService.complete(t.getId());
        }
        assertProcessEnded(processInstance.getId());
    }

    @Test
    public void testInjectParallelSubProcessComplexNoJoin() {
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/bpmn/dynamic/dynamic_test_process01.bpmn")
                .addClasspathResource("org/flowable/engine/test/bpmn/dynamic/dynamic_test_process02.bpmn")
                .deploy();
        
        deploymentIdsForAutoCleanup.add(deployment.getId()); // For auto-cleanup

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess01");
        
        waitForHistoryJobExecutorToProcessAllJobs(10000, 200);
        
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks).hasSize(4);

        Task taskB = tasks.get(1);
        ProcessDefinition subProcessDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("testProcess02").singleResult();
        dynamicBpmnService.injectEmbeddedSubProcessInProcessInstance(processInstance.getId(), new DynamicEmbeddedSubProcessBuilder()
                .id("injectedSubProcess")
                .processDefinitionId(subProcessDefinition.getId()));
        
        processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult();
        deploymentIdsForAutoCleanup.add(repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId()).getDeploymentId()); // For auto-cleanup

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("five", "four", "one", "task A", "task B", "task C", "task D", "three", "two");

        taskService.complete(taskB.getId());
        Task afterBTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("afterB").singleResult();
        assertThat(afterBTask.getId()).isNotNull();
        taskService.complete(afterBTask.getId());
        
        // first complete the tasks from the original process definition and check that it continues to the next task (After sub process).
        taskService.complete(taskService.createTaskQuery().taskName("task A").singleResult().getId());
        taskService.complete(taskService.createTaskQuery().taskName("task C").singleResult().getId());
        taskService.complete(taskService.createTaskQuery().taskName("task D").singleResult().getId());
        
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("after sub process", "five", "four", "one", "three", "two");

        Task afterSubProcessTask = tasks.get(0);
        assertThat(afterSubProcessTask.getName()).isEqualTo("after sub process");
        taskService.complete(afterSubProcessTask.getId());
        
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();
        assertThat(tasks)
                .extracting(Task::getName)
                .containsExactly("five", "four", "one", "three", "two");

        for (Task task : tasks) {
            taskService.complete(task.getId());
        }

        assertProcessEnded(processInstance.getId());
    }

}