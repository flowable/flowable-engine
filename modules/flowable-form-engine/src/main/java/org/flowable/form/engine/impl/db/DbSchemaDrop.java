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

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.FormEngines;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * @author Tijs Rademakers
 */
public class DbSchemaDrop {

    public static void main(String[] args) {
        try {
            FormEngine formEngine = FormEngines.getDefaultFormEngine();
            DataSource dataSource = formEngine.getFormEngineConfiguration().getDataSource();

            DatabaseConnection connection = new JdbcConnection(dataSource.getConnection());
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connection);
            database.setDatabaseChangeLogTableName(FormEngineConfiguration.LIQUIBASE_CHANGELOG_PREFIX + database.getDatabaseChangeLogTableName());
            database.setDatabaseChangeLogLockTableName(FormEngineConfiguration.LIQUIBASE_CHANGELOG_PREFIX + database.getDatabaseChangeLogLockTableName());

            if (StringUtils.isNotEmpty(formEngine.getFormEngineConfiguration().getDatabaseSchema())) {
                database.setDefaultSchemaName(formEngine.getFormEngineConfiguration().getDatabaseSchema());
                database.setLiquibaseSchemaName(formEngine.getFormEngineConfiguration().getDatabaseSchema());
            }

            if (StringUtils.isNotEmpty(formEngine.getFormEngineConfiguration().getDatabaseCatalog())) {
                database.setDefaultCatalogName(formEngine.getFormEngineConfiguration().getDatabaseCatalog());
                database.setLiquibaseCatalogName(formEngine.getFormEngineConfiguration().getDatabaseCatalog());
            }

            Liquibase liquibase = new Liquibase("org/flowable/form/db/liquibase/flowable-form-db-changelog.xml", new ClassLoaderResourceAccessor(), database);
            liquibase.dropAll();
            liquibase.getDatabase().close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
