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
import org.flowable.common.engine.impl.db.DbSchemaManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.util.CommandContextUtil;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

public class FormDbSchemaManager implements DbSchemaManager {
    
    public static String LIQUIBASE_CHANGELOG = "org/flowable/form/db/liquibase/flowable-form-db-changelog.xml";
    
    @Override
    public void dbSchemaCreate() {
        Liquibase liquibase = createLiquibaseInstance();
        try {
            liquibase.update("form");
        } catch (Exception e) {
            throw new FlowableException("Error creating form engine tables", e);
        }
    }

    @Override
    public void dbSchemaDrop() {
        Liquibase liquibase = createLiquibaseInstance();
        try {
            liquibase.dropAll();
        } catch (Exception e) {
            throw new FlowableException("Error dropping form engine tables", e);
        }
    }
    
    @Override
    public String dbSchemaUpdate() {
        dbSchemaCreate();
        return null;
    }

    protected static Liquibase createLiquibaseInstance() {
        try {
            Connection jdbcConnection = null;
            CommandContext commandContext = CommandContextUtil.getCommandContext();
            FormEngineConfiguration formEngineConfiguration = CommandContextUtil.getFormEngineConfiguration(commandContext);
            if (commandContext == null) {
                jdbcConnection = CommandContextUtil.getFormEngineConfiguration(commandContext).getDataSource().getConnection();
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

}
