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
package org.activiti.rest.conf;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * @author Yvo Swillens
 */
public class BaseEngineConfiguration {

  @Autowired
  protected Environment environment;

  @Bean
  public DataSource dataSource() {

    String jdbcUrl = environment.getProperty("jdbc.url", "jdbc:h2:mem:activiti;DB_CLOSE_DELAY=1000");
    String jdbcDriver = environment.getProperty("jdbc.driver", "org.h2.Driver");
    String jdbcUsername = environment.getProperty("jdbc.username", "sa");
    String jdbcPassword = environment.getProperty("jdbc.password", "");

    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(jdbcUrl);
    dataSource.setDriverClassName(jdbcDriver);
    dataSource.setUsername(jdbcUsername);
    dataSource.setPassword(jdbcPassword);

    // Connection pool settings (see https://github.com/brettwooldridge/HikariCP)
    Long connectionTimeout = environment.getProperty("datasource.connection.timeout", Long.class);
    if (connectionTimeout != null) {
      dataSource.setConnectionTimeout(connectionTimeout);
    }

    Long idleTimeout = environment.getProperty("datasource.connection.idletimeout", Long.class);
    if (idleTimeout != null) {
      dataSource.setIdleTimeout(idleTimeout);
    }

    Long maxLifetime = environment.getProperty("datasource.connection.maxlifetime", Long.class);
    if (maxLifetime != null) {
      dataSource.setMaxLifetime(maxLifetime);
    }

    Integer minIdle = environment.getProperty("datasource.connection.minidle", Integer.class);
    if (minIdle != null) {
      dataSource.setMinimumIdle(minIdle);
    }

    Integer maxPoolSize = environment.getProperty("datasource.connection.maxpoolsize", Integer.class);
    if (maxPoolSize != null) {
      dataSource.setMaximumPoolSize(maxPoolSize);
    }

    return dataSource;
  }

  @Bean(name = "transactionManager")
  public PlatformTransactionManager annotationDrivenTransactionManager() {
    DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
    transactionManager.setDataSource(dataSource());
    return transactionManager;
  }
}
