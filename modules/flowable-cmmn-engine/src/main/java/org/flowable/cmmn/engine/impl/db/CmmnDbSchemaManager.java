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

public class CmmnDbSchemaManager implements DbSchemaManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmmnDbSchemaManager.class);

    public static final String LIQUIBASE_CHANGELOG = "org/flowable/cmmn/db/liquibase/flowable-cmmn-db-changelog.xml";
    
    public void initSchema() {
        initSchema(CommandContextUtil.getCmmnEngineConfiguration());
    }
    
    public void initSchema(CmmnEngineConfiguration cmmnEngineConfiguration) {
        initSchema(cmmnEngineConfiguration, cmmnEngineConfiguration.getDatabaseSchemaUpdate());
    }
    
    public void initSchema(CmmnEngineConfiguration cmmnEngineConfiguration, String databaseSchemaUpdate) {
        try {
            Liquibase liquibase = createLiquibaseInstance(cmmnEngineConfiguration);
            if (CmmnEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP.equals(databaseSchemaUpdate)) {
                LOGGER.debug("Creating CMMN schema");
                liquibase.update("cmmn");
            }
            if (CmmnEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE.equals(databaseSchemaUpdate)) {
                LOGGER.debug("Dropping and creating schema CMMN");
                liquibase.dropAll();
                liquibase.update("cmmn");
            } else if (CmmnEngineConfiguration.DB_SCHEMA_UPDATE_TRUE.equals(databaseSchemaUpdate)) {
                LOGGER.debug("Updating schema CMMN");
                liquibase.update("cmmn");
            } else if (CmmnEngineConfiguration.DB_SCHEMA_UPDATE_FALSE.equals(databaseSchemaUpdate)) {
                LOGGER.debug("Validating schema CMMN");
                liquibase.validate();
            }
        } catch (Exception e) {
            throw new FlowableException("Error initialising cmmn data model");
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
        } catch (Exception e) {
            throw new FlowableException("Error dropping CMMN engine tables", e);
        }
    }

    @Override
    public String dbSchemaUpdate() {
        dbSchemaCreate();
        return null;
    }
    
}
