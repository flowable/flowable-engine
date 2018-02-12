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
package org.flowable.app.conf;

import java.beans.PropertyVetoException;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.mchange.v2.c3p0.ComboPooledDataSource;

@Configuration
@EnableTransactionManagement
public class DatabaseConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConfiguration.class);

    @Autowired
    protected Environment env;

    @Autowired
    protected ResourceLoader resourceLoader;

    @Bean
    public DataSource dataSource() {
        LOGGER.info("Configuring Datasource");

        String dataSourceJndiName = env.getProperty("datasource.jndi.name");
        if (StringUtils.isNotEmpty(dataSourceJndiName)) {

            LOGGER.info("Using jndi datasource '{}'", dataSourceJndiName);
            JndiDataSourceLookup dsLookup = new JndiDataSourceLookup();
            dsLookup.setResourceRef(env.getProperty("datasource.jndi.resourceRef", Boolean.class, Boolean.TRUE));
            DataSource dataSource = dsLookup.getDataSource(dataSourceJndiName);
            return dataSource;

        } else {

            String dataSourceDriver = env.getProperty("datasource.driver", "org.h2.Driver");
            String dataSourceUrl = env.getProperty("datasource.url", "jdbc:h2:mem:flowable;DB_CLOSE_DELAY=-1");

            String dataSourceUsername = env.getProperty("datasource.username", "sa");
            String dataSourcePassword = env.getProperty("datasource.password", "");

            Integer minPoolSize = env.getProperty("datasource.min-pool-size", Integer.class, 10);
            Integer maxPoolSize = env.getProperty("datasource.max-pool-size", Integer.class, 100);

            Integer acquireIncrement = env.getProperty("datasource.acquire-increment", Integer.class, 5);

            String preferredTestQuery = env.getProperty("datasource.preferred-test-query");
            Boolean testConnectionOnCheckin = env.getProperty("datasource.test-connection-on-checkin", Boolean.class, true);
            Boolean testConnectionOnCheckOut = env.getProperty("datasource.test-connection-on-checkout", Boolean.class, true);

            Integer maxIdleTime = env.getProperty("datasource.max-idle-time", Integer.class, 1800);
            Integer maxIdleTimeExcessConnections = env.getProperty("datasource.max-idle-time-excess-connections", Integer.class, 1800);

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Configuring Datasource with following properties (omitted password for security)");
                LOGGER.info("Datasource driver: {}", dataSourceDriver);
                LOGGER.info("Datasource url: {}", dataSourceUrl);
                LOGGER.info("Datasource user name: {}", dataSourceUsername);
                LOGGER.info("Min pool size | Max pool size | Acquire increment: {} | {} | {}", minPoolSize, maxPoolSize, acquireIncrement);
            }

            ComboPooledDataSource ds = new ComboPooledDataSource();
            try {
                ds.setDriverClass(dataSourceDriver);
            } catch (PropertyVetoException e) {
                LOGGER.error("Could not set Jdbc Driver class", e);
                return null;
            }

            // Connection settings
            ds.setJdbcUrl(dataSourceUrl);
            ds.setUser(dataSourceUsername);
            ds.setPassword(dataSourcePassword);

            // Pool config: see http://www.mchange.com/projects/c3p0/#configuration
            ds.setMinPoolSize(minPoolSize);
            ds.setInitialPoolSize(minPoolSize);
            ds.setMaxPoolSize(maxPoolSize);
            ds.setAcquireIncrement(acquireIncrement);
            if (preferredTestQuery != null) {
                ds.setPreferredTestQuery(preferredTestQuery);
            }
            ds.setTestConnectionOnCheckin(testConnectionOnCheckin);
            ds.setTestConnectionOnCheckout(testConnectionOnCheckOut);
            ds.setMaxIdleTimeExcessConnections(maxIdleTimeExcessConnections);
            ds.setMaxIdleTime(maxIdleTime);

            return ds;
        }
    }

    @Bean
    public PlatformTransactionManager annotationDrivenTransactionManager() {
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager();
        dataSourceTransactionManager.setDataSource(dataSource());
        return dataSourceTransactionManager;
    }

}
