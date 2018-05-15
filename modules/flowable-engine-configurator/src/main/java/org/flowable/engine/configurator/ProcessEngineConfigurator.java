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
package org.flowable.engine.configurator;

import java.util.Collections;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.AbstractEngineConfigurator;
import org.flowable.common.engine.impl.EngineDeployer;
import org.flowable.common.engine.impl.HasTaskIdGeneratorEngineConfiguration;
import org.flowable.common.engine.impl.db.DbSqlSessionFactory;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.configurator.impl.deployer.BpmnDeployer;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.impl.db.EntityDependencyOrder;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityImpl;
import org.flowable.variable.service.impl.persistence.entity.VariableByteArrayEntityImpl;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntityImpl;

/**
 * @author Tijs Rademakers
 */
public class ProcessEngineConfigurator extends AbstractEngineConfigurator {

    protected ProcessEngineConfiguration processEngineConfiguration;

    @Override
    public int getPriority() {
        return EngineConfigurationConstants.PRIORITY_ENGINE_PROCESS;
    }

    @Override
    protected List<EngineDeployer> getCustomDeployers() {
        return Collections.<EngineDeployer>singletonList(new BpmnDeployer());
    }

    @Override
    protected String getMybatisCfgPath() {
        return ProcessEngineConfigurationImpl.DEFAULT_MYBATIS_MAPPING_FILE;
    }

    @Override
    public void configure(AbstractEngineConfiguration engineConfiguration) {
        if (processEngineConfiguration == null) {
            processEngineConfiguration = new StandaloneProcessEngineConfiguration();
        }

        initialiseCommonProperties(engineConfiguration, processEngineConfiguration);

        initProcessEngine();

        initServiceConfigurations(engineConfiguration, processEngineConfiguration);
    }
    
    @Override
    protected void initDbSqlSessionFactory(AbstractEngineConfiguration engineConfiguration, AbstractEngineConfiguration targetEngineConfiguration) {
        DbSqlSessionFactory dbSqlSessionFactory = engineConfiguration.getDbSqlSessionFactory();
        targetEngineConfiguration.setDbSqlSessionFactory(engineConfiguration.getDbSqlSessionFactory());
        targetEngineConfiguration.setSqlSessionFactory(engineConfiguration.getSqlSessionFactory());

        if (getEntityInsertionOrder() != null) {
            // remove identity link and variable entity classes due to foreign key handling
            dbSqlSessionFactory.getInsertionOrder().remove(IdentityLinkEntityImpl.class);
            dbSqlSessionFactory.getInsertionOrder().remove(VariableInstanceEntityImpl.class);
            dbSqlSessionFactory.getInsertionOrder().remove(VariableByteArrayEntityImpl.class);
            for (Class<? extends Entity> clazz : getEntityInsertionOrder()) {
                dbSqlSessionFactory.getInsertionOrder().add(clazz);
            }
        }

        if (getEntityDeletionOrder() != null) {
            // remove identity link and variable entity classes due to foreign key handling
            dbSqlSessionFactory.getDeletionOrder().remove(IdentityLinkEntityImpl.class);
            dbSqlSessionFactory.getDeletionOrder().remove(VariableInstanceEntityImpl.class);
            dbSqlSessionFactory.getDeletionOrder().remove(VariableByteArrayEntityImpl.class);
            for (Class<? extends Entity> clazz : getEntityDeletionOrder()) {
                dbSqlSessionFactory.getDeletionOrder().add(clazz);
            }
        }
    }

    @Override
    protected List<Class<? extends Entity>> getEntityInsertionOrder() {
        return EntityDependencyOrder.INSERT_ORDER;
    }

    @Override
    protected List<Class<? extends Entity>> getEntityDeletionOrder() {
        return EntityDependencyOrder.DELETE_ORDER;
    }

    protected synchronized ProcessEngine initProcessEngine() {
        if (processEngineConfiguration == null) {
            throw new FlowableException("ProcessEngineConfiguration is required");
        }

        return processEngineConfiguration.buildProcessEngine();
    }

    @Override
    protected void initIdGenerator(AbstractEngineConfiguration engineConfiguration, AbstractEngineConfiguration targetEngineConfiguration) {
        super.initIdGenerator(engineConfiguration, targetEngineConfiguration);
        if (targetEngineConfiguration instanceof HasTaskIdGeneratorEngineConfiguration) {
            HasTaskIdGeneratorEngineConfiguration targetEgineConfiguration = (HasTaskIdGeneratorEngineConfiguration) targetEngineConfiguration;
            if (targetEgineConfiguration.getTaskIdGenerator() == null) {
                if (engineConfiguration instanceof HasTaskIdGeneratorEngineConfiguration) {
                    targetEgineConfiguration.setTaskIdGenerator(((HasTaskIdGeneratorEngineConfiguration) engineConfiguration).getTaskIdGenerator());
                } else {
                    targetEgineConfiguration.setTaskIdGenerator(engineConfiguration.getIdGenerator());
                }
            }
        }
    }

    public ProcessEngineConfiguration getProcessEngineConfiguration() {
        return processEngineConfiguration;
    }

    public ProcessEngineConfigurator setProcessEngineConfiguration(ProcessEngineConfiguration processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
        return this;
    }
}
