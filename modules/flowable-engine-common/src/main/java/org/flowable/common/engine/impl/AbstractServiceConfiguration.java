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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.event.EventDispatchAction;
import org.flowable.common.engine.impl.runtime.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tijs Rademakers
 */
public abstract class AbstractServiceConfiguration<S> {

    protected final Logger logger = LoggerFactory.getLogger(AbstractServiceConfiguration.class);

    /** The tenant id indicating 'no tenant' */
    public static final String NO_TENANT_ID = "";

    protected String engineName;

    protected Collection<ServiceConfigurator<S>> configurators;

    protected boolean enableEventDispatcher = true;
    protected FlowableEventDispatcher eventDispatcher;
    protected List<FlowableEventListener> eventListeners;
    protected Map<String, List<FlowableEventListener>> typedEventListeners;
    protected List<EventDispatchAction> additionalEventDispatchActions;

    protected ObjectMapper objectMapper;

    protected Clock clock;
    protected IdGenerator idGenerator;

    public AbstractServiceConfiguration(String engineName) {
        this.engineName = engineName;
    }

    protected abstract S getService();

    public String getEngineName() {
        return engineName;
    }

    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    public Collection<ServiceConfigurator<S>> getConfigurators() {
        return configurators;
    }

    public void setConfigurators(Collection<ServiceConfigurator<S>> configurators) {
        initConfigurators();
        this.configurators.clear();
        if (configurators != null) {
            this.configurators.addAll(configurators);
        }
    }

    public AbstractServiceConfiguration<S> addConfigurator(ServiceConfigurator<S> configurator) {
        initConfigurators();
        this.configurators.add(configurator);
        return this;
    }

    protected void initConfigurators() {
        if (this.configurators == null) {
            this.configurators = new TreeSet<>(Comparator.comparingInt(ServiceConfigurator::getPriority));
        }
    }

    protected void configuratorsBeforeInit() {
        if (this.configurators == null || this.configurators.isEmpty()) {
            return;
        }
        final S service = getService();
        this.configurators.stream().forEach(c -> {
            logger.info("Executing beforeInit() of {} (priority: {})", c.getClass(), c.getPriority());
            c.beforeInit(service);
        });
    }

    protected void configuratorsAfterInit() {
        if (this.configurators == null || this.configurators.isEmpty()) {
            return;
        }
        final S service = getService();
        this.configurators.stream().forEach(c -> {
            logger.info("Executing afterInit() of {} (priority: {})", c.getClass(), c.getPriority());
            c.afterInit(service);
        });
    }

    public boolean isEventDispatcherEnabled() {
        return getEventDispatcher() != null && getEventDispatcher().isEnabled();
    }

    public boolean isEnableEventDispatcher() {
        return enableEventDispatcher;
    }

    public AbstractServiceConfiguration<S> setEnableEventDispatcher(boolean enableEventDispatcher) {
        this.enableEventDispatcher = enableEventDispatcher;
        return this;
    }

    public FlowableEventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    public AbstractServiceConfiguration<S> setEventDispatcher(FlowableEventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
        return this;
    }

    public List<FlowableEventListener> getEventListeners() {
        return eventListeners;
    }

    public AbstractServiceConfiguration<S> setEventListeners(List<FlowableEventListener> eventListeners) {
        this.eventListeners = eventListeners;
        return this;
    }

    public Map<String, List<FlowableEventListener>> getTypedEventListeners() {
        return typedEventListeners;
    }

    public AbstractServiceConfiguration<S> setTypedEventListeners(Map<String, List<FlowableEventListener>> typedEventListeners) {
        this.typedEventListeners = typedEventListeners;
        return this;
    }

    public List<EventDispatchAction> getAdditionalEventDispatchActions() {
        return additionalEventDispatchActions;
    }

    public AbstractServiceConfiguration<S> setAdditionalEventDispatchActions(List<EventDispatchAction> additionalEventDispatchActions) {
        this.additionalEventDispatchActions = additionalEventDispatchActions;
        return this;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public AbstractServiceConfiguration<S> setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    public Clock getClock() {
        return clock;
    }

    public AbstractServiceConfiguration<S> setClock(Clock clock) {
        this.clock = clock;
        return this;
    }

    public IdGenerator getIdGenerator() {
        return idGenerator;
    }

    public AbstractServiceConfiguration<S> setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
        return this;
    }
}
