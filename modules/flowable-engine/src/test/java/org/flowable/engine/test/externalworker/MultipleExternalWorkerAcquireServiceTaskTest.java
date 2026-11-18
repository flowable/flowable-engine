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
package org.flowable.engine.test.externalworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.DefaultInternalJobManager;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.impl.CustomConfigurationFlowableTestCase;
import org.flowable.job.api.AcquiredExternalWorkerJob;
import org.flowable.job.api.ExternalWorkerJob;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
public class MultipleExternalWorkerAcquireServiceTaskTest extends CustomConfigurationFlowableTestCase {

    protected WaitingInternalJobManager waitingInternalJobManager;

    public MultipleExternalWorkerAcquireServiceTaskTest() {
        super("multipleExternalWorkerAcquireTest");
    }

    @Override
    protected void configureConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        waitingInternalJobManager = new WaitingInternalJobManager(processEngineConfiguration);
        processEngineConfiguration.setInternalJobManager(waitingInternalJobManager);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.bpmn20.xml")
    void testAcquireJobsInTheSameTime() throws ExecutionException, InterruptedException {
        for (int i = 0; i < 5; i++) {
            runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("simpleExternalWorker")
                    .businessKey("process" + i)
                    .variable("name", "kermit")
                    .start();
        }

        assertThat(managementService.createExternalWorkerJobQuery().list())
                .hasSize(5)
                .extracting(ExternalWorkerJob::getLockOwner)
                .containsOnlyNulls();

        // Both workers have to query the jobs before any of them locks them
        waitingInternalJobManager.acquireLatch = new CountDownLatch(2);

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        CompletableFuture<List<AcquiredExternalWorkerJob>> testWorker1 = CompletableFuture
                .supplyAsync(() -> managementService.createExternalWorkerJobAcquireBuilder()
                        .topic("simple", Duration.ofMinutes(30))
                        .acquireAndLock(3, "testWorker1"), executorService);

        CompletableFuture<List<AcquiredExternalWorkerJob>> testWorker2 = CompletableFuture
                .supplyAsync(() -> managementService.createExternalWorkerJobAcquireBuilder()
                        .topic("simple", Duration.ofMinutes(30))
                        .acquireAndLock(3, "testWorker2"), executorService);

        List<AcquiredExternalWorkerJob> worker1Jobs = testWorker1.get();
        List<AcquiredExternalWorkerJob> worker2Jobs = testWorker2.get();

        assertThat(worker1Jobs).isNotEmpty();
        assertThat(worker2Jobs).isNotEmpty();

        executorService.shutdown();

        assertThat(managementService.createExternalWorkerJobQuery().list())
                .extracting(ExternalWorkerJob::getLockOwner)
                .containsOnly("testWorker1", "testWorker2");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/externalworker/ExternalWorkerServiceTaskTest.testSimple.bpmn20.xml")
    void testAcquireJobsInTheSameTimeWithNoRetries() throws ExecutionException, InterruptedException {
        String databaseType = processEngineConfiguration.getDatabaseType();
        assumeThat(databaseType)
                .withFailMessage("Test is disabled on Oracle since Oracle manages to acquire the correct jobs for both workers.")
                .isNotEqualTo(AbstractEngineConfiguration.DATABASE_TYPE_ORACLE);
        for (int i = 0; i < 5; i++) {
            runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("simpleExternalWorker")
                    .businessKey("process" + i)
                    .variable("name", "kermit")
                    .start();
        }

        assertThat(managementService.createExternalWorkerJobQuery().list())
                .hasSize(5)
                .extracting(ExternalWorkerJob::getLockOwner)
                .containsOnlyNulls();

        // Both workers have to query the jobs before any of them locks them
        waitingInternalJobManager.acquireLatch = new CountDownLatch(2);

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        CompletableFuture<List<AcquiredExternalWorkerJob>> testWorker1 = CompletableFuture
                .supplyAsync(() -> managementService.createExternalWorkerJobAcquireBuilder()
                        .topic("simple", Duration.ofMinutes(30))
                        .acquireAndLock(3, "testWorker1", 1), executorService);

        CompletableFuture<List<AcquiredExternalWorkerJob>> testWorker2 = CompletableFuture
                .supplyAsync(() -> managementService.createExternalWorkerJobAcquireBuilder()
                        .topic("simple", Duration.ofMinutes(30))
                        .acquireAndLock(3, "testWorker2", 1), executorService);

        List<AcquiredExternalWorkerJob> worker1Jobs = testWorker1.get();
        List<AcquiredExternalWorkerJob> worker2Jobs = testWorker2.get();

        if (worker1Jobs.isEmpty()) {
            assertThat(worker2Jobs).isNotEmpty();
            assertThat(managementService.createExternalWorkerJobQuery().list())
                    .extracting(ExternalWorkerJob::getLockOwner)
                    .containsOnly("testWorker2", null);
        } else {
            assertThat(worker2Jobs).isEmpty();
            assertThat(managementService.createExternalWorkerJobQuery().list())
                    .extracting(ExternalWorkerJob::getLockOwner)
                    .containsOnly("testWorker1", null);
        }

        executorService.shutdown();

    }

    protected static class WaitingInternalJobManager extends DefaultInternalJobManager {

        protected volatile CountDownLatch acquireLatch;

        public WaitingInternalJobManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
            super(processEngineConfiguration);
        }

        @Override
        public Map<String, Object> resolveVariablesForExternalWorkerJobInternal(ExternalWorkerJob job) {
            // This is invoked after the jobs have been queried and before they (and their process instances) get locked.
            // Every acquire command waits only once (for its first job), a retried acquire does not wait since the latch is already at 0.
            CountDownLatch latch = acquireLatch;
            CommandContext commandContext = Context.getCommandContext();
            if (latch != null && commandContext.getAttribute("waitedForOtherWorker") == null) {
                commandContext.addAttribute("waitedForOtherWorker", Boolean.TRUE);
                latch.countDown();
                try {
                    latch.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return super.resolveVariablesForExternalWorkerJobInternal(job);
        }
    }
}
