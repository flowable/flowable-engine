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
import org.flowable.engine.impl.HistoricActivityInstanceQueryImpl;
import org.flowable.engine.impl.HistoricProcessInstanceQueryImpl;
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
            
            assertEquals(1, historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count());
            
            HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());
            query.delete();

            HistoricActivityInstanceQuery activityQuery = historyService.createHistoricActivityInstanceQuery();
            activityQuery.finishedBefore(cal.getTime());
            activityQuery.delete();
            
            assertEquals(0, historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count());
            assertEquals(0, historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count());
        }
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testDeleteSingleHistoricInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        runtimeService.setVariable(processInstance.getId(), "testVar", "testValue");
        runtimeService.setVariable(processInstance.getId(), "numVar", 43);
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            
            assertEquals(1, historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count());

            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId()).size()).isGreaterThan(0);
            assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            
            HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());
            query.delete();

            assertEquals(1, historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count());
            
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            taskService.setVariableLocal(task.getId(), "taskVar", "taskValue");
            taskService.complete(task.getId());
                    
            query.delete();

            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(0);

            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId()).size()).isGreaterThan(0);
            assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
                    
            historyService.deleteTaskAndActivityDataOfRemovedHistoricProcessInstances();

            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(0);
            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(0);
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(0);

            assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId()).size()).isGreaterThan(0);
            assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            
            historyService.deleteRelatedDataOfRemovedHistoricProcessInstances();
            
            assertEquals(0, historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count());
            assertEquals(0, historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count());
            assertEquals(0, historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count());
            assertEquals(0, historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId()).size());
            assertEquals(0, historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstance.getId()).count());
            assertEquals(0, historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count());
            assertEquals(0, historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count());
        }
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testDeleteSingleHistoricInstanceWithSingleMethod() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        runtimeService.setVariable(processInstance.getId(), "testVar", "testValue");
        runtimeService.setVariable(processInstance.getId(), "numVar", 43);
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            
            assertEquals(1, historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count());
            
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            taskService.setVariableLocal(task.getId(), "taskVar", "taskValue");
            taskService.complete(task.getId());
                    
            HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());
            query.deleteWithRelatedData();
            
            assertEquals(0, historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count());
            assertEquals(0, historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count());
            assertEquals(0, historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count());
            assertEquals(0, historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId()).size());
            assertEquals(0, historyService.getHistoricEntityLinkChildrenForProcessInstance(processInstance.getId()).size());
            assertEquals(0, historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstance.getId()).count());
            assertEquals(0, historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count());
            assertEquals(0, historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count());
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
            
            assertEquals(20, historyService.createHistoricProcessInstanceQuery().count());
            
            for (int i = 0; i < 10; i++) {
                Task task = taskService.createTaskQuery().processInstanceId(processInstanceIds.get(i)).singleResult();
                taskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
                taskService.complete(task.getId());
            }
                    
            HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());
            query.deleteWithRelatedData();
            
            assertEquals(10, historyService.createHistoricProcessInstanceQuery().count());
            assertEquals(30, historyService.createHistoricActivityInstanceQuery().count());
            assertEquals(10, historyService.createHistoricTaskInstanceQuery().count());
            
            for (int i = 0; i < 20; i++) {
                if (i < 10) {
                    assertEquals(0, historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i)).size());
                    assertEquals(0, historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count());
                    assertEquals(0, historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count());
                    assertEquals(0, historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count());
                    
                } else {
                    assertEquals(1, historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i)).size());
                    assertEquals(1, historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count());
                    assertEquals(2, historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count());
                    assertEquals(2, historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count());
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
                
                assertEquals(20, historyService.createHistoricProcessInstanceQuery().count());
                
                for (int i = 0; i < 10; i++) {
                    Task task = taskService.createTaskQuery().processInstanceId(processInstanceIds.get(i)).singleResult();
                    taskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
                    taskService.complete(task.getId());
                }
                        
                managementService.handleHistoryCleanupTimerJob();
                
                assertEquals(1, managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).count());
                
                Job executableJob = managementService.moveTimerToExecutableJob(managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).singleResult().getId());
                managementService.executeJob(executableJob.getId());
                
                assertEquals(1, managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).count());
                
                assertEquals(10, historyService.createHistoricProcessInstanceQuery().count());
                assertEquals(30, historyService.createHistoricActivityInstanceQuery().count());
                assertEquals(10, historyService.createHistoricTaskInstanceQuery().count());
                
                for (int i = 0; i < 20; i++) {
                    if (i < 10) {
                        assertEquals(0, historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i)).size());
                        assertEquals(0, historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count());
                        assertEquals(0, historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count());
                        assertEquals(0, historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count());
                        
                    } else {
                        assertEquals(1, historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i)).size());
                        assertEquals(1, historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count());
                        assertEquals(2, historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count());
                        assertEquals(2, historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count());
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
