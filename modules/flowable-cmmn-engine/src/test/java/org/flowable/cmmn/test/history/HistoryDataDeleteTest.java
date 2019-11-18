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
import java.util.GregorianCalendar;
import java.util.List;

import org.flowable.cmmn.api.history.HistoricCaseInstanceQuery;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class HistoryDataDeleteTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources="org/flowable/cmmn/test/one-human-task-model.cmmn")
    public void testDeleteSingleHistoricCaseInstance() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneTaskCase").start();
        cmmnRuntimeService.setVariable(caseInstance.getId(), "testVar", "testValue");
        cmmnRuntimeService.setVariable(caseInstance.getId(), "numVar", 43);
        
        if (cmmnEngineConfiguration.getHistoryLevel() != HistoryLevel.NONE) {
            assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count());
            
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            cmmnTaskService.setVariableLocal(task.getId(), "taskVar", "taskValue");
            cmmnTaskService.complete(task.getId());
                
            HistoricCaseInstanceQuery query = cmmnHistoryService.createHistoricCaseInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());
                    
            query.deleteWithRelatedData();
            
            assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count());
            assertEquals(0, cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).count());
            assertEquals(0, cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).count());
            assertEquals(0, cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId()).size());
            assertEquals(0, cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId()).size());
            assertEquals(0, cmmnHistoryService.createHistoricTaskLogEntryQuery().caseInstanceId(caseInstance.getId()).count());
            assertEquals(0, cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).count());
        }
    }
    
    @Test
    @CmmnDeployment(resources="org/flowable/cmmn/test/human-task-milestone-model.cmmn")
    public void testDeleteHistoricInstances() {
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
                    
            HistoricCaseInstanceQuery query = cmmnHistoryService.createHistoricCaseInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());
            query.deleteWithRelatedData();
            
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
        }
    }
}
