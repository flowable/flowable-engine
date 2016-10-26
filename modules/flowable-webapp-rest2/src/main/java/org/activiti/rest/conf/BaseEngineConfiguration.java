/**
 * Activiti app component part of the Activiti project
 * Copyright 2005-2015 Alfresco Software, Ltd. All rights reserved.
 * <p>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.activiti.rest.conf;

import com.zaxxer.hikari.HikariDataSource;
import org.activiti.engine.ProcessEngine;
import org.activiti.spring.ProcessEngineFactoryBean;
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
