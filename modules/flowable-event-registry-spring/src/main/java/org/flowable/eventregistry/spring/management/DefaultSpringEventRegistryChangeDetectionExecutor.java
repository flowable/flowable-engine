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
package org.flowable.eventregistry.spring.management;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.flowable.eventregistry.api.management.EventRegistryChangeDetectionExecutor;
import org.flowable.eventregistry.api.management.EventRegistryChangeDetectionManager;
import org.flowable.eventregistry.impl.management.EventRegistryChangeDetectionRunnable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Joram Barrez
 */
public class DefaultSpringEventRegistryChangeDetectionExecutor implements EventRegistryChangeDetectionExecutor, DisposableBean {

    protected long initialDelayInMs;
    protected long delayInMs;
    protected TaskScheduler taskScheduler;
    protected ThreadPoolTaskScheduler threadPoolTaskScheduler; // If non-null, it means the scheduler has been created in this class

    protected EventRegistryChangeDetectionManager eventRegistryChangeDetectionManager;

    public DefaultSpringEventRegistryChangeDetectionExecutor(long initialDelayInMs, long delayInMs) {
       this(initialDelayInMs, delayInMs, null);
    }

    public DefaultSpringEventRegistryChangeDetectionExecutor(long initialDelayInMs, long delayInMs, TaskScheduler taskScheduler) {
        this.initialDelayInMs = initialDelayInMs;
        this.delayInMs = delayInMs;

        if (taskScheduler != null) {
            this.taskScheduler = taskScheduler;
        } else {
            createDefaultTaskScheduler();
        }
    }

    protected void createDefaultTaskScheduler() {
        threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(1);
        threadPoolTaskScheduler.setThreadNamePrefix("flowable-event-change-detector-");
        taskScheduler = threadPoolTaskScheduler;
    }

    @Override
    public void initialize() {

        if (threadPoolTaskScheduler != null) {
            threadPoolTaskScheduler.initialize();
        }

        Instant initialInstant = Instant.now().plus(initialDelayInMs, ChronoUnit.MILLIS);
        // Note we cannot use the method with the Instant since it was added in Spring 5.0, and we still want to support 4.3
        taskScheduler.scheduleWithFixedDelay(createChangeDetectionRunnable(), Date.from(initialInstant), delayInMs);
    }

    protected Runnable createChangeDetectionRunnable() {
        return new EventRegistryChangeDetectionRunnable(eventRegistryChangeDetectionManager);
    }

    @Override
    public void shutdown() {
        destroy();
    }

    @Override
    public void destroy() {
        if (threadPoolTaskScheduler != null) {
            threadPoolTaskScheduler.destroy();
        }
    }

    public EventRegistryChangeDetectionManager getEventRegistryChangeDetectionManager() {
        return eventRegistryChangeDetectionManager;
    }
    @Override
    public void setEventRegistryChangeDetectionManager(EventRegistryChangeDetectionManager eventRegistryChangeDetectionManager) {
        this.eventRegistryChangeDetectionManager = eventRegistryChangeDetectionManager;
    }
    public TaskScheduler getTaskScheduler() {
        return taskScheduler;
    }
    public void setTaskScheduler(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

}
