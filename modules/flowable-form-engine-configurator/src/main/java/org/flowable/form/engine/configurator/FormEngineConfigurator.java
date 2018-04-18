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
package org.flowable.form.engine.configurator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.AbstractEngineConfigurator;
import org.flowable.common.engine.impl.EngineDeployer;
import org.flowable.common.engine.impl.db.MybatisTypeAliasConfigurator;
import org.flowable.common.engine.impl.db.MybatisTypeHandlerConfigurator;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.deployer.FormDeployer;
import org.flowable.form.engine.impl.cfg.StandaloneFormEngineConfiguration;
import org.flowable.form.engine.impl.db.EntityDependencyOrder;
import org.flowable.form.engine.impl.persistence.ResourceRefTypeHandler;
import org.flowable.form.engine.impl.persistence.entity.ResourceRef;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class FormEngineConfigurator extends AbstractEngineConfigurator {

    protected FormEngineConfiguration formEngineConfiguration;

    @Override
    public int getPriority() {
        return EngineConfigurationConstants.PRIORITY_ENGINE_FORM;
    }

    @Override
    protected List<EngineDeployer> getCustomDeployers() {
        List<EngineDeployer> deployers = new ArrayList<>();
        deployers.add(new FormDeployer());
        return deployers;
    }

    @Override
    protected String getMybatisCfgPath() {
        return FormEngineConfiguration.DEFAULT_MYBATIS_MAPPING_FILE;
    }

    @Override
    protected List<MybatisTypeAliasConfigurator> getMybatisTypeAliases() {
        return Collections.<MybatisTypeAliasConfigurator>singletonList(new MybatisTypeAliasConfigurator() {
            @Override
            public void configure(TypeAliasRegistry typeAliasRegistry) {
                typeAliasRegistry.registerAlias("ResourceRefTypeHandler", ResourceRefTypeHandler.class);
            }
        });
    }

    @Override
    protected List<MybatisTypeHandlerConfigurator> getMybatisTypeHandlers() {
        return Collections.<MybatisTypeHandlerConfigurator>singletonList(new MybatisTypeHandlerConfigurator() {
            @Override
            public void configure(TypeHandlerRegistry typeHandlerRegistry) {
                typeHandlerRegistry.register(ResourceRef.class,
                        JdbcType.VARCHAR,
                        ResourceRefTypeHandler.class);
            }
        });
    }

    @Override
    public void configure(AbstractEngineConfiguration engineConfiguration) {
        if (formEngineConfiguration == null) {
            formEngineConfiguration = new StandaloneFormEngineConfiguration();
        }

        initialiseCommonProperties(engineConfiguration, formEngineConfiguration);

        initFormEngine();

        initServiceConfigurations(engineConfiguration, formEngineConfiguration);
    }

    @Override
    protected List<Class<? extends Entity>> getEntityInsertionOrder() {
        return EntityDependencyOrder.INSERT_ORDER;
    }

    @Override
    protected List<Class<? extends Entity>> getEntityDeletionOrder() {
        return EntityDependencyOrder.DELETE_ORDER;
    }

    protected synchronized FormEngine initFormEngine() {
        if (formEngineConfiguration == null) {
            throw new FlowableException("FormEngineConfiguration is required");
        }

        return formEngineConfiguration.buildFormEngine();
    }

    public FormEngineConfiguration getFormEngineConfiguration() {
        return formEngineConfiguration;
    }

    public FormEngineConfigurator setFormEngineConfiguration(FormEngineConfiguration formEngineConfiguration) {
        this.formEngineConfiguration = formEngineConfiguration;
        return this;
    }

}
