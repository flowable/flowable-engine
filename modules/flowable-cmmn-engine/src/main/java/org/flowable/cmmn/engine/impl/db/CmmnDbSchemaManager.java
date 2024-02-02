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
package org.flowable.cmmn.engine.impl.db;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.db.EngineDatabaseConfiguration;
import org.flowable.common.engine.impl.db.LiquibaseBasedSchemaManager;
import org.flowable.common.engine.impl.db.LiquibaseDatabaseConfiguration;
import org.flowable.common.engine.impl.db.SchemaManager;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.idm.engine.IdmEngineConfiguration;

public class CmmnDbSchemaManager extends LiquibaseBasedSchemaManager {

    public static final String LIQUIBASE_CHANGELOG = "org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml";

    public static final String LIQUIBASE_CHANGELOG_CRDB = "org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog-crdb.xml";

    public CmmnDbSchemaManager(String changelogFile) {
        super("cmmn", changelogFile, CmmnEngineConfiguration.LIQUIBASE_CHANGELOG_PREFIX);
    }

    @Override
    protected LiquibaseDatabaseConfiguration getDatabaseConfiguration() {
        return new EngineDatabaseConfiguration(CommandContextUtil.getCmmnEngineConfiguration());
    }

    public void initSchema() {
        initSchema(CommandContextUtil.getCmmnEngineConfiguration().getDatabaseSchemaUpdate());
    }

    @Override
    public void initSchema(String databaseSchemaUpdate) {
        super.initSchema(databaseSchemaUpdate);

        // When the databaseSchemaUpdate is drop-create:
        // the IDM engine will have done a drop-create due to the configurator running first
        // The CmmnDbSchemaManager will go next, but it will do a dropAll, dropping the idm tables too.
        if (AbstractEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE.equals(databaseSchemaUpdate)) {
            AbstractEngineConfiguration abstractEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration().getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG);
            if (abstractEngineConfiguration != null) {
                IdmEngineConfiguration idmEngineConfiguration = (IdmEngineConfiguration) abstractEngineConfiguration;
                idmEngineConfiguration.getSchemaManager().schemaCreate();
            }
        }
    }

    @Override
    public void schemaCreate() {
        try {
            getCommonSchemaManager().schemaCreate();
            getIdentityLinkSchemaManager().schemaCreate();
            getEntityLinkSchemaManager().schemaCreate();
            getEventSubscriptionSchemaManager().schemaCreate();
            getTaskSchemaManager().schemaCreate();
            getVariableSchemaManager().schemaCreate();
            getJobSchemaManager().schemaCreate();
            getBatchSchemaManager().schemaCreate();

            super.schemaCreate();
        } catch (Exception e) {
            throw new FlowableException("Error creating CMMN engine tables", e);
        }
    }

    @Override
    public void schemaDrop() {
        try {
            super.schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping CMMN engine tables", e);
        }

        try {
            getBatchSchemaManager().schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping batch tables", e);
        }
        
        try {
            getJobSchemaManager().schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping job tables", e);
        }
          
        try {
            getVariableSchemaManager().schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping variable tables", e);
        }
        
        try {
            getTaskSchemaManager().schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping task tables", e);
        }
        
        try {
            getEventSubscriptionSchemaManager().schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping event subscription tables", e);
        }
        
        try {
            getEntityLinkSchemaManager().schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping entity link tables", e);
        }
        
        try {
            getIdentityLinkSchemaManager().schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping identity link tables", e);
        }
        
        try {
            getCommonSchemaManager().schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping common tables", e);
        }
    }

    @Override
    public String schemaUpdate() {
        try {
            
            getCommonSchemaManager().schemaUpdate();
            
            if (CommandContextUtil.getCmmnEngineConfiguration().isExecuteServiceSchemaManagers()) {
                getIdentityLinkSchemaManager().schemaUpdate();
                getEntityLinkSchemaManager().schemaUpdate();
                getEventSubscriptionSchemaManager().schemaUpdate();
                getTaskSchemaManager().schemaUpdate();
                getVariableSchemaManager().schemaUpdate();
                getJobSchemaManager().schemaUpdate();
                getBatchSchemaManager().schemaUpdate();
            }

            super.schemaUpdate();

        } catch (Exception e) {
            throw new FlowableException("Error updating CMMN engine tables", e);
        }
        return null;
    }
    
    protected SchemaManager getCommonSchemaManager() {
        return CommandContextUtil.getCmmnEngineConfiguration().getCommonSchemaManager();
    }
    
    protected SchemaManager getIdentityLinkSchemaManager() {
        return CommandContextUtil.getCmmnEngineConfiguration().getIdentityLinkSchemaManager();
    }
    
    protected SchemaManager getEntityLinkSchemaManager() {
        return CommandContextUtil.getCmmnEngineConfiguration().getEntityLinkSchemaManager();
    }
    
    protected SchemaManager getEventSubscriptionSchemaManager() {
        return CommandContextUtil.getCmmnEngineConfiguration().getEventSubscriptionSchemaManager();
    }
    
    protected SchemaManager getVariableSchemaManager() {
        return CommandContextUtil.getCmmnEngineConfiguration().getVariableSchemaManager();
    }
    
    protected SchemaManager getTaskSchemaManager() {
        return CommandContextUtil.getCmmnEngineConfiguration().getTaskSchemaManager();
    }
    
    protected SchemaManager getJobSchemaManager() {
        return CommandContextUtil.getCmmnEngineConfiguration().getJobSchemaManager();
    }

    protected SchemaManager getBatchSchemaManager() {
        return CommandContextUtil.getCmmnEngineConfiguration().getBatchSchemaManager();
    }
}
