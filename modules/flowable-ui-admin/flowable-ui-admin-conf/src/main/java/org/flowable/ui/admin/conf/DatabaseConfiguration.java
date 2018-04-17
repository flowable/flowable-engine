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
package org.flowable.ui.admin.conf;

import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.flowable.ui.admin.domain.generator.MinimalDataGenerator;
import org.flowable.ui.admin.properties.FlowableAdminAppProperties;
import org.flowable.ui.common.service.exception.InternalServerErrorException;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

@Configuration
public class DatabaseConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConfiguration.class);

    protected static final String LIQUIBASE_CHANGELOG_PREFIX = "ACT_ADM_";

    @Autowired
    private FlowableAdminAppProperties env;

    @Autowired
    private ResourceLoader resourceLoader;

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);

        try {
            Properties properties = new Properties();
            properties.put("prefix", env.getDataSourcePrefix());
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
    public SqlSessionTemplate SqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean(name = "liquibase")
    public Liquibase liquibase(DataSource dataSource) {
        LOGGER.debug("Configuring Liquibase");

        try {

            DatabaseConnection connection = new JdbcConnection(dataSource.getConnection());
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
