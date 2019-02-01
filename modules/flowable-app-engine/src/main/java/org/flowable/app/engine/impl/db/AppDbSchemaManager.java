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
package org.flowable.app.engine.impl.db;

import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.db.EngineDatabaseConfiguration;
import org.flowable.common.engine.impl.db.LiquibaseBasedSchemaManager;
import org.flowable.common.engine.impl.db.LiquibaseDatabaseConfiguration;
import org.flowable.common.engine.impl.db.SchemaManager;

public class AppDbSchemaManager extends LiquibaseBasedSchemaManager {
    
    public static final String LIQUIBASE_CHANGELOG = "org/flowable/app/db/liquibase/flowable-app-db-changelog.xml";

    public AppDbSchemaManager() {
        super("app", LIQUIBASE_CHANGELOG, AppEngineConfiguration.LIQUIBASE_CHANGELOG_PREFIX);
    }

    @Override
    protected LiquibaseDatabaseConfiguration getDatabaseConfiguration() {
        return new EngineDatabaseConfiguration(CommandContextUtil.getAppEngineConfiguration());
    }

    public void initSchema() {
        initSchema(CommandContextUtil.getAppEngineConfiguration().getDatabaseSchemaUpdate());
    }
    
    @Override
    public void schemaCreate() {
        try {
            
            getCommonSchemaManager().schemaCreate();
            getIdentityLinkSchemaManager().schemaCreate();
            getVariableSchemaManager().schemaCreate();
            
            super.schemaCreate();
        } catch (Exception e) {
            throw new FlowableException("Error creating App engine tables", e);
        }
    }

    @Override
    public void schemaDrop() {
        try {
            super.schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping App engine tables", e);
        }
        
        try {
            getVariableSchemaManager().schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping variable tables", e);
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
            
            if (CommandContextUtil.getAppEngineConfiguration().isExecuteServiceSchemaManagers()) {
                getIdentityLinkSchemaManager().schemaUpdate();
                getVariableSchemaManager().schemaUpdate();
            }

            super.schemaUpdate();

        } catch (Exception e) {
            throw new FlowableException("Error updating App engine tables", e);
        }
        return null;
    }
    
    protected SchemaManager getCommonSchemaManager() {
        return CommandContextUtil.getAppEngineConfiguration().getCommonSchemaManager();
    }
    
    protected SchemaManager getIdentityLinkSchemaManager() {
        return CommandContextUtil.getAppEngineConfiguration().getIdentityLinkSchemaManager();
    }
    
    protected SchemaManager getVariableSchemaManager() {
        return CommandContextUtil.getAppEngineConfiguration().getVariableSchemaManager();
    }
}
