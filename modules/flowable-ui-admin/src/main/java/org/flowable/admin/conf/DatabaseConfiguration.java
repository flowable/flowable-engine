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
package org.flowable.admin.conf;

import java.beans.PropertyVetoException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.flowable.admin.domain.generator.MinimalDataGenerator;
import org.flowable.app.service.exception.InternalServerErrorException;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

@Configuration
@EnableTransactionManagement
public class DatabaseConfiguration {

    private final Logger log = LoggerFactory.getLogger(DatabaseConfiguration.class);

    protected static final String LIQUIBASE_CHANGELOG_PREFIX = "ACT_ADM_";

    @Autowired
    private Environment env;

    @Autowired
    private ResourceLoader resourceLoader;

    @Bean
    public DataSource dataSource() {
        log.info("Configuring Datasource");

        String dataSourceJndiName = env.getProperty("datasource.jndi.name");
        if (StringUtils.isNotEmpty(dataSourceJndiName)) {

            log.info("Using jndi datasource '{}'", dataSourceJndiName);
            JndiDataSourceLookup dsLookup = new JndiDataSourceLookup();
            dsLookup.setResourceRef(env.getProperty("datasource.jndi.resourceRef", Boolean.class, Boolean.TRUE));
            DataSource dataSource = dsLookup.getDataSource(dataSourceJndiName);
            return dataSource;

        } else {

            String dataSourceDriver = env.getProperty("datasource.driver", "com.mysql.jdbc.Driver");
            String dataSourceUrl = env.getProperty("datasource.url", "jdbc:mysql://127.0.0.1:3306/flowable?characterEncoding=UTF-8");

            String dataSourceUsername = env.getProperty("datasource.username", "flowable");
            String dataSourcePassword = env.getProperty("datasource.password", "flowable");

            Integer minPoolSize = env.getProperty("datasource.min-pool-size", Integer.class);
            if (minPoolSize == null) {
                minPoolSize = 5;
            }

            Integer maxPoolSize = env.getProperty("datasource.max-pool-size", Integer.class);
            if (maxPoolSize == null) {
                maxPoolSize = 20;
            }

            Integer acquireIncrement = env.getProperty("datasource.acquire-increment", Integer.class);
            if (acquireIncrement == null) {
                acquireIncrement = 1;
            }

            String preferredTestQuery = env.getProperty("datasource.preferred-test-query");

            Boolean testConnectionOnCheckin = env.getProperty("datasource.test-connection-on-checkin", Boolean.class);
            if (testConnectionOnCheckin == null) {
                testConnectionOnCheckin = true;
            }

            Boolean testConnectionOnCheckOut = env.getProperty("datasource.test-connection-on-checkout", Boolean.class);
            if (testConnectionOnCheckOut == null) {
                testConnectionOnCheckOut = true;
            }

            Integer maxIdleTime = env.getProperty("datasource.max-idle-time", Integer.class);
            if (maxIdleTime == null) {
                maxIdleTime = 1800;
            }

            Integer maxIdleTimeExcessConnections = env.getProperty("datasource.max-idle-time-excess-connections", Integer.class);
            if (maxIdleTimeExcessConnections == null) {
                maxIdleTimeExcessConnections = 1800;
            }

            if (log.isInfoEnabled()) {
                log.info("Configuring Datasource with following properties (omitted password for security)");
                log.info("datasource driver : {}", dataSourceDriver);
                log.info("datasource url : {}", dataSourceUrl);
                log.info("datasource user name : {}", dataSourceUsername);
                log.info("Min pool size | Max pool size | acquire increment : {} | {} | {}", minPoolSize, maxPoolSize, acquireIncrement);
            }

            ComboPooledDataSource ds = new ComboPooledDataSource();
            try {
                ds.setDriverClass(dataSourceDriver);
            } catch (PropertyVetoException e) {
                log.error("Could not set Jdbc Driver class", e);
                return null;
            }

            // Connection settings
            ds.setJdbcUrl(dataSourceUrl);
            ds.setUser(dataSourceUsername);
            ds.setPassword(dataSourcePassword);

            // Pool config: see http://www.mchange.com/projects/c3p0/#configuration
            ds.setMinPoolSize(minPoolSize);
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

    @Bean
    public SqlSessionFactory sqlSessionFactory() {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource());

        try {
            Properties properties = new Properties();
            properties.put("prefix", env.getProperty("datasource.prefix", ""));
            sqlSessionFactoryBean.setConfigurationProperties(properties);
            sqlSessionFactoryBean
                    .setMapperLocations(ResourcePatternUtils.getResourcePatternResolver(resourceLoader).getResources("classpath:/META-INF/admin-mybatis-mappings/*.xml"));
            sqlSessionFactoryBean.afterPropertiesSet();
            return sqlSessionFactoryBean.getObject();
        } catch (Exception e) {
            throw new RuntimeException("Could not create sqlSessionFactory", e);
        }

    }

    @Bean(destroyMethod = "clearCache") // destroyMethod: see https://github.com/mybatis/old-google-code-issues/issues/778
    public SqlSessionTemplate SqlSessionTemplate() {
        return new SqlSessionTemplate(sqlSessionFactory());
    }

    @Bean(name = "liquibase")
    public Liquibase liquibase() {
        log.debug("Configuring Liquibase");

        try {

            DatabaseConnection connection = new JdbcConnection(dataSource().getConnection());
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connection);
            database.setDatabaseChangeLogTableName(LIQUIBASE_CHANGELOG_PREFIX + database.getDatabaseChangeLogTableName());
            database.setDatabaseChangeLogLockTableName(LIQUIBASE_CHANGELOG_PREFIX + database.getDatabaseChangeLogLockTableName());

            Liquibase liquibase = new Liquibase("META-INF/liquibase/flowable-admin-app-db-changelog.xml", new ClassLoaderResourceAccessor(), database);
            liquibase.update("flowable");
            return liquibase;

        } catch (Exception e) {
            throw new InternalServerErrorException("Error creating liquibase database");
        }
    }

    @Bean(name = "minimalDataGenerator")
    public MinimalDataGenerator minimalDataGenerator() {
        return new MinimalDataGenerator();
    }
}
