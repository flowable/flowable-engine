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
package org.flowable.common.engine.impl.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.ui.LoggerUIService;

/**
 * @author Filip Hrisafov
 */
public abstract class LiquibaseBasedSchemaManager implements SchemaManager {

    private static final String LIQUIBASE_HUB_SERVICE_CLASS_NAME = "liquibase.hub.HubService";
    protected static final Map<String, Object> LIQUIBASE_SCOPE_VALUES = new HashMap<>();

    static {
        if (ClassUtils.isPresent(LIQUIBASE_HUB_SERVICE_CLASS_NAME, null)) {
            LIQUIBASE_SCOPE_VALUES.put("liquibase.plugin." + LIQUIBASE_HUB_SERVICE_CLASS_NAME, FlowableLiquibaseHubService.class);
            LoggerUIService uiService = new LoggerUIService();
            uiService.setStandardLogLevel(Level.FINE);
            LIQUIBASE_SCOPE_VALUES.put(Scope.Attr.ui.name(), uiService);
        }
    }

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final String context;
    protected final String changeLogFile;
    protected final String changeLogPrefix;

    public LiquibaseBasedSchemaManager(String context, String changeLogFile, String changeLogPrefix) {
        this.context = context;
        this.changeLogFile = changeLogFile;
        this.changeLogPrefix = changeLogPrefix;
    }

    public void initSchema(String databaseSchemaUpdate) {
        try {
            if (AbstractEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP.equals(databaseSchemaUpdate)) {
                runForLiquibase(this::schemaCreate);

            } else if (AbstractEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE.equals(databaseSchemaUpdate)) {
                runForLiquibase(() -> {
                    schemaDrop();
                    schemaCreate();
                });

            } else if (AbstractEngineConfiguration.DB_SCHEMA_UPDATE_TRUE.equals(databaseSchemaUpdate)) {
                runForLiquibase(this::schemaUpdate);

            } else if (AbstractEngineConfiguration.DB_SCHEMA_UPDATE_FALSE.equals(databaseSchemaUpdate)) {
                runForLiquibase(this::schemaCheckVersion);

            }
        } catch (Exception e) {
            throw new FlowableException("Error initialising " + context + " data model", e);
        }
    }

    protected void runForLiquibase(Runnable runnable) throws Exception {
        Scope.child(LIQUIBASE_SCOPE_VALUES, runnable::run);
    }

    @Override
    public void schemaCreate() {
        Liquibase liquibase = null;
        try {
            liquibase = createLiquibaseInstance(getDatabaseConfiguration());
            liquibase.update(context);
        } catch (Exception e) {
            throw new FlowableException("Error creating " + context + " engine tables", e);
        } finally {
            closeDatabase(liquibase);
        }
    }

    @Override
    public void schemaDrop() {
        Liquibase liquibase = null;
        try {
            liquibase = createLiquibaseInstance(getDatabaseConfiguration());
            liquibase.dropAll();
        } catch (Exception e) {
            throw new FlowableException("Error dropping " + context + " engine tables", e);
        } finally {
            closeDatabase(liquibase);
        }
    }

    @Override
    public String schemaUpdate() {
        Liquibase liquibase = null;
        try {
            liquibase = createLiquibaseInstance(getDatabaseConfiguration());
            liquibase.update(context);
        } catch (Exception e) {
            throw new FlowableException("Error updating " + context + " engine tables", e);
        } finally {
            closeDatabase(liquibase);
        }
        return null;
    }

    @Override
    public void schemaCheckVersion() {
        Liquibase liquibase = null;
        try {
            liquibase = createLiquibaseInstance(getDatabaseConfiguration());
            liquibase.validate();
        } catch (Exception e) {
            throw new FlowableException("Error validating " + context + " engine schema", e);
        } finally {
            closeDatabase(liquibase);
        }
    }

    protected abstract LiquibaseDatabaseConfiguration getDatabaseConfiguration();

    protected Liquibase createLiquibaseInstance(LiquibaseDatabaseConfiguration databaseConfiguration) throws SQLException {
        Connection jdbcConnection = null;
        boolean closeConnection = false;
        try {
            CommandContext commandContext = Context.getCommandContext();
            if (commandContext == null) {
                jdbcConnection = databaseConfiguration.getDataSource().getConnection();
                closeConnection = true;
            } else {
                jdbcConnection = commandContext.getSession(DbSqlSession.class).getSqlSession().getConnection();
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
            database.setDatabaseChangeLogTableName(changeLogPrefix + database.getDatabaseChangeLogTableName());
            database.setDatabaseChangeLogLockTableName(changeLogPrefix + database.getDatabaseChangeLogLockTableName());

            String databaseSchema = databaseConfiguration.getDatabaseSchema();
            if (StringUtils.isNotEmpty(databaseSchema)) {
                database.setDefaultSchemaName(databaseSchema);
                database.setLiquibaseSchemaName(databaseSchema);
            }

            String databaseCatalog = databaseConfiguration.getDatabaseCatalog();
            if (StringUtils.isNotEmpty(databaseCatalog)) {
                database.setDefaultCatalogName(databaseCatalog);
                database.setLiquibaseCatalogName(databaseCatalog);
            }

            return new Liquibase(changeLogFile, new ClassLoaderResourceAccessor(), database);

        } catch (Exception e) {
            // We only close the connection if an exception occurred, otherwise the Liquibase instance cannot be used
            if (jdbcConnection != null && closeConnection) {
                jdbcConnection.close();
            }
            throw new FlowableException("Error creating " + context + " liquibase instance", e);
        }
    }

    protected void closeDatabase(Liquibase liquibase) {
        if (liquibase != null) {
            Database database = liquibase.getDatabase();
            if (database != null) {
                // do not close the shared connection if a command context is currently active
                if (Context.getCommandContext() == null) {
                    try {
                        database.close();
                    } catch (DatabaseException e) {
                        logger.warn("Error closing database for {}", context, e);
                    }
                }
            }
        }
    }

}
