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
package org.flowable.common.engine.impl;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.impl.event.EventDispatchAction;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.runtime.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tijs Rademakers
 */
public abstract class AbstractServiceConfiguration {
    
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractServiceConfiguration.class);
    
    /** The tenant id indicating 'no tenant' */
    public static final String NO_TENANT_ID = "";

    protected boolean enableEventDispatcher = true;
    protected FlowableEventDispatcher eventDispatcher;
    protected List<FlowableEventListener> eventListeners;
    protected Map<String, List<FlowableEventListener>> typedEventListeners;
    protected List<EventDispatchAction> additionalEventDispatchActions;
    
    protected HistoryLevel historyLevel;
    
    protected ObjectMapper objectMapper;

    protected Clock clock;
    
    public boolean isHistoryLevelAtLeast(HistoryLevel level) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Current history level: {}, level required: {}", historyLevel, level);
        }
        // Comparing enums actually compares the location of values declared in the enum
        return historyLevel.isAtLeast(level);
    }

    public boolean isHistoryEnabled() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Current history level: {}", historyLevel);
        }
        return historyLevel != HistoryLevel.NONE;
    }

    public boolean isEnableEventDispatcher() {
        return enableEventDispatcher;
    }

    public AbstractServiceConfiguration setEnableEventDispatcher(boolean enableEventDispatcher) {
        this.enableEventDispatcher = enableEventDispatcher;
        return this;
    }

    public FlowableEventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    public AbstractServiceConfiguration setEventDispatcher(FlowableEventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
        return this;
    }

    public List<FlowableEventListener> getEventListeners() {
        return eventListeners;
    }

    public AbstractServiceConfiguration setEventListeners(List<FlowableEventListener> eventListeners) {
        this.eventListeners = eventListeners;
        return this;
    }

    public Map<String, List<FlowableEventListener>> getTypedEventListeners() {
        return typedEventListeners;
    }

    public AbstractServiceConfiguration setTypedEventListeners(Map<String, List<FlowableEventListener>> typedEventListeners) {
        this.typedEventListeners = typedEventListeners;
        return this;
    }
    
    public List<EventDispatchAction> getAdditionalEventDispatchActions() {
        return additionalEventDispatchActions;
    }

    public AbstractServiceConfiguration setAdditionalEventDispatchActions(List<EventDispatchAction> additionalEventDispatchActions) {
        this.additionalEventDispatchActions = additionalEventDispatchActions;
        return this;
    }
    
    public HistoryLevel getHistoryLevel() {
        return historyLevel;
    }
    
    public AbstractServiceConfiguration setHistoryLevel(HistoryLevel historyLevel) {
        this.historyLevel = historyLevel;
        return this;
    }
    
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public AbstractServiceConfiguration setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    public Clock getClock() {
        return clock;
    }

    public AbstractServiceConfiguration setClock(Clock clock) {
        this.clock = clock;
        return this;
    }
}
