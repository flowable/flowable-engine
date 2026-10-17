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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.ibatis.session.SqlSession;
import org.flowable.common.engine.api.management.TableMetaData;
import org.flowable.common.engine.api.query.Query.NullHandlingOnOrder;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.persistence.entity.TableDataManagerImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpannerEngineConfigurationTest {

    private CommandContext commandContext;

    @BeforeEach
    void setUp() {
        commandContext = mock(CommandContext.class);
        Context.setCommandContext(commandContext);
    }

    @AfterEach
    void tearDown() {
        Context.removeCommandContext();
    }

    @Test
    void testSpannerDatabaseTypeConstant() {
        assertThat(AbstractEngineConfiguration.DATABASE_TYPE_SPANNER).isEqualTo("spanner");
    }

    @Test
    void testSpannerPropertiesFileLoadedCorrectly() throws Exception {
        Properties properties = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("org/flowable/common/db/properties/spanner.properties")) {
            assertThat(is).as("spanner.properties must exist on the classpath").isNotNull();
            properties.load(is);
        }

        assertThat(properties.getProperty("limitAfter")).isEqualTo("LIMIT #{maxResults} OFFSET #{firstResult}");
        assertThat(properties.getProperty("blobType")).isEqualTo("BLOB");
        assertThat(properties.getProperty("boolValue")).isEqualTo("TRUE");
    }

    @Test
    void testListQueryParameterObjectNullOrderingForSpanner() {
        ListQueryParameterObject queryParam = new ListQueryParameterObject();
        queryParam.setDatabaseType(AbstractEngineConfiguration.DATABASE_TYPE_SPANNER);

        queryParam.addOrder("RES.NAME_", "asc", NullHandlingOnOrder.NULLS_FIRST);
        assertThat(queryParam.getOrderBy()).isEqualTo("order by RES.NAME_ asc NULLS FIRST");

        ListQueryParameterObject queryParamDesc = new ListQueryParameterObject();
        queryParamDesc.setDatabaseType(AbstractEngineConfiguration.DATABASE_TYPE_SPANNER);
        queryParamDesc.addOrder("RES.CREATE_TIME_", "desc", NullHandlingOnOrder.NULLS_LAST);
        assertThat(queryParamDesc.getOrderBy()).isEqualTo("order by RES.CREATE_TIME_ desc NULLS LAST");
    }

    @Test
    void testIsTablePresentForSpannerUsesEmptySchemaAndUppercase() throws SQLException {
        SchemaManagerDatabaseConfiguration dbConfiguration = mock(SchemaManagerDatabaseConfiguration.class);
        when(commandContext.getSession(SchemaManagerDatabaseConfiguration.class)).thenReturn(dbConfiguration);

        when(dbConfiguration.getDatabaseType()).thenReturn(AbstractEngineConfiguration.DATABASE_TYPE_SPANNER);
        when(dbConfiguration.getDatabaseCatalog()).thenReturn(null);
        when(dbConfiguration.getDatabaseSchema()).thenReturn(null);
        when(dbConfiguration.getDatabaseTablePrefix()).thenReturn("");
        when(dbConfiguration.isTablePrefixIsSchema()).thenReturn(false);

        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(dbConfiguration.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getTables(isNull(), eq(""), eq("ACT_GE_PROPERTY"), any())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        AbstractSqlScriptBasedDbSchemaManager schemaManager = new AbstractSqlScriptBasedDbSchemaManager() {
            @Override
            protected String getResourcesRootDirectory() {
                return "org/flowable/common/db/";
            }

            @Override
            public void schemaCreate() {
            }

            @Override
            public void schemaDrop() {
            }

            @Override
            public String schemaUpdate() {
                return null;
            }

            @Override
            public void schemaCheckVersion() {
            }

            @Override
            public String getContext() {
                return "test";
            }
        };

        boolean present = schemaManager.isTablePresent("act_ge_property");
        assertThat(present).isTrue();
    }

    @Test
    void testTableDataManagerSpannerSchemaAndMetadata() throws SQLException {
        DbSqlSession dbSqlSession = mock(DbSqlSession.class);
        DbSqlSessionFactory dbSqlSessionFactory = mock(DbSqlSessionFactory.class);
        SqlSession sqlSession = mock(SqlSession.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        ResultSet resultSet = mock(ResultSet.class);
        java.sql.ResultSetMetaData rsMetaData = mock(java.sql.ResultSetMetaData.class);

        when(commandContext.getSession(DbSqlSession.class)).thenReturn(dbSqlSession);
        when(dbSqlSession.getDbSqlSessionFactory()).thenReturn(dbSqlSessionFactory);
        when(dbSqlSessionFactory.getDatabaseType()).thenReturn(AbstractEngineConfiguration.DATABASE_TYPE_SPANNER);
        when(dbSqlSessionFactory.getDatabaseTablePrefix()).thenReturn("");

        when(dbSqlSession.getSqlSession()).thenReturn(sqlSession);
        when(sqlSession.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getSearchStringEscape()).thenReturn("\\");

        AbstractEngineConfiguration engineConfig = mock(AbstractEngineConfiguration.class);
        when(engineConfig.getDatabaseSchema()).thenReturn(null);
        when(engineConfig.getDatabaseCatalog()).thenReturn(null);

        // Expect getColumns with null catalog, empty string schema, and uppercase table name
        when(metaData.getColumns(isNull(), eq(""), eq("ACT_GE_PROPERTY"), isNull())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getMetaData()).thenReturn(rsMetaData);
        when(rsMetaData.getColumnCount()).thenReturn(2);
        when(rsMetaData.getColumnName(1)).thenReturn("COLUMN_NAME");
        when(rsMetaData.getColumnName(2)).thenReturn("TYPE_NAME");
        when(resultSet.getString("COLUMN_NAME")).thenReturn("NAME_");
        when(resultSet.getString("TYPE_NAME")).thenReturn("STRING(64)");

        TestTableDataManager tableDataManager = new TestTableDataManager(engineConfig);

        String tableNameFilter = tableDataManager.callGetTableNameFilter(metaData, "", "act");
        assertThat(tableNameFilter).isEqualTo("ACT\\_%");

        TableMetaData tableMetaData = tableDataManager.getTableMetaData("act_ge_property");
        assertThat(tableMetaData).isNotNull();
        assertThat(tableMetaData.getTableName()).isEqualTo("ACT_GE_PROPERTY");
        assertThat(tableMetaData.getColumnNames()).containsExactly("NAME_");
        assertThat(tableMetaData.getColumnTypes()).containsExactly("STRING(64)");
    }

    private static class TestTableDataManager extends TableDataManagerImpl {
        public TestTableDataManager(AbstractEngineConfiguration engineConfiguration) {
            super(engineConfiguration);
        }

        public String callGetTableNameFilter(DatabaseMetaData metaData, String tablePrefix, String flowablePrefix) throws SQLException {
            return getTableNameFilter(metaData, tablePrefix, flowablePrefix);
        }
    }
}
