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

import org.flowable.rest.app.properties.RestAppProperties;
import org.flowable.rest.conf.BootstrapConfiguration;
import org.flowable.rest.conf.DevelopmentConfiguration;
import org.flowable.rest.conf.SecurityConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author Filip Hrisafov
 */
@EnableConfigurationProperties({
    RestAppProperties.class
})
@Import({
    BootstrapConfiguration.class,
    SecurityConfiguration.class,
    DevelopmentConfiguration.class
})
@SpringBootApplication
public class FlowableRestApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(FlowableRestApplication.class, args);
    }

    @Bean
    public WebMvcConfigurer swaggerDocsConfigurer() {
        return new WebMvcConfigurer() {

            @Override
            public void addViewControllers(ViewControllerRegistry registry) {
                registry.addViewController("/docs").setViewName("redirect:/docs/");
                registry.addViewController("/docs/").setViewName("forward:/docs/index.html");
            }
        };
    }

    @Bean
    public GrantedAuthorityDefaults grantedAuthorityDefaults(RestAppProperties commonAppProperties) {
        return new GrantedAuthorityDefaults(commonAppProperties.getRolePrefix());
    }
}
