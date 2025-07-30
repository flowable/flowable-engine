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
package org.flowable.spring.test.jobexecutor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.test.CleanTest;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.nontx.NonTransactionalJobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Similar to NonTransactionalJobHandlerTest, but checking Spring Transaction state extra
 *
 * @author Joram Barrez
 */
@CleanTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration("classpath:org/flowable/spring/test/components/SpringjobExecutorTest-context.xml")
public class SpringNonTransactionalJobHandlerTest extends SpringFlowableTestCase {

    protected NonTransactionalTestJobHandler nonTransactionalTestJobHandler = new NonTransactionalTestJobHandler();
    protected NonTransactionalTestJobHandlerWithException nonTransactionalTestJobHandlerWithException = new NonTransactionalTestJobHandlerWithException();

    @BeforeEach
    public void Setup() {
        processEngineConfiguration.getJobServiceConfiguration().addJobHandler(nonTransactionalTestJobHandler.getType(), nonTransactionalTestJobHandler);
        processEngineConfiguration.getJobServiceConfiguration().addJobHandler(nonTransactionalTestJobHandlerWithException.getType(), nonTransactionalTestJobHandlerWithException);
    }

    @AfterEach
    public void tearDown() {
        processEngineConfiguration.getJobServiceConfiguration().getJobHandlers().remove(nonTransactionalTestJobHandler.getType());
        nonTransactionalTestJobHandler.reset();

        processEngineConfiguration.getJobServiceConfiguration().getJobHandlers().remove(nonTransactionalTestJobHandlerWithException.getType());
        nonTransactionalTestJobHandlerWithException.reset();

        managementService.createJobQuery().handlerType(nonTransactionalTestJobHandler.getType()).list().forEach(j -> managementService.deleteJob(j.getId()));
        managementService.createJobQuery().handlerType(nonTransactionalTestJobHandlerWithException.getType()).list().forEach(j -> managementService.deleteJob(j.getId()));
        managementService.createTimerJobQuery().handlerType(nonTransactionalTestJobHandler.getType()).list().forEach(j -> managementService.deleteTimerJob(j.getId()));
        managementService.createTimerJobQuery().handlerType(nonTransactionalTestJobHandlerWithException.getType()).list().forEach(j -> managementService.deleteTimerJob(j.getId()));
    }

    @Test
    public void testJobExecutedWithoutTransaction() {
        managementService.executeCommand(commandContext -> {
            JobService jobService = CommandContextUtil.getJobService();
            JobEntity job = jobService.createJob();
            job.setJobHandlerType(nonTransactionalTestJobHandler.getType());
            job.setJobHandlerConfiguration("myTest");
            jobService.createAsyncJob(job, false);
            jobService.scheduleAsyncJob(job);
            return null;
        });

        JobTestHelper.waitForJobExecutorOnCondition(processEngineConfiguration, 10000L, 20L,
                () -> managementService.createJobQuery().count() == 0);

        assertThat(nonTransactionalTestJobHandler.getCounter().get()).isEqualTo(1);
        assertThat(nonTransactionalTestJobHandler.getWithCommandContext().get()).isEqualTo(0);
        assertThat(nonTransactionalTestJobHandler.getWithoutTransactionCounter().get()).isEqualTo(1);
        assertThat(nonTransactionalTestJobHandler.getWithTransactionCounter().get()).isEqualTo(0);
        assertThat(nonTransactionalTestJobHandler.getWithoutCommandContext().get()).isEqualTo(1);
        assertThat(nonTransactionalTestJobHandler.nonTransactionalOutput).isEqualTo("myTest");
        assertThat(nonTransactionalTestJobHandler.nonTransactionalCounter).hasValue(1);

        assertThat(managementService.createJobQuery().count()).isEqualTo(0);
    }

