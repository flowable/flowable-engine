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
import static org.junit.Assert.assertSame;

import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngines;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class CmmnEngineConfiguratorAsyncHistoryTest {
    
    private ProcessEngine processEngine;
    private CmmnEngine cmmnEngine;
    
    @Before
    public void setup() {
        processEngine = ProcessEngineConfiguration.createProcessEngineConfigurationFromResource("flowable.async.history.cfg.xml").buildProcessEngine();
        cmmnEngine = CmmnEngines.getDefaultCmmnEngine();
    }
    
    @After
    public void cleanup() {
        cmmnEngine.close();
        processEngine.close();
    }
    
    @Test
    public void testSharedAsyncHistoryExecutor() {
        // The async history executor should be the same instance
        AsyncExecutor processEngineAsyncExecutor = processEngine.getProcessEngineConfiguration().getAsyncHistoryExecutor();
        AsyncExecutor cmmnEngineAsyncExecutor = cmmnEngine.getCmmnEngineConfiguration().getAsyncHistoryExecutor();
        assertNotNull(processEngineAsyncExecutor);
        assertNotNull(cmmnEngineAsyncExecutor);
        assertSame(processEngineAsyncExecutor, cmmnEngineAsyncExecutor);
        
        // Running them together should have moved the job execution scope to 'all' (from process which is null)
        assertEquals(JobServiceConfiguration.JOB_EXECUTION_SCOPE_ALL, 
                processEngine.getProcessEngineConfiguration().getAsyncHistoryExecutor().getJobServiceConfiguration().getHistoryJobExecutionScope());
        
        // 2 job handlers / engine
        assertEquals(4, processEngineAsyncExecutor.getJobServiceConfiguration().getHistoryJobHandlers().size());
        
        // Deploy and start test processes/cases
        processEngine.getRepositoryService().createDeployment().addClasspathResource("org/flowable/cmmn/test/oneTaskProcess.bpmn20.xml").deploy();
        cmmnEngine.getCmmnRepositoryService().createDeployment().addClasspathResource("org/flowable/cmmn/test/ProcessTaskTest.testOneTaskProcessNonBlocking.cmmn").deploy();
        cmmnEngine.getCmmnRuntimeService().createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
        cmmnEngine.getCmmnRuntimeService().triggerPlanItemInstance(cmmnEngine.getCmmnRuntimeService().createPlanItemInstanceQuery().planItemInstanceState(PlanItemInstanceState.ACTIVE).singleResult().getId());
        
        // As async history is enabled, there should be  no historical entries yet, but there should be history jobs
        assertEquals(0, cmmnEngine.getCmmnHistoryService().createHistoricCaseInstanceQuery().count());
        assertEquals(0, processEngine.getHistoryService().createHistoricProcessInstanceQuery().count());
        assertEquals(2, cmmnEngine.getCmmnManagementService().createHistoryJobQuery().count());
        assertEquals(2, processEngine.getManagementService().createHistoryJobQuery().count());
        
        // Starting the async history executor should process all of these
        CmmnJobTestHelper.waitForAsyncHistoryExecutorToProcessAllJobs(cmmnEngine.getCmmnEngineConfiguration(), 10000L, 200L, true);
        
        assertEquals(1, cmmnEngine.getCmmnHistoryService().createHistoricCaseInstanceQuery().count());
        assertEquals(1, processEngine.getHistoryService().createHistoricProcessInstanceQuery().count());
        assertEquals(0, cmmnEngine.getCmmnManagementService().createHistoryJobQuery().count());
        assertEquals(0, processEngine.getManagementService().createHistoryJobQuery().count());
    }

}
