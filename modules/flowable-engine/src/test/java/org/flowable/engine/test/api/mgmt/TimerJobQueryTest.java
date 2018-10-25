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
package org.flowable.engine.test.api.mgmt;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.flowable.engine.impl.jobexecutor.TriggerTimerEventJobHandler;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.Execution;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class TimerJobQueryTest extends PluggableFlowableTestCase {
    
    private Date testStartTime;
    private String processInstanceId;
    
    @BeforeEach
    protected void setUp() throws Exception {
        this.testStartTime = new Date();
        repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/api/mgmt/TimerJobQueryTest.bpmn20.xml").deploy();
        this.processInstanceId = runtimeService.startProcessInstanceByKey("timerJobQueryTest").getId();
    }
    
    @AfterEach
    protected void tearDown() throws Exception {
        for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }
    
    @Test
    public void testByJobId() {
        List<Job> timerJobs = managementService.createTimerJobQuery().list();
        assertEquals(3, timerJobs.size());
        assertEquals(1, managementService.createJobQuery().count());
        
        for (Job job : timerJobs) {
            assertTrue(job instanceof TimerJobEntity);
            assertNotNull(managementService.createTimerJobQuery().jobId(job.getId()));
        }
    }
    
    @Test
    public void testByProcessInstanceId() {
        assertEquals(3, managementService.createTimerJobQuery().processInstanceId(processInstanceId).list().size());
    }
    
    @Test
    public void testByExecutionId() {
        for (String id : Arrays.asList("timerA", "timerB", "timerC")) {
            Execution execution = runtimeService.createExecutionQuery().activityId(id).singleResult();
            assertNotNull(managementService.createTimerJobQuery().executionId(execution.getId()).singleResult());
            assertTrue(managementService.createTimerJobQuery().executionId(execution.getId()).count() > 0);
        }
    }
    
    @Test
    public void testByProcessDefinitionId() {
        String processDefinitionid = repositoryService.createProcessDefinitionQuery().singleResult().getId();
        assertEquals(3, managementService.createTimerJobQuery().processDefinitionId(processDefinitionid).count());
        assertEquals(3, managementService.createTimerJobQuery().processDefinitionId(processDefinitionid).list().size());
    }
    
    @Test
    public void testByExecutable() {
        assertEquals(0, managementService.createTimerJobQuery().executable().count());
        assertEquals(0, managementService.createTimerJobQuery().executable().list().size());
        processEngineConfiguration.getClock().setCurrentTime(new Date(testStartTime.getTime() + (5 * 60 * 1000 * 1000)));
        assertEquals(3, managementService.createTimerJobQuery().executable().count());
        assertEquals(3, managementService.createTimerJobQuery().executable().list().size());
    }
    
    @Test
    public void testByHandlerType() {
        assertEquals(3, managementService.createTimerJobQuery().handlerType(TriggerTimerEventJobHandler.TYPE).count());
        assertEquals(3, managementService.createTimerJobQuery().handlerType(TriggerTimerEventJobHandler.TYPE).list().size());
        assertEquals(0, managementService.createTimerJobQuery().handlerType("invalid").count());
    }
    
    @Test
    public void testByJobType() {
        assertEquals(3, managementService.createTimerJobQuery().timers().count());
        assertEquals(3, managementService.createTimerJobQuery().timers().list().size());
        assertEquals(0, managementService.createTimerJobQuery().messages().count());
        assertEquals(0, managementService.createTimerJobQuery().messages().list().size());
        
        // Executing the async job throws an exception -> job retry + creation of timer
        Job asyncJob = managementService.createJobQuery().singleResult();
        assertNotNull(asyncJob);
        assertThatThrownBy(() -> managementService.executeJob(asyncJob.getId()));
        
        assertEquals(0, managementService.createJobQuery().count());
        assertEquals(3, managementService.createTimerJobQuery().timers().count());
        assertEquals(3, managementService.createTimerJobQuery().timers().list().size());
        assertEquals(1, managementService.createTimerJobQuery().messages().count());
        assertEquals(1, managementService.createTimerJobQuery().messages().list().size());
        
        assertEquals(1, managementService.createTimerJobQuery().withException().count());
        assertEquals(1, managementService.createTimerJobQuery().withException().list().size());
    }
    
    @Test
    public void testByduedateLowerThan() {
        Date date = new Date(testStartTime.getTime() + (10 * 60 * 1000 * 1000));
        assertEquals(3, managementService.createTimerJobQuery().timers().duedateLowerThan(date).count());
        assertEquals(3, managementService.createTimerJobQuery().timers().duedateLowerThan(date).list().size());
    }
    
    @Test
    public void testByDuedateHigherThan() {
        assertEquals(0, managementService.createTimerJobQuery().timers().duedateLowerThan(testStartTime).count());
        assertEquals(0, managementService.createTimerJobQuery().timers().duedateLowerThan(testStartTime).list().size());
    }

}
