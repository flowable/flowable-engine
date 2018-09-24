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
package org.flowable.content.engine.impl.db;

import java.sql.Connection;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.db.SchemaManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.content.engine.ContentEngineConfiguration;
import org.flowable.content.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

public class ContentDbSchemaManager implements SchemaManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentDbSchemaManager.class);
    
    public static String LIQUIBASE_CHANGELOG = "org/flowable/content/db/liquibase/flowable-content-db-changelog.xml";
    
    @Override
    public void schemaCreate() {
        Liquibase liquibase = createLiquibaseInstance(CommandContextUtil.getContentEngineConfiguration());
        try {
            liquibase.update("content");
        } catch (Exception e) {
            throw new FlowableException("Error creating content engine tables", e);
        } finally {
            closeDatabase(liquibase);
        }
    }

    @Override
    public void schemaDrop() {
        Liquibase liquibase = createLiquibaseInstance(CommandContextUtil.getContentEngineConfiguration());
        try {
            liquibase.dropAll();
        } catch (Exception e) {
            throw new FlowableException("Error dropping content engine tables", e);
        } finally {
            closeDatabase(liquibase);
        }
    }
    
    @Override
    public String schemaUpdate() {
        schemaCreate();
        return null;
    }

    protected static Liquibase createLiquibaseInstance(ContentEngineConfiguration configuration) {
        try {
            
            Connection jdbcConnection = null;
            CommandContext commandContext = CommandContextUtil.getCommandContext();
            if (commandContext == null) {
                jdbcConnection = configuration.getDataSource().getConnection();
            } else {
                jdbcConnection = CommandContextUtil.getDbSqlSession(commandContext).getSqlSession().getConnection();
            }
            if (!jdbcConnection.getAutoCommit()) {
                jdbcConnection.commit();
            }
            
            DatabaseConnection connection = new JdbcConnection(jdbcConnection);
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connection);
            database.setDatabaseChangeLogTableName(ContentEngineConfiguration.LIQUIBASE_CHANGELOG_PREFIX + database.getDatabaseChangeLogTableName());
            database.setDatabaseChangeLogLockTableName(ContentEngineConfiguration.LIQUIBASE_CHANGELOG_PREFIX + database.getDatabaseChangeLogLockTableName());

            Liquibase liquibase = new Liquibase(LIQUIBASE_CHANGELOG, new ClassLoaderResourceAccessor(), database);
            return liquibase;

        } catch (Exception e) {
            throw new FlowableException("Error creating liquibase instance", e);
        }
    }
    
    public void initSchema(ContentEngineConfiguration configuration, String databaseSchemaUpdate) {
        Liquibase liquibase = null;
        try {
            liquibase = createLiquibaseInstance(configuration);
            if (ContentEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE.equals(databaseSchemaUpdate)) {
                LOGGER.debug("Dropping and creating schema Content");
                liquibase.dropAll();
                liquibase.update("content");
            } else if (ContentEngineConfiguration.DB_SCHEMA_UPDATE_TRUE.equals(databaseSchemaUpdate)) {
                LOGGER.debug("Updating schema Content");
                liquibase.update("content");
            } else if (ContentEngineConfiguration.DB_SCHEMA_UPDATE_FALSE.equals(databaseSchemaUpdate)) {
                LOGGER.debug("Validating schema Content");
                liquibase.validate();
            }
        } catch (Exception e) {
            throw new FlowableException("Error initialising Content schema", e);
        } finally {
            closeDatabase(liquibase);
        }
    }
    
    @Override
    public void schemaCheckVersion() {
        Liquibase liquibase = null;
        try {
            liquibase = createLiquibaseInstance(CommandContextUtil.getContentEngineConfiguration());
            liquibase.validate();
        } catch (Exception e) {
            throw new FlowableException("Error validating app engine schema", e);
        } finally {
            closeDatabase(liquibase);
        }
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
