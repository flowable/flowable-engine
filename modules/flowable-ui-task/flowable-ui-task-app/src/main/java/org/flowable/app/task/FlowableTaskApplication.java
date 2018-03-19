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
package org.flowable.app.task;

import org.flowable.app.conf.ApplicationConfiguration;
import org.flowable.app.servlet.AppDispatcherServletConfiguration;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.common.impl.runtime.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author Filip Hrisafov
 */
@Import({
    ApplicationConfiguration.class,
    AppDispatcherServletConfiguration.class
})
@SpringBootApplication
public class FlowableTaskApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(FlowableTaskApplication.class, args);
    }

    @Bean
    public WebMvcConfigurer workflow() {
        return new WebMvcConfigurerAdapter() {

            @Override
            public void addViewControllers(ViewControllerRegistry registry) {
                registry.addViewController("/workflow").setViewName("redirect:/workflow/");
                registry.addViewController("/workflow/").setViewName("forward:/workflow/index.html");
            }
        };
    }

    @Bean(name = "clock")
    public Clock getClock(ProcessEngineConfiguration engineConfiguration) {
        return engineConfiguration.getClock();
    }

}
