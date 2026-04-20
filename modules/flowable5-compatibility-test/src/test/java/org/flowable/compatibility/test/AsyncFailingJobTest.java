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
package org.flowable.compatibility.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.Callable;

import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.junit.jupiter.api.Test;

public class AsyncFailingJobTest extends AbstractFlowable6CompatibilityTest {

    @Test
    public void testFailingJobFromV5Process() {

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("asyncFailingExpression").singleResult();
        assertNotNull(processInstance);

        // There should be one async job (for the async task)
        final Job job = managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(job);
        assertEquals(3, job.getRetries());

        // Triggering the job should fail
        // Note that we're starting the async executor vs doing executeJob on purpose to test the actual threading behavior
        try {
            processEngineConfiguration.getAsyncExecutor().start();
            JobTestHelper.waitForJobExecutorOnCondition(processEngineConfiguration, 200000L, 256L, new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    Job j = managementService.createJobQuery().jobId(job.getId()).singleResult();
                    return j == null || j.getRetries() == 2;
                }
            });
        } finally {
            processEngineConfiguration.getAsyncExecutor().shutdown();
        }

        // The original job should have been transformed to a timer job
        assertNull(managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull( ((TimerJobEntity) timerJob).getLockExpirationTime());
        assertNull( ((TimerJobEntity) timerJob).getLockOwner());
        assertEquals(2, timerJob.getRetries());


    }

}
