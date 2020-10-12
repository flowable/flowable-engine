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
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.FlowablePlanItemFutureJavaDelegate;
import org.flowable.cmmn.api.delegate.MapBasedFlowablePlanItemFutureJavaDelegate;
import org.flowable.cmmn.api.delegate.PlanItemJavaDelegate;
import org.flowable.cmmn.api.delegate.ReadOnlyDelegatePlanItemInstance;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.async.AsyncTaskExecutor;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.javax.el.ELException;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Test;

/**
 * @author Filip Hrisafov
 */
public class ServiceTaskWithFuturesTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testExpressionReturnsFuture() {

        CountDownLatch latch = new CountDownLatch(2);
        TestBeanReturnsFuture testBean = new TestBeanReturnsFuture(latch, cmmnEngineConfiguration.getAsyncTaskExecutor());
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .transientVariable("bean", testBean)
                .start();

        assertCaseInstanceEnded(caseInstance);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId())
                .singleResult();

            // Every service task sleeps for 1s, but when we use futures it should take less then 2s.
            long durationInMillis = historicCaseInstance.getEndTime().getTime() - historicCaseInstance.getEndTime().getTime();
            assertThat(durationInMillis).isLessThan(1500);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/ServiceTaskWithFuturesTest.testParallelAndSequentialExpression.cmmn")
    public void testParallelAndSequentialExpressionDoesNotReturnFuture() {
        // when the expression doesn't return a future then the execution order should be the following (same as the planned order):
        // serviceTask1 -> serviceTask2 -> serviceTask1_2 -> serviceTask2_2
        Object bean = new Object() {

            public Integer invoke(AtomicInteger counter) {
                return counter.incrementAndGet();
            }
        };
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .transientVariable("counter", new AtomicInteger(0))
                .transientVariable("bean1", bean)
                .transientVariable("bean2", bean)
                .start();

        assertCaseInstanceEnded(caseInstance);

        assertThat(caseInstance.getCaseVariables())
                .contains(
                        entry("service1_1Var", 1),
                        entry("service1_2Var", 3),
                        entry("service2_1Var", 2),
                        entry("service2_2Var", 4)
                );
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/ServiceTaskWithFuturesTest.testParallelAndSequentialExpression.cmmn")
    public void testParallelAndSequentialExpressionReturnsCompletedFuture() {
        // when the expression returns a completed future then the execution order should be the same as when it doesn't return a future.
        // The order should be the following (same as the planned order):
        // serviceTask1 -> serviceTask2 -> serviceTask1_2 -> serviceTask2_2
        Object bean = new Object() {

            public CompletableFuture<Integer> invoke(AtomicInteger counter) {
                return CompletableFuture.completedFuture(counter.incrementAndGet());
            }
        };
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .transientVariable("counter", new AtomicInteger(0))
                .transientVariable("bean1", bean)
                .transientVariable("bean2", bean)
                .start();

        assertCaseInstanceEnded(caseInstance);

        assertThat(caseInstance.getCaseVariables())
                .contains(
                        entry("service1_1Var", 1),
                        entry("service1_2Var", 3),
                        entry("service2_1Var", 2),
                        entry("service2_2Var", 4)
                );
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/ServiceTaskWithFuturesTest.testParallelAndSequentialExpression.cmmn")
    public void testParallelAndSequentialExpressionReturnsNotCompletedFuture() {
        // This test has the most complex setup.
        // The idea is to show that when a future is completed its action will immediately be executed.
        // With this setup bean1 cannot continue until bean2 has started executing.
        CountDownLatch bean1Enter = new CountDownLatch(1);
        CountDownLatch bean2Enter = new CountDownLatch(1);
        CountDownLatch commonLatch = new CountDownLatch(2);

        Object bean1 = new Object() {

            public CompletableFuture<Integer> invoke(AtomicInteger counter) {
                return cmmnEngineConfiguration.getAsyncTaskExecutor().submit(() -> {
                    commonLatch.countDown();
                    bean1Enter.countDown();
                    // Now wait for bean2 to be entered
                    try {
                        if (!bean2Enter.await(2, TimeUnit.SECONDS)) {
                            throw new FlowableException("Bean2 did not hit its latch");
                        }

                        if (commonLatch.await(2, TimeUnit.SECONDS)) {
                            Thread.sleep(100); // Thread sleep to slower the execution a bit
                            return counter.incrementAndGet();
                        } else {
                            throw new FlowableException("Countdown latch did not reach 0");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return counter.incrementAndGet();
                });

            }
        };

        Object bean2 = new Object() {

            public CompletableFuture<Integer> invoke(AtomicInteger counter) {
                return cmmnEngineConfiguration.getAsyncTaskExecutor().submit(() -> {
                    try {
                        if (!bean1Enter.await(2, TimeUnit.SECONDS)) {
                            throw new FlowableException("Bean1 did not hit its latch");
                        }
                        bean2Enter.countDown();
                        return counter.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        commonLatch.countDown();
                    }
                    return counter.incrementAndGet();
                });

            }
        };

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .transientVariable("counter", new AtomicInteger(0))
                .transientVariable("bean1", bean1)
                .transientVariable("bean2", bean2)
                .start();

        assertCaseInstanceEnded(caseInstance);

        assertThat(caseInstance.getCaseVariables())
                .contains(
                        entry("service1_1Var", 3),
                        entry("service1_2Var", 4),
                        entry("service2_1Var", 1),
                        entry("service2_2Var", 2)
                );
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/ServiceTaskWithFuturesTest.testExpressionReturnsFuture.cmmn")
    public void testExpressionDoesNotReturnFuture() {

        assertThatThrownBy(() -> {
            CountDownLatch latch = new CountDownLatch(2);
            TestBean testBean = new TestBean(latch);
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .transientVariable("bean", testBean)
                    .start();
        })
                .isExactlyInstanceOf(FlowableException.class)
                .getCause()
                .isInstanceOf(ELException.class)
                .getCause()
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("Countdown latch did not reach 0");
    }

    @Test
    @CmmnDeployment
    public void testDelegateExpressionWithFutureJavaDelegate() {

        String currentThreadName = Thread.currentThread().getName();

        // When using a normal JavaDelegate this process cannot complete
        // because the latch needs to be decreased twice before every task returns a result
        // Try changing TestFutureJavaDelegate to implement JavaDelegate instead
        CountDownLatch latch = new CountDownLatch(2);
        TestFutureJavaDelegate testBean = new TestFutureJavaDelegate(latch);
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .transientVariable("bean", testBean)
                .transientVariable("counter", new AtomicInteger(0))
                .start();

        assertCaseInstanceEnded(caseInstance);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricVariableInstance> historicVariableInstances = cmmnHistoryService.createHistoricVariableInstanceQuery().list();
            Map<String, Object> historicVariables = historicVariableInstances.stream()
                .filter(variable -> !"initiator".equals(variable.getVariableName()))
                .collect(Collectors.toMap(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue));

            assertThat(historicVariables)
                .containsOnlyKeys(
                    "counter1", "beforeExecutionThreadName1", "executionThreadName1", "afterExecutionThreadName1",
                    "counter2", "beforeExecutionThreadName2", "executionThreadName2", "afterExecutionThreadName2"
                )
                .contains(
                    entry("counter1", 1),
                    entry("beforeExecutionThreadName1", currentThreadName),
                    entry("afterExecutionThreadName1", currentThreadName),
                    entry("counter2", 2),
                    entry("beforeExecutionThreadName2", currentThreadName),
                    entry("afterExecutionThreadName2", currentThreadName)
                );

            assertThat(historicVariables.get("executionThreadName1"))
                .asInstanceOf(STRING)
                .isNotEqualTo(currentThreadName)
                .startsWith("flowable-async-job-executor-thread-");

            assertThat(historicVariables.get("executionThreadName2"))
                .asInstanceOf(STRING)
                .isNotEqualTo(currentThreadName)
                // The executions should be done on different threads
                .isNotEqualTo(historicVariables.get("executionThreadName1"))
                .startsWith("flowable-async-job-executor-thread-");
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/ServiceTaskWithFuturesTest.testDelegateExpressionWithFutureJavaDelegate.cmmn", tenantId = "flowable")
    public void testDelegateExpressionWithMapBasedFutureJavaDelegate() {
        List<ReadOnlyDelegatePlanItemInstance> delegatePlanItems = new ArrayList<>();
        MapBasedFlowablePlanItemFutureJavaDelegate bean = inputData -> {
            delegatePlanItems.add(inputData);
            return Collections.singletonMap(inputData.getPlanItemDefinitionId(), "done");
        };
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .tenantId("flowable")
                .businessKey("test-key")
                .name("Case name")
                .variable("testVar", "test")
                .transientVariable("bean", bean)
                .transientVariable("counter", new AtomicInteger(0))
                .start();

        assertThat(caseInstance.getCaseVariables())
                .contains(
                        entry("serviceTask1", "done"),
                        entry("serviceTask2", "done")
                );

        assertCaseInstanceEnded(caseInstance);
        assertThat(delegatePlanItems)
                .extracting(ReadOnlyDelegatePlanItemInstance::getPlanItemDefinitionId)
                .containsExactly("serviceTask1", "serviceTask2");

        ReadOnlyDelegatePlanItemInstance task1Delegate = delegatePlanItems.get(0);
        assertThat(task1Delegate.getId()).isNotNull();
        assertThat(task1Delegate.getCaseInstanceId()).isEqualTo(caseInstance.getId());
        assertThat(task1Delegate.getCaseDefinitionId()).isNotNull();
        assertThat(task1Delegate.getCreateTime()).isNotNull();
        assertThat(task1Delegate.getLastAvailableTime()).isNotNull();
        assertThat(task1Delegate.getLastStartedTime()).isNotNull();
        assertThat(task1Delegate.getTenantId()).isEqualTo("flowable");
        assertThat(task1Delegate.isStage()).isFalse();
        assertThat(task1Delegate.hasVariable("serviceTask1")).isFalse();
        assertThat(task1Delegate.hasVariable("serviceTask2")).isFalse();
        assertThat(task1Delegate.getVariable("testVar")).isEqualTo("test");
        assertThat(task1Delegate.getPlanItemDefinition()).isInstanceOf(ServiceTask.class);
        ServiceTask serviceTask1 = (ServiceTask) task1Delegate.getPlanItemDefinition();
        assertThat(serviceTask1.getId()).isEqualTo("serviceTask1");

        ReadOnlyDelegatePlanItemInstance task2Delegate = delegatePlanItems.get(1);
        assertThat(task2Delegate.getId()).isNotNull().isNotEqualTo(task1Delegate.getId());
        assertThat(task2Delegate.getCaseInstanceId()).isEqualTo(caseInstance.getId());
        assertThat(task2Delegate.getCaseDefinitionId()).isNotNull();
        assertThat(task2Delegate.getCreateTime()).isNotNull();
        assertThat(task2Delegate.getLastAvailableTime()).isNotNull();
        assertThat(task2Delegate.getLastStartedTime()).isNotNull();
        assertThat(task2Delegate.getTenantId()).isEqualTo("flowable");
        assertThat(task2Delegate.isStage()).isFalse();
        assertThat(task2Delegate.hasVariable("serviceTask1")).isFalse();
        assertThat(task2Delegate.hasVariable("serviceTask2")).isFalse();
        assertThat(task2Delegate.getVariable("testVar")).isEqualTo("test");
        assertThat(task2Delegate.getPlanItemDefinition()).isInstanceOf(ServiceTask.class);
        ServiceTask serviceTask2 = (ServiceTask) task2Delegate.getPlanItemDefinition();
        assertThat(serviceTask2.getId()).isEqualTo("serviceTask2");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/ServiceTaskWithFuturesTest.testDelegateExpressionWithFutureJavaDelegate.cmmn")
    public void testDelegateExpressionWithJavaDelegate() {

        // This is using a normal JavaDelegate and thus the process cannot complete
        // because the latch needs to be decreased twice before every task returns a result
        CountDownLatch latch = new CountDownLatch(2);
        TestJavaDelegate testBean = new TestJavaDelegate(latch);
        assertThatThrownBy(() -> {
            cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .transientVariable("bean", testBean)
                    .start();
        })
                .isExactlyInstanceOf(FlowableException.class)
                .hasNoCause()
                .hasMessage("Countdown latch did not reach 0");

    }

    @Test
    @CmmnDeployment
    public void testDelegateExpressionParallelAndSequentialFutureJavaDelegates() {
        // the setup of the test is the following:
        // there are 3 delegate executions:
        // delegate1_1 -> delegate1_2
        // delegate2_1
        // for delegate 1_1 to complete delegate2_1 should start executing
        // for delegate 1_2 to complete delegate2_1 should start executing and 1_1 should be done
        // for delegate2_1 to complete delegate1_2 should complete

        CountDownLatch delegate1_1Done = new CountDownLatch(1);
        CountDownLatch delegate1_2Done = new CountDownLatch(1);
        CountDownLatch delegate2_1Done = new CountDownLatch(1);
        CountDownLatch delegate2_1Start = new CountDownLatch(1);

        MapBasedFlowablePlanItemFutureJavaDelegate futureDelegate1_1 = new MapBasedFlowablePlanItemFutureJavaDelegate() {

            @Override
            public Map<String, Object> execute(ReadOnlyDelegatePlanItemInstance inputData) {

                try {

                    if (delegate2_1Start.await(2, TimeUnit.SECONDS)) {
                        AtomicInteger counter = (AtomicInteger) inputData.getVariable("counter");
                        return Collections.singletonMap("counterDelegate1_1", counter.incrementAndGet());
                    }

                    throw new FlowableException("Delegate 2_1 did not start");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new FlowableException("Thread was interrupted");
                }
            }

            @Override
            public void afterExecution(DelegatePlanItemInstance planItemInstance, Map<String, Object> executionData) {
                MapBasedFlowablePlanItemFutureJavaDelegate.super.afterExecution(planItemInstance, executionData);
                delegate1_1Done.countDown();
            }
        };

        MapBasedFlowablePlanItemFutureJavaDelegate futureDelegate1_2 = new MapBasedFlowablePlanItemFutureJavaDelegate() {

            @Override
            public Map<String, Object> execute(ReadOnlyDelegatePlanItemInstance inputData) {
                assertThat(inputData.getVariable("counterDelegate1_1")).isEqualTo(1);
                assertThat(inputData.hasVariable("counterDelegate1_2")).isFalse();
                assertThat(inputData.hasVariable("counterDelegate2_1")).isFalse();
                AtomicInteger counter = (AtomicInteger) inputData.getVariable("counter");
                return Collections.singletonMap("counterDelegate1_2", counter.incrementAndGet());
            }

            @Override
            public void afterExecution(DelegatePlanItemInstance planItemInstance, Map<String, Object> executionData) {
                MapBasedFlowablePlanItemFutureJavaDelegate.super.afterExecution(planItemInstance, executionData);
                delegate1_2Done.countDown();
            }
        };

        MapBasedFlowablePlanItemFutureJavaDelegate futureDelegate2_1 = new MapBasedFlowablePlanItemFutureJavaDelegate() {

            @Override
            public Map<String, Object> execute(ReadOnlyDelegatePlanItemInstance inputData) {
                delegate2_1Start.countDown();

                try {
                    if (delegate1_2Done.await(2, TimeUnit.SECONDS)) {
                        AtomicInteger counter = (AtomicInteger) inputData.getVariable("counter");
                        return Collections.singletonMap("counterDelegate2_1", counter.incrementAndGet());
                    }

                    throw new FlowableException("Delegate 1_2 did not complete");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new FlowableException("Thread was interrupted");
                }
            }

            @Override
            public void afterExecution(DelegatePlanItemInstance planItemInstance, Map<String, Object> executionData) {
                assertThat(planItemInstance.getVariables())
                        .contains(
                                entry("counterDelegate1_1", 1),
                                entry("counterDelegate1_2", 2)
                        )
                        .doesNotContainKeys("counterDelegate2_1");
                MapBasedFlowablePlanItemFutureJavaDelegate.super.afterExecution(planItemInstance, executionData);
                delegate2_1Done.countDown();
            }
        };

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .transientVariable("futureDelegate1_1", futureDelegate1_1)
                .transientVariable("futureDelegate1_2", futureDelegate1_2)
                .transientVariable("futureDelegate2_1", futureDelegate2_1)
                .transientVariable("counter", new AtomicInteger(0))
                .start();

        assertCaseInstanceEnded(caseInstance);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricVariableInstance> historicVariableInstances = cmmnHistoryService.createHistoricVariableInstanceQuery().list();
            Map<String, Object> historicVariables = historicVariableInstances.stream()
                .filter(variable -> !"initiator".equals(variable.getVariableName()))
                .collect(Collectors.toMap(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue));

            assertThat(historicVariables)
                .containsOnly(
                    entry("counterDelegate1_1", 1),
                    entry("counterDelegate1_2", 2),
                    entry("counterDelegate2_1", 3)
                );
        }
    }

    @Test
    @CmmnDeployment
    public void testClassWithFutureJavaDelegate() {

        String currentThreadName = Thread.currentThread().getName();

        // When using a normal JavaDelegate this process cannot complete
        // because the latch needs to be decreased twice before every task returns a result
        // Try changing TestFutureJavaDelegate to implement JavaDelegate instead
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .transientVariable("counter", new AtomicInteger(0))
                .transientVariable("countDownLatch", new CountDownLatch(2))
                .start();

        assertCaseInstanceEnded(caseInstance);

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricVariableInstance> historicVariableInstances = cmmnHistoryService.createHistoricVariableInstanceQuery().list();
            Map<String, Object> historicVariables = historicVariableInstances.stream()
                .filter(variable -> !"initiator".equals(variable.getVariableName()))
                .collect(Collectors.toMap(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue));

            assertThat(historicVariables)
                .containsOnlyKeys(
                    "counter1", "beforeExecutionThreadName1", "executionThreadName1", "afterExecutionThreadName1",
                    "counter2", "beforeExecutionThreadName2", "executionThreadName2", "afterExecutionThreadName2"
                )
                .contains(
                    entry("counter1", 1),
                    entry("beforeExecutionThreadName1", currentThreadName),
                    entry("afterExecutionThreadName1", currentThreadName),
                    entry("counter2", 2),
                    entry("beforeExecutionThreadName2", currentThreadName),
                    entry("afterExecutionThreadName2", currentThreadName)
                );

            assertThat(historicVariables.get("executionThreadName1"))
                .asInstanceOf(STRING)
                .isNotEqualTo(currentThreadName)
                .startsWith("flowable-async-job-executor-thread-");

            assertThat(historicVariables.get("executionThreadName2"))
                .asInstanceOf(STRING)
                .isNotEqualTo(currentThreadName)
                // The executions should be done on different threads
                .isNotEqualTo(historicVariables.get("executionThreadName1"))
                .startsWith("flowable-async-job-executor-thread-");
        }
    }

    @Test
    @CmmnDeployment
    public void testClassWithJavaDelegate() {

        // This is using a normal JavaDelegate and thus the process cannot complete
        // because the latch needs to be decreased twice before every task returns a result
        assertThatThrownBy(() -> {
            cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .transientVariable("countDownLatch", new CountDownLatch(2))
                    .start();
        })
                .isExactlyInstanceOf(FlowableException.class)
                .hasNoCause()
                .hasMessage("Countdown latch did not reach 0");

    }

    protected static class TestFutureJavaDelegate implements FlowablePlanItemFutureJavaDelegate<Map<String, Object>, Map<String, Object>> {

        protected final CountDownLatch countDownLatch;

        @SuppressWarnings("unused") // used by the class delegate
        public TestFutureJavaDelegate() {
            this(null);
        }

        public TestFutureJavaDelegate(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public Map<String, Object> prepareExecutionData(DelegatePlanItemInstance planItemInstance) {
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("beforeExecutionThreadName", Thread.currentThread().getName());
            AtomicInteger counter = (AtomicInteger) planItemInstance.getTransientVariable("counter");
            inputData.put("counter", counter.incrementAndGet());
            inputData.put("countDownLatch", planItemInstance.getTransientVariable("countDownLatch"));
            return inputData;
        }

        @Override
        public Map<String, Object> execute(Map<String, Object> inputData) {
            CountDownLatch countDownLatch = this.countDownLatch;
            if (countDownLatch == null) {
                countDownLatch = (CountDownLatch) inputData.get("countDownLatch");
            }
            countDownLatch.countDown();
            try {
                if (countDownLatch.await(2, TimeUnit.SECONDS)) {
                    int counter = (int) inputData.get("counter");
                    Map<String, Object> outputData = new HashMap<>();
                    outputData.put("executionThreadName" + counter, Thread.currentThread().getName());
                    outputData.put("counter" + counter, counter);
                    outputData.put("beforeExecutionThreadName" + counter, inputData.get("beforeExecutionThreadName"));
                    return outputData;
                } else {
                    throw new FlowableException("Countdown latch did not reach 0");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return null;
        }

        @Override
        public void afterExecution(DelegatePlanItemInstance planItemInstance, Map<String, Object> executionData) {
            if (executionData != null) {
                for (Map.Entry<String, Object> entry : executionData.entrySet()) {
                    String key = entry.getKey();
                    planItemInstance.setVariable(key, entry.getValue());

                    if (key.startsWith("counter")) {
                        planItemInstance.setVariable("afterExecutionThreadName" + key.substring(7), Thread.currentThread().getName());
                    }
                }
            }
        }
    }

    protected static class TestJavaDelegate implements PlanItemJavaDelegate {

        protected final CountDownLatch countDownLatch;

        @SuppressWarnings("unused") // used from the class delegate
        public TestJavaDelegate() {
            this(null);
        }

        public TestJavaDelegate(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void execute(DelegatePlanItemInstance planItemInstance) {
            CountDownLatch countDownLatch = this.countDownLatch;
            if (countDownLatch == null) {
                countDownLatch = (CountDownLatch) planItemInstance.getTransientVariable("countDownLatch");
            }
            countDownLatch.countDown();
            try {
                if (countDownLatch.await(2, TimeUnit.SECONDS)) {
                    // Nothing to do
                } else {
                    throw new FlowableException("Countdown latch did not reach 0");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected static class TestBeanReturnsFuture {

        protected final AtomicInteger counter = new AtomicInteger(0);
        protected final CountDownLatch countDownLatch;
        protected final AsyncTaskExecutor asyncTaskExecutor;

        public TestBeanReturnsFuture(CountDownLatch countDownLatch, AsyncTaskExecutor asyncTaskExecutor) {
            this.countDownLatch = countDownLatch;
            this.asyncTaskExecutor = asyncTaskExecutor;
        }

        public Future<Integer> invoke() {
            return asyncTaskExecutor.submit(() -> {
                countDownLatch.countDown();
                try {
                    if (countDownLatch.await(2, TimeUnit.SECONDS)) {
                        return counter.incrementAndGet();
                    } else {
                        throw new FlowableException("Countdown latch did not reach 0");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return counter.incrementAndGet();
            });
        }
    }

    protected static class TestBean {

        protected final CountDownLatch countDownLatch;

        public TestBean(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        public String invoke() {
            countDownLatch.countDown();
            try {
                if (countDownLatch.await(3, TimeUnit.SECONDS)) {
                    return null;
                } else {
                    throw new FlowableException("Countdown latch did not reach 0");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }
}
