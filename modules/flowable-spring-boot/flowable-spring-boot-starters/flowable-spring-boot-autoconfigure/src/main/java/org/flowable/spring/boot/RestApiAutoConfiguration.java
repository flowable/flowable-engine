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
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.rest.service.api.DmnRestUrls;
import org.flowable.engine.ProcessEngine;
import org.flowable.eventregistry.impl.EventRegistryEngine;
import org.flowable.eventregistry.rest.service.api.EventRestUrls;
import org.flowable.external.job.rest.service.api.ExternalJobRestUrls;
import org.flowable.idm.engine.IdmEngine;
import org.flowable.idm.rest.service.api.IdmRestResponseFactory;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.spring.boot.app.AppEngineRestConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.FlowableAppProperties;
import org.flowable.spring.boot.cmmn.CmmnEngineRestConfiguration;
import org.flowable.spring.boot.cmmn.CmmnEngineServicesAutoConfiguration;
import org.flowable.spring.boot.cmmn.FlowableCmmnProperties;
import org.flowable.spring.boot.dmn.DmnEngineRestConfiguration;
import org.flowable.spring.boot.dmn.DmnEngineServicesAutoConfiguration;
import org.flowable.spring.boot.dmn.FlowableDmnProperties;
import org.flowable.spring.boot.eventregistry.EventRegistryRestConfiguration;
import org.flowable.spring.boot.eventregistry.EventRegistryServicesAutoConfiguration;
import org.flowable.spring.boot.eventregistry.FlowableEventRegistryProperties;
import org.flowable.spring.boot.idm.FlowableIdmProperties;
import org.flowable.spring.boot.idm.IdmEngineRestConfiguration;
import org.flowable.spring.boot.idm.IdmEngineServicesAutoConfiguration;
import org.flowable.spring.boot.job.ExternalJobRestConfiguration;
import org.flowable.spring.boot.process.FlowableProcessProperties;
import org.flowable.spring.boot.process.ProcessEngineRestConfiguration;
import org.flowable.spring.boot.rest.BaseRestApiConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration and starter for the Flowable REST APIs.
 *
 * @author Joram Barrez
 * @author Josh Long
 * @author Vedran Pavic
 * @author Filip Hrisafov
 */
@ConditionalOnClass(ContentTypeResolver.class)
@ConditionalOnWebApplication
@AutoConfiguration(after = {
    MultipartAutoConfiguration.class,
    FlowableSecurityAutoConfiguration.class,
    AppEngineServicesAutoConfiguration.class,
    ProcessEngineServicesAutoConfiguration.class,
    CmmnEngineServicesAutoConfiguration.class,
    DmnEngineServicesAutoConfiguration.class,
    EventRegistryServicesAutoConfiguration.class,
    IdmEngineServicesAutoConfiguration.class
})
public class RestApiAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestUrls.class)
    @ConditionalOnBean(ProcessEngine.class)
    @EnableConfigurationProperties(FlowableProcessProperties.class)
    public static class ProcessEngineRestApiConfiguration extends BaseRestApiConfiguration {

        @Bean
        public ServletRegistrationBean processService(FlowableProcessProperties properties) {
            return registerServlet(properties.getServlet(), ProcessEngineRestConfiguration.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ExternalJobRestUrls.class)
    @Conditional(ExternalJobRestApiConfiguration.ExternalJobRestCondition.class)
    @EnableConfigurationProperties(FlowableProcessProperties.class)
    public static class ExternalJobRestApiConfiguration extends BaseRestApiConfiguration {

        @Bean
        public ServletRegistrationBean externalJobRestService(FlowableProcessProperties properties) {
            FlowableServlet servlet = new FlowableServlet("/external-job-api", "Flowable External Job Rest API");
            FlowableServlet processServlet = properties.getServlet();
            servlet.setLoadOnStartup(processServlet.getLoadOnStartup());
            return registerServlet(servlet, ExternalJobRestConfiguration.class);
        }

        static final class ExternalJobRestCondition extends AnyNestedCondition {

            ExternalJobRestCondition() {
                super(ConfigurationPhase.REGISTER_BEAN);
            }

            @ConditionalOnBean(ProcessEngine.class)
            private static final class ProcessEngineBeanCondition {

            }

            @ConditionalOnBean(CmmnEngine.class)
            private static final class CmmnEngineBeanCondition {

            }
        }

    }

    @Bean
    public ContentTypeResolver contentTypeResolver() {
        ContentTypeResolver resolver = new DefaultContentTypeResolver();
        return resolver;
    }
    
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(AppRestUrls.class)
    @ConditionalOnBean(AppEngine.class)
    public static class AppEngineRestApiConfiguration extends BaseRestApiConfiguration {

        @Bean
        public ServletRegistrationBean appServlet(FlowableAppProperties properties) {
            return registerServlet(properties.getServlet(), AppEngineRestConfiguration.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(CmmnRestUrls.class)
    @ConditionalOnBean(CmmnEngine.class)
    public static class CmmnEngineRestApiConfiguration extends BaseRestApiConfiguration {

        @Bean
        public ServletRegistrationBean cmmnServlet(FlowableCmmnProperties properties) {
            return registerServlet(properties.getServlet(), CmmnEngineRestConfiguration.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(DmnRestUrls.class)
    @ConditionalOnBean(DmnEngine.class)
    public static class DmnEngineRestApiConfiguration extends BaseRestApiConfiguration {

        @Bean
        public ServletRegistrationBean dmnServlet(FlowableDmnProperties properties) {
            return registerServlet(properties.getServlet(), DmnEngineRestConfiguration.class);
        }
    }
    
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(EventRestUrls.class)
    @ConditionalOnBean(EventRegistryEngine.class)
    public static class EventRegistryRestApiConfiguration extends BaseRestApiConfiguration {

        @Bean
        public ServletRegistrationBean eventRegistryServlet(FlowableEventRegistryProperties properties) {
            return registerServlet(properties.getServlet(), EventRegistryRestConfiguration.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(IdmRestResponseFactory.class)
    @ConditionalOnBean(IdmEngine.class)
    public static class IdmEngineRestApiConfiguration extends BaseRestApiConfiguration {

        @Bean
        public ServletRegistrationBean idmServlet(FlowableIdmProperties properties) {
            return registerServlet(properties.getServlet(), IdmEngineRestConfiguration.class);
        }
    }

}
