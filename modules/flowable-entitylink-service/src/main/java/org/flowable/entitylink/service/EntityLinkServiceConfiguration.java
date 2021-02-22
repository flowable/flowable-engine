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
package org.flowable.entitylink.service;

import org.flowable.common.engine.impl.AbstractServiceConfiguration;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.entitylink.api.EntityLinkService;
import org.flowable.entitylink.api.history.HistoricEntityLinkService;
import org.flowable.entitylink.service.impl.EntityLinkServiceImpl;
import org.flowable.entitylink.service.impl.HistoricEntityLinkServiceImpl;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntityManager;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntityManagerImpl;
import org.flowable.entitylink.service.impl.persistence.entity.HistoricEntityLinkEntityManager;
import org.flowable.entitylink.service.impl.persistence.entity.HistoricEntityLinkEntityManagerImpl;
import org.flowable.entitylink.service.impl.persistence.entity.data.EntityLinkDataManager;
import org.flowable.entitylink.service.impl.persistence.entity.data.HistoricEntityLinkDataManager;
import org.flowable.entitylink.service.impl.persistence.entity.data.impl.MybatisEntityLinkDataManager;
import org.flowable.entitylink.service.impl.persistence.entity.data.impl.MybatisHistoricEntityLinkDataManager;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tijs Rademakers
 */
public class EntityLinkServiceConfiguration extends AbstractServiceConfiguration {

    // SERVICES
    // /////////////////////////////////////////////////////////////////

    protected EntityLinkService entityLinkService = new EntityLinkServiceImpl(this);
    protected HistoricEntityLinkService historicEntityLinkService = new HistoricEntityLinkServiceImpl(this);

    // DATA MANAGERS ///////////////////////////////////////////////////

    protected EntityLinkDataManager entityLinkDataManager;
    protected HistoricEntityLinkDataManager historicEntityLinkDataManager;

    // ENTITY MANAGERS /////////////////////////////////////////////////
    
    protected EntityLinkEntityManager entityLinkEntityManager;
    protected HistoricEntityLinkEntityManager historicEntityLinkEntityManager;
    
    protected HistoryLevel historyLevel;
    
    protected ObjectMapper objectMapper;
    
    public EntityLinkServiceConfiguration(String engineName) {
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
        if (entityLinkDataManager == null) {
            entityLinkDataManager = new MybatisEntityLinkDataManager(this);
        }
        if (historicEntityLinkDataManager == null) {
            historicEntityLinkDataManager = new MybatisHistoricEntityLinkDataManager(this);
        }
    }

    public void initEntityManagers() {
        if (entityLinkEntityManager == null) {
            entityLinkEntityManager = new EntityLinkEntityManagerImpl(this, entityLinkDataManager);
        }
        if (historicEntityLinkEntityManager == null) {
            historicEntityLinkEntityManager = new HistoricEntityLinkEntityManagerImpl(this, historicEntityLinkDataManager);
        }
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    public EntityLinkServiceConfiguration getIdentityLinkServiceConfiguration() {
        return this;
    }
    
    public EntityLinkService getEntityLinkService() {
        return entityLinkService;
    }

    public EntityLinkServiceConfiguration setEntityLinkService(EntityLinkService entityLinkService) {
        this.entityLinkService = entityLinkService;
        return this;
    }
    
    public HistoricEntityLinkService getHistoricEntityLinkService() {
        return historicEntityLinkService;
    }

    public EntityLinkServiceConfiguration setHistoricEntityLinkService(HistoricEntityLinkService historicEntityLinkService) {
        this.historicEntityLinkService = historicEntityLinkService;
        return this;
    }

    public EntityLinkDataManager getEntityLinkDataManager() {
        return entityLinkDataManager;
    }

    public EntityLinkServiceConfiguration setEntityLinkDataManager(EntityLinkDataManager entityLinkDataManager) {
        this.entityLinkDataManager = entityLinkDataManager;
        return this;
    }
    
    public HistoricEntityLinkDataManager getHistoricEntityLinkDataManager() {
        return historicEntityLinkDataManager;
    }

    public EntityLinkServiceConfiguration setHistoricEntityLinkDataManager(HistoricEntityLinkDataManager historicEntityLinkDataManager) {
        this.historicEntityLinkDataManager = historicEntityLinkDataManager;
        return this;
    }

    public EntityLinkEntityManager getEntityLinkEntityManager() {
        return entityLinkEntityManager;
    }

    public EntityLinkServiceConfiguration setEntityLinkEntityManager(EntityLinkEntityManager entityLinkEntityManager) {
        this.entityLinkEntityManager = entityLinkEntityManager;
        return this;
    }
    
    public HistoricEntityLinkEntityManager getHistoricEntityLinkEntityManager() {
        return historicEntityLinkEntityManager;
    }

    public EntityLinkServiceConfiguration setHistoricEntityLinkEntityManager(HistoricEntityLinkEntityManager historicEntityLinkEntityManager) {
        this.historicEntityLinkEntityManager = historicEntityLinkEntityManager;
        return this;
    }
    
    @Override
    public HistoryLevel getHistoryLevel() {
        return historyLevel;
    }
    
    @Override
    public EntityLinkServiceConfiguration setHistoryLevel(HistoryLevel historyLevel) {
        this.historyLevel = historyLevel;
        return this;
    }
    
    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public EntityLinkServiceConfiguration setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }
}