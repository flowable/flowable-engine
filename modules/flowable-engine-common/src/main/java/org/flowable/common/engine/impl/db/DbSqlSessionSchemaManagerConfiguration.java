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

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.impl.interceptor.Session;

/**
 * @author Filip Hrisafov
 */
public class DbSqlSessionSchemaManagerConfiguration implements SchemaManagerDatabaseConfiguration, Session {

    protected final DbSqlSession dbSqlSession;

    public DbSqlSessionSchemaManagerConfiguration(DbSqlSession dbSqlSession) {
        this.dbSqlSession = dbSqlSession;
    }

    @Override
    public void flush() {
        // Nothing to do
    }

    @Override
    public void close() {
        // Nothing to do
    }

    @Override
    public String getDatabaseType() {
        return dbSqlSession.getDbSqlSessionFactory().getDatabaseType();
    }

    @Override
    public String getDatabaseTablePrefix() {
        return dbSqlSession.getDbSqlSessionFactory().getDatabaseTablePrefix();
    }

    @Override
    public boolean isTablePrefixIsSchema() {
        return dbSqlSession.getDbSqlSessionFactory().isTablePrefixIsSchema();
    }

    @Override
    public String getDatabaseCatalog() {
        String catalog = dbSqlSession.getConnectionMetadataDefaultCatalog();
        DbSqlSessionFactory dbSqlSessionFactory = dbSqlSession.getDbSqlSessionFactory();
        if (dbSqlSessionFactory.getDatabaseCatalog() != null && dbSqlSessionFactory.getDatabaseCatalog().length() > 0) {
            catalog = dbSqlSessionFactory.getDatabaseCatalog();
        }
        return catalog;
    }

    @Override
    public String getDatabaseSchema() {
        String schema = dbSqlSession.getConnectionMetadataDefaultSchema();
        DbSqlSessionFactory dbSqlSessionFactory = dbSqlSession.getDbSqlSessionFactory();
        if (dbSqlSessionFactory.getDatabaseSchema() != null && dbSqlSessionFactory.getDatabaseSchema().length() > 0) {
            schema = dbSqlSessionFactory.getDatabaseSchema();
        } else if (dbSqlSessionFactory.isTablePrefixIsSchema() && StringUtils.isNotEmpty(dbSqlSessionFactory.getDatabaseTablePrefix())) {
            schema = dbSqlSessionFactory.getDatabaseTablePrefix();
            if (StringUtils.isNotEmpty(schema) && schema.endsWith(".")) {
                schema = schema.substring(0, schema.length() - 1);
            }
        }
        return schema;
    }

    @Override
    public Connection getConnection() {
        return dbSqlSession.getSqlSession().getConnection();
    }
}
