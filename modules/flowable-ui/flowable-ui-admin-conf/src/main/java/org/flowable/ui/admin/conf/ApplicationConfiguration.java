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
package org.flowable.ui.admin.conf;

import javax.servlet.MultipartConfigElement;

import org.flowable.ui.admin.properties.FlowableAdminAppProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.lang.NonNull;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackages = {
    "org.flowable.ui.admin.repository",
    "org.flowable.ui.common.repository",
})
@Import(value = {
    AdminDatabaseConfiguration.class
})
@EnableConfigurationProperties(FlowableAdminAppProperties.class)
public class ApplicationConfiguration implements ApplicationContextAware {

    protected ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Bean
    public ServletRegistrationBean<DispatcherServlet> flowableAdminAppServlet(ObjectProvider<MultipartConfigElement> multipartConfig) {
        AnnotationConfigWebApplicationContext dispatcherServletConfiguration = new AnnotationConfigWebApplicationContext();
        dispatcherServletConfiguration.setParent(applicationContext);
        dispatcherServletConfiguration.register(DispatcherServletConfiguration.class);
        DispatcherServlet servlet = new DispatcherServlet(dispatcherServletConfiguration);
        ServletRegistrationBean<DispatcherServlet> registrationBean = new ServletRegistrationBean<>(servlet, "/admin-app/*");
        registrationBean.setName("Flowable Admin App");
        registrationBean.setLoadOnStartup(1);
        registrationBean.setAsyncSupported(true);
        multipartConfig.ifAvailable(registrationBean::setMultipartConfig);
        return registrationBean;
    }

    @Bean
    public WebMvcConfigurer adminApplicationWebMvcConfigurer() {
        return new WebMvcConfigurer() {

            @Override
            public void addViewControllers(@NonNull ViewControllerRegistry registry) {

                if (!ClassUtils.isPresent("org.flowable.ui.task.conf.ApplicationConfiguration", getClass().getClassLoader())) {
                    // If the task application is not present, then the root should be mapped to admin
                    registry.addViewController("/").setViewName("redirect:/admin/");
                }
                registry.addViewController("/admin").setViewName("redirect:/admin/");
                registry.addViewController("/admin/").setViewName("forward:/admin/index.html");
            }
        };
    }

}
