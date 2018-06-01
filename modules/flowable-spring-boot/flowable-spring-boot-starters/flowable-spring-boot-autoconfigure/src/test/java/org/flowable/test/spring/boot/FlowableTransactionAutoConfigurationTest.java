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
package org.flowable.test.spring.boot;

import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.flowable.spring.boot.FlowableTransactionAutoConfiguration;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Filip Hrisafov
 */
public class FlowableTransactionAutoConfigurationTest {

    private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            FlowableTransactionAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            TransactionAutoConfiguration.class
        ));

    @Test
    public void noPlatformTransactionManagerOnClasspath() {
        contextRunner
            .withClassLoader(new FilteredClassLoader(PlatformTransactionManager.class))
            .run(context -> {
                assertThat(context).doesNotHaveBean(PlatformTransactionManager.class);
            });
    }

    @Test
    public void noDataSourceOnClasspath() {
        contextRunner
            .withClassLoader(new FilteredClassLoader(DataSource.class))
            .run(context -> {
                assertThat(context).doesNotHaveBean(DataSource.class);
            });
    }

    @Test
    public void usingSpringBootTransactionManager() {
        contextRunner
            .withConfiguration(AutoConfigurations.of(DataSourceTransactionManagerAutoConfiguration.class))
            .withClassLoader(new FilteredClassLoader(EntityManagerFactory.class))
            .run(context -> {
                assertThat(context)
                    .hasSingleBean(FlowableTransactionAutoConfiguration.DataSourceConfiguration.class)
                    .doesNotHaveBean(FlowableTransactionAutoConfiguration.JpaTransactionConfiguration.class)
                    .hasSingleBean(PlatformTransactionManager.class)
                    .doesNotHaveBean("flowableTransactionManager");
            });
    }

    @Test
    public void usingFlowableConfiguredTransactionManager() {
        contextRunner
            .withClassLoader(new FilteredClassLoader(EntityManagerFactory.class))
            .run(context -> {
                assertThat(context)
                    .hasSingleBean(FlowableTransactionAutoConfiguration.DataSourceConfiguration.class)
                    .doesNotHaveBean(FlowableTransactionAutoConfiguration.JpaTransactionConfiguration.class)
                    .hasSingleBean(PlatformTransactionManager.class)
                    .hasBean("flowableTransactionManager");
            });
    }

    @Test
    public void usingSpringBootJpaConfiguredTransactionManager() {
        contextRunner
            .withConfiguration(AutoConfigurations.of(
//                DataSourceTransactionManagerAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class
            ))
            .run(context -> {
                assertThat(context)
                    .doesNotHaveBean(FlowableTransactionAutoConfiguration.DataSourceConfiguration.class)
                    .hasSingleBean(FlowableTransactionAutoConfiguration.JpaTransactionConfiguration.class)
                    .hasSingleBean(PlatformTransactionManager.class)
                    .doesNotHaveBean("flowableTransactionManager");
            });
    }
}
