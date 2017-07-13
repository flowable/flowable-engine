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

import org.flowable.content.engine.ContentEngineConfiguration;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.db.AbstractNonCachingDbSqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class DbSqlSession extends AbstractNonCachingDbSqlSession {

    private static final Logger log = LoggerFactory.getLogger(DbSqlSession.class);

    public DbSqlSession(DbSqlSessionFactory dbSqlSessionFactory) {
        super(dbSqlSessionFactory);
    }

    public DbSqlSession(DbSqlSessionFactory dbSqlSessionFactory, Connection connection, String catalog, String schema) {
        super(dbSqlSessionFactory, connection, catalog, schema);
    }

    // schema operations
    // ////////////////////////////////////////////////////////

    public void dbSchemaCheckVersion() {
        log.debug("flowable content db schema check successful");
    }

    public void dbSchemaCreate() {
        Liquibase liquibase = createLiquibaseInstance();
        try {
            liquibase.update("form");
        } catch (Exception e) {
            throw new FlowableException("Error creating form engine tables", e);
        }
    }

    public void dbSchemaDrop() {
        Liquibase liquibase = createLiquibaseInstance();
        try {
            liquibase.dropAll();
        } catch (Exception e) {
            throw new FlowableException("Error dropping form engine tables", e);
        }
    }

    protected Liquibase createLiquibaseInstance() {
        try {
            DatabaseConnection connection = new JdbcConnection(sqlSession.getConnection());
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connection);
            database.setDatabaseChangeLogTableName(ContentEngineConfiguration.LIQUIBASE_CHANGELOG_PREFIX + database.getDatabaseChangeLogTableName());
            database.setDatabaseChangeLogLockTableName(ContentEngineConfiguration.LIQUIBASE_CHANGELOG_PREFIX + database.getDatabaseChangeLogLockTableName());

            Liquibase liquibase = new Liquibase("org/flowable/form/db/liquibase/flowable-form-db-changelog.xml", new ClassLoaderResourceAccessor(), database);
            return liquibase;

        } catch (Exception e) {
            throw new FlowableException("Error dropping form engine tables", e);
        }
    }

}
