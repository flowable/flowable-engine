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
package org.flowable.engine.data.inmemory;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.data.inmemory.impl.activity.MemoryActivityInstanceDataManager;
import org.flowable.engine.data.inmemory.impl.eventsubscription.MemoryEventSubscriptionDataManager;
import org.flowable.engine.data.inmemory.impl.execution.MemoryExecutionDataManager;
import org.flowable.engine.data.inmemory.impl.identitylink.MemoryIdentityLinkDataManager;
import org.flowable.engine.data.inmemory.impl.job.MemoryDeadLetterJobDataManager;
import org.flowable.engine.data.inmemory.impl.job.MemoryExternalWorkerJobDataManager;
import org.flowable.engine.data.inmemory.impl.job.MemoryJobDataManager;
import org.flowable.engine.data.inmemory.impl.job.MemorySuspendedJobDataManager;
import org.flowable.engine.data.inmemory.impl.job.MemoryTimerJobDataManager;
import org.flowable.engine.data.inmemory.impl.task.MemoryTaskDataManager;
import org.flowable.engine.data.inmemory.impl.variable.MemoryVariableInstanceDataManager;
import org.flowable.engine.impl.persistence.entity.ActivityInstanceEntity;
import org.flowable.engine.impl.persistence.entity.data.ActivityInstanceDataManager;
import org.flowable.engine.impl.persistence.entity.data.ExecutionDataManager;
import org.flowable.engine.impl.test.AbstractTestCase;
import org.flowable.eventsubscription.service.impl.persistence.entity.data.EventSubscriptionDataManager;
import org.flowable.identitylink.service.impl.persistence.entity.data.IdentityLinkDataManager;
import org.flowable.job.service.impl.persistence.entity.data.DeadLetterJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.ExternalWorkerJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.JobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.SuspendedJobDataManager;
import org.flowable.job.service.impl.persistence.entity.data.TimerJobDataManager;
import org.flowable.task.service.impl.persistence.entity.data.TaskDataManager;
import org.flowable.variable.service.impl.persistence.entity.data.VariableInstanceDataManager;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryDataManagerFlowableTestCase extends AbstractTestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryDataManagerFlowableTestCase.class);

    protected ProcessEngine processEngine;

    @BeforeEach
    public void setup() {
        MemoryDataProcessEngineConfiguration config = new MemoryDataProcessEngineConfiguration();
        config.getAsyncExecutorConfiguration().setDefaultAsyncJobAcquireWaitTime(Duration.ofMillis(100));
        config.getAsyncExecutorConfiguration().setDefaultTimerJobAcquireWaitTime(Duration.ofMillis(100));
        config.setAsyncExecutorActivate(false);
        config.setEngineName(this.getClass().getName());
        this.processEngine = config.buildProcessEngine();
    }

    protected MemoryDataProcessEngineConfiguration getConfig() {
        return (MemoryDataProcessEngineConfiguration) processEngine.getProcessEngineConfiguration();
    }

    @SafeVarargs
    protected final <X extends Object> List<X> list(X... items) {
        List<X> r = new ArrayList<>();
        if (items == null) {
            return r;
        }
        for (X item : items) {
            r.add(item);
        }
        return r;
    }

    protected final Map<String, Object> map(String k, Object v) {
        HashMap<String, Object> m = new HashMap<>();
        m.put(k, v);
        return m;
    }

    protected final Map<String, Object> map(String k, Object v, String k2, Object v2) {
        HashMap<String, Object> m = new HashMap<>();
        m.put(k, v);
        m.put(k2, v2);
        return m;
    }

    /**
     * Asserts that all 'getters' (public methods starting with 'get' or 'is')
     * of the given T query have been called.
     * 
     * @param clazz
     *            The QueryImpl class that query is an instance of
     * @param query
     *            A Mockito.spy() wrapped query implementation to verify
     * @param ignoredMethods
     *            Methods to ignore
     */
    protected <AT extends Object> void assertQueryMethods(Class<AT> clazz, AT query, String... ignoredMethods) {
        // find all getters of the query object and verify that each was
        // called at least once (because the above query is a match it needs
        // to have called each getter of the query object)
        List<String> ignored = list(ignoredMethods);
        List<Method> methods = list(clazz.getDeclaredMethods()).stream().filter(method -> {
            if (method == null || method.getName() == null || method.getName().equals("getClass")) {
                return false;
            }
            if (ignoredMethods != null && ignored.contains(method.getName())) {
                return false;
            }
            if (!Modifier.isPublic(method.getModifiers())) {
                return false;
            }
            if (method.getName().startsWith("get") && method.getReturnType() == boolean.class) {
                // 'is*' should be used instead
                return false;
            }
            return method.getName().startsWith("get") || method.getName().startsWith("is");
        }).collect(Collectors.toList());

        Set<Method> invoked = Mockito.mockingDetails(query).getInvocations().stream().map(invocation -> {
            return invocation.getMethod();
        }).collect(Collectors.toSet());

        // Find all methods that were not invoked
        List<Method> untested = methods.stream().filter(method -> !invoked.contains(method)).collect(Collectors.toList());
        assertThat(untested.isEmpty()).withFailMessage("Untested methods for " + clazz.getSimpleName() + ": " + joinUntested(untested)).isTrue();

    }

    private String joinUntested(List<Method> untested) {
        StringBuilder sb = null;
        for (Method m : untested) {
            if (sb == null) {
                sb = new StringBuilder();
            } else {
                sb.append(',');
            }
            LOGGER.info("Untested method: {}", m.getName());
            sb.append(m.getName());
        }
        return sb == null ? "-" : sb.toString();
    }

    protected ActivityInstanceEntity waitForActivity(String processInstanceId, String activityId) throws InterruptedException {
        Instant start = Instant.now();

        while (Duration.between(start, Instant.now()).toMillis() < 5000) {
            ActivityInstanceEntity entity = getActivityInstanceDataManager().findActivityInstancesByProcessInstanceId(processInstanceId, true).stream()
                            .filter(act -> {
                                return act.getActivityId().equals(activityId);
                            }).findFirst().orElse(null);
            if (entity != null) {
                return entity;
            }
            Thread.sleep(10);
        }
        return null;
    }

    protected MemoryActivityInstanceDataManager getActivityInstanceDataManager() {
        ActivityInstanceDataManager manager = getConfig().getActivityInstanceDataManager();
        assertThat(manager instanceof MemoryActivityInstanceDataManager).isTrue();
        return (MemoryActivityInstanceDataManager) manager;

    }
    protected MemoryExecutionDataManager getExecutionDataManager() {
        ExecutionDataManager manager = getConfig().getExecutionDataManager();
        assertThat(manager instanceof MemoryExecutionDataManager).isTrue();
        return (MemoryExecutionDataManager) manager;
    }

    protected MemoryVariableInstanceDataManager getVariableInstanceDataManager() {
        VariableInstanceDataManager manager = getConfig().getVariableServiceConfiguration().getVariableInstanceDataManager();
        assertThat(manager instanceof VariableInstanceDataManager).isTrue();
        return (MemoryVariableInstanceDataManager) manager;
    }

    protected MemoryTaskDataManager getTaskDataManager() {
        TaskDataManager manager = getConfig().getTaskServiceConfiguration().getTaskDataManager();
        assertThat(manager instanceof MemoryTaskDataManager).isTrue();
        return (MemoryTaskDataManager) manager;
    }

    protected MemoryJobDataManager getJobDataManager() {
        JobDataManager manager = getConfig().getJobServiceConfiguration().getJobDataManager();
        assertThat(manager instanceof MemoryJobDataManager).isTrue();
        return (MemoryJobDataManager) manager;

    }

    protected MemoryTimerJobDataManager getTimerJobDataManager() {
        TimerJobDataManager manager = getConfig().getJobServiceConfiguration().getTimerJobDataManager();
        assertThat(manager instanceof MemoryTimerJobDataManager).isTrue();
        return (MemoryTimerJobDataManager) manager;

    }

    protected MemorySuspendedJobDataManager getSuspendedJobDataManager() {
        SuspendedJobDataManager manager = getConfig().getJobServiceConfiguration().getSuspendedJobDataManager();
        assertThat(manager instanceof MemorySuspendedJobDataManager).isTrue();
        return (MemorySuspendedJobDataManager) manager;

    }
    protected MemoryDeadLetterJobDataManager getDeadLetterJobDataManager() {
        DeadLetterJobDataManager manager = getConfig().getJobServiceConfiguration().getDeadLetterJobDataManager();
        assertThat(manager instanceof MemoryDeadLetterJobDataManager).isTrue();
        return (MemoryDeadLetterJobDataManager) manager;

    }

    protected MemoryExternalWorkerJobDataManager getExternalWorkerJobDataManager() {
        ExternalWorkerJobDataManager manager = getConfig().getJobServiceConfiguration().getExternalWorkerJobDataManager();
        assertThat(manager instanceof MemoryExternalWorkerJobDataManager).isTrue();
        return (MemoryExternalWorkerJobDataManager) manager;
    }

    protected MemoryEventSubscriptionDataManager getEventSubscriptionDataManager() {
        EventSubscriptionDataManager manager = getConfig().getEventSubscriptionServiceConfiguration().getEventSubscriptionDataManager();
        assertThat(manager instanceof MemoryEventSubscriptionDataManager).isTrue();
        return (MemoryEventSubscriptionDataManager) manager;
    }

    protected MemoryIdentityLinkDataManager getIdentityLinkDataManager() {
        IdentityLinkDataManager manager = getConfig().getIdentityLinkServiceConfiguration().getIdentityLinkDataManager();
        assertThat(manager instanceof MemoryIdentityLinkDataManager).isTrue();
        return (MemoryIdentityLinkDataManager) manager;
    }

}
