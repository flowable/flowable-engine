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
package org.flowable.batch.service;

import org.flowable.batch.api.BatchService;
import org.flowable.batch.service.impl.BatchServiceImpl;
import org.flowable.batch.service.impl.persistence.entity.BatchEntityManager;
import org.flowable.batch.service.impl.persistence.entity.BatchEntityManagerImpl;
import org.flowable.batch.service.impl.persistence.entity.BatchPartEntityManager;
import org.flowable.batch.service.impl.persistence.entity.BatchPartEntityManagerImpl;
import org.flowable.batch.service.impl.persistence.entity.data.BatchDataManager;
import org.flowable.batch.service.impl.persistence.entity.data.BatchPartDataManager;
import org.flowable.batch.service.impl.persistence.entity.data.impl.MybatisBatchDataManager;
import org.flowable.batch.service.impl.persistence.entity.data.impl.MybatisBatchPartDataManager;
import org.flowable.common.engine.impl.AbstractServiceConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tijs Rademakers
 */
public class BatchServiceConfiguration extends AbstractServiceConfiguration {

    // SERVICES
    // /////////////////////////////////////////////////////////////////

    protected BatchService batchService = new BatchServiceImpl(this);

    // DATA MANAGERS ///////////////////////////////////////////////////

    protected BatchDataManager batchDataManager;
    protected BatchPartDataManager batchPartDataManager;

    // ENTITY MANAGERS /////////////////////////////////////////////////
    
    protected BatchEntityManager batchEntityManager;
    protected BatchPartEntityManager batchPartEntityManager;
    
    protected ObjectMapper objectMapper;
    
    public BatchServiceConfiguration(String engineName) {
        super(engineName);
    }

    // init
    // /////////////////////////////////////////////////////////////////////

    public void init() {
        initDataManagers();
        initEntityManagers();
    }

    // Data managers
    ///////////////////////////////////////////////////////////

    public void initDataManagers() {
        if (batchDataManager == null) {
            batchDataManager = new MybatisBatchDataManager(this);
        }
        if (batchPartDataManager == null) {
            batchPartDataManager = new MybatisBatchPartDataManager(this);
        }
    }

    public void initEntityManagers() {
        if (batchEntityManager == null) {
            batchEntityManager = new BatchEntityManagerImpl(this, batchDataManager);
        }
        if (batchPartEntityManager == null) {
            batchPartEntityManager = new BatchPartEntityManagerImpl(this, batchPartDataManager);
        }
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    public BatchServiceConfiguration getIdentityLinkServiceConfiguration() {
        return this;
    }
    
    public BatchService getBatchService() {
        return batchService;
    }

    public BatchServiceConfiguration setBatchService(BatchService batchService) {
        this.batchService = batchService;
        return this;
    }
    
    public BatchDataManager getBatchDataManager() {
        return batchDataManager;
    }

    public BatchServiceConfiguration setBatchDataManager(BatchDataManager batchDataManager) {
        this.batchDataManager = batchDataManager;
        return this;
    }

    public BatchPartDataManager getBatchPartDataManager() {
        return batchPartDataManager;
    }

    public BatchServiceConfiguration setBatchPartDataManager(BatchPartDataManager batchPartDataManager) {
        this.batchPartDataManager = batchPartDataManager;
        return this;
    }

    public BatchEntityManager getBatchEntityManager() {
        return batchEntityManager;
    }

    public BatchServiceConfiguration setBatchEntityManager(BatchEntityManager batchEntityManager) {
        this.batchEntityManager = batchEntityManager;
        return this;
    }

    public BatchPartEntityManager getBatchPartEntityManager() {
        return batchPartEntityManager;
    }

    public BatchServiceConfiguration setBatchPartEntityManager(BatchPartEntityManager batchPartEntityManager) {
        this.batchPartEntityManager = batchPartEntityManager;
        return this;
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public BatchServiceConfiguration setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }
}