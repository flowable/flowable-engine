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
package org.flowable.rest.conf;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariDataSource;

/**
 * @author Yvo Swillens
 * @author Joram Barrez
 */
@Configuration
@EnableTransactionManagement
public class DatabaseConfiguration {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConfiguration.class);

    @Autowired
    protected Environment environment;

    @Bean
    public DataSource dataSource() {
        
        String dataSourceJndiName = environment.getProperty("datasource.jndi.name");
        if (StringUtils.isNotEmpty(dataSourceJndiName)) {

            LOGGER.info("Using jndi datasource '{}'", dataSourceJndiName);
            JndiDataSourceLookup dsLookup = new JndiDataSourceLookup();
            dsLookup.setResourceRef(environment.getProperty("datasource.jndi.resourceRef", Boolean.class, Boolean.TRUE));
            DataSource dataSource = dsLookup.getDataSource(dataSourceJndiName);
            return dataSource;

        } else {

            String jdbcUrl = environment.getProperty("datasource.url", "jdbc:h2:mem:flowable;DB_CLOSE_DELAY=1000");
            String jdbcDriver = environment.getProperty("datasource.driver", "org.h2.Driver");
            String jdbcUsername = environment.getProperty("datasource.username", "sa");
            String jdbcPassword = environment.getProperty("datasource.password", "");
    
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setJdbcUrl(jdbcUrl);
            dataSource.setDriverClassName(jdbcDriver);
            dataSource.setUsername(jdbcUsername);
            dataSource.setPassword(jdbcPassword);
    
            /*
             * Connection pool settings (see https://github.com/brettwooldridge/HikariCP)
             */
            
            // Timeout to wait for a connection of the pool
            Long connectionTimeout = environment.getProperty("datasource.connection-timeout", Long.class);
            if (connectionTimeout == null) {
                // Backwards compatible property name (pre 6.3.0)
                connectionTimeout = environment.getProperty("datasource.connection.timeout", Long.class);
            }
            if (connectionTimeout != null) {
                dataSource.setConnectionTimeout(connectionTimeout);
            }
            
            // Minimum amount of connections to keep idle in the pool
            Integer minIdle = environment.getProperty("datasource.min-pool-size", Integer.class);
            if (minIdle == null) {
                // Backwards compatible property name (pre 6.3.0)
                minIdle = environment.getProperty("datasource.connection.minidle", Integer.class);
            }
            if (minIdle != null) {
                dataSource.setMinimumIdle(minIdle);
            }
    
            // Maximum amount of connections in the pool
            Integer maxPoolSize = environment.getProperty("datasource.max-pool-size", Integer.class);
            if (maxPoolSize == null) {
                // Backwards compatible property name (pre 6.3.0)
                maxPoolSize = environment.getProperty("datasource.connection.maxpoolsize", Integer.class);
            }
            if (maxPoolSize != null) {
                dataSource.setMaximumPoolSize(maxPoolSize);
            }

            // Time in milliseconds to indicate when a connection will be removed from the pool. 
            // Use 0 to never remove an idle connection from the pool 
            Long idleTimeout = environment.getProperty("datasource.max-idle-time", Long.class);
            if (idleTimeout != null) {
                // Property has been historically expressed in seconds, but Hikari expects milliseconds
                idleTimeout = idleTimeout * 1000;
            } else if (idleTimeout == null) {
                // Backwards compatible property name (pre 6.3.0)
                idleTimeout = environment.getProperty("datasource.connection.idletimeout", Long.class);
            }
            if (idleTimeout != null) {
                dataSource.setIdleTimeout(idleTimeout);
            }
            
            // The maximum lifetime of a connection in the pool
            Long maxLifetime = environment.getProperty("datasource.connection.max-lifetime", Long.class);
            if (maxLifetime == null) {
                maxLifetime = environment.getProperty("datasource.connection.maxlifetime", Long.class);
            }
            if (maxLifetime != null) {
                dataSource.setMaxLifetime(maxLifetime);
            }
    
            // Test query
            String testQuery = environment.getProperty("datasource.test-query");
            if (testQuery == null) {
                // Backwards compatible property name (pre 6.3.0)
                testQuery = environment.getProperty("datasource.preferred-test-query");
            }
            if (testQuery != null) {
                dataSource.setConnectionTestQuery(testQuery);
            }
            
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Configuring datasource with following properties");
                LOGGER.info("Datasource driver: {}", jdbcDriver);
                LOGGER.info("Datasource url: {}", jdbcUrl);
                LOGGER.info("Datasource user name: {}", jdbcUsername);
                LOGGER.info("Min pool size | Max pool size | {} | {}", 
                        minIdle != null ? minIdle : "default",
                        maxPoolSize != null ? maxPoolSize : "default");
            }
            
            return dataSource;
        }

    }

    @Bean(name = "transactionManager")
    public PlatformTransactionManager annotationDrivenTransactionManager() {
        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(dataSource());
        return transactionManager;
    }
}
