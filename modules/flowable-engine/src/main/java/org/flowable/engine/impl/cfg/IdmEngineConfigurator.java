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
package org.flowable.engine.impl.cfg;

import java.util.Arrays;
import java.util.List;

import org.apache.ibatis.type.JdbcType;
import org.flowable.engine.cfg.AbstractEngineConfigurator;
import org.flowable.engine.common.impl.db.CustomMyBatisTypeHandlerConfig;
import org.flowable.engine.common.impl.db.CustomMybatisTypeAliasConfig;
import org.flowable.engine.common.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.common.impl.persistence.entity.Entity;
import org.flowable.engine.impl.persistence.deploy.Deployer;
import org.flowable.idm.engine.IdmEngine;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.cfg.StandaloneIdmEngineConfiguration;
import org.flowable.idm.engine.impl.db.EntityDependencyOrder;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class IdmEngineConfigurator extends AbstractEngineConfigurator {

    protected IdmEngineConfiguration idmEngineConfiguration;
    
    @Override
    public int getPriority() {
        return EngineConfigurationConstants.PRIORITY_ENGINE_IDM;
    }
    
    @Override
    protected List<Deployer> getCustomDeployers() {
        return null;
    }
    
    @Override
    protected String getMybatisCfgPath() {
        return IdmEngineConfiguration.DEFAULT_MYBATIS_MAPPING_FILE;
    }
    
    @Override
    protected List<CustomMybatisTypeAliasConfig> getMybatisTypeAliases() {
        return Arrays.asList(new CustomMybatisTypeAliasConfig("IdmByteArrayRefTypeHandler", org.flowable.idm.engine.impl.persistence.ByteArrayRefTypeHandler.class));
    }
    
    @Override
    protected List<CustomMyBatisTypeHandlerConfig> getMybatisTypeHandlers() {
        return Arrays.asList(new CustomMyBatisTypeHandlerConfig(
                org.flowable.idm.engine.impl.persistence.entity.ByteArrayRef.class, 
                JdbcType.VARCHAR, 
                org.flowable.idm.engine.impl.persistence.ByteArrayRefTypeHandler.class));
    }
    
    @Override
    public void configure(ProcessEngineConfigurationImpl processEngineConfiguration) {
        if (idmEngineConfiguration == null) {
            idmEngineConfiguration = new StandaloneIdmEngineConfiguration();
            initialiseCommonProperties(processEngineConfiguration, idmEngineConfiguration, EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG);
        }
        
        IdmEngine idmEngine = idmEngineConfiguration.buildIdmEngine();
        processEngineConfiguration.setIdmEngineInitialized(true);
        processEngineConfiguration.setIdmIdentityService(idmEngine.getIdmIdentityService());
    }
    
    @Override
    protected List<Class<? extends Entity>> getEntityInsertionOrder() {
        return EntityDependencyOrder.INSERT_ORDER;
    }
    
    @Override
    protected List<Class<? extends Entity>> getEntityDeletionOrder() {
        return EntityDependencyOrder.DELETE_ORDER;
    }

    public IdmEngineConfiguration getIdmEngineConfiguration() {
        return idmEngineConfiguration;
    }

    public IdmEngineConfigurator setIdmEngineConfiguration(IdmEngineConfiguration idmEngineConfiguration) {
        this.idmEngineConfiguration = idmEngineConfiguration;
        return this;
    }

}
