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
package org.flowable.eventsubscription.service;

import java.time.Duration;
import java.util.UUID;

import org.flowable.common.engine.impl.AbstractServiceConfiguration;
import org.flowable.eventsubscription.service.impl.EventSubscriptionServiceImpl;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntityManager;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntityManagerImpl;
import org.flowable.eventsubscription.service.impl.persistence.entity.data.EventSubscriptionDataManager;
import org.flowable.eventsubscription.service.impl.persistence.entity.data.impl.MybatisEventSubscriptionDataManager;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tijs Rademakers
 */
public class EventSubscriptionServiceConfiguration extends AbstractServiceConfiguration<EventSubscriptionServiceConfiguration> {

    // SERVICES
    // /////////////////////////////////////////////////////////////////

    protected EventSubscriptionService eventSubscriptionService = new EventSubscriptionServiceImpl(this);

    // DATA MANAGERS
    // /////////////////////////////////////////////////

    protected EventSubscriptionDataManager eventSubscriptionDataManager;

    // ENTITY MANAGERS
    // ///////////////////////////////////////////////

    protected EventSubscriptionEntityManager eventSubscriptionEntityManager;

    // LOCKING
    // //////////////////////////////////////////////

    /**
     * The amount of time an event subscription is locked.
     * A lock on an event subscription is used when using the 'unique process/case instance on start' feature.
     */
    private Duration eventSubscriptionLockTime = Duration.ofMinutes(10);

    /**
     * The value that should be used when locking eventsubscriptions.
     */
    private String lockOwner = UUID.randomUUID().toString();

    protected ObjectMapper objectMapper;

    public EventSubscriptionServiceConfiguration(String engineName) {
        super(engineName);
    }

    @Override
    protected EventSubscriptionServiceConfiguration getService() {
        return this;
    }

    // init
    // /////////////////////////////////////////////////////////////////////

    public void init() {
        configuratorsBeforeInit();

        initDataManagers();
        initEntityManagers();

        configuratorsAfterInit();
    }

    // Data managers
    ///////////////////////////////////////////////////////////

    public void initDataManagers() {
        if (eventSubscriptionDataManager == null) {
            eventSubscriptionDataManager = new MybatisEventSubscriptionDataManager(this);
        }
    }

    public void initEntityManagers() {
        if (eventSubscriptionEntityManager == null) {
            eventSubscriptionEntityManager = new EventSubscriptionEntityManagerImpl(this, eventSubscriptionDataManager);
        }
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    public EventSubscriptionServiceConfiguration getIdentityLinkServiceConfiguration() {
        return this;
    }

    public EventSubscriptionService getEventSubscriptionService() {
        return eventSubscriptionService;
    }

    public EventSubscriptionServiceConfiguration setEventSubscriptionService(EventSubscriptionService eventSubscriptionService) {
        this.eventSubscriptionService = eventSubscriptionService;
        return this;
    }

    public EventSubscriptionDataManager getEventSubscriptionDataManager() {
        return eventSubscriptionDataManager;
    }

    public EventSubscriptionServiceConfiguration setEventSubscriptionDataManager(EventSubscriptionDataManager eventSubscriptionDataManager) {
        this.eventSubscriptionDataManager = eventSubscriptionDataManager;
        return this;
    }

    public EventSubscriptionEntityManager getEventSubscriptionEntityManager() {
        return eventSubscriptionEntityManager;
    }

    public EventSubscriptionServiceConfiguration setEventSubscriptionEntityManager(EventSubscriptionEntityManager eventSubscriptionEntityManager) {
        this.eventSubscriptionEntityManager = eventSubscriptionEntityManager;
        return this;
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public EventSubscriptionServiceConfiguration setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    public Duration getEventSubscriptionLockTime() {
        return eventSubscriptionLockTime;
    }

    public EventSubscriptionServiceConfiguration setEventSubscriptionLockTime(Duration eventSubscriptionLockTime) {
        this.eventSubscriptionLockTime = eventSubscriptionLockTime;
        return this;
    }

    public String getLockOwner() {
        return lockOwner;
    }

    public EventSubscriptionServiceConfiguration setLockOwner(String lockOwner) {
        this.lockOwner = lockOwner;
        return this;
    }
}
