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
package org.flowable.identitylink.service;

import org.flowable.common.engine.impl.AbstractServiceConfiguration;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.identitylink.service.impl.HistoricIdentityLinkServiceImpl;
import org.flowable.identitylink.service.impl.IdentityLinkServiceImpl;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntityManager;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntityManagerImpl;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityManager;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityManagerImpl;
import org.flowable.identitylink.service.impl.persistence.entity.data.HistoricIdentityLinkDataManager;
import org.flowable.identitylink.service.impl.persistence.entity.data.IdentityLinkDataManager;
import org.flowable.identitylink.service.impl.persistence.entity.data.impl.MybatisHistoricIdentityLinkDataManager;
import org.flowable.identitylink.service.impl.persistence.entity.data.impl.MybatisIdentityLinkDataManager;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tijs Rademakers
 */
public class IdentityLinkServiceConfiguration extends AbstractServiceConfiguration {

    // SERVICES
    // /////////////////////////////////////////////////////////////////

    protected IdentityLinkService identityLinkService = new IdentityLinkServiceImpl(this);
    protected HistoricIdentityLinkService historicIdentityLinkService = new HistoricIdentityLinkServiceImpl(this);

    // DATA MANAGERS ///////////////////////////////////////////////////

    protected IdentityLinkDataManager identityLinkDataManager;
    protected HistoricIdentityLinkDataManager historicIdentityLinkDataManager;

    // ENTITY MANAGERS /////////////////////////////////////////////////
    
    protected IdentityLinkEntityManager identityLinkEntityManager;
    protected HistoricIdentityLinkEntityManager historicIdentityLinkEntityManager;

    /** IdentityLink event handler */
    protected IdentityLinkEventHandler identityLinkEventHandler;
    
    protected HistoryLevel historyLevel;
    
    protected ObjectMapper objectMapper;
    
    public IdentityLinkServiceConfiguration(String engineName) {
        super(engineName);
    }

    // init
    // /////////////////////////////////////////////////////////////////////

    public void init() {
        initDataManagers();
        initEntityManagers();
    }
    
    @Override
    public boolean isHistoryLevelAtLeast(HistoryLevel level) {
        if (logger.isDebugEnabled()) {
            logger.debug("Current history level: {}, level required: {}", historyLevel, level);
        }
        // Comparing enums actually compares the location of values declared in the enum
        return historyLevel.isAtLeast(level);
    }

    @Override
    public boolean isHistoryEnabled() {
        if (logger.isDebugEnabled()) {
            logger.debug("Current history level: {}", historyLevel);
        }
        return historyLevel != HistoryLevel.NONE;
    }

    // Data managers
    ///////////////////////////////////////////////////////////

    public void initDataManagers() {
        if (identityLinkDataManager == null) {
            identityLinkDataManager = new MybatisIdentityLinkDataManager(this);
        }
        if (historicIdentityLinkDataManager == null) {
            historicIdentityLinkDataManager = new MybatisHistoricIdentityLinkDataManager(this);
        }
    }

    public void initEntityManagers() {
        if (identityLinkEntityManager == null) {
            identityLinkEntityManager = new IdentityLinkEntityManagerImpl(this, identityLinkDataManager);
        }
        if (historicIdentityLinkEntityManager == null) {
            historicIdentityLinkEntityManager = new HistoricIdentityLinkEntityManagerImpl(this, historicIdentityLinkDataManager);
        }
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    public IdentityLinkServiceConfiguration getIdentityLinkServiceConfiguration() {
        return this;
    }
    
    public IdentityLinkService getIdentityLinkService() {
        return identityLinkService;
    }

    public IdentityLinkServiceConfiguration setIdentityLinkService(IdentityLinkService identityLinkService) {
        this.identityLinkService = identityLinkService;
        return this;
    }
    
    public HistoricIdentityLinkService getHistoricIdentityLinkService() {
        return historicIdentityLinkService;
    }

    public IdentityLinkServiceConfiguration setHistoricIdentityLinkService(HistoricIdentityLinkService historicIdentityLinkService) {
        this.historicIdentityLinkService = historicIdentityLinkService;
        return this;
    }

    public IdentityLinkDataManager getIdentityLinkDataManager() {
        return identityLinkDataManager;
    }

    public IdentityLinkServiceConfiguration setIdentityLinkDataManager(IdentityLinkDataManager identityLinkDataManager) {
        this.identityLinkDataManager = identityLinkDataManager;
        return this;
    }
    
    public HistoricIdentityLinkDataManager getHistoricIdentityLinkDataManager() {
        return historicIdentityLinkDataManager;
    }

    public IdentityLinkServiceConfiguration setHistoricIdentityLinkDataManager(HistoricIdentityLinkDataManager historicIdentityLinkDataManager) {
        this.historicIdentityLinkDataManager = historicIdentityLinkDataManager;
        return this;
    }

    public IdentityLinkEntityManager getIdentityLinkEntityManager() {
        return identityLinkEntityManager;
    }

    public IdentityLinkServiceConfiguration setIdentityLinkEntityManager(IdentityLinkEntityManager identityLinkEntityManager) {
        this.identityLinkEntityManager = identityLinkEntityManager;
        return this;
    }
    
    public HistoricIdentityLinkEntityManager getHistoricIdentityLinkEntityManager() {
        return historicIdentityLinkEntityManager;
    }

    public IdentityLinkServiceConfiguration setHistoricIdentityLinkEntityManager(HistoricIdentityLinkEntityManager historicIdentityLinkEntityManager) {
        this.historicIdentityLinkEntityManager = historicIdentityLinkEntityManager;
        return this;
    }
    
    @Override
    public HistoryLevel getHistoryLevel() {
        return historyLevel;
    }
    
    @Override
    public IdentityLinkServiceConfiguration setHistoryLevel(HistoryLevel historyLevel) {
        this.historyLevel = historyLevel;
        return this;
    }
    
    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public IdentityLinkServiceConfiguration setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    public IdentityLinkEventHandler getIdentityLinkEventHandler() {
        return identityLinkEventHandler;
    }

    public IdentityLinkServiceConfiguration setIdentityLinkEventHandler(IdentityLinkEventHandler identityLinkEventHandler) {
        this.identityLinkEventHandler = identityLinkEventHandler;
        return this;
    }
}
