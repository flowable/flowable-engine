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
package org.flowable.engine.test.jobexecutor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.JobInfo;
import org.flowable.job.service.JobProcessor;
import org.flowable.job.service.JobProcessorContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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
        super("org/flowable/engine/test/jobexecutor/JobProcessorTest.flowable.cfg.xml");
    }

    @AfterEach
    public void tearDown() throws Exception {
        CHECK_SUM.set(0);
    }

    @Test
    @Deployment
    public void testIntermediateTimer() {
        // Arrange
        assertCheckSum(0);

        // Act

        // start the process and because the job processor has been triggered due to the creation
        // of the job, assert that the check sum has been incremented
        runtimeService.startProcessInstanceByKey("intermediateTimer");
        assertCheckSum(1);
        executeJobExecutorForTime(14000, 500);

        // Assert
        assertCheckSum(2);
    }

    @Test
    @Deployment
    public void testAsyncTask() {
        // Arrange
        assertCheckSum(0);

        // Act

        // start the process and because the job processor has been triggered due to the creation
        // of the job, assert that the check sum has been incremented
        runtimeService.startProcessInstanceByKey("asyncTask");
        assertCheckSum(1);
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
            }

            if (jobProcessorContext.isInPhase(JobProcessorContext.Phase.BEFORE_EXECUTE)) {
                // check the random custom value as the before execute phase is executed in the async thread
                String customValues = jobProcessorContext.getJobEntity().getCustomValues();
                assertThat(customValues).as("The custom values must be equal").isEqualTo(randomCustomValues);
            }
        }
    }

    private static void assertCheckSum(int expectedCheckSum) {
        assertThat(CHECK_SUM.get()).as("The checksum must be equal").isEqualTo(expectedCheckSum);
    }

}