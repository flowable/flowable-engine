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
package org.flowable.spring.boot.rest;

import javax.servlet.MultipartConfigElement;

import org.flowable.spring.boot.FlowableServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * A common base rest api configuration for registering custom servlets.
 *
 * @author Filip Hrisafov
 */
public class BaseRestApiConfiguration implements ApplicationContextAware {

    protected ApplicationContext applicationContext;

    protected MultipartConfigElement multipartConfigElement;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    protected ServletRegistrationBean registerServlet(FlowableServlet servletProperties, Class<?> baseConfig) {
        AnnotationConfigWebApplicationContext dispatcherServletConfiguration = new AnnotationConfigWebApplicationContext();
        dispatcherServletConfiguration.setParent(applicationContext);
        dispatcherServletConfiguration.register(baseConfig);
        DispatcherServlet servlet = new DispatcherServlet(dispatcherServletConfiguration);
        String path = servletProperties.getPath();
        String urlMapping = (path.endsWith("/") ? path + "*" : path + "/*");
        ServletRegistrationBean registrationBean = new ServletRegistrationBean(servlet, urlMapping);
        registrationBean.setName(servletProperties.getName());
        registrationBean.setLoadOnStartup(servletProperties.getLoadOnStartup());
        registrationBean.setAsyncSupported(true);
        if (multipartConfigElement != null) {
            registrationBean.setMultipartConfig(multipartConfigElement);
        }
        return registrationBean;
    }

    @Autowired(required = false)
    public void setMultipartConfigElement(MultipartConfigElement multipartConfigElement) {
        this.multipartConfigElement = multipartConfigElement;
    }
}
