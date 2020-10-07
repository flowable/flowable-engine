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
package org.flowable.engine.test.bpmn.servicetask;

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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.async.AsyncTaskInvoker;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.FlowableFutureJavaDelegate;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
public class ServiceTaskWithFuturesNoQueueCapacityTest extends ResourceFlowableTestCase {

    public ServiceTaskWithFuturesNoQueueCapacityTest() {
        super("flowable.cfg.xml");
    }

    @Override
    protected void additionalConfiguration(ProcessEngineConfiguration processEngineConfiguration) {
        super.additionalConfiguration(processEngineConfiguration);

        // The thread pool will start rejecting jobs once we reach 6 parallel jobs (1 in queue, 4 running)
        ((ProcessEngineConfigurationImpl) processEngineConfiguration).setAsyncExecutorThreadPoolQueueSize(1);
        ((ProcessEngineConfigurationImpl) processEngineConfiguration).setAsyncExecutorCorePoolSize(4);
        ((ProcessEngineConfigurationImpl) processEngineConfiguration).setAsyncExecutorMaxPoolSize(4);
    }

    @Test
    @Deployment
    void testDelegateExpression() {

        String currentThreadName = Thread.currentThread().getName();

        // The 5th job will fail due to no more place on the queue
        // If that happens then the async execution should be run on the same thread the process
        CountDownLatch latch = new CountDownLatch(5);
        TestFutureJavaDelegate testBean = new TestFutureJavaDelegate(latch);
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("myProcess")
                .transientVariable("bean", testBean)
                .start();

        assertProcessEnded(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<HistoricVariableInstance> historicVariableInstances = historyService.createHistoricVariableInstanceQuery().list();
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
    @Deployment(resources = "org/flowable/engine/test/bpmn/servicetask/ServiceTaskWithFuturesNoQueueCapacityTest.testDelegateExpression.bpmn20.xml")
    void testDelegateExpressionWithRejectAsyncTaskInvoker() {
        // No need to reset the invoker because the ResourceFlowableTestCase creates a new engine before every test run
        processEngineConfiguration.setAsyncTaskInvoker(new AsyncTaskInvoker() {

            @Override
            public <T> CompletableFuture<T> submit(Callable<T> task) {
                return processEngineConfiguration.getAsyncTaskExecutor().submit(task);
            }
        });

        // The 5th job will fail due to no more place on the queue
        // If that happens then the async execution should be run on the same thread the process
        CountDownLatch latch = new CountDownLatch(5);
        TestFutureJavaDelegate testBean = new TestFutureJavaDelegate(latch);
        assertThatThrownBy(() -> {
            runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("myProcess")
                    .transientVariable("bean", testBean)
                    .start();
        }).isInstanceOf(RejectedExecutionException.class);
    }

    protected static class TestFutureJavaDelegate implements FlowableFutureJavaDelegate<Map<String, Object>, Map<String, Object>> {

        protected final AtomicInteger counter = new AtomicInteger(0);
        protected final CountDownLatch latch;

        public TestFutureJavaDelegate(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public Map<String, Object> prepareExecutionData(DelegateExecution execution) {
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
        public void afterExecution(DelegateExecution execution, Map<String, Object> executionData) {
            if (executionData != null) {
                for (Map.Entry<String, Object> entry : executionData.entrySet()) {
                    execution.setVariable(entry.getKey(), entry.getValue());
                }
            }
        }
    }
}
