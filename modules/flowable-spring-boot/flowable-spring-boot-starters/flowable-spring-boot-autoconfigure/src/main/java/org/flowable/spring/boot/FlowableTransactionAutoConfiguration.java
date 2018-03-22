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
package org.flowable.spring.boot;

import org.flowable.engine.ProcessEngine;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 * Configuration of the Transaction manager in case none has  been found.
 *
 * @author Filip Hrisafov
 */
@Configuration
@AutoConfigureAfter({
    DataSourceAutoConfiguration.class,
    TransactionAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
public class FlowableTransactionAutoConfiguration {


    @Configuration
    @ConditionalOnMissingClass("javax.persistence.EntityManagerFactory")
    public static class DataSourceConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

    }

    @Configuration
    @ConditionalOnClass(name = "javax.persistence.EntityManagerFactory")
    public static class JpaTransactionConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
            return new JpaTransactionManager(emf);
        }

        @ConditionalOnClass(ProcessEngine.class)
        @ConditionalOnMissingBean(name = "jpaProcessEngineConfigurer")
        @Bean
        public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> jpaProcessEngineConfigurer(EntityManagerFactory emf) {
            return processEngineConfiguration -> {
                processEngineConfiguration.setJpaEntityManagerFactory(emf);
                processEngineConfiguration.setJpaHandleTransaction(false);
                processEngineConfiguration.setJpaCloseEntityManager(false);
            };
        }
    }

}
