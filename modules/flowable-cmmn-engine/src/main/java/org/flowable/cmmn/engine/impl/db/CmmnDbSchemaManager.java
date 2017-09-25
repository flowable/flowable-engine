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

import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.db.DbSchemaManager;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

public class CmmnDbSchemaManager implements DbSchemaManager {

    public static final String LIQUIBASE_CHANGELOG = "org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml";
    
    public void initSchema() {
        initSchema(CommandContextUtil.getCmmnEngineConfiguration());
    }
    
    public void initSchema(CmmnEngineConfiguration cmmnEngineConfiguration) {
        initSchema(cmmnEngineConfiguration, cmmnEngineConfiguration.getDatabaseSchemaUpdate());
    }
    
    public void initSchema(CmmnEngineConfiguration cmmnEngineConfiguration, String databaseSchemaUpdate) {
        try {
            if (CmmnEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP.equals(databaseSchemaUpdate)) {
                dbSchemaCreate();
                
            } else if (CmmnEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE.equals(databaseSchemaUpdate)) {
                dbSchemaDrop();
                dbSchemaCreate();
                
            } else if (CmmnEngineConfiguration.DB_SCHEMA_UPDATE_TRUE.equals(databaseSchemaUpdate)) {
                dbSchemaUpdate();
                
            } else if (CmmnEngineConfiguration.DB_SCHEMA_UPDATE_FALSE.equals(databaseSchemaUpdate)) {
                Liquibase liquibase = createLiquibaseInstance(cmmnEngineConfiguration);
                liquibase.validate();
                
            }
        } catch (Exception e) {
            throw new FlowableException("Error initialising cmmn data model", e);
        }
    }

    protected Liquibase createLiquibaseInstance(CmmnEngineConfiguration cmmnEngineConfiguration)
            throws SQLException, DatabaseException, LiquibaseException {
        DatabaseConnection connection = new JdbcConnection(cmmnEngineConfiguration.getDataSource().getConnection());
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connection);
        database.setDatabaseChangeLogTableName(CmmnEngineConfiguration.LIQUIBASE_CHANGELOG_PREFIX + database.getDatabaseChangeLogTableName());
        database.setDatabaseChangeLogLockTableName(CmmnEngineConfiguration.LIQUIBASE_CHANGELOG_PREFIX + database.getDatabaseChangeLogLockTableName());

        String databaseSchema = cmmnEngineConfiguration.getDatabaseSchema();
        if (StringUtils.isNotEmpty(databaseSchema)) {
            database.setDefaultSchemaName(databaseSchema);
            database.setLiquibaseSchemaName(databaseSchema);
        }

        String databaseCatalog = cmmnEngineConfiguration.getDatabaseCatalog();
        if (StringUtils.isNotEmpty(databaseCatalog)) {
            database.setDefaultCatalogName(databaseCatalog);
            database.setLiquibaseCatalogName(databaseCatalog);
        }

        return createLiquibaseInstance(database);
    }

    public Liquibase createLiquibaseInstance(Database database) throws LiquibaseException {
        return new Liquibase(LIQUIBASE_CHANGELOG, new ClassLoaderResourceAccessor(), database);
    }

    @Override
    public void dbSchemaCreate() {
        try {
            
            getCommonDbSchemaManager().dbSchemaCreate();
            getVariableDbSchemaManager().dbSchemaCreate();
            
            Liquibase liquibase = createLiquibaseInstance(CommandContextUtil.getCmmnEngineConfiguration());
            liquibase.update("cmmn");
        } catch (Exception e) {
            throw new FlowableException("Error creating CMMN engine tables", e);
        }
    }

    @Override
    public void dbSchemaDrop() {
        try {
            Liquibase liquibase = createLiquibaseInstance(CommandContextUtil.getCmmnEngineConfiguration());
            liquibase.dropAll();
            
            getVariableDbSchemaManager().dbSchemaDrop();
            getCommonDbSchemaManager().dbSchemaDrop();
        } catch (Exception e) {
            throw new FlowableException("Error dropping CMMN engine tables", e);
        }
    }

    @Override
    public String dbSchemaUpdate() {
        try {
            
            getCommonDbSchemaManager().dbSchemaUpdate();
            getVariableDbSchemaManager().dbSchemaUpdate();
            
            Liquibase liquibase = createLiquibaseInstance(CommandContextUtil.getCmmnEngineConfiguration());
            liquibase.update("cmmn");
        } catch (Exception e) {
            throw new FlowableException("Error updating CMMN engine tables", e);
        }
        return null;
    }
    
    protected DbSchemaManager getCommonDbSchemaManager() {
        return CommandContextUtil.getCmmnEngineConfiguration().getCommonDbSchemaManager();
    }
    
    protected DbSchemaManager getVariableDbSchemaManager() {
        return CommandContextUtil.getCmmnEngineConfiguration().getVariableDbSchemaManager();
    }
    
    
}
