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
package org.flowable.form.engine.impl.db;

import java.sql.Connection;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.db.SchemaManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

public class FormDbSchemaManager implements SchemaManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FormDbSchemaManager.class);
    
    public static String LIQUIBASE_CHANGELOG = "org/flowable/form/db/liquibase/flowable-form-db-changelog.xml";
    
    public void initSchema(FormEngineConfiguration formEngineConfiguration) {
        Liquibase liquibase = null;
        try {
            liquibase = createLiquibaseInstance(formEngineConfiguration);

            String databaseSchemaUpdate = formEngineConfiguration.getDatabaseSchemaUpdate();
            if (FormEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE.equals(databaseSchemaUpdate)) {
                LOGGER.debug("Dropping and creating schema FORM");
                liquibase.dropAll();
                liquibase.update("form");
            } else if (FormEngineConfiguration.DB_SCHEMA_UPDATE_TRUE.equals(databaseSchemaUpdate)) {
                LOGGER.debug("Updating schema FORM");
                liquibase.update("form");
            } else if (FormEngineConfiguration.DB_SCHEMA_UPDATE_FALSE.equals(databaseSchemaUpdate)) {
                LOGGER.debug("Validating schema FORM");
                liquibase.validate();
            }
        } catch (Exception e) {
            throw new FlowableException("Error initialising form data schema", e);
        } finally {
            closeDatabase(liquibase);
        }
    }
    
    @Override
    public void schemaCreate() {
        Liquibase liquibase = createLiquibaseInstance();
        try {
            liquibase.update("form");
        } catch (Exception e) {
            throw new FlowableException("Error creating form engine tables", e);
        } finally {
            closeDatabase(liquibase);
        }
    }

    @Override
    public void schemaDrop() {
        Liquibase liquibase = createLiquibaseInstance();
        try {
            liquibase.dropAll();
        } catch (Exception e) {
            throw new FlowableException("Error dropping form engine tables", e);
        } finally {
            closeDatabase(liquibase);
        }
    }
    
    @Override
    public String schemaUpdate() {
        schemaCreate();
        return null;
    }
    
    @Override
    public void schemaCheckVersion() {
        Liquibase liquibase = null;
        try {
            liquibase = createLiquibaseInstance(CommandContextUtil.getFormEngineConfiguration());
            liquibase.validate();
        } catch (Exception e) {
            throw new FlowableException("Error validating app engine schema", e);
        } finally {
            closeDatabase(liquibase);
        }
    }

    protected static Liquibase createLiquibaseInstance() {
        return createLiquibaseInstance(CommandContextUtil.getFormEngineConfiguration());
    }
    
    protected static Liquibase createLiquibaseInstance(FormEngineConfiguration formEngineConfiguration) {
        try {
            Connection jdbcConnection = null;
            CommandContext commandContext = CommandContextUtil.getCommandContext();
            if (commandContext == null) {
                jdbcConnection = formEngineConfiguration.getDataSource().getConnection();
            } else {
                jdbcConnection = CommandContextUtil.getDbSqlSession(commandContext).getSqlSession().getConnection();
            }
            if (!jdbcConnection.getAutoCommit()) {
                jdbcConnection.commit();
            }
            
            DatabaseConnection connection = new JdbcConnection(jdbcConnection);
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connection);
            database.setDatabaseChangeLogTableName(FormEngineConfiguration.LIQUIBASE_CHANGELOG_PREFIX + database.getDatabaseChangeLogTableName());
            database.setDatabaseChangeLogLockTableName(FormEngineConfiguration.LIQUIBASE_CHANGELOG_PREFIX + database.getDatabaseChangeLogLockTableName());

            if (StringUtils.isNotEmpty(formEngineConfiguration.getDatabaseSchema())) {
                database.setDefaultSchemaName(formEngineConfiguration.getDatabaseSchema());
                database.setLiquibaseSchemaName(formEngineConfiguration.getDatabaseSchema());
            }

            if (StringUtils.isNotEmpty(formEngineConfiguration.getDatabaseCatalog())) {
                database.setDefaultCatalogName(formEngineConfiguration.getDatabaseCatalog());
                database.setLiquibaseCatalogName(formEngineConfiguration.getDatabaseCatalog());
            }

            Liquibase liquibase = new Liquibase(LIQUIBASE_CHANGELOG, new ClassLoaderResourceAccessor(), database);
            return liquibase;

        } catch (Exception e) {
            throw new FlowableException("Error creating liquibase instance", e);
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
