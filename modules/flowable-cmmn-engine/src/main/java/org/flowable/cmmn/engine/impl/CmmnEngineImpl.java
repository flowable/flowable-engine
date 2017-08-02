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
package org.flowable.cmmn.engine.impl;

import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.CmmnRepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CmmnEngineImpl implements CmmnEngine {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CmmnEngineImpl.class);

    protected String name;
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    protected CmmnRepositoryService cmmnRepositoryService;
    
    public CmmnEngineImpl(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
        this.name = cmmnEngineConfiguration.getEngineName();
        this.cmmnRepositoryService = cmmnEngineConfiguration.getCmmnRepositoryService();

        LOGGER.info("CmmnEngine {} created", name);
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public void close() {
        // TODO (see ProcessEngineImpl)
    }
    
    public CmmnEngineConfiguration getCmmnEngineConfiguration() {
        return cmmnEngineConfiguration;
    }

    public void setCmmnEngineConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }
    
    public CmmnRepositoryService getCmmnRepositoryService() {
        return cmmnRepositoryService;
    }
    
    public void setCmmnRepositoryService(CmmnRepositoryService cmmnRepositoryService) {
        this.cmmnRepositoryService = cmmnRepositoryService;
    }

}
