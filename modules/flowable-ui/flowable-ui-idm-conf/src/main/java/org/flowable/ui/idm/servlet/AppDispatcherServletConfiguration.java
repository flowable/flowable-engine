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
package org.flowable.ui.idm.servlet;

import org.flowable.ui.common.rest.idm.remote.RemoteAccountResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
@ComponentScan(value = { "org.flowable.ui.idm.rest.app", "org.flowable.ui.common.rest.exception" }, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = RemoteAccountResource.class))
@EnableAsync
public class AppDispatcherServletConfiguration extends WebMvcConfigurationSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppDispatcherServletConfiguration.class);

    @Override
    protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
        LOGGER.debug("Creating requestMappingHandlerMapping");
        RequestMappingHandlerMapping requestMappingHandlerMapping = new RequestMappingHandlerMapping();
        requestMappingHandlerMapping.setRemoveSemicolonContent(false);
        return requestMappingHandlerMapping;
    }
}
