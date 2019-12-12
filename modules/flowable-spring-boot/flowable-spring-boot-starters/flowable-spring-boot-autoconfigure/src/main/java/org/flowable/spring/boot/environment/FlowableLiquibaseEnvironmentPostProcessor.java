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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * When one of the engines that uses liquibase is pulled in with Spring Boot, it pulls the liquibase dependency
 * and that activates {@link org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration}. However,
 * this leads to issues, when the user doesn't actually use it. Therefore, we must disable it per default. In order
 * to activate it, users need to set {@code liquibase.enabled=true} explicitly.
 *
 * @author Filip Hrisafov
 */
@Order(100)
public class FlowableLiquibaseEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableLiquibaseEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String liquibaseProperty = getLiquibaseProperty();
        if (!environment.containsProperty(liquibaseProperty)) {
            LOGGER.warn("Liquibase has not been explicitly enabled or disabled. Overriding default from Spring Boot from `true` to `false`. "
                + "Flowable pulls in Liquibase, but does not use the Spring Boot configuration for it. "
                + "If you are using it you would need to set `{}` to `true` by yourself", liquibaseProperty);
            Map<String, Object> source = new HashMap<>();
            source.put(liquibaseProperty, false);
            environment.getPropertySources().addLast(new MapPropertySource("flowable-liquibase-override", source));
        }
    }

    protected String getLiquibaseProperty() {
        String springBootVersion = SpringBootVersion.getVersion();
        if (springBootVersion == null || !springBootVersion.startsWith("1")) {
            return "spring.liquibase.enabled";
        } else {
            return "liquibase.enabled";
        }
    }
}
