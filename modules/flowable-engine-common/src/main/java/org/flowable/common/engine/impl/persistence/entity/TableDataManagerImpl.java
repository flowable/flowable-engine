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

package org.flowable.common.engine.impl.persistence.entity;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.ibatis.session.RowBounds;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.management.TableMetaData;
import org.flowable.common.engine.api.management.TablePage;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 */
public class TableDataManagerImpl implements TableDataManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TableDataManagerImpl.class);
    
    protected AbstractEngineConfiguration engineConfiguration;
    
    public TableDataManagerImpl(AbstractEngineConfiguration engineConfiguration) {
        this.engineConfiguration = engineConfiguration;
    }

    @Override
    public Map<String, Long> getTableCount() {
        Map<String, Long> tableCount = new HashMap<>();
        try {
            for (String tableName : getTablesPresentInDatabase()) {
                tableCount.put(tableName, getTableCount(tableName));
            }
            LOGGER.debug("Number of rows per flowable table: {}", tableCount);
        } catch (Exception e) {
            throw new FlowableException("couldn't get table counts", e);
        }
        return tableCount;
    }

    @Override
    public List<String> getTablesPresentInDatabase() {
        List<String> tableNames = new ArrayList<>();
        try {
            Connection connection = getDbSqlSession().getSqlSession().getConnection();
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            LOGGER.debug("retrieving flowable tables from jdbc metadata");
            String databaseTablePrefix = getDbSqlSession().getDbSqlSessionFactory().getDatabaseTablePrefix();
            String actTableNameFilter = getTableNameFilter(databaseMetaData, databaseTablePrefix, "ACT");
            String flwTableNameFilter = getTableNameFilter(databaseMetaData, databaseTablePrefix, "FLW");

            String catalog = getDatabaseCatalog();

            String schema = getDatabaseSchema();

            tableNames.addAll(getTableNames(databaseMetaData, catalog, schema, actTableNameFilter));
            tableNames.addAll(getTableNames(databaseMetaData, catalog, schema, flwTableNameFilter));
        } catch (Exception e) {
            throw new FlowableException("couldn't get flowable table names using metadata: " + e.getMessage(), e);
        }
        return tableNames;
    }

    protected String getTableNameFilter(DatabaseMetaData databaseMetaData, String databaseTablePrefix, String flowableTablePrefix) throws SQLException {
        String tableNameFilter = databaseTablePrefix + flowableTablePrefix + "_%";
        if ("postgres".equals(getDbSqlSession().getDbSqlSessionFactory().getDatabaseType())
                || "cockroachdb".equals(getDbSqlSession().getDbSqlSessionFactory().getDatabaseType())) {
            tableNameFilter = databaseTablePrefix + flowableTablePrefix.toLowerCase(Locale.ROOT) + "_%";
        }
        if ("oracle".equals(getDbSqlSession().getDbSqlSessionFactory().getDatabaseType())) {
            tableNameFilter = databaseTablePrefix + flowableTablePrefix + databaseMetaData.getSearchStringEscape() + "_%";
        }

        return tableNameFilter;
    }

    protected List<String> getTableNames(DatabaseMetaData databaseMetaData, String catalog, String schema, String tableNameFilter) throws SQLException {
        List<String> tableNames = new ArrayList<>();
        try (ResultSet tables = databaseMetaData.getTables(catalog, schema, tableNameFilter, DbSqlSession.JDBC_METADATA_TABLE_TYPES)) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                tableName = tableName.toUpperCase(Locale.ROOT);
                tableNames.add(tableName);
                LOGGER.debug("retrieved flowable table name {}", tableName);
            }
        }

        return tableNames;
    }

    protected String getDatabaseCatalog() {
        String catalog = null;
        if (engineConfiguration.getDatabaseCatalog() != null && engineConfiguration.getDatabaseCatalog().length() > 0) {
            catalog = engineConfiguration.getDatabaseCatalog();
        }

        return catalog;
    }

    protected String getDatabaseSchema() {
        String schema = null;
        if (engineConfiguration.getDatabaseSchema() != null && engineConfiguration.getDatabaseSchema().length() > 0) {
            if ("oracle".equals(getDbSqlSession().getDbSqlSessionFactory().getDatabaseType())) {
                schema = engineConfiguration.getDatabaseSchema().toUpperCase(Locale.ROOT);
            } else {
                schema = engineConfiguration.getDatabaseSchema();
            }
        }

        return schema;
    }

    protected long getTableCount(String tableName) {
        LOGGER.debug("selecting table count for {}", tableName);
        Long count = (Long) getDbSqlSession().selectOne("org.flowable.common.engine.impl.TablePageMap.selectTableCount", Collections.singletonMap("tableName", tableName));
        return count;
    }

    @Override
    @SuppressWarnings("unchecked")
    public TablePage getTablePage(TablePageQueryImpl tablePageQuery, int firstResult, int maxResults) {

        TablePage tablePage = new TablePage();

        @SuppressWarnings("rawtypes")
        List tableData = getDbSqlSession().getSqlSession().selectList("org.flowable.common.engine.impl.TablePageMap.selectTableData", tablePageQuery, new RowBounds(firstResult, maxResults));

        tablePage.setTableName(tablePageQuery.getTableName());
        tablePage.setTotal(getTableCount(tablePageQuery.getTableName()));
        tablePage.setRows((List<Map<String, Object>>) tableData);
        tablePage.setFirstResult(firstResult);

        return tablePage;
    }

    @Override
    public TableMetaData getTableMetaData(String tableName) {
        TableMetaData result = new TableMetaData();
        try {
            result.setTableName(tableName);
            DatabaseMetaData metaData = getDbSqlSession().getSqlSession().getConnection().getMetaData();

            if ("postgres".equals(getDbSqlSession().getDbSqlSessionFactory().getDatabaseType())
                    || "cockroachdb".equals(getDbSqlSession().getDbSqlSessionFactory().getDatabaseType())) {
                tableName = tableName.toLowerCase(Locale.ROOT);
            }

            String catalog = getDatabaseCatalog();

            String schema = getDatabaseSchema();

            ResultSet resultSet = metaData.getColumns(catalog, schema, tableName, null);
            while (resultSet.next()) {
                boolean wrongSchema = false;
                if (schema != null && schema.length() > 0) {
                    for (int i = 0; i < resultSet.getMetaData().getColumnCount(); i++) {
                        String columnName = resultSet.getMetaData().getColumnName(i + 1);
                        if ("TABLE_SCHEM".equalsIgnoreCase(columnName) || "TABLE_SCHEMA".equalsIgnoreCase(columnName)) {
                            if (!schema.equalsIgnoreCase(resultSet.getString(resultSet.getMetaData().getColumnName(i + 1)))) {
                                wrongSchema = true;
                            }
                            break;
                        }
                    }
                }

                if (!wrongSchema) {
                    String name = resultSet.getString("COLUMN_NAME").toUpperCase(Locale.ROOT);
                    String type = resultSet.getString("TYPE_NAME").toUpperCase(Locale.ROOT);
                    result.addColumnMetaData(name, type);
                }
            }

        } catch (SQLException e) {
            throw new FlowableException("Could not retrieve database metadata: " + e.getMessage());
        }

        if (result.getColumnNames().isEmpty()) {
            // According to API, when a table doesn't exist, null should be returned
            result = null;
        }
        return result;
    }
    
    protected DbSqlSession getDbSqlSession() {
        return Context.getCommandContext().getSession(DbSqlSession.class);
    }

}
