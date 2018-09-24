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

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.db.SchemaManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

public class CmmnDbSchemaManager implements SchemaManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CmmnDbSchemaManager.class);

    public static final String LIQUIBASE_CHANGELOG = "org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml";
    
    public void initSchema() {
        initSchema(CommandContextUtil.getCmmnEngineConfiguration());
    }
    
    public void initSchema(CmmnEngineConfiguration cmmnEngineConfiguration) {
        initSchema(cmmnEngineConfiguration, cmmnEngineConfiguration.getDatabaseSchemaUpdate());
    }
    
    public void initSchema(CmmnEngineConfiguration cmmnEngineConfiguration, String databaseSchemaUpdate) {
        Liquibase liquibase = null;
        try {
            if (CmmnEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP.equals(databaseSchemaUpdate)) {
                schemaCreate();
                
            } else if (CmmnEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE.equals(databaseSchemaUpdate)) {
                schemaDrop();
                schemaCreate();
                
            } else if (CmmnEngineConfiguration.DB_SCHEMA_UPDATE_TRUE.equals(databaseSchemaUpdate)) {
                schemaUpdate();
                
            } else if (CmmnEngineConfiguration.DB_SCHEMA_UPDATE_FALSE.equals(databaseSchemaUpdate)) {
               schemaCheckVersion();
                
            }
        } catch (Exception e) {
            throw new FlowableException("Error initialising cmmn data model", e);
        } finally {
            closeDatabase(liquibase);
        }
    }

    protected Liquibase createLiquibaseInstance(CmmnEngineConfiguration cmmnEngineConfiguration)
            throws SQLException, DatabaseException, LiquibaseException {
        
        // If a command context is currently active, the current connection needs to be reused.
        Connection jdbcConnection = null;
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        if (commandContext == null) {
            jdbcConnection = cmmnEngineConfiguration.getDataSource().getConnection();
        } else {
            jdbcConnection = CommandContextUtil.getDbSqlSession(commandContext).getSqlSession().getConnection();
        }
        
        // A commit is needed here, because one of the things that Liquibase does when acquiring its lock
        // is doing a rollback, which removes all changes done so far. 
        // For most databases, this is not a problem as DDL statements are not transactional.
        // However for some (e.g. sql server), this would remove all previous statements, which is not wanted,
        // hence the extra commit here.
        if (!jdbcConnection.getAutoCommit()) {
            jdbcConnection.commit();
        }
        
        DatabaseConnection connection = new JdbcConnection(jdbcConnection);
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
    public void schemaCreate() {
        Liquibase liquibase = null;
        try {
            
            getCommonDbSchemaManager().schemaCreate();
            getIdentityLinkDbSchemaManager().schemaCreate();
            getTaskDbSchemaManager().schemaCreate();
            getVariableDbSchemaManager().schemaCreate();
            getJobDbSchemaManager().schemaCreate();
            
            liquibase = createLiquibaseInstance(CommandContextUtil.getCmmnEngineConfiguration());
            liquibase.update("cmmn");
        } catch (Exception e) {
            throw new FlowableException("Error creating CMMN engine tables", e);
        } finally {
            closeDatabase(liquibase);
        }
    }

    @Override
    public void schemaDrop() {
        Liquibase liquibase = null;
        try {
            liquibase = createLiquibaseInstance(CommandContextUtil.getCmmnEngineConfiguration());
            liquibase.dropAll();
        } catch (Exception e) {
            LOGGER.info("Error dropping CMMN engine tables", e);
        } finally {
            closeDatabase(liquibase);
        }
        
        try {
            getJobDbSchemaManager().schemaDrop();
        } catch (Exception e) {
            LOGGER.info("Error dropping job tables", e);
        }
          
        try {
            getVariableDbSchemaManager().schemaDrop();
        } catch (Exception e) {
            LOGGER.info("Error dropping variable tables", e);
        }
        
        try {
            getTaskDbSchemaManager().schemaDrop();
        } catch (Exception e) {
            LOGGER.info("Error dropping task tables", e);
        }
        
        try {
            getIdentityLinkDbSchemaManager().schemaDrop();
        } catch (Exception e) {
            LOGGER.info("Error dropping identity link tables", e);
        }
        
        try {
            getCommonDbSchemaManager().schemaDrop();
        } catch (Exception e) {
            LOGGER.info("Error dropping common tables", e);
        }
    }

    @Override
    public String schemaUpdate() {
        Liquibase liquibase = null;
        try {
            
            getCommonDbSchemaManager().schemaUpdate();
            
            if (CommandContextUtil.getCmmnEngineConfiguration().isExecuteServiceDbSchemaManagers()) {
                getIdentityLinkDbSchemaManager().schemaUpdate();
                getTaskDbSchemaManager().schemaUpdate();
                getVariableDbSchemaManager().schemaUpdate();
                getJobDbSchemaManager().schemaUpdate();
            }
            
            liquibase = createLiquibaseInstance(CommandContextUtil.getCmmnEngineConfiguration());
            liquibase.update("cmmn");

        } catch (Exception e) {
            throw new FlowableException("Error updating CMMN engine tables", e);
        } finally {
            closeDatabase(liquibase);
        }
        return null;
    }
    
    @Override
    public void schemaCheckVersion() {
        Liquibase liquibase = null;
        try {
            liquibase = createLiquibaseInstance(CommandContextUtil.getCmmnEngineConfiguration());
            liquibase.validate();
        } catch (Exception e) {
            throw new FlowableException("Error validating app engine schema", e);
        } finally {
            closeDatabase(liquibase);
        }
    }
    
    protected SchemaManager getCommonDbSchemaManager() {
        return CommandContextUtil.getCmmnEngineConfiguration().getCommonDbSchemaManager();
    }
    
    protected SchemaManager getIdentityLinkDbSchemaManager() {
        return CommandContextUtil.getCmmnEngineConfiguration().getIdentityLinkDbSchemaManager();
    }
    
    protected SchemaManager getVariableDbSchemaManager() {
        return CommandContextUtil.getCmmnEngineConfiguration().getVariableDbSchemaManager();
    }
    
    protected SchemaManager getTaskDbSchemaManager() {
        return CommandContextUtil.getCmmnEngineConfiguration().getTaskDbSchemaManager();
    }
    
    protected SchemaManager getJobDbSchemaManager() {
        return CommandContextUtil.getCmmnEngineConfiguration().getJobDbSchemaManager();
    }
    
    private void closeDatabase(Liquibase liquibase) {
        if (liquibase != null) {
            Database database = liquibase.getDatabase();
            if (database != null) {
                // do not close the shared connection if a command context is currently active
                if (CommandContextUtil.getCommandContext() == null) {
                    try {
                        database.close();
                    } catch (DatabaseException e) {
                        LOGGER.warn("Error closing database", e);
                    }
                }
            }
        }
    }
}
