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
package org.flowable.eventregistry.impl.management;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.flowable.eventregistry.api.management.EventRegistryChangeDetectionExecutor;
import org.flowable.eventregistry.api.management.EventRegistryChangeDetectionManager;

/**
 * @author Joram Barrez
 */
public class DefaultEventRegistryChangeDetectionExecutor implements EventRegistryChangeDetectionExecutor {

    protected EventRegistryChangeDetectionManager eventRegistryChangeDetectionManager;
    protected long initialDelayInMs;
    protected long delayInMs;

    protected ScheduledExecutorService scheduledExecutorService;
    protected String threadName = "flowable-event-registry-change-detector-%d";
    protected Runnable changeDetectionRunnable;

    public DefaultEventRegistryChangeDetectionExecutor(EventRegistryChangeDetectionManager eventRegistryChangeDetectionManager, long initialDelayInMs, long delayInMs) {
        this.eventRegistryChangeDetectionManager = eventRegistryChangeDetectionManager;
        this.initialDelayInMs = initialDelayInMs;
        this.delayInMs = delayInMs;
    }

    @Override
    public void initialize() {
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new BasicThreadFactory.Builder().namingPattern(threadName).build());
        this.changeDetectionRunnable = createChangeDetectionRunnable();
        this.scheduledExecutorService.scheduleAtFixedRate(this.changeDetectionRunnable, initialDelayInMs, delayInMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
        }
    }

    protected Runnable createChangeDetectionRunnable() {
        return new EventRegistryChangeDetectionRunnable(eventRegistryChangeDetectionManager);
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }
    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }
    public String getThreadName() {
        return threadName;
    }
    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }
    public Runnable getChangeDetectionRunnable() {
        return changeDetectionRunnable;
    }
    public void setChangeDetectionRunnable(Runnable changeDetectionRunnable) {
        this.changeDetectionRunnable = changeDetectionRunnable;
    }
    public EventRegistryChangeDetectionManager getEventRegistryChangeDetectionManager() {
        return eventRegistryChangeDetectionManager;
    }
    @Override
    public void setEventRegistryChangeDetectionManager(EventRegistryChangeDetectionManager eventRegistryChangeDetectionManager) {
        this.eventRegistryChangeDetectionManager = eventRegistryChangeDetectionManager;
    }
}
