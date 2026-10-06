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
package org.flowable.common.engine.impl.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class DbUtilTest {

    @Test
    void testSpannerDatabaseTypeConstant() {
        assertThat(DbUtil.DATABASE_TYPE_SPANNER).isEqualTo("spanner");
    }

    @Test
    void testDefaultDatabaseTypeMappingsContainsSpanner() {
        Properties properties = DbUtil.getDefaultDatabaseTypeMappings();
        assertThat(properties.getProperty("Google Cloud Spanner")).isEqualTo("spanner");
    }

    @Test
    void testDetermineDatabaseTypeFromSpannerProductName() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("Google Cloud Spanner");

        String databaseType = DbUtil.determineDatabaseType(dataSource, LoggerFactory.getLogger(DbUtilTest.class));
        assertThat(databaseType).isEqualTo("spanner");
    }

    @Test
    void testDetermineDatabaseTypeFromSpannerProductNameFallback() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("Custom Cloud Spanner Enterprise");

        String databaseType = DbUtil.determineDatabaseType(dataSource, LoggerFactory.getLogger(DbUtilTest.class));
        assertThat(databaseType).isEqualTo("spanner");
    }

    @Test
    void testDetermineDatabaseTypeFromSpannerJdbcUrlFallback() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("UnknownDriver");
        when(metaData.getURL()).thenReturn("jdbc:cloudspanner://localhost:9010/projects/p/instances/i/databases/d");

        String databaseType = DbUtil.determineDatabaseType(dataSource, LoggerFactory.getLogger(DbUtilTest.class));
        assertThat(databaseType).isEqualTo("spanner");
    }
}
