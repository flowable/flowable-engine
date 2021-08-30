
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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.FlowableTest;
import org.flowable.job.api.HistoryJob;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@FlowableTest
public class ChangeHistoryLevelTest {

    protected HistoryLevel originalHistoryLevel;

    @BeforeEach
    void getHistoryLevel(ProcessEngineConfiguration configuration) {
        originalHistoryLevel = configuration.getHistoryLevel();
    }

    @AfterEach
    void restoreHistoryLevel(ProcessEngineConfiguration configuration) {
        configuration.setHistoryLevel(originalHistoryLevel);
        List<HistoryJob> historyJobs = configuration.getManagementService().createHistoryJobQuery().list();
        for (HistoryJob historyJob : historyJobs) {
            configuration.getManagementService().deleteHistoryJob(historyJob.getId());
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void noneToActivityTaskComplete(ProcessEngineConfiguration configuration, RuntimeService runtimeService,
            TaskService taskService, HistoryService historyService, ManagementService managementService) {
        
        configuration.setHistoryLevel(HistoryLevel.NONE);
        ProcessInstance oneTaskProcess = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess").start();
        configuration.setHistoryLevel(HistoryLevel.ACTIVITY);
        Task task = taskService.createTaskQuery().processInstanceId(oneTaskProcess.getId()).singleResult();
        taskService.complete(task.getId());

        HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId(task.getId()).singleResult();
        assertThat(historicActivityInstance).isNull();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void noneToFullVariableSet(ProcessEngineConfiguration configuration, RuntimeService runtimeService,
            TaskService taskService, HistoryService historyService, ManagementService managementService) {
        
        configuration.setHistoryLevel(HistoryLevel.NONE);
        ProcessInstance oneTaskProcess = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess")
                .variable("var", "initialValue").start();
        configuration.setHistoryLevel(HistoryLevel.FULL);
        Task task = taskService.createTaskQuery().processInstanceId(oneTaskProcess.getId()).singleResult();
        taskService.complete(task.getId(), Collections.singletonMap("var", "updatedValue"));
        
        if (!((ProcessEngineConfigurationImpl) configuration).isAsyncHistoryEnabled()) {
            HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId(task.getId()).singleResult();
            assertThat(historicActivityInstance).isNull();
            HistoricVariableInstance var = historyService.createHistoricVariableInstanceQuery().processInstanceId(oneTaskProcess.getId()).variableName("var").singleResult();
            assertThat(var.getValue()).isEqualTo("updatedValue");
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void noneToFullClaimTask(ProcessEngineConfiguration configuration, RuntimeService runtimeService,
            TaskService taskService, HistoryService historyService, ManagementService managementService) {
        
        configuration.setHistoryLevel(HistoryLevel.NONE);
        ProcessInstance oneTaskProcess = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess")
                .variable("var", "initialValue").start();
        configuration.setHistoryLevel(HistoryLevel.FULL);
        Task task = taskService.createTaskQuery().processInstanceId(oneTaskProcess.getId()).singleResult();
        taskService.claim(task.getId(), "kermit");
        taskService.complete(task.getId(), Collections.singletonMap("var", "updatedValue"));
        
        HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId(task.getId()).singleResult();
        assertThat(historicActivityInstance).isNull();
    }
}
