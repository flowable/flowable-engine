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
package org.flowable.rest.conf.jpa;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.CompositeDatabasePopulator;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptException;

@Configuration(proxyBeanMethods = false)
public class DatabaseInititializer {

    @Value("classpath:org/flowable/rest/api/jpa/data.sql")
    private Resource dataScript;

    @Autowired
    protected DataSource dataSource;

    @Bean
    public DataSourceInitializer dataSourceInitializer() {
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(databasePopulator());
        initializer.setDatabaseCleaner(new FlowableDataSourcePopulator(false));
        return initializer;
    }

    private DatabasePopulator databasePopulator() {
        return new CompositeDatabasePopulator(
                new FlowableDataSourcePopulator(true),
                new ResourceDatabasePopulator(dataScript)
        );
    }

    protected static class FlowableDataSourcePopulator implements DatabasePopulator {

        protected final boolean createTable;

        protected FlowableDataSourcePopulator(boolean createTable) {
            this.createTable = createTable;
        }

        @Override
        public void populate(Connection connection) throws SQLException, ScriptException {
            if (isTablePresent(connection)) {
                connection.createStatement().execute("drop table message");
            }

            if (createTable) {
                connection.createStatement().execute("create table message (id int, text varchar(100))");
            }
        }


    }

    protected static boolean isTablePresent(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        boolean tablePresent = metaData.getTables(null, null, "MESSAGE", new String[] { "TABLE" }).next();
        if (tablePresent) {
            return true;
        }
        return metaData.getTables(null, null, "message", new String[] { "TABLE" }).next();
    }

}
