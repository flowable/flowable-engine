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
package org.activiti.engine.test.jobexecutor;


import org.activiti.engine.impl.test.ResourceFlowableTestCase;
import org.activiti.engine.runtime.JobProcessor;
import org.activiti.engine.runtime.JobProcessorContext;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.JobInfo;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests the functionality of the {@link JobProcessor} and the persistence of the {@link JobInfo#getCustomValues()} field.
 *
 * @author Guy Brand
 * @see JobProcessor
 * @see JobInfo#getCustomValues()
 */

public class JobProcessorTest extends ResourceFlowableTestCase {

    /**
     * Used to test the amount of invocations of the {@link JobProcessor}.
     */
    private static final AtomicInteger CHECK_SUM = new AtomicInteger(0);

    public JobProcessorTest() {
        super("org/activiti/engine/test/jobexecutor/JobProcessorTest.flowable.cfg.xml");
    }


    @Override
    public void setUp() throws Exception {
        super.setUp();
        org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl activiti5ProcessEngineConfig = (org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl) processEngineConfiguration.getFlowable5CompatibilityHandler()
                .getRawProcessConfiguration();
        if (activiti5ProcessEngineConfig.getJobProcessors() == null || activiti5ProcessEngineConfig.getJobProcessors().isEmpty()) {
            activiti5ProcessEngineConfig.setJobProcessors(Collections.<JobProcessor>singletonList(new TestJobProcessor()));
        }
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        CHECK_SUM.set(0);
    }

    @Deployment
    public void testIntermediateTimer() {
        // Arrange
        assertCheckSum(0);

        // Act

        // start the process and because the job processor has been triggered due to the creation
        // of the job, assert that the check sum has been incremented
        runtimeService.startProcessInstanceByKey("intermediateTimer");
        executeJobExecutorForTime(14000, 500);

        // Assert
        assertCheckSum(2);
    }

    @Deployment
    public void testAsyncTask() {
        // Arrange
        assertCheckSum(0);

        // Act

        // start the process and because the job processor has been triggered due to the creation
        // of the job, assert that the check sum has been incremented
        runtimeService.startProcessInstanceByKey("asyncTask");
        executeJobExecutorForTime(14000, 500);

        // Assert
        assertCheckSum(2);
    }

    public static class TestJobProcessor implements JobProcessor {

        private final String randomCustomValues = Double.toString(Math.random());

        @Override
        public void process(JobProcessorContext jobProcessorContext) {
            // increment the check sum to check the amount of invocations
            CHECK_SUM.incrementAndGet();

            if (jobProcessorContext.isInPhase(JobProcessorContext.Phase.BEFORE_CREATE)) {
                // set the random custom values
                jobProcessorContext.getJobEntity().setCustomValues(randomCustomValues);
                jobProcessorContext.getJobEntity().setExceptionStacktrace(randomCustomValues);
            }

            if (jobProcessorContext.isInPhase(JobProcessorContext.Phase.BEFORE_EXECUTE)) {
                // check the random custom value as the before execute phase is executed in the async thread
                String customValues = jobProcessorContext.getJobEntity().getCustomValues();
                assertEquals("The custom values must be equal", randomCustomValues, customValues);
            }
        }
    }

    private static void assertCheckSum(int expectedCheckSum) {
        assertEquals("The checksum must be equal", expectedCheckSum, CHECK_SUM.get());
    }

}