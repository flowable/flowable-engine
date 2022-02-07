package org.flowable.rest.app;/* Licensed under the Apache License, Version 2.0 (the "License");
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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.metrics.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

/**
 * @author Filip Hrisafov
 */
@SpringBootTest
@AutoConfigureMetrics
public class FlowableRestApplicationTest {

    @Autowired
    private ConfigurableEnvironment environment;

    @Test
    public void contextShouldLoadPropertiesInACorrectOrder() {
        assertThat(environment.getPropertySources())
            .extracting(PropertySource::getName)
            .containsExactly(
                "configurationProperties",
                "Inlined Test Properties",
                "servletConfigInitParams",
                "servletContextInitParams",
                "systemProperties",
                "systemEnvironment",
                "random",
                "class path resource [db.properties]",
                "class path resource [engine.properties]",
                "Config resource 'class path resource [application.properties]' via location 'optional:classpath:/'",
                "flowableDefaultConfig: [classpath:/flowable-default.properties]",
                "flowable-liquibase-override",
                "Management Server"
            );
    }

}