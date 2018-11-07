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

import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * @author Filip Hrisafov
 */
public class FlowableDefaultPropertiesEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    public static final int DEFAULT_ORDER = ConfigFileApplicationListener.DEFAULT_ORDER + 10;

    private static final String DEFAULT_NAME = "flowable-default";

    private int order = DEFAULT_ORDER;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        new Loader(environment, application.getResourceLoader()).load();
    }

    @Override
    public int getOrder() {
        return order;
    }

    private static class Loader {

        private final ConfigurableEnvironment environment;

        private final ResourceLoader resourceLoader;

        private final List<PropertySourceLoader> propertySourceLoaders;

        Loader(ConfigurableEnvironment environment, ResourceLoader resourceLoader) {
            this.environment = environment;
            this.resourceLoader = resourceLoader == null ? new DefaultResourceLoader()
                : resourceLoader;
            this.propertySourceLoaders = SpringFactoriesLoader.loadFactories(
                PropertySourceLoader.class, getClass().getClassLoader());
        }

        void load() {
            for (PropertySourceLoader loader : propertySourceLoaders) {
                for (String extension : loader.getFileExtensions()) {
                    String location = "classpath:/" + FlowableDefaultPropertiesEnvironmentPostProcessor.DEFAULT_NAME + "." + extension;
                    load(location, loader);
                }
            }

        }

        void load(String location, PropertySourceLoader loader) {
            try {
                Resource resource = resourceLoader.getResource(location);
                if (!resource.exists()) {
                    return;
                }
                String propertyResourceName = "flowableDefaultConfig: [" + location + "]";

                List<PropertySource<?>> propertySources = loader.load(propertyResourceName, resource);
                if (propertySources == null) {
                    return;
                }
                propertySources.forEach(source -> environment.getPropertySources().addLast(source));
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to load property "
                    + "source from location '" + location + "'", ex);
            }
        }
    }
}
