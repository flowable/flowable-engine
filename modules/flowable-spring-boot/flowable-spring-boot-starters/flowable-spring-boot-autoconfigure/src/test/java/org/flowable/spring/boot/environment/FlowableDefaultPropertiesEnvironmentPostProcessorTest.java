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
package org.flowable.spring.boot.environment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

/**
 * @author Filip Hrisafov
 */
public class FlowableDefaultPropertiesEnvironmentPostProcessorTest {

    private ConfigurableApplicationContext context;

    @After
    public void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    public void flowableDefaultPropertiesAreBeLoaded() {
        SpringApplication application = new SpringApplication(Config.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        context = application.run();

        ConfigurableEnvironment environment = context.getEnvironment();
        assertThat(environment.getPropertySources())
            .extracting(PropertySource::getName)
            .containsExactly(
                "configurationProperties",
                "systemProperties",
                "systemEnvironment",
                "random",
                "applicationConfig: [classpath:/application.properties]",
                "applicationConfig: [classpath:/application.yml]",
                "flowableDefaultConfig: [classpath:/flowable-default.properties]",
                "flowableDefaultConfig: [classpath:/flowable-default.yml]",
                "flowable-liquibase-override"
            );

        assertThat(environment.getProperty("foo")).isEqualTo("from-flowable-default-properties");
        assertThat(environment.getProperty("bar")).isEqualTo("from-flowable-default-yaml");
        assertThat(environment.getProperty("baz")).isEqualTo("from-application-properties");
        assertThat(environment.getProperty("qux")).isEqualTo("from-application-yaml");
    }

    @Test
    public void flowableDefaultPropertiesAreBeforeApplicationDefaultProperties() {
        SpringApplication application = new SpringApplication(Config.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        Map<String, Object> defaultProperties = new HashMap<>();
        defaultProperties.put("foobar", "from-default-properties");
        application.setDefaultProperties(defaultProperties);
        context = application.run();

        ConfigurableEnvironment environment = context.getEnvironment();
        assertThat(environment.getPropertySources())
            .extracting(PropertySource::getName)
            .containsExactly(
                "configurationProperties",
                "systemProperties",
                "systemEnvironment",
                "random",
                "applicationConfig: [classpath:/application.properties]",
                "applicationConfig: [classpath:/application.yml]",
                "flowableDefaultConfig: [classpath:/flowable-default.properties]",
                "flowableDefaultConfig: [classpath:/flowable-default.yml]",
                "flowable-liquibase-override",
                "defaultProperties"
            );

        assertThat(environment.getProperty("foo")).isEqualTo("from-flowable-default-properties");
        assertThat(environment.getProperty("bar")).isEqualTo("from-flowable-default-yaml");
        assertThat(environment.getProperty("baz")).isEqualTo("from-application-properties");
        assertThat(environment.getProperty("qux")).isEqualTo("from-application-yaml");
    }

    @Configuration
    static class Config {

    }
}
