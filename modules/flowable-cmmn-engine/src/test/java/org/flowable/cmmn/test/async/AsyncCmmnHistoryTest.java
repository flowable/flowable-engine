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
package org.flowable.cmmn.test.async;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.identitylink.service.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class AsyncCmmnHistoryTest extends CustomCmmnConfigurationFlowableTestCase {
    
    @Override
    protected String getEngineName() {
        return "AsyncCmmnHistoryTest";
    }
    
    @Override
    protected void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        cmmnEngineConfiguration.setAsyncHistoryEnabled(true);
        cmmnEngineConfiguration.setAsyncExecutorActivate(false);
        cmmnEngineConfiguration.setAsyncHistoryJsonGroupingEnabled(true);
        cmmnEngineConfiguration.setAsyncHistoryJsonGroupingThreshold(1);
        cmmnEngineConfiguration.setAsyncFailedJobWaitTime(100);
        cmmnEngineConfiguration.setDefaultFailedJobWaitTime(100);
        cmmnEngineConfiguration.setAsyncHistoryExecutorNumberOfRetries(10);
        cmmnEngineConfiguration.setAsyncHistoryExecutorDefaultAsyncJobAcquireWaitTime(100);
    }
    
    @Test
    @CmmnDeployment
    public void testCaseInstanceStartAndEnd() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .name("someName")
                .businessKey("someBusinessKey")
                .start();
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().count());
        
        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().count());
        
        HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().singleResult();
        assertEquals(caseInstance.getId(), historicCaseInstance.getId());
        assertEquals("someName", historicCaseInstance.getName());
        assertNull(historicCaseInstance.getParentId());
        assertEquals("someBusinessKey", historicCaseInstance.getBusinessKey());
        assertEquals(caseInstance.getCaseDefinitionId(), historicCaseInstance.getCaseDefinitionId());
        assertEquals(CaseInstanceState.ACTIVE, historicCaseInstance.getState());
        assertNotNull(historicCaseInstance.getStartTime());
        assertNull(historicCaseInstance.getEndTime());
        
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        
        waitForAsyncHistoryExecutorToProcessAllJobs();
        
        assertCaseInstanceEnded(caseInstance);
        historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().singleResult();
        assertEquals(caseInstance.getId(), historicCaseInstance.getId());
        assertEquals("someName", historicCaseInstance.getName());
        assertNull(historicCaseInstance.getParentId());
        assertEquals("someBusinessKey", historicCaseInstance.getBusinessKey());
        assertEquals(caseInstance.getCaseDefinitionId(), historicCaseInstance.getCaseDefinitionId());
        assertEquals(CaseInstanceState.ACTIVE, historicCaseInstance.getState());
        assertNotNull(historicCaseInstance.getStartTime());
        assertNotNull(historicCaseInstance.getEndTime());
    }
    
    @Test
    @CmmnDeployment
    public void testHistoricCaseInstanceDeleted() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .name("someName")
                .businessKey("someBusinessKey")
                .start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());
        waitForAsyncHistoryExecutorToProcessAllJobs();
        
        assertEquals(0, cmmnRuntimeService.createPlanItemInstanceQuery().count());
        assertEquals(1, cmmnHistoryService.createHistoricCaseInstanceQuery().count());
        cmmnHistoryService.deleteHistoricCaseInstance(caseInstance.getId());
        
        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertEquals(0, cmmnHistoryService.createHistoricCaseInstanceQuery().count());
    }
    
    @Test
    @CmmnDeployment
    public void testMilestoneReached() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("caseWithOneMilestone").start();
        assertEquals(1, cmmnRuntimeService.createMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count());
        assertEquals(0, cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count());
        
        waitForAsyncHistoryExecutorToProcessAllJobs();
        
        assertEquals(1, cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).count());
        HistoricMilestoneInstance historicMilestoneInstance =  cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId())
                .singleResult();
        assertEquals("xyzMilestone", historicMilestoneInstance.getName());
        assertEquals("milestonePlanItem1", historicMilestoneInstance.getElementId());
        assertEquals(caseInstance.getId(), historicMilestoneInstance.getCaseInstanceId());
        assertEquals(caseInstance.getCaseDefinitionId(), historicMilestoneInstance.getCaseDefinitionId());
        assertNotNull(historicMilestoneInstance.getTimeStamp());
    }
    
    @Test
    @CmmnDeployment
    public void testIdentityLinks() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("caseWithOneMilestone").start();
        cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "someUser", IdentityLinkType.PARTICIPANT);
        assertEquals(0, cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId()).size());
        
        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertEquals(1, cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId()).size());
        
        cmmnRuntimeService.deleteUserIdentityLink(caseInstance.getId(), "someUser", IdentityLinkType.PARTICIPANT);
        
        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertEquals(0, cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId()).size());
    }
    
    @Test
    @CmmnDeployment
    public void testVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("oneHumanTaskCase").start();
        assertEquals(0, cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).count());

        cmmnRuntimeService.setVariable(caseInstance.getId(), "test", "hello world");
        cmmnRuntimeService.setVariable(caseInstance.getId(), "test2", 2);
        
        // Create
        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertEquals(2, cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).count());
        
        HistoricVariableInstance historicVariableInstance = cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).variableName("test").singleResult();
        assertEquals("test", historicVariableInstance.getVariableName());
        assertEquals(caseInstance.getId(), historicVariableInstance.getScopeId());
        assertEquals(ScopeTypes.CMMN, historicVariableInstance.getScopeType());
        assertEquals("hello world", historicVariableInstance.getValue());
        assertNotNull(historicVariableInstance.getCreateTime());
        assertNotNull(historicVariableInstance.getLastUpdatedTime());
        
        historicVariableInstance = cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).variableName("test2").singleResult();
        assertEquals("test2", historicVariableInstance.getVariableName());
        assertEquals(caseInstance.getId(), historicVariableInstance.getScopeId());
        assertNull(historicVariableInstance.getSubScopeId());
        assertEquals(ScopeTypes.CMMN, historicVariableInstance.getScopeType());
        assertEquals(2, historicVariableInstance.getValue());
        assertNotNull(historicVariableInstance.getCreateTime());
        assertNotNull(historicVariableInstance.getLastUpdatedTime());
        
        // Update
        cmmnRuntimeService.setVariable(caseInstance.getId(), "test", "hello test");
        waitForAsyncHistoryExecutorToProcessAllJobs();
        
        HistoricVariableInstance updatedHistoricVariable = cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).variableName("test").singleResult();
        assertEquals("test", updatedHistoricVariable.getVariableName());
        assertEquals(caseInstance.getId(), updatedHistoricVariable.getScopeId());
        assertNull(updatedHistoricVariable.getSubScopeId());
        assertEquals(ScopeTypes.CMMN, updatedHistoricVariable.getScopeType());
        assertEquals("hello test", updatedHistoricVariable.getValue());
        assertNotNull(updatedHistoricVariable.getCreateTime());
        assertNotNull(updatedHistoricVariable.getLastUpdatedTime());
        assertNotEquals(updatedHistoricVariable.getLastUpdatedTime(), historicVariableInstance.getLastUpdatedTime());
        
        // Delete
        cmmnRuntimeService.removeVariable(caseInstance.getId(), "test");
        waitForAsyncHistoryExecutorToProcessAllJobs();
        assertNull(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).variableName("test").singleResult());
    }

}
