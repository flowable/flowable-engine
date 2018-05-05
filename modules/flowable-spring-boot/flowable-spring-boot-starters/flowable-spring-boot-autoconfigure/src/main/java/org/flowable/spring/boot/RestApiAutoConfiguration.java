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
package org.flowable.spring.boot;

import org.flowable.app.engine.AppEngine;
import org.flowable.app.rest.AppRestUrls;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.common.rest.resolver.ContentTypeResolver;
import org.flowable.common.rest.resolver.DefaultContentTypeResolver;
import org.flowable.content.engine.ContentEngine;
import org.flowable.content.rest.ContentRestUrls;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.rest.service.api.DmnRestUrls;
import org.flowable.engine.ProcessEngine;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.rest.FormRestUrls;
import org.flowable.idm.engine.IdmEngine;
import org.flowable.idm.rest.service.api.IdmRestResponseFactory;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.spring.boot.app.AppEngineRestConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.FlowableAppProperties;
import org.flowable.spring.boot.cmmn.CmmnEngineRestConfiguration;
import org.flowable.spring.boot.cmmn.CmmnEngineServicesAutoConfiguration;
import org.flowable.spring.boot.cmmn.FlowableCmmnProperties;
import org.flowable.spring.boot.content.ContentEngineRestConfiguration;
import org.flowable.spring.boot.content.ContentEngineServicesAutoConfiguration;
import org.flowable.spring.boot.content.FlowableContentProperties;
import org.flowable.spring.boot.dmn.DmnEngineRestConfiguration;
import org.flowable.spring.boot.dmn.DmnEngineServicesAutoConfiguration;
import org.flowable.spring.boot.dmn.FlowableDmnProperties;
import org.flowable.spring.boot.form.FlowableFormProperties;
import org.flowable.spring.boot.form.FormEngineRestConfiguration;
import org.flowable.spring.boot.form.FormEngineServicesAutoConfiguration;
import org.flowable.spring.boot.idm.FlowableIdmProperties;
import org.flowable.spring.boot.idm.IdmEngineRestConfiguration;
import org.flowable.spring.boot.idm.IdmEngineServicesAutoConfiguration;
import org.flowable.spring.boot.process.FlowableProcessProperties;
import org.flowable.spring.boot.process.ProcessEngineRestConfiguration;
import org.flowable.spring.boot.rest.BaseRestApiConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration and starter for the Flowable REST APIs.
 *
 * @author Joram Barrez
 * @author Josh Long
 * @author Vedran Pavic
 * @author Filip Hrisafov
 */
@Configuration
@ConditionalOnClass(ContentTypeResolver.class)
@ConditionalOnWebApplication
@AutoConfigureAfter({
    //FIXME in order to support both 1.5.x and 2.0 we can't use MultipartAutoConfiguration (the package is changed)
    //MultipartAutoConfiguration.class,
    SecurityAutoConfiguration.class,
    AppEngineServicesAutoConfiguration.class,
    ProcessEngineServicesAutoConfiguration.class,
    CmmnEngineServicesAutoConfiguration.class,
    ContentEngineServicesAutoConfiguration.class,
    DmnEngineServicesAutoConfiguration.class,
    FormEngineServicesAutoConfiguration.class,
    IdmEngineServicesAutoConfiguration.class
})
public class RestApiAutoConfiguration {

    @Configuration
    @ConditionalOnClass(RestUrls.class)
    @ConditionalOnBean(ProcessEngine.class)
    @EnableConfigurationProperties(FlowableProcessProperties.class)
    public static class ProcessEngineRestApiConfiguration extends BaseRestApiConfiguration {

        @Bean
        public ServletRegistrationBean processService(FlowableProcessProperties properties) {
            return registerServlet(properties.getServlet(), ProcessEngineRestConfiguration.class);
        }
    }

    @Bean
    public ContentTypeResolver contentTypeResolver() {
        ContentTypeResolver resolver = new DefaultContentTypeResolver();
        return resolver;
    }
    
    @Configuration
    @ConditionalOnClass(AppRestUrls.class)
    @ConditionalOnBean(AppEngine.class)
    public static class AppEngineRestApiConfiguration extends BaseRestApiConfiguration {

        @Bean
        public ServletRegistrationBean appServlet(FlowableAppProperties properties) {
            return registerServlet(properties.getServlet(), AppEngineRestConfiguration.class);
        }
    }

    @Configuration
    @ConditionalOnClass(CmmnRestUrls.class)
    @ConditionalOnBean(CmmnEngine.class)
    public static class CmmnEngineRestApiConfiguration extends BaseRestApiConfiguration {

        @Bean
        public ServletRegistrationBean cmmnServlet(FlowableCmmnProperties properties) {
            return registerServlet(properties.getServlet(), CmmnEngineRestConfiguration.class);
        }
    }

    @Configuration
    @ConditionalOnClass(ContentRestUrls.class)
    @ConditionalOnBean(ContentEngine.class)
    public static class ContentEngineRestApiConfiguration extends BaseRestApiConfiguration {

        @Bean
        public ServletRegistrationBean contentServlet(FlowableContentProperties properties) {
            return registerServlet(properties.getServlet(), ContentEngineRestConfiguration.class);
        }
    }

    @Configuration
    @ConditionalOnClass(DmnRestUrls.class)
    @ConditionalOnBean(DmnEngine.class)
    public static class DmnEngineRestApiConfiguration extends BaseRestApiConfiguration {

        @Bean
        public ServletRegistrationBean dmnServlet(FlowableDmnProperties properties) {
            return registerServlet(properties.getServlet(), DmnEngineRestConfiguration.class);
        }
    }

    @Configuration
    @ConditionalOnClass(FormRestUrls.class)
    @ConditionalOnBean(FormEngine.class)
    public static class FormEngineRestApiConfiguration extends BaseRestApiConfiguration {

        @Bean
        public ServletRegistrationBean formServlet(FlowableFormProperties properties) {
            return registerServlet(properties.getServlet(), FormEngineRestConfiguration.class);
        }
    }

    @Configuration
    @ConditionalOnClass(IdmRestResponseFactory.class)
    @ConditionalOnBean(IdmEngine.class)
    public static class IdmEngineRestApiConfiguration extends BaseRestApiConfiguration {

        @Bean
        public ServletRegistrationBean idmServlet(FlowableIdmProperties properties) {
            return registerServlet(properties.getServlet(), IdmEngineRestConfiguration.class);
        }
    }

}
