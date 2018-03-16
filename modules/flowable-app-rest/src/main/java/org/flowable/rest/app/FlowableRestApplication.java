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
package org.flowable.rest.app;

import org.flowable.rest.conf.BootstrapConfiguration;
import org.flowable.rest.conf.DatabaseConfiguration;
import org.flowable.rest.conf.FlowableEngineConfiguration;
import org.flowable.rest.conf.SecurityConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author Filip Hrisafov
 */
@PropertySources({
    // For backwards compatibility (pre 6.3.0)
    @PropertySource(value = "classpath:db.properties", ignoreResourceNotFound = true),
    @PropertySource(value = "classpath:engine.properties", ignoreResourceNotFound = true)

})
@Import({
    BootstrapConfiguration.class,
    DatabaseConfiguration.class,
    FlowableEngineConfiguration.class,
    SecurityConfiguration.class

})
@SpringBootApplication
public class FlowableRestApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(FlowableRestApplication.class, args);
    }

    @Bean
    public WebMvcConfigurer swaggerDocsConfigurer() {
        return new WebMvcConfigurerAdapter() {

            @Override
            public void addViewControllers(ViewControllerRegistry registry) {
                registry.addViewController("/docs").setViewName("redirect:/docs/");
                registry.addViewController("/docs/").setViewName("forward:/docs/index.html");
            }
        };
    }
}
