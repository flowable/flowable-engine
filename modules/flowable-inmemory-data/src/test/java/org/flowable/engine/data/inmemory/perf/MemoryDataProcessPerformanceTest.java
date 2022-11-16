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
package org.flowable.engine.data.inmemory.perf;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.RandomStringUtils;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.data.inmemory.MemoryDataManagerFlowableTestCase;
import org.flowable.engine.data.inmemory.impl.execution.MemoryExecutionDataManager;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryDataProcessPerformanceTest extends MemoryDataManagerFlowableTestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryDataProcessPerformanceTest.class);

    // @Test not part of any automated testing
    public void testExecutionPerformance() throws InterruptedException, ExecutionException {
        assertThat(getConfig().getExecutionDataManager() instanceof MemoryExecutionDataManager).isTrue();
        try {
            runTest(processEngine, "MEMORY");
        } finally {
            processEngine.close();
        }
    }

    protected void runTest(ProcessEngine processEngine, String prefix) throws InterruptedException, ExecutionException {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/triggerableExecution.bpmn20.xml").deploy();

        try {
            long count = 500;
            int numThreads = 10;
            execute(processEngine, prefix, count, numThreads, "triggerableExecution");
            execute(processEngine, prefix, count, numThreads, "triggerableExecution");
            execute(processEngine, prefix, count, numThreads, "triggerableExecution");
            execute(processEngine, prefix, count, numThreads, "triggerableExecution");
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }

    }

    protected static void execute(ProcessEngine engine, String prefix, long numTasks, int numThreads, String key)
                    throws InterruptedException, ExecutionException {

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        List<Future<String>> instanceIds = new ArrayList<>();
        Map<String, Object> variables = new HashMap<>();
        for (int i = 0; i < 200; i++) {
            variables.put("randomStringVariable" + i, RandomStringUtils.random(256, true, true));
        }

        Instant start = Instant.now();
        for (int i = 0; i < numTasks; i++) {
            instanceIds.add(executor.submit(() -> {
                return engine.getRuntimeService().startProcessInstanceByKey(key, variables).getId();
            }));
        }

        List<String> toTrigger = new ArrayList<>();

        List<Future<String>> triggeredInstances = new ArrayList<>();
        Instant waitStart = Instant.now();
        // Wait for all processes to start
        while (!instanceIds.isEmpty() && Duration.between(waitStart, Instant.now()).toMillis() < 60000) {
            Iterator<Future<String>> it = instanceIds.iterator();
            while (it.hasNext()) {
                Future<String> item = it.next();
                try {
                    String id = item.get(10, TimeUnit.SECONDS);
                    toTrigger.add(id);
                    it.remove();
                } catch (TimeoutException e) {
                    // OK
                }
            }
            if (instanceIds.isEmpty()) {
                break;
            }
        }

        Duration startDuration = Duration.between(start, Instant.now());
        LOGGER.info("{} - Started {} in {}ms", prefix, numTasks, startDuration.toMillis());
        start = Instant.now();

        // Trigger all
        for (String id : toTrigger) {
            // trigger the instance
            triggeredInstances.add(executor.submit(() -> {
                Execution execution = engine.getRuntimeService().createExecutionQuery().processInstanceId(id).activityId("service1").singleResult();
                engine.getRuntimeService().trigger(execution.getId());
                return id;
            }));
        }

        // Wait for all to trigger
        waitStart = Instant.now();
        while (!triggeredInstances.isEmpty() && Duration.between(waitStart, Instant.now()).toMillis() < 60000) {
            Iterator<Future<String>> it = triggeredInstances.iterator();
            while (it.hasNext()) {
                Future<String> item = it.next();
                try {
                    String id = item.get(1, TimeUnit.MILLISECONDS);
                    assertThat(engine.getRuntimeService().createProcessInstanceQuery().processInstanceId(id).list()).isEmpty();
                    it.remove();
                } catch (TimeoutException e) {
                    // OK
                }
            }
            if (instanceIds.isEmpty()) {
                break;
            }
        }
        Duration triggerDuration = Duration.between(start, Instant.now());
        long ms = triggerDuration.plus(startDuration).toMillis();
        LOGGER.info("{} - Triggered {} in {}ms", prefix, numTasks, triggerDuration.toMillis());
        LOGGER.info("{} - Executed {} in {}ms ({}ms per task)", prefix, numTasks, ms, BigDecimal.valueOf((double) ms / (double) numTasks).setScale(4));

    }
}
