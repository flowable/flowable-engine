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

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.spring.boot.actuate.endpoint.ProcessEngineEndpoint;
import org.flowable.spring.boot.actuate.endpoint.ProcessEngineMvcEndpoint;
import org.flowable.spring.boot.condition.ConditionalOnProcessEngine;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The idea behind this module is that Spring Security could talk to the {@link org.flowable.engine.IdentityService} as required.
 *
 * @author Josh Long
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Endpoint.class)
@ConditionalOnProcessEngine
public class EndpointAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    //@ConditionalOnEnabledEndpoint
    public ProcessEngineEndpoint processEngineEndpoint(ProcessEngine engine) {
        return new ProcessEngineEndpoint(engine);
    }

//FIXME Bring back support for the process engine MVC Endpoint for displaying images
//    @Bean
//    @ConditionalOnMissingBean
//    @ConditionalOnEnabledEndpoint
    public ProcessEngineMvcEndpoint processEngineMvcEndpoint(
        RepositoryService repositoryService) {
        return new ProcessEngineMvcEndpoint(repositoryService);
    }
}
