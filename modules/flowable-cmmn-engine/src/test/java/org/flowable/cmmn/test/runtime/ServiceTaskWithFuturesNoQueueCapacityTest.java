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
package org.flowable.cmmn.test.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.FlowablePlanItemFutureJavaDelegate;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.async.AsyncTaskExecutor;
import org.flowable.common.engine.api.async.AsyncTaskInvoker;
import org.flowable.common.engine.impl.async.DefaultAsyncTaskExecutor;
import org.flowable.common.engine.impl.async.DefaultAsyncTaskInvoker;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Filip Hrisafov
 */
public class ServiceTaskWithFuturesNoQueueCapacityTest extends FlowableCmmnTestCase {

    protected AsyncTaskInvoker originalAsyncTaskInvoker;
    protected AsyncTaskExecutor originalAsyncTaskExecutor;

    @Before
    public void setUp() {
        this.originalAsyncTaskInvoker = this.cmmnEngineConfiguration.getAsyncTaskInvoker();
        this.originalAsyncTaskExecutor = this.cmmnEngineConfiguration.getAsyncTaskExecutor();
    }

    @After
    public void tearDown() {
        if (this.originalAsyncTaskInvoker != null) {
            this.cmmnEngineConfiguration.setAsyncTaskInvoker(this.originalAsyncTaskInvoker);
        }

        AsyncTaskExecutor currentAsyncTaskExecutor = this.cmmnEngineConfiguration.getAsyncTaskExecutor();

        if (this.originalAsyncTaskExecutor != null) {
            this.cmmnEngineConfiguration.setAsyncTaskExecutor(this.originalAsyncTaskExecutor);
        }

        if (this.originalAsyncTaskExecutor != currentAsyncTaskExecutor) {
            // If they are different shut down the current one
            currentAsyncTaskExecutor.shutdown();
        }
    }

    @Test
    @CmmnDeployment
    public void testDelegateExpression() {

        DefaultAsyncTaskExecutor asyncTaskExecutor = new DefaultAsyncTaskExecutor();
        asyncTaskExecutor.setCorePoolSize(4);
        asyncTaskExecutor.setMaxPoolSize(4);
        asyncTaskExecutor.setQueueSize(1);
        asyncTaskExecutor.start();
        cmmnEngineConfiguration.setAsyncTaskExecutor(asyncTaskExecutor);
        cmmnEngineConfiguration.setAsyncTaskInvoker(new DefaultAsyncTaskInvoker(asyncTaskExecutor));

        String currentThreadName = Thread.currentThread().getName();

        // The 5th job will fail due to no more place on the queue
        // If that happens then the async execution should be run on the same thread the process
        CountDownLatch latch = new CountDownLatch(5);
        TestFutureJavaDelegate testBean = new TestFutureJavaDelegate(latch);
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .transientVariable("bean", testBean)
                .start();

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricVariableInstance> historicVariableInstances = cmmnHistoryService.createHistoricVariableInstanceQuery().list();
            Map<String, Object> historicVariables = historicVariableInstances.stream()
                .filter(variable -> !"initiator".equals(variable.getVariableName()))
                .collect(Collectors.toMap(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue));

            assertThat(historicVariables)
                .containsOnlyKeys(
                    "executionThread1", "executionThread2", "executionThread3",
                    "executionThread4", "executionThread5", "executionThread6",
                    "executionThread7", "executionThread8", "executionThread9"
                )
                .containsValues(
                    "flowable-async-job-executor-thread-1",
                    "flowable-async-job-executor-thread-2",
                    "flowable-async-job-executor-thread-3",
                    "flowable-async-job-executor-thread-4",
                    currentThreadName
                );
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/ServiceTaskWithFuturesNoQueueCapacityTest.testDelegateExpression.cmmn")
    public void testDelegateExpressionWithRejectAsyncTaskInvoker() {
        DefaultAsyncTaskExecutor asyncTaskExecutor = new DefaultAsyncTaskExecutor();
        asyncTaskExecutor.setCorePoolSize(4);
        asyncTaskExecutor.setMaxPoolSize(4);
        asyncTaskExecutor.setQueueSize(1);
        asyncTaskExecutor.start();
        cmmnEngineConfiguration.setAsyncTaskExecutor(asyncTaskExecutor);
        cmmnEngineConfiguration.setAsyncTaskInvoker(new AsyncTaskInvoker() {

            @Override
            public <T> CompletableFuture<T> submit(Callable<T> task) {
                return cmmnEngineConfiguration.getAsyncTaskExecutor().submit(task);
            }
        });

        // The 5th job will fail due to no more place on the queue
        // If that happens then the async execution should be run on the same thread the process
        CountDownLatch latch = new CountDownLatch(5);
        TestFutureJavaDelegate testBean = new TestFutureJavaDelegate(latch);
        assertThatThrownBy(() -> {
            cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .transientVariable("bean", testBean)
                    .start();
        }).isInstanceOf(RejectedExecutionException.class);
    }

    protected static class TestFutureJavaDelegate implements FlowablePlanItemFutureJavaDelegate<Map<String, Object>, Map<String, Object>> {

        protected final AtomicInteger counter = new AtomicInteger(0);
        protected final CountDownLatch latch;

        public TestFutureJavaDelegate(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public Map<String, Object> prepareExecutionData(DelegatePlanItemInstance planItemInstance) {
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("counter", counter.incrementAndGet());
            return inputData;
        }

        @Override
        public Map<String, Object> execute(Map<String, Object> inputData) {
            Map<String, Object> outputData = new HashMap<>();
            int counter = (int) inputData.get("counter");
            outputData.put("executionThread" + counter, Thread.currentThread().getName());
            latch.countDown();
            try {
                if (latch.await(2, TimeUnit.SECONDS)) {
                    return outputData;
                } else {
                    throw new FlowableException("latch did not reach 0");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return outputData;
        }

        @Override
        public void afterExecution(DelegatePlanItemInstance planItemInstance, Map<String, Object> executionData) {
            if (executionData != null) {
                for (Map.Entry<String, Object> entry : executionData.entrySet()) {
                    planItemInstance.setVariable(entry.getKey(), entry.getValue());
                }
            }
        }
    }
}
