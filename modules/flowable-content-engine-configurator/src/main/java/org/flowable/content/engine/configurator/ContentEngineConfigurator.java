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
package org.flowable.content.engine.configurator;

import java.util.List;

import org.flowable.content.engine.ContentEngine;
import org.flowable.content.engine.ContentEngineConfiguration;
import org.flowable.content.engine.impl.cfg.StandaloneContentEngineConfiguration;
import org.flowable.content.engine.impl.db.EntityDependencyOrder;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.AbstractEngineConfiguration;
import org.flowable.engine.common.impl.AbstractEngineConfigurator;
import org.flowable.engine.common.impl.EngineDeployer;
import org.flowable.engine.common.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.common.impl.persistence.entity.Entity;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class ContentEngineConfigurator extends AbstractEngineConfigurator {

    protected ContentEngineConfiguration contentEngineConfiguration;
    
    @Override
    public int getPriority() {
        return EngineConfigurationConstants.PRIORITY_ENGINE_CONTENT;
    }
    
    @Override
    protected List<EngineDeployer> getCustomDeployers() {
        return null;
    }
    
    @Override
    protected String getMybatisCfgPath() {
        return ContentEngineConfiguration.DEFAULT_MYBATIS_MAPPING_FILE;
    }

    @Override
    public void configure(AbstractEngineConfiguration engineConfiguration) {
        if (contentEngineConfiguration == null) {
            contentEngineConfiguration = new StandaloneContentEngineConfiguration();
        }
        
        initialiseCommonProperties(engineConfiguration, contentEngineConfiguration);

        initContentEngine();
        
        initServiceConfigurations(engineConfiguration, contentEngineConfiguration);
    }
    
    @Override
    protected List<Class<? extends Entity>> getEntityInsertionOrder() {
        return EntityDependencyOrder.INSERT_ORDER;
    }
    
    @Override
    protected List<Class<? extends Entity>> getEntityDeletionOrder() {
        return EntityDependencyOrder.DELETE_ORDER;
    }

    protected synchronized ContentEngine initContentEngine() {
        if (contentEngineConfiguration == null) {
            throw new FlowableException("ContentEngineConfiguration is required");
        }

        return contentEngineConfiguration.buildContentEngine();
    }

    public ContentEngineConfiguration getContentEngineConfiguration() {
        return contentEngineConfiguration;
    }

    public ContentEngineConfigurator setContentEngineConfiguration(ContentEngineConfiguration contentEngineConfiguration) {
        this.contentEngineConfiguration = contentEngineConfiguration;
        return this;
    }

}
