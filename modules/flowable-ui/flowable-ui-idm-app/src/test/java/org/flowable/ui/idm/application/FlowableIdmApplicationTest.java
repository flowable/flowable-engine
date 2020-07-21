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
package org.flowable.ui.idm.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Filip Hrisafov
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class FlowableIdmApplicationTest {

    @Autowired
    private ConfigurableEnvironment environment;

    @Test
    public void contextShouldLoadPropertiesInACorrectOrder() {
        assertThat(environment.getPropertySources())
            .extracting(PropertySource::getName)
            .containsExactly(
                "configurationProperties",
                "Inlined Test Properties",
                "systemProperties",
                "systemEnvironment",
                "random",
                "class path resource [META-INF/flowable-ui-app/flowable-ui-app.properties]",
                "class path resource [flowable-ui-app.properties]",
                "applicationConfig: [classpath:/application.properties]",
                "flowableDefaultConfig: [classpath:/flowable-default.properties]",
                "flowable-liquibase-override",
                "Management Server"
            );
    }
}
