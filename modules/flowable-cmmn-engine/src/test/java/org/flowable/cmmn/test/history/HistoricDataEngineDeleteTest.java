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
package org.flowable.cmmn.test.history;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.job.CmmnHistoryCleanupJobHandler;
import org.flowable.cmmn.engine.test.CmmnConfigurationResource;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTest;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

@FlowableCmmnTest
@CmmnConfigurationResource("flowable.historyclean.cmmn.cfg.xml")
public class HistoricDataEngineDeleteTest {

    @Test
    @CmmnDeployment(resources="org/flowable/cmmn/test/human-task-milestone-model.cmmn")
    public void testHistoryCleanupTimerJob(CmmnEngineConfiguration cmmnEngineConfiguration, CmmnRuntimeService cmmnRuntimeService,
                    CmmnHistoryService cmmnHistoryService, CmmnTaskService cmmnTaskService, CmmnManagementService cmmnManagementService) {
        
        try {
            Clock clock = cmmnEngineConfiguration.getClock();
            Calendar cal = clock.getCurrentCalendar();
            cal.add(Calendar.DAY_OF_YEAR, -400);
            clock.setCurrentCalendar(cal);
            
            List<String> caseInstanceIds = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
                caseInstanceIds.add(caseInstance.getId());
                cmmnRuntimeService.setVariable(caseInstance.getId(), "testVar", "testValue" + (i + 1));
                cmmnRuntimeService.setVariable(caseInstance.getId(), "numVar", (i + 1));
            }
            
            if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE) {
                
                assertEquals(20, cmmnHistoryService.createHistoricCaseInstanceQuery().count());
                
                for (int i = 0; i < 10; i++) {
                    Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstanceIds.get(i)).singleResult();
                    cmmnTaskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
                    cmmnTaskService.complete(task.getId());
                }
                
                assertEquals(1, cmmnManagementService.createTimerJobQuery().handlerType(CmmnHistoryCleanupJobHandler.TYPE).count());
                
                Job executableJob = cmmnManagementService.moveTimerToExecutableJob(cmmnManagementService.createTimerJobQuery().handlerType(CmmnHistoryCleanupJobHandler.TYPE).singleResult().getId());
                cmmnManagementService.executeJob(executableJob.getId());
                
                assertEquals(1, cmmnManagementService.createTimerJobQuery().handlerType(CmmnHistoryCleanupJobHandler.TYPE).count());
                
                assertEquals(10, cmmnHistoryService.createHistoricCaseInstanceQuery().count());
                assertEquals(20, cmmnHistoryService.createHistoricPlanItemInstanceQuery().count());
                assertEquals(10, cmmnHistoryService.createHistoricTaskInstanceQuery().count());
                
                for (int i = 0; i < 20; i++) {
                    if (i < 10) {
                        assertEquals(0, cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstanceIds.get(i)).size());
                        assertEquals(0, cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstanceIds.get(i)).count());
                        assertEquals(0, cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceIds.get(i)).count());
                        assertEquals(0, cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstanceIds.get(i)).count());
                        
                    } else {
                        assertEquals(1, cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstanceIds.get(i)).size());
                        assertEquals(1, cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstanceIds.get(i)).count());
                        assertEquals(2, cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceIds.get(i)).count());
                        assertEquals(1, cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstanceIds.get(i)).count());
                    }
                }
                
                cmmnManagementService.deleteTimerJob(cmmnManagementService.createTimerJobQuery().handlerType(CmmnHistoryCleanupJobHandler.TYPE).singleResult().getId());
            }
        
        } finally {
            cmmnEngineConfiguration.resetClock();
        }
    }
}