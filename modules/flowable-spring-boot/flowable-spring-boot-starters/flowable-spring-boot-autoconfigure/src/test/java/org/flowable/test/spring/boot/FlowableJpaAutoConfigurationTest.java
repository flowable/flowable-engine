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

import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.flowable.spring.boot.FlowableJpaAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * @author Filip Hrisafov
 */
public class FlowableJpaAutoConfigurationTest {

    private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            FlowableJpaAutoConfiguration.class
        ));

    @Test
    public void withMissingEntityManagerFactoryBean() {
        contextRunner
            .run(context -> {
                assertThat(context).doesNotHaveBean(FlowableJpaAutoConfiguration.class);
            });
    }

    @Test
    public void withEntityManagerFactoryBeanAndMissingSpringProcessEngineConfigurationClass() {
        contextRunner
            .withConfiguration(AutoConfigurations.of(
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class
            ))
            .withClassLoader(new FilteredClassLoader(SpringProcessEngineConfiguration.class))
            .run(context -> {
                assertThat(context).doesNotHaveBean(FlowableJpaAutoConfiguration.class);
            });
    }

    @Test
    public void withEntityManagerFactoryBeanAndSpringProcessEngineConfigurationClass() {
        contextRunner
            .withConfiguration(AutoConfigurations.of(
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class
            ))
            .run(context -> {
                assertThat(context)
                    .hasSingleBean(FlowableJpaAutoConfiguration.class)
                    .hasBean("jpaProcessEngineConfigurer");

                EntityManagerFactory entityManagerFactory = context.getBean(EntityManagerFactory.class);
                @SuppressWarnings("unchecked")
                EngineConfigurationConfigurer<SpringProcessEngineConfiguration> jpaProcessEngineConfigurer =
                    (EngineConfigurationConfigurer<SpringProcessEngineConfiguration>) context
                        .getBean("jpaProcessEngineConfigurer", EngineConfigurationConfigurer.class);

                SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();

                assertThat(configuration.getJpaEntityManagerFactory())
                    .as("Process JPA Entity Manager Factory")
                    .isNull();
                assertThat(configuration.isJpaHandleTransaction())
                    .as("Process JPA handle transaction")
                    .isFalse();
                assertThat(configuration.isJpaCloseEntityManager())
                    .as("Process JPA close entity manager")
                    .isFalse();

                jpaProcessEngineConfigurer.configure(configuration);

                assertThat(configuration.getJpaEntityManagerFactory())
                    .as("Process JPA Entity Manager Factory")
                    .isSameAs(entityManagerFactory);
                assertThat(configuration.isJpaHandleTransaction())
                    .as("Process JPA handle transaction")
                    .isFalse();
                assertThat(configuration.isJpaCloseEntityManager())
                    .as("Process JPA close entity manager")
                    .isFalse();
            });
    }
}
