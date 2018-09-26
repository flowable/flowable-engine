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

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.engine.impl.util.CommandContextUtil;
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

public class AppDbSchemaManager implements SchemaManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AppDbSchemaManager.class);

    public static final String LIQUIBASE_CHANGELOG = "org/flowable/app/db/liquibase/flowable-app-db-changelog.xml";
    
    public void initSchema() {
        initSchema(CommandContextUtil.getAppEngineConfiguration());
    }
    
    public void initSchema(AppEngineConfiguration appEngineConfiguration) {
        initSchema(appEngineConfiguration, appEngineConfiguration.getDatabaseSchemaUpdate());
    }
    
    public void initSchema(AppEngineConfiguration appEngineConfiguration, String databaseSchemaUpdate) {
        Liquibase liquibase = null;
        try {
            if (AppEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP.equals(databaseSchemaUpdate)) {
                schemaCreate();
                
            } else if (AppEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE.equals(databaseSchemaUpdate)) {
                schemaDrop();
                schemaCreate();
                
            } else if (AppEngineConfiguration.DB_SCHEMA_UPDATE_TRUE.equals(databaseSchemaUpdate)) {
                schemaUpdate();
                
            } else if (AppEngineConfiguration.DB_SCHEMA_UPDATE_FALSE.equals(databaseSchemaUpdate)) {
               schemaCheckVersion();
                
            }
        } catch (Exception e) {
            throw new FlowableException("Error initialising app data model", e);
        } finally {
            closeDatabase(liquibase);
        }
    }

    protected Liquibase createLiquibaseInstance(AppEngineConfiguration appEngineConfiguration)
            throws SQLException, DatabaseException, LiquibaseException {
        
        // If a command context is currently active, the current connection needs to be reused.
        Connection jdbcConnection = null;
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        if (commandContext == null) {
            jdbcConnection = appEngineConfiguration.getDataSource().getConnection();
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
        database.setDatabaseChangeLogTableName(AppEngineConfiguration.LIQUIBASE_CHANGELOG_PREFIX + database.getDatabaseChangeLogTableName());
        database.setDatabaseChangeLogLockTableName(AppEngineConfiguration.LIQUIBASE_CHANGELOG_PREFIX + database.getDatabaseChangeLogLockTableName());

        String databaseSchema = appEngineConfiguration.getDatabaseSchema();
        if (StringUtils.isNotEmpty(databaseSchema)) {
            database.setDefaultSchemaName(databaseSchema);
            database.setLiquibaseSchemaName(databaseSchema);
        }

        String databaseCatalog = appEngineConfiguration.getDatabaseCatalog();
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
            
            getCommonSchemaManager().schemaCreate();
            getIdentityLinkSchemaManager().schemaCreate();
            getVariableSchemaManager().schemaCreate();
            
            liquibase = createLiquibaseInstance(CommandContextUtil.getAppEngineConfiguration());
            liquibase.update("app");
        } catch (Exception e) {
            throw new FlowableException("Error creating App engine tables", e);
        } finally {
            closeDatabase(liquibase);
        }
    }

    @Override
    public void schemaDrop() {
        Liquibase liquibase = null;
        try {
            liquibase = createLiquibaseInstance(CommandContextUtil.getAppEngineConfiguration());
            liquibase.dropAll();
        } catch (Exception e) {
            LOGGER.info("Error dropping App engine tables", e);
        } finally {
            closeDatabase(liquibase);
        }
        
        try {
            getVariableSchemaManager().schemaDrop();
        } catch (Exception e) {
            LOGGER.info("Error dropping variable tables", e);
        }
        
        try {
            getIdentityLinkSchemaManager().schemaDrop();
        } catch (Exception e) {
            LOGGER.info("Error dropping identity link tables", e);
        }
        
        try {
            getCommonSchemaManager().schemaDrop();
        } catch (Exception e) {
            LOGGER.info("Error dropping common tables", e);
        }
    }

    @Override
    public String schemaUpdate() {
        Liquibase liquibase = null;
        try {
            
            getCommonSchemaManager().schemaUpdate();
            
            if (CommandContextUtil.getAppEngineConfiguration().isExecuteServiceSchemaManagers()) {
                getIdentityLinkSchemaManager().schemaUpdate();
                getVariableSchemaManager().schemaUpdate();
            }
            
            liquibase = createLiquibaseInstance(CommandContextUtil.getAppEngineConfiguration());
            liquibase.update("cmmn");

        } catch (Exception e) {
            throw new FlowableException("Error updating App engine tables", e);
        } finally {
            closeDatabase(liquibase);
        }
        return null;
    }
    
    @Override
    public void schemaCheckVersion() {
        Liquibase liquibase = null;
        try {
            liquibase = createLiquibaseInstance(CommandContextUtil.getAppEngineConfiguration());
            liquibase.validate();
        } catch (Exception e) {
            throw new FlowableException("Error validating app engine schema", e);
        } finally {
            closeDatabase(liquibase);
        }
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
