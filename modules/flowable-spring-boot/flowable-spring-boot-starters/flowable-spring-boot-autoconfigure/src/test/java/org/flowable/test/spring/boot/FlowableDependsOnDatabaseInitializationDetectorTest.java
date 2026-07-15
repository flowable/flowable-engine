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

import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * The Flowable engines create/validate their own schema on engine build, so their beans must be
 * ordered after Spring Boot managed database initializers (Flyway, Liquibase, spring.sql.init).
 * This is done through the {@code DependsOnDatabaseInitializationDetector} SPI (issue #4213).
 *
 * @author Nikhil Bharadwaj Ramashasthri
 */
class FlowableDependsOnDatabaseInitializationDetectorTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceAutoConfiguration.class,
                    DataSourceTransactionManagerAutoConfiguration.class,
                    DataSourceInitializationAutoConfiguration.class,
                    ProcessEngineAutoConfiguration.class,
                    ProcessEngineServicesAutoConfiguration.class));

    @Test
    void processEngineDependsOnDatabaseInitialization() {
        contextRunner
                .withPropertyValues("spring.sql.init.mode=always")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    String[] dependsOn = context.getBeanFactory()
                            .getBeanDefinition("processEngine")
                            .getDependsOn();
                    assertThat(dependsOn)
                            .as("processEngine must wait for the SQL init database initializer")
                            .isNotNull()
                            .contains("dataSourceScriptDatabaseInitializer");
                });
    }

    @Test
    void detectionCanBeOptedOut() {
        contextRunner
                .withPropertyValues(
                        "spring.sql.init.mode=always",
                        "flowable.depends-on-database-initialization-detection=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    String[] dependsOn = context.getBeanFactory()
                            .getBeanDefinition("processEngine")
                            .getDependsOn();
                    assertThat(dependsOn == null || !java.util.Arrays.asList(dependsOn)
                            .contains("dataSourceScriptDatabaseInitializer"))
                            .as("opt-out must remove the database initializer dependency")
                            .isTrue();
                });
    }
}
