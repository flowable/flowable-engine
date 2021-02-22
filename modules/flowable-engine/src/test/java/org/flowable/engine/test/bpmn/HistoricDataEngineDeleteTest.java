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
package org.flowable.engine.test.bpmn;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.engine.impl.jobexecutor.BpmnHistoryCleanupJobHandler;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

public class HistoricDataEngineDeleteTest extends ResourceFlowableTestCase {

    public HistoricDataEngineDeleteTest() {
        super("org/flowable/engine/test/bpmn/HistoricDataEngineDeleteTest.flowable.cfg.xml");
    }

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testHistoryCleanupTimerJob() {
        try {
            Clock clock = processEngineConfiguration.getClock();
            Calendar cal = clock.getCurrentCalendar();
            cal.add(Calendar.DAY_OF_YEAR, -400);
            clock.setCurrentCalendar(cal);
            
            List<String> processInstanceIds = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
                processInstanceIds.add(processInstance.getId());
                runtimeService.setVariable(processInstance.getId(), "testVar", "testValue" + (i + 1));
                runtimeService.setVariable(processInstance.getId(), "numVar", (i + 1));
            }
            
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                
                assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(20);
                
                for (int i = 0; i < 10; i++) {
                    Task task = taskService.createTaskQuery().processInstanceId(processInstanceIds.get(i)).singleResult();
                    taskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
                    taskService.complete(task.getId());
                }
                
                assertThat(managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).count()).isEqualTo(1);
                
                Job executableJob = managementService.moveTimerToExecutableJob(managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).singleResult().getId());
                managementService.executeJob(executableJob.getId());
                
                assertThat(managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).count()).isEqualTo(1);
                
                assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(10);
                assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(30);
                assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(10);
                
                for (int i = 0; i < 20; i++) {
                    if (i < 10) {
                        assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).isEmpty();
                        assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                        assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                        
                    } else {
                        assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).hasSize(1);
                        assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(1);
                        assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(2);
                        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(2);
                    }
                }
                
                managementService.deleteTimerJob(managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).singleResult().getId());
            }
        
        } finally {
            processEngineConfiguration.resetClock();
        }
    }
}