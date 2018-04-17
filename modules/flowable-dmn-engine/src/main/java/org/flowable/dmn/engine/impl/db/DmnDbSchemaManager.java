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

package org.flowable.dmn.engine.impl.db;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.db.DbSchemaManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
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

public class DmnDbSchemaManager implements DbSchemaManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DmnDbSchemaManager.class);
    
    public static String LIQUIBASE_CHANGELOG = "org/flowable/dmn/db/liquibase/flowable-dmn-db-changelog.xml";
    
    public void initSchema() {
        initSchema(CommandContextUtil.getDmnEngineConfiguration());
    }
    
    public void initSchema(DmnEngineConfiguration dmnEngineConfiguration) {
        initSchema(dmnEngineConfiguration, dmnEngineConfiguration.getDatabaseSchemaUpdate());
    }
    
    public void initSchema(DmnEngineConfiguration dmnEngineConfiguration, String databaseSchemaUpdate) {
        try {
            Liquibase liquibase = createLiquibaseInstance(dmnEngineConfiguration);
            if (DmnEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE.equals(databaseSchemaUpdate)) {
                LOGGER.debug("Dropping and creating schema DMN");
                liquibase.dropAll();
                liquibase.update("dmn");
            } else if (DmnEngineConfiguration.DB_SCHEMA_UPDATE_TRUE.equals(databaseSchemaUpdate)) {
                LOGGER.debug("Updating schema DMN");
                liquibase.update("dmn");
            } else if (DmnEngineConfiguration.DB_SCHEMA_UPDATE_FALSE.equals(databaseSchemaUpdate)) {
                LOGGER.debug("Validating schema DMN");
                liquibase.validate();
            }
        } catch (Exception e) {
            throw new FlowableException("Error initialising dmn data model", e);
        }
    }

    public Liquibase createLiquibaseInstance(DmnEngineConfiguration dmnEngineConfiguration)
            throws SQLException, DatabaseException, LiquibaseException {
        
        Connection jdbcConnection = null;
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        if (commandContext == null) {
            jdbcConnection = dmnEngineConfiguration.getDataSource().getConnection();
        } else {
            jdbcConnection = CommandContextUtil.getDbSqlSession(commandContext).getSqlSession().getConnection();
        }
        
        if (!jdbcConnection.getAutoCommit()) {
            jdbcConnection.commit();
        }
        
        DatabaseConnection connection = new JdbcConnection(jdbcConnection);
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connection);
        database.setDatabaseChangeLogTableName(DmnEngineConfiguration.LIQUIBASE_CHANGELOG_PREFIX + database.getDatabaseChangeLogTableName());
        database.setDatabaseChangeLogLockTableName(DmnEngineConfiguration.LIQUIBASE_CHANGELOG_PREFIX + database.getDatabaseChangeLogLockTableName());

        String databaseSchema = dmnEngineConfiguration.getDatabaseSchema();
        if (StringUtils.isNotEmpty(databaseSchema)) {
            database.setDefaultSchemaName(databaseSchema);
            database.setLiquibaseSchemaName(databaseSchema);
        }

        String databaseCatalog = dmnEngineConfiguration.getDatabaseCatalog();
        if (StringUtils.isNotEmpty(databaseCatalog)) {
            database.setDefaultCatalogName(databaseCatalog);
            database.setLiquibaseCatalogName(databaseCatalog);
        }

        Liquibase liquibase = new Liquibase(LIQUIBASE_CHANGELOG, new ClassLoaderResourceAccessor(), database);
        return liquibase;
    }
    
    @Override
    public void dbSchemaCreate() {
        try {
            Liquibase liquibase = createLiquibaseInstance(CommandContextUtil.getDmnEngineConfiguration());
            liquibase.update("dmn");
        } catch (Exception e) {
            throw new FlowableException("Error creating DMN engine tables", e);
        }
    }

    @Override
    public void dbSchemaDrop() {
        try {
            Liquibase liquibase = createLiquibaseInstance(CommandContextUtil.getDmnEngineConfiguration());
            liquibase.dropAll();
        } catch (Exception e) {
            throw new FlowableException("Error dropping DMN engine tables", e);
        }
    }

    @Override
    public String dbSchemaUpdate() {
        dbSchemaCreate();
        return null;
    }

}
