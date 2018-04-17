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
package org.flowable.cmmn.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.List;

import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class CmmnTimerTaskTest extends AbstractProcessEngineIntegrationTest {
    
    @Before
    public void deployTimerProcess() {
        if (cmmnRepositoryService.createDeploymentQuery().count() == 0) {
            cmmnRepositoryService.createDeployment().addClasspathResource("org/flowable/cmmn/test/timerInStage.cmmn").deploy();
        }
    }
    
    @Test
    public void testCmmnTimerTask() {
        Date startTime = setCmmnClockFixedToCurrentTime();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testTimerInStage").start();
        
        assertEquals(1L, cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).planItemInstanceState(PlanItemInstanceState.ACTIVE).count());
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .planItemInstanceState(PlanItemInstanceState.AVAILABLE)
                .planItemDefinitionType(PlanItemDefinitionType.TIMER_EVENT_LISTENER)
                .singleResult();
        assertNotNull(planItemInstance);
        
        assertEquals(1L, cmmnTaskService.createTaskQuery().count());
        
        List<Job> timerJobs = processEngineManagementService.createTimerJobQuery().scopeId(caseInstance.getId()).scopeType(ScopeTypes.CMMN).list();
        assertEquals(1, timerJobs.size());
        String timerJobId = timerJobs.get(0).getId();
        
        timerJobs = processEngineManagementService.createTimerJobQuery().scopeId(caseInstance.getId()).scopeType(ScopeTypes.CMMN).executable().list();
        assertEquals(0, timerJobs.size());
        
        processEngine.getProcessEngineConfiguration().getClock().setCurrentTime(new Date(startTime.getTime() + (3 * 60 * 60 * 1000 + 1)));
        
        timerJobs = processEngineManagementService.createTimerJobQuery().scopeId(caseInstance.getId()).scopeType(ScopeTypes.CMMN).executable().list();
        assertEquals(1, timerJobs.size());
        
        try {
            JobTestHelper.waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(processEngine.getProcessEngineConfiguration(), processEngineManagementService, 5000, 200, true);
            fail("should throw time limit exceeded");
        } catch (FlowableException e) {
            assertEquals("time limit of 5000 was exceeded", e.getMessage());
        }
        
        // Timer fires after 3 hours, so setting it to 3 hours + 1 second
        cmmnEngineConfiguration.getClock().setCurrentTime(new Date(startTime.getTime() + (3 * 60 * 60 * 1000 + 1)));
        
        timerJobs = cmmnManagementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).executable().list();
        assertEquals(1, timerJobs.size());
        assertEquals(timerJobId, timerJobs.get(0).getId());
        
        CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 5000L, 200L, true);
        
        // User task should be active after the timer has triggered
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).orderByTaskName().asc().list();
        assertEquals(2, tasks.size());
        assertEquals("A", tasks.get(0).getName());
        assertEquals("B", tasks.get(1).getName());
        
        cmmnEngineConfiguration.resetClock();
        ((ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration()).resetClock();
        
        for (CmmnDeployment deployment : cmmnRepositoryService.createDeploymentQuery().list()) {
            cmmnRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }
    
}
