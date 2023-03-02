
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
import static org.assertj.core.api.Assumptions.assumeThat;

import java.util.Collections;
import java.util.List;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.HistoryJob;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ChangeHistoryLevelTest extends PluggableFlowableTestCase {

    protected HistoryLevel originalHistoryLevel;

    @BeforeEach
    void getHistoryLevel() {
        originalHistoryLevel = processEngineConfiguration.getHistoryLevel();
    }

    @AfterEach
    void restoreHistoryLevel() {
        processEngineConfiguration.setHistoryLevel(originalHistoryLevel);
        List<HistoryJob> historyJobs = managementService.createHistoryJobQuery().list();
        for (HistoryJob historyJob : historyJobs) {
            managementService.deleteHistoryJob(historyJob.getId());
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void noneToActivityTaskComplete() {
        String processInstanceId = null;
        try {
            processEngineConfiguration.setHistoryLevel(HistoryLevel.NONE);
            ProcessInstance oneTaskProcess = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess").start();
            processInstanceId = oneTaskProcess.getProcessInstanceId();
            processEngineConfiguration.setHistoryLevel(HistoryLevel.ACTIVITY);
            Task task = taskService.createTaskQuery().processInstanceId(oneTaskProcess.getId()).singleResult();
            taskService.complete(task.getId());
    
            HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("theTask").singleResult();
            assertThat(historicActivityInstance).isNull();
            
        } finally {
            deleteHistoricData(processInstanceId, null);
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void noneToFullVariableSet() {
        assumeThat(processEngineConfiguration.isAsyncHistoryEnabled())
                .as("async history")
                .isFalse();
        String processInstanceId = null;
        try {
            processEngineConfiguration.setHistoryLevel(HistoryLevel.NONE);
            ProcessInstance oneTaskProcess = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess")
                    .variable("var", "initialValue").start();
            processInstanceId = oneTaskProcess.getProcessInstanceId();
            processEngineConfiguration.setHistoryLevel(HistoryLevel.FULL);
            Task task = taskService.createTaskQuery().processInstanceId(oneTaskProcess.getId()).singleResult();
            taskService.complete(task.getId(), Collections.singletonMap("var", "updatedValue"));
            
            HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("theTask").singleResult();
            assertThat(historicActivityInstance).isNull();
            HistoricVariableInstance var = historyService.createHistoricVariableInstanceQuery().processInstanceId(oneTaskProcess.getId()).variableName("var").singleResult();
            assertThat(var.getValue()).isEqualTo("updatedValue");
            
        } finally {
            deleteHistoricData(processInstanceId, null);
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void noneToFullClaimTask() {
        String processInstanceId = null;
        String taskId = null;
        try {
            processEngineConfiguration.setHistoryLevel(HistoryLevel.NONE);
            ProcessInstance oneTaskProcess = runtimeService.createProcessInstanceBuilder().processDefinitionKey("oneTaskProcess")
                    .variable("var", "initialValue").start();
            processInstanceId = oneTaskProcess.getProcessInstanceId();
            processEngineConfiguration.setHistoryLevel(HistoryLevel.FULL);
            Task task = taskService.createTaskQuery().processInstanceId(oneTaskProcess.getId()).singleResult();
            taskId = task.getId();
            taskService.claim(task.getId(), "kermit");
            taskService.complete(task.getId(), Collections.singletonMap("var", "updatedValue"));
            
            HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().activityId("theTask").singleResult();
            assertThat(historicActivityInstance).isNull();
            
        } finally {
            deleteHistoricData(processInstanceId, taskId);
        }
    }
    
    protected void deleteHistoricData(String processInstanceId, String taskId) {
        if (processInstanceId != null) {
            historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).delete();
            managementService.executeCommand(new Command<Void>() {

                @Override
                public Void execute(CommandContext commandContext) {
                    processEngineConfiguration.getHistoricDetailEntityManager().deleteHistoricDetailsByProcessInstanceId(processInstanceId);
                    processEngineConfiguration.getVariableServiceConfiguration().getHistoricVariableService().deleteHistoricVariableInstancesByProcessInstanceId(processInstanceId);
                    processEngineConfiguration.getIdentityLinkServiceConfiguration().getHistoricIdentityLinkService().deleteHistoricIdentityLinksByProcessInstanceId(processInstanceId);
                    
                    if (taskId != null) {
                        processEngineConfiguration.getCommentEntityManager().deleteCommentsByTaskId(taskId);
                    }
                    
                    return null;
                }
            });
        }
    }
}
