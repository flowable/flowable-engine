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
package org.flowable.app.engine.impl.cfg;

import java.util.Collections;
import java.util.List;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.AbstractEngineConfigurator;
import org.flowable.common.engine.impl.EngineDeployer;
import org.flowable.common.engine.impl.db.MybatisTypeAliasConfigurator;
import org.flowable.common.engine.impl.db.MybatisTypeHandlerConfigurator;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.cfg.StandaloneIdmEngineConfiguration;
import org.flowable.idm.engine.impl.db.EntityDependencyOrder;
import org.flowable.idm.engine.impl.persistence.ByteArrayRefTypeHandler;
import org.flowable.idm.engine.impl.persistence.entity.ByteArrayRef;

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
    protected List<EngineDeployer> getCustomDeployers() {
        return null;
    }

    @Override
    protected String getMybatisCfgPath() {
        return IdmEngineConfiguration.DEFAULT_MYBATIS_MAPPING_FILE;
    }

    @Override
    protected List<MybatisTypeAliasConfigurator> getMybatisTypeAliases() {
        return Collections.<MybatisTypeAliasConfigurator>singletonList(new MybatisTypeAliasConfigurator() {
            @Override
            public void configure(TypeAliasRegistry typeAliasRegistry) {
                typeAliasRegistry.registerAlias("IdmByteArrayRefTypeHandler", ByteArrayRefTypeHandler.class);
            }
        });
    }

    @Override
    protected List<MybatisTypeHandlerConfigurator> getMybatisTypeHandlers() {
        return Collections.<MybatisTypeHandlerConfigurator>singletonList(new MybatisTypeHandlerConfigurator() {
            @Override
            public void configure(TypeHandlerRegistry typeHandlerRegistry) {
                typeHandlerRegistry.register(
                        ByteArrayRef.class,
                        JdbcType.VARCHAR,
                        ByteArrayRefTypeHandler.class
                );
            }
        });
    }

    @Override
    public void configure(AbstractEngineConfiguration engineConfiguration) {
        if (idmEngineConfiguration == null) {
            idmEngineConfiguration = new StandaloneIdmEngineConfiguration();
        }

        initialiseCommonProperties(engineConfiguration, idmEngineConfiguration);

        idmEngineConfiguration.buildIdmEngine();

        initServiceConfigurations(engineConfiguration, idmEngineConfiguration);
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