    @Test
    public void testJobExecutedWithoutTransactionThrowsException() {
        managementService.executeCommand(commandContext -> {
            JobService jobService = CommandContextUtil.getJobService();
            JobEntity job = jobService.createJob();
            job.setJobHandlerType(nonTransactionalTestJobHandlerWithException.getType());
            job.setJobHandlerConfiguration("myTest");
            jobService.createAsyncJob(job, false);
            jobService.scheduleAsyncJob(job);
            return null;
        });

        Job job = managementService.createJobQuery().singleResult();
        int initialRetries = job.getRetries();

        // Job will fail and then become a timer job
        JobTestHelper.waitForJobExecutorOnCondition(processEngineConfiguration, 10000L, 20L,
                () -> managementService.createTimerJobQuery().count() == 1);

        assertThat(nonTransactionalTestJobHandlerWithException.getCounter().get()).isEqualTo(1);
        assertThat(nonTransactionalTestJobHandlerWithException.getWithCommandContext().get()).isEqualTo(0);
        assertThat(nonTransactionalTestJobHandlerWithException.getWithoutTransactionCounter().get()).isEqualTo(1);
        assertThat(nonTransactionalTestJobHandlerWithException.getWithTransactionCounter().get()).isEqualTo(0);
        assertThat(nonTransactionalTestJobHandlerWithException.getWithoutCommandContext().get()).isEqualTo(1);
        assertThat(nonTransactionalTestJobHandlerWithException.nonTransactionalOutput).isNull();
        assertThat(nonTransactionalTestJobHandlerWithException.nonTransactionalCounter).hasValue(0);

        assertThat(managementService.createJobQuery().count()).isEqualTo(0);

        job = managementService.createTimerJobQuery().singleResult();
        assertThat(job.getRetries()).isEqualTo(initialRetries - 1);
    }

    public static class NonTransactionalTestJobHandler implements NonTransactionalJobHandler<String> {

        protected AtomicInteger counter = new AtomicInteger();
        protected AtomicInteger withCommandContext = new AtomicInteger(0);
        protected AtomicInteger withoutCommandContext = new AtomicInteger(0);
        protected AtomicInteger withTransactionCounter = new AtomicInteger(0);
        protected AtomicInteger withoutTransactionCounter = new AtomicInteger(0);
        protected AtomicInteger nonTransactionalCounter = new AtomicInteger(0);

        protected String jobConfiguration;
        protected String nonTransactionalOutput;

        @Override
        public String getType() {
            return "non-transactional";
        }

        @Override
        public String executeNonTransactionally(JobEntity job, String configuration) {

            // Not checking the passed command context, but checking the low-level one on Context
            counter.incrementAndGet();

            CommandContext currentCommandContext = Context.getCommandContext();
            if (currentCommandContext != null) {
                withCommandContext.incrementAndGet();
            } else {
                withoutCommandContext.incrementAndGet();
            }

            // Spring-specific code vs test in normal code
            boolean actualTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
            if (actualTransactionActive) {
                withTransactionCounter.incrementAndGet();
            } else {
                withoutTransactionCounter.incrementAndGet();
            }

            this.jobConfiguration = job.getJobHandlerConfiguration();
            return this.jobConfiguration;

        }

        @Override
        public void afterExecute(JobEntity job, String configuration, String output, CommandContext commandContext) {
            nonTransactionalCounter.incrementAndGet();
            this.nonTransactionalOutput = output;
        }

        public AtomicInteger getCounter() {
            return counter;
        }

        public AtomicInteger getWithCommandContext() {
            return withCommandContext;
        }

        public AtomicInteger getWithoutCommandContext() {
            return withoutCommandContext;
        }

        public AtomicInteger getWithTransactionCounter() {
            return withTransactionCounter;
        }

        public AtomicInteger getWithoutTransactionCounter() {
            return withoutTransactionCounter;
        }

        public String getJobConfiguration() {
            return jobConfiguration;
        }

        public void reset() {
            this.counter.set(0);
            this.withCommandContext.set(0);
            this.withoutCommandContext.set(0);
            this.withTransactionCounter.set(0);
            this.withoutTransactionCounter.set(0);
        }

    }

    public static class NonTransactionalTestJobHandlerWithException extends NonTransactionalTestJobHandler {

        @Override
        public String getType() {
            return "non-transactional-with-exception";
        }

        @Override
        public String executeNonTransactionally(JobEntity job, String configuration) {
            super.executeNonTransactionally(job, configuration);

            throw new RuntimeException();
        }
    }

}
