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
package org.flowable.spring.boot.process;

import java.util.List;

import org.flowable.common.rest.variable.RestVariableConverter;
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.spring.boot.DispatcherServletConfiguration;
import org.flowable.spring.boot.json.Jackson2JsonRestConverterConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import tools.jackson.databind.ObjectMapper;

/**
 * Component scan for the Process engine REST API Configuration.
 *
 * @author Filip Hrisafov
 */
@Import({
        DispatcherServletConfiguration.class,
        Jackson2JsonRestConverterConfiguration.class,
})
@ComponentScan("org.flowable.rest.service.api")
public class ProcessEngineRestConfiguration {
    
    @ConditionalOnMissingBean //If we don't include this annotation, we cannot override the RestResponseFactory bean
    @Bean
    public RestResponseFactory restResponseFactory(ObjectMapper objectMapper, ObjectProvider<RestVariableConverter> variableConverters) {
        RestResponseFactory responseFactory = new RestResponseFactory(objectMapper);
        List<RestVariableConverter> additionalVariableConverters = variableConverters.orderedStream().toList();
        responseFactory.getVariableConverters().addAll(additionalVariableConverters);
        return responseFactory;
    }
}
