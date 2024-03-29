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
package org.flowable.spring.boot.process;

import java.time.Duration;

import org.flowable.spring.boot.FlowableServlet;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * @author Filip Hrisafov
 */
@ConfigurationProperties(prefix = "flowable.process")
public class FlowableProcessProperties {

    /**
     * The servlet configuration for the Process Rest API.
     */
    @NestedConfigurationProperty
    private final FlowableServlet servlet = new FlowableServlet("/process-api", "Flowable BPMN Rest API");
    
    /**
     * Allow to override default history level for specific process definitions using the historyLevel extension element
     */
    protected boolean enableProcessDefinitionHistoryLevel = true;

    /**
     * The maximum amount of process definitions available in the process definition cache.
     * Per default it is -1 (all process definitions).
     */
    protected int definitionCacheLimit = -1;

    /**
     * Enables extra checks on the BPMN xml that is parsed.
     * Unfortunately, this feature is not available on some platforms, hence you need to disable if your platform does not allow the use of
     * StaxSource during XML parsing.
     */
    private boolean enableSafeXml = true;

    /**
     * Whether process instances should be started asynchronously from an event registry.
     * This is a fallback applied for all events. We suggest modelling your processes appropriately, i.e. making the start event async.
     */
    private boolean eventRegistryStartProcessInstanceAsync = false;

    /**
     * Whether the check for unique process instances should be done with a lock.
     * We do not recommend changing this property, unless you have been explicitly asked by a Flowable maintainer.
     */
    private boolean eventRegistryUniqueProcessInstanceCheckWithLock = true;

    /**
     * The amount of time for the lock of a unique start event.
     */
    private Duration eventRegistryUniqueProcessInstanceStartLockTime = Duration.ofMinutes(10);

    public FlowableServlet getServlet() {
        return servlet;
    }

    public boolean isEnableProcessDefinitionHistoryLevel() {
        return enableProcessDefinitionHistoryLevel;
    }

    public void setEnableProcessDefinitionHistoryLevel(boolean enableProcessDefinitionHistoryLevel) {
        this.enableProcessDefinitionHistoryLevel = enableProcessDefinitionHistoryLevel;
    }

    public int getDefinitionCacheLimit() {
        return definitionCacheLimit;
    }

    public void setDefinitionCacheLimit(int definitionCacheLimit) {
        this.definitionCacheLimit = definitionCacheLimit;
    }

    public boolean isEnableSafeXml() {
        return enableSafeXml;
    }

    public void setEnableSafeXml(boolean enableSafeXml) {
        this.enableSafeXml = enableSafeXml;
    }
    
    public boolean isEventRegistryStartProcessInstanceAsync() {
        return eventRegistryStartProcessInstanceAsync;
    }

    public void setEventRegistryStartProcessInstanceAsync(boolean eventRegistryStartProcessInstanceAsync) {
        this.eventRegistryStartProcessInstanceAsync = eventRegistryStartProcessInstanceAsync;
    }

    public boolean isEventRegistryUniqueProcessInstanceCheckWithLock() {
        return eventRegistryUniqueProcessInstanceCheckWithLock;
    }

    public void setEventRegistryUniqueProcessInstanceCheckWithLock(boolean eventRegistryUniqueProcessInstanceCheckWithLock) {
        this.eventRegistryUniqueProcessInstanceCheckWithLock = eventRegistryUniqueProcessInstanceCheckWithLock;
    }

    public Duration getEventRegistryUniqueProcessInstanceStartLockTime() {
        return eventRegistryUniqueProcessInstanceStartLockTime;
    }

    public void setEventRegistryUniqueProcessInstanceStartLockTime(Duration eventRegistryUniqueProcessInstanceStartLockTime) {
        this.eventRegistryUniqueProcessInstanceStartLockTime = eventRegistryUniqueProcessInstanceStartLockTime;
    }

    public static class AsyncHistory {
        
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
    }
    
}
