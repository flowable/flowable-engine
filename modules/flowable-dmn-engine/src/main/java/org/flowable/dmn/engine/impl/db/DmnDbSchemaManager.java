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

import org.apache.commons.lang3.StringUtils;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.flowable.engine.common.api.FlowableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

public class DmnDbSchemaManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DmnDbSchemaManager.class);
    
    public static void initSchema() {
        initSchema(CommandContextUtil.getDmnEngineConfiguration());
    }
    
    public static void initSchema(DmnEngineConfiguration dmnEngineConfiguration) {
        initSchema(dmnEngineConfiguration, dmnEngineConfiguration.getDatabaseSchemaUpdate());
    }
    
    public static void initSchema(DmnEngineConfiguration dmnEngineConfiguration, String databaseSchemaUpdate) {
        try {
            DatabaseConnection connection = new JdbcConnection(dmnEngineConfiguration.getDataSource().getConnection());
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

            Liquibase liquibase = new Liquibase("org/flowable/dmn/db/liquibase/flowable-dmn-db-changelog.xml", new ClassLoaderResourceAccessor(), database);
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
            throw new FlowableException("Error initialising dmn data model");
        }
    }

}
