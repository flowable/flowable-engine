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
import java.util.GregorianCalendar;
import java.util.List;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.impl.jobexecutor.BpmnHistoryCleanupJobHandler;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

public class HistoricDataDeleteTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/StartToEndTest.testStartToEnd.bpmn20.xml")
    public void testDeleteSingleHistoricProcessInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
            
            HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());
            query.delete();

            HistoricActivityInstanceQuery activityQuery = historyService.createHistoricActivityInstanceQuery();
            activityQuery.finishedBefore(cal.getTime());
            activityQuery.delete();
            
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
        }
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testDeleteSingleHistoricInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        runtimeService.setVariable(processInstance.getId(), "testVar", "testValue");
        runtimeService.setVariable(processInstance.getId(), "numVar", 43);
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);

            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId())).isNotEmpty();
            assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            }
            
            HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());
            query.delete();

            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
            
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            taskService.setVariableLocal(task.getId(), "taskVar", "taskValue");
            taskService.complete(task.getId());
            if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                waitForHistoryJobExecutorToProcessAllJobs(7000, 300);
            }
                    
            query.delete();

            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();

            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId())).isNotEmpty();
            assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            }
                    
            historyService.deleteTaskAndActivityDataOfRemovedHistoricProcessInstances();

            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();

            assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId())).isNotEmpty();
            assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            }
            
            historyService.deleteRelatedDataOfRemovedHistoricProcessInstances();
            
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId())).isEmpty();
            assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count()).isZero();
            }
        }
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testDeleteSingleHistoricInstanceWithSingleMethod() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        runtimeService.setVariable(processInstance.getId(), "testVar", "testValue");
        runtimeService.setVariable(processInstance.getId(), "numVar", 43);
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
            
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            taskService.setVariableLocal(task.getId(), "taskVar", "taskValue");
            taskService.complete(task.getId());

            if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                waitForHistoryJobExecutorToProcessAllJobs(7000, 300);
            }
                    
            HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());
            query.deleteWithRelatedData();
            
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId())).isEmpty();
            assertThat(historyService.getHistoricEntityLinkChildrenForProcessInstance(processInstance.getId())).isEmpty();
            assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count()).isZero();
        }
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testDeleteHistoricInstances() {
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

            if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                waitForHistoryJobExecutorToProcessAllJobs(7000, 300);
            }
                    
            HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());
            query.deleteWithRelatedData();
            
            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(10);
            assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(30);
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(10);
            
            for (int i = 0; i < 20; i++) {
                if (i < 10) {
                    assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).isEmpty();
                    assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    }
                    
                } else {
                    assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).hasSize(1);
                    assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(1);
                    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(2);
                    if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(2);
                    }
                }
            }
        }
    }

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testHistoryCleanupTimerJob() {
        try {
            processEngineConfiguration.setEnableHistoryCleaning(true);
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

                if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                    waitForHistoryJobExecutorToProcessAllJobs(7000, 300);
                }
                        
                managementService.handleHistoryCleanupTimerJob();
                
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
                        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                            assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                        }
                        
                    } else {
                        assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).hasSize(1);
                        assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(1);
                        assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(2);
                        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                            assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(2);
                        }
                    }
                }
                
                managementService.deleteTimerJob(managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).singleResult().getId());
            }
        
        } finally {
            processEngineConfiguration.setEnableHistoryCleaning(false);
            processEngineConfiguration.resetClock();
        }
    }
}
